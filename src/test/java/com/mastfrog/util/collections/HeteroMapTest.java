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

import com.mastfrog.util.collections.HeteroMap.Key;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class HeteroMapTest {
    
    Key<String> a = HeteroMap.newKey(String.class, "first");
    Key<String> b = HeteroMap.newKey(String.class, "second");
    Key<String> c = HeteroMap.newKey(String.class, "first");
    
    Key<Integer> ia = HeteroMap.newKey(Integer.class, "int1");
    Key<Integer> ib = HeteroMap.newKey(Integer.class, "int2");

    @Test
    public void testSomeMethod() {
        HeteroMap m = new HeteroMap();
        m.put(a, "hello");
        assertEquals("hello", m.get(a));
        assertNull(m.get(String.class));
        m.put(b, "goodbye");
        assertEquals("hello", m.get(a));
        assertEquals("goodbye", m.get(b));
        
        m.put(a, "seeya");
        assertEquals("seeya", m.get(a));
        assertEquals("goodbye", m.get(b));
        m.remove(a);
        assertNull(m.get(a));
        
        
        m.put(ia, 23);
        m.put(ib, 42);
        assertEquals(Integer.valueOf(23), m.get(ia));
        assertEquals(Integer.valueOf(42), m.get(ib));
        
        m.put("hooger");
        assertEquals("hooger", m.get(String.class));
        
        m.remove(String.class);
        assertNull(m.get(String.class));
    }
    
}
