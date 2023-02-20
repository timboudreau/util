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

import com.mastfrog.concurrent.stats.StandardLongStatistics.StandardLongStatisticsComputer;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Optional;
import java.util.function.LongConsumer;

/**
 *
 * @author Tim Boudreau
 */
public enum StandardLongStatistics implements StatisticComputation<LongConsumer, StandardLongStatisticsComputer, Long> {

    MIN,
    MAX,
    MEAN,
    SUM,
    COUNT;

    @Override
    public StandardLongStatisticsComputer map() {
        switch (this) {
            case COUNT:
                return new Count();
            case MAX:
                return new Max();
            case MEAN:
                return new Averager();
            case MIN:
                return new Min();
            case SUM:
                return new Summer();
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Optional<Long> reduce(StandardLongStatisticsComputer mapper) {
        return mapper.value();
    }

    public static abstract class StandardLongStatisticsComputer implements LongConsumer {

        abstract Optional<Long> value();
    }

    private static final class Summer extends StandardLongStatisticsComputer {

        private long sum;

        @Override
        public void accept(long value) {
            sum += value;
        }

        @Override
        Optional<Long> value() {
            return Optional.of(sum);
        }
    }

    private static final class Averager extends StandardLongStatisticsComputer {

        private long sum;
        private long count;

        @Override
        Optional<Long> value() {
            if (count == 0) {
                return Optional.empty();
            }
            double val = (double) sum / count;
            return Optional.of(Math.round(val));
        }

        @Override
        public void accept(long value) {
            count++;
            sum += value;
        }
    }

    private static final class Min extends StandardLongStatisticsComputer {

        private Long min = Long.MAX_VALUE;
        private boolean sawValues;

        @Override
        Optional<Long> value() {
            return !sawValues ? Optional.empty() : Optional.of(min);
        }

        @Override
        public void accept(long value) {
            sawValues = true;
            min = min(min, value);
        }
    }

    private static final class Max extends StandardLongStatisticsComputer {

        private Long min = Long.MIN_VALUE;
        private boolean sawValues;

        @Override
        Optional<Long> value() {
            return !sawValues ? Optional.empty() : Optional.of(min);
        }

        @Override
        public void accept(long value) {
            sawValues = true;
            min = max(min, value);
        }
    }

    private static final class Count extends StandardLongStatisticsComputer {

        private long count;

        @Override
        Optional<Long> value() {
            return Optional.of(count);
        }

        @Override
        public void accept(long value) {
            count++;
        }
    }

}
