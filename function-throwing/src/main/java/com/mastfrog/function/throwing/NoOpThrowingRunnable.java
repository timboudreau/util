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

/**
 *
 * @author Tim Boudreau
 */
final class NoOpThrowingRunnable implements ThrowingRunnable {

    @Override
    public void run() throws Exception {
        // do nothing
    }

    @Override
    public ThrowingRunnable andThen(ThrowingRunnable run) {
        return run;
    }

    @Override
    public ThrowingRunnable andThen(Runnable run) {
        return run::run;
    }

    @Override
    public ThrowingRunnable andThen(Callable<Void> run) {
        return run::call;
    }

    @Override
    public ThrowingRunnable andAlwaysRun(Runnable run) {
        return run::run;
    }

    @Override
    public ThrowingRunnable andAlways(ThrowingRunnable run) {
        return run;
    }

    @Override
    public String toString() {
        return "no-op-throwing-runnable";
    }

}
