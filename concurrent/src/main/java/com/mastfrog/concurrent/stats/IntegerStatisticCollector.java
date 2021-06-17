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
import com.mastfrog.util.preconditions.Checks;
import java.util.IntSummaryStatistics;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Subtype of StatisticCollector for integers.
 *
 * @author Tim Boudreau
 */
public interface IntegerStatisticCollector extends StatisticCollector<IntConsumer, IntQuadConsumer>, IntConsumer {

    /**
     * Convenience addition method.
     *
     * @param value The value to add
     * @return this
     */
    default IntegerStatisticCollector add(int value) {
        this.accept(value);
        return this;
    }

    /**
     * Convert this instance to an IntSummaryStatistics which snapshots the
     * current state.
     *
     * @return An IntSummaryStatistics
     */
    IntSummaryStatistics toStatistics();

    /**
     * Create an <i>concurrent</i> integer statistic collector.
     *
     * @param samples
     * @return
     */
    static IntegerStatisticCollector create(int samples) {
        return new ConcurrentIntegerStats(samples);
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
    default IntegerStatisticCollector intermittentlySampling(Random rnd, int oneIn) {
        Checks.greaterThanZero("oneIn", oneIn);
        return intermittentlySampling(() -> {
            return rnd.nextInt(oneIn) == 1;
        });
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
    default IntegerStatisticCollector intermittentlySampling(BooleanSupplier shouldSample) {
        return new IntegerStatisticCollector() {
            @Override
            public boolean isEmpty() {
                return IntegerStatisticCollector.this.isEmpty();
            }

            @Override
            public int capacity() {
                return IntegerStatisticCollector.this.capacity();
            }

            @Override
            public void reset() {
                IntegerStatisticCollector.this.reset();
            }

            @Override
            public double average() {
                return IntegerStatisticCollector.this.average();
            }

            @Override
            public int forEach(IntConsumer consumer) {
                return IntegerStatisticCollector.this.forEach(consumer);
            }

            @Override
            public boolean withStats(IntQuadConsumer c) {
                return IntegerStatisticCollector.this.withStats(c);
            }

            @Override
            public boolean withStatsAndValues(IntConsumer valueVisitor, IntQuadConsumer statsConsumer) {
                return IntegerStatisticCollector.this.withStatsAndValues(valueVisitor, statsConsumer);
            }

            @Override
            public boolean median(IntConsumer c) {
                return IntegerStatisticCollector.this.median(c);
            }

            @Override
            public void accept(int value) {
                if (shouldSample.getAsBoolean()) {
                    IntegerStatisticCollector.this.accept(value);
                }
            }

            @Override
            public IntegerStatisticCollector add(int value) {
                this.accept(value);
                return this;
            }

            @Override
            public IntSummaryStatistics toStatistics() {
                return IntegerStatisticCollector.this.toStatistics();
            }
        };
    }
}
