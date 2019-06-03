/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

package com.mastfrog.util.cache;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.Collection;
import java.util.concurrent.DelayQueue;

/**
 * Expires entries from all caches.
 *
 * @author Tim Boudreau
 */
class Expirer implements Runnable {

    private final DelayQueue<Expirable> queue = new DelayQueue<>();
    private volatile boolean started;
    private final int prio;

    Expirer(int prio) {
        this.prio = prio;
    }

    Expirer() {
        this(Thread.MIN_PRIORITY);
    }

    void removeAll(Collection<? extends Expirable> dead) {
        queue.removeAll(dead);
    }

    void offer(Expirable expirable) {
        queue.offer(expirable);
        checkStarted();
    }

    private void checkStarted() {
        if (!started) {
            started = true;
        }
        Thread expireThread = new Thread(this);
        expireThread.setName("antlr-cache-expire");
        expireThread.setPriority(prio);
        expireThread.setDaemon(true);
        expireThread.start();
    }

    void expireOne(Expirable toExpire) {
        toExpire.expire();
    }

    @Override
    public void run() {
        for (;;) {
            Thread.currentThread().setName("Global expirer for "
                    + TimedCache.class.getSimpleName() + " entries");
            try {
                Expirable toExpire = queue.take();
                expireOne(toExpire);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
                break;
            }
        }
    }
}
