/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.file;

import static com.mastfrog.util.file.FileUtilsTest.TEST_CONTENT;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CharBuffersCharSequenceTest {

    @Test
    public void testChars() throws IOException {
        String[] stuff = {"Hello", "world", "ьшокола", "this is some stuff"};
        StringBuilder sb = new StringBuilder();
        CharBuffer[] bufs = new CharBuffer[stuff.length];
        for (int i = 0; i < stuff.length; i++) {
            bufs[i] = CharBuffer.wrap(stuff[i]);
            sb.append(stuff[i]);
        }
        CharBuffersCharSequence seq = new CharBuffersCharSequence(bufs);
        assertEquals(sb.toString(), seq.toString());

        IntStream expChars = sb.chars();
        IntStream gotChars = seq.chars();

        assertEquals(expChars.count(), gotChars.count());
        expChars = sb.chars();
        gotChars = seq.chars();
        List<Integer> expected = collect(expChars);
        List<Integer> got = collect(gotChars);

        assertEquals(expected, got);
    }

    static List<Integer> collect(IntStream ints) {
        List<Integer> result = new ArrayList<>();
        ints.forEach(result::add);
        return result;
    }

    @Test
    public void testCbSeq() throws IOException {
        List<CharBuffer> l = new ArrayList<>();
        for (String s : TEST_CONTENT.split("\n")) {
            l.add(CharBuffer.wrap(s));
            l.add(CharBuffer.wrap("\n"));
        }
        CharSequence s = new CharBuffersCharSequence(l.toArray(new CharBuffer[0]));
        assertEquals(TEST_CONTENT.length(), s.length());
        assertEquals(TEST_CONTENT, s.toString());
        assertEquals("Hash codes do not match", TEST_CONTENT.hashCode(), s.hashCode());
        assertTrue("Equality test failed for '" + s + "'", s.equals(TEST_CONTENT));
        for (int i = 0; i < TEST_CONTENT.length(); i++) {
            char expect = TEST_CONTENT.charAt(i);
            char got = s.charAt(i);
            assertEquals("Wrong char at " + i, expect, got);
        }
        try {
            s.charAt(1000);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
        try {
            s.charAt(-1);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
        try {
            s.charAt(-2);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
    }

    @Test
    public void testEmpty() throws Exception {
        CharBuffersCharSequence b = new CharBuffersCharSequence(new CharBuffer[0]);
        assertEquals(0, b.length());
        assertEquals("", b.toString());
        for (int i = 0; i < 10; i++) {
            try {
                b.charAt(i);
            } catch (IndexOutOfBoundsException ex) {
                // ok
            }
        }
    }

    @Test
    public void testEmptyWithEmptyBuffers() throws Exception {
        CharBuffersCharSequence b = new CharBuffersCharSequence(new CharBuffer[]{
            (CharBuffer /* jdk 8 */) CharBuffer.allocate(10).position(10),
            (CharBuffer /* jdk 8 */) CharBuffer.allocate(10).position(10)
        });
        assertEquals(0, b.length());
        assertEquals("", b.toString());
        for (int i = 0; i < 10; i++) {
            try {
                b.charAt(i);
            } catch (IndexOutOfBoundsException ex) {
                // ok
            }
        }
    }

    @Test
    public void testSomeEmptyBuffers() throws Exception {
        CharBuffersCharSequence b = new CharBuffersCharSequence(new CharBuffer[]{
            (CharBuffer /* jdk 8 */) CharBuffer.allocate(10).position(10),
            (CharBuffer /* jdk 8 */) CharBuffer.wrap("Hello world"),
            (CharBuffer /* jdk 8 */) CharBuffer.allocate(10).position(10)
        });
        assertEquals("Hello world".length(), b.length());
        assertEquals("Hello world", b.toString());
        for (int i = "Hello world".length(); i < "Hello World".length() + 10; i++) {
            try {
                b.charAt(i);
            } catch (IndexOutOfBoundsException ex) {
                // ok
            }
        }
        assertEquals("Hash codes do not match", "Hello world".hashCode(), b.hashCode());
        assertTrue("Equality test failed for '" + b + "'", b.equals("Hello world"));
    }

}
