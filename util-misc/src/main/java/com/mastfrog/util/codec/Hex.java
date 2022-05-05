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
package com.mastfrog.util.codec;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.Arrays;

/**
 * Hexadecimal encoders and decoders.
 *
 * @author Tim Boudreau
 */
public final class Hex {

    private Hex() {
        throw new AssertionError();
    }

    /**
     * Creates a new hex encoder with the specified parameters.
     *
     * @param lowerCase Use a-f instead of A-F when encoding (the corresponding
     * decoder will accept either)
     * @param delegateClose If true, closing any stream wrapped by one produced
     * in this decoder will close the underlying stream; if false, the
     * underlying stream will be left open
     * @return An encoder
     */
    public static StringFormatEncoder newEncoder(boolean lowerCase, boolean delegateClose) {
        return new HexEncoder(lowerCase, delegateClose);
    }

    /**
     * Creates a new hex decoder.
     *
     * @param delegateClose If true, closing any stream wrapped by one produced
     * in this decoder will close the underlying stream; if false, the
     * underlying stream will be left open
     * @return A decoder
     */
    public static StringFormatDecoder newDecoder(boolean delegateClose) {
        return new HexDecoder(delegateClose);
    }

    private static final class HexEncoder implements StringFormatEncoder {

        private final boolean lowerCase;
        private final boolean delegateClose;

        HexEncoder(boolean lowerCase, boolean delegateClose) {
            this.lowerCase = lowerCase;
            this.delegateClose = delegateClose;
        }

        @Override
        public OutputStream wrap(OutputStream os) {
            return new OS(os, delegateClose);
        }

        @Override
        public ByteBuffer encode(ByteBuffer buffer) {
            ByteBuffer output = ByteBuffer.allocate(buffer.remaining() * 2);
            while (buffer.hasRemaining()) {
                int oneByte = 0xFF & buffer.get();
                String value = Integer.toHexString(oneByte);
                if (value.length() == 1) {
                    output.put((byte) '0');
                } else {
                    output.put(value.getBytes(US_ASCII));
                }
            }
            // Return it in the same state as ByteBuffer.wrap() does in the
            // JDK impl
            output.flip();
            return output;
        }

        @Override
        public String encodeToString(byte[] src) {
            StringBuilder sb = new StringBuilder(src.length * 2);
            for (int i = 0; i < src.length; i++) {
                int oneByte = 0xFF & src[i];
                if (oneByte <= 0x0A) {
                    sb.append('0');
                    sb.append(charForNybble(oneByte, lowerCase));
                } else {
                    sb.append(charForNybble(oneByte >> 4, lowerCase));
                    sb.append(charForNybble(oneByte & 0x0F, lowerCase));
                }
            }
            return sb.toString();
        }

        @Override
        public int encode(byte[] src, byte[] dst) {
            if (dst.length < src.length * 2) {
                throw new IllegalArgumentException("Need at least "
                        + (src.length * 2) + " bytes to encode an array of "
                        + src.length + " as hex, but was passed a destination "
                        + "array of only " + dst.length + " bytes.");
            }
            for (int srcCursor = 0, destCursor = 0; srcCursor < src.length; srcCursor++, destCursor += 2) {
                int oneByte = 0xFF & src[srcCursor];
                if (oneByte <= 0x0A) {
                    dst[destCursor] = '0';
                    dst[destCursor + 1] = (byte) charForNybble(oneByte, lowerCase);
                } else {
                    dst[destCursor] = (byte) charForNybble(oneByte >> 4, lowerCase);
                    dst[destCursor + 1] = (byte) charForNybble(oneByte & 0x0F, lowerCase);
                }
            }
            return src.length * 2;
        }

        @Override
        public byte[] encode(byte[] src) {
            byte[] dst = new byte[src.length * 2];
            encode(src, dst);
            return dst;
        }

        private final class OS extends FilterOutputStream {

            private final boolean delegateClose;

            OS(OutputStream out, boolean delegateClose) {
                super(out);
                this.delegateClose = delegateClose;
            }

            @Override
            public void close() throws IOException {
                if (delegateClose) {
                    super.close();
                }
            }

            @Override
            public void write(int oneByte) throws IOException {
                if (oneByte <= 0x0A) {
                    super.write('0');
                    super.write(charForNybble(oneByte, lowerCase));
                } else {
                    super.write(charForNybble(oneByte >> 4, lowerCase));
                    super.write(charForNybble(oneByte & 0x0F, lowerCase));
                }
            }

            @Override
            public void write(byte[] b) throws IOException {
                super.write(encode(b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                final int end = off + len;
                for (int i = off; i < end; i++) {
                    int oneByte = 0xFF & b[i];
                    if (oneByte <= 0x0A) {
                        super.write('0');
                        super.write(charForNybble(oneByte, lowerCase));
                    } else {
                        super.write(charForNybble(oneByte >> 4, lowerCase));
                        super.write(charForNybble(oneByte & 0x0F, lowerCase));
                    }
                }
            }
        }
    }

    private static final class HexDecoder implements StringFormatDecoder {

        private final boolean delegateClose;

        HexDecoder(boolean delegateClose) {
            this.delegateClose = delegateClose;
        }

        @Override
        public ByteBuffer decode(ByteBuffer buffer) {
            int rem = buffer.remaining();
            if (buffer.remaining() % 2 != 0) {
                throw new IllegalArgumentException(
                        "A hex buffer must contain an even number of "
                        + "character-bytes, but this one has "
                        + buffer.remaining());
            }
            ByteBuffer result = ByteBuffer.allocate(rem / 2);
            for (int i = 0; i < rem; i++) {
                char left = (char) (buffer.get() & 0xFF);
                char right = (char) (buffer.get() & 0xFF);
                byte val = (byte) decodeHex(left, right);
                result.put(val);
            }
            return result;
        }

        @Override
        public int decode(byte[] src, byte[] dst) {
            if (src.length == 0) {
                return 0;
            }
            if (dst.length < src.length / 2) {
                throw new IllegalArgumentException("Destination must be at least "
                        + (src.length / 2) + " bytes to accomodate "
                        + src.length + " hexadecimal characters worth of bytes, "
                        + "but got destination array of " + dst.length + ".");
            }
            for (int srcCursor = 0, destCursor = 0; srcCursor < src.length; srcCursor += 2, destCursor++) {
                dst[destCursor] = (byte) decodeHex(src[srcCursor], src[srcCursor + 1]);
            }
            return src.length / 2;
        }

        @Override
        public byte[] decode(String src) {
            if (src.isEmpty()) {
                return new byte[0];
            }
            int len = src.length();
            if (len % 2 != 0) {
                throw new IllegalArgumentException("Hex string length must be "
                        + "divisible by 2, but have " + src.length()
                        + " characters in '" + src + "'");
            }

            byte[] result = new byte[len / 2];
            for (int srcCursor = 0, destCursor = 0; srcCursor < len; srcCursor += 2, destCursor++) {
                char left = src.charAt(srcCursor);
                char right = src.charAt(srcCursor + 1);
                result[destCursor] = (byte) decodeHex(left, right);
            }
            return result;
        }

        @Override
        public byte[] decode(byte[] src) {
            if (src.length == 0) {
                return new byte[0];
            }
            if (src.length % 2 != 0) {
                throw new IllegalArgumentException("Hex string length must be "
                        + "divisible by 2, but have " + src.length
                        + " characters in '" + Arrays.toString(src) + "'");
            }
            byte[] result = new byte[src.length / 2];
            decode(src, result);
            return result;
        }

        @Override
        public InputStream wrap(InputStream is) {
            return new In(is, delegateClose);
        }

        private final class In extends InputStream {

            private final boolean delegateClose;
            private final InputStream delegate;

            public In(InputStream in, boolean delegateClose) {
                this.delegateClose = delegateClose;
                this.delegate = in;
            }

            @Override
            public void close() throws IOException {
                if (delegateClose) {
                    super.close();
                }
            }

            @Override
            public int available() throws IOException {
                return delegate.available() / 2;
            }

            @Override
            public long skip(long n) throws IOException {
                return delegate.skip(n * 2) / 2L;
            }

            @Override
            public boolean markSupported() {
                return delegate.markSupported();
            }

            @Override
            public synchronized void reset() throws IOException {
                delegate.reset();
            }

            @Override
            public synchronized void mark(int readlimit) {
                delegate.mark(readlimit * 2);
            }

            @Override
            public int read(byte[] b) throws IOException {
                byte[] b1 = new byte[b.length * 2];
                int count = delegate.read(b1);
                decode(b1, b);
                return count / 2;
            }

            @Override
            public int read() throws IOException {
                char left = (char) (delegate.read());
                char right = (char) (delegate.read());
                return decodeHex(left, right) & 0xFF;
            }
        }
    }

    private static int decodeHex(char left, char right) {
        return (nybbleForChar(left) << 4) | (nybbleForChar(right));
    }

    private static int decodeHex(byte left, byte right) {
        return (nybbleForChar((char) (left & 0xFF)) << 4)
                | (nybbleForChar((char) (right & 0xFF)));
    }

    private static int nybbleForChar(char ch) {
        switch (ch) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
            case 'a':
                return 10;
            case 'B':
            case 'b':
                return 11;
            case 'C':
            case 'c':
                return 12;
            case 'D':
            case 'd':
                return 13;
            case 'E':
            case 'e':
                return 14;
            case 'F':
            case 'f':
                return 15;
            default:
                throw new IllegalArgumentException("Not a hexadecimal character: '"
                        + ch + "'");
        }
    }

    private static char charForNybble(int oneNybbl, boolean lowerCase) {
        // More verbose, but faster
        switch (oneNybbl) {
            case 0:
                return '0';
            case 1:
                return '1';
            case 2:
                return '2';
            case 3:
                return '3';
            case 4:
                return '4';
            case 5:
                return '5';
            case 6:
                return '6';
            case 7:
                return '7';
            case 8:
                return '8';
            case 9:
                return '9';
            case 10:
                return lowerCase ? 'a' : 'A';
            case 11:
                return lowerCase ? 'b' : 'B';
            case 12:
                return lowerCase ? 'c' : 'C';
            case 13:
                return lowerCase ? 'd' : 'D';
            case 14:
                return lowerCase ? 'e' : 'E';
            case 15:
                return lowerCase ? 'f' : 'F';
            default:
                throw new IllegalArgumentException("Not a nybbl: "
                        + oneNybbl + " (0x" + Integer.toHexString(oneNybbl)
                        + ")");
        }
    }
}
