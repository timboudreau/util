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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class DoubleMapImplTest {

    @Test
    public void testRemove() {
        DoubleMapImpl<Integer> map = new DoubleMapImpl<>(10);
        for (int i = 0; i < 100; i++) {
            double key = i;
            Integer val = i;
            map.put(key, val);
            assertTrue("Not added: " + key + " to " + map, map.containsKey(key));
            assertEquals(val, map.get(key));
            assertEquals(i, map.indexOf(key));
        }
        map.removeRange(10, 20.5);
    }

    @Test
    public void testSimpleMap() {
        DoubleMapImpl<Integer> map = new DoubleMapImpl<>(10);
        for (int i = 0; i < 100; i++) {
            double key = i;
            Integer val = i;
            map.put(key, val);
            assertTrue("Not added: " + key + " to " + map, map.containsKey(key));
            assertEquals(val, map.get(key));
            assertEquals(i, map.indexOf(key));
        }

        for (int i = 0; i < 100; i++) {
            double key = i;
            Integer val = map.get(key);
            assertEquals("Wrong val for " + key + " @ " + i, Integer.valueOf(i), val);
        }

        DoubleMapImpl<Integer> b = new DoubleMapImpl<>(5);
        for (int i = 99; i >= 0; i--) {
            double key = i;
            Integer val = i;
            b.put(key, val);
            assertTrue("Not added: " + key + " to " + map, map.containsKey(key));
        }
        assertEquals(map, b);
        assertEquals(map.hashCode(), b.hashCode());
    }

    @Test
    @SuppressWarnings("UnnecessaryBoxing")
    public void testAddWithReordering() {
        DoubleMapImpl<Integer> map = new DoubleMapImpl<>(10);
        for (int i = 99; i >= 0; i--) {
            double key = i;
            Integer val = i;
            map.put(key, val);
            assertTrue("Not added: " + key + " to " + map, map.containsKey(key));
            assertEquals(val, map.get(key));
        }

        for (int i = 0; i < 100; i++) {
            double key = i;
            Integer val = map.get(key);
            assertEquals("Wrong val for " + key + " @ " + i, Integer.valueOf(i), val);
        }

        int[] cur = new int[1];
        map.forEach((ix, key, val) -> {
            int pos = cur[0]++;
            assertEquals(ix, pos);
            assertEquals((double) pos, key, 0.00000001);
            assertEquals(Integer.valueOf(pos), val);

            assertEquals(Integer.valueOf(pos), map.get(key));

            map.nearestValueTo(key + 0.1, (ix1, key1, val1) -> {
                assertEquals(Integer.valueOf(pos), val1);
                assertEquals(key, key1, 0.000000001);
                assertEquals(pos, ix1);
            });

            map.nearestValueTo(key - 0.1, (ix1, key1, val1) -> {
                assertEquals(Integer.valueOf(pos), val1);
                assertEquals(key, key1, 0.000000001);
                assertEquals(pos, ix1);
            });

            if (pos < 99) {
                map.nearestValueExclusive(key, (ix1, key1, val1) -> {
                    assertEquals(Integer.valueOf(pos + 1), val1);
                    assertEquals((double) (pos + 1), key1, 0.000000001);
                    assertEquals(pos + 1, ix1);
                });
            } else {
                map.nearestValueExclusive(key, (ix1, key1, val1) -> {
                    assertEquals(Integer.valueOf(pos - 1), val1);
                    assertEquals((double) (pos - 1), key1, 0.000000001);
                    assertEquals(pos - 1, ix1);
                });
            }
        });
    }
}
