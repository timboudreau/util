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

import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public class ThreadLocalBoolean {

    private final ThreadLocal<Boolean> tl = new ThreadLocal<>();
    private final boolean initialValue;

    public ThreadLocalBoolean() {
        this(false);
    }

    public ThreadLocalBoolean(boolean initialValue) {
        this.initialValue = initialValue;
    }

    public boolean get() {
        Boolean result = tl.get();
        if (result == null) {
            result = initialValue;
            tl.set(result);
        }
        return result;
    }

    public void set(boolean val) {
        tl.set(val);
    }

    public void toggle(Runnable r) {
        boolean old = get();
        set(!initialValue);
        try {
            r.run();
        } finally {
            set(old);
        }
    }

    public void toggleDuring(ThrowingRunnable r) throws Exception {
        boolean old = get();
        set(!initialValue);
        try {
            r.run();
        } finally {
            set(old);
        }
    }

    public <T> T toggleAndGetOrThrow(ThrowingSupplier<T> r) throws Exception {
        boolean old = get();
        set(!initialValue);
        try {
            return r.get();
        } finally {
            set(old);
        }
    }

    public <T> T toggleAndGet(Supplier<T> r) {
        boolean old = get();
        set(!initialValue);
        try {
            return r.get();
        } finally {
            set(old);
        }
    }

    public com.mastfrog.function.misc.QuietAutoClosable in() {
        boolean old = get();
        set(!initialValue);
        return () -> {
            set(old);
        };
    }
}
