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
import java.util.function.LongConsumer;

/**
 * Implementation of LongConsumer used by Percentiles.
 */
public final class PercentileStatisticComputation implements LongConsumer {

    private final boolean acceptValues;
    private final LongStore store;
    private final double percentile;

    PercentileStatisticComputation(double percentile, boolean acceptValues, LongStore store) {
        this.percentile = percentile;
        this.acceptValues = acceptValues;
        this.store = store;
    }

    @Override
    public void accept(long value) {
        if (acceptValues) {
            store.accept(value);
        }
    }

    Optional<Long> result(PercentileComputation comp) {
        store.sort();
        return comp.value(percentile, store.size(), store);
    }

}
