/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicLinkedQueueTest {

    @Test
    public void testCopiesAreIndependent() {
        String e = "e";
        AtomicLinkedQueue<String> q1 = new AtomicLinkedQueue<>(Arrays.asList("a", "b", "c", "d", "e"));
        AtomicLinkedQueue<String> q2 = q1.copy();
        q2.add("f");
        assertFalse(q1.contains("f"));
        assertTrue(q2.contains("f"));
        q1.removeByIdentity(e);
        assertTrue(q2.contains(e));
    }

    @Test
    public void testRemoveByIdentityTailAndHead() {
        Thing[] things = new Thing[100];
        for (int i = 0; i < things.length; i++) {
            things[i] = new Thing(1, i);
        }
        AtomicLinkedQueue<Thing> q = new AtomicLinkedQueue<>(Arrays.asList(things));
        assertTrue(q.removeByIdentity(things[things.length - 1]));
        assertFalse(q.contains(things[things.length - 1]));
        q.forEach(thing -> {
            assertNotEquals(things[things.length - 1], thing);
        });
        assertFalse(q.pop().equals(things[things.length - 1]));
        assertTrue(q.removeByIdentity(things[0]));
        assertFalse(q.contains(things[0]));
        q.forEach(thing -> {
            assertNotEquals(things[0], thing);
        });

        assertTrue(q.removeByIdentity(things[things.length / 2]));
        assertFalse(q.contains(things[things.length / 2]));
        q.forEach(thing -> {
            assertNotEquals(things[things.length / 2], thing);
        });
    }

    @Test
    public void testRemoveByIdentityCannotCreateDuplicatesWhenSingleThreaded() {
        Thing[] things = new Thing[100];
        for (int i = 0; i < things.length; i++) {
            things[i] = new Thing(1, i);
        }
        AtomicLinkedQueue<Thing> q = new AtomicLinkedQueue<>(Arrays.asList(things));
        Random r = new Random(10391013L);
        for (int i = 0; i < 500; i++) {
            int ix = r.nextInt(things.length);
            Thing toRemove = things[ix];
            q.removeByIdentity(toRemove);
            ix++;
            if (ix >= things.length) {
                ix = 0;
            }
            Thing toRemove2 = things[ix];
            q.removeByIdentity(toRemove2);
            q.add(toRemove);
            q.add(toRemove2);
        }
        List<Thing> all = q.drain();
        Collections.sort(all);
        assertEquals(all.toString(), new LinkedHashSet<>(all).size(), all.size());
    }

    @Test
    public void testRemoveByIdentity() {
        Thing[] things = new Thing[]{new Thing(1, 1), new Thing(1, 2), new Thing(1, 3), new Thing(1, 4)};
        AtomicLinkedQueue<Thing> q = new AtomicLinkedQueue<>(Arrays.asList(things));
        assertTrue(q.removeByIdentity(things[1]));
        assertEquals(things[3], q.pop());
        assertEquals(things[2], q.pop());
        assertEquals(things[0], q.pop());
        assertTrue(q.isEmpty());
    }

    @Test
    public void testReverseInPlace() {
        AtomicLinkedQueue<String> q1 = new AtomicLinkedQueue<>(Arrays.asList("a", "b", "c", "d"));
        q1.reverseInPlace();
        assertEquals("a", q1.pop());
        assertEquals("b", q1.pop());
        assertEquals("c", q1.pop());
        assertEquals("d", q1.pop());
    }

    @Test
    public void testPopInto() {
        AtomicLinkedQueue<String> q1 = new AtomicLinkedQueue<>(Arrays.asList("a", "b", "c", "d"));
        AtomicLinkedQueue<String> q2 = new AtomicLinkedQueue<>();
        StringSupplier supp = new StringSupplier();
        q1.popInto(q2, supp);
        assertQueue(q1, "a", "b", "c");
        assertQueue(q2, "d");

        q1.popInto(q2, supp);
        assertQueue(q1, "a", "b");
        assertQueue(q2, "d", "c");

        q1.popInto(q2, supp);
        assertQueue(q1, "a");
        assertQueue(q2, "d", "c", "b");

        q1.popInto(q2, supp);
        assertQueue(q1);
        assertQueue(q2, "d", "c", "b", "a");

        q1.popInto(q2, supp);
        assertQueue(q1);
        assertQueue(q2, "d", "c", "b", "a", "e");

        q1.popInto(q2, supp);
        assertQueue(q1);
        assertQueue(q2, "d", "c", "b", "a", "e", "f");
    }

    private void assertQueue(AtomicLinkedQueue<String> q, String... values) {
        List<String> expect = Arrays.asList(values);
        List<String> got = new ArrayList<>(values.length);
        q.forEach(str -> {
            got.add(0, str);
        });
        assertEquals(expect, got);
    }

    private static final class StringSupplier implements Supplier<String> {

        private char c = 'e';

        @Override
        public String get() {
            return new String(new char[]{c++});
        }
    }

    @Test
    public void testAtomicQueue() {
        AtomicLinkedQueue<String> q = new AtomicLinkedQueue<>();
        q.add("a").add("b").add("c");
        assertEquals(3, q.size());
        List<String> l = q.drain();
        assertEquals(Arrays.asList("a", "b", "c"), l);
        assertTrue(q.isEmpty());

        q.add("a").add("b").add("c");
        q.removeByIdentity("b");
        assertEquals(2, q.size());
        assertEquals(Arrays.asList("a", "c"), q.drain());
        assertTrue(q.isEmpty());

        q.add("a").add("b").add("c");
        assertEquals(3, q.size());
        q.pop();
        assertEquals(Arrays.asList("a", "b"), q.drain());

        q.add("a").add("b").add("c").add("d");
        q.filter(s -> {
            switch (s) {
                case "a":
                case "b":
                    return true;
                default:
                    return false;
            }
        }, (left, right) -> {
            assertEquals(Arrays.asList("a", "b"), left.drain());
            assertEquals(Arrays.asList("c", "d"), right.drain());
        });
        assertEquals(4, q.size());

        q.reverseInPlace();;
        assertEquals(Arrays.asList("d", "c", "b", "a"), q.drain());

        q.add("a").add("b").add("c").add("d");
        l = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            l.add(q.pop(() -> "x"));
        }
        assertEquals(Arrays.asList("d", "c", "b", "a", "x"), l);
    }

    <T> void assertListsEqual(List<T> expect, List<T> got) {
        if (!expect.equals(got)) {
            List<T> missing = new ArrayList<>(expect);
            missing.removeAll(got);
            List<T> surprises = new ArrayList<>(got);
            surprises.removeAll(expect);
            Set<T> s = new HashSet<>(got);
            List<T> duplicates = new ArrayList<>(got);
            for (T obj : s) {
                duplicates.remove(obj);
            }

            fail("Missing items: " + missing + ", unexpected: " + surprises
                    + " sizes " + expect.size() + " / " + got.size() + " duplicated items: " + duplicates);
        }
    }

    @Test
    public void testConsistencyUnderConcurrency() throws InterruptedException {
        AtomicLinkedQueue<Thing> q = new AtomicLinkedQueue<>();
        int count = 200;
        int threads = 8;
        CountDownLatch latch = new CountDownLatch(threads);
        Adder[] addrs = new Adder[threads];
        List<Thing> all = new ArrayList<>(count * threads);
        Phaser phaser = new Phaser(1);
        for (int i = 0; i < threads; i++) {
            addrs[i] = new Adder(i, count, q, true, latch, phaser);
            all.addAll(addrs[i].toAdd);
            new Thread(addrs[i]).start();
        }
        phaser.arriveAndDeregister();
        latch.await(30, TimeUnit.SECONDS);
        Collections.sort(all);
        List<Thing> actual = q.drain();
        Collections.sort(actual);
        assertListsEqual(all, actual);
    }

    static class Adder implements Runnable {

        private final List<Thing> toAdd = new CopyOnWriteArrayList<>();
        private final List<Thing> added = new ArrayList<>();
        private final AtomicLinkedQueue<Thing> q;
        private final boolean weird;
        private final CountDownLatch latch;
        private final Random random = new Random(98239892L);
        private final Phaser phaser;

        Adder(int series, int max, AtomicLinkedQueue<Thing> q, boolean weird, CountDownLatch latch, Phaser phaser) {
            for (int i = 0; i < max; i++) {
                toAdd.add(new Thing(series, i));
            }
            this.q = q;
            this.weird = weird;
            this.latch = latch;
            this.phaser = phaser;
        }

        @Override
        public void run() {
            phaser.arriveAndAwaitAdvance();
            try {
                List<Thing> all = new ArrayList<>(toAdd);
                Iterator<Thing> iter = all.iterator();
                for (int i = 0; iter.hasNext(); i++) {
                    Thing t = iter.next();
                    q.add(t);
                    toAdd.remove(t);
                    added.add(t);
                    if (weird && i % 20 == 0) {
                        Collections.shuffle(added, random);
                        Iterator<Thing> ai = added.iterator();
                        for (int j = 0; ai.hasNext() && j < 4; j++) {
                            Thing rem = ai.next();
                            boolean result = q.removeByIdentity(rem);
                            if (result) {
                                toAdd.add(rem);
                            }
                        }
                        all = new ArrayList<>(toAdd);
                        iter = all.iterator();
                    }
                }
            } finally {
                latch.countDown();
            }
        }
    }

    static class Thing implements Comparable<Thing> {

        private final int series;
        private final int ix;

        public Thing(int series, int ix) {
            this.series = series;
            this.ix = ix;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.series;
            hash = 37 * hash + this.ix;
            return hash;
        }

        public String toString() {
            return series + "-" + ix;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Thing other = (Thing) obj;
            if (this.series != other.series) {
                return false;
            }
            return this.ix == other.ix;
        }

        @Override
        public int compareTo(Thing o) {
            int result = Integer.compare(series, o.series);
            if (result == 0) {
                result = Integer.compare(ix, o.ix);
            }
            return result;
        }
    }
}
