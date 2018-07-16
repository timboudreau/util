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

import java.util.Map;
import java.util.function.ToLongFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ImmutableArrayMapTest {

    @Test
    public void testSomeMethod() {
        Map<String,Integer> m = CollectionUtils.<String,Integer>map("1000").to(23)
                .map("2000").to(20).map("3000").to(4)
                .map("23").to(5).map("200").to(10)
                .map("42").to(12).map("4139").to(24).build();
        Map<String,Integer> got = CollectionUtils.immutableArrayMap(m, String.class, Integer.class, new StringToLong());
        int last = Integer.MIN_VALUE;
        for (Map.Entry<String,Integer> e : got.entrySet()) {
            int val = Integer.parseInt(e.getKey());
            assertTrue(val > last);
            last = val;
        }
        assertEquals(m.size(), got.size());
        for (Map.Entry<String,Integer> e : m.entrySet()) {
            assertEquals(e.getValue(), got.get(e.getKey()));
        }
    }

    static class StringToLong implements ToLongFunction<String> {
        @Override
        public long applyAsLong(String value) {
            return Long.parseLong(value);
        }
    }
}
