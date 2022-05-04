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
package com.mastfrog.concurrent;

import static com.mastfrog.concurrent.IncrementableLatch.masked;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IncrementableLatchTest {
    
    @Test
    public void testMasked() {
        int val = Integer.MIN_VALUE | 23;
        assertEquals(23, masked(val));
    }

    @Test
    public void testSimple() throws InterruptedException {
        IncrementableLatch latch = IncrementableLatch.create();
        assertTrue(latch.isUnused());
        assertFalse(latch.countDown());
        assertEquals("1", latch.toString());
        assertFalse(latch.hasWaiters());
        AtomicBoolean inter = new AtomicBoolean();
        AtomicBoolean released = new AtomicBoolean();
        AtomicLong releasedAt = new AtomicLong();
        CountDownLatch entry = new CountDownLatch(1);
        assertFalse(latch.increment());
        assertFalse(latch.increment());
        AtomicInteger countOnEntry = new AtomicInteger();
        AtomicBoolean exited = new AtomicBoolean();
        long start = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try {
                countOnEntry.set(latch.count());
                latch.increment();
                entry.countDown();
                released.set(latch.await(Duration.ofSeconds(30)));
                releasedAt.set(System.currentTimeMillis());
            } catch (InterruptedException ex) {
                inter.set(true);
                Logger.getLogger(IncrementableLatchTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                exited.set(true);
            }
        });
        t.setDaemon(true);
        t.start();
        entry.await(2, TimeUnit.SECONDS);
        
        assertFalse(exited.get());
        assertEquals(2, countOnEntry.get());
        assertEquals(3, latch.count());
        BooleanSupplier supp = () -> {
            boolean result = latch.countDown();
            try {
                Thread.sleep(result ? 200 : 100);
            } catch (InterruptedException ex) {
                Logger.getLogger(IncrementableLatchTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return result;
        };

        assertFalse(supp.getAsBoolean());
        assertFalse(exited.get());
        assertFalse(supp.getAsBoolean());
        assertTrue(latch.hasWaiters());
        assertFalse(exited.get());
        assertFalse(latch.increment());
        assertFalse(latch.increment());
        Thread.sleep(30);
        assertFalse(supp.getAsBoolean());
        assertFalse(exited.get());
        assertFalse(supp.getAsBoolean());
        assertFalse(exited.get());
        
        assertTrue(supp.getAsBoolean());
        t.join(10000);
        assertEquals(0, latch.count());
        assertTrue(exited.get());
        assertFalse(inter.get());
        assertTrue(releasedAt.get() - start < 27000);
        assertFalse(latch.isUnused());
        assertEquals("0", latch.toString());
        assertFalse(latch.hasWaiters());
    }
}
