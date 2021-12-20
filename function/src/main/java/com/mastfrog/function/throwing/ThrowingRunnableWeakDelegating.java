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

import java.lang.ref.WeakReference;

/**
 *
 * @author Tim Boudreau
 */
final class ThrowingRunnableWeakDelegating implements ThrowingRunnable {

    private final WeakReference<ThrowingRunnable> ref;

    ThrowingRunnableWeakDelegating(ThrowingRunnable delegate) {
        this.ref = new WeakReference<>(delegate);
    }

    public String toString() {
        ThrowingRunnable del = ref.get();
        if (del != null) {
            return del.toString();
        }
        return "-gc'd-";
    }

    @Override
    public void run() throws Exception {
        ThrowingRunnable del = ref.get();
        if (del != null) {
            del.run();
        }
    }
}
