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

import com.mastfrog.function.state.Bool;
import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Random;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class ConcurrentLongStatsTest {

    @Test
    public void testBasicStats() {
        Random r = new Random(658121039204L);
        int count = 1000;
        ConcurrentLongStats stats = new ConcurrentLongStats(count);

        for (int i = 0; i < 1000; i++) {
            long val = r.nextLong();
            if (val != 0) {
                stats.add(abs(val));
            }
        }

        LongSummaryStatistics sum = stats.toStatistics();

        Map<StatisticComputation<LongConsumer, ? extends LongConsumer, Long>, Long> results
                = stats.compute(asList(StandardLongStatistics.values()));

        for (StandardLongStatistics sl : StandardLongStatistics.values()) {
            Long value = results.get(sl);
            assertNotNull(value, "No value for " + sl);

            long expected;
            switch (sl) {
                case COUNT:
                    expected = sum.getCount();
                    break;
                case MAX:
                    expected = sum.getMax();
                    break;
                case MIN:
                    expected = sum.getMin();
                    break;
                case SUM:
                    expected = sum.getSum();
                    break;
                case MEAN:
                    expected = (long) sum.getAverage();
                    break;
                default:
                    throw new AssertionError(sl);
            }
            assertEquals(expected, value.longValue(), () -> "Wrong value for " + sl);
        }

    }

    @Test
    public void testLongStats() {

        Random r = new Random(51039204L);
        int count = 20;
        LongSummaryStatistics is = new LongSummaryStatistics();
        ConcurrentLongStats stats = new ConcurrentLongStats(count);

        Runnable checkIt = () -> {
            assertStatsEquals(stats, is);
        };

        assertEquals(count, stats.capacity());
        assertTrue(stats.isEmpty());
        assertEquals(0, stats.sampleCount());

        for (int i = 0; i < count; i++) {
            assertEquals(stats.sampleCount(), i);
            checkIt.run();
            int val = r.nextInt(100);
            int statsRes = stats.addValue(val);
            assertEquals(i, statsRes, "Wrong position returned for sample " + i);
            is.accept(val);
        }
        checkIt.run();

        for (int i = 0; i < count * 2; i++) {
            int val = r.nextInt(100);
            int statsRes = stats.addValue(val);
            assertTrue(statsRes >= 0 && statsRes < count, "Invalid position " + count);
        }

        stats.reset();
        assertEquals(0, stats.sampleCount());
        Bool called = Bool.create();
        assertEquals(0, stats.forEach(ignored -> {
            called.set();
        }));
        assertFalse(called.getAsBoolean());

        stats.add(1);
        assertEquals(1, stats.sampleCount());
        stats.add(1);
        assertEquals(2, stats.sampleCount());
        stats.add(1);
        assertEquals(1D, stats.average(), 0.1);
        assertEquals(3, stats.forEach(ignored -> {
            called.set();
        }));
        assertTrue(called.getAsBoolean());
    }

    private void assertStatsEquals(ConcurrentLongStats stats, LongSummaryStatistics a) {
        LongSummaryStatistics b = stats.toStatistics();
        assertEquals(a.getCount(), b.getCount(), "Counts differ between " + a + " and " + b);
        assertEquals(a.getSum(), b.getSum(), "Sums differ between " + a + " and " + b);
        assertEquals(a.getMin(), b.getMin(), "Mins differ between " + a + " and " + b);
        assertEquals(a.getMax(), b.getMax(), "Maxes differ between " + a + " and " + b);
        assertEquals(a.getAverage(), b.getAverage(), 0.01, "Averages differ between " + a + " and " + b);
        assertEquals(stats.sampleCount(), a.getCount());
        boolean called = stats.withStats((min, max, sum, count) -> {
            assertEquals(a.getMin(), min, "Internally computed min differs");
            assertEquals(a.getMax(), max, "Internally computed max differs");
            assertEquals(a.getSum(), sum, "Internally comouted sum differs");
            assertEquals(a.getCount(), count, "Internally comouted count differs");
        });
        assertEquals(called, !stats.isEmpty());
    }
}
