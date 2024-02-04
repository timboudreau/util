/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.util.polled;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tim Boudreau
 */
public final class PollThread<T> {

    private static final ThreadLocal<boolean[]> BOOLS = ThreadLocal.withInitial(() -> new boolean[1]);
    private static PollState[] STATES = PollState.values();

    private final AtomicInteger state = new AtomicInteger();
    private final Thread thread = new Thread(this::loop);
    private final WorkProcessor<T> processor;
    private final BlockingQueue<T> work;
    private final ShutdownWorkPolicy policy;

    public PollThread(WorkProcessor<T> processor, int priority,
            ShutdownWorkPolicy policy) {
        this(processor, priority, policy, new LinkedBlockingQueue<>());
    }

    public PollThread(WorkProcessor<T> processor, int priority,
            ShutdownWorkPolicy policy, BlockingQueue<T> work) {
        this.processor = processor;
        this.policy = policy;
        this.work = work;
        this.thread.setDaemon(true);
        this.thread.setPriority(priority);
    }

    public void shutdown() {
        state.set(PollState.SHUTDOWN.ordinal());
        if (thread.isAlive()) {
            thread.interrupt();
        }
    }

    public void submit(T work) {
        boolean[] runningHolder = BOOLS.get();
        state.getAndUpdate(old -> {
            if (old == PollState.NOT_STARTED.ordinal()) {
                runningHolder[0] = true;
                return PollState.STARTED.ordinal();
            }
            runningHolder[0] = old == PollState.STARTED.ordinal();
            return old;
        });
        if (runningHolder[0]) {
            this.work.offer(work);
        } else {
            switch (policy) {
                case CALLER_RUNS:
                    runOne(work);
                    break;
                case DISCARD:
                    // do nothing
                    break;
                default:
                    throw new AssertionError(policy);
            }
        }
    }

    private void start() {
        thread.start();
    }

    private void runOne(T obj) {
        try {
            obj = work.take();
            processor.process(obj, state());
        } catch (Exception ex) {
            var st = state();
            if (processor.shouldAbort(obj, st, ex)) {
                abort();
            }
        }
    }

    private void abort() {
        state.compareAndSet(PollState.STARTED.ordinal(), PollState.ABORTED.ordinal());
        if (thread.isAlive() && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }

    private void loop() {

        T obj = null;
        int st;
        while ((st = state.get()) != PollState.SHUTDOWN.ordinal() && st != PollState.ABORTED.ordinal()) {
            try {
                obj = work.take();
                processor.process(obj, STATES[st]);
            } catch (Exception ex) {
                PollState os = STATES[st];
                if (processor.shouldAbort(obj, os, ex)) {
                    abort();
                    break;
                } else if (os == PollState.SHUTDOWN) {
                    break;
                }
            }
            obj = null;
        }
    }

    private PollState state() {
        return PollState.values()[state.get()];
    }

    public interface WorkProcessor<T> {

        void process(T work, PollState state) throws Exception;

        default boolean shouldAbort(T work, PollState state, Exception ex) {
            ex.printStackTrace();
            return state == PollState.SHUTDOWN;
        }
    }

    public enum ShutdownWorkPolicy {
        DISCARD,
        CALLER_RUNS,
    }

    public enum PollState {
        NOT_STARTED,
        STARTED,
        ABORTED,
        SHUTDOWN,
    }
}
