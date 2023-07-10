/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.thread;

import com.mastfrog.function.misc.QuietAutoClosable;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages a pool of ByteBuffers for concurrent writing without allocating a new
 * one every time.
 *
 * @author Tim Boudreau
 */
public final class BufferPool {

    private final List<BufferHolder> buffers;
    private final AtomicInteger inUseCount = new AtomicInteger();
    private volatile boolean hasWaiter;
    private final int bufferSize;
    private final boolean direct;

    /**
     * Create a pool which will allocate buffers of the given size.
     *
     * @param bufferSize The buffer size
     */
    public BufferPool(int bufferSize) {
        this(bufferSize, true);
    }

    public BufferPool(int bufferSize, boolean direct) {
        buffers = new CopyOnWriteArrayList<>();
        this.bufferSize = bufferSize;
        this.direct = direct;
    }

    /**
     * Returns or creates a new buffer holder.
     *
     * @return A buffer holder
     */
    public BufferHolder buffer() {
        for (BufferHolder bh : buffers) {
            if (bh.open()) {
                return bh;
            }
        }
        BufferHolder nue = new BufferHolder();
        nue.open();
        buffers.add(nue);
        return nue;
    }

    /**
     * Clear the state of this object (any buffers in use will continue to be
     * usable.
     */
    public void close() {
        buffers.clear();
        inUseCount.set(0);
        hasWaiter = false;
    }

    /**
     * Block until no buffers are in use, and then return any buffers whose
     * position is > 0.
     *
     * @return A list of buffers.
     * @throws InterruptedException
     */
    public List<ByteBuffer> awaitQuiet() throws InterruptedException {
        hasWaiter = true;
        int count = 0;
        while (inUseCount.get() > 0 && count++ < 5000) {
            synchronized (this) {
                wait(5);
            }
        }
        List<ByteBuffer> all = new LinkedList<>();
        for (BufferHolder h : buffers) {
            if (h.buf.position() > 0) {
                all.add(h.buf);
            }
        }
        Collections.sort(all);
        return all;
    }

    /**
     * Holds one buffer - obtain it from buffer(), and call close() to return it
     * to the pool.
     */
    public final class BufferHolder implements QuietAutoClosable, Comparable<BufferHolder> {

        final ByteBuffer buf = direct ? ByteBuffer.allocateDirect(bufferSize)
                : ByteBuffer.allocate(bufferSize);
        private final AtomicBoolean inUse = new AtomicBoolean(false);
        private final AtomicLong lastObtained = new AtomicLong();

        boolean open() {
            if (inUse.compareAndSet(false, true)) {
                inUseCount.incrementAndGet();
                lastObtained.set(System.nanoTime());
                return true;
            }
            return false;
        }

        public boolean inUse() {
            return inUse.get();
        }

        public ByteBuffer buffer() {
            return buf;
        }

        @Override
        public void close() {
            inUse.set(false);
            int numInUse = inUseCount.decrementAndGet();
            if (numInUse == 0 && hasWaiter) {
                synchronized (this) {
                    notifyAll();
                }
            } else if (numInUse < 0) {
                inUseCount.set(0);
            }
        }

        @Override
        public int compareTo(BufferHolder o) {
            long mine = lastObtained.get();
            long theirs = o.lastObtained.get();
            return mine == theirs ? 0 : mine > theirs ? 1 : -1;
        }
    }
}
