/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import static com.mastfrog.util.collections.CollectionUtils.hashingMap;
import static com.mastfrog.util.collections.CollectionUtils.map;
import com.mastfrog.util.multivariate.Pair;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class MapBuilderTest {

    @Test
    public void testMapBuilder() {
        Map<String, Object> m = map("foo").to("bar").map("baz").to(23).map("quux").finallyTo(true);
        assertNotNull(m);
        assertEquals(3, m.size());
        assertEquals("bar", m.get("foo"));
        assertEquals(23, m.get("baz"));
        assertEquals(true, m.get("quux"));
    }

    @Test
    public void testHashingMapBuilder() {
        Pair<Map<String, Object>, String> p = hashingMap("foo").to("bar").map("baz").to(23).map("quux").toAndBuildWithStringHash(true);
        Map<String, Object> m = p.a;
        assertEquals(3, m.size());
        assertEquals("bar", m.get("foo"));
        assertEquals(23, m.get("baz"));
        assertEquals(true, m.get("quux"));
        assertNotNull(p.b);
        assertFalse(p.b.length() == 0);

        Pair<Map<String, Object>, String> p2 = hashingMap("foo").to("bar").map("baz").to(23).map("quux").toAndBuildWithStringHash(true);
        assertNotNull(p2);
        assertNotNull(p2.a);
        assertNotNull(p2.b);
        assertEquals(p.b, p2.b);
    }
}
