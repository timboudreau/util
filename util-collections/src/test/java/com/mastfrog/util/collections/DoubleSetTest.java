package com.mastfrog.util.collections;

import com.mastfrog.util.collections.DoubleSetImpl;
import com.mastfrog.util.search.Bias;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class DoubleSetTest {

    @Test
    public void testEmptyIdentity() {
        DoubleSet set = DoubleSet.create(10);
        assertEquals(set.hashCode(), DoubleSet.emptyDoubleSet().hashCode());
        assertEquals(DoubleSet.emptyDoubleSet(), set);
        assertEquals(set, DoubleSet.emptyDoubleSet());
    }

    @Test
    public void testWrapperEquality() {
        double[] vals = new double[]{500, 600, 700, 100, 200, 300, 400};
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        assertEquals(set.hashCode(), set.toReadOnlyCopy().hashCode());
        assertEquals(set.hashCode(), set.toSynchronizedSet().hashCode());
        assertEquals(set, set.toSynchronizedSet());
        assertEquals(set, set.toReadOnlyCopy());
        assertEquals(set.copy(), set.toReadOnlyCopy());
        assertEquals(set.copy(), set.toSynchronizedSet());
    }

    @Test
    public void testRemoveRanges() {
        double[] vals = new double[]{
            5, 10, 15, 20, 25, 30, 35, 40
//          0,  1,  2,  3,  4,  5,  6,  7
        };
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        IntSet toRemove = IntSet.create(vals.length);
        toRemove.add(1);
        toRemove.add(2);
        toRemove.add(3);
        set.removeIndices(toRemove);
        assertEquals("Should have shrunk by 3: " + set, vals.length - 3, set.size());

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }

        set = DoubleSetImpl.ofDoubles(vals);
        toRemove.add(6);
        set.removeIndices(toRemove);

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }

        set = DoubleSetImpl.ofDoubles(vals);
        toRemove.add(5);
        set.removeIndices(toRemove);

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }

        set = DoubleSetImpl.ofDoubles(vals);
        toRemove.add(4);
        set.removeIndices(toRemove);

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }

        set = DoubleSetImpl.ofDoubles(vals);
        toRemove.add(7);
        set.removeIndices(toRemove);

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }

        set = DoubleSetImpl.ofDoubles(vals);
        toRemove.add(0);
        set.removeIndices(toRemove);

        for (int i = 0; i < vals.length; i++) {
            if (toRemove.contains(i)) {
                assertFalse(set.contains(vals[i]));
            } else {
                assertTrue(set.contains(vals[i]));
            }
        }
    }

    @Test
    public void testRemoveByIndex() {
        double[] vals = new double[]{
            5, 10, 15, 20, 25, 30, 35, 40
//          0,  1,  2,  3,  4,  5,  6,  7
        };
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        for (int i = 0; i < vals.length; i++) {
            assertTrue(set.contains(vals[i]));
        }
        set.removeIndex(6);
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != 35) {
                assertTrue(set.contains(vals[i]));
            } else {
                assertFalse(set.contains(vals[i]));
            }
        }
        assertEquals(set.size(), vals.length - 1);
        set.removeIndex(4);

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != 35 && vals[i] != 25) {
                assertTrue("Set " + set + " should no longer contain " + vals[i] + " but says it does",
                        set.contains(vals[i]));
            } else {
                assertFalse("Set " + set + " should not contain " + vals[i] + " but says it does", set.contains(vals[i]));
            }
        }
        assertEquals(set.size(), vals.length - 2);
        set.removeIndex(2);

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != 35 && vals[i] != 25 && vals[i] != 15) {
                assertTrue(set.contains(vals[i]));
            } else {
                assertFalse(set.contains(vals[i]));
            }
        }
        assertEquals(set.size(), vals.length - 3);

        set.removeIndex(0);
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != 35 && vals[i] != 25 && vals[i] != 15 && vals[i] != 5) {
                assertTrue(set.contains(vals[i]));
            } else {
                assertFalse(set.contains(vals[i]));
            }
        }
        assertEquals(set.size(), vals.length - 4);

        set.removeIndex(0);
        assertEquals(set.size(), vals.length - 5);
        assertFalse(set.contains(10));
        assertEquals(20D, set.getAsDouble(0), 0.00000000000001D);

        double val = set.greatest();
        assertEquals(40D, val, 0.00001);
        set.removeIndex(set.size - 1);
        assertFalse(set.contains(40D));
    }

    @Test
    public void testSimple() {
        double[] vals = new double[]{500, 600, 700, 100, 200, 300, 400};
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        assertEquals(100D, set.least(), 0.000001);
        assertEquals(700D, set.greatest(), 0.000001);
        assertNotNull(set);
        assertFalse(set.isEmpty());
        assertEquals(vals.length, set.size());
        double[] sorted = Arrays.copyOf(vals, vals.length);
        Arrays.sort(sorted);
        assertArrayEquals(set.toString(), sorted, set.toDoubleArray(), 0.0000001);
        double[] v2 = new double[vals.length];
        int[] cursor = new int[1];
        set.forEachDouble(v -> {
            v2[cursor[0]++] = v;
        });
        assertArrayEquals(set.toString(), sorted, v2, 0.0000001);
        double[] v3 = new double[vals.length];
        cursor[0] = 0;
        for (double d : set) {
            v3[cursor[0]++] = d;
        }
        assertArrayEquals(set.toString(), sorted, v3, 0.0000001);

        for (int i = 0; i < vals.length; i++) {
            assertTrue("Missing " + vals[i] + " in " + set, set.contains(vals[i]));
        }

        for (int i = 1; i < sorted.length; i++) {
            double prev = sorted[i - 1];
            double curr = sorted[i];
            assertTrue(set.contains(prev));
            assertTrue(set.contains(curr));
            assertFalse(set.contains(prev + 1));
            assertFalse(set.contains(prev - 1));
            assertNearest(set, prev, prev + 49);
            assertNearest(set, prev, prev + 1);
            assertNearest(set, prev, prev - 1);
            assertNearest(set, prev, prev - 49);

            assertNearest(set, curr, prev + 51);
            assertNearest(set, curr, prev + 99);
        }
        set.add(0);
        double[] exp = new double[]{0, 100, 200, 300, 400, 500, 600, 700};

        for (int i = 1; i < sorted.length; i++) {
            double prev = sorted[i - 1];
            double curr = sorted[i];
            assertNearest(set, prev, prev + 49);
            assertNearest(set, prev, prev + 1);
            assertNearest(set, prev, prev - 1);
            assertNearest(set, prev, prev - 49);

            assertNearest(set, curr, prev + 51);
            assertNearest(set, curr, prev + 99);
        }
        assertArrayEquals(exp, set.toDoubleArray(), 0.0000001);
    }

    private void assertNearest(DoubleSetImpl set, double expected, double nearTo) {
        double val = set.nearestValueTo(nearTo);
        assertEquals("Nearest value to " + nearTo + " should be " + expected
                + " in " + set, expected, val, 0.0000001);
    }

    @Test
    public void testDedup() {
        double[] vals = new double[]{500, 600, 700, 100, 100, 200, 300, 400, 500};
        double[] exp = new double[]{100, 200, 300, 400, 500, 600, 700};
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        assertArrayEquals(set.toString(), exp, set.toDoubleArray(), 0.0000001);

        set.add(300);
        assertArrayEquals(set.toString(), exp, set.toDoubleArray(), 0.0000001);
    }

    @Test
    public void testDedupWithTrailingDuplicates() {
        double[] vals = new double[]{100, 200, 300, 400, 500, 600, 700, 700};
        double[] exp = new double[]{100, 200, 300, 400, 500, 600, 700};
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(vals);
        assertArrayEquals(set.toString(), exp, set.toDoubleArray(), 0.0000001);
    }

    @Test
    public void testEmpty() {
        DoubleSetImpl set = DoubleSetImpl.ofDoubles();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertArrayEquals(new double[0], set.toDoubleArray(), 0.0000001);
        assertFalse(set.contains(0));
        assertEquals(Double.MIN_VALUE, set.nearestValueTo(1), 0.0000001);
        assertEquals(Double.MIN_VALUE, set.nearestValueTo(0), 0.0000001);
        for (Bias b : Bias.values()) {
            assertEquals(-1, set.nearestIndexTo(0, b));
            assertEquals(-1, set.nearestIndexTo(1, b));
            assertEquals(-1, set.nearestIndexTo(-1, b));
            assertEquals(-1, set.nearestIndexTo(Double.MIN_VALUE, b));
            assertEquals(-1, set.nearestIndexTo(Double.MAX_VALUE, b));
        }
    }

    @Test
    public void testSingle() {
        double v = 2.1247D;
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(1, v);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertArrayEquals(new double[]{v}, set.toDoubleArray(), 0.0000001);
        assertFalse(set.contains(0));
        assertTrue(set.contains(v));
        assertEquals(0, set.indexOf(v));
        assertEquals(v, set.nearestValueTo(1), 0.0000001);
        assertEquals(v, set.nearestValueTo(0), 0.0000001);
        assertEquals(v, set.nearestValueTo(1000), 0.0000001);
        assertEquals(v, set.nearestValueTo(Double.MIN_VALUE), 0.0000001);
        assertEquals(v, set.nearestValueTo(Double.MAX_VALUE), 0.0000001);
    }

    @Test
    public void testGrowAndPartition() {
        DoubleSetImpl set = new DoubleSetImpl(3);
        double[] d = new double[100];
        for (int i = 0; i < 100; i++) {
            set.add(100 - i);
            d[i] = 100 - i;
        }
        Arrays.sort(d);
        assertEquals(d.length, set.size());
        for (double dd : d) {
            assertTrue(dd + " not present in " + set, set.contains(dd));
        }
        assertArrayEquals(d, set.toDoubleArray(), 0.0000001);

        DoubleSetImpl[] partitions = set.partition(3);
        for (int i = 0; i < partitions.length; i++) {
            DoubleSetImpl ds = partitions[i];
        }
        DoubleSetImpl nue = new DoubleSetImpl();
        for (DoubleSetImpl p : partitions) {
            nue.addAll(p);
        }
        assertArrayEquals(d, nue.toDoubleArray(), 0.0000001);
        assertEquals(set, nue);
    }

    @Test
    public void testDual() {
        double v1 = 100;
        double v2 = 200;
        DoubleSetImpl set = DoubleSetImpl.ofDoubles(v1, v2);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertArrayEquals(new double[]{v1, v2}, set.toDoubleArray(), 0.0000001);
        assertFalse(set.contains(0));
        assertTrue(set.contains(v1));
        assertEquals(0, set.indexOf(v1));
        assertTrue(set.contains(v2));
        assertEquals(1, set.indexOf(v2));

        assertEquals(v1, set.nearestValueTo(149), 0.0000001);
        assertEquals(v2, set.nearestValueTo(151), 0.0000001);

        assertEquals(v2, set.nearestValueTo(201), 0.0000001);
        assertEquals(v1, set.nearestValueTo(0), 0.0000001);

        set = DoubleSetImpl.ofDoubles(v1, v1);
        assertEquals(1, set.size());
        assertEquals(v1, set.getAsDouble(0), 0.0000001);
    }

    @Test
    public void testRemove() {
        DoubleSetImpl set = new DoubleSetImpl(3);
        DoubleSetImpl exp = new DoubleSetImpl(57);
        double[] d = new double[100];
        for (int i = 0; i < 100; i++) {
            int val = 100-i;
            // Add out of order
            set.add(val);
            d[i] = val;
            if (val < 10 || val > 20) {
                exp.add(val);
            }
        }
        Arrays.sort(d);
        DoubleSetImpl toRemove = DoubleSetImpl.ofDoubles(10D,
                10.5D, 11, 11.5D, 12, 12.5D, 13,
                13.5D, 14, 14.5D, 15, 15.5D, 16,
                16.5D, 17, 17.5D, 18, 18.5D, 19, 20);

        set.removeAll(toRemove);
        for (double i = 1; i <= 100; i++) {
            if (i < 10D || i > 20D) {
                assertTrue(set.contains(i));
            } else {
                assertFalse(set.contains(i));
            }
        }
        assertEquals(exp, set);
        set = new DoubleSetImpl(3);
        exp = new DoubleSetImpl(20);
        toRemove = new DoubleSetImpl(100);
        for (int i = 0; i <= 100; i++) {
            set.add(i);
            int v = i - (10 * (i / 10));
            if (v > 3 && v < 7) {
                toRemove.add(i);
            } else {
                exp.add(i);
            }
        }
        set.removeAll(toRemove);
        assertEquals(exp, set);

    }

    @Test
    public void testRemoveRange() {
        DoubleSetImpl set = new DoubleSetImpl(23);
        double[] d = new double[100];
        for (int i = 0; i < 100; i++) {
            set.add(100 - i);
            d[i] = 100 - i;
        }
        Arrays.sort(d);

        set.removeRange(10, 20.5);
        for (double i = 1; i <= 100; i++) {
            if (i < 10D || i >= 20D) {
                assertTrue("Set should contain " + i + ": " + set,
                        set.contains(i));
            } else {
                assertFalse("Set still contains " + i + ": " + set,
                        set.contains(i));
            }
        }
    }

    @Test
    public void testRetain() {
        DoubleSetImpl set = new DoubleSetImpl(3);
        double[] d = new double[100];
        for (int i = 0; i < 100; i++) {
            set.add(100 - i);
            d[i] = 100 - i;
        }
        Arrays.sort(d);
        DoubleSetImpl toRetain = DoubleSetImpl.ofDoubles(10D,
                10.5D, 11, 11.5D, 12, 12.5D, 13,
                13.5D, 14, 14.5D, 15, 15.5D, 16,
                16.5D, 17, 17.5D, 18, 18.5D, 19, 20);
        set.retainAll(toRetain);
        for (double i = 1; i <= 100; i++) {
            if (i < 10D || i > 20D) {
                assertFalse(set.contains(i));
            } else {
                assertTrue(set.contains(i));
                assertFalse(set.contains(i + 0.5D));
            }
        }
    }
}
