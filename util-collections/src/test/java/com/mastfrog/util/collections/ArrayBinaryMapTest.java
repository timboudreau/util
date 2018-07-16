/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import com.mastfrog.util.strings.Strings;
import com.mastfrog.util.collections.CollectionUtils.ComparableComparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class ArrayBinaryMapTest {

    @Test
    public void testMaps() {
        // Disabling for now - class is not exposed
        if (true) {
            return;
        }
        ArrayBinaryMap<String, String> m = new ArrayBinaryMap<>(String.class, new ComparableComparator<String>(), 5);
        m.put("b", "2");
        m.put("a", "1");
        m.put("c", "3");
        m.put("d", "4");
        assertMaps(map("a", "1", "b", "2", "c", "3", "d", "4"), m);
        m.put("a", "1a");
        assertMaps(map("a", "1a", "b", "2", "c", "3", "d", "4"), m);
        m.put("a", "1");
        assertMaps(map("a", "1", "b", "2", "c", "3", "d", "4"), m);
        m.put("e", "5");
        assertMaps(map("a", "1", "b", "2", "c", "3", "d", "4", "e", "5"), m);
        System.out.println("KEYS BEFORE " + Strings.join(',', m.keys.objs));
        m.remove("b");
        System.out.println("KEYS AFTER " + Strings.join(',', m.keys.objs));
        assertMaps(map("a", "1", "c", "3", "d", "4", "e", "5"), m);
        m.remove("a");
        assertMaps(map("c", "3", "d", "4", "e", "5"), m);
        m.remove("e");
        assertMaps(map("c", "3", "d", "4"), m);
        m.remove("q");
        assertMaps(map("c", "3", "d", "4"), m);
        m.remove("d");
        assertMaps(map("c", "3"), m);
        m.remove("c");
        assertTrue(m.isEmpty());

    }

    private void assertMaps(Map<String, String> expect, ArrayBinaryMap<String, String> got) {
        System.out.println("GOT " + got);
        System.out.println("ENTRIES " + got.entrySet());
        assertEquals("size mismatch " + got, expect.size(), got.size());
        Set<Map.Entry<String, String>> entrySet = got.entrySet();
        Set<Map.Entry<String, String>> expectEntrySet = got.entrySet();
        assertEquals(expectEntrySet.size(), entrySet.size());
        for (Map.Entry<String, String> e : got.entrySet()) {
            assertTrue(got.containsKey(e.getKey()));
            assertEquals(got.get(e.getKey()), e.getValue());
            assertTrue(expect.containsKey(e.getKey()));
            assertEquals(expect.get(e.getKey()), e.getValue());
        }
        for (Map.Entry<String, String> e : expectEntrySet) {
            assertTrue(got.containsKey(e.getKey()));
            assertEquals(e.getValue(), got.get(e.getKey()));
        }
        ArrayBinarySetMutableTest.assertSetsMatch(expect.keySet(), (ArrayBinarySetMutable<String>) got.keySet());
        assertEquals(expect, got);
    }

    private final Map<String, String> map(String... strings) {
        assertTrue((strings.length % 2) == 0);
        Map<String, String> res = new LinkedHashMap<>();
        for (int i = 0; i < strings.length; i += 2) {
            res.put(strings[i], strings[i + 1]);
        }
        return res;
    }
}
