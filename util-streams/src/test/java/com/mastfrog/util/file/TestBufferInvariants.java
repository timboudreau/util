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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TestBufferInvariants {

    @Test
    public void testSubsequencesOfCharBuffersBehaviorOnBufferWrite() {
        ByteBuffer bb = ByteBuffer.allocateDirect(256);
        CharBuffer b = bb.asCharBuffer();
        b.put("Hello world".toCharArray());
        b.flip();
        CharSequence sub = b.subSequence(0, 5);
        assertEquals("Hello", sub.toString());
        b.rewind();
        assertEquals("Hello", sub.toString());
        b.put('x');
        b.put('y');
        b.put('z');
        assertEquals("xyzlo", sub.toString());
    }

    @Test
    public void testBufferPositionAfterPartialCharacterRead() {
        String s = "This is a thing.";
        byte[] bytes = s.getBytes(UTF_16);
        ByteBuffer b = ByteBuffer.allocate(5);
        CharsetDecoder dec = UTF_16.newDecoder();
        CharBuffer chars = CharBuffer.allocate(20);
        for (int pos = 0;;) {
            int remainingBytes = bytes.length - pos;
            if (remainingBytes <= 0) {
                break;
            }
            int countToPut = Math.min(b.remaining(), remainingBytes);
            b.put(bytes, pos, countToPut);
            b.flip();
            CoderResult res = dec.decode(b, chars, false);
            if (chars.position() == s.length()) {
                break;
            }
            if (b.position() < b.limit()) {
                ByteBuffer tail = b.slice();
                b.rewind();
                b.put(tail);
                pos += countToPut;
            } else {
                b.clear();
                pos += countToPut;
            }
        }
        chars.flip();
    }
}
