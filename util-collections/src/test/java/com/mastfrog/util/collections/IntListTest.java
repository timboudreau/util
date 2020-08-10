package com.mastfrog.util.collections;

import com.mastfrog.util.search.Bias;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntListTest {

    private List<Integer> real = new ArrayList<>();
    private IntListImpl test = new IntListImpl(5);

    @Test
    public void testBasic() {
        addOne(3);
        addOne(5);
        addOne(7);
        addOne(9);
        addMany(11, 13, 15, 17, 19);
        addMany2(21, 23, 25, 27);
        test(a -> {
            boolean result = a.contains(7);
            assertTrue(real.toString(), result);
            return result;
        }, b -> {
            boolean result = b.contains(7);
            assertTrue(test.toString(), result);
            return result;
        });
        remove(2);
        test(a -> {
            boolean result = a.contains(7);
            assertFalse(test.toString(), result);
            return result;
        }, b -> {
            boolean result = b.contains(7);
            assertFalse(test.toString(), result);
            return result;
        });
        insertAt(72, 2);
        insertAt(73, 2);
        clear();
        assertTrue(test.isEmpty());
        assertEquals(0, test.size());
    }

    private void remove(int at) {
        check((a, b) -> {
            a.remove(at);
            b.removeAt(at);
        });
    }

    private void insertAt(int value, int at) {
        check((a, b) -> {
            a.add(at, value);
            b.add(at, value);
        });
    }

    private void addMany(int... val) {
        check((a, b) -> {
            List<Integer> l = new ArrayList<>(val.length);
            for (int i = 0; i < val.length; i++) {
                l.add(val[i]);
            }
            a.addAll(l);
            b.addArray(val);
        });
    }

    private void addMany2(int... val) {
        check((a, b) -> {
            List<Integer> l = new ArrayList<>(val.length);
            for (int i = 0; i < val.length; i++) {
                l.add(val[i]);
            }
            a.addAll(l);
            b.addAll(val);
        });
    }

    private void addOne(int val) {
        check((a, b) -> {
            a.add(val);
            b.add(val);
        });
    }

    private void clear() {
        check((a, b) -> {
            a.clear();
            b.clear();
        });
    }

    void test(Predicate<List<Integer>> a, Predicate<IntListImpl> b) {
        test((a1, b1) -> a.test(a1) == b.test(b1));
    }

    void test(BiPredicate<List<Integer>, IntListImpl> tester) {
        assertTrue(tester.test(real, test));
    }

    void check(BiConsumer<List<Integer>, IntListImpl> manipulation) {
        manipulation.accept(real, test);
        assertEquals(test.toString() + " expected " + real, real.size(), test.size());
        assertEquals(test.toString(), real, test);
        assertEquals(test.toString(), real.hashCode(), test.hashCode());
    }

    @Test
    public void testSort() {
        int[] exp = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] toUse = ArrayUtils.copyOf(exp);
        ArrayUtils.shuffle(new Random(130290329), toUse);
        assertFalse(Arrays.equals(exp, toUse));
        IntListImpl impl = new IntListImpl(toUse);
        impl.sort();
        int[] got = impl.toIntArray();
        assertArrayEquals(exp, got);
    }

    @Test
    public void testSearchCornerCases() {
        IntListImpl il2 = new IntListImpl(new int[]{22});
        assertEquals(0, il2.nearestIndexToPresumingSorted(25, Bias.BACKWARD));
        assertEquals(-1, il2.nearestIndexToPresumingSorted(3, Bias.BACKWARD));
        assertEquals(-1, il2.nearestIndexToPresumingSorted(25, Bias.FORWARD));
        assertEquals(0, il2.nearestIndexToPresumingSorted(3, Bias.FORWARD));

        IntListImpl il = new IntListImpl();
        assertEquals(-1, il.nearestIndexToPresumingSorted(25, Bias.FORWARD));
        assertEquals(-1, il.nearestIndexToPresumingSorted(25, Bias.BACKWARD));

        IntListImpl il3 = new IntListImpl(new int[]{10, 20});
        assertEquals(0, il3.nearestIndexToPresumingSorted(15, Bias.BACKWARD));
        assertEquals(1, il3.nearestIndexToPresumingSorted(15, Bias.FORWARD));

        assertEquals(0, il3.nearestIndexToPresumingSorted(3, Bias.FORWARD));
        assertEquals(-1, il3.nearestIndexToPresumingSorted(3, Bias.BACKWARD));

        assertEquals(1, il3.nearestIndexToPresumingSorted(27, Bias.BACKWARD));
        assertEquals(-1, il3.nearestIndexToPresumingSorted(27, Bias.FORWARD));

        IntListImpl il4 = new IntListImpl(new int[]{
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
    public void testFuzzySearch() {
        int[] vals = new int[20];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = i * 10;
        }
        IntListImpl impl = new IntListImpl(vals);
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
                int bwdTarget = expectedValue + j;
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
        IntListImpl il = new IntListImpl(new int[]{0, 1, 10, 11, 20, 21, 30, 31, 40, 41});
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
    public void testBug() {
        IntListImpl il = new IntListImpl(new int[]{11, 19, 27, 35});
        assertEquals(1, il.nearestIndexToPresumingSorted(20, Bias.BACKWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(21, Bias.BACKWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(18, Bias.FORWARD));
        assertEquals(1, il.nearestIndexToPresumingSorted(17, Bias.FORWARD));
    }

    @Test
    public void testBug2() {
        IntListImpl il = new IntListImpl(new int[]{1, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 47, 49, 51, 53, 55});
        int first = il.nearestIndexToPresumingSorted(11, Bias.FORWARD);
        assertEquals(5, first);

    }

    @Test
    public void testLargeList() {
        IntListImpl il = new IntListImpl(new int[]{1, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 47, 49, 51, 53, 55});
        //                                         0  1  2  3  4   5   6   7
        Random rnd = new Random(23901309);
        for (int x = 0; x < 7; x++) {
            for (int i = 1; i < il.size() - 2; i++) {
                int val = il.get(i);
                int prev = il.get(i - 1);
                int next = il.get(i + 1);
                for (int j = prev + 1; j <= val; j++) {
                    assertEquals("Wrong index seeking " + j + " in " + il + " FORWARD", i, il.nearestIndexToPresumingSorted(j, Bias.FORWARD));
                    if (j < val) {
                        assertEquals("Wrong index seeking " + j + " in " + il + " BACKWARD", i - 1, il.nearestIndexToPresumingSorted(j, Bias.BACKWARD));
                    } else {
                        assertEquals("Wrong index seeking " + j + " in " + il + " BACKWARD", i, il.nearestIndexToPresumingSorted(j, Bias.BACKWARD));
                    }
                }
            }
            int f = il.first();
            for (int i = 0; i < f - 1; i++) {
                assertEquals(0, il.nearestIndexToPresumingSorted(i, Bias.FORWARD));
                assertEquals(-1, il.nearestIndexToPresumingSorted(i, Bias.BACKWARD));
            }
            int l = il.last();
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.FORWARD));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.BACKWARD));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.NEAREST));
            assertEquals(il.size() - 1, il.nearestIndexToPresumingSorted(l, Bias.NONE));
            for (int i = l + 1; i < l + 10; i++) {
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
        IntListImpl il = new IntListImpl(new int[]{2, 5, 13, 21, 29, 37});
        //                                         0  1   2   3   4   5
        int ix = il.nearestIndexToPresumingSorted(48, Bias.BACKWARD);
        assertEquals(5, ix);

        ix = il.nearestIndexToPresumingSorted(0, Bias.FORWARD);
        assertEquals(0, ix);
    }

    @Test
    public void testSortedIndex() {
        IntListImpl il = new IntListImpl(new int[]{20});
        assertEquals(-1, il.indexOfPresumingSorted(0));
        assertEquals(0, il.indexOfPresumingSorted(20));
    }

    @Test
    public void testTwoItem() {
        IntListImpl il = new IntListImpl(new int[]{3, 46});
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
        IntListImpl il = new IntListImpl(new int[]{10, 23, 70});
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

    @Test
    public void testDuplicateToleranceWithLargeNumbersOfDuplicates() {
        for (int i = 2; i < 20; i++) {
            IntListImpl il = new IntListImpl(50);
            il.add(10);
            for (int j = 0; j < i; j++) {
                il.add(23);
            }
            il.add(70);

            int ix = il.nearestIndexToPresumingSorted(10, Bias.FORWARD);
            assertEquals(0, ix);

            ix = il.nearestIndexToPresumingSorted(30, Bias.FORWARD);
            assertEquals(il.size() - 1, ix);

            ix = il.nearestIndexToPresumingSorted(30, Bias.BACKWARD);
            assertEquals(il.size() - 2, ix);

            ix = il.nearestIndexToPresumingSorted(23, Bias.FORWARD);
            assertEquals(il.size() - 2, ix);

            ix = il.nearestIndexToPresumingSorted(23, Bias.BACKWARD);
            assertEquals(il.size() - 2, ix);

            ix = il.nearestIndexToPresumingSorted(23, Bias.NONE);
            assertEquals(il.size() - 2, ix);

        }
    }

    @Test
    public void testDuplicateTolerance() {
        // In the case of duplicates, the highest index should always be
        // returned.
        IntListImpl il = new IntListImpl(new int[]{10, 10, 20, 30, 30, 30, 40, 50, 50});
        //                                           0   1   2   3   4   5   6   7, 8
        int ix = il.nearestIndexToPresumingSorted(12, Bias.BACKWARD);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(12, Bias.FORWARD);
        assertEquals(2, ix);

        ix = il.nearestIndexToPresumingSorted(10, Bias.NONE);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(21, Bias.BACKWARD);
        assertEquals(2, ix);

        ix = il.nearestIndexToPresumingSorted(0, Bias.FORWARD);
        assertEquals(1, ix);

        ix = il.nearestIndexToPresumingSorted(25, Bias.FORWARD);
        assertEquals(5, ix);

        ix = il.nearestIndexToPresumingSorted(35, Bias.BACKWARD);
        assertEquals(5, ix);

        ix = il.nearestIndexToPresumingSorted(35, Bias.FORWARD);
        assertEquals(6, ix);

        ix = il.nearestIndexToPresumingSorted(45, Bias.FORWARD);
        assertEquals(8, ix);

        ix = il.nearestIndexToPresumingSorted(55, Bias.BACKWARD);
        assertEquals(8, ix);

        ix = il.nearestIndexToPresumingSorted(55, Bias.FORWARD);
        assertEquals(-1, ix);

//        ix = il.nearestIndexToPresumingSorted(11, Bias.NONE);
//        assertEquals(-1, ix);
    }

    @Test
    public void testGets() {
        IntList il = new IntListImpl(100);
        for (int i = 1; i < 100; i++) {
            il.add(i * 10);
        }
        assertEquals(99, il.size());
        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
            assertEquals("" + val, i, il.indexOf(val));
        }
        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
            assertEquals((i + 1) * 10, val);
            assertEquals("" + i, i, il.nearestIndexToPresumingSorted(val, Bias.NONE));
        }
        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
            assertEquals("" + val, i, il.nearestIndexToPresumingSorted((val) + 1, Bias.BACKWARD));
        }

        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
            assertEquals("" + val, i, il.nearestIndexToPresumingSorted((val) - 1, Bias.FORWARD));
        }

        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
//            assertEquals("Request for " + (val - 1) + " in " + il + " with bias none should get -1",
//                    -1, il.nearestIndexToPresumingSorted(val - 1, Bias.NONE));
            assertTrue("Request for " + (val - 1) + " in " + il + " with bias none should get -1",
                    il.nearestIndexToPresumingSorted(val - 1, Bias.NONE) < 0);
        }

        for (int i = 0; i < il.size(); i++) {
            int val = il.get(i);
//            assertEquals("" + val, -1, il.nearestIndexToPresumingSorted((val) + 1, Bias.NONE));
            assertTrue("" + val, il.nearestIndexToPresumingSorted((val) + 1, Bias.NONE) < 0);
        }
    }

    @Test
    public void testSwap() {
        IntListImpl ili = new IntListImpl(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertTrue(ili.swap(9, 1));
        assertEquals(9, ili.getAsInt(1));
        assertEquals(1, ili.getAsInt(9));
        assertTrue(ili.swap(0, 10));
        assertEquals(10, ili.getAsInt(0));
        assertEquals(0, ili.getAsInt(10));
        assertFalse(ili.swap(1, 1));
        assertEquals(9, ili.getAsInt(1));
        assertArrayEquals(new int[]{10, 9, 2, 3, 4, 5, 6, 7, 8, 1, 0}, ili.toIntArray());
        assertFalse(ili.swap(1, 13));
        assertFalse(ili.swap(13, 13));
        assertFalse(ili.swap(14, 13));
    }

    @Test
    public void testSwapIndices() {
        IntListImpl ili = new IntListImpl(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertEquals(11, ili.size());
        assertTrue(ili.swapIndices(9, 1));
        assertEquals(9, ili.getAsInt(1));
        assertEquals(1, ili.getAsInt(9));
        assertEquals(11, ili.size());
        assertTrue(ili.swapIndices(0, 10));
        assertEquals(10, ili.getAsInt(0));
        assertEquals(0, ili.getAsInt(10));
        assertFalse(ili.swapIndices(1, 1));
        assertEquals(11, ili.size());
        assertEquals(9, ili.getAsInt(1));
        assertArrayEquals(new int[]{10, 9, 2, 3, 4, 5, 6, 7, 8, 1, 0}, ili.toIntArray());
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
    public void testToBack() {
        IntListImpl ili = new IntListImpl(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertEquals(11, ili.size());
        assertTrue(ili.toBack(11));
        assertEquals(12, ili.size());
        assertEquals(11, ili.getAsInt(11));
        assertTrue(ili.toBack(0));
        assertEquals(12, ili.size());
        assertEquals(0, ili.getAsInt(11));
        assertEquals(11, ili.getAsInt(10));
        assertEquals(1, ili.getAsInt(0));
        assertTrue(ili.toBack(5));
        assertEquals(12, ili.size());
        assertEquals(5, ili.getAsInt(11));
        assertEquals(0, ili.getAsInt(10));
        assertEquals(11, ili.getAsInt(9));
        assertEquals(1, ili.getAsInt(0));
        assertEquals(11, ili.indexOf(5));
        assertFalse(ili.toBack(5));
        assertArrayEquals(new int[]{1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 0, 5}, ili.toIntArray());
    }

    @Test
    public void testToFront() {
        IntListImpl ili = new IntListImpl(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        assertTrue(ili.toFront(10));
        assertEquals(ili.toString(), 10, ili.getAsInt(0));
        assertEquals(ili.toString(), 9, ili.getAsInt(10));
        assertEquals(ili.toString(), 0, ili.indexOf(10));
        assertEquals(ili.toString(), 10, ili.indexOf(9));
        assertArrayEquals(ili.toString(), new int[]{10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, ili.toIntArray());
        assertFalse(ili.toString(), ili.toFront(10));
        assertArrayEquals(ili.toString(), new int[]{10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, ili.toIntArray());
        assertFalse(ili.toFront(11, false));
        assertTrue(ili.toFront(11, true));
        assertEquals(ili.toString(), 12, ili.size());
        assertEquals(ili.toString(), 11, ili.getAsInt(0));
        assertEquals(ili.toString(), 0, ili.indexOf(11));
        assertArrayEquals(ili.toString(), new int[]{11, 10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, ili.toIntArray());
        assertFalse(ili.toString(), ili.toFront(11));
        assertTrue(ili.toString(), ili.toFront(5));
        assertArrayEquals(ili.toString(), new int[]{5, 11, 10, 0, 1, 2, 3, 4, 6, 7, 8, 9}, ili.toIntArray());
        assertTrue(ili.toFront(6));
        assertArrayEquals(ili.toString(), new int[]{6, 5, 11, 10, 0, 1, 2, 3, 4, 7, 8, 9}, ili.toIntArray());
        assertTrue(ili.toFront(0));
        assertArrayEquals(ili.toString(), new int[]{0, 6, 5, 11, 10, 1, 2, 3, 4, 7, 8, 9}, ili.toIntArray());
    }

    @Test
    public void testAddEmptyAtZero() {
        IntListImpl ili = new IntListImpl(5);
        assertEquals(0, ili.size());
        ili.add(0, 23);
        assertEquals(1, ili.size());
        ili.clear();
        try {
            ili.add(23, 23);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
    }

    @Test
    public void testStartsWith() {
        Random rnd = new Random(2309);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            IntList a = IntList.create(sz);
            for (int j = 0; j < sz; j++) {
                a.add(rnd.nextInt());
            }
            for (int j = 1; j < sz - 1; j++) {
                IntList sub = a.subList(0, j);
                List<Integer> reg = new ArrayList<>(sub);
                assertEquals(reg, sub);
                assertEquals(sub, reg);
                assertEquals(reg.hashCode(), sub.hashCode());
                assertNotEquals(a, sub);
                assertTrue(a.startsWith(sub));
                assertFalse(sub.startsWith(a));
                assertTrue(sub instanceof IntListImpl);
                IntListImpl iil = (IntListImpl) sub;
                for (int k = 0; k < j; k++) {
                    iil.set(k, iil.get(k) + 1);
                    assertFalse(a.startsWith(sub));
                }
                assertTrue(a.startsWith(reg));
            }
            assertFalse(a.startsWith(IntList.create(1)));
        }
    }

    @Test
    public void testEndsWith() {
        Random rnd = new Random(2309);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            IntList a = IntList.create(sz);
            for (int j = 0; j < sz; j++) {
                a.add(rnd.nextInt());
            }
            for (int j = 1; j < sz - 1; j++) {
                IntList sub = a.subList(a.size() - j, a.size());
                assertEquals("subList(" + (a.size() - j)
                        + "," + a.size() + " should return a list of size "
                        + j + " but got size " + sub.size() + ".\nOrig list: "
                        + a + "\nSub list:" + sub, j, sub.size());
                List<Integer> reg = new ArrayList<>(sub);
                assertEquals(reg, sub);
                assertEquals(sub, reg);
                assertEquals(reg.hashCode(), sub.hashCode());
                assertNotEquals(a, sub);
                assertTrue(a.endsWith(sub));
                assertFalse(sub.endsWith(a));
                assertTrue(sub instanceof IntListImpl);
                IntListImpl iil = (IntListImpl) sub;
                for (int k = 0; k < j; k++) {
                    iil.set(k, iil.get(k) + 1);
                    assertFalse(a.endsWith(sub));
                }
                assertTrue(a.endsWith(reg));
                IntList sub2 = a.subList(0, j);
                if (!sub.equals(sub2)) { // can happen if all one value
                    assertFalse(a + " should not also end with " + sub2,
                            a.endsWith(sub2));
                }
            }
            assertFalse("endsWith(List) should return false for the empty list",
                    a.endsWith(Collections.emptyList()));
            assertFalse("endsWith(IntList) should return false for the empty list.",
                    a.endsWith(IntList.create(1)));
        }
    }

    @Test
    public void testRemoveRange() {
        Random rnd = new Random(613451);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            IntList a = IntList.create(sz);
            for (int j = 0; j < sz; j++) {
                a.add(rnd.nextInt());
            }
            for (int start = 0; start < sz - 1; start++) {
                for (int end = start; end <= sz; end++) {
                    IntListImpl copy = (IntListImpl) a.copy();
                    assertEquals(a, copy);
                    assertNotSame(a, copy);
                    II comparison = new II(copy);
                    assertEquals(comparison, copy);
                    assertEquals(copy, comparison);
                    comparison.removeRange(start, end);
                    copy.removeRange(start, end);
                    assertEquals("After removing " + start + ":" + end
                            + " of " + a.size()
                            + " from " + a + "\nshould have\n" + comparison
                            + "\nnot\n" + copy + "\n" + "Lengths: "
                            + comparison.size() + " vs " + copy.size() + "\n", comparison, copy);
                    assertArrayEquals("Array values do not match but equality does.",
                            copy.toIntArray(), comparison.toIntArray());

                    IntListImpl copy2 = (IntListImpl) a.copy();
                    int sz2 = copy2.size();
                    for (int j = end - 1; j >= start; j--) {
                        copy2.removeAt(j);
                        assertEquals(sz2 - 1, copy2.size());
                        sz2--;
                    }
                    assertEquals(copy, copy2);
                    assertEquals(comparison, copy2);
                }
            }
        }
    }

    @Test
    public void testRemoveValue() {
        Random rnd = new Random(613451);
        for (int i = 0; i < 100; i++) {
            int sz = rnd.nextInt(20) + 10;
            IntList a = IntList.create(sz);
            int base = 0;
            for (int j = 0; j < sz; j++) {
                int val = base + rnd.nextInt(10000);
                a.add(val);
            }
            for (int j = 0; j < a.size(); j++) {
                IntList copy = a.copy();
                List<Integer> comparison = new ArrayList<>(copy);
                assertEquals(comparison, copy);
                int toRemove = a.get(j);
                copy.removeValue(toRemove);
                comparison.remove(Integer.valueOf(toRemove));
                assertEquals("Remove of " + toRemove + " from " + a
                        + " got different results", comparison, copy);

                assertEquals("Removing a value not present should return -1",
                        -1, copy.removeValue(-1));
            }
        }
    }

    @Test
    public void testAddCornerCase() {
        int[] arr = new int[]{-5, 0, 2, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45,
            50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105};

        IntListImpl ll = new IntListImpl(arr);
        List<Integer> al = new ArrayList<>(ArrayUtils.toBoxedList(arr));
        assertArrayEquals(arr, ll.toIntArray());
        ll.add(8, 23);
        al.add(8, 23);
        assertEquals(23L, ll.getAsInt(8));
        assertEquals(ll, al);
        assertEquals(al, ll);
    }

    @Test
    public void testAddAllCornerCase1() throws Exception {
        // inserting [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11] at 1 in [0, 1, 2, 3, 4]
        IntListImpl ll = new IntListImpl(5);
        ll.addAll(Arrays.asList(1, 2, 3, 4));
        ll.addAll(0, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        List<Integer> exp = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2, 3, 4);
        assertEquals(exp, ll);
        ll.sort();
        assertEquals(ArrayUtils.toBoxedList(new int[]{1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9, 10, 11}), ll);
        ll.toString();
        IntSet set = ll.toSet();
        assertTrue(set.containsAll(ll));
        assertTrue(ll.containsAll(set));
    }

    static class II extends ArrayList<Integer> {

        II(IntList il) {
            super(il);
        }

        @Override
        public void removeRange(int fromIndex, int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }

        public int[] toIntArray() {
            int[] result = new int[size()];
            for (int i = 0; i < size(); i++) {
                result[i] = get(i);
            }
            return result;
        }
    }
}
