/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ConvertedMapTest {

    private final class IC implements Converter<Integer, CharSequence> {

        @Override
        public Integer convert(CharSequence r) {
            return Integer.parseInt(r.toString());
        }

        @Override
        public String unconvert(Integer t) {
            return Integer.toString(t);
        }
    }

    IC ic = new IC();
    Map<Integer, String> map = new HashMap<>();

    @Before
    public void before() {
        for (int i = 0; i < 100; i++) {
            map.put(i, "Item-" + i);
        }
    }

    @Test
    public void testIt() {
        try {
            Map<CharSequence, String> m = CollectionUtils.convertedKeyMap(CharSequence.class, map, ic);
            assertEquals(m.size(), map.size());
            for (int i = 0; i < 100; i++) {
                assertEquals("Item-" + i, m.get(Integer.toString(i)));
                assertTrue(m.containsKey(Integer.toString(i)));
            }
            m.put("200", "Item-200");
            for (Map.Entry<CharSequence, String> e : m.entrySet()) {
                int key = Integer.parseInt(e.getKey().toString());
                assertEquals(e.getValue(), map.get(key));
                assertTrue(m.containsKey(e.getKey()));
            }
            assertEquals("Item-200", map.get(200));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
