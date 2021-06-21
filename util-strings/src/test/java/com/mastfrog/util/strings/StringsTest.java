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

import com.mastfrog.util.preconditions.InvalidArgumentException;
import com.mastfrog.util.strings.Strings.CharPred;
import static com.mastfrog.util.strings.Strings.isDigits;
import static com.mastfrog.util.strings.Strings.is;
import static com.mastfrog.util.strings.Strings.isPositiveDecimal;
import static com.mastfrog.util.strings.Strings.quickJson;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.HashSet;
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
    public void testIsBlank() {
        assertBlankConsistent("");
        assertBlankConsistent("a");
        assertBlankConsistent(" ");
        assertBlankConsistent("   ");
        assertBlankConsistent("    b");
        assertBlankConsistent("     c");
        assertBlankConsistent("a     ");
        assertBlankConsistent("a    ");
        assertBlankConsistent("\r\n");
        assertBlankConsistent(" \r\n");
        assertBlankConsistent(" \r\nx");
        assertBlankConsistent(" x\r\n");
        assertBlankConsistent(" x \r\n");
        assertBlankConsistent(" x  \r\n");
        assertBlankConsistent(" \r\n\r\t");
        assertBlankConsistent("\t");
        assertBlankConsistent("wookie");
        assertBlankConsistent("wookie ");
        assertBlankConsistent(" wookie");
        assertBlankConsistent(" wookie ");
        assertBlankConsistent(" 6 ");
        assertBlankConsistent(" 6  ");
        assertBlankConsistent("  6  ");
    }

    private void assertBlankConsistent(String s) {
        assertEquals(s.trim().isEmpty(), Strings.isBlank(s));
    }

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
        assertEquals(count, stringForHash.size());
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

    @Test
    public void testEscaping() {
        String escaped = Strings.escapeControlCharactersAndQuotes("Foo\tbar\nAnother line\r\nHoo\fSlashes\\");
        assertEquals("Foo\\tbar\\nAnother line\\r\\nHoo\\fSlashes\\\\", escaped);
    }

    @Test
    public void testZeroPrefix() {
        assertEquals("05", Strings.zeroPrefix(5, 2));
        assertEquals("00", Strings.zeroPrefix(0, 2));
        assertEquals("00", Strings.zeroPrefix(-0, 2));
        assertEquals("-5", Strings.zeroPrefix(-5, 2));

        assertEquals("125", Strings.zeroPrefix(125, 2));
        assertEquals("-125", Strings.zeroPrefix(-125, 2));

        assertEquals("00125", Strings.zeroPrefix(125, 5));
        assertEquals("-0125", Strings.zeroPrefix(-125, 5));
    }

    @Test
    public void testZeroPrefixLong() {
        assertEquals("05", Strings.zeroPrefix(5L, 2));
        assertEquals("00", Strings.zeroPrefix(0L, 2));
        assertEquals("00", Strings.zeroPrefix(-0L, 2));
        assertEquals("-5", Strings.zeroPrefix(-5L, 2));

        assertEquals("125", Strings.zeroPrefix(125L, 2));
        assertEquals("-125", Strings.zeroPrefix(-125L, 2));

        assertEquals("00125", Strings.zeroPrefix(125L, 5));
        assertEquals("-0125", Strings.zeroPrefix(-125L, 5));
    }

    @Test
    public void testWriteInto() {
        assertWi("125", 125, 2);
        assertWi("005", 5, 3);
        assertWi("-05", -5, 3);
        assertWi("0125", 125, 4);
        assertWi("00125", 125, 5);
        assertWi("000125", 125, 6);
        assertWi("-125", -125, 4);
        assertWi("125", 125, 3);
        assertWi("125", 125, 2);
        assertWi("125", 125, 1);
        assertWi("125", 125, 0);
        assertWi("00000000000000000125", 125, 20);
        assertWi("-0000000000000000125", -125, 20);
        assertWi("0", 0, 0);
        assertWi("0", 0, 1);
        assertWi("-125", -125, 0);
        assertWi("-125", -125, 1);
        assertWi("-125", -125, 2);
        assertWi("-125", -125, 3);
        assertWi("-125", -125, 4);
        assertWi("-0125", -125, 5);
    }

    @Test
    public void testWriteIntoLong() {
        String IMV_60 = zp(Integer.MAX_VALUE, 60);
        String ImV_60 = zp(Integer.MIN_VALUE, 60);
        String LMV_60 = zp(Long.MAX_VALUE, 60);
        String LmV_60 = zp(Long.MIN_VALUE, 60);
        String LMVm1_60 = zp(Long.MAX_VALUE - 1L, 60);
        String LmVp1_60 = zp(Long.MIN_VALUE + 1L, 60);
        // sanity check string values for extremes
        assertEquals(60, IMV_60.length());
        assertEquals(60, ImV_60.length());
        assertEquals(60, LMV_60.length());
        assertEquals(60, LmV_60.length());
        assertEquals(60, LMVm1_60.length());
        assertEquals(60, LmVp1_60.length());

        assertEquals(Integer.MAX_VALUE, Integer.parseInt(IMV_60));
        assertEquals(Integer.MIN_VALUE, Integer.parseInt(ImV_60));

        assertEquals(Long.MAX_VALUE, Long.parseLong(LMV_60));
        assertEquals(Long.MIN_VALUE, Long.parseLong(LmV_60));
        assertEquals(Long.MAX_VALUE - 1L, Long.parseLong(LMVm1_60));
        assertEquals(Long.MIN_VALUE + 1L, Long.parseLong(LmVp1_60));

        assertWi("125", 125L, 2);
        assertWi("005", 5L, 3);
        assertWi("-05", -5L, 3);
        assertWi("0125", 125L, 4);
        assertWi("00125", 125L, 5);
        assertWi("000125", 125L, 6);
        assertWi("-125", -125L, 4);
        assertWi("125", 125L, 3);
        assertWi("125", 125L, 2);
        assertWi("125", 125L, 1);
        assertWi("125", 125L, 0);
        assertWi("00000000000000000125", 125L, 20);
        assertWi("-0000000000000000125", -125L, 20);
        assertWi("0", 0L, 0);
        assertWi("0", 0L, 1);
        assertWi("-125", -125L, 0);
        assertWi("-125", -125L, 1);
        assertWi("-125", -125L, 2);
        assertWi("-125", -125L, 3);
        assertWi("-125", -125L, 4);
        assertWi("-0125", -125L, 5);

        assertWi(LMVm1_60, Long.MAX_VALUE - 1L, 60);
        assertWi(LmVp1_60, Long.MIN_VALUE + 1L, 60);
        assertWi(LMV_60, Long.MAX_VALUE, 60);
        assertWi(IMV_60, Integer.MAX_VALUE, 60);
        assertWi(ImV_60, Integer.MIN_VALUE, 60);
        assertWi(LmV_60, Long.MIN_VALUE, 60);
    }

    static String zp(long value, int length) {
        // Doing zero padding the expensive way for comparison's sake
        boolean negative = value < 0L;
        boolean lmv = value == Long.MIN_VALUE;
        if (negative) {
            if (lmv) {
                // We have to special case Long.MIN_VALUE because
                // Long.MAX_VALUE = -9223372036854775808 and
                // Long.MIN_VALUE = 9223372036854775807
                // (note the difference in the last digit), so
                // Long.MIN_VALUE * -1 = Long.MAX_VALUE + 1 which wraps
                // around to Long.MIN_VALUE which is, wait for it...
                // Long.MIN_VALUE again.  So
                // -Long.MIN_VALUE == Long.MIN_VALUE.
                // Ain't corner cases grand.
                value = Long.MAX_VALUE;
            } else {
                long old = value;
                value = -value;
                if (value != 0L && value == old) {
                    fail("Huh? " + old + " * -1L = " + value + "?!!");
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        while (sb.length() < length) {
            sb.insert(0, '0');
        }
        if (negative) {
            if (sb.charAt(0) == '0') {
                sb.setCharAt(0, '-');
            } else {
                sb.insert(0, '-');
            }
        }
        if (lmv) {
            char c = (char) (sb.charAt(sb.length() - 1) + 1);
            sb.setCharAt(sb.length() - 1, c);
        }
        return sb.toString();
    }

    static void assertWi(String exp, int val, int length) {
        char[] c = new char[length];
        char[] result = Strings.writeInto(val, c);
        for (int i = 0; i < result.length; i++) {
            char cc = result[i];
            switch (cc) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '-':
                    continue;
                default:
                    fail("Not a number or minus: '" + cc + "' @ " + i + " in '" + new String(result) + "'");

            }
        }
        assertEquals("Wrong result for " + val + " with length " + length, exp, new String(result));
        assertEquals("Wrong parse result", Integer.parseInt(new String(result)), val);
    }

    static void assertWi(String exp, long val, int length) {
        char[] c = new char[length];
        char[] result = Strings.writeInto(val, c);
        for (int i = 0; i < result.length; i++) {
            char cc = result[i];
            switch (cc) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '-':
                    continue;
                default:
                    fail("Not a number or minus: '" + cc + "' @ " + i + " in '" + new String(result) + "'");
            }
        }
        assertEquals("Wrong result for " + val + " with length " + length, exp, new String(result));
        assertEquals("Wrong parse result", Long.parseLong(new String(result)), val);
    }

    @Test
    public void testQuote() throws Throwable {
        assertEquals("\"Hello\\nworld\"", Strings.quote("Hello\nworld"));
        List<String> lines = Arrays.asList("A\ttab", "Another line", "And another");
        String exp = "\"A\\ttab\",\n\"Another line\",\n\"And another\"";
        assertEquals(exp, Strings.quotedCommaDelimitedLines(lines));
        assertEquals("'This is a\\nthing!'", Strings.singleQuote("This is a\nthing!"));
    }

    @Test
    public void testEscaper() throws Throwable {
        Escaper weird = Escaper.CONTROL_CHARACTERS.escapeDoubleQuotes().and(c -> {
            switch (c) {
                case 'q':
                    return "QQ";
            }
            return null;
        });
        assertEquals("\\\"The QQueen QQuaffed QQuickly\\\"\\n",
                Strings.escape("\"The queen quaffed quickly\"\n", weird));
    }

    @Test
    public void testElide() {
        String base = "This is some text which will be elided for you";
        assertElidedTo("This is…for you", base, 16);

        assertElidedTo("This is some…elided for you", base, 28);
        assertElidedTo("This is some…be elided for you", base, 32);
        assertElidedTo("This is some text…elided for you", base, 36);
        assertElidedTo("This…you", base, 10);
        assertElidedTo("T…u", base, 3);
        assertElidedTo("T…u", base, 1);
        assertElidedTo("Th…ou", base, 5);
        assertElidedTo(base, base, base.length());
        assertElidedTo(base, base, 1000);
        try {
            Strings.elide("abc", -1);
            fail("Exception not thrown");
        } catch (InvalidArgumentException ex) {
            // ok
        }
        try {
            Strings.elide("abc", 0);
            fail("Exception not thrown");
        } catch (InvalidArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testTruncate() {
        String base = "This is some text which will be elided for you";
        for (int i = base.length() + 1; i >= 1; i--) {
            if (i >= base.length()) {
                assertTruncatedTo(base, base, i);
            } else {
                String exp = base.substring(0, i).trim() + '\u2026';
                CharSequence got = Strings.truncate(base, i);
                assertFalse("Zero length?", got.length() == 0);
                switch (i) {
                    case 45:
                    case 44:
                    case 43:
                    case 42:
                        assertTruncatedTo("This is some text which will be elided for…", base, i);
                        break;
                    case 41:
                        assertTruncatedTo("This is some text which will be elided fo…", base, i);
                        break;
                    case 40:
                    case 39:
                    case 38:
                        assertTruncatedTo("This is some text which will be elided…", base, i);
                        break;
                    case 37:
                        assertTruncatedTo("This is some text which will be elide…", base, i);
                        break;
                    case 36:
                    case 35:
                    case 34:
                    case 33:
                    case 32:
                    case 31:
                        assertTruncatedTo("This is some text which will be…", base, i);
                        break;
                    case 30:
                        assertTruncatedTo("This is some text which will b…", base, i);
                        break;
                    case 29:
                    case 28:
                        assertTruncatedTo("This is some text which will…", base, i);
                        break;
                    case 27:
                        assertTruncatedTo("This is some text which wil…", base, i);
                        break;
                    case 26:
                    case 25:
                    case 24:
                    case 23:
                        assertTruncatedTo("This is some text which…", base, i);
                        break;
                    case 22:
                        assertTruncatedTo("This is some text whic…", base, i);
                        break;
                    case 21:
                    case 20:
                    case 19:
                    case 18:
                        assertTruncatedTo("This is some text…", base, i);
                        break;
                    case 17:
                        assertTruncatedTo("This is some text…", base, i);
                        break;
                    case 16:
                        assertTruncatedTo("This is some tex…", base, i);
                        break;
                    case 15:
                    case 14:
                    case 13:
                    case 12:
                        assertTruncatedTo("This is some…", base, i);
                        break;
                    case 11:
                        assertTruncatedTo("This is som…", base, i);
                        break;
                    case 10:
                        assertTruncatedTo("This is…", base, i);
                        break;
                    case 9:
                    case 8:
                    case 7:
                        assertTruncatedTo("This is…", base, i);
                        break;
                    case 6:
                        assertTruncatedTo("This i…", base, i);
                        break;
                    case 5:
                    case 4:
                        assertTruncatedTo("This…", base, i);
                        break;
                    case 3:
                        assertTruncatedTo("Thi…", base, i);
                        break;
                    case 2:
                        assertTruncatedTo("Th…", base, i);
                        break;
                    case 1:
                        assertTruncatedTo("T…", base, i);
                        break;
                }
            }
        }
    }

    private static void assertElidedTo(String expect, CharSequence orig, int amount) {
        assertCharSequences(expect, Strings.elide(orig, amount));
    }

    private static void assertTruncatedTo(String expect, CharSequence orig, int amount) {
        CharSequence trunc = Strings.truncate(orig, amount);
        assertCharSequences(expect, trunc);
        assertTrue(trunc.length() <= amount + 1);
        assertEquals(expect.length(), trunc.length());
    }

    private static void assertCharSequences(String exp, CharSequence got) {
        assertEquals(exp, got.toString());
    }

    @Test
    public void testIsDigits() {
        assertFalse("Empty string cannot be digits", isDigits(""));
        assertTrue(isDigits("0"));
        assertTrue(isDigits("01"));
        assertTrue(isDigits("012"));
        assertTrue(isDigits("0123"));
        assertTrue(isDigits("01234"));

        assertFalse(isDigits("A"));
        assertFalse(isDigits("-"));
        assertFalse(isDigits("\u0000"));
        assertFalse(isDigits("0A"));
        assertFalse(isDigits("A0"));
        assertFalse(isDigits("A0A"));
        assertFalse(isDigits("0A0"));
        assertFalse(isDigits("0A00"));
        assertFalse(isDigits("00A00"));
        assertFalse(isDigits("00A0A"));
        assertFalse(isDigits("A0A0A"));
        assertTrue(isDigits("00000"));
        assertFalse(isDigits("0A000"));
        assertFalse(isDigits("000A0"));
        assertFalse(isDigits("0000A0"));
        assertFalse(isDigits("0000A"));
        assertFalse(isDigits("AAAAA"));
        assertFalse(isDigits("------"));
        assertFalse(isDigits(" "));
        assertFalse(isDigits(" 1233"));
    }

    @Test
    public void testIsPositiveDecimal() {
        assertTrue(isPositiveDecimal("0.1"));
        assertTrue(isPositiveDecimal("0.123"));
        assertTrue(isPositiveDecimal("0.1234"));
        assertTrue(isPositiveDecimal("0.12345"));
        assertTrue(isPositiveDecimal("10.12345"));
        assertTrue(isPositiveDecimal(".1"));
        assertTrue(isPositiveDecimal(".123"));
        assertTrue(isPositiveDecimal(".1234"));
        assertTrue(isPositiveDecimal(".12345"));
        assertTrue(isPositiveDecimal(".123456"));
        assertTrue(isPositiveDecimal(".1234567"));
        assertFalse(isPositiveDecimal("."));
        assertFalse(isPositiveDecimal("1."));
        assertFalse(isPositiveDecimal("12."));
        assertFalse(isPositiveDecimal("123."));
        assertFalse(isPositiveDecimal("1234."));
        assertFalse(isPositiveDecimal("12345."));
        assertFalse(isPositiveDecimal("a"));
        assertFalse(isPositiveDecimal("a."));
        assertFalse(isPositiveDecimal(".a."));
        assertFalse(isPositiveDecimal("a.a"));
        assertFalse(isPositiveDecimal("1.1.1"));
        assertFalse(isPositiveDecimal("12.3.12"));
        assertFalse(isPositiveDecimal("123.45.987"));
        assertFalse(isPositiveDecimal(".1234."));
        assertFalse(isPositiveDecimal("0.12345."));
    }

    @Test
    public void testIs() {
        EfficiencyCheckingCharPred cp = new EfficiencyCheckingCharPred(Character::isDigit);
        testOneIs(true, cp, "0");
        testOneIs(true, cp, "01");
        testOneIs(true, cp, "012");
        testOneIs(true, cp, "0123");
        testOneIs(true, cp, "01234");
        testOneIs(true, cp, "012345");
        testOneIs(true, cp, "0123456");
        testOneIs(true, cp, "01234567");
        testOneIs(true, cp, "012345678");
        testOneIs(true, cp, "0123456789");
        testOneIs(false, cp, "a0123456789");
        testOneIs(false, cp, "0a123456789");
        testOneIs(false, cp, "01a23456789");
        testOneIs(false, cp, "012a3456789");
        testOneIs(false, cp, "0123a456789");
        testOneIs(false, cp, "01234a56789");
        testOneIs(false, cp, "012345a6789");
        testOneIs(false, cp, "0123456a789");
        testOneIs(false, cp, "01234567a89");
        testOneIs(false, cp, "012345678a9");
        testOneIs(false, cp, "0123456789a");
        testOneIs(false, cp, "a");
        testOneIs(false, cp, "");
    }

    private void testOneIs(boolean expect, EfficiencyCheckingCharPred pred, String what) {
        if (expect) {
            assertTrue("Expected true with '" + what + "'", is(what, pred.reset(what)));
            Set<Character> shouldHaveTested = new HashSet<>();
            for (char c : what.toCharArray()) {
                shouldHaveTested.add(c);
            }
            if (!shouldHaveTested.equals(pred.calledFor)) {
                Set<Character> absent = new HashSet<>(shouldHaveTested);
                Set<Character> surprises = new HashSet<>(pred.calledFor);
                absent.removeAll(pred.calledFor);
                surprises.removeAll(shouldHaveTested);
                fail("Not called for all characters - missing " + absent
                        + (surprises.isEmpty() ? "" : " unexpected: " + surprises)
                        + " for '" + what + "' length " + (what.length() % 2 == 0 ? "even" : "odd"));
            }
        } else {
            assertFalse("Expected false with '" + what + "'", is(what, pred.reset(what)));
        }
    }

    static class EfficiencyCheckingCharPred implements CharPred {

        private final CharPred delegate;
        private final Set<Character> calledFor = new HashSet<>();
        private String msg;

        public EfficiencyCheckingCharPred(CharPred delegate) {
            this.delegate = delegate;
        }

        EfficiencyCheckingCharPred reset() {
            calledFor.clear();
            return this;
        }

        EfficiencyCheckingCharPred reset(String msg) {
            this.msg = msg;
            calledFor.clear();
            return this;
        }

        char addOrFail(char ch) {
            Character c = ch;
            assertFalse("Already called for " + c + " in "
                    + calledFor + (msg == null ? "" : ": " + msg), calledFor.contains(c));
            calledFor.add(c);
            return ch;
        }

        @Override
        public boolean test(char ch) {
            return delegate.test(addOrFail(ch));
        }
    }
}
