/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class ComposableThrowingRunnable implements ThrowingRunnable {

    private ThrowingRunnable inner = ThrowingRunnable.NO_OP;
    private final boolean oneShot;
    private final boolean lifo;

    ComposableThrowingRunnable(boolean oneShot, boolean lifo) {
        this.oneShot = oneShot;
        this.lifo = lifo;
    }

    @Override
    public void run() throws Exception {
        ThrowingRunnable in;
        synchronized (this) {
            in = inner;
            if (oneShot) {
                inner = ThrowingRunnable.NO_OP;
            }
        }
        in.run();
    }

    @Override
    public synchronized ThrowingRunnable andThen(ThrowingRunnable run) {
        inner = inner.andThen(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysRun(Runnable run) {
        inner = lifo ? inner.andAlwaysRunFirst(run) : inner.andAlwaysRun(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlways(ThrowingRunnable run) {
        inner = lifo ? inner.andAlwaysFirst(run) : inner.andAlways(run);
        return this;
    }

    @Override
    public ThrowingRunnable andAlwaysRunFirst(Runnable run) {
        inner = !lifo ? inner.andAlwaysRunFirst(run) : inner.andAlwaysRun(run);
        return this;
    }

    @Override
    public ThrowingRunnable andAlwaysFirst(ThrowingRunnable run) {
        inner = !lifo ? inner.andAlwaysFirst(run) : inner.andAlways(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysIf(BooleanSupplier test, ThrowingRunnable run) {
        inner = inner.andAlwaysIf(test, run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andAlwaysIfNotNull(Supplier<?> testForNull, ThrowingRunnable run) {
        inner = inner.andAlwaysIfNotNull(testForNull, run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThenIfNotNull(Supplier<?> test, ThrowingRunnable run) {
        inner = inner.andThenIfNotNull(test, run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThenIf(BooleanSupplier test, ThrowingRunnable run) {
        inner = inner.andThenIf(test, run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThen(Runnable run) {
        inner = inner.andThen(run);
        return this;
    }

    @Override
    public synchronized ThrowingRunnable andThen(Callable<Void> run) {
        inner = inner.andThen(run);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + inner + ")";
    }
}
