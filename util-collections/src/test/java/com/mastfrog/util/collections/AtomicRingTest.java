/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.util.collections;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicRingTest {

    @Test
    public void testSimpleAtomic() {
        testSimple(AtomicRing::new);
    }

    @Test
    public void testSimpleAtomicFallible() {
        testSimple(AtomicFallibleRing::new);
    }

    @Test
    public void testSizeFallible() {
        testTargetSizeCannotBeExceeded("Fallible", AtomicFallibleRing::new);
    }

    @Test
    public void testSize() {
        testTargetSizeCannotBeExceeded("Atomic", AtomicRing::new);
    }

    private void testTargetSizeCannotBeExceeded(String nm, IntFunction<Ring<String>> factory) {
        for (int size : new int[]{31, 64, 73, 129}) {
            Ring<String> ring = factory.apply(size);
            for (int i = 0; i < size * 3; i++) {
                String s = Integer.toString(i);
                ring.accept(s);
                if (i >= size) {
                    List<String> contents = new ArrayList<>();
                    for (String item : ring) {
                        contents.add(item);
                    }
                    assertEquals(nm + ": Wrong size for ring contents after " + i
                            + "/" + size, size, contents.size());
                }
            }
        }
    }

    private void testSimple(IntFunction<Ring<String>> factory) {
        int size = 7;
        Ring<String> r = factory.apply(size);
        for (char c = 'A'; c <= 'z'; c++) {
            String s = new String(new char[]{c});
            r.accept(s);
            StringBuilder sb = new StringBuilder();
            r.forEach(sb::append);
            char start = (char) Math.max('A', c - (size - 1));
            StringBuilder expect = new StringBuilder(size);
            for (char c1 = start; c1 <= c; c1++) {
                expect.append(c1);
            }
            assertEquals(expect.toString(), sb.toString());

            StringBuilder iterSb = new StringBuilder();
            for (String st : r) {
                iterSb.append(st);
            }
            assertEquals(expect.toString(), sb.toString());
        }
    }

    @Test
    public void testConcurrentAtomic() throws InterruptedException {
        testConcurrent(AtomicRing::new);
    }

    @Test
    public void testConcurrentFallibleAtomic() throws InterruptedException {
        testConcurrent(AtomicFallibleRing::new);
    }

    public void testConcurrent(IntFunction<Ring<String>> factory) throws InterruptedException {
        int iters = 1500;
        List<String> collected = new ArrayList<>(2000);
        int ringSize = 23;
        Ring<String> ring = factory.apply(ringSize);
        // Delay start until everybody is ready to inter their loop
        Phaser phas = new Phaser(7);

        // Synchronize loop starts, so shorter ranges of characters
        // aren't exhausted so quickly - they will wait for the rest
        // and all iterations start for all threads at the same time
        CyclicBarrier loopSync = new CyclicBarrier(5);
        CR firstUppers = new CR('A', 'M', iters, ring, phas, loopSync);
        CR secondUppers = new CR('N', 'Z', iters, ring, phas, loopSync);
        CR secondLowers = new CR('a', 'm', iters, ring, phas, loopSync);
        CR firstLowers = new CR('n', 'z', iters, ring, phas, loopSync);
        CR numbers = new CR('0', '9', iters, ring, phas, loopSync);
        Watcher w = new Watcher(ring, phas, collected);
        LinkedList<Thread> threads = new LinkedList<>();
        threads.add(firstUppers.start());
        threads.add(secondUppers.start());
        threads.add(firstLowers.start());
        threads.add(secondLowers.start());
        threads.add(numbers.start());
        Thread watchThread = w.start();
        Thread.yield();
        // Release the cracken
        phas.arriveAndDeregister();
        while (!threads.isEmpty()) {
            threads.pop().join();
        }
        // Kill the watch thread and wait for it to exit
        watchThread.interrupt();
        watchThread.join();
        w.rethrow();
        assertFalse(collected.isEmpty());
        // We only care about unique strings
        HashSet<String> all = new HashSet<>(collected);
        for (String c : all) {
            // What we need to test here is:
            //  - That we never see an entry longer than we expect - that
            //    would mean we saw an entry with a new tail added and the
            //    old head - which is impossible with the current implementation,
            //    but a risk for any changes
            assertTrue("Wrong length - collector saw a partially updated ring",
                    c.length() <= ringSize);
            for (int i = 1; i < c.length(); i++) {
                // And, if we ever saw the same character twice in a row, that
                // would likely mean a double-add, which also would mean
                // concurrency is broken
                char prev = c.charAt(i - 1);
                char curr = c.charAt(i);
                // FIXME:  This test is not correct for this case - we should
                // use an atomic integer or something that cannot possibly repeat,
                // otherwise it IS possible to encounter adjacent values - the
                // background threads just need to complete an entire cycle between
                // the watcher thread's read of one element and the next

//                assertNotEquals("Adjacent characters " + prev + " in " + c
//                        + " - same item was added twice", prev, curr);
            }
        }
    }

    private static final class Watcher implements Runnable {

        private final Ring<String> ring;
        private final StringBuilder sb = new StringBuilder();
        private final List<String> collected;
        private final Phaser ph;
        private volatile Throwable thrown;

        public Watcher(Ring<String> ring, Phaser ph, List<String> collected) {
            this.ring = ring;
            this.ph = ph;
            this.collected = collected;
        }

        Thread start() {
            Thread t = new Thread(this, "watcher");
            t.setDaemon(true);
            t.start();
            return t;
        }

        void rethrow() {
            if (thrown != null) {
                Exceptions.chuck(thrown);
            }
        }

        @Override
        public void run() {
            try {
                ph.arriveAndAwaitAdvance();
                Consumer<String> cb = sb::append;
                char[] cc = new char[1];
                while (!Thread.interrupted()) {
                    ring.forEach(ch -> {
                        char last = cc[0];
                        sb.append(ch);
                        cc[0] = ch.charAt(0);
                    });
                    collected.add(sb.toString());
                    sb.setLength(0);
                    cc[0] = 0;
                }
            } catch (Throwable thrown) {
                this.thrown = thrown;
            }
        }

    }

    private static class CR implements Runnable {

        private final char start;
        private final char end;
        private final int iters;
        private final Ring<String> ring;
        private final Phaser phaser;
        private final CyclicBarrier loopSync;

        public CR(char start, char end, int iters, Ring<String> ring, Phaser phaser, CyclicBarrier loopSync) {
            this.start = start;
            this.end = end;
            this.iters = iters;
            this.ring = ring;
            this.phaser = phaser;
            this.loopSync = loopSync;

            StringBuilder sb = new StringBuilder();
            for (char c = start; c <= end; c++) {
                sb.append(c);
            }
//            System.out.println("CR " + sb);
        }

        Thread start() {
            Thread t = new Thread(this, start + "-" + end);
            t.setDaemon(true);
            t.start();
            return t;
        }

        @Override
        public void run() {
            phaser.arriveAndAwaitAdvance();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(AtomicRingTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            char[] arr = new char[1];
            for (int i = 0; i < iters; i++) {
                for (char c = start; c <= end; c++) {
                    arr[0] = c;
                    ring.accept(new String(arr));
                    Thread.yield();
                }
                try {
                    loopSync.await();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    Logger.getLogger(AtomicRingTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
