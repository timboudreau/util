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

import com.mastfrog.concurrent.stats.LongStatisticCollector;
import com.mastfrog.concurrent.stats.StatisticComputation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntToLongFunction;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class PercentilesTest {

    enum PM implements PercentileComputation {
        NEAREST,
        INTERPOLATED;

        @Override
        public Optional<Long> value(double percentile, int seriesSize, IntToLongFunction itemLookup) {
            switch (this) {
                case NEAREST:
                    return Percentiles.nearestPercentile(percentile, seriesSize, itemLookup);
                case INTERPOLATED:
                    return Percentiles.interpolated(percentile, seriesSize, itemLookup);
                default:
                    throw new AssertionError();
            }
        }
    }

    @Test
    public void testInterpolationAlgorithms() {
        // Test implementation where the number of elements is larger than
        // we need
        LongArrayStore list = new LongArrayStore(200);

        LongStatisticCollector coll = LongStatisticCollector.create(200);

        for (int i = 0; i < 200; i++) {
            coll.accept((i + 1) * 100);
        }

        for (PM m : PM.values()) {

            list.clear();
            Percentiles pct = m.newPercentiles(list);

            StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p90Factory = pct.p90();
            StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p10Factory = pct.p10();
            StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p99Factory = pct.p99();
            StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p926Factory = pct.percentile(0.926);
            StatisticComputation<LongConsumer, PercentileStatisticComputation, Long> p962Factory = pct.percentile(0.962);

            Map<StatisticComputation<LongConsumer, ? extends LongConsumer, Long>, Long> collected = coll.compute(Arrays.asList(p90Factory, p10Factory, p99Factory, p926Factory, p962Factory));

            double gotP90, gotP99, gotP10, gotP926, gotP962;

            gotP90 = collected.get(p90Factory);
            gotP99 = collected.get(p99Factory);
            gotP10 = collected.get(p10Factory);
            gotP926 = collected.get(p926Factory);
            gotP962 = collected.get(p962Factory);

            // We test a few definitely-in-between values to ensure we aren't
            // being fooled by always picking up the exact value
            double expectedP90, expectedP99, expectedP10, expectedP926, expectedP962;
            switch (m) {
                case NEAREST:
                    expectedP90 = 18000;
                    expectedP99 = 19800;
                    expectedP10 = 2000;
                    expectedP926 = 18500;
                    expectedP962 = 19200;
                    break;
                case INTERPOLATED:
                    expectedP90 = 18000;
                    expectedP99 = 19800;
                    expectedP10 = 2000;
                    expectedP926 = 18540;
                    expectedP962 = 19230;
                    break;
                default:
                    throw new AssertionError(m);
            }

            assertEquals(expectedP90, gotP90, "p90");
            assertEquals(expectedP99, gotP99, "p99");
            assertEquals(expectedP10, gotP10, "p10");
            assertEquals(expectedP926, gotP926, "p926");
            assertEquals(expectedP962, gotP962, "p962");
        }
    }

}
