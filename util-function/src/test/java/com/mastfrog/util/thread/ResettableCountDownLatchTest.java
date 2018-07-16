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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class ResettableCountDownLatchTest {

    @Test
    public void testReset() throws InterruptedException {
        ResettableCountDownLatch latch = new ResettableCountDownLatch(3);
        assertEquals(3L, latch.getCount());
        latch.reset(5);
        assertEquals(5L, latch.getCount());
        latch.countDown();
        assertEquals(4, latch.getCount());
        latch.countDown();
        assertEquals(3, latch.getCount());
        latch.countDown();
        assertEquals(2, latch.getCount());
        latch.countDown();
        assertEquals(1, latch.getCount());
        latch.countDown();
        assertEquals(0, latch.getCount());
        latch.reset(1);
        assertEquals(1, latch.getCount());
        latch.countDown();
        assertEquals(0, latch.getCount());

        latch.reset(3);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger countDownCount = new AtomicInteger();
        final CountDownLatch exitLatch = new CountDownLatch(1);
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        startLatch.await();
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ResettableCountDownLatchTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    while (latch.getCount() > 0) {
                        latch.countDown();
                        countDownCount.incrementAndGet();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ResettableCountDownLatchTest.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } finally {
                    exitLatch.countDown();
                }
            }
        });
        th.start();
        Thread.sleep(50);
        latch.reset(5);
        startLatch.countDown();
        latch.await(10, TimeUnit.SECONDS);
        exitLatch.await(5, TimeUnit.SECONDS);
        assertEquals(0L, latch.getCount());
        assertEquals(5, countDownCount.get());
    }

}
