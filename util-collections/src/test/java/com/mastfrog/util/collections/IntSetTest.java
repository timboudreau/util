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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class IntSetTest {

    @Test
    public void test() {
        IntSet is = new IntSetImpl(10);
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
        IntSet is = new IntSetImpl(10);
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
        IntSet is = new IntSetImpl();
        assertEquals(0, is.size());
        assertTrue(is.isEmpty());
        assertFalse(is.iterator().hasNext());
        assertFalse(is.contains(0));
        assertEquals(-1, is.max());
        assertEquals(0, is.toArray().length);
    }

    @Test
    public void testInterop() {
        IntSet is = new IntSetImpl(10);
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
        IntSet is = new IntSetImpl(10);
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
        IntSet exclude = new IntSetImpl().addAll(1, 2, 3, 4);
        IntSet test = new IntSetImpl().addAll(1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 23, 24);
        IntSet found = new IntSetImpl();
        Set<Integer> excl = new HashSet<>(exclude);
        Random r = new Random(23239);
        for (int i = 0; i < 8; i++) {
            Integer val = test.pick(r, excl);
            if (val == null) {
                break;
            }
            found.add(val);
//            assertFalse("" + val, exclude.contains(val));
            exclude.add(val);
        }
        assertEquals(new IntSetImpl().addAll(11, 12, 13, 14, 21, 22, 23, 24), found);
        assertNull(test.pick(r, exclude));
    }

    @Test
    public void testRandomNonRepeating() {
        IntSet is = new IntSetImpl(10);
        Set<Integer> used = new HashSet<>();
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

    @Test
    public void testAllOf() {
        testAllOf(10, 20);
        testAllOf(1000000, 1000010);
        testAllOf(5, -5);
        testAllOf(-7, 23);
    }

    private void testAllOf(int first, int last) {
        int min = Math.min(first, last);
        int max = Math.max(first, last);
        IntSet is = IntSet.allOf(first, last);
        String msg = is.getClass().getSimpleName() + "(" + is.toString() + ")";
        for (int i = min - 10; i < min; i++) {
            assertFalse(msg, is.contains(i));
        }
        for (int i = min; i <= max; i++) {
            assertTrue(msg, is.contains(i));
        }
        for (int i = max + 1; i < max + 10; i++) {
            assertFalse(msg, is.contains(i));
        }
        assertEquals(msg, (max - min) + 1, is.size());
    }

    @Test
    public void testIndexOf() {
        testIndexOf(arrays, 11, 3);
        testIndexOf(bits, 11, 3);
        testIndexOf(arrays, 20, 5);
        testIndexOf(bits, 20, 5);
    }

    private void testIndexOf(Supplier<IntSet> factory, int size, int iterations) {
        Random rnd = new Random(1029040997L * size + iterations);
        for (int i = 0; i < iterations; i++) {
            IntSet set = factory.get();
            String msg = set.getClass().getSimpleName() + ": ";
            Set<Integer> baseline = new HashSet<>();
            for (int j = 0; j < size; j++) {
                int r = rnd.nextInt(200);
                set.add(r);
                baseline.add(r);
                assertTrue(msg + " absent " + r + " after add in " + set, set.contains(r));
                assertEquals(msg + " did not grow after add of " + r + " to " + set,
                        baseline.size(), set.size());
            }
            assertEquals(baseline, set);
            assertEquals(msg + baseline + " vs " + set, baseline.size(), set.size());
            int index = 0;
            List<Integer> exp = new ArrayList<>(baseline);
            Collections.sort(exp);
            for (Integer v : exp) {
                int ix = set.indexOf(v.intValue());
                int expected = index++;
                assertEquals(msg + " returned wrong index " + ix + " for " + v
                        + " - should be " + expected + " - in " + set, expected, ix);

                int val = set.valueAt(ix);
                assertEquals(msg + " returned wrong value " + val + " for index " + ix
                        + " - should be " + v + " as returned by indexOf(v)",
                        val, v.intValue());
            }
            assertEquals(msg, set, baseline);
            int tested = 0;
            while (tested < 5) {
                int r = rnd.nextInt(200);
                if (!baseline.contains(r)) {
                    tested++;
                    assertEquals(msg + " returned wrong index for absent value " + r,
                            -1, set.indexOf(r));
                }
            }
            assertEquals(msg, -1, set.indexOf(-1));
            set.forEachInt(val -> {
                assertEquals(msg + " returned wrong index for negative of present value " + -val, -1, set.indexOf(-val));
            });
        }
    }

    @Test
    public void testDiscontiguous() {
        testDiscontiguous(bits);
        testDiscontiguous(arrays);
    }

    private void testDiscontiguous(Supplier<IntSet> supp) {
        IntSet set = supp.get();
        String msg = set.getClass().getSimpleName() + ": ";
        for (int i = 0; i < 100; i++) {
            int tens = i / 10;
            if (tens % 2 == 0) {
                set.add(i);
            }
        }
        int first = set.lastContiguous();
        assertEquals(msg + " first run should end at 9", 9, first);
        for (int i = 20; i < 30; i++) {
            assertEquals(msg + " for " + i, 29, set.lastContiguous(i));
        }
        assertEquals(msg, 89, set.lastContiguous(80));
        assertEquals(msg, 89, set.lastContiguous(89));
        assertEquals(msg, 90, set.lastContiguous(90));
        assertEquals(msg, 92, set.lastContiguous(92));

        for (int i = 10; i < 20; i++) {
            assertEquals(i, set.lastContiguous(i));
        }

        set.remove(0);
        assertEquals(0, set.lastContiguous());

        int neg = set.lastContiguous(-10);
        if (set.isArrayBased()) {
            assertEquals(msg, -10, neg);
        } else {
            assertEquals(msg, set.lastContiguous(), neg);
        }
        try {
            set.valueAt(-1000);
            fail(msg + "Exception not thrown on valueAt() with negative index");
        } catch (IndexOutOfBoundsException ex) {
            // ok
        }
    }

    @Test
    public void testValuesBetween() {
        testValuesBetween(arrays, 50, 3);
        testValuesBetween(bits, 50, 3);
        testValuesBetween(arrays, 7, 1);
        testValuesBetween(bits, 7, 1);
    }

    private void testValuesBetween(Supplier<IntSet> is, int size, int iterations) {
        Random rnd = new Random(73371 * size + iterations);
        for (int i = 0; i < iterations; i++) {
            TreeSet<Integer> sanityCheck = new TreeSet<>();
            IntSet set = is.get();
            String msg = set.getClass().getSimpleName() + ": ";
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (int j = 0; j < size; j++) {
                int curr = rnd.nextInt(200);
                sanityCheck.add(curr);
                set.add(curr);
                max = Math.max(max, curr);
                min = Math.min(min, curr);
            }
            assertSets("Test is broken. ", sanityCheck, bruteForceValuesBetween(max + 1, min - 1, sanityCheck));
            assertSets("Test is broken. ", sanityCheck, bruteForceValuesBetween(max, min, sanityCheck));
            assertEquals(msg + "Set sizes differ", sanityCheck.size(), set.size());
            assertEquals(msg + "Sets differ", sanityCheck, set);
            for (int first = min - 1; first <= max + 1; first++) {
                for (int last = first; last < max; last++) {
                    Set<Integer> expect = bruteForceValuesBetween(first, last, sanityCheck);
                    Set<Integer> got = intSetValuesBetween(first, last, set);
                    assertSets(msg, expect, got);
                    assertEquals(msg + "Right set but wrong count returned",
                            expect.size(), lastCount);
                }
            }
        }
    }

    private void assertSets(String msg, Set<Integer> expect, Set<Integer> got) {
        if (expect.equals(got)) {
            return;
        }
        Set<Integer> isect = CollectionUtils.intersection(expect, got);
        Set<Integer> notPresent = new HashSet<>(expect);
        notPresent.removeAll(got);
        Set<Integer> surprises = new HashSet<>(got);
        surprises.removeAll(expect);
        StringBuilder sb = new StringBuilder(msg).append("Sets do not match. ");
        sb.append("Missing ").append(notPresent.size()).append(" items. ")
                .append(surprises.size()).append(" unexpected items.  Common items: ")
                .append(isect.size()).append(" of expected ").append(expect.size())
                .append("\nMissing: ").append(notPresent)
                .append("\nUnexpected: ").append(surprises)
                .append("\nIntersection: ").append(isect);
        fail(sb.toString());
    }

    int lastCount;

    private Set<Integer> intSetValuesBetween(int a, int b, IntSet is) {
        Set<Integer> result = new TreeSet<>();
        lastCount = is.valuesBetween(a, b, (ix, val) -> {
            result.add(val);
        });
        return result;
    }

    private Set<Integer> bruteForceValuesBetween(int a, int b, Set<Integer> set) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        Set<Integer> result = new TreeSet<>();
        for (int i = min; i <= max; i++) {
            if (set.contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    private static final Supplier<IntSet> arrays = () -> {
        return new IntSetArray();
    };
    private static final Supplier<IntSet> bits = () -> {
        return new IntSetImpl();
    };

}
