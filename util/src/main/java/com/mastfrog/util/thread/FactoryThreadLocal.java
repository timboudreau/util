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

import static com.mastfrog.util.Checks.notNull;
import java.util.function.Supplier;

/**
 * A ThreadLocal for things like lists and stacks, which has a factory that
 * supplies a value if none was present.
 *
 * @author Tim Boudreau
 */
public final class FactoryThreadLocal<T> {

    private final Supplier<T> factory;
    private final ThreadLocal<T> threadLocal = new ThreadLocal<>();

    public FactoryThreadLocal(Supplier<T> factory) {
        this.factory = notNull("factory", factory);
    }

    public boolean hasValue() {
        return threadLocal.get() != null;
    }

    /**
     * Get the object for this thread, creating it if necessary.
     */
    public T get() {
        T result = threadLocal.get();
        if (result == null) {
            result = factory.get();
            threadLocal.set(result);
        }
        return result;
    }

    /**
     * Set the object for this thread.
     *
     * @param obj The object
     */
    public void set(T obj) {
        threadLocal.set(obj);
    }

    /**
     * Get an autocloseable which sets the value, and restores the default
     * value afterward.
     *
     * @param val
     * @return
     */
    public NonThrowingAutoCloseable open(T val) {
        set(val);
        return this::clear;
    }

    public void clear() {
        set(factory.get());
    }
}
