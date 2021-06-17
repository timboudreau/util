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

/**
 * Collects and can compute statistics; unlike the JDK's IntSummaryStatistics
 * and friends, those in this package are
 * <li>
 * <ul>Concurrent and non-blocking, using atomics</ul>
 * <ul>Use a finite number of samples in what is effectively a ring-buffer</ul>
 * </li>
 * All methods which read statistics should be called bearing in mind that the
 * state may be modified concurrently - you are getting a snapshot of the state
 * at some point during your call. Use the <code>withStatsAndValues()</code> to
 * capture the internal state and averages, min, max, etc. from the same
 * internal state.
 *
 * @author Tim Boudreau
 */
public interface StatisticCollector<ValueConsumer, StatsConsumer> {

    /**
     * If true, no statistics have been collected yet, or the instance has been
     * reset.
     *
     * @return
     */
    boolean isEmpty();

    /**
     * Get the capacity of the backing ring-buffer - the number of data points
     * that can be collected before wrap-around.
     *
     * @return The capacity
     */
    int capacity();

    /**
     * Reset the internal state, discarding all samples, as if the instance were
     * newly created.
     */
    void reset();

    /**
     * Get the current average value; returns 0 if no samples.
     *
     * @return The avrage
     */
    double average();

    /**
     * Visit each retained value.
     *
     * @param consumer A consumer
     * @return The number of times the consumer was called
     */
    int forEach(ValueConsumer consumer);

    /**
     * Get aggregate statistics about the current set of samples; the passed
     * four-argument consumer is passed the minimum, maximum, sum and count of
     * values; if no samples are present, the consumer will not be called and
     * false is returned.
     *
     * @param c A consumer of some sort, defined by the concrete implementation
     * type
     * @return true if the consumer was called - if there as some data to
     * compute statistics over
     */
    boolean withStats(StatsConsumer c);

    /**
     * Get aggregate statistics about the current set of samples, <i>and</i>
     * visit each value. The passed four-argument consumer is passed the
     * minimum, maximum, sum and count of values; if no samples are present, the
     * consumer will not be called and false is returned. The passed
     * single-argument consumer is called (before the stats consumer) with each
     * value currently sampled.
     * <p>
     * This method guarantees that the set of samples and the computed
     * statistics are consistent, from a single snapshot in time of the internal
     * state of the consumer.
     *
     * @param c A consumer of some sort, defined by the concrete implementation
     * type
     * @return true if the consumer was called - if there as some data to
     * compute statistics over
     */
    boolean withStatsAndValues(ValueConsumer valueVisitor, StatsConsumer statsConsumer);

    /**
     * Fetch the median value, if there is one; if there are less than three
     * samples, the passed consumer will not be called and false will be
     * returned.
     *
     * @param c A consumer
     * @return true if a median value exists and the consumer was called
     */
    boolean median(ValueConsumer c);
}
