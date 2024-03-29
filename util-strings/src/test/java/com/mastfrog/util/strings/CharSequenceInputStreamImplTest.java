/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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

import com.mastfrog.util.strings.CharSequenceInputStream.EncodingErrorBehavior;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static java.lang.Integer.min;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.charset.UnmappableCharacterException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author timb
 */
public class CharSequenceInputStreamImplTest {

    private static final Charset[] CHARSETS = new Charset[]{
        UTF_8, UTF_16LE, UTF_16BE
    };

    @Test
    public void testAscii() throws IOException {
        assertUnaltered("This should work just fine");
    }

    @Test
    public void testRussian() throws IOException {
        assertUnaltered("Тогда же пресс-служба");
        assertUnaltered("Hello Тогда же пресс-служба Goodbye");
    }

    @Test
    public void testGreek() throws IOException {
        assertUnaltered("Με την άδειά σας");
        assertUnaltered("Whatever Με την άδειά σας Whatever");
    }

    @Test
    public void testCzech() throws IOException {
        assertUnaltered(
                "Vládní spolupráce s extrémisty totiž není nepřitažlivá jen pro celou zemi");
    }

    @Test
    public void testHebrew() throws IOException {
        assertUnaltered(
                "וְאָהַבְתָּ אֵת יְיָ | אֱלֹהֶיךָ, בְּכָל-לְבָֽבְךָ, וּבְכָל-נַפְשְׁךָ,"
                + " וּבְכָל-מְאֹדֶֽךָ. וְהָיוּ הַדְּבָרִים הָאֵלֶּה, אֲשֶׁר | אָֽנֹכִי מְצַוְּךָ");
    }

    @Test
    public void testUnencodableCharactersAreElided() throws IOException {
        String target = "Hello Тогда же пресс-служба Goodbye";
        CharSequenceInputStreamImpl sis = new CharSequenceInputStreamImpl(target, US_ASCII);
        byte[] bytes = sis.readAllBytes();
        String got = new String(bytes, US_ASCII);

        assertEquals(target.length(), got.length(),
                "Differing lengths:\n" + target + "\n" + got);
        assertTrue(got.startsWith("Hello "));
        assertTrue(got.endsWith(" Goodbye"));
        for (int i = 0; i < target.length(); i++) {
            if (' ' == target.charAt(i)) {
                assertEquals(' ', got.charAt(i), "Spaces should be encoded");
            }
        }
    }

    @Test
    public void testOmittingBehavior() throws IOException {
        String target = "Hello Тогда же пресс-служба Goodbye";
        CharSequenceInputStreamImpl sis = new CharSequenceInputStreamImpl(target, US_ASCII, 12,
                EncodingErrorBehavior.OMIT);
        byte[] bytes = sis.readAllBytes();
        String got = new String(bytes, US_ASCII);
        assertEquals("Hello   - Goodbye", got);
    }

    @Test
    public void testThrowingBehavior() throws IOException {
        String target = "Hello Тогда же пресс-служба Goodbye";
        CharSequenceInputStreamImpl sis = new CharSequenceInputStreamImpl(target, US_ASCII, 12,
                EncodingErrorBehavior.THROW);
        try {
            byte[] bytes = sis.readAllBytes();
            fail("Exception should have been thrown");
        } catch (UnmappableCharacterException ex) {
            // ok
        }
    }

    private void assertUnaltered(String what) throws IOException {
        for (Charset cs : CHARSETS) {
            for (int i = 0; i < 10; i++) {
                assertUnaltered(what, cs);
            }
            what = what + what;
        }
    }

    private void assertUnaltered(String what, Charset encoding) throws IOException {
        for (int bufferSize : new int[]{
            0, 2, what.length() / 4, what.length() / 2, what.length() * 2
        }) {
            assertUnaltered(what, encoding, bufferSize);
        }
    }

    private void assertUnaltered(String what, Charset encoding, int bufferSize)
            throws IOException {
        CharSequenceInputStreamImpl theStream;
        try ( CharSequenceInputStreamImpl in = new CharSequenceInputStreamImpl(what, encoding,
                bufferSize)) {
            theStream = in;
            // First, test simply reading all the bytes the simple way
            byte[] bytes = in.readAllBytes();
            assertArrayEquals(what.getBytes(encoding), bytes,
                    "Byte stream was altered.  Buffer size " + bufferSize
                    + " encoding " + encoding.displayName() + ". Expected:\n" + what
                    + "\nGot:\n" + new String(bytes, encoding));
            int expectedLength = bytes.length;
            // Now test read(byte[]) with a variety of array sizes
            for (int size : new int[]{
                2, expectedLength / 2, expectedLength - 1, expectedLength + 1
            }) {
                bytes = readBytesByBuffers(size, in);
                assertArrayEquals(what.getBytes(encoding), bytes,
                        "Got " + bytes.length + " for read buffer size "
                        + size + " and total size " + expectedLength
                        + ", byte stream was altered.  Buffer size " + bufferSize
                        + " encoding " + encoding.displayName() + ". Expected:\n" + what
                        + "\nGot:\n" + new String(bytes, encoding));
            }
            // Test read(byte[], int, int)
            bytes = readBytesByBuffersInPortions(expectedLength, in);
            assertArrayEquals(what.getBytes(encoding), bytes,
                    "Subdivided read was altered.  Buffer size " + bufferSize
                    + " encoding " + encoding.displayName() + ". Expected:\n" + what
                    + "\nGot:\n" + new String(bytes, encoding));
            // Test read()
            bytes = readBytesByteOverByte(in);
            assertArrayEquals(what.getBytes(encoding), bytes,
                    "Byte-over-byte read was altered.  Buffer size " + bufferSize
                    + " encoding " + encoding.displayName() + ". Expected:\n" + what
                    + "\nGot:\n" + new String(bytes, encoding));

            // Test transferTo()
            bytes = readBytesViaTransferTo(in, expectedLength);
            assertArrayEquals(what.getBytes(encoding), bytes,
                    "TransferTo read was altered.  Buffer size " + bufferSize
                    + " encoding " + encoding.displayName() + ". Expected:\n" + what
                    + "\nGot:\n" + new String(bytes, encoding));

        }
        assertEquals(0,
                theStream.available(),
                "Stream should show no available characters after it is closed");
        try {
            theStream.read();
            fail("Stream should throw on read() after close");
        } catch (IOException ex) {
            assertEquals("Stream is closed.", ex.getMessage());
        }
        try {
            theStream.read(new byte[10]);
            fail("Stream should throw on read(byte[]) after close");
        } catch (IOException ex) {
            assertEquals("Stream is closed.", ex.getMessage());
        }
        try {
            theStream.read(new byte[10], 1, 5);
            fail("Stream should throw on read(byte[], int, int) after close");
        } catch (IOException ex) {
            assertEquals("Stream is closed.", ex.getMessage());
        }
        try {
            theStream.transferTo(new ByteArrayOutputStream());
            fail("Stream should throw on transferTo() after close");
        } catch (IOException ex) {
            assertEquals("Stream is closed.", ex.getMessage());
        }
        // This should reopen the stream
        theStream.rewind();
        theStream.read();
    }

    private byte[] readBytesByBuffers(int bufferSize, CharSequenceInputStreamImpl in)
            throws IOException {
        in.rewind();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private byte[] readBytesByBuffersInPortions(int totalLength,
            CharSequenceInputStreamImpl in)
            throws IOException {
        in.rewind();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[totalLength];
        int portion = totalLength / 3;
        int cursor = 0;
        for (int read
                = in.read(buffer, cursor, min(portion, buffer.length - cursor));
                read > 0;
                read = in.read(buffer, cursor, min(portion,
                        buffer.length - cursor))) {
            out.write(buffer, cursor, read);
            cursor += read;
        }

        return out.toByteArray();
    }

    private byte[] readBytesByteOverByte(CharSequenceInputStreamImpl in) throws IOException {
        in.rewind();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ct = 0;
        for (int read = in.read(); read != -1; read = in.read()) {
            out.write((byte) read);
        }
        return out.toByteArray();
    }

    private byte[] readBytesViaTransferTo(CharSequenceInputStreamImpl in,
            long expectedLength) throws IOException {
        in.rewind();
        try ( ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            long read = in.transferTo(out);
            assertEquals(expectedLength, read,
                    "Different number of bytes read than expected from " + in);
            return out.toByteArray();
        }
    }

    // This code was originally written against junit 5
    private static void assertEquals(char a, char b, String message) {
        if (a != b) {
            Assert.fail("Expected '" + a + "' got '" + b + "': " + message);
        }
    }

    private static void assertEquals(int a, int b, String message) {
        if (a != b) {
            Assert.fail("Expected '" + a + "' got '" + b + "': " + message);
        }
    }

    private static void assertEquals(long a, long b, String message) {
        if (a != b) {
            Assert.fail("Expected '" + a + "' got '" + b + "': " + message);
        }
    }

    private static void assertEquals(String a, String b) {
        Assert.assertEquals(a, b);
    }

    private static void fail(String msg) {
        Assert.fail(msg);
    }

    private static void assertArrayEquals(byte[] a, byte[] b, String msg) {
        Assert.assertArrayEquals(msg, a, b);
    }

    private static void assertTrue(boolean val) {
        Assert.assertTrue(val);
    }

    private static void assertTrue(boolean val, String msg) {
        Assert.assertTrue(msg, val);
    }
}
