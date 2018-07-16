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
package com.mastfrog.util.strings;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class AppendableCharSequenceTest {

    @Test
    public void testCharSequences() throws Exception {
        String expect = "one two three";
        char[] chars = expect.toCharArray();
        AppendableCharSequence s = new AppendableCharSequence();
        s.append("one").append(' ').append("two").append(' ').append("three");
        assertEquals(5, s.elementCount());
        assertEquals(expect.length(), s.length());
        assertEquals("one two three", s.toString());
        for (int i = 0; i < chars.length; i++) {
            assertEquals("Mismatch at " + i + " '" + chars[i] + "' vs '" + s.charAt(i) + "'", chars[i], s.charAt(i));
        }
        testSubstrings(expect, s);
    }

    private void testSubstrings(String expect, CharSequence s) {
        char[] chars = expect.toCharArray();
        int ix = 0;
        for (int i = 0; i <= chars.length; i++) {
            for (int j = i; j <= chars.length; j++) {
                CharSequence sub = s.subSequence(i, j);
                CharSequence expectedSubstring = expect.subSequence(i, j);
                assertEquals(sub.toString(), sub.length(), sub.toString().length());
                assertEquals(expectedSubstring + " vs. " + sub + " lengths differ", expectedSubstring.length(), sub.length());
                assertEquals("FAIL " + ix + " Bad result on sub from " + i + " to " + j, expectedSubstring, sub.toString());
                for (int k = 0; k < expectedSubstring.length(); k++) {
                    char ce = expectedSubstring.charAt(k);
                    char se = sub.charAt(k);
                    assertEquals("Mismatch at " + k + " in " + ce + " vs. " + sub, ce, se);
                }
                ix++;
            }
        }
    }

    public void testConstructor() throws Exception {
        AppendableCharSequence s = new AppendableCharSequence("hello", " ", "world");
        assertEquals("hello world", s.toString());
    }

    @Test
    public void test() {
        assertTrue(true);
        AppendableCharSequence cs = new AppendableCharSequence();
        cs.append("abc").append("def").append("ghi");
        assertString("abcdefghi", cs);
        assertString("abcdefghi", cs.subSequence(0, cs.length()));
        assertString("ab", cs.subSequence(0, 2));
        assertString("abc", cs.subSequence(0, 3));
        assertString("abcd", cs.subSequence(0, 4));
        assertString("abcde", cs.subSequence(0, 5));
        assertString("abcdef", cs.subSequence(0, 6));
        assertString("abcdefg", cs.subSequence(0, 7));
        assertString("abcdefgh", cs.subSequence(0, 8));
        assertString("abcdefghi", cs.subSequence(0, 9));

        assertString("b", cs.subSequence(1, 2));
        assertString("bc", cs.subSequence(1, 3));
        assertString("bcd", cs.subSequence(1, 4));
        assertString("bcde", cs.subSequence(1, 5));
        assertString("bcdef", cs.subSequence(1, 6));
        assertString("bcdefg", cs.subSequence(1, 7));
        assertString("bcdefgh", cs.subSequence(1, 8));
        assertString("bcdefghi", cs.subSequence(1, 9));

        assertString("c", cs.subSequence(2, 3));
        assertString("cd", cs.subSequence(2, 4));
        assertString("cde", cs.subSequence(2, 5));
        assertString("cdef", cs.subSequence(2, 6));
        assertString("cdefg", cs.subSequence(2, 7));
        assertString("cdefgh", cs.subSequence(2, 8));
        assertString("cdefghi", cs.subSequence(2, 9));

        assertString("d", cs.subSequence(3, 4));
        assertString("de", cs.subSequence(3, 5));
        assertString("def", cs.subSequence(3, 6));
        assertString("defg", cs.subSequence(3, 7));
        assertString("defgh", cs.subSequence(3, 8));
        assertString("defghi", cs.subSequence(3, 9));

        assertString("e", cs.subSequence(4, 5));
        assertString("ef", cs.subSequence(4, 6));
        assertString("efg", cs.subSequence(4, 7));
        assertString("efgh", cs.subSequence(4, 8));
        assertString("efghi", cs.subSequence(4, 9));
    }

    private AppendableCharSequenceTest assertString(CharSequence a, CharSequence b) {
        assertEquals(a.toString(), b.toString());
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(b, a);
        return this;
    }

}
