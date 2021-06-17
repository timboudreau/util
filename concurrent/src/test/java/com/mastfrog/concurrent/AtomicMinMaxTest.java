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

import com.mastfrog.function.state.Int;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicMinMaxTest {

    private static final int threadCount = 6;
    private static final int entryCount = 500;
    private static int[][] ints = new int[threadCount][];
    private static int min;
    private static int max;

    @Test
    public void testMinMaxConcurrent() throws Throwable {
        AtomicMinMax minMax = new AtomicMinMax();
        Int ix = Int.create();
        ConcurrentTestHarness<R1> harn = new ConcurrentTestHarness<R1>(threadCount, () -> new R1(ints[ix.increment()], minMax));;
        harn.run();
        assertEquals(min, minMax.min());
        assertEquals(max, minMax.max());
    }

    @BeforeAll
    public static void setup() {
        Random rnd = new Random(2983892839L);
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        for (int i = 0; i < threadCount; i++) {
            int[] vals = new int[entryCount];
            ints[i] = vals;
            for (int j = 0; j < entryCount; j++) {
                int val = rnd.nextInt();
                min = Math.min(val, min);
                max = Math.max(val, max);
                vals[j] = val;
            }
        }
    }

    class R1 implements Runnable {

        private final int[] ints;
        private final IntConsumer cons;

        public R1(int[] ints, IntConsumer cons) {
            this.ints = ints;
            this.cons = cons;
        }

        @Override
        public void run() {
            for (int i = 0; i < ints.length; i++) {
                cons.accept(ints[i]);
                LockSupport.parkNanos(5);
            }
        }
    }
}
