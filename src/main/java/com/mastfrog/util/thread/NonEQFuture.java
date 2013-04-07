/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.thread;

import java.awt.EventQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Future which enforces the threading model that it cannot be waited for on
 * the event thread.
 *
 * @author Tim Boudreau
 */
public class NonEQFuture<T> implements Future<T> {
    private final Future<T> delegate;

    public NonEQFuture(Future<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> Future<T> wrap(Future<T> f) {
        if (f instanceof NonEQFuture) {
            return f;
        }
        return new NonEQFuture<T>(f);
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        checkThread();
        return delegate.get(timeout, unit);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        checkThread();
        return delegate.get();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    private void checkThread() throws ExecutionException {
        if (EventQueue.isDispatchThread()) {
            throw new ExecutionException("Attempt to block on the event thread",
                    new IllegalThreadStateException());
        }
    }
}
