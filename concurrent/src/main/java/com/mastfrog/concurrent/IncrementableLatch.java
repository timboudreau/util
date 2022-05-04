/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import static com.mastfrog.util.preconditions.Checks.notNull;
import static java.lang.Long.max;
import java.time.Duration;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A slight twist on the CountDownLatch pattern, allowing the count to be
 * incremented as well as decremented, so the latch can either be reused, or
 * work added that will further delay waiters while they are waiting.
 *
 * @author Tim Boudreau
 */
public final class IncrementableLatch {

    private final Sync sync;

    IncrementableLatch() {
        this.sync = new Sync();
    }

    int count() {
        return sync.count();
    }

    /**
     * Create a new latch.
     *
     * @return A latch
     */
    public static IncrementableLatch create() {
        return new IncrementableLatch();
    }

    /**
     * Await the count reaching zero.
     *
     * @throws InterruptedException If the thread is interrupted
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Increase the count of this latch.
     *
     * @return True if it was incremented from zero, meaning this latch was
     * previously completed.
     */
    public boolean increment() {
        return sync.increment();
    }

    /**
     * Increment this latch for the duration that the passed runnable runs; if
     * you have some code that might briefly bring the count to zero, but you do
     * not want to release blocked threads, at least until the runnable has
     * completed, use this method.
     *
     * @param r A runnable, non-null
     */
    public void hold(Runnable r) {
        increment();
        try {
            notNull("r", r).run();
        } finally {
            countDown();
        }
    }

    /**
     * Await the count reaching zero for the amount of time expressed by the
     * passed duration.
     *
     * @param duration A duration
     * @return true if the count down completed, false if timed out
     * @throws InterruptedException
     */
    public boolean await(Duration duration) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, max(0, duration.toNanos()));
    }

    /**
     * Determine if this instance has never been blocked on and never counted
     * down.
     *
     * @return True if the instance is unused.
     */
    public boolean isUnused() {
        return sync.isPristine();
    }

    /**
     * Decrement the count down by one.
     *
     * @return True if the count reached zero (that does not mean it is *still*
     * at zero)
     */
    public boolean countDown() {
        return sync.releaseShared(1);
    }

    /**
     * Decrement the count all the way to zero, unblocking all waiting threads.
     */
    public void releaseAll() {
        sync.clear();
    }

    /**
     * Determine if any threads are waiting for this latch.
     *
     * @return True if there were waiters at the instant of this call
     */
    public boolean hasWaiters() {
        return sync.hasQueuedThreads();
    }

    @Override
    public String toString() {
        return Integer.toString(sync.count());
    }

    static int masked(int val) {
        return val & ~Integer.MIN_VALUE;
    }

    private static final class Sync extends AbstractQueuedSynchronizer {

        Sync() {
            // aka 0b10000000000000000000000000000000
            // We use the last bit as a placeholder until this latch has been
            // used at least once, to prevent calls to await() from seeing 0 and
            // immediately unblocking if called before first use.
            setState(Integer.MIN_VALUE);
        }

        int count() {
            int st = getState();
            int result = masked(st);
            if (result != st) {
                return 1;
            }
            return result;
        }

        int clear() {
            for (;;) {
                int old = getState();
                int nue = 0;
                if (compareAndSetState(old, 0)) {
                    return masked(old);
                }
            }
        }

        boolean isPristine() {
            int st = getState();
            int result = masked(st);
            return result != st;
        }

        boolean increment() {
            for (;;) {
                int c = getState();
                int nextC = masked(c) + 1;
                if (compareAndSetState(c, nextC)) {
                    return c == 0;
                }
            }
        }

        @Override
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int releases) {
            for (;;) {
                int c = getState();
                int m = masked(c);
                if (m == 0 || m != c) {
                    return false;
                }
                int nextc = masked(c) - 1;
                if (compareAndSetState(c, nextc)) {
                    return nextc == 0;
                }
            }
        }
    }
}
