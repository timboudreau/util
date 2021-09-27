package com.mastfrog.concurrent.lock;

import com.mastfrog.concurrent.lock.Favor;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MultiLockTest {

    @Test
    public void testAcquireExclusion() throws InterruptedException {
        MultiLock lock = new MultiLock();
        BitSet excluded = new BitSet(64);
        excluded.set(2, 7);
        excluded.set(10, 12);
        excluded.set(63);
        excluded.set(0);
        excluded.set(61);
        excluded.set(60);
        excluded.set(59);
        excluded.set(40, 53);

        PrimitiveIterator.OfInt iter = lock.acquireN(20, Favor.BOTH, i -> !excluded.get(i));
        int[] arr = toIntArray(iter);

        System.out.println("GOT " + Arrays.toString(arr));
        assertEquals(arr.length, 20);
        for (int i = 0; i < arr.length; i++) {
            assertFalse(excluded.get(arr[i]));
        }
    }

    @Test
    public void testTerminate() throws InterruptedException {
        AtomicInteger ct = new AtomicInteger();
        MultiLock lock = new MultiLock();
        AtomicIntegerArray stateArr = new AtomicIntegerArray(64);
        CountDownLatch entryLatch = new CountDownLatch(63);
        CountDownLatch latch = new CountDownLatch(63);
        Runnable deadlocker = () -> {
            int curr = ct.getAndIncrement();
            try {
                stateArr.set(curr, 1);
                lock.lock(curr);
                stateArr.set(curr, 2);
                latch.countDown();
                lock.lock(curr);
                stateArr.set(curr, 3);
            } catch (InterruptedException ex) {
            } finally {
                stateArr.set(curr, 4);
                latch.countDown();
            }
        };
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 64; i++) {
            Thread t = new Thread(deadlocker, "t-" + i);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        entryLatch.await(1, TimeUnit.SECONDS);
        awaitAll(2, stateArr);
        int interrupted = lock.terminate();
        assertEquals(64, interrupted);
        latch.await(1, TimeUnit.SECONDS);
        for (Thread t1 : threads) {
            t1.join(100);
        }
        for (Thread t2 : threads) {
            assertFalse(t2.isAlive(), "Thread " + t2.getName() + " is still alive");
        }
    }

    private static void awaitAll(int val, AtomicIntegerArray arr) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            boolean allOk = true;
            for (int j = 0; j < arr.length(); j++) {
                allOk &= arr.get(j) == val;
                if (!allOk) {
                    break;
                }
            }
            if (allOk) {
                break;
            }
            Thread.sleep(20);
        }
    }

    @Test
    public void testLockingTwiceDeadlocks() throws InterruptedException {
        MultiLock lock = new MultiLock();
        AtomicInteger lockedState = new AtomicInteger();
        Runnable doubleLock = () -> {
            try {
                lockedState.set(1);
                lock.lock(3, 4, 5);
                lockedState.set(2);
                lock.lock(5, 6, 7);
                lockedState.set(3);
            } catch (InterruptedException ex) {
                lockedState.set(4);
            } finally {
                lock.unlock(3, 4, 5, 6);
                lockedState.set(5);
            }
            lockedState.set(6);
        };
        Thread t = new Thread(doubleLock, "double-lock");
        t.setDaemon(true);
        t.start();
        t.join(300);
        assertTrue(t.isAlive(), "Thread should still be alive (and deadlocked on itself)");
        assertEquals(2, lockedState.get(), "Locked state should not have progressed past 2");
        boolean lockBitsSeen = false;
        boolean parkSeen = false;
        for (StackTraceElement ste : t.getStackTrace()) {
            String s = ste.toString();
            if (s.contains("lockBits")) {
                lockBitsSeen = true;
            }
            if (s.contains("park")) {
                parkSeen = true;
            }
        }
        assertTrue(parkSeen, "Thread should be in LockSupport.park() or Unsafe.park() depending on JDK version");
        assertTrue(lockBitsSeen, "Thread should be blocked in lockBits");
        t.interrupt();
        t.join(1000);
        assertEquals(6, lockedState.get(), "Thread state should have progressed to 6");
    }

    @Test
    public void testAcquireAdhoc() throws InterruptedException {
        Map<Favor, int[]> exp = new EnumMap<>(Favor.class);
        exp.put(Favor.LEAST, new int[]{0, 3, 4, 6, 8, 9, 10, 11, 12, 13});
        exp.put(Favor.GREATEST, new int[]{47, 48, 49, 50, 51, 52, 55, 58, 61, 62});
        exp.put(Favor.BOTH, new int[]{0, 3, 4, 6, 8, 52, 55, 58, 61, 62});

        for (Favor favoring : Favor.values()) {

            MultiLock lock = new MultiLock();
            lock.lock(1, 2, 5, 7, 63, 60, 59, 57, 53, 56, 54);
            int origInUse = lock.inUse();
            PrimitiveIterator.OfInt iter = lock.acquireN(10, favoring);

            int nowInUse = lock.inUse();
            assertEquals(origInUse + 10, nowInUse);

            int[] expected = exp.get(favoring);
            assertNotNull(expected, "New enum constant added? " + favoring);
            int[] got = toIntArray(iter);
            assertArrayEquals(expected, got, "Non-match for " + favoring);

            PrimitiveIterator.OfInt empty = lock.acquireN(lock.available() + 1, favoring);
            assertFalse(empty.hasNext());
        }
    }

    private int[] toIntArray(PrimitiveIterator.OfInt bits) {
        List<Integer> ints = new ArrayList<>();
        while (bits.hasNext()) {
            ints.add(bits.next());
        }
        int[] result = new int[ints.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ints.get(i);
        }
        return result;
    }

    @Test
    public void testUnlockFromWrongThread() throws InterruptedException {
        MultiLock lock = new MultiLock();
        CountDownLatch latch = new CountDownLatch(1);
        Phaser ph = new Phaser(2);
        AtomicInteger unlocked = new AtomicInteger();
        AtomicLong lockState = new AtomicLong();
        AtomicInteger waitState = new AtomicInteger();
        CountDownLatch waitLatch = new CountDownLatch(1);
        CountDownLatch waitExit = new CountDownLatch(1);
        AtomicLong waitEndState = new AtomicLong();
        AtomicBoolean lastUnlock = new AtomicBoolean();

        Runnable waiter = () -> {
            waitState.set(1);
            waitLatch.countDown();
            try {
                lock.lock(1, 5);
                waitState.set(2);
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                waitEndState.set(lock.state());
                boolean u = lock.unlock(1, 5);
                lastUnlock.set(u);
                waitExit.countDown();
            }
        };

        Runnable locker = () -> {
            try {
                lock.lock(1, 2);
                ph.arriveAndDeregister();
                latch.await(10, SECONDS);
                lockState.set(lock.state());
                unlocked.set(lock.isLocked(1) ? 1 : 2);
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        Thread t = new Thread(locker, "t1");
        t.setDaemon(true);
        t.start();
        ph.arriveAndAwaitAdvance();

        Thread wait = new Thread(waiter, "t2");
        wait.setDaemon(true);
        wait.start();

        waitLatch.await(1, SECONDS);
        assertEquals(1, waitState.get());

        assertTrue(lock.unlock(2));
        assertEquals(0, unlocked.get());
        latch.countDown();
        t.join(1000);
        assertEquals(1, unlocked.get());
        assertTrue(lock.unlock(1));

        waitExit.await(1, SECONDS);
    }

    @Test
    public void testLockedBits() throws InterruptedException {
        MultiLock lock = new MultiLock();
        assertFalse(lock.isLocked());
        lock.lock(0, 1);
        assertTrue(lock.isLocked());
        assertTrue(lock.isLocked(0, 1));
        assertTrue(lock.isLocked(1));
        assertTrue(lock.isLocked(0));

        boolean u1 = lock.unlock(0);
        boolean u2 = lock.unlock(1);

        assertFalse(lock.isLocked());
        assertTrue(u1);
        assertTrue(u2);

        long empty = lock.state();
        assertEquals(0L, lock.state());

        boolean u3 = lock.unlock(1);
        boolean u4 = lock.unlock(0);
        boolean u5 = lock.unlock(23);
        assertEquals(0L, lock.state());
        assertFalse(u3);
        assertFalse(u4);
        assertFalse(u5);
    }

    @Test
    public void testReentrant() throws Exception {
        MultiLock lock = new MultiLock();
        AtomicInteger ct = new AtomicInteger();
        Runnable lockIt = new Runnable() {
            @Override
            public void run() {
                int position = ct.getAndIncrement();
                if (position < 64) {
                    try {
                        lock.lockingReentrantly(this, position);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        lockIt.run();
        assertEquals(65, ct.get());
    }

    @Test
    public void testReentrantMulti() throws Exception {
        MultiLock lock = new MultiLock();
        AtomicInteger ct = new AtomicInteger();
        Phaser p = new Phaser(2);
        AtomicInteger maxInLock = new AtomicInteger();
        AtomicInteger minAvail = new AtomicInteger(64);
        AtomicInteger maxAvail = new AtomicInteger();
        Runnable lockIt = new Runnable() {
            @Override
            public void run() {
                p.arriveAndDeregister();
                int position = ct.getAndIncrement();
                if (position < 64) {
                    try {
                        lock.lockingReentrantly(this, position);
                        maxInLock.getAndUpdate(old -> {
                            return Math.max(old, lock.inUse());
                        });
                        minAvail.getAndUpdate(old -> {
                            return Math.min(old, lock.available());
                        });
                        maxAvail.getAndUpdate(old -> {
                            return Math.max(old, lock.available());
                        });
                        boolean locked = lock.isLocked(position);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        Thread t = new Thread(lockIt);
        t.setDaemon(true);
        t.setName("oh");
        t.start();
        p.arriveAndAwaitAdvance();
        lockIt.run();
        t.join(1000);
        assertEquals(66, ct.get());
    }

    @Test
    public void testLock_int() throws Exception {
        MultiLock sml = new MultiLock();
        sml.lock(21);
        for (int ix = 0; ix < 64; ix++) {
            if (ix == 21) {
                assertTrue(sml.isLocked(ix));
            } else {
                assertFalse(sml.isLocked(ix));
            }
        }
    }

    @Test
    public void testUnlock_int() throws InterruptedException {
        MultiLock sml = new MultiLock();
        sml.lock(21);
        assertTrue(sml.isLocked(21));
        assertTrue(sml.unlock(21));
        assertFalse(sml.isLocked(21));
    }

    @Test
    public void testLock_int_intArr() throws Exception {
        MultiLock sml = new MultiLock();
        sml.lock(21, 57);
        assertTrue(sml.isLocked(21));
        assertTrue(sml.isLocked(57));
        assertTrue(sml.unlock(21));
        assertFalse(sml.isLocked(21));
        assertTrue(sml.unlock(57));
        assertFalse(sml.isLocked(57));
    }

    @Test
    public void testNonOverlappingCanBeAcquiredConcurrently() throws Exception {
        AtomicInteger max = new AtomicInteger();
        AtomicInteger curr = new AtomicInteger();
        MultiLock lock = new MultiLock();
        CountDownLatch init = new CountDownLatch(1);
        CountDownLatch init2 = new CountDownLatch(1);
        int ct = 3;
        CountDownLatch xit = new CountDownLatch(ct * 2);
        Phaser phas = new Phaser(ct + 1);
        Phaser phas2 = new Phaser(ct + 1);
        Thread[] t = new Thread[ct];
        Thread[] t2 = new Thread[ct];
        Locker[] l = new Locker[ct];
        Locker[] l2 = new Locker[ct];
        for (int i = 0; i < ct; i++) {
            int offset = i * ct;
            l[i] = new Locker(xit, init, lock, curr, phas, max, offset, offset + 1, offset + 2);
            t[i] = new Thread(l[i], "ta-" + i);
            t[i].setDaemon(true);
            t[i].start();
            l2[i] = new Locker(xit, init2, lock, curr, phas2, max, offset, offset + 1, offset + 2);
            t2[i] = new Thread(l[i], "tb-" + i);
            t2[i].setDaemon(true);
            t2[i].start();
        }
        Thread.sleep(200);
        init.countDown();
        Thread.sleep(100);
        init2.countDown();
        Thread.sleep(200);
        phas.arriveAndDeregister();
        phas2.arriveAndDeregister();

        xit.await(1, TimeUnit.MINUTES);
        for (int i = 0; i < ct; i++) {
            t[i].join(2400);
            l[i].rethrow();
        }

        for (int i = 0; i < ct; i++) {
            t2[i].join(2400);
            l2[i].rethrow();
        }

        for (int i = 0; i < ct; i++) {
            if (t[i].isAlive()) {
                throw new Fake(t[i].getStackTrace());
            }
            if (t2[i].isAlive()) {
                throw new Fake(t2[i].getStackTrace());
            }
        }

        assertEquals(ct, max.get());
        assertEquals(0, curr.get());
    }

    static class Fake extends Exception {

        private final StackTraceElement[] el;

        Fake(StackTraceElement[] el) {
            this.el = el;
            setStackTrace(el);
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return el;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private class Locker implements Runnable {

        private final CountDownLatch exit;

        private final CountDownLatch init;
        private final MultiLock lock;
        private final Phaser phas;
        private final AtomicInteger maxConcurrent;
        private final AtomicInteger current;
        private final int first;
        private final int[] acquire;
        private volatile Exception thrown;
        private volatile long acquireTime;
        private volatile long entryTime;
        private volatile long exitTime;
        private volatile boolean didNotLock;

        public Locker(CountDownLatch exit, CountDownLatch init, MultiLock lock, AtomicInteger current, Phaser phas, AtomicInteger maxConcurrent, int first, int... acquire) {
            this.exit = exit;
            this.init = init;
            this.lock = lock;
            this.current = current;
            this.phas = phas;
            this.maxConcurrent = maxConcurrent;
            this.first = first;
            this.acquire = acquire;
        }

        void rethrow() {
            if (thrown != null) {
                Exceptions.chuck(thrown);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder().append(first);
            for (int i = 0; i < acquire.length; i++) {
                sb.append(',');
                sb.append(acquire[i]);
            }
            return sb.toString();
        }

        @Override
        public void run() {
            try {
                init.await(20, SECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            boolean locked = false;
            try {
                long then = System.currentTimeMillis();
                lock.lock(first, acquire);
                locked = true;
                long now = System.currentTimeMillis();
                entryTime = now;
                acquireTime = now - then;
                int curr = current.incrementAndGet();
                int mc = maxConcurrent.updateAndGet(old -> {
                    return Math.max(curr, old);
                });
                phas.arriveAndAwaitAdvance();
            } catch (Exception ex) {
                thrown = ex;
                ex.printStackTrace();
            } finally {
                if (locked) {
                    exitTime = System.currentTimeMillis();
                    current.decrementAndGet();
                    boolean unlocked = lock.unlock(first, acquire);
                } else {
                    didNotLock = true;
                }
                exit.countDown();
            }
        }
    }

    @Test
    public void testConcurrentAccessIsExclusive() throws Exception {
//        if (true) {
//            return;
//        }
        Thread.interrupted();
        try {
            Thread.sleep(30);
        } catch (InterruptedException ex) {

        }
        int threads = 8;
        Phaser ph = new Phaser(threads + 1);
        CountDownLatch entry = new CountDownLatch(threads);
        List<Thread> thread = new ArrayList<>(threads);
        List<Concurrent> concs = new ArrayList<>(threads);
        AtomicBoolean done = new AtomicBoolean();
        EntryTracker tracker = new EntryTracker(64);
        MultiLock ml = new MultiLock();
        Concurrency maxConcurrency = new Concurrency();
        for (int i = 0; i < threads; i++) {
            Concurrent c = new Concurrent(ml, ph, entry, done, tracker, maxConcurrency);
            Thread t = new Thread(c, "th-" + i);
            thread.add(t);
            concs.add(c);
            t.setDaemon(true);
            t.start();
        }
        Thread.interrupted();
        entry.await(1, TimeUnit.MINUTES);
        ph.arriveAndDeregister();
        Thread.sleep(1000);
        done.set(true);
        System.out.flush();
        for (Thread t : thread) {
            if (t.isAlive()) {
                t.interrupt();
            }
        }
        for (Thread t : thread) {
            t.join(10000);
        }

        int sum = 0;
        int loops = 0;
        long lockingTime = 0;
        long unlockingTime = 0;
        long[] iloops = new long[concs.size()];
        int cursor = 0;
        for (Concurrent c : concs) {
            sum += c.iterations;
            loops += c.loops;
            lockingTime += c.lockingTime.get();
            iloops[cursor++] = c.loops;
            unlockingTime += c.unlockingTime.get();
        }
        assertTrue(sum > iloops.length * 100);
        for (int i = 0; i < iloops.length; i++) {
            assertTrue(iloops[i] > 10, "Less than 10 loops on " + i);
        }
    }

    static class Concurrent implements Runnable {

        static int ix = 0;
        private final int index = (++ix * 9271031);
        private final Random rnd = new Random(index);
        private final MultiLock ml;
        private final Phaser ph;
        private final CountDownLatch entry;
        private final AtomicBoolean done;
        private volatile int iterations = 0;
        private volatile int loops = 0;
        private final EntryTracker tracker;
        private final Concurrency maxConcurrency;
        private final AtomicLong lockingTime = new AtomicLong();
        private final AtomicLong unlockingTime = new AtomicLong();

        Concurrent(MultiLock ml, Phaser ph, CountDownLatch entry,
                AtomicBoolean done, EntryTracker tracker, Concurrency maxConcurrency) {
            this.ml = ml;
            this.ph = ph;
            this.entry = entry;
            this.done = done;
            this.tracker = tracker;
            this.maxConcurrency = maxConcurrency;
        }

        @Override
        public void run() {
            try {
                entry.countDown();
                ph.arriveAndAwaitAdvance();
                while (!done.get()) {
                    loops++;
                    int ix1 = rnd.nextInt(64);
                    int ix2 = (ix1 + 1) % 64;
                    boolean keepGoing;
                    try {
                        long then = System.nanoTime();
                        ml.lock(ix1, ix2);
                        long elapsed = System.nanoTime() - then;
                        lockingTime.getAndAdd(elapsed);
                        keepGoing = tracker.run(ix1, () -> {
                            return tracker.run(ix2, () -> {
                                try {
                                    maxConcurrency.run(() -> {
                                        iterations++;
                                        LockSupport.parkNanos(1);
                                    });
                                    return true;
                                } finally {
                                    long before = System.nanoTime();
                                    ml.unlock(ix1, ix2);
                                    long after = System.nanoTime() - before;
                                    unlockingTime.getAndAdd(after);
                                }
                            });
                        });
                    } catch (Exception ex) {
                        if (done.get()) {
                            return;
                        }
                        Logger.getLogger(MultiLockTest.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                    if (!keepGoing) {
                        break;
                    }
                }
            } finally {
                System.out.println("Exit " + Thread.currentThread());
            }
        }
    }

    static class Concurrency {

        private final AtomicInteger current = new AtomicInteger();
        private final AtomicInteger concur = new AtomicInteger();

        void run(Runnable run) {
            int now = current.incrementAndGet();
            try {
                concur.getAndUpdate(old -> {
                    return Math.max(old, now);
                });
                run.run();
            } finally {
                current.decrementAndGet();
            }
        }

        int get() {
            return concur.get();
        }
    }
}
