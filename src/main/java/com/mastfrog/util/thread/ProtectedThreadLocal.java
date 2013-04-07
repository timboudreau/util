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

/**
 *
 * @author Tim Boudreau
 */
public class ProtectedThreadLocal<T> {

    private final ThreadLocal<T> tl = new ThreadLocal<>();

    public QuietAutoCloseable set(T obj) {
        final T old = tl.get();
        tl.set(obj);
        return new TLAutoClose<>(tl, old);
    }

    private static final class TLAutoClose<T> extends QuietAutoCloseable {

        private final ThreadLocal<T> tl;
        private final T oldVal;

        private TLAutoClose(ThreadLocal<T> tl, T oldVal) {
            this.tl = tl;
            this.oldVal = oldVal;
        }

        @Override
        public void close() {
            if (oldVal == null) {
                tl.remove();
            } else {
                tl.set(oldVal);
            }
        }

        @Override
        public String toString() {
            return super.toString() + '[' + (tl.get() != null
                    ? tl.get() + "" : oldVal != null ? oldVal + "" : "null") + ']';
        }
    }

    public T get() {
        return tl.get();
    }

    public void remove() {
        tl.remove();
    }

    @Override
    public String toString() {
        T obj = get();
        return obj == null ? tl.toString() : obj + "";
    }
}
