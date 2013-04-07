/*
 *               BSD LICENSE NOTICE
 * Copyright (c) 2010-2012, Tim Boudreau
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.util.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicMaximumTest {

    static final class R implements Runnable {
        private final AtomicMaximum max;
        private final CountDownLatch entryLatch;
        private final CountDownLatch exitLatch;
        private final CountDownLatch inLatch;
        private final CountDownLatch threadStartLatch;
        R(AtomicMaximum max, CountDownLatch entryLatch, CountDownLatch exitLatch, CountDownLatch inLatch, CountDownLatch threadStartLatch) {
            this.max = max;
            this.entryLatch = entryLatch;
            this.exitLatch = exitLatch;
            this.inLatch = inLatch;
            this.threadStartLatch = threadStartLatch;
        }

        @Override
        public void run() {
            //tell the main thread that this runnable is now running
            InLock in = new InLock();
            threadStartLatch.countDown();
            try {
                //wait for the main thread - we will get the go signal once
                //all R's have reached this line of code
                entryLatch.await();
                //now enter the AtomicMaximum, which will count this thread
                //and keep the max number of threads
                max.run(in);
                //We're done, tell the main thread that this runnable has
                //exited AtomicMaximum.run()
                exitLatch.countDown();
            } catch (InterruptedException ex) {
                Logger.getLogger(AtomicMaximumTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        volatile boolean hasRun;
        private class InLock implements Runnable {

            @Override
            public void run() {
                hasRun = true;
                try {
                    inLatch.await();
                } catch (InterruptedException ex) {
                    Logger.getLogger(AtomicMaximumTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

    }

    @Test
    public void testStatistics() throws InterruptedException {
        int maxThreads = 50;
        //our statistics gatherer
        AtomicMaximum max = new AtomicMaximum();
        //array of 50 runnables
        R[] rs = new R[maxThreads];
        //all threads will be launched and wait for this latch
        CountDownLatch entryLatch = new CountDownLatch(1);
        //main thread will wait until all threads have been started
        CountDownLatch threadStartLatch = new CountDownLatch (maxThreads);
        //all background threads will wait inside AtomicMaximum.run() for this
        //latch
        CountDownLatch inLatch = new CountDownLatch (1);
        //main thread will wait on this latch to guarantee threads have completed
        CountDownLatch exitLatch = new CountDownLatch (maxThreads);
        //create a thread pool
        ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
        for (int i = 0; i < rs.length; i++) {
            rs[i] = new R(max, entryLatch, exitLatch, inLatch, threadStartLatch);
            //launch a thread with this R
            threadPool.submit(rs[i]);
        }
        //Wait until all threads have been started
        threadStartLatch.await();
        //release the background threads so they can enter AtomicMaximum.run()
        entryLatch.countDown();
        //busy-loop until we are sure
        boolean allRun = false;
        int ix = 0;
        while (!allRun) {
            allRun = true;
            for (R r : rs) {
                allRun &= r.hasRun;
            }
            if (!allRun) {
                Thread.sleep(50);
            }
        }
        //All threads are now inside AtomicMaximum.run(), so the total number
        //of threads must equal maxThreads.  Release them so they can exit
        inLatch.countDown();
        //Wait until all threads have returned from AtomicMaximum.run()
        exitLatch.await();
        //Test that all runnables ran
        for (R r : rs) {
            assertTrue (r.hasRun);
        }
        //There should be no threads in there anymore
        assertEquals (0, max.countActiveThreads());
        //Test that all threads were in AtomicMaximum.run() at the same time
        //and were counted correctly
        assertEquals (maxThreads, max.getMaximum());
        max.reset();
        assertEquals (0, max.countActiveThreads());
        assertEquals (0, max.getMaximum());
    }

    @Test
    public void testSetIfGreater() {
        AtomicMaximum max = new AtomicMaximum();
        assertEquals (0, max.getMaximum());
        assertTrue (max.setMaximum(10));
        assertEquals (10, max.getMaximum());
        assertFalse (max.setMaximum(5));
        assertEquals (10, max.getMaximum());
        assertTrue (max.setMaximum(20));
        assertEquals (20, max.getMaximum());
        assertFalse (max.setMaximum(0));
        assertEquals (20, max.getMaximum());
        max.reset();
        assertEquals (0, max.getMaximum());
        assertTrue (max.setMaximum(50));
        assertEquals (50, max.getMaximum());
    }
}