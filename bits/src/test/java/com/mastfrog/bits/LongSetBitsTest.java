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
package com.mastfrog.bits;

import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Tim Boudreau
 */
public class LongSetBitsTest {

    private Random rnd;

    @Test
    public void testLongBits() {
        TreeSet<Long> all = randomLongs(23);
        LongSetBits lsb = new LongSetBits(all);
        assertEquals(all, convert(lsb));
        assertEquals(all, convertDown(lsb));

        System.out.println(lsb);

        for (Long l : all) {
            assertTrue(lsb.get(l));
        }
        TreeSet<Long> more = randomLongs(23);
        more.removeAll(all);
        LongSetBits osb = new LongSetBits(more);
        assertFalse(lsb.intersects(osb));
        for (Long l : more) {
            assertFalse(lsb.get(l));
        }

        Iterator<Long> it = all.iterator();
        Long prev = it.next();
        while (it.hasNext()) {
            Long curr = it.next();
            assertEquals(curr.longValue(), lsb.previousSetBitLong(curr));
            Long nx = lsb.nextSetBitLong(curr);
            assertEquals(curr.longValue(), nx,
                    "Next set bit of a bit which is set should be itself, " + curr
                    + " but got " + nx + " - present in set? " + all.contains(nx));
            long p = lsb.previousSetBitLong(curr - 1);
            assertEquals(prev.longValue(), p, "Prev of " + curr + " less one should be " + p);
            long n = lsb.nextSetBitLong(prev + 1);
            assertEquals(curr.longValue(), n);

            if (curr != prev + 1) {
                assertEquals(prev + 1, lsb.nextClearBitLong(prev));
                assertEquals(prev + 1, lsb.nextClearBitLong(prev + 1));
                assertEquals(curr - 1, lsb.previousClearBitLong(curr));
                assertEquals(curr - 1, lsb.previousClearBitLong(curr - 1));
            }

            prev = curr;
        }

        long tail = all.last();
        assertEquals(-1L, lsb.nextSetBitLong(tail + 1), "No item should be after last entry " + tail);
        assertEquals(tail, lsb.nextSetBitLong(tail), "Next of tail entry itself should be itself");

        long head = all.first();
        assertEquals(-1L, lsb.previousSetBitLong(head - 1), "Should not have an item before first entry " + head);
        assertEquals(head, lsb.previousSetBitLong(head));
        assertEquals(lsb.cardinality(), all.size());

        assertTrue(lsb.canContain(Integer.MAX_VALUE), "Should be able to contain max");
        assertTrue(lsb.canContain(Integer.MIN_VALUE), "Should be able to contain min");
    }

    private TreeSet<Long> convert(Bits bits) {
        TreeSet<Long> result = new TreeSet<>();
        long count = bits.forEachLongSetBitAscending(result::add);
        return result;
    }

    private TreeSet<Long> convertDown(Bits bits) {
        TreeSet<Long> result = new TreeSet<>();
        bits.forEachLongSetBitDescending(result::add);
        return result;
    }

    private TreeSet<Long> randomLongs(int count) {
        TreeSet<Long> result = new TreeSet<>();
        for (int i = 0; i < count; i++) {
            result.add(Math.abs(rnd.nextLong()));
        }
        return result;
    }

    @BeforeEach
    public void setup() {
        rnd = new Random(234280);
    }

}
