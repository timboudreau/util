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
 * Variant on the CountDownLatch pattern, which self-resets after releasing a
 * thread, and releases only one thread at a time.
 *
 * @author Tim Boudreau
 */
public final class OneThreadLatch {

    private final Sync sync = new Sync();

    private static final class Sync extends AbstractQueuedSynchronizer {

        private volatile boolean disabled;

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
                    compareAndSetState(c, cAbs + 1);
                    return 1;
                }
                if (c > 0) {
                    return -1;
                }
            }
            return 1;
        }

        @Override
        protected boolean tryReleaseShared(int releases) {
            for (int c = getState(), initial = c, initialAbs = Math.abs(initial), absC = initialAbs;; c = getState(), absC = Math.abs(c)) {
                if (c > 0 || absC > initialAbs) {
                    if (compareAndSetState(c, -c)) {
                        compareAndSetState(c, absC + 1);
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

        int state() {
            return getState();
        }
    }

    /**
     * Release one thread if any are waiting.
     *
     * @return whether or not a thread was released
     */
    public boolean releaseOne() {
        return sync.releaseShared(1);
    }

    /**
     * Release <i>all</i> waiting threads (possibly releasing one thread
     * multiple times, if it blocks again quickly enough).
     *
     * @return The count of releases this call generated
     */
    public int releaseAll() {
        return sync.releaseAll();
    }

    /**
     * Disable this latch, so threads proceed without blocking unless this latch
     * is reenabled.
     *
     * @return True if the enabled state was changed
     */
    public boolean disable() {
        return sync.disabled(true);
    }

    /**
     * Enable this latch if it is disabled, so threads calling any of the await
     * methods block - must be preceded by a call to <code>disable()</code>, as
     * the initial state of the latch is enabled.
     *
     * @return True if the enabled state was changed
     */
    public boolean enable() {
        return sync.disabled(false);
    }

    /**
     * Determine if this latch has been disabled.
     *
     * @return True if it is disabled
     */
    public boolean isDisabled() {
        return sync.isDisabled();
    }

    /**
     * Get the number of waiting threads, following the contract of
     * <code>AbstractQueuedSynchronizer.getQueueLength()</code>.
     *
     * @see java.util.concurrent.locks.AbstractQueuedSynchronizer
     * @return The number of waiting threads
     */
    public int waiterCount() {
        return sync.getQueueLength();
    }

    /**
     * Get the number of times in the lifetime of this latch, or since the last
     * call to <code>enable()</code> following a call to <code>disable()</code>,
     * that a thread has been released and the state has been reset, with the
     * negative bit set if no threads are waiting. This is mainly used in tests
     * to ensure the latch really was accessed and a waiting thread was
     * released, and that none are still blocked, and can be used detecting only
     * changes in the value to determine if new threads have blocked on this
     * latch.
     *
     * @return The release count, * -1 if no threads are currently queued
     */
    public int releaseCount() {
        return sync.state();
    }

    /**
     * Await a call to <code>releaseOne()</code> or <code>releaseAll()</code>
     * indefinitely.
     *
     * @throws InterruptedException
     */
    public void await() throws InterruptedException {
        if (sync.isDisabled()) {
            return;
        }
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Await a call to <code>releaseOne()</code> or <code>releaseAll()</code>
     * indefinitely.
     */
    public void awaitUninterruptibly() {
        if (sync.isDisabled()) {
            return;
        }
        sync.acquireShared(1);
    }

    /**
     * Await a call to <code>releaseOne()</code> or <code>releaseAll()</code>
     * until the timeout expires.
     *
     * @param timeout The timeout
     * @param unit The timeout units
     * @return true if the operation succeeded
     * @throws InterruptedException If the thread is interrupted while waiting
     * (can happen during system shutdown)
     */
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (sync.isDisabled()) {
            return true;
        }
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }
}
