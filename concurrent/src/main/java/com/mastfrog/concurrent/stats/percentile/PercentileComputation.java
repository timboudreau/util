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

import java.util.Optional;
import java.util.function.IntToLongFunction;

/**
 * A method of computing a percentile value from a series. Two built-in ones
 * are supported, <code>INTERPOLATED</code> and <code>NEAREST</code>.
 * Otherse, such as spline interpolation, are possible.
 */
public interface PercentileComputation {

    public static PercentileComputation NEAREST = Percentiles::nearestPercentile;
    public static PercentileComputation INTERPOLATED = Percentiles::interpolated;

    /**
     * Returns the percentile value of the passed <b>PRE-SORTED</b> series
     * according to the rules of this computation.
     *
     * @param percentile The percentile - must be &lt;=1 and &gt;0.
     * @param seriesSize The size of the series - the maximum integer value
     * that can be passed to the passed <code>itemLookup</code> function to
     * look up a value for an index.
     * @param itemLookup Looks up values in the backing storage
     * @return The percentile value, if one can be computed
     */
    public Optional<Long> value(double percentile, int seriesSize, IntToLongFunction itemLookup);

    default Percentiles newPercentiles(int sizeHint) {
        return new Percentiles(this, sizeHint);
    }

    default Percentiles newPercentiles(LongStore store) {
        return new Percentiles(this, store);
    }

    default Percentiles newPercentiles(long[] store) {
        return new Percentiles(this, store);
    }

    default Percentiles newPercentiles() {
        return new Percentiles(this);
    }
    
}
