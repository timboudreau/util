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
package com.mastfrog.util.collections;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
    public void testRemoveSingle() throws Throwable {
        AtomicLinkedQueue<Integer> q = new AtomicLinkedQueue<>();
        q.add(5);
        assertEquals(1, q.size());
        q.remove(6);
        q.remove(5);
        assertEquals(0, q.size());
    }

    @Test
    public void testConcurrentRemove() throws InterruptedException {
        Phaser ph = new Phaser(4);
        List<Integer> toRemove = new CopyOnWriteArrayList<>();
        int count = 10000;
        for (int i = 0; i < count; i++) {
            toRemove.add(i);
        }
        AtomicLinkedQueue<Integer> in = new AtomicLinkedQueue<>(toRemove);
        BatchRemover rem1 = new BatchRemover(ph, toRemove, in);
        BatchRemover rem2 = new BatchRemover(ph, toRemove, in);
        BatchRemover rem3 = new BatchRemover(ph, toRemove, in);
        Thread t1 = new Thread(rem1);
        Thread t2 = new Thread(rem2);
        Thread t3 = new Thread(rem3);
        t1.setName("t1");
        t2.setName("t2");
        t2.setName("t3");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t3.setDaemon(true);

        t1.start();
        t2.start();
        t3.start();
        ph.arriveAndDeregister();
        t1.join(10000);
        t2.join(10000);
        t3.join(10000);
        rem1.rethrow();
        rem2.rethrow();
        assertEquals("Remove count does not match", count, rem1.removed + rem2.removed + rem3.removed);
        assertTrue("Queue not emptied", in.isEmpty());
    }

    static class BatchRemover implements Runnable {

        private final Phaser ph;
        private final List<Integer> ints;
        private final AtomicLinkedQueue<Integer> q;
        private Throwable thrown;
        volatile int removed;

        public BatchRemover(Phaser ph, List<Integer> ints, AtomicLinkedQueue<Integer> q) {
            this.ph = ph;
            this.ints = ints;
            this.q = q;
        }

        @Override
        public void run() {
            try {
                ph.arriveAndAwaitAdvance();
                int loop;
                for (loop = 0;; loop++) {
                    if (!ints.isEmpty()) {
                        Integer val = null;
                        try {
                            val = ints.remove(0);
                        } catch (Exception e) {
                            // concurrent removal is possible
                        }
                        if (val != null) {
                            if (q.remove(val)) {
                                removed++;
                            } else {
                                System.out.println("failed to remove " + val);
                            }
                        }
                    }
                    if (ints.isEmpty()) {
                        break;
                    }
                }
                System.out.println(loop + " loops on " + Thread.currentThread().getName());
            } catch (Throwable t) {
                thrown = t;
            }
        }

        void rethrow() {
            if (thrown != null) {
                Exceptions.chuck(thrown);
            }
        }
    }

    @Test
    public void testSwap() {
        AtomicLinkedQueue<String> a = new AtomicLinkedQueue<>(Arrays.asList("A", "B", "C"));
        AtomicLinkedQueue<String> b = new AtomicLinkedQueue<>(Arrays.asList("D", "E", "F"));
        a.swapContents(b);
        assertEquals("D,E,F", a.toString());
        assertEquals("A,B,C", b.toString());
        a.swapContents(b);
        assertEquals("D,E,F", b.toString());
        assertEquals("A,B,C", a.toString());
    }

    @Test
    public void testRemoveRetain() {
        AtomicLinkedQueue<String> q = new AtomicLinkedQueue<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        q.retainAll(Arrays.asList("B", "D", "F", "G", "I", "Q"));
        assertEquals("B,D,F,G,I", q.toString());

        q = new AtomicLinkedQueue<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        q.removeAll(Arrays.asList("B", "D", "F", "G", "I"));
        q.retainAll(Collections.emptySet());
        assertTrue(q.isEmpty());

        q = new AtomicLinkedQueue<>();
        q.retainAll(Arrays.asList("A", "B", "C"));
        assertTrue(q.isEmpty());

        q.addAll(Arrays.asList("A", "B", "C", "D"));
        assertEquals("A,B,C,D", q.toString());
        q.retainAll(Arrays.asList("G", "H", "I"));
        assertTrue(q.isEmpty());
    }

    @Test
    public void testTransfer() {
        AtomicLinkedQueue<String> a = new AtomicLinkedQueue<>(Arrays.asList("A", "B", "C"));
        AtomicLinkedQueue<String> b = new AtomicLinkedQueue<>();
        assertEquals(3, a.size());
        assertTrue(b.isEmpty());

        a.drainTo(b);
        assertTrue(a.isEmpty());
        assertEquals(3, b.size());
        assertEquals("A,B,C", b.toString());

        AtomicLinkedQueue<String> c = new AtomicLinkedQueue<>(Arrays.asList("D", "E", "F"));
        c.drainTo(b);

        assertTrue(c.isEmpty());
        assertEquals(6, b.size());
        assertTrue(a.isEmpty());
        assertEquals("A,B,C,D,E,F", b.toString());

        a.transferContentsFrom(b);
        assertTrue(b.isEmpty());
        assertEquals(6, a.size());
        assertEquals("A,B,C,D,E,F", a.toString());

        AtomicLinkedQueue<String> d = new AtomicLinkedQueue<>(Arrays.asList("G", "H", "I"));
        a.transferContentsFrom(d);

        assertTrue(d.isEmpty());
        assertEquals(9, a.size());
        assertEquals("A,B,C,D,E,F,G,H,I", a.toString());

        d.transferContentsFrom(new AtomicLinkedQueue<String>());
        assertTrue(d.isEmpty());
        d.drainTo(b);
        assertTrue(d.isEmpty());
        assertTrue(b.isEmpty());
    }

    @Test
    public void testStream() {
        int max = 20000;
        AtomicLinkedQueue<Integer> q = new AtomicLinkedQueue<>();
        LinkedList<Integer> expected = new LinkedList<>();
        for (int i = 0; i < max; i++) {
            q.add(i);
            expected.push(i);
        }
        LinkedList<Integer> l = new LinkedList<>();
        try (Stream<Integer> s = q.stream()) {
            s.forEach(l::add);
        }
        assertEquals(expected, l);
        Map<Set<Integer>, Boolean> m = Collections.synchronizedMap(new IdentityHashMap<>());
        ThreadLocal<Set<Integer>> tl = ThreadLocal.withInitial(HashSet::new);

        try (Stream<Integer> s = q.parallelStream()) {
            s.forEach(i -> {
                Set<Integer> set = tl.get();
                m.put(set, true);
                tl.set(set);
                set.add(i);
            });
        }
        Set<Integer> all = new TreeSet<>();
        for (Set<Integer> s : m.keySet()) {
            all.addAll(s);
        }
        LinkedList<Integer> parallelGot = new LinkedList<>(all);
        Collections.reverse(parallelGot);
        assertEquals(expected, parallelGot);

        List<Set<Integer>> allSets = new ArrayList<>(m.keySet());
        for (int i = 0; i < allSets.size(); i++) {
            for (int j = 0; j < allSets.size(); j++) {
                if (i == j) {
                    continue;
                }
                Set<Integer> copy = new HashSet<>(allSets.get(i));
                copy.retainAll(allSets.get(j));
                assertTrue("More than one set contains " + copy, copy.isEmpty());
            }
        }
    }

    @Test
    public void testReplaceContents() {
        AtomicLinkedQueue<String> q1 = new AtomicLinkedQueue<>(Arrays.asList("a", "b", "c", "d"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), q1.asList());
        q1.replaceContents(Arrays.asList("e", "f", "g", "h"));
        assertEquals(Arrays.asList("e", "f", "g", "h"), q1.asList());
        assertEquals("h", q1.get(0));
        assertEquals("g", q1.get(1));
        assertEquals("f", q1.get(2));
        assertEquals("e", q1.get(3));
        try {
            q1.get(4);
            fail("Should have thrown for nonexistent element");
        } catch (NoSuchElementException ex) {

        }
    }

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
        AtomicLinkedQueue<String> q = new AtomicLinkedQueue<>(Arrays.asList("a", "b", "c"));
        assertEquals(3, q.size());
        List<String> l = q.drain();
        assertEquals(Arrays.asList("a", "b", "c"), l);
        assertTrue(q.isEmpty());

        q.addAll(Arrays.asList("a", "b", "c"));
        q.removeByIdentity("b");
        assertEquals(2, q.size());
        assertEquals(Arrays.asList("a", "c"), q.drain());
        assertTrue(q.isEmpty());

        q.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, q.size());
        q.pop();
        assertEquals(Arrays.asList("a", "b"), q.drain());

        q.addAll(Arrays.asList("a", "b", "c", "d"));
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

//        q.add("a").add("b").add("c").add("d");
        q.addAll(Arrays.asList("a", "b", "c", "d"));
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
