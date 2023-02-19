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

import com.mastfrog.function.LongQuadConsumer;
import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.Collection;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Subtype of StatisticCollector for integers.
 *
 * @author Tim Boudreau
 */
public interface LongStatisticCollector extends StatisticCollector<LongConsumer, LongQuadConsumer>, LongConsumer, IntConsumer {

    /**
     * Convenience addition method.
     *
     * @param value The value to add
     * @return this
     */
    default LongStatisticCollector add(long value) {
        this.accept(value);
        return this;
    }

    /**
     * Create an <i>concurrent</i> integer statistic collector.
     *
     * @param samples
     * @return A new collector
     */
    static LongStatisticCollector create(int samples) {
        return new ConcurrentLongStats(samples);
    }

    /**
     * Convert this instance to an LongSummaryStatistics which snapshots the
     * current state.
     *
     * @return An IntSummaryStatistics
     */
    LongSummaryStatistics toStatistics();

    @Override
    public default LongConsumer aggregateValueConsumers(Collection<? extends LongConsumer> group) {
        LongConsumer result = null;
        for (LongConsumer c : group) {
            if (result == null) {
                result = c;
            } else {
                result = result.andThen(c);
            }
        }
        return result == null ? val -> {
        } : result;
    }

    /**
     * Wrap this statistic collection in one which ignores some percentage of
     * samples.
     *
     * @param rnd A random
     * @param oneIn The number there should be a one -in-what chance of the
     * sample being added for
     * @return a wrapper statistic collector
     */
    default LongStatisticCollector intermittentlySampling(Random rnd, int oneIn) {
        Checks.greaterThanZero("oneIn", oneIn);
        return intermittentlySampling(() -> {
            return rnd.nextInt(oneIn) == 1;
        });
    }

    default LongStatisticCollector time(TimeUnit unit, Runnable r) {
        try {
            return timeThrowing(unit, true, () -> r.run());
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
    }

    default LongStatisticCollector timeThrowing(TimeUnit unit, boolean recordFailed, ThrowingRunnable r) throws Exception {
        boolean failed = false;
        boolean nanos = unit == TimeUnit.MICROSECONDS || unit == TimeUnit.NANOSECONDS;
        long then = nanos ? System.nanoTime() : System.currentTimeMillis();
        try {
            r.run();
            return this;
        } catch (Exception | Error ex) {
            failed = true;
            return Exceptions.chuck(ex);
        } finally {
            if (!failed || recordFailed) {
                long now = nanos ? System.nanoTime() : System.currentTimeMillis();
                long elapsed = now - then;
                long value = unit.convert(elapsed, nanos ? TimeUnit.NANOSECONDS : TimeUnit.MILLISECONDS);
                accept(value);
            }
        }
    }

    /**
     * Wrap this statistic collection in one which ignores some of samples when
     * the passed BooleanSupplier does not return true. Useful for cases where
     * there are cyclic patterns in what is being requested that can skew
     * results, and randomizing what is sampled will avoid statistics bouncing
     * between two modes.
     *
     * @param shouldSample A supplier that can determine whether or not to
     * sample
     * @return A wrapper LongStatisticCollector
     */
    default LongStatisticCollector intermittentlySampling(BooleanSupplier shouldSample) {
        return new LongStatisticCollector() {
            @Override
            public boolean isEmpty() {
                return LongStatisticCollector.this.isEmpty();
            }

            @Override
            public int capacity() {
                return LongStatisticCollector.this.capacity();
            }

            @Override
            public void reset() {
                LongStatisticCollector.this.reset();
            }

            @Override
            public double average() {
                return LongStatisticCollector.this.average();
            }

            @Override
            public int forEach(LongConsumer consumer) {
                return LongStatisticCollector.this.forEach(consumer);
            }

            @Override
            public boolean withStats(LongQuadConsumer c) {
                return LongStatisticCollector.this.withStats(c);
            }

            @Override
            public boolean withStatsAndValues(LongConsumer valueVisitor, LongQuadConsumer statsConsumer) {
                return LongStatisticCollector.this.withStatsAndValues(valueVisitor, statsConsumer);
            }

            @Override
            public boolean median(LongConsumer c) {
                return LongStatisticCollector.this.median(c);
            }

            @Override
            public void accept(long value) {
                if (shouldSample.getAsBoolean()) {
                    LongStatisticCollector.this.accept(value);
                }
            }

            @Override
            public void accept(int value) {
                this.accept((long) value);
            }

            @Override
            public LongStatisticCollector add(long value) {
                this.accept(value);
                return this;
            }

            @Override
            public LongSummaryStatistics toStatistics() {
                return LongStatisticCollector.this.toStatistics();
            }

            @Override
            public String toString() {
                return LongStatisticCollector.this.toString() + " @ " + shouldSample;
            }
        };
    }
}
