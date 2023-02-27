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
package com.mastfrog.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * A Trieber stack, for when you just need multiple threads to append to a
 * non-blocking list. Note the API is considerably less right than a list, but
 * all operations are guaranteed to be atomic.
 * <p>
 * Note that one price of atomicity - specifically the requirement that all
 * mutations involve mutation of <i>exactly and only one field</i> (and the fact
 * that tail mutation means we don't know <i>which<i> field until we find it)
 * means that the JVM's maximum stack-size fundamentally limits list-size.
 * </p><p>
 * This class does implement iterable; any iterator created will reflect a
 * snapshot of the collection contents at <i>some</i> time during its lifetime;
 * it may or may not be the current state. Iterators will never throw a
 * ConcurrentModificationException, and do not implement <code>remove()</code>.
 *
 * @author Tim Boudreau
 */
public final class ConcurrentLinkedList<T> implements Iterable<T> {

    private static final AtomicReferenceFieldUpdater<ConcurrentLinkedList, Cell> HEAD
            = AtomicReferenceFieldUpdater.newUpdater(ConcurrentLinkedList.class, Cell.class, "head");
    private volatile Cell<T> head;
    private final boolean fifo;

    public static <T> ConcurrentLinkedList<T> lifo() {
        return new ConcurrentLinkedList<>(false, null);
    }

    public static <T> ConcurrentLinkedList<T> fifo() {
        return new ConcurrentLinkedList<>(true, null);
    }

    ConcurrentLinkedList(boolean fifo, Cell<T> head) {
        this.fifo = fifo;
        this.head = head;
    }

    public boolean isEmpty() {
        return headCell() == null;
    }

    public int size() {
        Cell<T> headCell = headCell();
        return headCell == null ? 0 : headCell.size();
    }

    public ConcurrentLinkedList<T> copy() {
        Cell<T> headCell = headCell();
        return new ConcurrentLinkedList<>(fifo, headCell == null ? null : headCell.copy());
    }
    
    public boolean isFifo() {
        return fifo;
    }

    public boolean contains(Object o) {
        boolean result = false;
        Cell<T> cell = headCell();
        while (cell != null) {
            if (Objects.equals(o, cell.get())) {
                return true;
            }
            cell = cell.next();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public ConcurrentLinkedList<T> push(T obj) {
        // The rule here is that you only every mutate exactly ONE
        // thing if you want to maintain atomicity.  That makes the ability
        // mutate at the tail a bit fraught until you think it through:
        //
        // Two threads concurrently update the tail; one will succeed in
        // appending a new tail to the cell that was the tail before they
        // both entered.  The second one WILL pass through getAndUpdate() and
        // attempt the mutation - and it will then get RE-RUN when that fails
        // (atomics loop on contention); the second time, it will see the new
        // tail appended by the first one, and traverse down to IT'S tail
        // and set that.  Voila, atomicity preserved.
        HEAD.getAndUpdate(this, cell -> {
            if (cell == null) {
                return new Cell<>(obj);
            } else {
                if (!fifo) {
                    return new Cell<>(obj, cell);
                } else {
                    cell.append(obj);
                    return cell;
                }
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    Cell<T> headCell() {
        return HEAD.get(this);
    }

    public T peek() {
        Cell<T> h = headCell();
        return h == null ? null : h.get();
    }

    @SuppressWarnings("unchecked")
    public T pop() {
        Cell<T> old = HEAD.getAndUpdate(this, cell -> {
            return cell == null ? null : cell.next();
        });
        return old == null ? null : old.get();
    }

    @Override
    public void forEach(Consumer<? super T> c) {
        Cell<T> headCell = headCell();
        if (headCell == null) {
            return;
        }
        headCell.each(cell -> c.accept(cell.get()));
    }

    @SuppressWarnings("unchecked")
    public int drain(Consumer<T> c) {
        Cell<T> old = HEAD.getAndUpdate(this, cell -> {
            return null;
        });
        if (old != null) {
            return old.each(cell -> c.accept(cell.get()));
        }
        return 0;
    }

    @SuppressWarnings("Unchecked")
    public int drain(T newHead, Consumer<T> c) {

        @SuppressWarnings("unchecked")
        Cell<T> old = HEAD.getAndUpdate(this, cell -> {
            return new Cell<>(newHead);
        });
        if (old != null) {
            return old.each(cell -> c.accept(cell.get()));
        }
        return 0;
    }

    @Override
    public Iterator<T> iterator() {
        Cell<T> h = headCell();
        return h == null ? Collections.emptyIterator() : h.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Cell<T> cell = headCell();
        while (cell != null) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(cell.get());
            cell = cell.next();
        }
        return sb.append(']').toString();
    }

    private static class Cell<T> implements Supplier<T>, Iterable<T> {

        private static final AtomicReferenceFieldUpdater<Cell, Cell> NEXT
                = AtomicReferenceFieldUpdater.newUpdater(Cell.class, Cell.class, "next");

        private volatile Cell<T> next;
        private final T value;

        Cell(T value) {
            this.value = value;
        }

        Cell(T value, Cell<T> next) {
            this.value = value;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        Cell<T> clearNext() {
            return NEXT.getAndSet(this, null);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        Cell<T> copy() {
            Cell<T> n = next();
            return new Cell<>(value, n == null ? null : n.copy());
        }

        @Override
        public T get() {
            return value;
        }

        @SuppressWarnings("empty-statement")
        public int size() {
            int result = 0;
            for (Cell<T> cell = this; cell != null; result++, cell = cell.next());
            return result;
        }

        int each(Consumer<? super Cell<T>> cellConsumer) {
            cellConsumer.accept(this);
            int result = 1;
            for (Cell<T> nextCell = next(); nextCell != null; nextCell = nextCell.next(), result++) {
                cellConsumer.accept(nextCell);
            }
            return result;
        }

        @Override
        public void forEach(Consumer<? super T> cellConsumer) {
            each(cell -> cellConsumer.accept(cell.get()));
        }

        @SuppressWarnings({"rawTypes", "unchecked"})
        private Cell<T> doUpdate(UnaryOperator<Cell<T>> uo) {
            UnaryOperator<Cell> cast = (UnaryOperator) uo;
            return NEXT.getAndUpdate(this, cast);
        }

        @SuppressWarnings("unchecked")
        Cell<T> next() {
            return NEXT.get(this);
        }

        @SuppressWarnings("unchecked")
        Cell<T> updateLast(UnaryOperator<Cell<T>> u) {
            U<T> uu = new U<>(u);
            Cell<T> cell = this;
            // If we do the *whole* traversal with recursion, we wind up
            // with a fairly small limit on data structure size - the JVM's
            // stack limit.  So, we go as far as we can in a loop, and then
            // recurse so a concurrent append will cause us to drop to the
            // newly added cell
            for (;;) {
                // a plain read is fine here; it triggers a safe read in either branch
                if (cell.next == null) {
                    cell.doUpdate(uu);
                    return uu.last;
                }
                cell = cell.next();
            }
        }

        static class U<T> implements UnaryOperator<Cell<T>> {

            private final UnaryOperator<Cell<T>> real;
            Cell<T> last;

            public U(UnaryOperator<Cell<T>> real) {
                this.real = real;
            }

            Cell<T> perform(Cell<T> initial) {
                assert initial != null;
                last = initial;
                return initial.doUpdate(this);
            }

            @Override
            public Cell<T> apply(Cell<T> old) {
                if (old == null) {
                    return real.apply(last);
                } else {
                    return perform(old);
                }
            }
        }

        @SuppressWarnings("unchecked")
        protected void append(T obj) {
            Cell<T> nue = new Cell<>(obj);
            Cell<T> cell = this;
            while (cell != null) {
                Cell<T> updated = NEXT.updateAndGet(cell, old -> {
                    if (old == null) {
                        return nue;
                    }
                    return old;
                });
                if (updated == nue) {
                    break;
                }
                cell = cell.next;
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new Iter<>(this);
        }

        static class Iter<T> implements Iterator<T> {

            Cell<T> current;

            Iter(Cell<T> first) {
                current = first;
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                Cell<T> old = current;
                if (old == null) {
                    throw new NoSuchElementException();
                }
                current = old.next();
                return old.get();
            }
        }
    }
}
