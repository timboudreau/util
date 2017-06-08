
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
package com.mastfrog.util;

import com.mastfrog.util.strings.ComparableCharSequence;
import com.mastfrog.util.strings.EightBitStrings;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
        List<CharSequence> cs = Strings.splitToList(',', s);
        int ix = 0;
        for (CharSequence ss : cs) {
            switch (ix++) {
                case 0:
                    assertEquals("hello", ss);
                    break;
                case 1:
                    assertEquals("there", ss);
                    break;
                case 2:
                    assertEquals("world", ss);
                    break;
                case 3:
                    assertEquals("how", ss);
                    break;
                case 4:
                    assertEquals("are", ss);
                    break;
                case 5:
                    assertEquals("you", ss);
                    break;
                default:
                    fail("Bad value '" + ss + "'");

            }
        }
        assertEquals(6, ix);
        s = "hello,,there";
        cs = Strings.splitToList(',', s);
        ix = 0;
        for (CharSequence ss : cs) {
            switch (ix++) {
                case 0:
                    assertEquals("hello", ss);
                    break;
                case 1:
                    assertEquals("", ss);
                    break;
                case 2:
                    assertEquals("there", ss);
                    break;
                default:
                    fail("Bad value '" + ss + "'");
            }
        }
        assertEquals(3, ix);
        cs = Strings.splitToList(',', ",");
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
    
    public void testStartsWith2() {
        assertTrue(Strings.startsWith("bytes=1-10", "bytes="));
        assertFalse(Strings.startsWith("BYTES=1-10", "bytes="));
        assertFalse(Strings.startsWithIgnoreCase("BYTES=1-10", "bytes="));
    }

    @Test
    public void testSplit2() {
        EightBitStrings strs = new EightBitStrings(false, true, true);
        ComparableCharSequence seq = strs.create("hello world how are you ");
        CharSequence[] result = Strings.split(' ', seq);
        assertEquals(Arrays.asList(result).toString(), 5, result.length);
        String[] actual = seq.toString().split("\\s");
        for (int i = 0; i < result.length; i++) {
            assertEquals(actual[i], result[i].toString());
            assertTrue(result[i] + " vs " + actual[i], Strings.charSequencesEqual(actual[i], result[i], false));
        }
    }

    private final String test = "Mastfrog is awesome!";
    private final String unlike = test + " ";
    private final EightBitStrings strings = new EightBitStrings(true, true, true);
    private final CharSequence ascii = strings.create("Mastfrog is awesome!");
    private final CharSequence upper = strings.create("MASTFROG IS AWESOME!");

    @Test
    public void testEquality() {
        assertTrue(Strings.charSequencesEqual(test, ascii, false));
        assertTrue(Strings.charSequencesEqual(test, ascii, true));
        assertTrue(Strings.charSequencesEqual(test, upper, true));
        assertFalse(Strings.charSequencesEqual(test, upper, false));
        assertFalse(Strings.charSequencesEqual(test, unlike, false));
        assertFalse(Strings.charSequencesEqual(ascii, unlike, false));
        assertFalse(Strings.charSequencesEqual(upper, unlike, false));
        assertFalse(Strings.charSequencesEqual(test, unlike, true));
        assertFalse(Strings.charSequencesEqual(ascii, unlike, true));
        assertFalse(Strings.charSequencesEqual(upper, unlike, true));
    }

    @Test
    public void testHashCode() {
        assertEquals(test.hashCode(), Strings.charSequenceHashCode(test, false));
        assertEquals(test.toLowerCase().hashCode(), Strings.charSequenceHashCode(test, true));
        assertEquals(test.hashCode(), Strings.charSequenceHashCode(ascii, false));
        assertNotEquals(test.hashCode(), Strings.charSequenceHashCode(unlike, false));
        assertNotEquals(test.hashCode(), Strings.charSequenceHashCode(unlike, true));
    }

    @Test
    public void testCharSequenceContains() {
        EightBitStrings str = new EightBitStrings(false, true, true);
        CharSequence lookFor = str.create(" Hello! ");

        StringBuilder sb = new StringBuilder();
        char at = 'A';
        for (char x = 'A'; x <= 'Z'; x++) {
            for (char c = 'A'; c <= 'Z'; c++) {
                if (c == at) {
                    sb.append(lookFor);
                }
                sb.append(c);
            }
            at++;
            assertTrue(sb.toString(), Strings.charSequenceContains(sb, lookFor, false));
            sb.setLength(0);
        }
        // test last position
        sb = new StringBuilder("ABCDEFGHIJKLMNOPQRSTUVWXYZ").append(lookFor);
        assertTrue(sb.toString(), Strings.charSequenceContains(sb, lookFor, false));
        
        sb = new StringBuilder(lookFor);
        assertTrue(Strings.charSequenceContains(sb, lookFor, false));
    }
    
    @Test
    public void testParseLong() {
        long lval = Long.MAX_VALUE;
        for (;;) {
            long prev = lval;
            long test = Strings.parseLong(Long.toString(lval));
            assertEquals(lval, test);
            test = Strings.parseLong(Long.toString(-lval));
            assertEquals(-lval, test);
            lval /= 2;
            if (lval == prev) {
                break;
            }
        }
    }
    
    @Test
    public void testParseInt() {
        int lval = Integer.MAX_VALUE;
        for (;;) {
            int prev = lval;
            int test = Strings.parseInt(Long.toString(lval));
            assertEquals(lval, test);
            test = Strings.parseInt(Long.toString(-lval));
            assertEquals(-lval, test);
            lval /= 2;
            if (lval == prev) {
                break;
            }
        }
    }
    
    @Test(expected=NumberFormatException.class)
    public void testParseTooLargeInt() {
        Strings.parseInt(Long.toString((long) Integer.MAX_VALUE + 1L));
    }
    
    @Test(expected=NumberFormatException.class)
    public void testParseTooLargeLong() {
        Strings.parseLong(Long.toString(Long.MAX_VALUE) + "0");
    }
    
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidLong() {
        Strings.parseLong("gx0321");
    }
   
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidInt() {
        Strings.parseInt("gx0321");
    }
    
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidLong2() {
        Strings.parseLong("3 72 ");
    }
   
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidInt2() {
        Strings.parseInt("5 41");
    }
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidLong3() {
        Strings.parseLong(" 372");
    }
   
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidInt3() {
        Strings.parseInt(" 541");
    }
    
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidLong4() {
        Strings.parseLong("--372");
    }
   
    @Test(expected=NumberFormatException.class)
    public void testParseInvalidInt4() {
        Strings.parseInt("--541");
    }
}
