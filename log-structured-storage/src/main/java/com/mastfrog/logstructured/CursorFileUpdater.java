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
import java.io.IOException;

/**
 * Takes a cursor writer and a file channel, and can be told to store the
 * current position of the channel.
 *
 * @author Tim Boudreau
 */
final class CursorFileUpdater {

    private final CursorFileWriter writer;
    private final Lease track;

    CursorFileUpdater(CursorFileWriter writer, Lease track) {
        this.writer = writer;
        this.track = track;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(writer=" + writer + " track=" + track + ")";
    }

    public long lastValue() throws IOException {
        return writer.lastValue();
    }

    public void delete() throws IOException {
        writer.delete();
    }

    public void rewind() throws IOException {
        track.position(0);
        commit();
    }

    public long commit() throws IOException {
        long pos = track.position();
        writer.accept(pos);
        return pos;
    }
}
