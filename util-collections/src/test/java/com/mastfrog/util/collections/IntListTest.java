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

        IntListImpl il4 = new IntListImpl(new int[] {
            0,1,2,3,4,5,6,7,8,9
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
}
