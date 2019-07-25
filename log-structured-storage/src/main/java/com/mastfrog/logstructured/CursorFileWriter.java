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

import com.mastfrog.file.channels.Lease;
import com.mastfrog.function.throwing.io.IOLongConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;

/**
 * Writes a single 64 bit value to a cursor file.
 *
 * @author Tim Boudreau
 */
final class CursorFileWriter implements IOLongConsumer {

    private final Path path;
    private Lease lease;
    private final ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
    private long lastValue = -1;
    private static final Set<StandardOpenOption> CURSOR_WRITE_OPTS
            = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    private final CursorFile cursorFile;

    CursorFileWriter(Path path, CursorFile cursorFile) {
        this.path = path;
        this.cursorFile = cursorFile;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + path.getFileName() + ')';
    }

    public synchronized void delete() throws IOException {
        lease.deleteFile();
        lease = null;
        lastValue = -1;
    }

    private synchronized Lease lease() throws IOException {
        if (lease == null) {
            lease = cursorFile.pool().lease(path, CURSOR_WRITE_OPTS, LogStructuredAppenderImpl.ATTRS);
        }
        return lease;
    }

    public synchronized long lastValue() throws IOException {
        if (lastValue == -1) {
            if (Files.exists(path)) {
                return cursorFile.read();
            }
        }
        return lastValue;
    }

    @Override
    public synchronized void accept(long val) throws IOException {
        if (val < 0) {
            throw new IllegalArgumentException("Negative cursor position");
        }
        bb.putLong(lastValue = val);
        bb.flip();
        lease().use(ch -> {
            ch.position(0);
            ch.write(bb);
            bb.rewind();
        });
    }
}
