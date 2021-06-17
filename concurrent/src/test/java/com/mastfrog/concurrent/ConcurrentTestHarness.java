/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * General test harness for starting a bunch of threads, doing something
 * concurrently with some guarantees that they actually run concurrently,
 * capturing any thrown exceptions and awaiting exit for a timeout, and failing
 * of the threads have not exited - implements a few helpful patterns to make
 * such tests reliable and robust.
 *
 * Solves a number of problems common to concurrent tests:
 * <ul>
 * <li>Threads don't actually run concurrently - they are started and run so
 * fast one exits before the next starts. We use a CountDownLatch so the main
 * thread waits for all threads to have been started by the OS and have entered
 * their runnable, and a Phaser to release threads to do work only after we know
 * that they ALL are ready</li>
 * <li>Test code doesn't know, or worse, uses guesses and sleeps, to decide when
 * to test what the threads have done: We use a CountDownLatch to block the main
 * thread until all threads have exited.</li>
 * <li>Test times out or continues indefinitely if one thread does not exit,
 * without giving the main thread an opprotunity to respond to the timeout - we
 * detect timeout and throw an exception, regardless of wheather the test thread
 * has exited.</li>
 * <li>Exceptions thrown on background threads do not propagate to the main
 * thread, so tests can fail miserably and still appear to pass. We capture
 * anything thrown and rethrow it in the main thread.</li>
 * </ul>
 *
 * @author Tim Boudreau
 */
public class ConcurrentTestHarness<R extends Runnable> {

    // Pending:  Move this to its own library
    private final int threadCount;
    private final IntFunction<R> runnables;

    public ConcurrentTestHarness(int threadCount, R run) {
        this(threadCount, _ignored -> run);
    }

    public ConcurrentTestHarness(int threadCount, Supplier<R> runnables) {
        this(threadCount, ignored -> runnables.get());
    }

    public ConcurrentTestHarness(int threadCount, IntFunction<R> runnables) {
        this.threadCount = threadCount;
        this.runnables = runnables;
    }

    /**
     * Run background threads using the default 1-minute timeout for them to
     * exit.
     *
     * @return The list of runnables used
     * @throws Throwable if a background thread throws an exception
     */
    public List<R> run() throws Throwable {
        return run(Duration.ofMinutes(1));
    }

    /**
     * Run background threads using the passed timeout for them to exit.
     *
     * @return The list of runnables used
     * @throws Throwable if a background thread throws an exception
     */
    public List<R> run(Duration timeout) throws Throwable {
        Phaser phaser = new Phaser(threadCount + 1);
        CountDownLatch entryLatch = new CountDownLatch(1);
        CountDownLatch exitLatch = new CountDownLatch(threadCount);
        List<R> result = new ArrayList<>(threadCount);
        List<WrapRun<R>> wrs = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            R r = runnables.apply(i);
            result.add(r);
            WrapRun<R> wr = new WrapRun(exitLatch, entryLatch, phaser, i, r);
            wrs.add(wr);
            Thread t = new Thread(wr, "test-" + i);
            t.setDaemon(true);
            t.start();
        }
        entryLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        phaser.arriveAndDeregister();
        exitLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (exitLatch.getCount() > 0) {
            throw new AssertionError("Threads are still running after "
                    + "timeout of " + timeout + " x 2");
        }
        Throwable thrown = null;
        for (WrapRun<R> wr : wrs) {
            Throwable t = wr.thrown();
            if (thrown != null) {
                try {
                    thrown.addSuppressed(t);
                } catch (Exception e) { // A few things like NPE fail here
                    if (thrown.getCause() == null) {
                        thrown.initCause(t);
                    } else {
                        // ignore
                        t.printStackTrace();
                    }
                }
            } else {
                thrown = t;
            }
        }
        if (thrown != null) {
            throw thrown;
        }
        return result;
    }

    private static class WrapRun<R extends Runnable> implements Runnable {

        private final CountDownLatch exitLatch;
        private final CountDownLatch entryLatch;
        private final Phaser phaser;
        private final int index;
        private final Runnable logic;
        private Throwable thrown;

        public WrapRun(CountDownLatch exitLatch, CountDownLatch entryLatch, Phaser phaser, int index, Runnable logic) {
            this.exitLatch = exitLatch;
            this.entryLatch = entryLatch;
            this.phaser = phaser;
            this.index = index;
            this.logic = logic;
        }

        synchronized Throwable thrown() {
            return thrown;
        }

        @Override
        public void run() {
            try {
                entryLatch.countDown();
                phaser.arriveAndAwaitAdvance();
                logic.run();
            } catch (Throwable th) {
                // We are accessing this from another thread, so the
                // memory barrier is a good idea
                synchronized (this) {
                    thrown = th;
                }
            } finally {
                exitLatch.countDown();
            }
        }
    }
}
