/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SymbolEscaperTest {

    SymbolEscaper esc = new SymbolEscaper(true);
    SymbolEscaper un = new SymbolEscaper(false);

    @Test
    public void testDelimited() {
        testDelimited("hello&goodbye&*hey-now", "hello_Ampersand_goodbye_Ampersand_Asterisk_hey_Hyphen_Minus_now");
        testDelimited("hello", "hello");
        testDelimited("3", "Three");
        testDelimited("&", "Ampersand");
        testDelimited("31hello\\goodbye", "Three_1hello_Backslash_goodbye");
    }

    @Test
    public void testUndelimited() {
        testUndelimited("hello&goodbye&*hey-now", "HelloAmpersandGoodbyeAmpersandAsteriskHeyHyphenMinusNow");
        testUndelimited("hello", "Hello");
        testUndelimited("3", "Three");
        testUndelimited("&", "Ampersand");
        testUndelimited("31hello\\goodbye", "Three1helloBackslashGoodbye");
    }

    private void testDelimited(String text, String expect) {
        String result = esc.escape(text);
        assertEquals(expect, result);
    }

    private void testUndelimited(String text, String expect) {
        String result = un.escape(text);
        assertEquals(expect, result);
    }

}
