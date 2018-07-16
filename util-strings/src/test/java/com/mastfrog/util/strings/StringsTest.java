
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

import static com.mastfrog.util.strings.Strings.quickJson;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class StringsTest {

    @Test
    public void testSha1() {
        String a = "abcdefghij";
        String b = "abcdefghijklmn";
        String c = "abqrstghij";
        assertNotEquals(Strings.sha1(a), Strings.sha1(b));
        assertNotEquals(Strings.sha1(a), Strings.sha1(c));
    }

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

    @Test(expected = NumberFormatException.class)
    public void testParseTooLargeInt() {
        Strings.parseInt(Long.toString((long) Integer.MAX_VALUE + 1L));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseTooLargeLong() {
        Strings.parseLong(Long.toString(Long.MAX_VALUE) + "0");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidLong() {
        Strings.parseLong("gx0321");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidInt() {
        Strings.parseInt("gx0321");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidLong2() {
        Strings.parseLong("3 72 ");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidInt2() {
        Strings.parseInt("5 41");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidLong3() {
        Strings.parseLong(" 372");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidInt3() {
        Strings.parseInt(" 541");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidLong4() {
        Strings.parseLong("--372");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidInt4() {
        Strings.parseInt("--541");
    }

    @Test(timeout = 1000)
    public void testLiteralReplace() {
        assertEquals("This is a thing", Strings.literalReplaceAll("{{verb}}", "is", "This {{verb}} a thing"));
        assertEquals("It eats a thing that eats food", Strings.literalReplaceAll("{{verb}}", "eats", "It {{verb}} a thing that {{verb}} food"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalReplace() {
        Strings.literalReplaceAll("{{foo}}", "{{foo}}", "xyzqkasdfh");
    }

    @Test
    public void testOverlap() {
        String exp = "xyzqkasdfh";
        String got = Strings.literalReplaceAll("{{foo}}", "This is {{foo}}", "xyzqkasdfh");
        assertEquals(exp, got);

        exp = "xyzqkasdfh This is {{foo}}";
        got = Strings.literalReplaceAll("{{foo}}", "This is {{foo}}", "xyzqkasdfh {{foo}}");
        assertEquals(exp, got);

        exp = "xyzqkasdfh bar";
        got = Strings.literalReplaceAll("{{foo}}", "bar", "xyzqkasdfh {{foo}}");
        assertEquals(exp, got);

        exp = "xyzqkasdfh {{foo}}";
        got = Strings.literalReplaceAll("{{fooo}}", "bar", "xyzqkasdfh {{foo}}");
        assertEquals(exp, got);

    }

    @Test
    public void testOffsetBug() {
        String searchFor = "gzip";
        String searchIn = "identity;q=1, *;q=0";
        assertFalse(Strings.charSequenceContains(searchIn, searchFor, true));
    }

    static Set<Character> setOf(char[] chars) {
        Set<Character> result = new LinkedHashSet<>();
        for (char c : chars) {
            result.add(c);
        }
        return result;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testShuffleAndExtract() {
        Random rnd = new Random(34948394);
        String s = "HelloWorldAndZebras";
        Set<Character> chars = setOf(s.toCharArray());
        for (int i = 0; i < 10; i++) {
            String t = Strings.shuffleAndExtract(rnd, s, 4);
            for (char c : t.toCharArray()) {
                assertTrue(chars.contains(c));
            }
        }
    }

    public static final String[] TRIMMABLE = new String[]{" some", "strings", " that ", "   need\t", "trimming\n "};
    private static final String TRIMMABLE_TRIMMED_COMMA_DELIM
            = "some,strings,that,need,trimming";
    private static final String[] TRIMMABLE_TRIMMED = TRIMMABLE_TRIMMED_COMMA_DELIM.split(",");

    @Test
    public void testTrimStringArray() {
        String[] in = TRIMMABLE;
        String[] expect = TRIMMABLE_TRIMMED;
        assertArrayEquals(expect, Strings.trim(in));
        assertSame(TRIMMABLE, in);
        assertEquals("Input array should not have been altered", " some", in[0]);
        in = new String[]{"", " some", "\n", "strings", " that ", "   ", "   need\t", "trimming\n ", " "};
        assertArrayEquals(expect, Strings.trim(in));
        in = new String[]{"", " ", ""};
        assertArrayEquals(new String[0], Strings.trim(in));
    }

    @Test
    public void testTrimCharSequenceArray() {
        EightBitStrings ebs = new EightBitStrings(false, true, true);
        CharSequence[] in = new CharSequence[]{ebs.create("  some"), ebs.create("strings"), ebs.create(" that "),
            ebs.create("   need\t"), ebs.create("trimming\n")};
        CharSequence[] got = Strings.trim(in);
        assertEquals("some,strings,that,need,trimming", Strings.join(',', got).toString());

        assertEquals("Input array should not have been altered", ebs.create("  some"), in[0]);
        in = new CharSequence[]{ebs.create("  some"), ebs.create(""), ebs.create("strings"),
            ebs.create("\n"), ebs.create(" that "), ebs.create("   "),
            ebs.create("   need\t"), ebs.create("trimming\n"), ebs.create(" "), ebs.create("\t")};
        got = Strings.trim(in);
        assertEquals("some,strings,that,need,trimming", Strings.join(',', got).toString());
    }

    @SafeVarargs
    private static final <T> Set<T> setOf(T... items) {
        return new LinkedHashSet<>(Arrays.asList(items));
    }

    @Test
    public void testTrimStringSet() {
        Set<String> in = setOf(Arrays.copyOf(TRIMMABLE, TRIMMABLE.length));
        Set<String> got = Strings.trim(in);
        assertEquals(setOf(Arrays.copyOf(TRIMMABLE_TRIMMED, TRIMMABLE_TRIMMED.length)), got);

        in = new TreeSet<>(in);
        got = Strings.trim(in);
        assertEquals(new TreeSet<>(Arrays.asList(TRIMMABLE_TRIMMED)), got);
        assertTrue(got instanceof SortedSet<?>);
    }

    @Test
    public void testTrimStringList() {
        List<String> in = asList(TRIMMABLE);
        List<String> got = Strings.trim(in);
        assertEquals(asList(TRIMMABLE_TRIMMED), got);
        assertEquals("Input list should not have been altered", TRIMMABLE[0], in.iterator().next());
    }

    @Test
    public void testReverse() {
        assertEquals("edcba", Strings.reverse("abcde"));
        assertEquals("", Strings.reverse(""));
        assertEquals("fedcba", Strings.reverse("abcdef"));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("Bearer abcd", "bearer"));
        assertTrue(Strings.startsWithIgnoreCase("bearer abcd", "bearer"));
        assertFalse(Strings.startsWithIgnoreCase("bear", "bearer"));
        assertFalse(Strings.startsWithIgnoreCase("", "bearer"));
        assertFalse(Strings.startsWithIgnoreCase("woogle", "bearer"));
    }

    @Test
    public void testSimpleJson() {
        CharSequence cs = quickJson("name", "Joe", "age", 29, "single", true, "arr", new int[]{3, 5, 10},
                "thingWithQuotes", "He said \"WTF?!\"\nthen\ttabbed");
        assertEquals("{\"name\":\"Joe\",\"age\":29,\"single\":true,\"arr\":[3,5,10],\"thingWithQuotes\":\"He said \\\"WTF?!\\\"\\nthen\\ttabbed\"}",
                cs.toString());
    }

    @Test
    public void testSplitEmpty() {
        CharSequence[] seqs = Strings.split(';', "");
        assertEquals(0, seqs.length);
    }

    @Test
    public void testSplitTrailing() {
        String s = "max-age=60;includeSubDomains;";
        CharSequence[] seqs = Strings.split(';', s);
        assertTrue(seqs[0] instanceof String);
        assertEquals("max-age=60", seqs[0]);
        assertEquals("includeSubDomains", seqs[1]);
        assertEquals(2, seqs.length);
    }

    @Test
    public void testSplitUniqueTrailing() {
        String s = "max-age=60;includeSubDomains;";
        CharSequence[] seqs = Strings.splitUniqueNoEmpty(';', s).toArray(new CharSequence[0]);
        for (int i = 0; i < seqs.length; i++) {
            seqs[i] = Strings.trim(seqs[i]);
        }
        assertTrue(seqs[0] instanceof String);
        assertEquals("max-age=60", seqs[0]);
        assertEquals("includeSubDomains", seqs[1]);
        assertEquals(2, seqs.length);
    }

    @Test
    public void testSplitLambdaTrailing() {
        String s = "max-age=60;includeSubDomains;";
        Strings.split(';', s, cs -> {
            if (cs.charAt(cs.length() - 1) == ';') {
                fail("Delimiter included: '" + cs + "'");
            }
            return true;
        });
    }

    @Test
    public void testStringsSha() {
        StringBuilder sb = new StringBuilder();
        int count = 20;
        char base = 'A';
        Map<String, String> stringForHash = new HashMap<>();

        for (int i = 0; i < count; i++) {
            String hash = Strings.sha1(sb.toString());
            if (stringForHash.containsKey(hash)) {
                fail(stringForHash.get(hash) + " and " + sb.toString() + " hash to the same value");
            }
            stringForHash.put(hash, sb.toString());
            sb.append(base);
            base++;
        }
        assertEquals(count,stringForHash.size());
    }

    @Test
    public void testStringsTrim() {
        StringBuilder a = new StringBuilder(" foo bar");
        StringBuilder b = new StringBuilder(" foo bar  ");
        StringBuilder c = new StringBuilder("foo bar  ");
        StringBuilder d = new StringBuilder("     foo bar  ");
        assertEquals("foo bar", Strings.trim(a).toString());
        assertEquals("foo bar", Strings.trim(b).toString());
        assertEquals("foo bar", Strings.trim(c).toString());
        assertEquals("foo bar", Strings.trim(d).toString());
        assertEquals("", Strings.trim(new StringBuilder("")).toString());
        assertEquals("", Strings.trim(new StringBuilder(" ")).toString());
        assertEquals("", Strings.trim(new StringBuilder("   ")).toString());
        assertEquals("", Strings.trim(new StringBuilder("    ")).toString());
    }
}
