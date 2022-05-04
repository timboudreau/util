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
package com.mastfrog.concurrent.stats;

import com.mastfrog.function.IntQuadConsumer;
import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Int;
import static com.mastfrog.util.preconditions.Checks.greaterThanOne;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntConsumer;

/**
 * Lockless, concurrent statitics which uses a fixed number of samples to
 * maintain statistics.
 *
 * @author Tim Boudreau
 */
final class ConcurrentIntegerStats implements IntConsumer, StatisticCollector<IntConsumer, IntQuadConsumer>, IntegerStatisticCollector {

    private final AtomicIntegerArray arr;
    private final AtomicInteger tail = new AtomicInteger();

    ConcurrentIntegerStats(int ringSize) {
        arr = new AtomicIntegerArray(greaterThanOne("ringSize", ringSize));
    }

    public ConcurrentIntegerStats copy() {
        ConcurrentIntegerStats result = new ConcurrentIntegerStats(arr.length());
        result.tail.set(tail.get());
        for (int i = 0; i < capacity(); i++) {
            result.arr.set(i, arr.get(i));
        }
        return result;
    }

    /**
     * Get the average value across all occupied cells at the time of this call.
     *
     * @return An average
     */
    @Override
    public double average() {
        Dbl dbl = Dbl.create();
        double count = forEach(val -> dbl.add(val));
        return count > 0 ? dbl.getAsDouble() / count : 0;
    }

    /**
     * Get the median value across all occupied cells at the time of this call.
     * If not enough values are present for a median to be computed (less than
     * 3), returns false and does not call the passed consumer.
     *
     * @return The median
     */
    @Override
    public boolean median(IntConsumer c) {
        int count = sampleCount();
        if (count < 3) {
            return false;
        }
        int[] vals = new int[sampleCount()];
        Int cursor = Int.create();
        forEach(val -> {
            // We can visit more values than we allocated for if the
            // data changes between allocating and running this loop -
            // that can and does happen
            int ct = cursor.increment(1);
            if (ct < vals.length) {
                vals[ct] = val;
            }
        });
        Arrays.sort(vals);
        int mid = vals[vals.length / 2];
        c.accept(mid);
        return true;
    }

    /**
     * Get the total number of samples this instance internally maintains.
     *
     * @return The capacity, > 1
     */
    @Override
    public int capacity() {
        return arr.length();
    }

    /**
     * Returns true of no samples have been collected or this instance has been
     * reset since the last sample was added.
     *
     * @return True if no samples are present
     */
    @Override
    public boolean isEmpty() {
        return tail.get() == 0;
    }

    /**
     * Reset the sample count to zero, not including old samples in any output
     * data.
     */
    @Override
    public void reset() {
        tail.set(0);
    }

    /**
     * Convert this instance to an IntSummaryStatistics which snapshots the
     * current state.
     *
     * @return An IntSummaryStatistics
     */
    public IntSummaryStatistics toStatistics() {
        IntSummaryStatistics result = new IntSummaryStatistics();
        forEach(result);
        return result;
    }

    /**
     * Get the number of samples currently used for statistical computation.
     *
     * @return The sumple count
     */
    public int sampleCount() {
        return Math.abs(tail.get());
    }

    private int next() {
        // We start out with a size of zero by setting the tail to zero
        //
        // While we have fewer than arr.length() samples, we *decrement* the
        // tail pointer each time - so, say, -5 means we have five samples
        // and arr[5] will be the next one written.
        //
        // Once we pass -arr.length(), we switch to positive numbers, since we
        // will henceforth have a complete set of samples, and the tail poionts
        // to the next index to write (i.e. 1 through count, inclusive).
        int count = arr.length();
        int resultBase = tail.updateAndGet(old -> {
            if (old == 0) {
                return -1;
            } else if (old < 0) {
                int next = old - 1;
                if (next < -count) {
                    return 1;
                }
                return next;
            } else {
                int next = old + 1;
                if (next >= count + 1) {
                    return 1;
                }
                return next;
            }
        });
        return Math.abs(resultBase) - 1;
    }

    /**
     * Add a new sample to this withStats.
     *
     * @param val The value
     * @return The index of the cell that was written
     */
    int addValue(int val) {
        int result;
        arr.set(result = next(), val);
        return result;
    }

    @Override
    public int forEach(IntConsumer consumer) {
        int t = tail.get();
        if (t == 0) {
            return 0;
        }
        if (t < 0) {
            for (int i = -1; i >= t; i--) {
                int ix = -i - 1;
                consumer.accept(arr.get(ix));
            }
            return -t;
        } else {
            int len = arr.length();
            for (int i = 0; i < len; i++) {
                consumer.accept(arr.get(i));
            }
            return len;
        }
    }

    /**
     * Collect basic statistical info, all computed from the same internal
     * state.
     *
     * @param c A consumer which will be passed the min, max, sum and count in
     * that order
     * @return True if any withStats were present and the consumer was called
     */
    @Override
    public boolean withStats(IntQuadConsumer c) {
        return withStatsAndValues(null, c);
    }

    /**
     * Allows us to collect statistics and values in a single pass, knowing the
     * data has not changed between computing the withStats and visiting the
     * values.
     *
     * @param statsConsumer Consumes min, max, sum, count
     * @param valueVisitor is passed individual values as the withStats are
     * being computed
     * @return true if any values were visited and the consumer was called; the
     * consumer is not called for an empty withStats.
     */
    @Override
    public boolean withStatsAndValues(IntConsumer valueVisitor, IntQuadConsumer statsConsumer) {
        Int min = Int.of(Integer.MAX_VALUE);
        Int max = Int.of(Integer.MIN_VALUE);
        Int sum = Int.create();
        Int callCount = Int.create();
        int count = forEach(val -> {
            min.min(val);
            max.max(val);
            sum.incrementSafe(val);
            callCount.increment();
            if (valueVisitor != null) {
                valueVisitor.accept(val);
            }
        });
        boolean result = count != 0;
        if (result) {
            statsConsumer.accept(min.getAsInt(), max.getAsInt(), sum.getAsInt(), callCount.get());
        }
        return result;
    }

    @Override
    public void accept(int value) {
        this.addValue(value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("stats{");
        boolean any = withStatsAndValues(val -> {
            sb.append(' ').append(val);
        }, (min, max, sum, count) -> {
            sb.append(" min=").append(min)
                    .append(" max=").append(max)
                    .append(" sum=").append(sum)
                    .append(" count=").append(count);
        });
        if (!any) {
            sb.append("-empty-");
        }
        return sb.append('}').toString();
    }
}
