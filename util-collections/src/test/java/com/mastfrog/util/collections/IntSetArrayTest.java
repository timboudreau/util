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
package com.mastfrog.util.collections;

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntSetArrayTest {

    @Test
    public void test() {
        IntSet is = new IntSetArray(10);
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
        assertFalse("Should no longer contain 5: " + is, is.contains(5));
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
        IntSet is = new IntSetArray(10);
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
        IntSet is = new IntSetArray(10);
        assertEquals(0, is.size());
        assertTrue(is.isEmpty());
        assertFalse(is.iterator().hasNext());
        assertFalse(is.contains(0));
        assertEquals(-1, is.max());
        assertEquals(0, is.toArray().length);
    }

    @Test
    public void testInterop() {
        IntSet is = new IntSetArray(10);
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
        IntSet is = new IntSetArray(10);
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
        Set<Integer> exclude = new HashSet<>(new IntSetArray().addAll(1, 2, 3, 4));
        IntSet test = new IntSetArray().addAll(1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 23, 24);
        IntSet found = new IntSetImpl();
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
        assertEquals(new IntSetArray(5).addAll(11, 12, 13, 14, 21, 22, 23, 24), found);
        assertNull(test.pick(r, exclude));
    }

    @Test
    public void testPicking() {
        Random r = new Random(10942042);
        IntSet exclude = new IntSetImpl().addAll(1, 2, 3, 4);
        IntSet all = new IntSetArray().addAll(1, 2, 3, 4, 5, 10, 17, 28, 33, 52, 64, 28, 119, 57);
        IntSet got = new IntSetImpl();
        for (int i = 0; i < 5; i++) {
            Integer val = all.pick(r, exclude);
            assertNotNull(val);
            got.add(val);
        }
        assertEquals(IntSet.of(5, 10, 52, 64, 119), got);
    }

    @Test
    public void testNoDuplicates() {
        IntSetArray is = new IntSetArray(20);
        for (int i = 0; i < 10; i++) {
            assertTrue(is.add(i));
            assertFalse("Added " + i + " twice", is.add(i));
        }
        for (int i = 0; i < 10; i++) {
            assertFalse("Added " + i + " again", is.add(i));
        }
        assertEquals(10, is.size());
        int[] arr = is.toIntArray();
        assertEquals(10, arr.length);
        for (int i = 0; i < arr.length; i++) {
            assertEquals(i, arr[i]);
        }
    }

    @Test
    public void testRemove() {
        IntSetArray is = new IntSetArray(20).addAll(3, 2);
        assertEquals(is.size(), 2);
        assertTrue(is.remove(3));
        assertEquals(is.size(), 1);
        assertFalse(is.contains(3));
        assertTrue(is.contains(2));
        is.remove(2);
        assertFalse(is.contains(2));
        assertTrue(is.isEmpty());
    }

    @Test
    public void testRemove2() {
        IntSet is = new IntSetArray(10);
        for (int i = 0; i < 100; i++) {
            assertFalse(is.contains(i * 5));
            is.add(i * 5);
        }
        assertTrue(is.remove(5));
        assertFalse("Should no longer contain 5: " + is, is.contains(5));
        assertFalse(is.remove(5));
    }

    @Test
    public void testConsecutiveItems() {
        testConsecutiveItems(() -> {
            return new IntSetArray(20);
        });
        testConsecutiveItems(() -> {
            return new IntSetImpl(20);
        });
    }

    public void testConsecutiveItems(Supplier<IntSet> setSupplier) {
        IntSet is = setSupplier.get().addAll(
                2, 3,
                5, 6, 7, 8, 9,
                20, 21, 22,
                30,
                40, 41);
        int[] nums = is.toIntArray();
        assertEquals(is.size(), nums.length);
        for (int i = 0; i < nums.length; i++) {
            assertEquals(nums[i], is.valueAt(i));
        }
        Set<IntSet> expected = setOf(
                new IntSetArray(2).addAll(2, 3),
                new IntSetArray(5).addAll(5, 6, 7, 8, 9),
                new IntSetArray(3).addAll(20, 21, 22),
                new IntSetArray(1).addAll(30),
                new IntSetArray(2).addAll(40, 41)
        );
        Set<IntSet> iss = new LinkedHashSet<>();
        int groups = is.visitConsecutiveIndices((int first, int last, int count) -> {
            assertEquals("Wrong count " + count + " for " + first + " to " + last
                    + " in " + is.getClass().getSimpleName() + " of " + is,
                    count, (last - first) + 1);
            IntSet curr = new IntSetImpl(count);
            for (int i = first; i <= last; i++) {
                curr.add(is.valueAt(i));
            }
            iss.add(curr);
        });
        assertEquals(is.getClass().getSimpleName(), 5, groups);
        assertEquals(is.getClass().getSimpleName(), expected, iss);
        iss.clear();
        groups = is.visitConsecutiveIndicesReversed((int first, int last, int count) -> {
            assertEquals("Wrong count " + count + " for " + first + " to " + last
                    + " in " + is.getClass().getSimpleName() + " of " + is,
                    (last - first) + 1, count);
            IntSet curr = new IntSetImpl(count);
            try {
                for (int i = first; i <= last; i++) {
                    curr.add(is.valueAt(i));
                }
            } catch (Exception ex) {
                throw new AssertionError("Exception iterating " + count
                        + " indices from " + first + " to " + last, ex);
            }
            iss.add(curr);
        });
        assertEquals(is.getClass().getSimpleName(), 5, groups);
        assertEquals(expected, iss);
        testConsecutiveItemsCornerCases(setSupplier);
        testConsecutiveItemsSingleLeadingAndTrailing(setSupplier);
        testConsecutiveItemsSingleGroup(setSupplier);
    }

    public void testConsecutiveItemsSingleLeadingAndTrailing(Supplier<IntSet> setSupplier) {
        IntSet is = setSupplier.get().addAll(
                0,
                2, 3, 4, 5, 6, 7, 8, 9,
                23, 24,
                59);
        Set<IntSet> expected = setOf(
                new IntSetImpl().addAll(0),
                new IntSetImpl().addAll(2, 3, 4, 5, 6, 7, 8, 9),
                new IntSetImpl().addAll(23, 24),
                new IntSetImpl().addAll(59)
        );
        Set<IntSet> iss = new LinkedHashSet<>();
        int groups = is.visitConsecutiveIndices((int first, int last, int count) -> {
            assertEquals("Wrong count " + count + " for " + first + " to " + last
                    + " in " + is.getClass().getSimpleName() + " of " + is,
                    count, (last - first) + 1);
            IntSet curr = new IntSetImpl(count);
            for (int i = first; i <= last; i++) {
                curr.add(is.valueAt(i));
            }
            iss.add(curr);
        });
        assertEquals(is.getClass().getSimpleName(), expected, iss);
        assertEquals(is.getClass().getSimpleName(), 4, groups);
        iss.clear();
        groups = is.visitConsecutiveIndicesReversed((int first, int last, int count) -> {
            assertEquals("Wrong count " + count + " for " + first + " to " + last
                    + " in " + is.getClass().getSimpleName() + " of " + is,
                    (last - first) + 1, count);
            IntSet curr = new IntSetImpl(count);
            for (int i = first; i <= last; i++) {
                curr.add(is.valueAt(i));
            }
            iss.add(curr);
        });
        assertEquals(is.getClass().getSimpleName(), expected, iss);
        assertEquals(is.getClass().getSimpleName(), 4, groups);
        expected = new HashSet<>(expected);
        expected.add(new IntSetImpl().addAll(73));
        is.add(73);
        iss.clear();
        groups = is.visitConsecutiveIndices((int first, int last, int count) -> {
            assertEquals("Wrong count " + count + " for " + first + " to " + last
                    + " in " + is.getClass().getSimpleName() + " of " + is,
                    count, (last - first) + 1);
            IntSet curr = new IntSetImpl(count);
            for (int i = first; i <= last; i++) {
                curr.add(is.valueAt(i));
            }
            iss.add(curr);
        });
        assertEquals(is.getClass().getSimpleName(), expected, iss);
        assertEquals(is.getClass().getSimpleName(), 5, groups);
    }

    public void testConsecutiveItemsSingleGroup(Supplier<IntSet> setSupplier) {
        IntSet is = setSupplier.get().addAll(
                2, 3, 4, 5, 6, 7, 8, 9);
        boolean[] called = new boolean[1];
        int groups = is.visitConsecutiveIndices((first, last, count) -> {
            called[0] = true;
            assertEquals(count, is.size());
            assertEquals(0, first);
            assertEquals(is.size() - 1, last);
        });
        assertTrue(is.getClass().getSimpleName(), called[0]);
        assertEquals(is.getClass().getSimpleName(), 1, groups);
        called[0] = false;
        groups = is.visitConsecutiveIndicesReversed((first, last, count) -> {
            assertEquals(count, is.size());
            assertEquals(0, first);
            assertEquals(is.size() - 1, last);
            called[0] = true;
        });
        assertTrue(is.getClass().getSimpleName(), called[0]);
        assertEquals(is.getClass().getSimpleName(), 1, groups);
    }

    public void testConsecutiveItemsCornerCases(Supplier<IntSet> setSupplier) {
        IntSet is = setSupplier.get().addAll(1);
        boolean[] called = new boolean[1];
        is.visitConsecutiveIndices((first, last, count) -> {
            assertEquals("Wrong first " + is.getClass().getSimpleName(), 0, first);
            assertEquals("Wrong last " + is.getClass().getSimpleName(), 0, last);
            assertEquals("Wrong count " + is.getClass().getSimpleName(), 1, count);
            called[0] = true;
        });
        assertTrue(is.getClass().getSimpleName(), called[0]);
        called[0] = false;
        is.visitConsecutiveIndicesReversed((first, last, count) -> {
            assertEquals("Wrong first " + is.getClass().getSimpleName(), 0, first);
            assertEquals("Wrong last " + is.getClass().getSimpleName(), 0, last);
            assertEquals("Wrong count " + is.getClass().getSimpleName(), 1, count);
            called[0] = true;
        });
        assertTrue(is.getClass().getSimpleName(), called[0]);
        called[0] = false;
        is.clear();
        assertTrue(is.getClass().getSimpleName(), is.isEmpty());
        assertEquals(is.getClass().getSimpleName(), 0, is.size());
        is.visitConsecutiveIndices((first, last, count) -> {
            fail("should not be called for empty set: " + is.getClass().getSimpleName()
                    + ": " + first + ", " + last + ", " + count);
        });
        is.visitConsecutiveIndicesReversed((first, last, count) -> {
            fail("should not be called for empty set: " + is.getClass().getSimpleName()
                    + ": " + first + ", " + last + ", " + count);
        });
    }

    @Test
    public void testRemoveAll() {
        Supplier<IntSet> arrSupplier = IntSetArray::new;
        Supplier<IntSet> bitsSupplier = IntSetImpl::new;
        testRemoveAll(bitsSupplier, bitsSupplier);
        testRemoveAll(bitsSupplier, arrSupplier);
        testRemoveAll(arrSupplier, arrSupplier);
        testRemoveAll(arrSupplier, bitsSupplier);
    }

    private void testRemoveAll(Supplier<IntSet> fromSupplier, Supplier<IntSet> srcSupplier) {
        IntSet is = fromSupplier.get();
        IntSet rem = srcSupplier.get();
        IntSet expect = fromSupplier.get();
        String msgBase = is.getClass().getSimpleName() + " / "
                + rem.getClass().getSimpleName() + ": ";
        for (int i = 0; i < 100; i++) {
            is.add(i);
            int tens = i / 10;
            if (tens % 2 != 0) {
                rem.add(i);
            } else {
                expect.add(i);
            }
            assertTrue(is.contains(i));
        }
        boolean altered = is.removeAll(rem);
        assertEquals(msgBase, expect, is);
        assertTrue(msgBase + " returned false from removeAll() but did alter the set",
                altered);

        IntSet r2 = srcSupplier.get().addAll(1000, 1001, 1002);
        assertFalse(msgBase, is.removeAll(r2));

        IntSet is2 = fromSupplier.get();
        IntSet rem2 = srcSupplier.get();
        IntSet expect2 = fromSupplier.get();

        for (int i = 0; i < 100; i++) {
            is2.add(i);
            if (i % 2 == 0) {
                rem2.add(i);
            } else {
                expect2.add(i);
            }
        }
        altered = is2.removeAll(rem2);
        assertEquals(msgBase, expect2, is2);
        assertTrue(msgBase, altered);
    }

    @Test
    public void testAddAll() {
        Supplier<IntSet> arrSupplier = IntSetArray::new;
        Supplier<IntSet> bitsSupplier = IntSetImpl::new;
        testAddAll(arrSupplier, arrSupplier);
        testAddAll(bitsSupplier, bitsSupplier);
        testAddAll(arrSupplier, bitsSupplier);
        testAddAll(bitsSupplier, arrSupplier);
    }

    private void testAddAll(Supplier<IntSet> aSupp, Supplier<IntSet> bSupp) {
        IntSet a = aSupp.get();
        IntSet b = bSupp.get();
        String msg = a.getClass().getSimpleName() + " / " + b.getClass().getSimpleName() + ": ";
        IntSet nue = aSupp.get();
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 0) {
                a.add(i);
            } else {
                b.add(i);
            }
        }
        boolean added = nue.addAll(a);
        assertTrue(added);
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 0) {
                assertTrue(msg + i, nue.contains(i));
            } else {
                assertFalse(msg + i, nue.contains(i));
            }
        }

        added = nue.addAll(b);
        assertTrue(added);
        for (int i = 0; i < 100; i++) {
            assertTrue(msg + i, nue.contains(i));
        }

        added = a.addAll(b);
        assertTrue(added);
        for (int i = 0; i < 100; i++) {
            assertTrue(msg + i, a.contains(i));
        }
        added = b.addAll(a);

        for (int i = 0; i < 100; i++) {
            assertTrue(msg + i, b.contains(i));
        }
        assertTrue(added);
    }

    @Test
    public void testRetainAll() {
        Supplier<IntSet> arrSupplier = IntSetArray::new;
        Supplier<IntSet> bitsSupplier = IntSetImpl::new;
        testRetainAll(arrSupplier, arrSupplier);
        testRetainAll(bitsSupplier, bitsSupplier);
        testRetainAll(arrSupplier, bitsSupplier);
        testRetainAll(bitsSupplier, arrSupplier);
    }

    private void testRetainAll(Supplier<IntSet> aSupp, Supplier<IntSet> bSupp) {
        IntSet target = aSupp.get();
        IntSet src = bSupp.get();
        String msg = target.getClass().getSimpleName() + " / " + src.getClass().getSimpleName() + ": ";
        for (int i = 0; i < 100; i++) {
            target.add(i);
            if (i % 2 == 0 || i % 3 == 0) {
                src.add(i);
            }
        }
        assertEquals(msg, 100, target.size());
        target.retainAll(src);
        assertEquals(msg, src, target);
    }
}
