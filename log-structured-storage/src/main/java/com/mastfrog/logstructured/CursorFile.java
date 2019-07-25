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
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Maintains an 8-byte cursor file which contains the current cursor position
 * when reading a log structured file storage.
 *
 * @author Tim Boudreau
 */
final class CursorFile {

    private final Path path;
    private final FileChannelPool pool;

    CursorFile(Path path, FileChannelPool pool) {
        this.path = notNull("path", path);
        this.pool = notNull("pool", pool);
    }

    FileChannelPool pool() {
        return pool;
    }

    public long read() throws IOException {
        if (!Files.exists(path)) {
            return -1;
        }
        Lease l = pool.lease(path, StandardOpenOption.READ);
        return l.position(0L).useAsLong((FileChannel ch) -> {
            ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
            ch.read(bb);
            bb.flip();
            return bb.getLong();
        });
    }

    public boolean createIfNotPresent() throws IOException {
        boolean created = !Files.exists(path);
        if (created) {
            CursorFileWriter writer = new CursorFileWriter(path, this);
            writer.accept(0);
        }
        return created;
    }

    public boolean isDone(Path with) throws IOException {
        if (!Files.exists(path)) {
            return true;
        }
        if (!Files.exists(this.path)) {
            return false;
        }
        return read() >= Files.size(with);
    }

    CursorFileWriter newWriter() {
        return new CursorFileWriter(path, this);
    }

    ChannelAndCursor updater(Path toTrack) throws IOException {
        long start = read();
        if (!Files.exists(toTrack) || Files.size(toTrack) <= start) {
            return null;
        }
        assert !toTrack.getFileName().toString().endsWith("cursor") : "Tracking wrong file";
        createIfNotPresent();
        ChannelAndCursor result = new ChannelAndCursor(toTrack);
        try {
            Lease channel = pool.lease(toTrack, StandardOpenOption.READ);
            result.setChannel(channel);
            channel.position(Math.max(0L, start));
            result.setCursorUpdater(new CursorFileUpdater(new CursorFileWriter(path, this), channel));
        } catch (IOException ex) {
            result.close();
            throw ex;
        }
        return result;
    }
}
