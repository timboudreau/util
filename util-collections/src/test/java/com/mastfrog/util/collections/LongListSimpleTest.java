/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import com.mastfrog.util.search.Bias;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class LongListSimpleTest {

    static final long[] SORTED = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
    static final long[] TENS = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    @Test
    public void testBasicOperations() {
        LongListSimple a = new LongListSimple(3);
        for (int i = 0; i < SORTED.length; i++) {
            a.add(SORTED[i]);
        }
        assertEquals(SORTED.length, a.size());
        for (int i = 0; i < SORTED.length; i++) {
            long l = SORTED[i];
            assertEquals(l, a.getAsLong(i));
            assertNotNull(a.get(i));
            assertEquals(l, a.get(i).longValue());
            assertEquals("Wrong index for " + l + " in " + a, i, a.indexOf(l));
            assertTrue("Not present in " + a + ": " + l, a.contains(l));
        }
        assertArrayEquals(SORTED, a.toLongArray());
        LongListSimple b = new LongListSimple(SORTED);
        assertEquals(SORTED.length, b.size());
        for (int i = 0; i < SORTED.length; i++) {
            long l = SORTED[i];
            assertEquals(l, b.getAsLong(i));
            assertNotNull(b.get(i));
            assertEquals(l, b.get(i).longValue());
            assertEquals("Wrong index for " + l + " in " + b, i, b.indexOf(l));
            assertTrue("Not present in " + b + ": " + l, b.contains(l));
        }
        assertArrayEquals(SORTED, b.toLongArray());
    }

    @Test
    public void testRemove() {
        LongListSimple a = new LongListSimple(SORTED);
//        assertTrue(a.isSorted());
        List<Long> vals = new ArrayList<>();
        assertEquals(-1, a.removeValue(-3));
        assertEquals(-1, a.removeValue(21));
        assertEquals(-1, a.removeValue(105));
        assertEquals(SORTED.length, a.size());
        for (int i = SORTED.length - 1; i >= 0; i--) {
            long l = SORTED[i];
            if ((l / 5) % 2 != 0) {
                long sz = a.size();
                int ix = a.removeValue(l);
                assertEquals("Wrong answer for remove " + l + " at " + i, i, ix);
                assertEquals(sz - 1, a.size());
                assertFalse(a.contains(l));
            } else {
                vals.add(0, l);
            }
        }
        long[] expected = ArrayUtils.toLongArray(vals);
        assertEquals("Wrong length of " + a, expected.length, a.size());
        for (long t : expected) {
            assertTrue(a.contains(t));
        }
        assertArrayEquals(expected, a.toLongArray());
        for (long t : TENS) {
            assertTrue(a.contains(t));
        }
        assertArrayEquals(TENS, a.toLongArray());
    }

    @Test
    public void testIndexOf() {
        LongListSimple x = new LongListSimple(new long[]{
            52, 73, 0, 15, 1, 12
        });
        assertEquals(6, x.size());
        assertEquals(1, x.indexOf(73));
        assertEquals(5, x.indexOf(12));

        LongListSimple a = new LongListSimple(SORTED);
        for (int i = 0; i < SORTED.length; i++) {
            assertEquals(i, a.indexOf(SORTED[i]));
        }
        long[] unsorted = ArrayUtils.reversed(SORTED);
        LongListSimple b = new LongListSimple(unsorted);

        for (int i = 0; i < unsorted.length; i++) {
            assertEquals(i, b.indexOf(unsorted[i]));
        }

        b.add(5L);
        assertEquals(b.size() - 1, b.lastIndexOf(5L));
        assertEquals(b.size() - 3, b.indexOf(5L));

        LongListSimple c = new LongListSimple(SORTED);
        assertEquals(1, c.indexOf(5L));
        assertEquals(1, c.lastIndexOf(5L));
        c.add(5L);
        assertEquals(1, c.indexOf(5L));
        assertEquals(c.size() - 1, c.lastIndexOf(5L));

        c.clear();
        assertEquals(0, c.size());
        assertTrue(c.isEmpty());

        c = new LongListSimple(SORTED);
        assertEquals(2, c.indexOfArray(new long[]{10, 15, 20}));
        assertEquals(0, c.indexOfArray(new long[]{0, 5, 10}));
        assertEquals(-1, c.indexOfArray(new long[]{20, 15, 10}));
        assertEquals(3, c.indexOfArray(new long[]{15}));
        assertEquals(3, c.indexOf(15L));
    }

    @Test
    public void testRemoveUnsorted() {
        long[] unsorted = ArrayUtils.reversed(SORTED);
        LongListSimple a = new LongListSimple(unsorted);
//        assertFalse(a.isSorted());
        List<Long> vals = new ArrayList<>();
        assertEquals(-1, a.removeValue(-3));
        assertEquals(-1, a.removeValue(21));
        assertEquals(-1, a.removeValue(105));
        assertEquals(unsorted.length, a.size());
        for (int i = unsorted.length - 1; i >= 0; i--) {
            long l = unsorted[i];
            if ((l / 5) % 2 != 0) {
                long sz = a.size();
                assertNotEquals(-1, a.indexOf(l));
                assertTrue(a.contains(l));
                int ix = a.removeValue(l);
//                assertEquals("Wrong answer for remove " + l + " at " + i, 500 - i, ix);
                assertEquals(sz - 1, a.size());
                assertFalse(a.contains(l));
            } else {
                vals.add(l);
            }
        }
        Collections.reverse(vals);
        long[] expected = ArrayUtils.toLongArray(vals);
        assertEquals("Wrong length of " + a, expected.length, a.size());
        assertArrayEquals(expected, a.toLongArray());
        for (long t : expected) {
            assertTrue("Should contain " + t + " but does not: " + a, a.contains(t));
        }
        long[] tensReversed = ArrayUtils.reversed(TENS);
        for (long t : tensReversed) {
            assertTrue(a.contains(t));
        }
        assertArrayEquals(tensReversed, a.toLongArray());
    }

    @Test
    public void testRemoveMore() {
        for (int i = 0; i < 499 - 129; i++) {
            int batchSize = Math.max(7, i / 3);
            LongListSimple l = list(batchSize);
            List<Long> expect = arrayList();
            for (int j = 129; j >= 0; j--) {
                int index = i + j;
                long value = index;
                int oldSize = l.size();
                assertEquals(i + "," + (129 - j) + " bs " + batchSize + " in " + l, index, l.indexOf(value));
                int removed = l.removeValue(value);
                assertEquals(index, removed);
                assertNotEquals("Remove succeeded but size unchanged", oldSize, l.size());
                assertFalse("At " + i + "," + j + " list still contains " + value
                        + " with batch size " + batchSize + " - " + l + " after removing " + value + " at " + removed, l.contains(value));
                expect.remove(value);
            }
            long[] expected = ArrayUtils.toLongArray(l);
            long[] got = l.toLongArray();
            assertArrayEquals(expected, got);
        }
    }

    @Test
    public void testRemoveRange() {
        AL<Long> al = arrayList(100);
        LongListSimple l = list(16, 100);
        check(al, l);
        l.removeRange(2, 10);
        al.removeRange(2, 10);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(14, 24);
        al.removeRange(14, 24);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(16, 32);
        al.removeRange(16, 32);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(0, 16);
        al.removeRange(0, 16);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(0, 1);
        al.removeRange(0, 1);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(90, 99);
        al.removeRange(90, 99);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(90, 100);
        al.removeRange(90, 100);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(0, 99);
        al.removeRange(0, 99);
        check(al, l);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(14, 32);
        al.removeRange(14, 32);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(15, 32);
        al.removeRange(15, 32);

        al = arrayList(100);
        l = list(16, 100);
        l.removeRange(0, 17);
        al.removeRange(0, 17);

    }

    private void check(List<Long> expect, LongListSimple got) {
        check(null, expect, got);
    }

    private void check(String msg, List<Long> expect, LongListSimple got) {
        long[] exp = ArrayUtils.toLongArray(expect);
        long[] gt = got.toLongArray();
//        if (!Arrays.equals(exp, gt)) {
//            System.out.println("EXP " + expect);
//            System.out.println("GOT " + Arrays.toString(got.toLongArray()));
//        }
        assertArrayEquals(msg, exp, gt);
        assertEquals(expect, got);
        assertEquals("hashCode mismatch", expect.hashCode(), got.hashCode());
    }

    @Test
    public void testEquals() {
        LongListSimple a = list(5, 100);
        LongListSimple b = list(13, 100);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testRemoveRangeMore() {
        for (int batchSize : new int[]{3, 7, 16, 24, 64}) {
            for (int sz = 3; sz <= batchSize * 3; sz++) {
                for (int j = 0; j < sz; j++) {
                    for (int k = j + 1; k < sz; k++) {
                        AL<Long> al = arrayList(sz);
                        LongListSimple l = list(batchSize, sz);
                        try {
                            al.removeRange(j, k);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            throw new ArrayIndexOutOfBoundsException("ArrayList.removeRange(" + j + "," + k + ") of " + al.size());
                        }
                        l.removeRange(j, k);
                        check("bs " + batchSize + " size " + sz + " removing range " + j + "," + k, al, l);
                    }
                }
            }
        }
    }

    @Test
    public void testCornerCases() {
        LongListSimple ll = new LongListSimple();
        LongListSimple ll2 = new LongListSimple();
        assertEquals(ll, ll2);
        assertEquals(Collections.emptyList(), ll);
        assertEquals(false, ll.contains(0));
        assertEquals(false, ll.contains(1));
        assertEquals(-1, ll.indexOf(0));
        assertEquals(-1, ll.indexOf(1));
        ll.addAll(new long[0]);
        assertTrue(ll.isEmpty());
        assertEquals(0, ll.size());
    }

    @Test
    public void testSubList() {
        for (int batchSize : new int[]{3, 7, 16, 24}) {
            for (int sz = 3; sz <= batchSize * 3; sz++) {
                for (int j = 0; j < sz; j++) {
                    for (int k = j + 1; k < sz; k++) {
                        AL<Long> al = arrayList(sz);
                        LongListSimple l = list(batchSize, sz);
                        LongListSimple sl = l.subList(j, k);
                        List<Long> sal = al.subList(j, k);
                        check(sal, sl);
                    }
                }
            }
        }
    }

    @Test
    public void testAddAll() {
        LongListSimple ll = new LongListSimple(SORTED);
        ll.addAll(1, Arrays.asList(1L, 2L, 3L, 4L));
        assertEquals(SORTED.length + 4, ll.size());
        long[] exp = {0, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());

        ll.addAll(0, Collections.emptyList());
        assertArrayEquals(exp, ll.toLongArray());

        ll.addAll(1, Arrays.asList(1L));
        exp = new long[]{0, 1, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());
//        assertFalse(ll.isSorted());

        ll.addAll(1, Arrays.asList(71L, 72L));
        exp = new long[]{0, 71L, 72L, 1, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());
//        assertFalse(ll.isSorted());

        ll.addAll(0, Arrays.asList(-1L, -2L));
        exp = new long[]{-1L, -2L, 0, 71L, 72L, 1, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());
//        assertFalse(ll.isSorted());
    }

    @Test
    public void testAddAllAboveBatchSize() {
        LongListSimple ll = new LongListSimple(SORTED);
//        assertTrue(ll.isSorted());
        ll.addAll(1, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
        long[] exp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());
//        assertFalse(ll.isSorted());
    }

    @Test
    public void testAddAllAboveBatchSize2() {
        LongListSimple ll = new LongListSimple(SORTED);
//        assertTrue(ll.isSorted());
        ll.addAll(1, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L));
        long[] exp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        assertArrayEquals(exp, ll.toLongArray());
//        assertFalse(ll.isSorted());
    }

    @Test
    public void testAddAllAtBatchSize() {
        LongListSimple ll = list(5, 5);
//        assertTrue(ll.isSorted());
        ll.addAll(5, Arrays.asList(5L));

        long[] exp = {0, 1, 2, 3, 4, 5};
        assertArrayEquals(exp, ll.toLongArray());
    }

    @Test
    public void testAddAllNearBatchSize() {
        LongListSimple ll = list(15, 16);
//        assertTrue(ll.isSorted());
        ll.addAll(16, Arrays.asList(16L));

        long[] exp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        assertArrayEquals(exp, ll.toLongArray());
    }

    @Test
    public void testAddGreaterThanBatchSize() {
        LongListSimple ll = list(5, 5);
        List<Long> al = arrayList(5);
        ll.addAll(4, Arrays.asList(4L));
        al.addAll(4, Arrays.asList(4L));
        check(al, ll);
    }

    @Test
    public void testAddWithIndex() {
        LongListSimple ll = new LongListSimple(SORTED);
        assertArrayEquals(SORTED, ll.toLongArray());
        assertEquals(CollectionUtils.toList(SORTED), ll);
        ll.add(1, 2);
        assertEquals(0L, ll.getAsLong(0));
        assertEquals(ll.toString(), 2L, ll.getAsLong(1));
        assertEquals(ll.toString(), 5L, ll.getAsLong(2));
        assertEquals(SORTED.length + 1, ll.size());
        long[] exp = {0, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};
        for (int i = 0; i < exp.length; i++) {
            long val = ll.getAsLong(i);
            assertEquals("Mismatch at " + i + " in " + ll, exp[i], val);
        }
        // {0, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100}
        ll.add(4, 12L);
        assertTrue(ll.contains(12L));
        assertArrayEquals(new long[]{0, 2, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100}, ll.toLongArray());

        ll.add(0, -5L);
        assertTrue(ll.contains(-5L));
        assertArrayEquals(new long[]{-5, 0, 2, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100}, ll.toLongArray());

        ll.add(ll.size(), 105L);
        assertTrue(ll.contains(105L));
        assertArrayEquals(new long[]{-5, 0, 2, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105}, ll.toLongArray());

        ll.add(8, 23L);
        assertEquals(23L, ll.getAsLong(8));
//        assertTrue(ll.isSorted());
        assertTrue(ll.contains(23L));
        assertEquals(26, ll.size());
        assertEquals("toLongArray returns wrong length", 26, ll.toLongArray().length);
        long[] exp2 = new long[]{-5, 0, 2, 5, 10, 12, 15, 20, 23, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105};
        check(ArrayUtils.toBoxedList(exp2), ll);
    }

    @Test
    public void testAddCornerCase() {
        long[] arr = new long[]{-5, 0, 2, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45,
            50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105};

        LongListSimple ll = new LongListSimple(arr);
        List<Long> al = new ArrayList<>(ArrayUtils.toBoxedList(arr));
        assertArrayEquals(arr, ll.toLongArray());
        ll.add(8, 23L);
        al.add(8, 23L);
        assertEquals(23L, ll.getAsLong(8));
        check(al, ll);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testCannotAddAtArbitraryIndex() {
        LongListSimple ll = new LongListSimple(SORTED);
        ll.add(ll.size() + 1, 110L);
    }

    @Test
    public void testSize() {
        LongListSimple ll = list(5, 100);
        assertEquals(100, ll.size());
        ll.addAll(new long[]{200, 210, 220});
        assertEquals("Size should be +3 after adding array of 3", 103, ll.size());

        ll.addAll(new long[]{300, 310, 320});
        assertEquals("Size should be +6 after adding second array of 3", 106, ll.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLongerator() {
        LongListSimple ll = list(5, 30);
        com.mastfrog.util.collections.Longerator g = ll.longerator();
        for (long i = 0; i < 30; i++) {
            assertEquals(i, g.next());
            assertEquals(i != 29, g.hasNext());
        }
    }

    @Test
    public void testIterator() {
        LongListSimple ll = list(5, 30);
        Iterator<Long> g = ll.iterator();
        for (long i = 0; i < 30; i++) {
            assertEquals(i, g.next().longValue());
            assertEquals(i != 29, g.hasNext());
        }
    }

    @Test
    public void testSorted() {
        LongListSimple ll = list(5, 100);
//        assertTrue("Sorted list is sorted", ll.isSorted());
        ll.add(0, -5L);
//        assertTrue("List with sorted insertions should think it's sorted",
//                ll.isSorted());
        ll.add(-5L);
//        assertFalse("List with unsorted additions should think it's not sorted",
//                ll.isSorted());

        ll = list(5, 100);
        assertEquals("List creation broken", 100, ll.size());
        ll.addAll(new long[]{200, 210, 220});
//        assertTrue("List with sorted appended array should think it's sorted",
//                ll.isSorted());
        assertEquals("Size should be +3 after adding array of 3", 103, ll.size());

        ll.addAll(0, new long[]{300, 301, 302});
        assertEquals("Size should be +6 after adding a second array of 3", 106, ll.size());
//        assertFalse("List with unsorted prepend should not think it's",
//                ll.isSorted());

        ll.sort();
        assertEquals("After sort, highest entry should be last", 302,
                ll.getAsLong(ll.size() - 1));
//        assertTrue("After sort w/o duplicates list should think it's sorted: " + ll, ll.isSorted());

        ll = new LongListSimple(SORTED);
//        assertTrue("List constructed from sorted array should think it's sorted", ll.isSorted());
        ll.addAll(1, new long[]{2, 3, 4});
        assertEquals(SORTED.length + 3, ll.size());
//        assertTrue("List with sorted insert should think it's sorted: " + ll,
//                ll.isSorted());

        long[] tst = new long[]{0, 5, 10, 15, 20, 25, 30, 100, 110, 120};
        ll = new LongListSimple(tst);
        assertEquals("Wrong size after construction", tst.length, ll.size());
        ll.addAll(7, new long[]{36, 37, 38, 39, 40, 41, 42});
        assertEquals("Wrong size after optimized array insert", tst.length + 7, ll.size());
//        assertTrue("List with optimized insert at batch boundary should think it's sorted: " + ll,
//                ll.isSorted());

//        System.out.println("\ngo\n\n");
        ll.addAll(5, new long[]{36, 37, 38});
//        System.out.println("NOW " + ll);
//        assertFalse("List with unsorted additions should not think it's sorted",
//                ll.isSorted());
        ll.sort();
        // will not qualify as sorted since it contains duplicates
//        assertFalse("List with duplicates should not think it's sorted: " + ll,
//                ll.isSorted());

        ll = new LongListSimple(SORTED);
        ll.add(1, 2L);
//        assertTrue("List with single sorted insertion should think it's sorted",
//                ll.isSorted());

        ll.add(10, 3L);
//        assertFalse("List with unsorted insertion should not think it's sorted",
//                ll.isSorted());
        assertEquals(3L, ll.getAsLong(10));
        ll.sort();
//        assertTrue("After sort, list should think it's sorted", ll.isSorted());

        ll = new LongListSimple(SORTED);
        ll.addAll(1, new long[]{3, 2, 4});
//        assertFalse("List containing insertion of unsorted array should "
//                + "not think it's sorted", ll.isSorted());
        ll.sort();
//        assertTrue("List containing insertion of unsorted array should "
//                + "think it's sorted after calling sort() if no duplicates", ll.isSorted());

        ll = new LongListSimple();
//        assertTrue("Empty list should think it's sorted", ll.isSorted());
        ll.add(1L);
//        assertTrue("List with one element should think it's sorted", ll.isSorted());
        ll.add(2L);
//        assertTrue("List with sorted additions should think it's sorted", ll.isSorted());
        ll.add(0L);
//        assertFalse("List with unsorted additions should not think it's sorted", ll.isSorted());
        ll.clear();
        assertTrue("Cleared list should think it's sorted", ll.isSorted());

        ll = new LongListSimple(SORTED);
        ll.addAll(new long[]{300, 301, 302, 303});
//        assertTrue("After append sorted array, should be sorted", ll.isSorted());
        ll.addAll(new long[]{410, 409});
//        assertFalse("After append unsorted array, should not be sorted", ll.isSorted());
    }

    @Test
    public void testAddAllNearBatchSize2() {
        LongListSimple ll = list(5, 10);
//        assertTrue(ll.isSorted());
        ll.addAll(4, Arrays.asList(4L));
        long[] exp = {0, 1, 2, 3, 4, 4, 5, 6, 7, 8, 9};
        check(ArrayUtils.toBoxedList(exp), ll);
        assertArrayEquals(exp, ll.toLongArray());
    }

    @Test
    public void testAddTwoNearBatchSize2() {
        LongListSimple ll = list(5, 5);
//        assertTrue(ll.isSorted());
        ll.addAll(4, Arrays.asList(4L, 5L));
//        assertEquals(15, ll.currentCapacity());
        long[] exp = {0, 1, 2, 3, 4, 5, 4};
        check(ArrayUtils.toBoxedList(exp), ll);
        assertArrayEquals(exp, ll.toLongArray());
    }

    @Test
    public void testAddAllCornerCase1() throws Exception {
        // inserting [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11] at 1 in [0, 1, 2, 3, 4]
        LongListSimple ll = list(5, 5);
        ArrayList<Long> al = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
//            ll.addAll(1, al);
            al.add((long) i);
        }
        List<Long> exp = ArrayUtils.toBoxedList(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2, 3, 4});
        ll.addAll(1, al);
        assertEquals(exp, ll);
    }

    @Test
    public void testAddAllExhaustive() throws Exception {
        int maxBatch = 11;
        int iter = 0;
        for (int rangeSize = 1; rangeSize <= (maxBatch * 2) + 3; rangeSize++) {
            for (int batchSize = 5; batchSize < maxBatch; batchSize++) {
                for (int targetSize = 5; targetSize < 4 * batchSize; targetSize++) {
                    for (int rangeStart = 0; rangeStart < targetSize + 1; rangeStart++) {
                        List<Long> range = range(rangeStart, rangeSize);
                        LongListSimple ll = list(batchSize, targetSize);
                        AL<Long> al = arrayList(targetSize);
                        String msg = iter + ". rangeSize=" + rangeSize
                                + ", targetSize=" + targetSize + ", batchSize="
                                + batchSize + " inserting " + range + " at " + rangeStart
                                + " in " + al;
                        String before = al.toString();
                        assertEquals(al, ll);
                        try {
                            al.addAll(rangeStart, range);
                            ll.addAll(rangeStart, range);
                        } catch (Exception ex) {
                            String m = ex.getMessage() == null ? msg
                                    : msg + "\n" + ex.getMessage() + "\nexpecting: " + al;
                            throw new AssertionError(m, ex);
                        }
                        check(iter + ". Add failed for rangeSize=" + rangeSize
                                + ", targetSize=" + targetSize + ", batchSize="
                                + batchSize + " inserting " + range + " at " + rangeStart
                                + " into " + before, al, ll);
                        iter++;
                    }
                }
            }
        }
    }

    @Test
    public void testOptimizedAddAll() {
        LongListSimple ll = new LongListSimple(7);
        ll.addAll(new long[]{1, 2, 3, 4, 5, 6, 7});
        ll.addAll(new long[]{8, 9, 10, 11, 12, 13, 14, 15});
        ll.addAll(new long[]{16, 17});
//        assertTrue(ll.isSorted());
        long[] exp = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        check(ArrayUtils.toBoxedList(exp), ll);
        ll.addAll(new long[]{20, 19});
//        assertFalse(ll.isSorted());
    }

    @Test
    public void testRemoveUpdatesSize() {
        LongList ll = new LongListSimple(3);
        long[] exp = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
        ll.addAll(exp);
        assertEquals(exp.length, ll.size());
        int sz = exp.length;
        while (sz > 0) {
            ll.remove(sz - 1);
            sz--;
            assertEquals(sz, ll.size());
        }
        assertEquals(0, ll.size());

        ll.addAll(exp);
        assertEquals(exp.length, ll.size());
        sz = exp.length;
        while (sz > 0) {
            ll.remove(0);
            sz--;
            assertEquals(sz, ll.size());
        }
        assertEquals(0, ll.size());
    }

    @Test
    public void testStartsWith() {
        Random rnd = new Random(2309);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            LongListSimple a = new LongListSimple(sz / 2);
            for (int j = 0; j < sz; j++) {
                a.add(rnd.nextLong());
            }
            for (int j = 1; j < sz - 1; j++) {
                LongList sub = a.subList(0, j);
                List<Long> reg = new ArrayList<>(sub);
                assertEquals(reg, sub);
                assertEquals(sub, reg);
                assertEquals(reg.hashCode(), sub.hashCode());
                assertNotEquals(a, sub);
                assertTrue(a.startsWith(sub));
                assertFalse(sub.startsWith(a));
                assertTrue(sub instanceof LongListSimple);
                LongListSimple iil = (LongListSimple) sub;
                for (int k = 0; k < j; k++) {
                    iil.set(k, iil.get(k) + 1);
                    assertFalse(a.startsWith(sub));
                }
                assertTrue(a.startsWith(reg));
            }
            assertFalse(a.startsWith(new LongListSimple(1)));
            assertFalse(a.startsWith(Collections.emptyList()));
        }
    }

    @Test
    public void testEndsWith() {
        Random rnd = new Random(2309);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            LongListSimple a = new LongListSimple(sz / 2);
            for (int j = 0; j < sz; j++) {
                a.add(rnd.nextLong());
            }
            for (int j = 1; j < sz - 1; j++) {
                LongList sub = a.subList(a.size() - j, a.size());
                assertEquals("subList(" + (a.size() - j)
                        + "," + a.size() + " should return a list of size "
                        + j + " but got size " + sub.size() + ".\nOrig list: "
                        + a + "\nSub list:" + sub, j, sub.size());
                List<Long> reg = new ArrayList<>(sub);
                assertEquals(reg, sub);
                assertEquals(sub, reg);
                assertEquals(reg.hashCode(), sub.hashCode());
                assertNotEquals(a, sub);
                assertTrue(a.endsWith(sub));
                assertFalse(sub + " does not end with " + a, sub.endsWith(a));
                assertTrue(sub instanceof LongListSimple);
                LongListSimple iil = (LongListSimple) sub;
                for (int k = 0; k < j; k++) {
                    iil.set(k, iil.get(k) + 1);
                    assertFalse(a.endsWith(sub));
                }
                assertTrue(a.endsWith(reg));
                LongList sub2 = a.subList(0, j);
                if (!sub.equals(sub2)) { // can happen if all one value
                    assertFalse(a + " should not also end with " + sub2,
                            a.endsWith(sub2));
                }
            }
            assertFalse("endsWith(List) should return false for the empty list",
                    a.endsWith(Collections.emptyList()));
            assertFalse("endsWith(IntList) should return false for the empty list.",
                    a.endsWith(new LongListSimple(5)));
        }
    }

    @Test
    public void testSwap() {
        LongListSimple ili = new LongListSimple(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertTrue(ili.swap(9, 1));
        assertEquals(9, ili.getAsLong(1));
        assertEquals(1, ili.getAsLong(9));
        assertTrue(ili.swap(0, 10));
        assertEquals(10, ili.getAsLong(0));
        assertEquals(0, ili.getAsLong(10));
        assertFalse(ili.swap(1, 1));
        assertEquals(9, ili.getAsLong(1));
        assertArrayEquals(new long[]{10, 9, 2, 3, 4, 5, 6, 7, 8, 1, 0}, ili.toLongArray());
        assertFalse(ili.swap(1, 13));
        assertFalse(ili.swap(13, 13));
        assertFalse(ili.swap(14, 13));
    }

    @Test
    public void testSwapIndices() {
        LongListSimple ili = new LongListSimple(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertEquals(11, ili.size());
        assertTrue(ili.swapIndices(9, 1));
        assertEquals(9, ili.getAsLong(1));
        assertEquals(1, ili.getAsLong(9));
        assertEquals(11, ili.size());
        assertTrue(ili.swapIndices(0, 10));
        assertEquals(10, ili.getAsLong(0));
        assertEquals(0, ili.getAsLong(10));
        assertFalse(ili.swapIndices(1, 1));
        assertEquals(11, ili.size());
        assertEquals(9, ili.getAsLong(1));
        assertArrayEquals(new long[]{10, 9, 2, 3, 4, 5, 6, 7, 8, 1, 0}, ili.toLongArray());
        assertEquals(0, ili.indexOf(10));
        assertEquals(1, ili.indexOf(9));
        assertEquals(10, ili.indexOf(0));
        assertEquals(9, ili.indexOf(1));
        try {
            ili.swapIndices(-1, 5);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            ili.swapIndices(15, 72);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testSort() {
        long[] exp = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        long[] toUse = ArrayUtils.copyOf(exp);
        ArrayUtils.shuffle(new Random(130290329), toUse);
        assertFalse(Arrays.equals(exp, toUse));
        LongListSimple impl = new LongListSimple(toUse);
        impl.sort();
        long[] got = impl.toLongArray();
        assertArrayEquals(exp, got);
    }

    @Test
    public void testFuzzySearch() {
        long[] vals = new long[20];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = i * 10;
        }
        LongListSimple impl = new LongListSimple(vals);
        for (int i = 0; i < vals.length; i++) {
            int exp = i * 10;
            assertEquals(i, impl.indexOf(exp));
            assertEquals(i, impl.indexOfPresumingSorted(exp));
            assertEquals(i, impl.nearestIndexToPresumingSorted(exp, Bias.NONE));
        }
        for (int i = 0; i < vals.length; i++) {
            int expectedValue = i * 10;
            for (int j = 1; j < 10; j++) {
                int fwdTarget = expectedValue - j;
                int fwdIndex = impl.nearestIndexToPresumingSorted(fwdTarget, Bias.BACKWARD);
                assertTrue("Returned an index >= size " + fwdIndex, fwdIndex < impl.size());
                assertFalse("Returned an index < -1" + fwdIndex, fwdIndex < -1);
                assertEquals("Wrong index with Bias.BACKWARD searching for " + fwdTarget + " in " + impl, i - 1, fwdIndex);
            }

            for (int j = 1; j < 10; j++) {
                int bwdTarget = expectedValue + j;
                int bwdIndex = impl.nearestIndexToPresumingSorted(bwdTarget, Bias.FORWARD);
                assertTrue("Returned an index >= size " + bwdIndex, bwdIndex < impl.size());
                assertFalse("Returned an index < -1" + bwdIndex, bwdIndex < -1);
                assertEquals(i + ". Wrong index with Bias.FORWARD searching for " + bwdTarget + " in " + impl, i + 1, i + 1, bwdIndex);
            }
            for (int j = 1; j < 10; j++) {
                int fwdTarget = expectedValue - j;
                int fwdIndex = impl.nearestIndexToPresumingSorted(fwdTarget, Bias.FORWARD);
                assertTrue("Returned an index >= size " + fwdIndex, fwdIndex < impl.size());
                assertFalse("Returned an index < -1" + fwdIndex, fwdIndex < -1);
                assertEquals("Wrong index with Bias.FORWARD searching for " + fwdTarget + " in " + impl, i, fwdIndex);
            }
            for (int j = 1; j < 10; j++) {
                int bwdTarget = expectedValue + j;
                int bwdIndex = impl.nearestIndexToPresumingSorted(bwdTarget, Bias.BACKWARD);
                assertTrue("Returned an index >= size " + bwdIndex, bwdIndex < impl.size());
                assertFalse("Returned an index < -1" + bwdIndex, bwdIndex < -1);
                assertEquals("Wrong index with Bias.FORWARD searching for " + bwdTarget + " in " + impl, i, bwdIndex);
            }
            for (int j = 1; j < 10; j++) {
                if (j == 5) {
                    continue;
                }
                long bwdTarget = expectedValue + j;
                if (bwdTarget > impl.last()) {
                    bwdTarget = impl.last();
                    continue;
                }
                int bwdIndex = impl.nearestIndexToPresumingSorted(bwdTarget, Bias.NEAREST);
                int expIndex = j >= 5 ? i + 1 : i;
                if (bwdTarget > impl.last()) {
                    expIndex = impl.size() - 1;
                }
                assertTrue("Returned an index >= size " + bwdIndex, bwdIndex < impl.size());
                assertFalse("Returned an index < -1" + bwdIndex, bwdIndex < -1);
                assertEquals(i + "/" + j + ". Wrong index with Bias.NEAREST searching for " + bwdTarget + " in " + impl, expIndex, bwdIndex);
            }
        }
    }

    @Test
    public void testFuzzySearchWithAdjacentPairs() {
        LongListSimple il = new LongListSimple(new long[]{0, 1, 10, 11, 20, 21, 30, 31, 40, 41});
        //                                         0   1   2  3   4   5   6   7   8   9
        assertEquals(1, il.nearestIndexToPresumingSorted(3, Bias.BACKWARD));
        assertEquals(2, il.nearestIndexToPresumingSorted(3, Bias.FORWARD));

        assertEquals(1, il.nearestIndexToPresumingSorted(1, Bias.BACKWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(1, Bias.FORWARD));

        assertEquals(0, il.nearestIndexToPresumingSorted(0, Bias.BACKWARD));
        assertEquals(0, il.nearestIndexToPresumingSorted(0, Bias.FORWARD));

        assertEquals(4, il.nearestIndexToPresumingSorted(13, Bias.FORWARD));
        assertEquals(3, il.nearestIndexToPresumingSorted(13, Bias.BACKWARD));
    }

    @Test
    public void testSearchCornerCases() {
        LongListSimple il2 = new LongListSimple(new long[]{22});
        assertEquals(0, il2.nearestIndexToPresumingSorted(25, Bias.BACKWARD));
        assertEquals(-1, il2.nearestIndexToPresumingSorted(3, Bias.BACKWARD));
        assertEquals(-1, il2.nearestIndexToPresumingSorted(25, Bias.FORWARD));
        assertEquals(0, il2.nearestIndexToPresumingSorted(3, Bias.FORWARD));

        LongListSimple il = new LongListSimple();
        assertEquals(-1, il.nearestIndexToPresumingSorted(25, Bias.FORWARD));
        assertEquals(-1, il.nearestIndexToPresumingSorted(25, Bias.BACKWARD));

        LongListSimple il3 = new LongListSimple(new long[]{10, 20});
        assertEquals(0, il3.nearestIndexToPresumingSorted(15, Bias.BACKWARD));
        assertEquals(1, il3.nearestIndexToPresumingSorted(15, Bias.FORWARD));

        assertEquals(0, il3.nearestIndexToPresumingSorted(3, Bias.FORWARD));
        assertEquals(-1, il3.nearestIndexToPresumingSorted(3, Bias.BACKWARD));

        assertEquals(1, il3.nearestIndexToPresumingSorted(27, Bias.BACKWARD));
        assertEquals(-1, il3.nearestIndexToPresumingSorted(27, Bias.FORWARD));

        LongListSimple il4 = new LongListSimple(new long[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        });
        for (int i = 0; i < 10; i++) {
            assertEquals(i, il4.nearestIndexToPresumingSorted(i, Bias.NONE));
            assertEquals(i, il4.nearestIndexToPresumingSorted(i, Bias.FORWARD));
            assertEquals(i, il4.nearestIndexToPresumingSorted(i, Bias.BACKWARD));
            assertEquals(i, il4.nearestIndexToPresumingSorted(i, Bias.NEAREST));
        }
    }

    @Test
    public void testLargeList() {
        LongListSimple il = new LongListSimple(new long[]{1, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 47, 49, 51, 53, 55});
        //                                         0  1  2  3  4   5   6   7
        Random rnd = new Random(23901309);
        for (int x = 0; x < 7; x++) {
            for (int i = 1; i < il.size() - 2; i++) {
                long val = il.get(i);
                long prev = il.get(i - 1);
                long next = il.get(i + 1);
                for (long j = prev + 1; j <= val; j++) {
                    assertEquals("Wrong index seeking " + j + " in " + il + " FORWARD", i, il.nearestIndexToPresumingSorted(j, Bias.FORWARD));
                    if (j < val) {
                        assertEquals("Wrong index seeking " + j + " in " + il + " BACKWARD", i - 1, il.nearestIndexToPresumingSorted(j, Bias.BACKWARD));
                    } else {
                        assertEquals("Wrong index seeking " + j + " in " + il + " BACKWARD", i, il.nearestIndexToPresumingSorted(j, Bias.BACKWARD));
                    }
                }
            }
            long f = il.first();
            for (int i = 0; i < f - 1; i++) {
                assertEquals(0, il.nearestIndexToPresumingSorted(i, Bias.FORWARD));
                assertEquals(-1, il.nearestIndexToPresumingSorted(i, Bias.BACKWARD));
            }
            long l = il.last();
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.FORWARD));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.BACKWARD));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.NEAREST));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.NONE));
            for (long i = l + 1; i < l + 10; i++) {
                assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(i, Bias.BACKWARD));
                assertEquals(-1, il.nearestIndexToPresumingSorted(i, Bias.FORWARD));
            }

            il.clear();
            int sz = 20 + rnd.nextInt(30);
            int val = rnd.nextInt(20);
            for (int i = 0; i < sz; i++) {
                val += rnd.nextInt(13) + 1;
                il.add(val);
            }
        }
    }

    @Test
    public void testBoundaries() {
        LongListSimple il = new LongListSimple(new long[]{2, 5, 13, 21, 29, 37});
        //                                         0  1   2   3   4   5
        int ix = il.nearestIndexToPresumingSorted(48, Bias.BACKWARD);
        assertEquals(5, ix);

        ix = il.nearestIndexToPresumingSorted(0, Bias.FORWARD);
        assertEquals(0, ix);
    }

    @Test
    public void testSortedIndex() {
        LongListSimple il = new LongListSimple(new long[]{20});
        assertEquals(-1, il.indexOfPresumingSorted(0));
        assertEquals(0, il.indexOfPresumingSorted(20));
    }

    @Test
    public void testTwoItem() {
        LongListSimple il = new LongListSimple(new long[]{3, 46});
        assertEquals(0, il.nearestIndexToPresumingSorted(1, Bias.FORWARD));
        assertEquals(0, il.nearestIndexToPresumingSorted(2, Bias.FORWARD));
        assertEquals(0, il.nearestIndexToPresumingSorted(3, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(4, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(5, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(44, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(45, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(46, Bias.FORWARD));
        assertEquals(-1, il.nearestIndexToPresumingSorted(47, Bias.FORWARD));

        assertEquals(1, il.nearestIndexToPresumingSorted(47, Bias.BACKWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(48, Bias.BACKWARD));
    }

    @Test
    public void testEndBoundary() {
        LongListSimple il = new LongListSimple(new long[]{10, 23, 70});
        int ix = il.nearestIndexToPresumingSorted(10, Bias.FORWARD);
        assertEquals(0, ix);

        ix = il.nearestIndexToPresumingSorted(30, Bias.FORWARD);
        assertEquals(2, ix);

        ix = il.nearestIndexToPresumingSorted(30, Bias.BACKWARD);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(23, Bias.FORWARD);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(23, Bias.BACKWARD);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(23, Bias.NONE);
        assertEquals(1, ix);
    }

//    @Test
    public void testBenchmark() {
        System.out.println("begin benchmarks");
        long firstLists = benchmarkLists();
        long firstLongLists = benchmarkLongLists();

        System.out.println("warmup array lists: " + firstLists + "ms");
        System.out.println("warmup long lists:  " + firstLongLists + "ms");

        long secondLists = benchmarkLists();
        long secondLongLists = benchmarkLongLists();

        System.out.println("benchmark array lists: " + secondLists + "ms");
        System.out.println("benchmark long lists:  " + secondLongLists + "ms");
    }

    @SuppressWarnings("unchecked")
    private long benchmarkLists() {
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
        }
        long freeMemBefore = Runtime.getRuntime().freeMemory();
        AL al = new AL();
        long time = 0;
        for (int i = 0; i < BENCHMARK_INNER_ITERS; i++) {
            time += benchmarkArrayList(al);
            al.clear();
        }
        long freeMemAfter = Runtime.getRuntime().freeMemory();
        System.out.println("arrayList free mem before " + freeMemBefore);
        System.out.println("arrayList free mem after  " + freeMemAfter);
        System.out.println("arrayList free mem diff " + (freeMemAfter - freeMemBefore));
        return time;
    }

    private long benchmarkLongLists() {
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
        }
        long freeMemBefore = Runtime.getRuntime().freeMemory();

        LongListSimple ll = new LongListSimple(24);
        long time = 0;
        for (int i = 0; i < BENCHMARK_INNER_ITERS; i++) {
            time += benchmarkLongList(ll);
            ll.clear();
        }
        long freeMemAfter = Runtime.getRuntime().freeMemory();
        System.out.println("longList free mem before " + freeMemBefore);
        System.out.println("longList free mem after  " + freeMemAfter);
        System.out.println("longList free mem diff " + (freeMemAfter - freeMemBefore));
        return time;
    }

    static long[][] BMK_ADD_ALL;
    static List<List<Long>> BMK_COLL_ADD_ALL = new ArrayList<>();
    static int[][] RANGES;
    static int AMT = 100;
    static int BENCHMARK_INNER_ITERS = 12;

    static {
        BMK_ADD_ALL = new long[AMT][];
        RANGES = new int[AMT][];
        long val = 1000L;
        long interval = 100;
        for (int i = 100; i < 100 + AMT; i++) {
            int ix = i - 100;
            long[] l = new long[i];
            for (int j = 0; j < i; j++) {
                l[j] = val;
                val += interval;
            }
            BMK_ADD_ALL[ix] = l;
            BMK_COLL_ADD_ALL.add(ArrayUtils.toBoxedList(l));
            RANGES[ix] = new int[]{1, 3};
            Arrays.sort(RANGES[ix]);
        }
    }

    private long benchmarkLongList(LongListSimple ll) {
        long now = System.currentTimeMillis();

        for (int i = 0; i < AMT; i++) {
            ll.addAll(BMK_ADD_ALL[i]);
            if (ll.size() > 128) {
                ll.removeRange(64, 128);
            }
            ll.indexOf(BMK_ADD_ALL[i][1]);
            ll.contains(BMK_ADD_ALL[i][2]);
            ll.contains(-1L);
        }
        for (int i = 0; i < AMT; i++) {
            ll.addAll(64, BMK_ADD_ALL[i]);
            if (ll.size() > 128) {
                ll.removeRange(64, 128);
            }
            ll.indexOf(BMK_ADD_ALL[i][1]);
            ll.contains(BMK_ADD_ALL[i][2]);
            ll.contains(-1L);
        }
        for (int i = 0; i < AMT; i++) {
            int[] rng = RANGES[i];
            ll.removeRange(rng[0], rng[1]);
        }
        int sz = ll.size();
        for (int i = 0; i < sz; i++) {
            long l = ll.getAsLong(i);
        }
        for (int i = 0; i < sz; i++) {
            ll.removeAt(0);
        }
        return System.currentTimeMillis() - now;
    }

    private long benchmarkArrayList(AL<Long> ll) {
        long now = System.currentTimeMillis();

        for (int i = 0; i < AMT; i++) {
            ll.addAll(BMK_COLL_ADD_ALL.get(i));
            if (ll.size() > 128) {
                ll.removeRange(64, 128);
            }
            ll.indexOf(BMK_ADD_ALL[i][1]);
            ll.contains(BMK_ADD_ALL[i][2]);
            ll.contains(-1L);
        }
        for (int i = 0; i < AMT; i++) {
            ll.addAll(64, BMK_COLL_ADD_ALL.get(i));
            if (ll.size() > 128) {
                ll.removeRange(64, 128);
            }
            ll.indexOf(BMK_ADD_ALL[i][1]);
            ll.contains(BMK_ADD_ALL[i][2]);
            ll.contains(-1L);
        }
        for (int i = 0; i < AMT; i++) {
            int[] rng = RANGES[i];
            ll.removeRange(rng[0], rng[1]);
        }
        int sz = ll.size();
        for (int i = 0; i < sz; i++) {
            long x = ll.get(i);
        }
        for (int i = 0; i < sz; i++) {
            ll.remove(0);
        }
        return System.currentTimeMillis() - now;
    }

    private List<Long> range(long a, int count) {
        List<Long> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add((long) a + i);
        }
        return result;
    }

    private AL<Long> arrayList() {
        return arrayList(500);
    }

    private LongListSimple list(int batchSize) {
        return list(batchSize, 500);
    }

    private AL<Long> arrayList(int n) {
        AL<Long> result = new AL<>(n);
        for (long i = 0; i < n; i++) {
            result.add(i);
        }
        return result;
    }

    private LongListSimple list(int batchSize, int n) {
        LongListSimple result = new LongListSimple(batchSize);
        for (long i = 0; i < n; i++) {
            result.add(i);
        }
        return result;
    }

    public static final class AL<T> extends ArrayList<T> {

        public AL(int initialCapacity) {
            super(initialCapacity);
        }

        public AL() {
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
