package com.mastfrog.util.collections;

import com.mastfrog.util.search.Bias;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        if (true) {
            return;
        }
        IntListImpl il = new IntListImpl(new int[]{1, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 47, 49, 51, 53, 55});
        //                                         0  1  2  3  4   5   6   7
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
}
