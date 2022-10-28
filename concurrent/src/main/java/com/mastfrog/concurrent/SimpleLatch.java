/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import java.util.concurrent.locks.LockSupport;

/**
 * A dirt simple thread-parking mechanism that threads can enter to be parked,
 * and which can be released in fifo order.
 * <p>
 * Note that this class offers no protection against <i>other code</i> unparking
 * a thread by using <code>LockSupport.unpark()</code> directly.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class SimpleLatch {

    private final ConcurrentLinkedList<Thread> parked = ConcurrentLinkedList.fifo();
    private final String name;

    /**
     * Create a new latch with a name (which will show up as what the thread is
     * parked on in thread dumps).
     *
     * @param name A name or null
     */
    public SimpleLatch(String name) {
        this.name = name;
    }

    /**
     * Create a new unnamed SimpleLatch.
     */
    public SimpleLatch() {
        this(null);
    }

    /**
     * Enter the latch, blocking the current thread.
     */
    public void enter() {
        parked.push(Thread.currentThread());
        LockSupport.park(this);
    }

    /**
     * Get the number of threads currently parked by this latch (not necessarily
     * true at any time other than the instant of the call to this method).
     *
     * @return A count of parked threads
     */
    public int count() {
        return parked.size();
    }

    /**
     * Release a single thread if one is present.
     *
     * @return true if a thread was released
     */
    public boolean releaseOne() {
        Thread t = parked.pop();
        if (t == null) {
            return false;
        }
        LockSupport.unpark(t);
        return true;
    }

    /**
     * Release all threads blocked by this latch.
     *
     * @return The number of threads released
     */
    public int releaseAll() {
        int released = 0;
        while (!parked.isEmpty()) {
            releaseOne();
            released++;
        }
        return released;
    }

    /**
     * Returns the name if one was set in the constructor.
     *
     * @return A name or the result of <code>super.toString()</code>
     */
    @Override
    public String toString() {
        return name == null ? super.toString() : name;
    }

}
