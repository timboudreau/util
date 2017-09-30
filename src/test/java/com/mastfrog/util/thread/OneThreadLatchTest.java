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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class OneThreadLatchTest {

    private static final int THREADS = 8;
    private ExecutorService pool;

    private final AtomicInteger executions = new AtomicInteger();

    @Before
    public void startup() {
        pool = Executors.newFixedThreadPool(THREADS);
    }

    @After
    public void shutdown() {
        pool.shutdownNow();
    }

    @Test(timeout = 40000)
    public void testExactlyOneThreadRuns() throws Throwable {
        Phaser launch = new Phaser(1);
        AtomicInteger exited = new AtomicInteger();
        AtomicMaximum max = new AtomicMaximum();
        OneThreadLatch latch = new OneThreadLatch();
        Thread.currentThread().setName("OneThreadLatch");
        AtomicBoolean exit = new AtomicBoolean();
        CountDownLatch onExit = new CountDownLatch(THREADS);
        AtomicBoolean active = new AtomicBoolean();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(new Acquirer(exit, onExit, latch, max, launch, exited, active));
        }
        Thread.sleep(100);
        launch.arriveAndDeregister();
        latch.releaseOne();
        Thread.sleep(50);
        exit.set(true);
        assertEquals(1, max.getMaximum());
        latch.disable();
        Thread.sleep(300);
        onExit.await(500, TimeUnit.MILLISECONDS);
        assertEquals("All threads have not exited " + System.currentTimeMillis(), THREADS, exited.get());
        assertEquals("All threads have not exited " + System.currentTimeMillis(), 0L, onExit.getCount());
    }

    @Test(timeout=40000)
    public void testWhenDisabledThreadsAreNotBlocked() throws Throwable {
        Phaser launch = new Phaser(1);
        AtomicBoolean exit = new AtomicBoolean();
        AtomicMaximum max = new AtomicMaximum();
        AtomicInteger exited = new AtomicInteger();
        OneThreadLatch latch = new OneThreadLatch();
        CountDownLatch onExit = new CountDownLatch(THREADS);
        AtomicBoolean active = new AtomicBoolean();
        assertTrue("Disable failed", latch.disable());
        for (int i = 0; i < THREADS; i++) {
            pool.submit(new Acquirer(exit, onExit, latch, max, launch, exited, active));
        }
        int arrivalPhase = launch.arriveAndDeregister();
        for (int i = 0; i < THREADS * 100; i++) {
            boolean oneReleased = latch.releaseOne();
            int currExe = executions.get();
            do {
                Thread.sleep(5);
            } while (executions.get() == currExe);
        }
        exit.set(true);
        latch.disable();
        for (int i = 0; i < THREADS; i++) {
            latch.releaseOne();
        }
        onExit.await(5, TimeUnit.SECONDS);
        assertFalse("All threads did not run concurrently", max.getMaximum() == 1);
        assertEquals("Not all threads exited", exited.get(), THREADS);
    }

    private final AtomicInteger threadIds = new AtomicInteger();

    class Acquirer implements Runnable {

        private final AtomicBoolean exit;
        private final CountDownLatch onExit;
        private final OneThreadLatch latch;
        private final AtomicMaximum max;
        private final Phaser launch;
        private final AtomicInteger exited;
        private final AtomicBoolean active;

        Acquirer(AtomicBoolean exit, CountDownLatch onExit, OneThreadLatch latch, AtomicMaximum max, Phaser launch, AtomicInteger exited, AtomicBoolean active) {
            this.exit = exit;
            this.onExit = onExit;
            this.latch = latch;
            this.max = max;
            this.launch = launch;
            this.exited = exited;
            this.active = active;
        }

        @Override
        public void run() {
            try {
                Thread.currentThread().setName("Acquirer " + threadIds.incrementAndGet());
                launch.arriveAndAwaitAdvance();
                for (;;) {
                    latch.await();
                    if (exit.get()) {
                        return;
                    }
                    int exe = executions.getAndIncrement();
                    active.set(true);
                    try (QuietAutoCloseable clos = max.enter()) {
                        Thread.sleep(1);
                    }
                    active.set(false);
                    if (exit.get()) {
                        return;
                    }
                }
            } catch (InterruptedException ex) {
                if (exit.get()) {
                    return;
                }
            } finally {
                int xcount = exited.incrementAndGet();
                onExit.countDown();
            }
        }

    }

}
