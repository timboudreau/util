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
package com.mastfrog.file.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A wrapper for FileChannels which prevents clients from closing them, which
 * they should not do with pooled channels.
 *
 * @author Tim Boudreau
 */
final class FileChannelWrapper extends FileChannel {

    private final FileChannel channel;

    FileChannelWrapper(FileChannel channel) {
        this.channel = channel;
    }

    static FileChannel unwrap(FileChannel ch) {
        if (ch instanceof FileChannelWrapper) {
            return ((FileChannelWrapper) ch).channel;
        }
        return ch;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return channel.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return channel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return channel.write(srcs, offset, length);
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        channel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return channel.size();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        channel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metadata) throws IOException {
        channel.force(metadata);
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return channel.transferTo(position, count, target);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return channel.transferFrom(src, position, count);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return channel.read(dst, position);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        return channel.write(src, position);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return channel.map(mode, position, size);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return channel.lock(position, size, shared);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return channel.tryLock(position, size, shared);
    }

    @Override
    protected void implCloseChannel() throws IOException {
        throw new IOException("This channel is managed by a FileChannelPool - do "
                + "not close it directly");
    }

    void closeUnderlying() throws IOException {
        channel.close();
    }
}
