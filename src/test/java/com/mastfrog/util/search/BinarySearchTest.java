/*
 * The MIT License
 *
 * Copyright 2010-2015 Tim Boudreau.
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

package com.mastfrog.util.search;

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class BinarySearchTest {

    @Test
    public void testSearchFor() {
        assertTrue(true);

        List<W> l = W.listOf(5, 10, 15, 20, 25, 30, 35, 40);
        BinarySearch<W> bs = new BinarySearch<W>(new E(), l);

        W w = bs.searchFor(9, Bias.FORWARD);
        assertNotNull(w);
        assertEquals(10, w.value);
        w = bs.searchFor(9, Bias.BACKWARD);
        assertNotNull(w);
        assertEquals(5, w.value);
        w = bs.searchFor(9, Bias.NONE);
        assertNull(w);
        w = bs.searchFor(9, Bias.NEAREST);
        assertEquals(10, w.value);

        w = bs.searchFor(-9, Bias.NEAREST);
        assertEquals(5, w.value);

        w = bs.searchFor(45, Bias.NEAREST);
        assertEquals(40, w.value);
//        
//        w = bs.searchFor(45, Bias.FORWARD);
//        assertNull("" + w, w);
//        
//        w = bs.searchFor(-11, Bias.BACKWARD);
//        assertNull(w + "", w);
        

        l = W.listOf(105, 10, 15, 20, 25, 30, 35, 40);
        try {
            bs = new BinarySearch<W>(new E(), l);
            fail("Constructed out of order");
        } catch (Throwable t) {
        }



    }

    private static class W implements Comparable<W> {

        private final long value;

        W(long value) {
            this.value = value;
        }

        public static List<W> listOf(long... values) {
            List<W> result = new LinkedList<W>();
            for (long l : values) {
                result.add(new W(l));
            }
            return result;
        }
        
        @Override
        public String toString() {
            return value + "";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final W other = (W) obj;
            if (this.value != other.value) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (int) (this.value ^ (this.value >>> 32));
            return hash;
        }

        @Override
        public int compareTo(W o) {
            return value == o.value ? 0 : value > o.value ? 1 : 0;
        }
    }

    static final class E implements BinarySearch.Evaluator<W> {

        @Override
        public long getValue(W obj) {
            return obj.value;
        }
    }
}
