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

import com.mastfrog.function.TriConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

public class ConcurrentLinkedListTest {

    private static final int COUNT = 4000;
    private static final int THREADS = 4;
    private static Set<Integer> INTS;

    @Test
    public void testCopyFifo() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.fifo();
        INTS.forEach(l::push);
        checkHasAll(l);
        ConcurrentLinkedList<Integer> l1 = l.copy();
        assertTrue(l.size() == l1.size());
        Set<Integer> l1s = new HashSet<>();
        l1.forEach(l1s::add);
        assertEquals(INTS, l1s);
        l1.push(-2);
        checkHasAll(l1);
        assertTrue(l1.contains(-2));
        l.push(-13);
        checkHasAll(l);
        assertTrue(l.contains(-13));
        assertFalse(l.contains(-2));
        assertFalse(l1.contains(-13));
        assertNotEquals(Integer.valueOf(-13), l.peek());
        assertNotEquals(Integer.valueOf(-2), l1.peek());

        checkHasAll(l);

        Set<Integer> drained = new TreeSet<>();
        int count = l.drain(-42, drained::add);
        assertEquals(count, drained.size());
        assertTrue(drained.containsAll(INTS), drained::toString);
        assertEquals(1, l.size());
        String s = l.toString();
        assertEquals(Integer.valueOf(-42), l.pop(), s);
        assertEquals(0, l.size());
        assertFalse(l.iterator().hasNext());
    }

    private void checkHasAll(ConcurrentLinkedList<Integer> l) {
        for (Integer i : INTS) {
            assertTrue(l.contains(i), () -> "Missing " + i + " in " + l);
        }
    }

    @Test
    public void testCopyLifo() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.lifo();
        INTS.forEach(in -> l.push(in));
        checkHasAll(l);
        assertTrue(l.contains(INTS.iterator().next()));
        ConcurrentLinkedList<Integer> l1 = l.copy();
        assertTrue(l.size() == l1.size());
        Set<Integer> l1s = new HashSet<>();
        l1.forEach(l1s::add);
        assertEquals(INTS, l1s);
        l1.push(-2);
        checkHasAll(l1);
        assertTrue(l1.contains(-2));
        l.push(-13);
        checkHasAll(l);
        assertTrue(l1.contains(-2));
        assertTrue(l.contains(-13));
        assertFalse(l.contains(-2));
        assertFalse(l1.contains(-13));
        Integer lpop = l.pop();
        assertEquals(Integer.valueOf(-13), lpop);
        Integer l1pop = l1.pop();
        assertEquals(Integer.valueOf(-2), l1pop);

        Set<Integer> drained = new HashSet<>();
        int count = l.drain(-42, drained::add);
        assertEquals(count, drained.size());
        assertTrue(drained.containsAll(INTS));
        assertEquals(1, l.size());
        assertEquals(Integer.valueOf(-42), l.pop());
        assertEquals(0, l.size());
        assertFalse(l.iterator().hasNext());
    }

    @Test
    public void testLifoIsLifo() {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.lifo();
        l.push(1);
        l.push(2);
        l.push(3);
        List<Integer> list = new ArrayList<>();
        l.forEach(list::add);
        assertEquals(Arrays.asList(3, 2, 1), list);
        assertEquals(Integer.valueOf(3), l.pop(), l::toString);
        assertEquals(Integer.valueOf(2), l.pop(), l::toString);
        assertEquals(Integer.valueOf(1), l.pop(), l::toString);
    }

    @Test
    public void testFifoIsFifo() {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.fifo();
        l.push(1);
        l.push(2);
        l.push(3);
        List<Integer> list = new ArrayList<>();
        l.forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertEquals(Integer.valueOf(1), l.pop(), l::toString);
        assertEquals(Integer.valueOf(2), l.pop(), l::toString);
        assertEquals(Integer.valueOf(3), l.pop(), l::toString);
    }

    @Test
    public void testLifo() throws Throwable {
        testConcurrentAdds(THREADS, ConcurrentLinkedList.lifo());
    }

    @Test
    public void testFifo() throws Throwable {
        testConcurrentAdds(THREADS, ConcurrentLinkedList.fifo());
    }

    @Test
    public void testFifoUnthreaded() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.fifo();
        INTS.forEach(in -> l.push(in));
        checkHasAll(l);
        assertEquals(INTS.size(), l.size());
        assertMatch(l);
    }

    @Test
    public void testLifoUnthreaded() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.lifo();
        INTS.forEach(in -> l.push(in));
        checkHasAll(l);
        assertEquals(INTS.size(), l.size());
        assertMatch(l);
    }

    @Test
    public void testLifoConcurrentPop() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.lifo();
        INTS.forEach(in -> l.push(in));
        testConcurrentPops(THREADS, l);
    }

    @Test
    public void testFifoConcurrentPop() throws Throwable {
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.fifo();
        INTS.forEach(in -> l.push(in));
        testConcurrentPops(THREADS, l);
    }

    private void testConcurrentPops(int threadCount, ConcurrentLinkedList<Integer> list) throws Throwable {
        List<ConcurrentPopper> poppers
                = new ConcurrentTestHarness<ConcurrentPopper>(threadCount, _ignored -> new ConcurrentPopper(list)).run();
        Set<Integer> all = new TreeSet<>();
        poppers.forEach(p -> p.coalesce(all));
        assertEquals(INTS, all);
    }

    private void testConcurrentAdds(int threadCount, ConcurrentLinkedList<Integer> list) throws Throwable {
        List<List<Integer>> partitioned = partition(threadCount);
        IntFunction<ConcurrentAdder> pc = partition -> new ConcurrentAdder(list, partitioned.get(partition));
        new ConcurrentTestHarness<>(threadCount, pc).run();
        assertMatch(list);
    }

    private void assertMatch(ConcurrentLinkedList<Integer> list) {
        Set<Integer> all = new TreeSet<>();
        for (Integer val : list) {
            all.add(val);
        }

        assertEquals(INTS, all, "Collections do not match for " + (list.isFifo() ? "FIFO" : "LIFO"));

        Set<Integer> all2 = new TreeSet<>();
        int drained = list.drain(all2::add);
        assertEquals(COUNT, all2.size(), "Drain did not move anything");
        assertTrue(list.isEmpty(), "List is not empty after drain");
        assertEquals(COUNT, drained, "Drained count does not match");
        assertEquals(0, list.size(), "List size should be zero");
    }

    static class ConcurrentPopper implements Runnable {

        private final ConcurrentLinkedList<Integer> popFrom;
        private final LinkedList<Integer> popTo = new LinkedList<>();

        public ConcurrentPopper(ConcurrentLinkedList<Integer> popFrom) {
            this.popFrom = popFrom;
        }

        void coalesce(Set<Integer> into) {
            into.addAll(popTo);
        }

        @Override
        public void run() {
            for (;;) {
                Integer val = popFrom.pop();
                if (val == null) {
                    break;
                }
                popTo.push(val);
            }
        }
    }

    static class ConcurrentAdder implements Runnable {

        private final Iterator<Integer> ints;
        private final ConcurrentLinkedList<Integer> list;
        private final int sz;

        private ConcurrentAdder(ConcurrentLinkedList<Integer> list, List<Integer> items) {
            ints = items.iterator();
            this.list = list;
            this.sz = items.size();
        }

        @Override
        public void run() {
            while (ints.hasNext()) {
                list.push(ints.next());
            }
        }
    }

    private List<List<Integer>> partition(int by) {
        List<List<Integer>> all = new ArrayList<>();
        int amt = INTS.size() / by;
        Iterator<Integer> ints = INTS.iterator();
        List<Integer> curr = new ArrayList<>();
        all.add(curr);
        int ix = 0;
        do {
            curr.add(ints.next());
            if ((++ix % amt) == 0) {
                if (ints.hasNext()) {
                    curr = new ArrayList<>(amt);
                    all.add(curr);
                }
            }
        } while (ints.hasNext());
        return all;
    }

    @BeforeAll
    public static void setup() {
        Random rnd = new Random(134);
        INTS = new TreeSet<>();
        for (int i = 0; i < COUNT; i++) {
            INTS.add(i);
        }
//        while (INTS.size() < COUNT) {
//            INTS.add(rnd.nextInt());
//        }
    }

    Set<Integer> toSet(ConcurrentLinkedList<Integer> l) {
        TreeSet<Integer> result = new TreeSet<>();
        l.forEach(result::add);
        return result;
    }

    Set<Integer> setOf(int first, int last) {
        TreeSet<Integer> result = new TreeSet<>();
        for (int i = first; i <= last; i++) {
            result.add(i);
        }
        return result;
    }

    void twoBatches(int first, int last, TriConsumer<ConcurrentLinkedList<Integer>, Set<Integer>, Set<Integer>> c) {
        assertTrue(last > first, "Test is broken: " + first + " -> " + last);
        ConcurrentLinkedList<Integer> l = ConcurrentLinkedList.fifo();
        Set<Integer> firstBatch = new TreeSet<>();
        Set<Integer> secondBatch = new TreeSet<>();
        int mid = first + ((last - first) / 2);
        for (int i = first; i <= last; i++) {
            if (i < mid) {
                firstBatch.add(i);
            } else {
                secondBatch.add(i);
            }
            l.push(i);
        }
        c.accept(l, firstBatch, secondBatch);
    }
}
