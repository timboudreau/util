/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent.stats.percentile;

import com.mastfrog.concurrent.stats.StatisticComputation;
import java.util.Optional;
import java.util.function.IntToLongFunction;
import java.util.function.LongConsumer;

/**
 * Factory for StatisticComputation implementations that compute percentile
 * values for a series, sharing series data across all instances it produces (so
 * if you have a million stats buckets in your LongStatisticCollector, you don't
 * wind up with as many long[1_000_000] as you want percentiles).
 * <p>
 * Use PercentileComputation.newPercentiles() to get an instance.
 * </p>
 * <b>Important:</b> Percentiles instances are "one and done" - create one, pass
 * some percentile computations to a StatisticCollector, read the values and
 * throw it away. Since each StatisticComputation produced by one of these is
 * itself a consumer of values, only the <i>first</i> instance created will
 * actually write to the backing LongStore - all instances must be used.
 *
 * @author Tim Boudreau
 */
public final class Percentiles {

    private static final double THRESHOLD = 0.001;
    private final PercentileComputation computationMethod;
    private final LongStore store;
    private int count;

    Percentiles(PercentileComputation computationMethod) {
        this(computationMethod, (LongStore) null);
    }

    Percentiles(PercentileComputation computationMethod, long[] valuesDest) {
        this(computationMethod, valuesDest == null ? null : new LongArrayStore(valuesDest));
    }

    Percentiles(PercentileComputation computationMethod, int sizeHint) {
        this(computationMethod, new LongArrayStore(sizeHint));
    }

    Percentiles(PercentileComputation computationMethod, LongStore store) {
        this.computationMethod = computationMethod;
        this.store = store == null ? new LongArrayStore(2048) : store;
    }

    /**
     * Get a computation for a particular percentile.
     *
     * @param percentile A percecntile, which must be &gt; 0 and &lt;=1
     * @return A computation
     */
    public StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> percentile(double percentile) {
        if (percentile <= 0 || percentile > 1) {
            throw new IllegalArgumentException("Percentile must be >= 0 and <= 1");
        }
        return new PercentileHandler(percentile);
    }

    /**
     * <code>percentile(0.5)</code>
     *
     * @return A computation
     */
    public StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> median() {
        return percentile(0.5);
    }

    /**
     * <code>percentile(0.9)</code>
     *
     * @return A computation
     */
    public StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p90() {
        return percentile(0.9);
    }

    /**
     * <code>percentile(0.99)</code>
     *
     * @return A computation
     */
    public StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p99() {
        return percentile(0.99);
    }

    /**
     * <code>percentile(0.1)</code>
     *
     * @return A computation
     */
    public StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p10() {
        return percentile(0.1);
    }

    private class PercentileHandler implements StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> {

        private final double percentile;

        PercentileHandler(double percentile) {
            this.percentile = percentile;
        }

        @Override
        public PercentileStatisticComputation map() {
            return new PercentileStatisticComputation(percentile, count++ == 0, store);
        }

        @Override
        public Optional<Long> reduce(PercentileStatisticComputation mapper) {
            return mapper.result(computationMethod);
        }

        @Override
        public String toString() {
            return "Percentile(" + percentile + ")";
        }
    }


    static Optional<Long> nearestPercentile(double percentile, int size, IntToLongFunction series) {
        switch (size) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(series.applyAsLong(0));
            default:
                int index = (int) Math.round(size * percentile);
                return Optional.of(series.applyAsLong(index - 1));
        }
    }

    static Optional<Long> interpolated(double percentile, int size, IntToLongFunction series) {
        if (size <= 1) {
            return Optional.empty();
        }
        double ix = size * percentile;
        double rounded = Math.round(ix);
        double delta = Math.abs(ix - rounded);
        if (delta < THRESHOLD) {
            return Optional.of(series.applyAsLong((int) (rounded - 1)));
        }
        int ixlow = (int) Math.floor(ix) - 1;
        int ixhigh = (int) Math.ceil(ix) - 1;
        /*
        Say we have a value whose logical index is 92.3.
        An average would be vals[92] + vals[93] / 2.
        
        What we want to do here is to use a little more of the high value
        and a little less of the low value.
        
        So, ((vals[93] * 1.3) + (vals[92] * 0.7)) / 2.
        
        If it were reversed, and we had a logical index of 92.7,
        then we would want
        
        (vals[92] * 1.3) + (vals[93] * 0.7).
         */
        double weightHigh;
        double weightLow;
        // Find the midpoint between the two nearest, e.g. 92.5
        double midpoint = ixlow + 0.5;

        if (ix - 1 == midpoint) {
            // If we are exactly at the midpoint, use the average
            return Optional.of((series.applyAsLong(ixlow) + series.applyAsLong(ixhigh)) / 2);
        } else if (ix - 1 > midpoint) {
            // If ix > midpoint, then weight high is 1 + (1 - (ix - floor(ix))
            weightHigh = 1 + (1 - (ix - Math.floor(ix)));
            // and weight low is 2 - weight high
            weightLow = 2 - weightHigh;
        } else {
            // if ix < midpoint then weight high = 1 - (ix - floor(ix))
            weightHigh = 1 - (ix - Math.floor(ix));
            // and weight low is 1 + (1 - weight high)
            weightLow = 1 + (1 - weightHigh);
        }
        long origLow = series.applyAsLong(ixlow);
        long origHigh = series.applyAsLong(ixhigh);

        double valLow = origLow * weightLow;
        double valHigh = origHigh * weightHigh;

        double result = (valLow + valHigh) / 2D;
        return Optional.of(Math.round(result));
    }

}
