/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Tim Boudreau
 */
final class ByteBufferCollectionInputStream extends InputStream {

    private final Iterator<ByteBuffer> iter;
    private int count;

    ByteBufferCollectionInputStream(ByteBuffer... buffers) {
        this(Arrays.asList(buffers));
    }

    ByteBufferCollectionInputStream(Iterable<ByteBuffer> ib) {
        iter = ib.iterator();
        if (ib instanceof Collection) {
            count = ((Collection)ib).size();
        }
    }
    private ByteBuffer buf;

    private ByteBuffer buf() {
        if (buf == null) {
            if (iter.hasNext()) {
                buf = iter.next();
                if (buf.position() != 0) {
                    buf.flip();
                }
                return buf;
            }
        }
        if (buf != null) {
            if (buf.position() == buf.limit()) {
                buf = null;
                return buf();
            }
        }
        return buf;
    }

    @Override
    public int read() throws IOException {
        ByteBuffer b = curr();
        if (b == null) {
            return -1;
        }
        return b.get();
    }

    ByteBuffer curr() {
        ByteBuffer b = buf();
        if (b == null) {
            return null;
        }
        while (b != null && b.position() == b.limit()) {
            b = buf();
        }
        return b;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        ByteBuffer b = curr();
        if (b == null) {
            return -1;
        }
        int oldPos = b.position();
        b.get(bytes);
        return b.position() - oldPos;
    }

    @Override
    public int read(byte[] bytes, int offset, int len) throws IOException {
        ByteBuffer b = curr();
        if (b == null) {
            return -1;
        }
        int oldPos = b.position();
        int remaining = b.limit() - oldPos;
        if (remaining > 0) {
            b.get(bytes, offset, Math.min(remaining, len));
        }
        return b.position() - oldPos;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " with " + count + " buffers finished?" + iter.hasNext();
    }
}
