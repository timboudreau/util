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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicSmallIntsTest {

    @Test
    public void testSmallInts() {
        AtomicSmallInts ints = new AtomicSmallInts();

        for (int i = 60000; i < 61000; i++) {
            ints.set(i, i + 1, i + 2, i + 3);
            assertEquals(i, ints.a());
            assertEquals(i + 1, ints.b());
            assertEquals(i + 2, ints.c());
            assertEquals(i + 3, ints.d());
            int ix = i;
            ints.get((a, b, c, d) -> {
                assertEquals(ix, a);
                assertEquals(ix + 1, b);
                assertEquals(ix + 2, c);
                assertEquals(ix + 3, d);
            });
            ints.update((a, b, c, d, cons) -> {
                cons.accept(d - 1, c - 1, b - 1, a - 1);
            });
            assertEquals(i + 2, ints.a());
            assertEquals(i + 1, ints.b());
            assertEquals(i, ints.c());
            assertEquals(i - 1, ints.d());
            int[] arr = ints.toArray();
            assertEquals(i + 2, arr[0]);
            assertEquals(i + 1, arr[1]);
            assertEquals(i, arr[2]);
            assertEquals(i - 1, arr[3]);

            ints.setA(i - 1);
            ints.setB(i - 2);
            ints.setC(i - 3);
            ints.setD(i - 4);
            assertEquals(i - 1, ints.a());
            assertEquals(i - 2, ints.b());
            assertEquals(i - 3, ints.c());
            assertEquals(i - 4, ints.d());
        }
    }

}
