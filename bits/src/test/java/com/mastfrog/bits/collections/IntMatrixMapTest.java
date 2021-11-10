/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.bits.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class IntMatrixMapTest {

    @Test
    public void dirtSimpleTest() {
        IntMatrixMap map = IntMatrixMap.atomic(67);
        assertEquals(67, map.capacity());
        map.put(1, 23);

        assertEquals(1, map.size());
        assertTrue(map.containsKey(1));
        assertEquals(23, map.get(1));
        map.forEachPair((k, v) -> {
            assertEquals(1, k);
            assertEquals(23, v);
            assertTrue(map.containsKey(k));
        });
        assertEquals(-1, map.get(23), "23 is a value not a key");
        assertFalse(map.containsKey(23));
    }

    @Test
    public void testPairs() {
        IntMatrixMap map = IntMatrixMap.atomic(128);
        int ct = 0;
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 3 != 0) {
                map.put(i, i - 1);
                ct++;
                assertEquals(ct, map.size(), "Size wrong after adding " + i + " to " + map);
            }
        }
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 3 != 0) {
                assertEquals(i - 1, map.get(i));
                assertTrue(map.containsKey(i), "Should contain the key " + (i) + ": " + map);
            } else {
                assertFalse(map.containsKey(i), "Should not contain the key " + i + ": " + map);
                assertEquals(-1, map.get(i), "Should get -1 for get of " + i + " in " + map);
            }
        }
        assertFalse(map.isEmpty(), "Map should not be empty");
        assertEquals(ct, map.size(), "Wrong size");
    }

    @Test
    public void testRemove() {
        IntMatrixMap map = IntMatrixMap.atomic(128);
        int ct = 0;
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 3 != 0) {
                map.put(i, i - 1);
                assertTrue(map.contains(i, i - 1));
                ct++;
            }
        }
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 3 != 0) {
                assertEquals(ct, map.size());
                map.remove(i);
                ct--;
                assertFalse(map.containsKey(i));
            }
        }
    }

    @Test
    public void testClear() {
        IntMatrixMap map = IntMatrixMap.atomic(128);
        int ct = 0;
        for (int i = 0; i < 128; i++) {
            if (i > 0 && i % 3 != 0) {
                map.put(i, i - 1);
                ct++;
            }
        }
        assertFalse(map.isEmpty());
        assertEquals(ct, map.size());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    @SuppressWarnings("UnnecessaryUnboxing")
    public void testAdapted() {
        IntMatrixMap map = IntMatrixMap.atomic(128);
        List<Long> keys = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            keys.add((long) i);
        }
        List<Long> values = new ArrayList<>(keys);
        Random rnd = new Random(10390139L);
        Collections.shuffle(keys, rnd);
        Collections.shuffle(values, rnd);

        class Adap implements IntMatrixMap.LongMapAdapter {

            @Override
            public int indexOfKey(long key) {
                return keys.indexOf(key);
            }

            @Override
            public int indexOfValue(long value) {
                return values.indexOf(value);
            }

            @Override
            public long keyForKeyIndex(int index) {
                return keys.get(index);
            }

            @Override
            public long valueForValueIndex(int index) {
                return values.get(index);
            }
        }

        Adap a = new Adap();
        IntMatrixMap.LongMatrixMap longs = map.asLongMap(a);
        Map<Long, Long> vals = new HashMap<>();
        for (int i = 0; i < 128; i++) {
            if (i % 2 == 0) {
                Long k = keys.get(i);
                Long v= values.get(i);
                vals.put(k, v);
                longs.put(k, v);
            }
        }
        for (int i = 0; i < 128; i++) {
            Long k = keys.get(i);
            Long v = vals.get(k);
            if (v == null) {
                assertEquals(-1, longs.getOrDefault(k, -1));
                assertFalse(longs.containsKey(k), "Should not contain a value for "
                        + k + " @ " + i + " but containsKey returns true; get gets " + longs.getOrDefault(k, -1)
                        + " in " + longs);
            } else {
                assertTrue(longs.containsKey(k));
                assertEquals(v.longValue(), longs.getOrDefault(k, -1));
            }
        }
    }
}
