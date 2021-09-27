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
package com.mastfrog.bits.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class BitsIterableTest {

    @Test
    public void testLong() {
        long val = 0;
        List<Integer> ints = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (i % 3 == 0) {
                val |= 1L << i;
                ints.add(i);
            }
        }
        List<Integer> got = new ArrayList<>();
        for (Integer i : BitsIterable.of(val)) {
            got.add(i);
        }
        assertEquals(ints, got);
        assertEquals(ints.size(), BitsIterable.of(val).size());
        assertEquals(0, BitsIterable.of(0L).size());
    }

    @Test
    public void testInt() {
        int val = 0;
        List<Integer> ints = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (i % 3 == 0) {
                val |= 1 << i;
                ints.add(i);
            }
        }
        List<Integer> got = new ArrayList<>();
        for (Integer i : BitsIterable.of(val)) {
            got.add(i);
        }
        assertEquals(ints, got);
        assertEquals(ints.size(), BitsIterable.of(val).size());
        assertEquals(0, BitsIterable.of(0).size());
    }
}
