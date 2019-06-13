/*
 * The MIT License
 *
 * Copyright 2010-2015 Tim Boudreau.
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
package com.mastfrog.util.streams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class ByteBufferCollectionInputStreamTest {

    long orig = System.currentTimeMillis();
    long ix = orig + 1;

    private String randomString() {
        byte[] bytes = new byte[20];
        ThreadLocalRandom.current().nextBytes(bytes);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putLong(ix++);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private boolean recognizes(String s) {
        byte[] bytes = Base64.getDecoder().decode(s);
        long val = ByteBuffer.wrap(bytes).getLong();
        return val > orig && val <= ix;
    }

    @Test
    public void testUids() throws IOException {
        String last = "";
        for (int i = 0; i < 20; i++) {
            String curr = randomString();
            assertNotEquals(last, curr);
            assertTrue(recognizes(curr));
            last = curr;
        }
    }

    @Test
    public void testRead() throws Exception {
        assertTrue(true);
        StringBuilder sb = new StringBuilder();
        ByteBuffer a = buffer('a', 23, sb);
        ByteBuffer b = buffer('b', 52, sb);
        ByteBuffer c = buffer('c', 17, sb);
        InputStream in = new ByteBufferCollectionInputStream(a, b, c);
        String s = Streams.readString(in, 16);
        assertEquals(sb.toString(), s);

        // test various pathologies
        in = new ByteBufferCollectionInputStream();
        s = Streams.readString(in, 20);
        assertEquals("", s);

        in = new ByteBufferCollectionInputStream();
        s = Streams.readString(in, 40);
        assertEquals("", s);

        sb = new StringBuilder();
        in = new ByteBufferCollectionInputStream(ByteBuffer.allocate(0),
                ByteBuffer.allocate(0), buffer('a', 17, sb));
        s = Streams.readString(in, 15);
        assertEquals(sb.toString(), s);

        sb = new StringBuilder();
        in = new ByteBufferCollectionInputStream(ByteBuffer.allocate(0),
                ByteBuffer.allocate(0), buffer('d', 13, sb),
                ByteBuffer.allocate(0));
        s = Streams.readString(in, 10);
        assertEquals(sb.toString(), s);

        sb = new StringBuilder();
        List<ByteBuffer> l = new LinkedList<>();
        for (char cc : new char[]{'a', 'q', '3'}) {
            l.add(buffer(cc, 383, sb));
        }
        in = new ByteBufferCollectionInputStream(l);
        s = Streams.readString(in);
        assertEquals(sb.toString(), s);
    }

    private ByteBuffer buffer(char c, int count, StringBuilder sb) throws Exception {
        byte[] b = new byte[count];
        Arrays.fill(b, (byte) c);
        sb.append(new String(b, US_ASCII));
        return ByteBuffer.wrap(b);
    }
}
