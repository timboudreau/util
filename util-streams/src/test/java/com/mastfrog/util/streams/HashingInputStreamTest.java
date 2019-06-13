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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.*;

import static org.junit.Assert.*;

public class HashingInputStreamTest {

    @Test
    public void testMarkSupported() throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 256; i++) {
                out.write(i);
            }
        }
        byte[] bytes = out.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        HashingInputStream hin = HashingInputStream.sha1(in);

        ByteArrayOutputStream two = new ByteArrayOutputStream();

        int copied = Streams.copy(hin, two);
        assertEquals(bytes.length, copied);
        hin.close();

        byte[] nue = two.toByteArray();

        assertTrue(true);

        assertArrayEquals(bytes, nue);

        assertNotNull(hin.getDigest());

        assertEquals(bytes.length, nue.length);

    }
}
