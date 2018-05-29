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

import java.util.Arrays;
import java.util.Collection;
import static java.util.Collections.emptySet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ArrayBinarySetMutableTest {

    @Test
    public void testMutability() {
        ArrayBinarySetMutable<String> s = new ArrayBinarySetMutable<>(true, new CollectionUtils.ComparableComparator<>(), String.class);

        s.addAll(set("a", "b", "c", "d"));
        assertEquals(set("a", "b", "c", "d"), s);
        Iterator<String> it = s.iterator();
        while (it.hasNext()) {
            String str = it.next();
            switch (str) {
                case "a":
                    it.remove();
                    assertEquals(set("b", "c", "d"), s);
                    break;
                case "d":
                    it.remove();
                    assertEquals(set("b", "c"), s);
                    break;
            }
        }
        s.clear();
        assertTrue(s.isEmpty());

        assertFalse(s.contains("a"));
        s.add("a");
        assertTrue(s.contains("a"));
        assertFalse(s.contains("b"));
        s.add("b");
        assertTrue(s.contains("b"));
        assertFalse(s.contains("c"));
        s.add("c");
        assertTrue(s.contains("c"));
        assertEquals("a,b,c", s.toString());
        assertSetsMatch(set("a", "b", "c"), s);
        s.addAll(Arrays.asList("d", "e", "f", "g"));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g"), s);

        s.remove("f");
        s.remove("g");
        assertSetsMatch(set("a", "b", "c", "d", "e"), s);

        assertTrue("retainAll retvalue", s.retainAll(set("a", "b", "c")));
        assertSetsMatch(set("a", "b", "c"), s);

        assertTrue("removeAll retvalue", s.removeAll(set("a", "b")));
        assertSetsMatch(set("c"), s);

        assertFalse("removeAll retvalue", s.removeAll(set("a", "b")));
        assertSetsMatch(set("c"), s);

        assertFalse(s.removeAll(emptySet()));
        assertFalse(s.addAll(emptySet()));
        assertSetsMatch(set("c"), s);

        s.add("a");
        s.add("b");
        s.remove("a");
        assertFalse(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertEquals(2, s.size());
        assertFalse(s.isEmpty());
        assertSetsMatch(set("b", "c"), s);

        assertTrue(s.contains("b"));
        assertTrue(s.contains("c"));
        assertFalse(s.contains("a"));
        assertTrue(s.addAll(set("a", "b", "c", "d", "e", "f", "g", "h")));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g", "h"), s);

        assertTrue(s.retainAll(set("b", "c", "e", "g")));
        assertSetsMatch(set("b", "c", "e", "g"), s);

        assertTrue(s.retainAll(emptySet()));
        assertEquals(s + "", 0, s.size());
        assertTrue(s.isEmpty());
        s.add("a");
        s.add("b");
        assertEquals(2, s.size());
        s.clear();
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());

        s.addAll(Arrays.asList("a", "a", "a", "b", "b", "a"));
        assertSetsMatch(set("a", "b"), s);

        s.clear();
        assertEquals(0, s.size());
        s.addAll(Arrays.asList("a", "c", "e", "g"));
        s.addAll(Arrays.asList("b", "d", "f", "h"));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g", "h"), s);

        s.retainAll(set("b", "c", "g", "h"));
        assertSetsMatch(set("b", "c", "g", "h"), s);

        s.addAll(Arrays.asList("a", "c", "e", "g"));
        s.addAll(Arrays.asList("b", "d", "f", "h"));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g", "h"), s);

        s.retainAll(Arrays.asList("c", "c", "d", "d", "d"));
        assertSetsMatch(set("c", "d"), s);

        s.removeAll(set("q", "x", "z", "z", "y"));
        assertSetsMatch(set("c", "d"), s);

        assertTrue(s.addAll(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), s);

        assertTrue(s.removeAll(set("a", "c", "e", "g", "h", "j")));
        assertSetsMatch(set("b", "d", "f", "i"), s);

        assertTrue(s.addAll(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));
        assertSetsMatch(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), s);
        assertTrue(s.removeAll(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));
        assertTrue(s.toString(), s.isEmpty());

        assertTrue(s.addAll(set("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")));
        assertTrue(s.removeAll(set("a", "j")));
        assertSetsMatch(set("b", "c", "d", "e", "f", "g", "h", "i"), s);

        s = new ArrayBinarySetMutable<>(true, new CollectionUtils.ComparableComparator<>(), String.class);
        s.add("b");
        s.add("a");
        s.add("c");
        s.add("d");
        s.add("e");
        assertSetsMatch(set("a", "b", "c", "d", "e"), s);
        s.remove("b");
        assertSetsMatch(set("a", "c", "d", "e"), s);
    }

    static void assertSetsMatch(Set<String> expect, ArrayBinarySetMutable<String> test) {
        String msg = "Non-match: " + expect;
        Set<String> iterated = new HashSet<>();
        for (String t : test) {
            assertNotNull(t);
            assertTrue("Contains returns false for '" + t + "' but its iterator provides it: " + test,
                    test.contains(t));
            iterated.add(t);
        }
        assertEquals("Iterator mismatch", expect, iterated);
        assertEquals(msg + " - size " + test + " vs " + expect, expect.size(), test.size());
        assertTrue(msg + " - test contains all expect " + test, test.containsAll(expect));
        assertEquals(msg + " - hashCode " + test, expect.hashCode(), test.hashCode());
        assertTrue(msg + " - expect contains all test " + test, expect.containsAll(test));
        assertEquals(msg + " - equality " + test, expect, test);
        for (String s : expect) {
            assertTrue(msg + " - " + s + " in " + test, test.contains(s));
        }
    }

    private Set<String> set(String... strings) {
        return new HS(Arrays.asList(strings));
    }

    public static final class HS extends HashSet<String> {

        public HS() {
        }

        public HS(Collection<? extends String> c) {
            super(c);
        }

        public HS(int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
        }

        public HS(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                Object e = it.next();
//                System.out.println("TEST " + e + " in " + c + " from " + it);
                if (!contains(e)) {
//                    System.out.println("DOES NOT CONTAIN " + e);
                    return false;
                }
            }
            return true;
        }
    }
}
