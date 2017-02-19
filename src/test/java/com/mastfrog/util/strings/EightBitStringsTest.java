/*
 * The MIT License
 *
 * Copyright 2016 Tim Boudreau.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class EightBitStringsTest {

    @Test
    public void testCreate() {
        EightBitStrings strings = new EightBitStrings(false);
        CharSequence testOne = strings.create("A first test one");
        CharSequence testTwo = strings.create("A second test one");
        assertNotEquals(testOne, testTwo);

        CharSequence hello = strings.create("Hello world");
        CharSequence hello2 = strings.create("Hello world");
        assertEquals(hello, hello2);
        assertSame(hello, hello2);
        assertEquals(hello.hashCode(), "Hello world".hashCode());
        assertEquals("Hello world", hello.toString());
        assertEquals("Hello world".length(), hello.length());

        CharSequence worlds = strings.create("Hello worlds");
        assertNotEquals(hello2, worlds);

        assertEquals(hello, "Hello world");
    }

    @Test
    public void testInterning() {
        EightBitStrings strings = new EightBitStrings(false);
        List<String> all = new ArrayList<>(500);
        List<CharSequence> seqs = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            String s = randomString(5 + r.nextInt(20));
            all.add(s);
            seqs.add(strings.create(s));
        }
        int size = strings.internTableSize();
        Set<CharSequence> xseqs = new HashSet<>(seqs);
        int oldSize = xseqs.size();
        for (String again : all) {
            CharSequence ss = strings.create(again);
            xseqs.add(ss);
            assertEquals(size, strings.internTableSize());
            assertEquals(oldSize, xseqs.size());
        }
    }

    @Test
    public void testConcatenations() {
        EightBitStrings strings = new EightBitStrings(false);
        CharSequence concat = strings.concat("Hello ", "there ", "how ", "are ", "you?");
        CharSequence concat2 = strings.concat("Hello ", "there ", "how ", "are ", "you?");
        assertEquals("Hello there how are you?", concat.toString());
        assertEquals("Hello there how are you?".hashCode(), concat.toString().hashCode());
        assertEquals(concat, concat2);
    }

    @Test
    public void testSorting() {
        EightBitStrings strings = new EightBitStrings(false);
        ComparableCharSequence a = strings.concat(strings.create("a"), strings.create("b"), strings.create("c"), strings.create("d"));
        ComparableCharSequence b = strings.concat(strings.create("a"), strings.create("b"), strings.create("cd"));
        ComparableCharSequence c = strings.concat(strings.create("ab"), strings.create("cd"));
        ComparableCharSequence d = strings.concat(strings.create("a"), strings.create("bcd"));
        ComparableCharSequence e = strings.concat(strings.create("abcd"));
        assertEquals(4, a.length());
        assertEquals(4, b.length());
        assertEquals(4, c.length());
        assertEquals(4, d.length());
        assertEquals(4, e.length());
        for (ComparableCharSequence c1 : new ComparableCharSequence[]{a, b, c, d, e}) {
            for (ComparableCharSequence c2 : new ComparableCharSequence[]{a, b, c, d, e}) {
                assertEquals(c1, c2);
                int comp = c1.compareTo(c2);
                assertEquals("'" + c1 + "/ sort with '" + c2 + "' wrongly sorts to " + comp, 0, comp);
            }
        }

        ComparableCharSequence cs = strings.concatQuoted(Arrays.asList(strings.create("a"), strings.create("b"), strings.create("c"), strings.create("d")));
        assertEquals("\"a\" \"b\" \"c\" \"d\"", cs.toString());

        strings = new EightBitStrings(true);
        a = strings.concat("a", "b", "c", "d", "");
        b = strings.concat("a", "b", "cd");
        c = strings.concat("ab", "cd");
        d = strings.concat("a", "bcd");
        e = strings.concat("abcd");
        assertEquals(4, a.length());
        assertEquals(4, b.length());
        assertEquals(4, c.length());
        assertEquals(4, d.length());
        assertEquals(4, e.length());
        for (ComparableCharSequence c1 : new ComparableCharSequence[]{a, b, c, d, e}) {
            for (ComparableCharSequence c2 : new ComparableCharSequence[]{a, b, c, d, e}) {
                assertEquals(c1, c2);
                assertEquals(0, c1.compareTo(c2));
            }
        }

        strings = new EightBitStrings(false);
        a = strings.concat(strings.create("abc"), strings.create("def"), strings.create(""));
        b = strings.concat("bcd", strings.create("efg"));
        c = strings.concat("cde", strings.create("fgh"));
        d = strings.create("defghi");
        e = strings.create("efghij");
        List<ComparableCharSequence> l = Arrays.asList(e, c, d, a, b);
        Collections.sort(l);
        assertEquals(l, Arrays.asList(a, b, c, d, e));
    }

    List<String> stringsOf(List<CharSequence> cs) {
        List<String> result = new ArrayList<>();
        for (CharSequence c : cs) {
            if (c == null) {
                break;
            }
            result.add(c.toString());
        }
        return result;
    }

    @Test
    public void testAggressive() {
        EightBitStrings strings = new EightBitStrings(false, true);
        ComparableCharSequence c = strings.concat("com.foo.", "bar.baz.", "Hey$You");
        assertEquals("com.foo.bar.baz.Hey$You", c.toString());
        ComparableCharSequence c2 = strings.concat("com.foo.", "bar.whoodle.", "Hey$You");
        List<String> interned = stringsOf(strings.dumpInternTable());
        for (String cs : interned) {
            if (cs == null) {
                break;
            }
//            System.out.println("  " + cs);
        }
        assertTrue(interned.contains("com"));
        assertTrue(interned.contains("foo"));
        assertTrue(interned.contains("bar"));
        assertTrue(interned.contains("baz"));
        assertTrue(interned.contains("Hey"));
        assertTrue(interned.contains("You"));
        assertTrue(interned.contains("whoodle"));
        assertTrue(interned.contains("."));
        assertTrue(interned.contains("$"));
        assertTrue(interned.contains("\""));
    }

    @Test
    public void testMoreSorting() {
        EightBitStrings strings = new EightBitStrings(false, true);
        char[] chars = "QWERTYUIOPASDFGHJKLZXCVBNM<>?:}\"!@#$%^&*()_+1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]=-\\|`~ \t".toCharArray();
        Arrays.sort(chars);
        Random r = new Random(5);
        List<String> l = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            for (int j = 0; j < chars.length - (i + 1); j++) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < i; k++) {
                    if (k == 0) {
                        sb.append(chars[k]);
                    } else {
                        sb.append(chars[r.nextInt(chars.length)]);
                    }
                }
                l.add(sb.toString());
            }
            if (i == 4) {
                l.add("");
            }
        }

        List<ComparableCharSequence> ll = new ArrayList<>();
        for (String s : l) {
            ll.add(strings.create(s));
        }
        Collections.sort(l);
        Collections.sort(ll);
        for (int i = 0; i < l.size(); i++) {
            assertEquals("At " + i + " bad order:\n" + l + "\n" + ll, l.get(i), ll.get(i).toString());
        }
    }

    @Test
    public void testConcatQuoted() {
        EightBitStrings strings = new EightBitStrings(false);
        List<Object> l = Arrays.asList(strings.create("hey"), false, 23, strings.create("bar"), "baz");
        CharSequence cc = strings.concatQuoted(l);
        assertEquals("\"hey\" false 23 \"bar\" \"baz\"", cc.toString());
    }

    private static String randomString(int len) {
        char[] c = new char[len];
        for (int i = 0; i < c.length; i++) {
            c[i] = randomChar();
        }
        return new String(c);
    }

    private static char randomChar() {
        return alpha[r.nextInt(alpha.length)];
    }

    static final Random r = new Random(320392);
    private static final char[] alpha = "abcdefghijklmnopqrstuvwxyz".toCharArray();

}
