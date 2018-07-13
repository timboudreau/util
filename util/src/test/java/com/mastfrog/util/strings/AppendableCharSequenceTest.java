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

import java.io.IOException;
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
}
