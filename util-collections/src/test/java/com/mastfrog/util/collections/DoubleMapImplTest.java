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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class DoubleMapImplTest {

    @Test
    public void testEmptyEquality() {
        DoubleMap<Object> dm = new DoubleMapImpl<>(5);
        assertEquals(DoubleMap.emptyDoubleMap(), dm);
        assertEquals(dm, DoubleMap.emptyDoubleMap());
        assertEquals(dm.hashCode(), DoubleMap.emptyDoubleMap().hashCode());
    }

    @Test
    public void testNearest() {
        DoubleMap<String> m = new DoubleMapImpl<>(5);
        m.put(100, "A");
        m.put(120, "B");
        m.put(150, "C");
        boolean res = m.nearestValueTo(109, (ix, val, str) -> {
            assertEquals(0, ix);
            assertEquals(100D, val, 0.0000000001);
            assertEquals("A", str);
        });
        assertTrue(res);

        DoubleMap.Entry<? extends String> e = m.nearestValueTo(109);
        assertNotNull(e);
        assertEquals("A", e.value());
        assertEquals(0, e.index());
        assertEquals(100D, e.key(), 0.0000000001);

        res = m.nearestValueTo(111, (ix, val, str) -> {
            assertEquals(1, ix);
            assertEquals(120d, val, 0.0000000001);
            assertEquals("B", str);
        });
        assertTrue(res);

        e = m.nearestValueTo(111);
        assertNotNull(e);
        assertEquals("B", e.value());
        assertEquals(1, e.index());
        assertEquals(120D, e.key(), 0.0000000001);

        res = m.nearestValueTo(109, 10, (ix, val, str) -> {
            assertEquals(0, ix);
            assertEquals(100D, val, 0.0000000001);
            assertEquals("A", str);
        });
        assertTrue(res);

        e = m.nearestValueTo(109, 10);
        assertNotNull(e);
        assertEquals("A", e.value());
        assertEquals(0, e.index());
        assertEquals(100D, e.key(), 0.0000000001);

        res = m.nearestValueTo(111, 10, (ix, val, str) -> {
            assertEquals(1, ix);
            assertEquals(120d, val, 0.0000000001);
            assertEquals("B", str);
        });
        assertTrue(res);

        e = m.nearestValueTo(111, 10);
        assertNotNull(e);
        assertEquals("B", e.value());
        assertEquals(1, e.index());
        assertEquals(120D, e.key(), 0.0000000001);

        res = m.nearestValueTo(111, 2, (ix, val, str) -> {
            fail("Should not find a value");
        });
        assertFalse(res);
        e = m.nearestValueTo(111, 2);
        assertNull(e);

        res = m.nearestValueTo(109, 2, (ix, val, str) -> {
            fail("Should not find a value");
        });
        assertFalse(res);

        e = m.nearestValueTo(109, 2);
        assertNull(e);

        res = m.nearestValueExclusive(109, 10, (ix, val, str) -> {
            assertEquals(0, ix);
            assertEquals(100D, val, 0.0000000001);
            assertEquals("A", str);
        });
        assertTrue(res);

        e = m.nearestValueExclusive(109, 10);
        assertNotNull(e);
        assertEquals("A", e.value());
        assertEquals(0, e.index());
        assertEquals(100D, e.key(), 0.0000000001);

        res = m.nearestValueExclusive(111, 10, (ix, val, str) -> {
            assertEquals(1, ix);
            assertEquals(120d, val, 0.0000000001);
            assertEquals("B", str);
        });
        assertTrue(res);

        e = m.nearestValueExclusive(111, 10);
        assertNotNull(e);
        assertEquals("B", e.value());
        assertEquals(1, e.index());
        assertEquals(120D, e.key(), 0.0000000001);
    }

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
        map.removeRange(10, 21);
        for (int i = 0; i < 100; i++) {
            if (i < 10 || i >= 21) {
                assertTrue("Key " + i + " should be present", map.containsKey(i));
                assertEquals("Value " + i + " should be present for " + i,
                        Integer.valueOf(i), map.get(i));
            } else {
                assertFalse("Key present " + i + " but should not be in " + map, map.containsKey(i));
                assertNull("Should not get a value for " + i, map.get(i));
            }
        }
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

    @Test
    public void testValuesBetween() {
        DoubleMapImpl<Integer> map = new DoubleMapImpl<>(51);
        List<Double> expKeys = new ArrayList<>();
        List<Integer> expValues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            map.put(i, i * 10);
            if (i >= 30 && i <= 50) {
                expKeys.add(Double.valueOf(i));
                expValues.add(Integer.valueOf(i * 10));
            }
        }
        List<Double> foundKeys = new ArrayList<>();
        List<Integer> foundValues = new ArrayList<>();
        int count = map.valuesBetween(30, 50, (int index, double value, Integer object) -> {
            foundKeys.add(value);
            foundValues.add(object);
        });
        assertEquals(21, count);
        assertEquals(expKeys, foundKeys);
        assertEquals(expValues, foundValues);
    }

    @Test
    public void testPutAll() {
        DoubleMapImpl<Integer> first = new DoubleMapImpl<>(50);
        DoubleMapImpl<Integer> second = new DoubleMapImpl<>(50);
        DoubleMapImpl<Integer> exp = new DoubleMapImpl<>(50);
        for (int i = 0; i < 100; i++) {
            int tens = i / 10;
            if (tens % 2 == 0) {
                first.put(i, i);
            } else {
                second.put(i, i);
            }
            exp.put(i, i);
        }
        assertEquals(first.toString(), 50, first.size());
        assertEquals(50, second.size());
        assertEquals(100, exp.size());
        for (int i = 0; i < 100; i++) {
            int tens = i / 10;
            if (tens % 2 == 0) {
                assertTrue(first.containsKey(i));
                assertFalse(second.containsKey(i));
                assertEquals(Integer.valueOf(i), first.get(i));
                assertNull(second.get(i));
            } else {
                assertFalse(first.containsKey(i));
                assertTrue(second.containsKey(i));
                assertEquals(Integer.valueOf(i), second.get(i));
                assertNull(first.get(i));
            }
        }

        first.putAll(second);
        assertEquals(100, first.size());
        for (int i = 0; i < 100; i++) {
            assertTrue(first.containsKey(i));
            assertEquals(Integer.valueOf(i), first.get(i));
        }
        DoubleMap<Integer> more = new DoubleMapImpl<>(30);
        for (int i = 0; i < 100; i++) {
            int tens = i / 10;
            if (tens % 2 == 1) {
                more.put(i, i * 10);
            }
        }
        first.putAll(more);
        int foundCount = 0;
        for (int i = 0; i < 100; i++) {
            int tens = i / 10;
            if (tens % 2 == 1) {
                Integer val = first.get(i);
                if (i * 10 == val.intValue()) {
                    foundCount++;
                }
            }
        }
        // Since duplicate behavior is undefined (however the sort
        // algorithm chooses to order duplicates), the best we can
        // test for is that *some* values make it
        assertTrue(foundCount > 0);
    }

    @Test
    public void testVisitMiddleOut() {
        DoubleMapImpl<String> m = new DoubleMapImpl<>(30);
        char c = 'a';
        for (int i = 40; i < 60; i++, c++) {
            String s = new String(new char[]{c});
            m.put(i, s);
        }
        List<String> found = new ArrayList<>();
        boolean res = m.visitMiddleOut(48, 52, (ix, key, val) -> {
//            System.out.println("VISIT " + key + " at " + ix + " with " + val);
            found.add(val);
            return false;
        });
        assertFalse(res);

        assertEquals(Arrays.asList("k", "j", "l", "i", "m"), found);

        found.clear();
        res = m.visitMiddleOut(47, 53, (ix, key, val) -> {
            found.add(val);
            return "i".equals(val);
        });
        assertTrue(res);
        assertEquals(Arrays.asList("k", "j", "l", "i"), found);
    }
}
