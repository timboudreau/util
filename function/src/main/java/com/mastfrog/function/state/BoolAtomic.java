/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

package com.mastfrog.function.state;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Tim Boudreau
 */
final class BoolAtomic implements Bool {

    private final AtomicBoolean value;

    BoolAtomic(boolean value) {
        this.value = new AtomicBoolean(value);
    }

    BoolAtomic() {
        this.value = new AtomicBoolean();
    }

    @Override
    public boolean set() {
        return value.compareAndSet(false, true);
    }

    @Override
    public boolean reset() {
        return !value.getAndSet(false);
    }

    @Override
    public boolean runAndSet(Runnable r) {
        if (value.compareAndSet(false, true)) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean set(boolean val) {
        boolean result = value.compareAndSet(!val, val);
        return result ? !val : val;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public boolean toggle() {
        // XXX should loop here until we successfully change
        // the value.
        boolean old = value.get();
        value.set(!old);
        return old;
    }

    @Override
    public void accept(boolean val) {
        value.set(val);
    }

    @Override
    public boolean getAsBoolean() {
        return value.get();
    }
}
