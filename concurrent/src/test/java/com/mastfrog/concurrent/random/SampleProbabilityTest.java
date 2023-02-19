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
package com.mastfrog.concurrent.random;

import static java.lang.Math.abs;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
 
/**
 *
 * @author Tim Boudreau
 */
public class SampleProbabilityTest {

    @Test
    public void testRandomlySampleWithAdjustment() {
        // Test that the percentages of true vs false emitted by a RandomlySample
        // with a given probability matches the target given enough samples
        for (SampleProbability a : SampleProbability.values()) {
            // Use a specific random or we could have a randomly failing test
            Random rnd = new Random(91232452393L);
            SampleProbability.RandomlySample smp = new SampleProbability.RandomlySample(120, rnd, a);
            int max = 1000000;
            int ct = 0;
            for (int j = 0; j < max; j++) {
                if (smp.getAsBoolean()) {
                    ct++;
                }
            }
            double got = (double) ct / max;
            double expected = a.targetProbability();
            double delta = abs(got - expected);
            assertEquals(expected, got, .0008, () -> "Wrong result for " + a.name() + " (" + a + ") delta " + delta);
        }
    }

    @Test
    public void testRandomlySample() {
        // Test default 50/50 randomness
        SampleProbability.RandomlySample rs = new SampleProbability.RandomlySample(20, new Random(1));
        boolean[] as = new boolean[20];
        for (int i = 0; i < 20; i++) {
            as[i] = rs.getAsBoolean();
        }
        // Ensure we don't get the same array with no reset (we are using
        // a random seed where that won't accidentally happen - so if it does,
        // the array was not reinitialized on wrap around.
        boolean[] bs = new boolean[20];
        for (int i = 0; i < 20; i++) {
            bs[i] = rs.getAsBoolean();
        }
        assertFalse(Arrays.equals(as, bs));

        int max = 100000;
        int ct = 0;
        for (int i = 0; i < max; i++) {
            if (rs.getAsBoolean()) {
                ct++;
            }
        }
        assertTrue(ct > max * 0.4, "Too low: Should be effectively a coin "
                + "toss, but got " + ((double) ct / (double) max));
        assertTrue(ct < max * 0.6, "Too high: Should be effectively a coin "
                + "toss, but got " + ((double) ct / (double) max));
    }
}