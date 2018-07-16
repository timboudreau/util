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
package com.mastfrog.util.strings;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MatchWordsTest {

    @Test
    public void testPositiveMatches() {
        MatchWords mw = new MatchWords(new String[]{"javax/swing", "javax/sql"}, true);
        assertTrue(mw.test("javax/swing"));
        assertTrue(mw.test("javax/sql"));
        assertTrue(mw.test("javax/swing/test"));
        assertTrue(mw.test("javax/sql/test/more/depth"));
    }

    @Test
    public void testNegativeMatches() {
        MatchWords mw = new MatchWords(new String[]{"javax/swing", "javax/sql"}, true);
        assertFalse(mw.test("javax/swin"));
        assertFalse(mw.test("javax/I_SHOULD_NOT_MATCH/sql"));
        assertFalse(mw.test("java"));
        assertFalse(mw.test("javax/"));
        assertFalse(mw.test("j"));
        assertFalse(mw.test(""));
    }

    @Test
    public void testComplex() {
        MatchWords mw = new MatchWords(new String[]{"argle/bargle", "com/foo", "wookie/boom", "aaa/bbb", "yippity/doo-dah", "123/456"}, true);
        assertTrue(mw.test("argle/bargle/foo"));
        assertFalse(mw.test("bom/foo"));
        assertTrue(mw.test("wookie/boom"));
        assertTrue(mw.test("wookie/boom/bam"));
        assertTrue(mw.test("wookie/boom/bam/boo"));
        assertFalse(mw.test("zippity/doo-dah"));
    }

    @Test
    public void testExactMatch() {
        String[] test = new String[] {"hey", "you", "now", "what", "spectrogram", "astonishing", "tubular", "tweedy", "he", "hebrew"};
        MatchWords mw = new MatchWords(test, false);
        assertFalse(mw.test("tweed"));
        for (String s : test) {
            assertTrue(mw.test(s));
            assertFalse(mw.test(" " + s));
            assertFalse(mw.test(s + " "));
            assertFalse(mw.test(s + "you"));
            assertFalse(mw.test(s.toUpperCase()));
        }
    }
}
