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

import com.mastfrog.function.misc.QuietAutoClosable;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.concurrent.Callable;

/**
 * A ThreadLocal which supports try-with-resources and functional operations,
 * with reentrant capabilities - if a value was set prior to a call to set(),
 * closing the returned AutoCloseable restores the value.
 *
 * @author Tim Boudreau
 */
public class AutoCloseThreadLocal<T> {

    private final ThreadLocal<T> tl = new ThreadLocal<>();

    /**
     * Set the value of this thread local; the value will be restored to its
 current value (which might be null) when the close() method of the
 returned QuietAutoCloseable is called.
     *
     * @param obj The object to set as the value
     * @return An AutoCloseable suitable for use in try-with-resources
     * operations
     */
    public QuietAutoClosable set(T obj) {
        final T old = tl.get();
        tl.set(obj);
        return new TLAutoClose<>(tl, old);
    }

    /**
     * Run the passed runnable, setting the value to the passed value before,
     * and restoring it to its current value ofer.
     *
     * @param value The value to set for this runnable
     * @param run The runnable to run
     */
    public void withValue(T value, Runnable run) {
        notNull("run", run);
        final T old = tl.get();
        try {
            tl.set(value);
            run.run();
        } finally {
            if (old != null) {
                tl.set(old);
            } else {
                tl.remove();
            }
        }
    }

    /**
     * Invoke the passed callable, setting the value to the passed value before,
     * and restoring it to its current value ofer.
     *
     * @param value The value to set for this runnable
     * @param run The runnable to run
     * @return the return value of the callable
     * @throws Exception if the callable fails
     */
    public T withValue(T value, Callable<T> run) throws Exception {
        notNull("run", run);
        final T old = tl.get();
        try {
            tl.set(value);
            return run.call();
        } finally {
            if (old != null) {
                tl.set(old);
            } else {
                tl.remove();
            }
        }
    }

    public void clear() {
        tl.remove();
    }

    private static final class TLAutoClose<T> implements QuietAutoClosable {

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

    /**
     * Get the current value for the calling thread.
     * @return The value or null if none
     */
    public T get() {
        return tl.get();
    }

    /**
     * Clear the value for the current thread.
     */
    public void remove() {
        tl.remove();
    }

    @Override
    public String toString() {
        T obj = get();
        return obj == null ? tl.toString() : obj + "";
    }
}
