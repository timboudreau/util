/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.function.throwing;

import com.mastfrog.function.state.Obj;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A composable runnable, used by ThrowingRunnable.composable() and
 * ThrowingRunnable.oneShot().
 *
 * @author Tim Boudreau
 */
final class ComposableAtomicThrowingRunnable implements ThrowingRunnable {

    private static final AtomicReferenceFieldUpdater<ComposableAtomicThrowingRunnable, Cell> REF_FIELD
            = AtomicReferenceFieldUpdater.newUpdater(ComposableAtomicThrowingRunnable.class,
                    Cell.class, "head");
    private volatile Cell head;
    private final boolean oneShot;
    private final boolean lifo;

    ComposableAtomicThrowingRunnable(boolean oneShot, boolean lifo) {
        this.oneShot = oneShot;
        this.lifo = lifo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Cell cell = REF_FIELD.get(this);
        while (cell != null) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(cell.run);
        }
        return sb.insert(0, getClass().getSimpleName() + "(").append(")").toString();
    }

    @Override
    public synchronized ThrowingRunnable andThen(ThrowingRunnable run) {
        addLast(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysRun(Runnable run) {
        addLast(ThrowingRunnable.fromRunnable(run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlways(ThrowingRunnable run) {
        addLast(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysRunFirst(Runnable run) {
        addFirst(ThrowingRunnable.fromRunnable(run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysFirst(ThrowingRunnable run) {
        addFirst(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysIf(BooleanSupplier test, ThrowingRunnable run) {
        addLast(new Conditional(test, run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysIfNotNull(Supplier<?> testForNull, ThrowingRunnable run) {
        addLast(new NullCheck(testForNull, run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThenIfNotNull(Supplier<?> test, ThrowingRunnable run) {
        addFirst(new NullCheck(test, run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThenIf(BooleanSupplier test, ThrowingRunnable run) {
        addFirst(new Conditional(test, run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThen(Runnable run) {
        addLast(ThrowingRunnable.fromRunnable(run));
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThen(Callable<Void> run) {
        addLast(() -> run.call());
        return this;
    }

    private void addLast(ThrowingRunnable run) {
        REF_FIELD.updateAndGet(this, old -> {
            return new Cell(run, old);
        });
    }

    private void addFirst(ThrowingRunnable run) {
        REF_FIELD.updateAndGet(this, old -> {
            if (old == null) {
                return new Cell(run, null);
            }
            List<ThrowingRunnable> cells = new LinkedList<>();
            while (old != null) {
                cells.add(old.run);
                old = old.parent;
            }
            Cell newHead = null;
            for (ThrowingRunnable t : cells) {
                newHead = new Cell(t, newHead);
            }
            return newHead;
        });
    }

    @Override
    public void run() throws Exception {
        Cell cell = REF_FIELD.getAndUpdate(this, old -> {
            if (oneShot) {
                return null;
            }
            return old;
        });
        if (cell == null) {
            return;
        }
        Obj<Throwable> thrown = Obj.create();
        if (lifo) {
            while (cell != null) {
                cell = cell.run(thrown);
            }
        } else {
            cell.runReverse(thrown);
        }
        thrown.ifNotNull(Exceptions::chuck);
    }

    static class Cell {

        private final ThrowingRunnable run;
        private final Cell parent;

        public Cell(ThrowingRunnable run, Cell parent) {
            this.run = run;
            this.parent = parent;
        }

        void runReverse(Obj<Throwable> th) {
            LinkedList<ThrowingRunnable> items = new LinkedList<>();
            Cell cell = this;
            while (cell != null) {
                items.addFirst(cell.run);
                cell = cell.parent;
            }
            for (ThrowingRunnable run : items) {
                try {
                    run.run();
                } catch (Exception | Error ex) {
                    th.apply(old -> {
                        if (old != null) {
                            old.addSuppressed(ex);
                            return old;
                        }
                        return ex;
                    });
                }
            }
        }

        Cell run(Obj<Throwable> th) {
            try {
                run.run();
            } catch (Exception | Error ex) {
                th.apply(old -> {
                    if (old != null) {
                        old.addSuppressed(ex);
                        return old;
                    }
                    return ex;
                });
            }
            return parent;
        }
    }

    // These could be lambdas, but this way we can preserve a meaningful
    // value of toString(), which is particularly useful as shutdown hooks
    // where it is needed for diagnosing ordering issues
    static class Conditional implements ThrowingRunnable {

        private final BooleanSupplier test;
        private final ThrowingRunnable run;

        public Conditional(BooleanSupplier test, ThrowingRunnable run) {
            this.test = test;
            this.run = run;
        }

        @Override
        public void run() throws Exception {
            if (test.getAsBoolean()) {
                run.run();
            }
        }

        @Override
        public String toString() {
            return "if(" + test + "){" + run + "}";
        }
    }

    static class NullCheck implements ThrowingRunnable {

        private final Supplier<?> supp;
        private final ThrowingRunnable run;

        public NullCheck(Supplier<?> supp, ThrowingRunnable run) {
            this.supp = supp;
            this.run = run;
        }

        @Override
        public void run() throws Exception {
            if (supp.get() != null) {
                run.run();
            }
        }

        @Override
        public String toString() {
            return "ifNotNull(" + supp + "){" + run + "}";
        }
    }
}
