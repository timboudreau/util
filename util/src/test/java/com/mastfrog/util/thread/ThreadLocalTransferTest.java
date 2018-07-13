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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class ThreadLocalTransferTest {
    
    @Test
    public void testTransfer() throws Throwable{
        int count = 8;
        ExecutorService exec = Executors.newFixedThreadPool(count);
        final ThreadLocalTransfer<Thread> th = new ThreadLocalTransfer<>();
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(count);
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            exec.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                        for (int j = 0; j < 200; j++) {
                            assertNull(th.get());
                            th.set(Thread.currentThread());
                            Thread.sleep(1);
                            assertSame(Thread.currentThread(), th.get());
                            assertNull(th.get());
                        }
                        counter.incrementAndGet();
                    } catch (Throwable ex) {
                        thrown.set(ex);
                        ex.printStackTrace();
                        return;
                    } finally {
                        done.countDown();;
                    }
                }
            });
        }
        startLatch.countDown();
        done.await(10, TimeUnit.SECONDS);
        Throwable t = thrown.get();
        if (t != null) {
            throw t;
        }
        assertEquals("Some threads did not run or exit normally", count, counter.get());
    }
}
