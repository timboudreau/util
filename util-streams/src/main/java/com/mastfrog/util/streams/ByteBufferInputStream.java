/*
 * The MIT License
 *
 * Copyright 2011 Mastfrog Technologies.
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

/**
 *
 * @author Tim Boudreau
 */
final class ByteBufferInputStream extends InputStream {

    final ByteBuffer buf;

    ByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        return buf.remaining() == 0 ? -1 : buf.get();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void reset() throws IOException {
        buf.reset();
    }

    @Override
    public synchronized void mark(int readlimit) {
        buf.mark();
    }

    @Override
    public int available() throws IOException {
        return buf.limit() - buf.position();
    }

    @Override
    public long skip(long n) throws IOException {
        int pos = buf.position();
        int rem = available();
        if (n > buf.remaining()) {
            buf.position(buf.limit());
            return rem;
        } else {
            int target = pos + Math.max(0, Math.min(Integer.MAX_VALUE, (int) n));
            buf.position(target);
            return target - pos;
        }
    }

    @Override
    public void close() throws IOException {
        buf.position(buf.limit());
        super.close();
    }

    //        @Override /* JDK9 */
    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int rem = available();
        int remainingInArray = b.length - off;
        int lengthToRead = Math.min(remainingInArray, Math.min(rem, len));
        buf.get(b, off, lengthToRead);
        return lengthToRead;
    }

    //        @Override /* JDK9 */
    @Override
    public byte[] readNBytes(int len) throws IOException {
        int lengthToRead = Math.min(len, available());
        byte[] bytes = new byte[lengthToRead];
        buf.get(bytes);
        return bytes;
    }

    //        @Override /* JDK9 */
    @Override
    public byte[] readAllBytes() throws IOException {
        byte[] result = new byte[available()];
        buf.get(result);
        return result;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int result = Math.min(available(), b.length);
        buf.get(b, 0, result);
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (off + len > b.length) {
            throw new IOException("offset + length=" + (off + len) + " but array length is " + b.length);
        }
        int rem = available();
        if (rem == 0) {
            return -1;
        }
        int result = Math.min(len, rem);
        buf.get(b, off, result);
        return result;
    }

}
