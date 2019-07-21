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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Variant on the CountDownLatch pattern, but which can
 *
 * @author Tim Boudreau
 */
public class OneThreadLatch {

    private final Sync sync = new Sync();

    private static final class Sync extends AbstractQueuedSynchronizer {

        Sync() {
            init();
        }

        void init() {
            setState(1);
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            if (disabled) {
                return 1;
            }
            // Sets the state positive if negative
            // If the absolute value of the state is > than the
            // absolute value in the first iteration, then
            // release has been called at least once, so acquire
            for (int c = getState(), initial = c, initialAbs = Math.abs(initial), cAbs = initialAbs; c != 0; c = getState(), cAbs = Math.abs(c)) {
                if (c < 0 || cAbs > initialAbs || disabled) {
//                    if (c < 0) {
                    compareAndSetState(c, cAbs + 1);
                    return 1;
//                    }
//                    return -1;
                }
                if (c > 0) {
                    return -1;
                }
                if (disabled) {
                    return 1;
                }
            }
            return 1;
        }

        @Override
        protected boolean tryReleaseShared(int releases) {
            for (int c = getState(), initial = c, initialAbs = Math.abs(initial), absC = initialAbs;; c = getState(), absC = Math.abs(c)) {
                if (c > 0 || (/*initial > 0 &&*/absC > initialAbs)) {
                    if (compareAndSetState(c, -c)) {
                        int newc = Math.abs(c) + 1;
                        compareAndSetState(c, newc);
                        return true;
                    }
                }
                return false;
            }
        }

        int releaseAll() {
            int count = 0;
            while (getQueueLength() > 0) {
                releaseShared(1);
                count++;
            }
            return count;
        }

        private volatile boolean disabled;

        public boolean enabled(boolean val) {
            if (disabled != !val) {
                if (!val) {
                    disabled = true;
                    releaseAll();
                } else {
                    init();
                    disabled = false;
                }
                return true;
            }
            return false;
        }

        boolean disabled(boolean disabled) {
            return enabled(!disabled);
        }

        boolean isDisabled() {
            return disabled;
        }
    }

    public boolean releaseOne() {
        return sync.releaseShared(1);
    }

    public int releaseAll() {
        return sync.releaseAll();
    }

    public boolean disable() {
        return sync.disabled(true);
    }

    public boolean enable() {
        return sync.disabled(false);
    }

    public boolean isDisabled() {
        return sync.isDisabled();
    }

    public int waiterCount() {
        return sync.getQueueLength();
    }

    public void await() throws InterruptedException {
        if (sync.isDisabled()) {
            return;
        }
        sync.acquireSharedInterruptibly(1);
    }

    public void awaitUninterruptibly() {
        if (sync.isDisabled()) {
            return;
        }
        sync.acquireShared(1);
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (sync.isDisabled()) {
            return true;
        }
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}
