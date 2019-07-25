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
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tim Boudreau
 */
final class CommittableItem<T> implements UnadvancedRead<T> {

    private final T env;
    private final long oldPos;
    private final long newPos;
    private final Lease channel;
    private final int sequence;
    private final AtomicInteger sequenceSource;
    private final CursorFileUpdater cursor;

    CommittableItem(T env, long oldPos, long newPos, Lease channel, int sequence, AtomicInteger sequenceSource, ChannelAndCursor cur) {
        this.cursor = cur.cursor();
        this.env = env;
        this.oldPos = oldPos;
        this.newPos = newPos;
        this.channel = channel;
        this.sequence = sequence;
        this.sequenceSource = sequenceSource;
    }

    @Override
    public boolean isCurrent() {
        return sequence == sequenceSource.get();
    }

    @Override
    public String toString() {
        return "CI:" + env + "@" + oldPos;
    }

    @Override
    public T get() {
        return env;
    }

    @Override
    public void advance() throws IOException {
        if (sequenceSource.get() != sequence) {
            throw new IllegalStateException("Another read has been performed "
                    + "since this UncommittedRead was created");
        }
        channel.position(newPos);
        if (channel.syncPosition() != newPos) {
            throw new IllegalStateException(
                    "Could not reposition to " + newPos + " pos is " + channel.position());
        }
        cursor.commit();
    }

    @Override
    public void rollback() throws IOException {
        if (sequenceSource.get() != sequence) {
            throw new IllegalStateException("Another read has been performed " + "since this UncommittedRead was created");
        }
        if (channel.syncPosition() != oldPos) {
            channel.position(oldPos);
        }
        cursor.commit();
    }

}
