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
package com.mastfrog.concurrent.coalesce;

import com.mastfrog.concurrent.ConcurrentLinkedList;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.BooleanFunction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Ensures that when multiple threads are enqueued to do the same work, only one
 * thread actually does it and the rest receive the result. Basically the
 * opposite of a mutex - a mutex sequences work so each thread does it
 * exclusively; this ensures that while only one thread does the work, the
 * computation is done once and all threads trying to do the same work
 * concurrently receive the same result.
 * <p>
 * This is a somewhat unusual pattern - example: You have a very large,
 * expensive-to-parse file on disk, which multiple threads will want access to
 * parse tree of, and the file can change unexpectedly. So you use a
 * WorkCoalescer which checks if the file has changed, reparsing it only if
 * needed. If two threads concurrently enter it, one of them will parse the
 * file, the other will be blocked until the parse completes, and both will
 * received the result of parsing.
 * </p>
 * <p>
 * Another use case is similar: You have timed weak references to some
 * expensive-to-create object. If something really needs the object and it is
 * gone, you need to recreate it; and multiple threads may want it concurrently
 * when it has expired. So you use a WorkCoalescer to test the existing weak
 * reference, and if it is null, re-create the object.
 * </p>
 * <p>
 * This class does <i>not</i> encapsulate the reference to the data or the means
 * of obtaining it, purposely because some of the uses of it will do a different
 * computation depending on their state, and encapsulating that in a supplier
 * that could be passed in can add up to a memory leak of local state.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class WorkCoalescer<T> {

    private final ConcurrentLinkedList<AtomicReference<T>> stack = ConcurrentLinkedList.lifo();
    private final Sync sync = new Sync();

    private final String name;

    /**
     * Create with a name to use in toString() for logging purposes.
     *
     * @param name A name
     */
    public WorkCoalescer(String name) {
        this.name = name;
    }

    /**
     * Create an unnamed instance.
     */
    public WorkCoalescer() {
        this("unnamed");
    }

    private void drainAndApply(T val) {
        AtomicReference<T> last = null;
        while (!stack.isEmpty()) {
            AtomicReference<T> ref = stack.pop();
            if (ref != null && ref != last) {
                ref.set(val);
            }
            last = ref;
        }
    }

    /**
     * Obtain a value of T using the passed supplier; if another thread is
     * already computing it, the calling thread will block until that
     * computation is complete. It is assumed that the value produced by any
     * supplier passed here is appropriate to return to any callers which enter
     * this method concurrently.
     *
     * @param resultComputation A supplier
     * @param ref A reference to put the value in
     * @return An instance of T
     * @throws InterruptedException if the current thread is interrupted while
     * blocked
     */
    public T coalesceComputation(Supplier<T> resultComputation,
            AtomicReference<T> ref) throws InterruptedException {
        stack.push(ref);
        return sync.hold(first -> {
            T result;
            if (first) {
                result = resultComputation.get();
                drainAndApply(result);
            } else {
                result = ref.get();
            }
            return result;
        });
    }

    @Override
    public String toString() {
        return name + "(" + sync + ")";
    }

    /**
     * Dirt simple synchronizer, since our only state is that something is or is
     * not already performing the computation.
     */
    private static final class Sync {

        private final AtomicBoolean state = new AtomicBoolean();
        private final ConcurrentLinkedList<Thread> threads
                = ConcurrentLinkedList.fifo();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Int count = Int.create();
            threads.forEach(thread -> {
                if (count.increment() > 0) {
                    sb.append(",");
                }
                sb.append(thread.getName());
            });
            String msg = state.get()
                    ? "locked"
                    : count.getAsInt() > 0
                    ? "draining"
                    : "unlocked";
            return msg + " with " + count + " threads: " + sb;
        }

        public <T> T hold(BooleanFunction<T> c) {
            boolean first = state.compareAndSet(false, true);
            if (!first) {
                threads.push(Thread.currentThread());
                LockSupport.park(this);
            }
            T result = c.applyAsBoolean(first);
            if (first) {
                try {
                    while (!threads.isEmpty()) {
                        Thread t = threads.pop();
                        if (t != null) {
                            LockSupport.unpark(t);
                        }
                    }
                } finally {
                    state.set(false);
                }
            }
            return result;
        }
    }
}
