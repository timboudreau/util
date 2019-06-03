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
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CaseInsensitiveStringMapTest {
    
    @Test
    public void testKey() {
        CharSequenceKey a = CharSequenceKey.create("Hello");
        CharSequenceKey b = CharSequenceKey.create("HELLO");
        CharSequenceKey c = CharSequenceKey.create("Hello");
        CharSequenceKey d = CharSequenceKey.create("hello");
        
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, c);
        assertEquals(c, a);
        assertEquals(a.hashCode(), c.hashCode());
        assertEquals(a, d);
        assertEquals(d, a);
        assertEquals(a.hashCode(), d.hashCode());
        
//        assertEquals(a, "Hello");
//        assertEquals("hello".hashCode(), a.hashCode());
        
    }   
    
    @Test
    public void testExistingMap() throws Exception {
            Map<CharSequence, String> m = new HashMap<>();
            m.put("hey", "you");
            m.put("Wookie", "food");
            m.put("TwentyThree", "skiddoo");
            
            Map<? extends CharSequence, String> cv = CollectionUtils.caseInsensitiveStringMap(m);
            for (CharSequence s : m.keySet()) {
                assertTrue("Key " + s + " missing", cv.containsKey(s));
                assertEquals(m.get(s), cv.get(s));
            }
            for (CharSequence s : m.keySet()) {
                assertTrue(cv.containsKey(s.toString().toUpperCase()));
                assertTrue(cv.containsKey(new StringBuilder(s.toString().toUpperCase())));
                assertEquals(m.get(s), cv.get(s.toString().toUpperCase()));
                assertEquals(m.get(s), cv.get(s.toString().toUpperCase()));
            }
            for (CharSequence s : m.keySet()) {
                assertTrue(cv.containsKey(s.toString().toLowerCase()));
                assertTrue(cv.containsKey(new StringBuilder(s.toString().toLowerCase())));
                assertEquals(m.get(s), cv.get(s.toString().toLowerCase()));
            }
        
    }

    @Test
    public void testNewMap() throws Exception {
        try {
            Map<CharSequence, String> m = CollectionUtils.caseInsensitiveStringMap();
            m.put("hey", "you");
            m.put("Wookie", "food");
            m.put("TwentyThree", "skiddoo");
            assertTrue(m.containsKey("hey"));
            assertTrue(m.containsKey("Wookie"));
            assertTrue(m.containsKey("Hey"));
            assertTrue(m.containsKey("wookie"));
            assertEquals("food", m.get("WOOKIE"));
            assertEquals("skiddoo", m.get("twentythrEE"));
            assertTrue(m.containsKey(new StringBuilder("twentyThree")));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
