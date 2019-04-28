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
package com.mastfrog.util.tree;

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class BitSetSetTest {

    List<String> strings = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
    BitSet bits = new BitSet();
    BitSetSet<String> set = new BitSetSet<>(Indexed.forList(strings), bits);

    String str() {
        return set.toString() + ":" + bits.toString();
    }

    @Test
    public void testSomeMethod() {
        assertTrue(set.isEmpty());
        for (String s : strings) {
            assertFalse(set.contains(s));
        }
        set.add("B");
        assertFalse(set.isEmpty());
        assertFalse(bits.get(0));
        assertTrue(bits.get(1));
        assertEquals(1, set.size());
        set.add("C");
        assertEquals(str(), setOf("B", "C"), set);
        assertEquals(str(), setOf("B", "C"), found());
        set.add("D");
        set.add("J");
        set.add("I");
        assertEquals(str(), setOf("B", "C", "D", "J", "I"), set);
        assertEquals(str(), 5, set.size());

        set.remove("D");
        assertFalse(bits.get(3));
        assertEquals(setOf("B", "C", "J", "I"), set);
        assertEquals(str(), set, found());
        assertFalse(str(), set.contains("D"));
        assertFalse(str(), set.contains("A"));
        for (String s : set) {
            assertTrue(s + ": " + str(), set.contains(s));
        }
        set.clear();
        assertTrue(set.isEmpty());
    }

    private Set<String> found() {
        Set<String> result = new HashSet<>();
        for (String s : set) {
            result.add(s);
        }
        return result;
    }

}
