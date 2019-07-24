/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class OneThreadLatchTest2 {

    private static final long DELAY_MS = 20L;
    private static final long DELAY_NANOS = DELAY_MS * 1000000L;
    private static final int THREADS = 6;
    private static final int LOOP_COUNT = 20;

    @Test
    public void testSomeMethod() throws InterruptedException {
        int threads = THREADS;
        Phaser phaser = new Phaser(1);
        CountDownLatch exited = new CountDownLatch(threads);
        OneThreadLatch latch = new OneThreadLatch();
        R[] rs = new R[threads];
        AtomicInteger total = new AtomicInteger();
        int loopCount = LOOP_COUNT;
        for (int i = 0; i < threads; i++) {
            rs[i] = new R(i + ". R", latch, phaser, total, exited);
            Thread t = new Thread(rs[i]);
            t.setName(rs[i].name);
            t.setDaemon(true);
            t.start();
        }
        phaser.arriveAndDeregister();

        for (int i = 0; i < loopCount; i++) {
            Thread.sleep(DELAY_MS);
//            System.out.println("\n release");
            assertEquals(threads, latch.waiterCount());
            latch.releaseOne();
        }
        latch.disable();
        exited.await(5, TimeUnit.SECONDS);
        assertEquals(0, latch.waiterCount());
        for (R r : rs) {
            assertTrue(r.exited);
            assertTrue("Not all threads looped: " + r.loops, r.loops > 1);
        }
//        System.out.println("TOTAL LOOPS " + total.get());
        // This depends on the vagaries of the thread scheduler, so a thread
        // may get an extra loop counted if it has just unparked when
        // we disable the latch
        assertTrue(total.get() >= loopCount - 1 && total.get() < loopCount + threads);
//        System.out.println("LATCH STATE " + latch.releaseCount());
        assertTrue(latch.releaseCount() < 0);
    }

    static class R implements Runnable {

        private final String name;
        private final OneThreadLatch latch;
        private final Phaser blockMainThreadUntilStarted;
        private final AtomicInteger total;
        int loops = 0;
        private volatile boolean exited;
        private final CountDownLatch exitLatch;

        R(String name, OneThreadLatch latch, Phaser blockMainThreadUntilStarted, AtomicInteger total, CountDownLatch exitLatch) {
            this.name = name;
            this.latch = latch;
            this.blockMainThreadUntilStarted = blockMainThreadUntilStarted;
            this.total = total;
            blockMainThreadUntilStarted.register();
            this.exitLatch = exitLatch;
        }

        @Override
        public void run() {
            try {
                blockMainThreadUntilStarted.arriveAndAwaitAdvance();
                for (;; loops++) {
                    try {
                        if (latch.isDisabled()) {
                            break;
                        }
//                        System.out.println("Before " + name + "-" + (loops + 1));
                        long then = System.nanoTime();
                        latch.await();
                        long elapsed = System.nanoTime() - then;
                        if (latch.isDisabled()) {
                            break;
                        }
                        assertTrue("Elapsed " + elapsed + " < " + DELAY_NANOS, elapsed >= DELAY_NANOS);
//                        System.out.println("DOIT!! " + name + "-" + (loops + 1) + " elapsed? " + elapsed);
                        total.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(OneThreadLatchTest2.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
//                System.out.println("EXIT " + name);
            } finally {
                exited = true;
                exitLatch.countDown();
            }
        }

    }
}
