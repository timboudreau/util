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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Variant on the CountDownLatch pattern, but which can
 *
 * @author Tim Boudreau
 */
public class OneThreadLatch {

    private final Sync sync = new Sync(1);

    private static final class Sync extends AbstractQueuedSynchronizer {

        Sync(int count) {
            setState(count);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            int c = getState();
            if (c == -1) {
                return true;
            }
            if (c == 0) {
                boolean result = compareAndSetState(0, 1);
                return result;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == -1) {
                return true;
            }
            return compareAndSetState(1, 0);
        }

        @Override
        protected boolean isHeldExclusively() {
            return true;
        }

        int releaseAll() {
            int q = getQueueLength();
            for (int i = 0; i < 1000; i++) {
                release(1);
                try {
                    Thread.sleep(1);
                    if (getState() == 0) {
                        break;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(OneThreadLatch.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return q;
        }

        boolean disabled(boolean disabled) {
            if (disabled) {
                releaseAll();
                for (;;) {
                    int c = getState();
                    if (c == -1) {
                        return false;
                    }
                    if (compareAndSetState(c, -1)) {
                        return true;
                    }
                }
            } else {
                for (;;) {
                    int c = getState();
                    if (c != -1) {
                        return false;
                    }
                    return compareAndSetState(c, 1);
                }
            }
        }

        boolean isDisabled() {
            return getState() == -1;
        }
    }

    public boolean releaseOne() {
        return sync.release(1);
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
        await(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (sync.isDisabled()) {
            return true;
        }
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}
