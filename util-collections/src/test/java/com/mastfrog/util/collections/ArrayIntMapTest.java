/*
 * The MIT License
 *
 * Copyright 2004 Tim Boudreau.
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ArrayIntMapTest {

    static {
        System.setProperty("ArrayIntMap.debug", "true");
    }

    NumberFormat nf = NumberFormat.getIntegerInstance(Locale.UK);

    {
        nf.setMinimumIntegerDigits(4);
    }

    public void testEmptyWithSupplierAddingSupplied() {
        ArrayIntMap<Thing> m = new ArrayIntMap<>(10, true, new ThingSupplier());
        assertFalse(m.containsKey(5));
        assertEquals(0, m.size());
        Thing t5 = new Thing(5);
        m.put(5, t5);
        assertEquals(1, m.size());
        assertTrue(m.containsValue(t5));
        assertTrue(m.containsKey(5));
        assertTrue(m.keySet().contains(5));
        assertArrayEquals(new int[]{5}, m.keys());
        assertFalse(m.containsKey(1000));
        Thing t1000 = m.get(1000);
        assertNotNull(t1000);
        assertTrue(m.containsKey(1000));
        assertEquals(2, m.size());
        assertArrayEquals(new int[]{5, 1000}, m.keys());
        assertSame(t1000, m.get(1000));
        assertEquals(1000, t1000.t);
        Thing t1001 = m.get(1001);
        assertNotNull(t1001);
        assertEquals(1001, t1001.t);
        assertTrue(m.containsKey(1001));
        assertArrayEquals(new int[]{5, 1000, 1001}, m.keys());
        assertEquals(3, m.size());
        assertFalse(m.isEmpty());
        Thing tRem = m.remove(5);
        assertSame(t5, tRem);
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey(1000));
        assertTrue(m.containsKey(1001));
        assertEquals(2, m.size());
        assertArrayEquals(new int[]{1000, 1001}, m.keys());
    }

    @Test
    public void testArraysConstructor() {
        IntMap<String> im = IntMap.of(new int[]{1, 2, 3, 4}, new String[]{"one", "two", "three", "four"});
        assertEquals(4, im.size());
        for (int i = 1; i < 5; i++) {
            String exp;
            switch (i) {
                case 1:
                    exp = "one";
                    break;
                case 2:
                    exp = "two";
                    break;
                case 3:
                    exp = "three";
                    break;
                case 4:
                    exp = "four";
                    break;
                default:
                    throw new AssertionError(i);
            }
            assertEquals(exp, im.get(i));
        }
        im.remove(3);
        assertFalse(im.containsKey(3));
        assertNull(im.get(3));
        for (int i = 1; i < 5; i++) {
            String exp;
            switch (i) {
                case 1:
                    exp = "one";
                    break;
                case 2:
                    exp = "two";
                    break;
                case 3:
                    exp = null;
                    break;
                case 4:
                    exp = "four";
                    break;
                default:
                    throw new AssertionError(i);
            }
            assertEquals(exp, im.get(i));
        }
    }

    @Test
    public void testPutOutOfOrder() {
        // .size()
        ArrayIntMap<Thing> m = new ArrayIntMap<>(10, true, new ThingSupplier());
        m.put(51, new Thing(51));
        assertEquals(1, m.size());
        assertTrue(m.keySet().contains(Integer.valueOf(51)));
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey(51));
        m.put(9, new Thing(9));
        assertEquals(2, m.size());
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey(9));
        assertTrue(m.keySet().contains(Integer.valueOf(9)));
        assertTrue(m.keySet().contains(Integer.valueOf(51)));
    }

    @Test
    public void testPutInOrder() {
        // .size()
        ArrayIntMap<Thing> m = new ArrayIntMap<>(10, true, new ThingSupplier());
        m.put(1, new Thing(1));
        assertEquals(1, m.size());
        assertTrue(m.keySet().contains(Integer.valueOf(1)));
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey(1));
        m.put(9, new Thing(9));
        assertEquals(2, m.size());
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey(9));
        assertTrue(m.keySet().contains(Integer.valueOf(9)));
        assertTrue(m.keySet().contains(Integer.valueOf(1)));
    }

    public void testEmptyWithSupplierNotAddingSupplied() {
        ArrayIntMap<Thing> m = new ArrayIntMap<>(10, false, new ThingSupplier());
        assertFalse(m.containsKey(5));
        Thing t5 = new Thing(5);
        m.put(5, t5);
        assertTrue(m.containsValue(t5));
        assertTrue(m.containsKey(5));
        assertFalse(m.containsKey(1000));
        Thing t1000 = m.get(1000);
        assertNotNull(t1000);
        assertFalse(m.containsKey(1000));
        assertEquals(1000, t1000.t);
        Thing t1001 = m.get(1001);
        assertNotNull(t1001);
        assertEquals(1001, t1001.t);
        assertFalse(m.isEmpty());
        Thing tRem = m.remove(5);
        assertSame(t5, tRem);
        assertTrue(m.isEmpty());
    }

    static final class ThingSupplier implements Supplier<Thing> {

        private int counter = 1000;

        @Override
        public Thing get() {
            return new Thing(counter++);
        }
    }

    private static class Thing {

        private final int t;

        public Thing(int t) {
            this.t = t;
        }

        public String toString() {
            return "T" + t;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Thing && ((Thing) o).t == t;
        }

        @Override
        public int hashCode() {
            return t * 31;
        }
    }

    @Test
    public void testEmpty() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.values().size());
        assertFalse(map.iterator().hasNext());
        assertFalse(map.keySet().iterator().hasNext());
        assertFalse(map.values().iterator().hasNext());
        assertNull(map.get(0));
        assertNull(map.get(4024));
        assertNull(map.get(Integer.MAX_VALUE));
        assertNull(map.get(Integer.MIN_VALUE));
        assertFalse(map.containsKey(0));
        assertFalse(map.containsKey(-1));
        assertFalse(map.containsKey(Integer.MAX_VALUE));
        assertFalse(map.containsKey(Integer.MIN_VALUE));
        assertEquals(0, map.keySet().toArray().length);
        map.put(23, "foo");
        assertEquals(1, map.size());
        assertEquals("foo", map.remove(23));
        assertNull(map.get(23));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testCopyConstructor() {
        Map<Integer, String> mm = new HashMap<>();
        int i = 3;
        while (mm.size() < 100) {
            int k = i * 2;
            String v = "v" + nf.format(k);
            mm.put(k, v);
            i += 3;
        }
        ArrayIntMap<String> map = new ArrayIntMap<>(mm);

        assertEquals(mm.size(), map.size());
        assertEquals("v0,162", map.get(162));
        assertEquals(mm, map);
    }

    @Test
    public void testReplacePut() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        Set<Integer> l = new HashSet<>();
        Map<Integer, String> mm = new HashMap<>();
        int i = 3;
        while (map.size() < 100) {
            int k = i * 2;
            String v = "v" + k;
            map.put(k, v);
            mm.put(k, v);
            l.add(k);
            i += 3;
        }
        assertEquals(mm, map);
        assertEquals(mm.keySet(), map.keySet());
        List<String> expect = new ArrayList<>(mm.values());
        List<String> got = new ArrayList<>(map.values());
        Collections.sort(expect);
        Collections.sort(got);
        assertEquals(expect, got);

        assertTrue(map.containsKey(6));
        String old = map.put(6, "first");
        assertEquals("v6", old);
        assertEquals(100, map.size());
        assertEquals("first", map.get(6));
        assertTrue(map.values().contains("first"));
        assertTrue(map.containsKey(6));

        int last = map.greatestKey();
        assertTrue(map.containsKey(last));
        old = map.put(last, "last");
        assertNotNull(old);
        assertEquals("last", map.get(last));

        int mid = 36;
        assertTrue(map.containsKey(mid));
        old = map.put(mid, "mid");
        assertNotNull(old);
        assertEquals("mid", map.get(mid));

        assertFalse(map.containsKey(206));
        assertFalse(map.containsKey(-1));
    }

    @Test
    public void testRemove() {
        ArrayIntMap<String> map = new ArrayIntMap<>();
        Set<Integer> keys = new HashSet<>();
        Set<String> values = new HashSet<>();
        int i = 3;
        while (map.size() < 100) {
            int k = i * 2;
            String v = "v" + nf.format(k);
            values.add(v);
            map.put(k, v);
            keys.add(k);
            assertEquals(v, map.get(k));
            assertTrue(map.containsKey(k));
            assertTrue(map.containsValue(v));
            i += 3;
        }
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());
        assertEquals(keys.size(), map.keySet().size());
        assertTrue(keys.containsAll(map.keySet()));
        assertTrue(map.keySet().containsAll(keys));
        assertEquals(keys, map.keySet());
        assertEquals(100, map.size());

        // Remove at head, tail and middle use different code
        int first = map.leastKey();
        assertEquals(6, first);
        assertTrue(map.containsKey(first));
        String old = map.get(first);
        assertNotNull(old);
        assertTrue(map.values().contains(old));
        String oold = map.remove(first);
        keys.remove(first);
        assertSame(old, oold);
        values.remove(oold);
        assertEquals(99, map.size());
        assertFalse(map.containsKey(first));
        assertFalse(map.values().contains(old));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        int last = map.greatestKey();
        assertTrue(map.containsKey(last));
        old = map.get(last);
        assertNotNull(old);
        assertTrue(map.values().contains(old));
        oold = map.remove(last);
        keys.remove(last);
        assertSame(old, oold);
        values.remove(oold);
        assertEquals(98, map.size());
        assertFalse(map.containsKey(last));
        assertFalse(map.values().contains(old));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        int mid = 162;
        old = map.get(mid);
        assertEquals("v0,162", old);
        assertTrue(map.containsKey(mid));
        assertTrue(map.keySet().contains(mid));
        oold = map.remove(mid);
        keys.remove(mid);
        values.remove(oold);
        assertSame(oold, old);
        assertEquals(97, map.size());
        assertFalse(map.containsKey(mid));
        assertCollectionsEquals(keys, map.keySet());
        assertCollectionsEquals(values, map.values());

        assertFalse("Should not contain " + oold + " but does at "
                + map.keyForValue(oold) + ": " + map.values(), map.values().contains(old));
    }

    private <T extends Comparable<T>> void assertCollectionsEquals(Collection<T> a, Collection<T> b) {
        List<T> aa = new ArrayList<>(a);
        List<T> bb = new ArrayList<>(b);
        Collections.sort(aa);
        Collections.sort(bb);
        assertEquals(aa.size(), bb.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aa.size(); i++) {
            T aaa = aa.get(i);
            T bbb = bb.get(i);
            if (!Objects.equals(aaa, bbb)) {
                sb.append("Difference at " + i + " - " + aaa + " vs " + bbb + "\n");
            }
        }
        assertEquals(sb.toString(), aa, bb);
    }

    @Test
    public void testFirst() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        assertTrue("First entry should be 5", map.leastKey() == 5);
    }

    @Test
    public void testNextEntry() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length - 1; i++) {
            int val = indices[i + 1];
            int next = map.nextEntry(indices[i]);
            assertTrue("Entry after " + indices[i] + " should be " + val + " not " + next, next == val);
        }
    }

    @Test
    public void testPrevEntry() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = indices.length - 1; i > 0; i--) {
            int val = indices[i - 1];
            int next = map.prevEntry(indices[i]);
            assertTrue("Entry before " + indices[i] + " should be " + val + " not " + next, next == val);
        }
    }

    @Test
    public void testNearest() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length - 1; i++) {
            int toTest = indices[i] + ((indices[i + 1] - indices[i]) / 2);
            int next = map.nearestKey(toTest, false);
            assertTrue("Nearest value to " + toTest + " should be " + indices[i + 1] + ", not " + next, next == indices[i + 1]);
        }

        assertTrue("Value after last entry should be 0th", map.nearestKey(indices[indices.length - 1] + 1000, false) == indices[0]);

        assertTrue("Value before first entry should be last", map.nearestKey(-1, true) == indices[indices.length - 1]);

        assertTrue("Value after < first entry should be 0th", map.nearestKey(-1, false) == indices[0]);

        for (int i = indices.length - 1; i > 0; i--) {
//            int toTest = indices[i] - (indices[i-1] + ((indices[i] - indices[i-1]) / 2));
            int toTest = indices[i - 1] + ((indices[i] - indices[i - 1]) / 2);
            int prev = map.nearestKey(toTest, true);
            assertTrue("Nearest value to " + toTest + " should be " + indices[i - 1] + ", not " + prev, prev == indices[i - 1]);
        }

        assertTrue("Entry previous to value lower than first entry should be last entry",
                map.nearestKey(indices[0] - 1, true) == indices[indices.length - 1]);

        assertTrue("Value after > last entry should be last 0th", map.nearestKey(indices[indices.length - 1] + 100, false) == indices[0]);

        assertTrue("Value before > last entry should be last entry", map.nearestKey(indices[indices.length - 1] + 100, true) == indices[indices.length - 1]);

        assertTrue("Value after < first entry should be 0th", map.nearestKey(-10, false) == indices[0]);

    }

    /**
     * Test of get method, of class org.netbeans.core.output2.ArrayIntMap.
     */
    @Test
    public void testGet() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        assert indices.length == values.length;

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        for (int i = 0; i < indices.length; i++) {
            assertTrue(map.get(indices[i]) == values[i]);
        }
    }

    @Test
    public void testKeysArray() {
        ArrayIntMap<Object> map = new ArrayIntMap<>();

        int[] indices = new int[]{5, 12, 23, 62, 247, 375, 489, 5255};

        Object[] values = new Object[]{
            "zeroth", "first", "second", "third", "fourth", "fifth", "sixth",
            "seventh"};

        for (int i = 0; i < indices.length; i++) {
            map.put(indices[i], values[i]);
        }

        int[] keys = map.keysArray();
        assertTrue("Keys returned should match those written.  Expected: " + i2s(indices) + " Got: " + i2s(keys), Arrays.equals(keys, indices));
    }

    private static String i2s(int[] a) {
        StringBuilder result = new StringBuilder(a.length * 2);
        for (int i = 0; i < a.length; i++) {
            result.append(a[i]);
            if (i != a.length - 1) {
                result.append(',');
            }
        }
        return result.toString();
    }

    @Test
    public void testSingleValueWithSupplier() {
        AtomicInteger at = new AtomicInteger(10);
        ArrayIntMap<Integer> m = new ArrayIntMap<>(10, true, at::getAndIncrement);

        assertEquals(Integer.valueOf(10), m.get(0));
        assertEquals(Integer.valueOf(11), m.get(5));
        assertEquals(Integer.valueOf(12), m.get(9));

        assertEquals(Integer.valueOf(10), m.get(0));
        assertEquals(Integer.valueOf(11), m.get(5));
        assertEquals(Integer.valueOf(12), m.get(9));

        m = new ArrayIntMap<>(10, false, at::getAndIncrement);
        assertEquals(Integer.valueOf(13), m.get(0));
        assertEquals(Integer.valueOf(14), m.get(0));
        assertEquals(Integer.valueOf(15), m.get(0));
    }

    @Test
    public void testIndices() {
        ArrayIntMap<Integer> m = new ArrayIntMap<>(11);
        for (int i = 0; i <= 10; i++) {
            m.put(i * 10, Integer.valueOf(i));
        }
        assertEquals(1, m.nearestIndexTo(5, false));
        assertEquals(0, m.nearestIndexTo(5, true));

        Set<Integer> ks = new HashSet<>();
        Set<Integer> vs = new HashSet<>();
        m.valuesBetween(15, 35, (k, v) -> {
            ks.add(k);
            vs.add(v);
        });
        assertEquals(setOf(20, 30), ks);
        assertEquals(setOf(2, 3), vs);
        ks.clear();
        vs.clear();

        m.valuesBetween(20, 30, (k, v) -> {
            ks.add(k);
            vs.add(v);
        });
        assertEquals(setOf(20, 30), ks);
        assertEquals(setOf(2, 3), vs);
        ks.clear();
        vs.clear();

        m.valuesBetween(95, 105, (k, v) -> {
            ks.add(k);
            vs.add(v);
        });
        assertEquals(setOf(100), ks);
        assertEquals(setOf(10), vs);

        m.valuesBetween(-100, -10, (k, v) -> {
            fail("should not be called");
        });

        m.valuesBetween(101, 110, (k, v) -> {
            fail("should not be called");
        });
        m.valuesBetween(2, 9, (k, v) -> {
            fail("should not be called");
        });

        m = new ArrayIntMap<>(23);
        m.put(37, Integer.valueOf(105));
        m.valuesBetween(0, 100, (k, v) -> {
            assertEquals(37, k);
            assertEquals(Integer.valueOf(105), v);
        });
        m.valuesBetween(0, 10, (k, v) -> {
            fail("should not be called");
        });
        m.valuesBetween(38, 39, (k, v) -> {
            fail("should not be called");
        });
    }

    @Test
    public void testBulkRemove() {
        ArrayIntMap<Integer> arm = new ArrayIntMap<>(100);
        Map<Integer, Integer> expected = new HashMap<>();
        IntSet toRemove = new IntSetImpl();
        IntSet expectedKeySet = new IntSetImpl();
        for (int i = 0; i < 100; i++) {
            arm.put(i, Integer.valueOf(i));
            int tens = i / 10;
            if (tens % 2 != 0) {
                toRemove.add(i);
            } else {
                expected.put(i, i);
                expectedKeySet.add(i);
            }
        }
        assertEquals(100, arm.size());
        int removed = arm.removeAll(toRemove);
        assertEquals(expectedKeySet, arm.keySet());
        assertEquals(removed, toRemove.size());
        assertEquals(expected, arm);
    }

    @Test
    public void testMove() {
        ArrayIntMap<Integer> arm = new ArrayIntMap<>();
        for (int i = 0; i < 100; i++) {
            arm.put(i, Integer.valueOf(i));
        }
        arm.move(1, 99, (int oldKey, Integer oldValue, int newKey, Integer newValue, BiConsumer<Integer, Integer> oldNewReceiver) -> {
            assertEquals(oldKey, 1);
            assertEquals(newKey, 99);
            assertEquals(oldKey, oldValue.intValue());
            assertEquals(newKey, newValue.intValue());
            oldNewReceiver.accept(-1, 1000);
            return 1000;
        });
        assertEquals(-1, arm.get(1).intValue());
        assertEquals(1000, arm.get(99).intValue());
    }

    @Test
    public void testMoves() {
        ArrayIntMap<String> arm = new ArrayIntMap<>();
        // 89767, 104051, 205394, 191110
        arm.put(89767, "one");
        arm.put(104051, "two");
        arm.put(205394, "three");
        arm.put(1, "four");
        arm.move(1, 191110, (int oldKey, String oldValue, int newKey, String newValue, BiConsumer<String, String> oldNewReceiver) -> {
            assertNull(newValue);
            assertEquals("four", oldValue);
            assertEquals(1, oldKey);
            assertEquals(191110, newKey);
            oldNewReceiver.accept(null, oldValue);
            return newValue;
        });
        assertEquals("four", arm.get(191110));
    }

    @Test
    public void testSearchAndSort() {
        ArrayIntMap<String> arm = new ArrayIntMap<>();
        // 89767, 104051, 205394, 191110
        arm.put(89767, "one");
        arm.put(104051, "two");
        arm.put(205394, "three");
//        arm.put(191110, "four");
        arm.put(1, "four");
        arm.move(1, 191110, (int oldKey, String oldValue, int newKey, String newValue, BiConsumer<String, String> oldNewReceiver) -> {
            assertNull(newValue);
            assertEquals("four", oldValue);
            assertEquals(1, oldKey);
            assertEquals(191110, newKey);
            oldNewReceiver.accept(null, oldValue);
            return newValue;
        });

        assertEquals(191110, arm.keys[3]);
        assertEquals("four", arm.get(191110));

        // Simulate an operation that messes up the sort order,
        // in order to ensure a resort is performed on the call to valuesBetween()
        Object[] va  = arm.valuesUnsafe();
        arm.keys[0] = 104051;
        va[0] = "two";
        va[1] = "one";
        arm.keys[1] = 89767;
        arm.setResortUnsafe(true);

        Set<String> found = new HashSet<>();
        int count = arm.valuesBetween(174220, 223468, (int key, String value) -> {
            found.add(value);
            assertNotEquals("one", value);
            assertNotEquals("two", value);
        });
        assertFalse("Nothing scanned", found.isEmpty());
        assertNotEquals(0, count);
        assertEquals(found.size(), count);
        assertEquals(setOf("three", "four"), found);
    }

    @Test
    public void testSearch2() {
        /**
         * Scan address range 145504 to 194752 bound 71.0, 95.0 81.0 x 81.0
         * ending at 176.0, 95.0 passed bounds 71.0, 95.0 81.0 x 81.0 CMS:
         * ValuesWith 152.0, 71.0 to 176.0, 95.0 in
         * java.awt.Rectangle[x=128,y=0,width=128,height=128] tested 0 entries
         * in [59858, 74142, 175484, 161201]
         */
        IntMap<String> m = new ArrayIntMap<String>()
                .add(59858).apply("one")
                .add(74142).apply("two")
                .add(175484).apply("three")
                .add(161201).apply("four");
        assertEquals(4, m.size());

        Set<String> res1 = new HashSet<>();
        int ct1 = m.valuesBetween(145504, 194752, (key, val) -> {
            res1.add(val);
        });

        Set<String> res2 = new HashSet<>();
        int ct2 = m.keysAndValuesBetween(145504, 194752, (index, key, val) -> {
            res2.add(val);
        });
        assertEquals(setOf("three", "four"), res1);
        assertEquals(setOf("three", "four"), res2);
        assertEquals(2, ct1);
        assertEquals(2, ct2);
    }

    @Test
    public void testPutAllOutOfOrder() {
        ArrayIntMap<String> am = new ArrayIntMap<>();
        am.add(1).apply("a");

        ArrayIntMap<String> ab = new ArrayIntMap<>();
        ab.put(10, "d");
        ab.put(5, "c");
        ab.put(2, "b");
        am.putAll(ab);
//        assertEquals("a", am.valueAt(0));
        am.consistent();
    }

    @Test
    public void testPutAllWithDuplicates() {
        ArrayIntMap<String> am = new ArrayIntMap<>();
        am.add(1).apply("a");
        am.add(9).apply("d");
        am.add(7).apply("c");
        am.add(3).apply("b");

        ArrayIntMap<String> am2 = new ArrayIntMap<>();
        am2.add(3).apply("b");
        am2.add(2).apply("a1");
        am2.add(4).apply("a2");
        am2.add(7).apply("c");
        am2.add(8).apply("a3");

        am.putAll(am2);
        am.consistent();
    }

    @Test
    public void testIndexBug() {
        int[] keys = new int[]{188576144, 266437232, 282432134, 1608230649, 1843289228, 187385956};
        int[] other = Arrays.copyOf(keys, keys.length);
        Arrays.sort(other);
        String[] vals = new String[]{"a", "b", "c", "d", "e", "f"};
        ArrayIntMap im = new ArrayIntMap(keys, vals);
        assertTrue("Should need a re-sort", im.resort);
        for (int i = 0; i < keys.length; i++) {
            int realIx = Arrays.binarySearch(other, keys[i]);
            assertEquals(realIx, im.indexOf(keys[i]));
            assertTrue(im.containsKey(keys[i]));
            assertNotNull(im.get(keys[i]));
        }
        assertFalse("Should need a re-sort", im.resort);
    }

    @Test
    public void testAddSeq() {
        int[] keys = new int[]{188576144, 266437232, 282432134, 1608230649, 1843289228, 187385956};
        int[] keysSorted = Arrays.copyOf(keys, keys.length);
        Arrays.sort(keysSorted);

        String[] vals = new String[]{"a", "b", "c", "d", "e", "f"};
        ArrayIntMap im = new ArrayIntMap(keysSorted, vals);

        IntSet is = IntSet.arrayBased(3);
        is.add(0);
        is.add(3);
        is.add(2);
        assertFalse("Set should not have sorted itself until a call required it",
                ((IntSetArray) is).currentlySorted());
        im.removeIndices(is);
        assertTrue("Remove indices should have forced a sort",
                ((IntSetArray) is).currentlySorted());
        assertFalse("Wrong items removed when removing " + is + " - got " + im, im.containsKey(keysSorted[0]));
        assertFalse("Wrong items removed when removing " + is + " - got " + im, im.containsKey(keysSorted[2]));
        assertFalse("Wrong items removed when removing " + is + " - got " + im, im.containsKey(keysSorted[3]));
        assertTrue("Wrong items removed when removing " + is + " - got " + im, im.containsKey(keysSorted[1]));
        assertTrue("Wrong items removed when removing " + is + " - got " + im, im.containsKey(keysSorted[4]));
    }
}
