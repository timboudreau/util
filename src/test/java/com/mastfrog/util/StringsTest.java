/*
 * The MIT License
 *
 * Copyright 2017 tim.
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

package com.mastfrog.util;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class StringsTest {

    @Test
    public void testSplit() {
        String s = "hello,there,world,how,are,you";
        List<CharSequence> cs = Strings.split(',', s);
        int ix = 0;
        for (CharSequence ss : cs) {
            switch(ix++) {
                case 0 :
                    assertEquals("hello", ss);
                    break;
                case 1:
                    assertEquals("there", ss);
                    break;
                case 2 :
                    assertEquals("world", ss);
                    break;
                case 3 :
                    assertEquals("how", ss);
                    break;
                case 4 :
                    assertEquals("are", ss);
                    break;
                case 5 :
                    assertEquals("you", ss);
                    break;
                default :
                    fail("Bad value '" + ss + "'");
                    
            }
        }
        assertEquals(6, ix);
        s = "hello,,there";
        cs = Strings.split(',', s);
        ix = 0;
        for (CharSequence ss : cs) {
            switch(ix++) {
                case 0 :
                    assertEquals("hello", ss);
                    break;
                case 1:
                    assertEquals("", ss);
                    break;
                case 2 :
                    assertEquals("there", ss);
                    break;
                default :
                    fail("Bad value '" + ss + "'");
            }
        }
        assertEquals(3, ix);
        cs = Strings.split(',', ",");
        assertEquals(1, cs.size());
        assertEquals("", cs.get(0));
    }
    
    public void testStartsWith() {
        StringBuilder a = new StringBuilder("hello there");
        StringBuilder b = new StringBuilder("hello");
        assertTrue(Strings.startsWith(a, b));
        b = new StringBuilder("HELlO");
        assertFalse(Strings.startsWith(a, b));
        assertTrue(Strings.startsWithIgnoreCase(a, b));
        assertFalse(Strings.startsWith(b, a));
    }
}
