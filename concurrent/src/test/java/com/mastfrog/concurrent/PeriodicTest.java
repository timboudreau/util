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
package com.mastfrog.concurrent;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class PeriodicTest {

    @Test
    public void testPeriodicConcurrent() throws Throwable {
        int threadCount = 4;
        long interval = 120;
        long fuzz = 15;
        Periodic p = new Periodic(interval);

        CopyOnWriteArrayList<Long> cowal = new CopyOnWriteArrayList<>();
        long start = System.currentTimeMillis();
        long stop = start + 2000;
        Runnable r = () -> {
            long now;
            while ((now = System.currentTimeMillis()) < stop) {
                if (p.getAsBoolean()) {
                    cowal.add(now);
                }
                LockSupport.parkNanos(100);
            }
        };

        ConcurrentTestHarness<Runnable> harn = new ConcurrentTestHarness<>(threadCount, r);
        harn.run();

        for (int i = 1; i < cowal.size(); i++) {
            long prev = cowal.get(i - 1);
            long curr = cowal.get(i);
            long diff = curr - prev;
            assertFuzzy(interval, diff, fuzz);
        }
    }

    private void assertFuzzy(long expect, long val, long tolerance) {
        long diff = Math.abs(val - expect);
        assertTrue(diff <= tolerance, "Value " + val + " outside of tolerance "
                + tolerance + ". Difference: " + diff + "; expected " + expect);
    }

}
