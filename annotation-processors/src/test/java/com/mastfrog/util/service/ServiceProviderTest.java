/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ServiceProviderTest {

    @Test
    public void testServicesAreRegistered() {
        Iterator<AbstractWoogle> woogs = ServiceLoader.load(AbstractWoogle.class).iterator();
        assertTrue(woogs.hasNext());
        AbstractWoogle first = woogs.next();
        assertNotNull(first);
        assertTrue(woogs.hasNext());
        AbstractWoogle second = woogs.next();
        assertNotNull(second);
        assertTrue(woogs.hasNext());
        AbstractWoogle third = woogs.next();
        assertNotNull(third);
        assertFalse(woogs.hasNext());
        assertEquals(setOf(ConcreteWoogle.class, AnotherWoogle.class, YetAnotherWoogle.class), setOf(first.getClass(), second.getClass(), third.getClass()));
    }

    private <T> Set<T> setOf(T a, T b, T c) {
        return new HashSet<>(Arrays.asList(a, b, c));
    }

    String[] expected = ("com.mastfrog.util.service.ConcreteWoogle\n"
            + "#position=10\n"
            + "com.mastfrog.util.service.AnotherWoogle\n"
            + "#position=23\n"
            + "com.mastfrog.util.service.YetAnotherWoogle\n").split("\n");

    @Test
    public void testMetaInfoFile() throws Exception {
        InputStream in = ServiceProviderTest.class.getResourceAsStream("/META-INF/services/" + AbstractWoogle.class.getName());
        assertNotNull(in);
        List<String> lines = readLines(in);
        assertEquals(lines.toString(), 5, lines.size());
        assertEquals(Arrays.asList(expected), lines);
    }

    private List<String> readLines(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(in, baos, 512);
        return Arrays.asList(new String(baos.toByteArray(), UTF_8).split("\n"));
    }

    public static int copy(final InputStream in, final OutputStream out, int bufferSize)
            throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int bytesCopied = 0;
        for (;;) {
            int byteCount = in.read(buffer, 0, buffer.length);
            if (byteCount <= 0) {
                break;
            } else {
                out.write(buffer, 0, byteCount);
                bytesCopied += byteCount;
            }
        }
        return bytesCopied;
    }
}
