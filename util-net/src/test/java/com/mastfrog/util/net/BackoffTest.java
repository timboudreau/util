/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.net;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntToLongFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class BackoffTest {

    @Test
    public void testFixed() {
        IntToLongFunction f = Backoff.constantDelay(10);
        for (int i = 0; i < 100; i++) {
            assertEquals(10, f.applyAsLong(i));
        }
    }

    @Test
    public void testStep() {
        IntToLongFunction f = Backoff.steppedBackoff(10, 10, 20, 30, 40, 50, 60, 70, 80);
        for (int i = 0; i < 80; i++) {
            long val = f.applyAsLong(i);
            long exp = ((i / 10) + 1) * 10;
            assertEquals(i + "", exp, val);
        }
    }

    @Test
    public void testExponential() {
        IntToLongFunction f = Backoff.exponentialBackoff(10, 0, 8200);
        for (int i = 1; i < 100; i++) {
            long val = f.applyAsLong(i);
            if (i < 29) {
                assertEquals((i * i) * 10, val);
            } else {
                assertEquals(8200, val);
            }
        }
    }

    @Test
    public void testLinear() {
        IntToLongFunction f = Backoff.linearBackoff(10, 0, 8200);
        for (int i = 1; i < 100; i++) {
            assertEquals(i * 10, f.applyAsLong(i));
        }
    }

    @Test
    public void testAsync() throws Throwable {
        Backoff backoff = new Backoff(Backoff.steppedBackoff(2, 50, 100, 200, 300));
        long[] timings = new long[9];
        Arrays.fill(timings, -1);
        ScheduledExecutorService svc = Executors.newScheduledThreadPool(1);
        C c = new C(timings);
        CountDownLatch latch = new CountDownLatch(1);
        String[] got = new String[1];
        BiConsumer<Throwable, String> bic = (Throwable th, String str) -> {
            if (th == null) {
                got[0] = str;
            } else {
                System.out.println(th.getMessage());
            }
            latch.countDown();
        };
        backoff.asyncBackoff(c, bic, svc);
        latch.await(60, TimeUnit.SECONDS);
        assertNotNull(got[0]);
        assertEquals("Hello", got[0]);
        // XXX if this randomly fails, we may need to explicitly configure
        // the garbage collector for test runs
        long[] expected = new long[]{5, 50, 50, 100, 100, 200, 200, 300, 300};
        assertArraysNearlyEqual(30, expected, timings);
    }

    private void assertArraysNearlyEqual(int jitter, long[] exp, long[] got) {
        assertEquals(exp.length, got.length);
        for (int i = 0; i < exp.length; i++) {
            if (i == 0) {
                // Initial invocation may not
                // be within tolerance without running a bunch of warmup
                // rounds
                continue;
            }
            long expMin = exp[i] - (jitter / 2);
            long expMax = expMin + jitter;
            assertTrue("Expected " + exp[i] + " +/- " + (jitter / 2)
                    + " but got " + got[i] + " which is not >= " + expMin
                    + " and <= " + expMax + " in " + Arrays.toString(got),
                    got[i] >= expMin && got[i] <= expMax);
        }
    }

    static class C implements Consumer<BiConsumer<Throwable, String>> {

        private final long[] timings;
        private int count;
        long last = System.currentTimeMillis();

        public C(long[] timings) {
            this.timings = timings;
        }

        @Override
        public void accept(BiConsumer<Throwable, String> t) {
            ++count;
            long oldLast = last;
            timings[count - 1] = (last = System.currentTimeMillis()) - oldLast;
            if (count == timings.length) {
                t.accept(null, "Hello");
            } else {
                t.accept(new IllegalStateException("Iter " + count), null);
            }
        }
    }
}
