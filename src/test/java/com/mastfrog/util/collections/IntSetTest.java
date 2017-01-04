/* 
 * The MIT License
 *
 * Copyright 2016 Tim Boudreau.
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
package com.mastfrog.util.collections;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class IntSetTest {

    @Test
    public void test() {
        IntSet is = new IntSet(10);
        for (int i = 0; i < 100; i++) {
            assertFalse(is.contains(i * 5));
            is.add(i * 5);
            assertTrue(is.contains(i * 5));
            assertFalse(is.contains((i * 5) + 1));
        }
        assertEquals(100, is.size());
        for (int i = 0; i < 100; i++) {
            assertTrue(is.contains(i * 5));
            if ((i % 5) != 0) {
                assertFalse(is.contains(i));
            }
        }
        int ix;
        Iterator<Integer> it;
        for (it = is.iterator(), ix = 0; it.hasNext(); ix++) {
            assertEquals("Mismatch at " + ix, Integer.valueOf(ix * 5), it.next());
        }
        is.remove(5);
        assertFalse(is.contains(5));
        Iterator<Integer> iter = is.iterator();
        assertEquals((Integer) 0, iter.next());
        assertEquals((Integer) 10, iter.next());

        is.retainAll(Arrays.asList(10, 20, 30, 40));
        assertEquals(4, is.size());
        assertTrue(is.containsAll(Arrays.asList(10, 20, 30, 40)));
        assertFalse(is.containsAll(Arrays.asList(5, 15, 25)));

        is.removeAll(Arrays.asList(10, 20));
        assertEquals(2, is.size());
        assertEquals(new HashSet<>(Arrays.asList(30, 40)), is);
    }

    @Test
    public void testToArray() {
        IntSet is = new IntSet(10);
        for (int i = 0; i < 100; i++) {
            is.add(i * 5);
        }
        Integer[] in = new Integer[100];
        in = is.toArray(in);
        assertEquals(100, in.length);
        for (int i = 0; i < 100; i++) {
            assertEquals("Fail at " + i, Integer.valueOf(i * 5), in[i]);
        }
        in = is.toArray(new Integer[0]);
        assertEquals(100, in.length);
        for (int i = 0; i < 100; i++) {
            assertEquals(Integer.valueOf(i * 5), in[i]);
        }
        Object[] o = is.toArray();
        assertEquals(100, o.length);
        for (int i = 0; i < 100; i++) {
            assertEquals(i * 5, o[i]);
        }
    }

    @Test
    public void testEmpty() {
        IntSet is = new IntSet();
        assertEquals(0, is.size());
        assertTrue(is.isEmpty());
        assertFalse(is.iterator().hasNext());
        assertFalse(is.contains(0));
        assertEquals(-1, is.max());
        assertEquals(0, is.toArray().length);
    }

    @Test
    public void testInterop() {
        IntSet is = new IntSet(10);
        for (int i = 0; i < 100; i++) {
            is.add(i * 5);
        }
        Set<Integer> hs = new HashSet<>(is);
        assertEquals(hs.size(), is.size());
        assertTrue(hs.containsAll(is));
        assertTrue(is.containsAll(hs));
        assertEquals(hs.hashCode(), is.hashCode());
        assertEquals(hs, is);
        assertEquals(is, hs);
    }

    @Test
    public void testRandom() {
        IntSet is = new IntSet(10);
        Random r = new Random(23239);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            is.add(i * 5);
        }
        for (int i = 0; i < 100000; i++) {
            int val = is.pick(r);
            seen.add(val);
        }
        Set<Integer> unseen = new HashSet<>(is);
        unseen.removeAll(seen);
        assertTrue("Did not see: " + unseen, unseen.isEmpty());
    }

    @Test
    public void testRandomMore() {
        IntSet exclude = new IntSet().addAll(1, 2, 3, 4);
        IntSet test = new IntSet().addAll(1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 23, 24);
        IntSet found = new IntSet();
        Random r = new Random(23239);
        for (int i = 0; i < 8; i++) {
            Integer val = test.pick(r, exclude);
            if (val == null) {
                break;
            }
            found.add(val);
//            assertFalse("" + val, exclude.contains(val));
            exclude.add(val);
        }
        assertEquals(new IntSet().addAll(11,12,13,14,21,22,23,24), found);
        assertNull(test.pick(r, exclude));
    }

    @Test
    public void testRandomNonRepeating() {
        IntSet is = new IntSet(10);
        IntSet used = new IntSet();
        Random r = new Random(23239);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            is.add(i * 5);
        }
        for (int i = 0; i < 10000; i++) {
            Integer val = is.pick(r, used);
            if (val == null) {
                break;
            }
            seen.add(val);
        }
        Set<Integer> unseen = new HashSet<>(is);
        unseen.removeAll(seen);
        assertTrue("Did not see: " + unseen, unseen.isEmpty());
    }
}
