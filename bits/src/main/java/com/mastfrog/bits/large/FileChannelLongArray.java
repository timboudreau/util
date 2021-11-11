/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.bits.large;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.function.LongConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of LongArray over a FileChannel. Is writable if the channel is
 * writable; no header, simply uses an 8-byte buffer to read and write on
 * demand. May be less performant than MappedFileLongArray, but does not come
 * with memory mapping size limits. Can contain more than Integer.MAX_VALUE
 * entries.
 *
 * @author Tim Boudreau
 */
public final class FileChannelLongArray implements CloseableLongArray {

    private final FileChannel channel;
    private static final ThreadLocal<ByteBuffer> BUF
            = ThreadLocal.withInitial(() -> ByteBuffer.allocate(Long.BYTES));

    // Pending: Efficiency-wise, it would be useful to use larger buffers,
    // and maintain a thread-local position+buffer object which could
    // read-ahead and return cached values.

    public FileChannelLongArray(FileChannel channel) {
        this.channel = channel;
    }

    private ByteBuffer buf() {
        ByteBuffer result = BUF.get();
        result.rewind();
        return result;
    }

    @Override
    public void close() {
        try {
            this.channel.close();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public long size() {
        try {
            return channel.size() / Long.BYTES;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public long get(long index) {
        try {
            ByteBuffer buf = buf();
            int readCount = channel.read(buf, index * Long.BYTES);
            if (readCount < 0) {
                throw new IllegalStateException("Could not read "
                        + Long.BYTES + " bytes at " + (index * Long.BYTES)
                        + " for offset " + index);
            }
            buf.flip();
            return buf.getLong();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void set(long index, long value) {
        ByteBuffer buf = buf();
        buf.putLong(value);
        buf.flip();
        try {
            channel.write(buf, index * Long.BYTES);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void resize(long size) {
        try {
            if (size * Long.BYTES < channel.size()) {
                channel.truncate(size * Long.BYTES);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new FileChannelLongArray(channel);
    }

    @Override
    public void clear() {
        try {
            channel.truncate(0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void addAll(long[] longs) {
        long sz = size();
        ByteBuffer buf = ByteBuffer.allocate(longs.length * Long.BYTES);
        buf.asLongBuffer().put(longs);
        buf.position(0);
        buf.limit(buf.capacity());
        try {
            channel.write(buf, sz);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean isZeroInitialized() {
        return true;
    }

    @Override
    public long maxSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public void forEach(LongConsumer lng) {
        ByteBuffer buf = ByteBuffer.allocate(64 * Long.BYTES);
        long pos = 0L;
        for (;;) {
            buf.rewind();
            buf.limit(buf.capacity());
            try {
                int readCount = channel.read(buf, pos);
                buf.flip();
                LongBuffer lb = buf.asLongBuffer();
                while (lb.remaining() > 0) {
                    lng.accept(lb.get());
                }
                if (readCount < buf.capacity()) {
                    break;
                }
                pos += buf.capacity();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Override
    public int getInto(long from, long[] into) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(into.length * Long.BYTES);
            int read = channel.read(buf, from * Long.BYTES);
            buf.flip();
            LongBuffer lb = buf.asLongBuffer();
            lb.get(into, 0, read / Long.BYTES);
            return read / Long.BYTES;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
