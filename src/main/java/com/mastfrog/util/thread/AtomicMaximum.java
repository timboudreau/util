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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Non-blocking counter which tracks thread contention.  To use, wrap the
 * logic to be run in a Runnable and pass it to the run() method.  This object
 * can be queried for the current number of threads inside the run() method and
 * the historical maximum number of threads.  Calls to collect such statistics
 * and the run() method are non-blocking operations and this class uses
 * no locks.  It is useful for gathering data about thread contention and
 * possibly for self-tuning algorithms which determine thread-pool size.
 * <p/>
 * For convenience, this class extends Number.  The numeric value is the
 * maximum threads.
 * <p/>
 * This class may also be used to create an object representing the maximum
 * value of some number which may be atomically updated from multiple threads,
 * using the setMaximum method.  These two uses are orthagonal.
 *
 * @author Tim Boudreau
 */
public final class AtomicMaximum extends Number {

    private volatile int value;
    private final AtomicIntegerFieldUpdater<AtomicMaximum> up;
    private final AtomicInteger max = new AtomicInteger();
    private volatile boolean wasReset;

    public AtomicMaximum() {
        up = AtomicIntegerFieldUpdater.newUpdater(AtomicMaximum.class, "value");
    }

    /**
     * Get the maximum number of threads which have ever been inside the
     * run() method at the same time.
     * @return
     */
    public int getMaximum() {
        return max.get();
    }
    
    public int getAndIncrement() {
        return max.getAndIncrement();
    }

    /**
     * Gets the number of threads inside the run() method at the time
     * this method is called.
     *
     * @return the current value
     */
    public final int countActiveThreads() {
        return value;
    }

    final boolean compareAndSet(int expect, int update) {
        return up.compareAndSet(this, expect, update);
    }

    /**
     * Synchronously run the passed Runnable, updating the statistics on
     * current and total threads while the runnable runs.  This call does
     * not take any locks.
     * @param toRun
     */
    public void run (Runnable toRun) {
        enter();
        try {
            toRun.run();
        } finally {
            exit();
        }
    }
    
    /**
     * Atomically set the recorded maximum value if the passed value is 
     * greater than the current maximum value.
     * <p/>
     * Does not impact the active thread count.
     * <p/>
     * Note that the use case for this method is very different than the
     * use case of the run() method.  Use the run method to collect
     * thread-contention statistics - i.e. count how many threads are
     * inside the run method at any given time.
     * <p/>
     * Use setMaximum() to simply atomically record values and preserve
     * the maximum value ever passed to this method.  Generally, if on the
     * same AtomicMaximum object, you are calling both of these methods,
     * you are probably doing something wrong.
     * 
     * @param value A new possible maximum value
     * @return true if the passed value was greater than the recorded maximum
     * and caused the value to be changed.
     */
    public boolean setMaximum (int value) {
        for (;;) {
            int currMax = max.get();
            int current = Math.max(value, currMax);
            if (currMax == current) {
                return false;
            }
            if (max.compareAndSet(currMax, current)) {
                return value > currMax;
            }
        }
    }

    public QuietAutoCloseable enter() {
        int current = countActiveThreads();
        for (;;) {
            if (wasReset) {
                wasReset = false;
                max.lazySet(0);
                break;
            }
            current = Math.max(current, countActiveThreads());
            int next = current + 1;
            if (compareAndSet(current, next)) {
                max.lazySet(Math.max(max.get(), next));
                break;
            }
        }
        return new QuietAutoCloseable() {
            @Override
            public void close() {
                exit();
            }
        };
    }

    void exit() {
        for (;;) {
            if (wasReset) {
                wasReset = false;
                max.lazySet(0);
                break;
            }
            int current = countActiveThreads();
            int next = current - 1;
            if (compareAndSet(current, next)) {
                break;
            }
        }
    }

    @Override
    public int intValue() {
        return max.get();
    }

    @Override
    public long longValue() {
        return max.get();
    }

    @Override
    public float floatValue() {
        return max.get();
    }

    @Override
    public double doubleValue() {
        return max.get();
    }

    /**
     * Reset the historical maximum number of threads to zero.  Note that
     * this method may not take effect immediately, and zeroing the value
     * may be deferred until the next call to run().
     */
    public void reset() {
        wasReset = true;
        max.lazySet(0);
    }

    @Override
    public String toString() {
        return super.toString() + "[currentThreads=" + countActiveThreads() +
                ",maxEver=" + getMaximum() + ']';
    }
}
