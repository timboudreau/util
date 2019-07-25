/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to useAsLong, copy, modify, merge, publish, distribute, sublicense, and/or sell
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
package com.mastfrog.logstructured;

import com.mastfrog.file.channels.FileChannelPool;
import com.mastfrog.file.channels.Lease;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
class LogStructuredReaderImpl<T> implements LogStructuredReader<T> {

    private ChannelAndCursor cac;
    private final Path srcFile;
    private final Path cursorFile;
    private final AtomicInteger readSequence = new AtomicInteger(Integer.MIN_VALUE);
    private final Serde<T> serde;
    private final FileChannelPool pool;

    LogStructuredReaderImpl(Path srcFile, Path cursorFile, Serde<T> serde, FileChannelPool pool) {
        this.srcFile = srcFile;
        this.cursorFile = cursorFile;
        this.serde = serde;
        this.pool = pool;
    }

    @Override
    public String toString() {
        return srcFile.getFileName().toString() + " with " + cac + " rs " + readSequence.get();
    }

    private ChannelAndCursor cac() throws IOException {
        if (cac == null) {
            CursorFile cf = new CursorFile(cursorFile, pool);
            cac = cf.updater(srcFile);
            if (cac != null) {
                cf.createIfNotPresent();
                long last = cf.read();
                cac.channel().position(Math.max(0, last));
            }
        }
        return cac;
    }

    @Override
    public void rewind() throws IOException {
        ChannelAndCursor cac = cac();
        if (cac != null && cac.cursor() != null) {
            cac.cursor().rewind();
        }
    }

    @Override
    public synchronized void deleteIfAllReadAndAdvanced() throws IOException {
        ChannelAndCursor cac = cac();
        if (cac != null) {
            discardCommitted();
            if (cac.deleteIfEmpty()) {
                cac.close();
                this.cac = null;
            }
        }
    }

    @Override
    public synchronized void discardCommitted() throws IOException {
        ChannelAndCursor cac = cac();
        if (cac != null) {
            Lease read = cac.channel();
            Lease write = read.forSameFile(StandardOpenOption.WRITE, StandardOpenOption.SYNC);
            assert write.isWrite() : "Not a write lease: " + write;

            assert read.isRead() : "Not a read lease: " + read;
            long currPos = read.position();
            if (currPos > 0) {
                long size = read.size();
                if (size > 0) {
                    read.use(readChannel -> {
                        write.use(writeChannel -> {
                            writeChannel.position(0);
                            assert writeChannel.position() == 0 : "Opened in append mode?";
                            long count = readChannel.transferTo(currPos, size - currPos, writeChannel);
                            assert count == size - currPos : "Only transferred " + count + " not " + (size - currPos);
                            writeChannel.truncate(size - currPos);
                            read.position(0);
                            cac.cursor().commit();
                        });
                        readChannel.position(0);
                    });
                    read.position(0);
                }
            }
        }
    }

    @Override
    public synchronized boolean hasUnread() throws IOException {
        ChannelAndCursor channelAndCursor = cac();
        Lease channel = channelAndCursor.channel();
        if (channel.size() == 0) {
            return false;
        }
        long channelPosition = channel.position();
        return channelPosition != channel.size();
    }

    @Override
    public synchronized UnadvancedRead<T> read() throws IOException {
        ChannelAndCursor channelAndCursor = cac();
        if (channelAndCursor == null) {
            // no file
            return null;
        }
        Lease lease = channelAndCursor.channel();
        long oldPosition = lease.position();
        if (oldPosition == lease.size()) {
            return null;
        }
        try {
            return lease.<CommittableItem<T>>use((FileChannel readChannel) -> {
                if (readChannel.position() == readChannel.size()) {
                    return null;
                }
                T obj = serde.deserialize(srcFile, readChannel);
                long newPos = readChannel.position();
                readChannel.position(oldPosition);
                int sq = readSequence.incrementAndGet();
                return new CommittableItem<>(obj, oldPosition, newPos, lease, sq, readSequence, channelAndCursor);
            });
        } catch (IOException ex) {
            lease.position(oldPosition);
            channelAndCursor.commit();
            throw ex;
        } finally {
            lease.position(oldPosition);
        }
    }

    @Override
    public synchronized T readAndAdvance() throws IOException {
        ChannelAndCursor channelAndCursor = cac();
        if (channelAndCursor == null) {
            // no file
            return null;
        }
        Lease channel = channelAndCursor.channel();
        long oldPosition = channel.position();
        if (oldPosition == channel.size()) {
            return null;
        }
        try {
            return channel.<T>use((FileChannel readChannel) -> {
                readSequence.incrementAndGet();
                return serde.deserialize(srcFile, readChannel);
            });
        } catch (IOException ex) {
            channel.position(oldPosition);
            throw ex;
        } finally {
            channelAndCursor.commit();
        }
    }

    @Override
    public synchronized boolean readAndAdvance(Consumer<T> consumer) throws IOException {
        ChannelAndCursor channelAndCursor = cac();
        if (channelAndCursor == null) {
            // no file
            return false;
        }
        Lease channel = channelAndCursor.channel();
        assert !channel.of().getFileName().toString().endsWith("cursor") : "Wrong file: " + channel.of();
        long oldPosition = channel.position();
        if (oldPosition == channel.size()) {
            return false;
        }
        try {
            channel.<T>use((FileChannel readChannel) -> {
                readSequence.incrementAndGet();
                T obj = serde.deserialize(srcFile, readChannel);
                consumer.accept(obj);
                channelAndCursor.commit();
            });
        } catch (IOException ex) {
            channel.position(oldPosition);
            channelAndCursor.commit();
            throw ex;
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        if (cac != null) {
            cac.close();
            cac = null;
        }
    }

}
