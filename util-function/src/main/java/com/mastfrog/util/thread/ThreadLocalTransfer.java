/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

/**
 * One-shot transfer an object between two decoupled parties, clearing the
 * reference after a call to get.  Basically, a ThreadLocal whose value
 * gets cleared after any call to get() so it cannot leak memory.
 *
 * @author Tim Boudreau
 */
public final class ThreadLocalTransfer<T> implements QuietAutoCloseable {

    private final ThreadLocal<Transfer<T>> xfer = new ThreadLocal<>();

    public T get() {
        Transfer<T> xf = xfer.get();
        return xf == null ? null : xf.get();
    }

    public QuietAutoCloseable set(T obj) {
        Transfer<T> xf = xfer.get();
        if (xf == null) {
            xf = new Transfer<>(obj);
            xfer.set(xf);
        } else {
            xf.set(obj);
        }
        return this;
    }

    @Override
    public void close() {
        Transfer<T> xf = xfer.get();
        if (xf != null) {
            xf.clear();
        }
    }

    private static final class Transfer<T> {

        private T obj;

        Transfer(T obj) {
            this.obj = obj;
        }

        T set(T obj) {
            T old = this.obj;
            this.obj = obj;
            return old;
        }

        T get() {
            return set(null);
        }

        void clear() {
            obj = null;
        }
    }
}
