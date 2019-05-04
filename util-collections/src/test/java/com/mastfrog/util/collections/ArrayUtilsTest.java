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
package com.mastfrog.util.collections;

import static com.mastfrog.util.collections.ArrayUtils.arrayOf;
import static com.mastfrog.util.collections.ArrayUtils.concatenate;
import static com.mastfrog.util.collections.ArrayUtils.concatenateAll;
import static com.mastfrog.util.collections.ArrayUtils.dedup;
import static com.mastfrog.util.collections.ArrayUtils.dedupByType;
import static com.mastfrog.util.collections.ArrayUtils.emptyForNull;
import static com.mastfrog.util.collections.ArrayUtils.flatten;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ArrayUtilsTest {

    @Test
    public void testMultiConcatenateByteArrays() {
        byte[] a = "Hello".getBytes(UTF_8);
        byte[] b = " there, ".getBytes(UTF_8);
        byte[] c = "you ".getBytes(UTF_8);
        byte[] d = "crazy".getBytes(UTF_8);
        byte[] e = " concatenator".getBytes(UTF_8);
        byte[] f = " of ".getBytes(UTF_8);
        byte[] g = "arrays!".getBytes(UTF_8);
        byte[] all = ArrayUtils.concatenate(a, b, c, d, e, f, g);
        assertEquals("Hello there, you crazy concatenator of arrays!",
                new String(all, UTF_8));
    }

    @Test
    public void testSplitByteArray() {
        byte[] bytes = "Hello there, you crazy concatenator of arrays!".getBytes(UTF_8);
        byte[][] split = ArrayUtils.split(bytes, 6);
        String a = new String(split[0], UTF_8);
        String b = new String(split[1], UTF_8);
        assertEquals("Hello ", a);
        assertEquals("there, you crazy concatenator of arrays!", b);
    }

    @Test
    public void testMultiSplitByteArray() {
        byte[] bytes = "Hello there, you crazy concatenator of arrays!".getBytes(UTF_8);
        byte[][] split = ArrayUtils.split(bytes, 6, 12);
        String a = new String(split[0], UTF_8);
        String b = new String(split[1], UTF_8);
        String c = new String(split[2], UTF_8);
        assertEquals("Hello ", a);
        assertEquals("there,", b);
        assertEquals(" you crazy concatenator of arrays!", c);
    }

    @Test
    public void testMegaMultiSplitByteArray() {
        byte[] bytes = "Hello there, you crazy concatenator of arrays!".getBytes(UTF_8);
        byte[][] split = ArrayUtils.split(bytes, 6, 12, 16, 22, 32, 36);
        String a = new String(split[0], UTF_8);
        String b = new String(split[1], UTF_8);
        String c = new String(split[2], UTF_8);
        String d = new String(split[3], UTF_8);
        String e = new String(split[4], UTF_8);
        String f = new String(split[5], UTF_8);
        String g = new String(split[6], UTF_8);
        assertEquals("Hello ", a);
        assertEquals("there,", b);
        assertEquals(" you", c);
        assertEquals(" crazy", d);
        assertEquals(" concatena", e);
        assertEquals("tor ", f);
        assertEquals("of arrays!", g);
        assertEquals(new String(bytes, UTF_8), a + b + c + d + e + f + g);
    }

    @Test
    public void testConcatAndMore() {
        Object[] a = new Object[]{1, "hello"};
        Object[] b = new Object[]{1, "goodbye"};

        Object[] c = concatenate(a, b);

        assertArrayEquals(new Object[]{1, "hello", 1, "goodbye"}, c);

        Object[] d = dedup(c);
        assertArrayEquals(new Object[]{1, "hello", "goodbye"}, d);

        Object[] e = dedupByType(c);
        assertArrayEquals(new Object[]{1, "goodbye"}, e);

        Object[] a1 = concatenate(a, new Object[]{new Object[]{"b", "c", new Object[]{"d", "e"}}, "f", new Object[]{"g", "h"}});

        Object[] b1 = flatten(a1);
        assertArrayEquals(new Object[]{1, "hello", "b", "c", "d", "e", "f", "g", "h"}, b1);

        int[] n1 = ArrayUtils.<Integer>toIntArray(Arrays.<Integer>asList(1, 2, 3, 4, 5, 6));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, n1);

        long[] n2 = ArrayUtils.<Long>toLongArray(Arrays.<Long>asList(1L, 2L, 3L, 4L, 5L, 6L));
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L, 5L, 6L}, n2);
    }

    @Test
    public void testMultiConcatenate() {
        Object[] a = new Object[]{"a", "b", "c"};
        Object[] b = new Object[]{"d", "e", "f"};
        Object[] c = new Object[]{"g", "h", "i"};
        Object[] d = new Object[]{"j", "k", "l"};

        Object[] cc = concatenateAll(a, b, c, null, d);

        assertArrayEquals(new Object[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"}, cc);
    }

    @Test
    public void testEmptyForNull() {
        Object[] a = new Object[]{"a", "b", "c"};
        Object[] b = emptyForNull(a);
        assertSame(a, b);

        Object[] c = emptyForNull(null);
        assertArrayEquals(new Object[0], c);
    }

    @Test
    public void testCopy() {
        String[] s = {"a", "b", "c", "d"};
        String[] b = ArrayUtils.copyOf(s);
        assertNotSame(s, b);
        assertArrayEquals(s, b);
    }

    @Test
    public void testReverseInPlace() {
        String[] s = {"a", "b", "c", "d"};
        String[] old = s;
        s = ArrayUtils.reverseInPlace(s);
        assertSame(old, s);
        assertArrayEquals(new String[]{"d", "c", "b", "a"}, s);

        s = new String[]{"a", "b", "c", "d", "e"};
        old = s;
        s = ArrayUtils.reverseInPlace(s);
        assertSame(old, s);
        assertArrayEquals(new String[]{"e", "d", "c", "b", "a"}, s);
    }

    @Test
    public void testReversed() {
        String[] s = {"a", "b", "c", "d"};
        String[] old = s;
        s = ArrayUtils.reversed(s);
        assertNotSame(old, s);
        assertArrayEquals(new String[]{"d", "c", "b", "a"}, s);

        s = new String[]{"a", "b", "c", "d", "e"};
        old = s;
        s = ArrayUtils.reversed(s);
        assertNotSame(old, s);
        assertArrayEquals(new String[]{"e", "d", "c", "b", "a"}, s);
    }

    @Test
    public void testArrayOf() {
        String[] foos = arrayOf("foo", 6);
        assertEquals(6, foos.length);
        for (int i = 0; i < foos.length; i++) {
            assertEquals("foo", foos[i]);
        }
    }

    @Test
    public void testJDK8ArraysComplexEquals() {
        int[] a = new int[] {0, 1, 2, 3, 4, 5};
        int[] b = new int[] {3, 4, 5, 6, 7, 8};
        assertTrue(ArrayUtils.arraysEquals(a, 3, 6, b, 0, 3));
        assertFalse(ArrayUtils.arraysEquals(a, 0, 3, b, 0, 3));
        a[3] = 1;
        assertFalse(ArrayUtils.arraysEquals(a, 3, 6, b, 0, 3));
        b[0] = 1;
        assertTrue(ArrayUtils.arraysEquals(a, 3, 6, b, 0, 3));
    }
}
