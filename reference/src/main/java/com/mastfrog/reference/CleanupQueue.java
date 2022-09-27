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
package com.mastfrog.reference;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ReferenceQueue which runs references added to it which implement Runnable,
 * when those references' referents have been garbage collected. Simply subclass
 * WeakReference (or whatever-reference) and implement runnable, and pass
 * CleanupQueue.queue() to its constructor.
 * <p>
 * This is a cleaned up port of NetBeans Utilities.activeReferenceQueue() to
 * modern Java, that does the same thing - implement Runnable on a to have the
 * run method invoked on garbage collection. Singleton, which has a single
 * minimum priority cleanup thread which pulls from the queue to do work.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class CleanupQueue {

    private static final Logger LOG = Logger.getLogger(CleanupQueue.class.getName());
    private static final boolean CLEANUP_QUEUE_DISABLED
            = Boolean.getBoolean(CleanupQueue.class.getName() + ".disabled"); // for tests
    private static final CleanupQueue INSTANCE = new CleanupQueue();
    private final AtomicBoolean started = new AtomicBoolean();
    private final QueueImpl q = new QueueImpl();
    private final Thread pollThread = new Thread(this::pollLoop);

    private CleanupQueue() {
        pollThread.setDaemon(true);
        pollThread.setPriority(Thread.MIN_PRIORITY);
    }

    public static ReferenceQueue<Object> queue() {
        return INSTANCE._q();
    }

    ReferenceQueue<Object> _q() {
        if (!CLEANUP_QUEUE_DISABLED && started.compareAndSet(false, true)) {
            pollThread.start();
        }
        return q;
    }

    private Reference<?> onePollLoop() throws InterruptedException {
        Reference<?> ref = q.remove();
        LOG.log(Level.FINE, "Got dequeued reference {0}", new Object[]{ref});
        if (!(ref instanceof Runnable)) {
            LOG.log(Level.WARNING, "A reference not implementing runnable has been added "
                    + "to the CleanupQueue.queue(): {0}", ref.getClass());
            return null;
        }
        Runnable run = (Runnable) ref;
        run.run();
        return ref;
    }

    private void pollLoop() {
        for (;;) {
            Reference<?> ref = null;
            try {
                onePollLoop();
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                // Should not happen.
                // If it happens, it is a bug in client code, notify!
                LOG.log(Level.WARNING, "Cannot process " + ref, t);
            } finally {
                // to allow GC
                ref = null;
            }
        }
    }

    private static final class QueueImpl extends ReferenceQueue<Object> {

        @Override
        public java.lang.ref.Reference<Object> poll() {
            throw new UnsupportedOperationException();
        }

    }
}
