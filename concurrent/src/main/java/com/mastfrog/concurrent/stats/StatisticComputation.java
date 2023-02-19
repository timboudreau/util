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
package com.mastfrog.concurrent.stats;

import java.util.Optional;

/**
 * Computes a statistical value from the set of values in a StatisticCollector.
 * This uses a map-reduce like paradigm - emit a collector, which is passed each
 * value in a series, which is then passed back to the <code>reduce()</code>
 * method to obtain the resulting value.
 * <p>
 * Multiple instances of <code>StatisticComputation</code> may be passed to
 * StatisticCollector.compute() to compute a collection of statistical metrics
 * over the same snapshot of the collector's data.
 * </p>
 *
 * @see {@link com.mastfrog.concurrent.stats.StatisticCollector.compute}
 * @author Tim Boudreau
 */
public interface StatisticComputation<ValueConsumer, C extends ValueConsumer, N extends Number> {

    /**
     * Create a new consumer such as an IntConsumer or LongConsumer which will
     * passed each value in a series and can emit a result when passed back to
     * the <code>reduce()</code> method afterwards.
     *
     * @return A consumer
     */
    C map();

    /**
     * Takes a mapper of the specific type returned by this computation, and
     * completes the computation.
     *
     * @param mapper A mapper which was provided by the <code>map()</code>
     * method
     * @return The result of computation
     */
    Optional<N> reduce(C mapper);
}
