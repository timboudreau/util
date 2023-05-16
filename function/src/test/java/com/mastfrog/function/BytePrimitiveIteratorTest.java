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
package com.mastfrog.function;

import static com.mastfrog.function.BytePrimitiveIterator.byteIterator;
import static com.mastfrog.function.ShortPrimitiveIterator.shortIterator;
import java.util.Iterator;
import java.util.PrimitiveIterator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;

public class BytePrimitiveIteratorTest {

    static byte[] BYTES = new byte[256];
    static int[] INTS = new int[256];
    static short[] SHORTS = new short[256];

    static {
        for (int i = 0; i < 255; i++) {
            BYTES[i] = (byte) i;
            SHORTS[i] = (short) i;
            INTS[i] = i;
        }
    }

    @Test
    public void testSignedInts() {
        BytePrimitiveIterator bytes = byteIterator(BYTES);
        PrimitiveIterator.OfInt ints = byteIterator(BYTES).asInts();

        int cursor = 0;
        while (bytes.hasNext()) {
            assertTrue(ints.hasNext());
            int bval = bytes.nextByte();
            int ival = ints.nextInt();
            assertEquals(bval, ival, "Wrong value at " + cursor);
            cursor++;
        }
    }

    @Test
    public void testUnsignedInts() {
        PrimitiveIterator.OfInt bytes = byteIterator(BYTES).asUnsignedInts();
        PrimitiveIterator.OfInt ints = intPrimitiveIterator(INTS);

        int cursor = 0;
        while (bytes.hasNext()) {
            assertTrue(ints.hasNext());
            int bval = bytes.nextInt();
            int ival = ints.nextInt();
            assertEquals(bval, ival, "Wrong value at " + cursor);
            cursor++;
        }
    }

    @Test
    public void testUnsignedShorts() {
        ShortPrimitiveIterator bytes = byteIterator(BYTES).asUnsignedShorts();
        ShortPrimitiveIterator ints = shortIterator(SHORTS);

        int cursor = 0;
        while (bytes.hasNext()) {
            assertTrue(ints.hasNext());
            int bval = bytes.nextShort();
            int ival = ints.nextShort();
            assertEquals(ival, bval, "Wrong value at " + cursor);
            cursor++;
        }
    }

    @Test
    @Timeout(5)
    public void sanityCheckIntCount() {
        sanityCheckCount(INTS.length, intPrimitiveIterator(INTS));
    }

    @Test
    @Timeout(5)
    public void sanityCheckShortCount() {
        sanityCheckCount(SHORTS.length, shortIterator(SHORTS));
    }

    @Test
    @Timeout(5)
    public void sanityCheckByteCount() {
        sanityCheckCount(BYTES.length, byteIterator(BYTES));
    }

    public void sanityCheckCount(int ct, Iterator<?> iter) {
        int counted = 0;
        while (iter.hasNext()) {
            iter.next();
            counted++;
            if (counted > ct) {
                fail("Count too large: " + counted + " / " + ct);
            }
        }
        assertEquals(ct, counted, "Wrong count for " + iter);
    }

    @Test
    public void testShortConversion() {
        BytePrimitiveIterator b = BytePrimitiveIterator.byteIterator(BYTES);
    }

    static PrimitiveIterator.OfInt intPrimitiveIterator(int[] ints) {
        assertNotNull(ints);
        return new IPI(ints);
    }

    static class IPI implements PrimitiveIterator.OfInt {

        private final int[] values;
        private int cursor = -1;

        IPI(int[] values) {
            this.values = values;
        }

        @Override
        public int nextInt() {
            return values[++cursor];
        }

        @Override
        public boolean hasNext() {
            return cursor < values.length - 1;
        }

    }

}
