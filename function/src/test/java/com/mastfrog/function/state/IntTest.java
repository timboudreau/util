/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntTest {

    @Test
    public void testIncDec() {
        Int a = Int.create();
        assertEquals(0, a.getAsInt());
        for (int i = 0; i < 10; i++) {
            a.increment(1);
            assertEquals(i + 1, a.getAsInt());
        }
        assertEquals(10, a.getAsInt());
        for (int i = 0; i < 10; i++) {
            a.decrement(1);
            assertEquals(10 - (i + 1), a.getAsInt());
        }
        assertEquals(0, a.getAsInt());

        for (int i = 0; i < 10; i++) {
            a.incrementSafe(1);
            assertEquals(i + 1, a.getAsInt());
        }
        assertEquals(10, a.getAsInt());
        for (int i = 0; i < 10; i++) {
            a.decrementSafe(1);
            assertEquals(10 - (i + 1), a.getAsInt());
        }
        assertEquals(0, a.getAsInt());
    }

    @Test
    public void testSafe() {
        int halfPlusOne = (Integer.MAX_VALUE / 2) + 1;
        int maxMinusOne = Integer.MAX_VALUE - 1;
        Int a = Int.of(halfPlusOne);
        Int b = Int.of(halfPlusOne);
        try {
            int res = a.plusSafe(b).getAsInt();
            fail("Exception should have been thrown, but got " + res);
        } catch (ArithmeticException e) {

        }
        assertEquals(halfPlusOne, a.getAsInt(), "State should be unchanged");
        try {
            int res = a.incrementSafe(maxMinusOne);
            fail("Exception should have been thrown, but got " + res);
        } catch (ArithmeticException e) {

        }
        assertEquals(halfPlusOne, a.getAsInt(), "State should be unchanged");

        int was = a.decrementSafe(halfPlusOne);
        assertNotEquals(halfPlusOne, a.getAsInt(), "Value unchanged");
        assertEquals(halfPlusOne, was);
        assertEquals(0, a.getAsInt());
        a.decrementSafe(halfPlusOne);
        assertEquals(-halfPlusOne, a.getAsInt());
        try {
            int res = a.decrementSafe(maxMinusOne);
            fail("Exception should have been thrown, but got " + res);
        } catch (ArithmeticException e) {

        }
    }

    @Test
    public void toSupplier() {
        Int a = Int.of(3);
        Int b = Int.of(2);
        assertEquals(5L, a.plus(b).getAsLong());
        assertEquals(5, a.plusSafe(b).getAsInt());

        assertEquals(6L, a.times(b).getAsLong());
        assertEquals(6L, a.timesSafe(b).getAsInt());

        assertEquals(1L, a.minus(b).getAsLong());
        assertEquals(1, a.minusSafe(b).getAsInt());

        assertEquals(1, a.dividedBy(b).getAsInt());

        assertEquals(0, a.dividedBy(() ->0).getAsInt());
    }

}
