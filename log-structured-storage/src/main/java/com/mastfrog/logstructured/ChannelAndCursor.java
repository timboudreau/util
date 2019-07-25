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
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a single instance of a FileChannel and an associated cursor writer
 * such that the cursor updater is always associated with the channel which is
 * actually in useAsLong by any reader over that file.
 *
 * @author Tim Boudreau
 */
final class ChannelAndCursor implements AutoCloseable {

    private Lease lease;
    private CursorFileUpdater cursorUpdater;
    private final Path dataFile;

    ChannelAndCursor(Path name) {
        this.dataFile = name;
    }

    @Override
    public String toString() {
        try {
            return dataFile + " with lease " + lease + " and cursor " + cursorUpdater.lastValue();
        } catch (IOException ex) {
            Logger.getLogger(ChannelAndCursor.class.getName()).log(Level.SEVERE, null, ex);
            return dataFile + " with lease " + lease + " and cursor X";
        }
    }

    Lease setChannel(Lease lease) {
        this.lease = lease;
        return lease;
    }

    boolean deleteIfEmpty() throws IOException {
        if (cursorUpdater == null || lease == null) {
            throw new ClosedChannelException();
        }
        if (cursorUpdater.lastValue() >= lease.size()) {
            lease.deleteFile();
            cursorUpdater.delete();
            close();
            return true;
        }
        return false;
    }

    CursorFileUpdater setCursorUpdater(CursorFileUpdater up) {
        this.cursorUpdater = up;
        return up;
    }

    public Lease channel() {
        return lease;
    }

    public CursorFileUpdater cursor() {
        return cursorUpdater;
    }

    public synchronized void commit() throws IOException {
        if (cursorUpdater != null) {
            cursorUpdater.commit();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        cursorUpdater = null;
        lease = null;
    }
}
