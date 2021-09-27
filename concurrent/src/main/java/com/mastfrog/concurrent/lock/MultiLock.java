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
package com.mastfrog.concurrent.lock;

import com.mastfrog.function.throwing.ThrowingRunnable;
import static java.lang.Long.bitCount;
import static java.lang.Long.lowestOneBit;
import static java.lang.Long.numberOfTrailingZeros;
import java.util.BitSet;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntPredicate;

/**
 * A (mostly) non-reentrant 64-slot lock where individual slots or multiple
 * slots may be atomically locked, blocking until all are available, and
 * supporting semaphore-like operation where a set of <code>n</code> arbitrary
 * slots may be locked, biasing the choice to reduce contention is possible.
 * <p>
 * This is the chainsaw-with-no-guard of locking. But when you have the problem
 * it solves, it is powerful, fast and efficient.
 * </p>
 * <h3>Intended Use-Cases</h3>
 * <ul>
 * <li>Memory-manager-like applications where ranges of storage must be able to
 * be locked without serializing all work within it</li>
 * <li>Migrating code from coarse-grained locking to finer-grained locking by
 * creating individual locks for those fields which were formerly all covered by
 * a single lock, and allowing code that accesses them to lock multiple of them
 * atomically</li>
 * </ul><p>
 * It is not just possible, but <i>easy</i> to deadlock without considerable
 * discipline in using this class - just call
 * <code>lock.lock(1); lock.lock(1);</code> and a thread can deadlock on itself
 * (<code>lockReentrantly()</code> provides a simple way to avoid that
 * particular problem, using a <code>ThreadLocal</code> to avoid locking any
 * slots that were already locked by that thread).
 * </p><p>
 * But locking operations either succeed, block until they do, or fail, to lock
 * all requested slots in a single atomic operation.
 * </p>
 *
 * @author Tim Boudreau
 */
final class MultiLock implements SlottedLock {

    /**
     * The internal state.
     */
    private final Sync sync = new Sync();
    /**
     * Used by lockReentrantly() to avoid re-locking already acquired slots.
     */
    private final ThreadLocal<Long> tl = ThreadLocal.withInitial(() -> 0L);

    /**
     * Lock a specific slot in the lock, blocking if necessary until it is
     * unlocked if it is currently locked.
     *
     * @param index The index, from 0..63 inclusive
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    @Override
    public void lock(int index) throws InterruptedException {
        lockBits(1L << validate64(index));
    }

    /**
     * Unlock a specific slot in the lock, if it is locked. Note that due to the
     * nature of this lock, it is possible for a thread that did not lock the
     * slot to unlock it - in general, that is to be avoided unless it is really
     * what you want.
     *
     * @param index The slot to unlock
     * @return True if the slot was unlocked
     */
    @Override
    public boolean unlock(int index) {
        return unlockBits(1L << validate64(index));
    }

    /**
     * Lock multiple slots in the lock, blocking if necessary until all can be
     * atomically acquired.
     *
     * @param first The first slot
     * @param more Some additional number of slots
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    @Override
    public void lock(int first, int... more) throws InterruptedException {
        lockBits(toBits(first, more));
    }

    /**
     * Lock multiple slots in the lock, blocking if necessary until all can be
     * atomically acquired.
     *
     * @param slots Slots to lock
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    @Override
    public void lock(BitSet slots) throws InterruptedException {
        lockBits(toBits(slots));
    }

    /**
     * Unlock multiple slots in the lock. Note that due to the nature of this
     * lock, it is possible for a thread that did not lock the slot to unlock it
     * - in general, that is to be avoided unless it is really what you want.
     *
     * @param first The first slot
     * @param more additional slots
     * @return true if any slots were unlocked
     */
    @Override
    public boolean unlock(int first, int... more) {
        return unlockBits(toBits(first, more));
    }

    /**
     * Unlock multiple slots in the lock. Note that due to the nature of this
     * lock, it is possible for a thread that did not lock the slot to unlock it
     * - in general, that is to be avoided unless it is really what you want.
     *
     * @param slots Slots to lock
     * @return true if any slots were unlocked
     */
    @Override
    public boolean unlock(BitSet slots) {
        return unlockBits(toBits(slots));
    }

    /**
     * Lock an arbitrary set of <code>n</code> slots in the lock, without
     * specifying which ones - this is useful in memory-manager like structures
     * where you want use of some number of available resources, and the only
     * thing that matters is that nothing else is using them.
     * <p>
     * This call is non-blocking and will return an empty iterator if
     * insufficient unlocked slots are available.
     *
     * @param n The number of slots to lock
     * @return An iterator over those slots that were acquired - if empty,
     * insufficient slots were available
     */
    @Override
    public PrimitiveIterator.OfInt acquireN(int n) {
        return acquireN(n, Favor.BOTH);
    }

    /**
     * Lock an arbitrary set of <code>n</code> slots in the lock, without
     * specifying which ones - this is useful in memory-manager like structures
     * where you want use of some number of available resources, and the only
     * thing that matters is that nothing else is using them.
     * <p>
     * This call is non-blocking and will return an empty iterator if
     * insufficient unlocked slots are available.
     *
     * @param n The number of slots to lock
     * @param favoring A bias toward favoring high or low numbered slots, or
     * both alternating - if contending with something else which is also using
     * the lock, it can be useful to bias one toward the high bits and one
     * toward the low bits so that there will only be contention if nearly all
     * slots are in use
     * @return An iterator over those slots that were acquired - if empty,
     * insufficient slots were available
     */
    @Override
    public PrimitiveIterator.OfInt acquireN(int n, Favor favoring) {
        if (n <= 0 || n > 64) {
            throw new IllegalArgumentException("N must be between 1 and 64: " + n);
        }
        long result = sync.acquire(n, favoring);
        return new BitsIter(result);
    }

    /**
     * Lock an arbitrary set of <code>n</code> slots in the lock, without
     * specifying which ones - this is useful in memory-manager like structures
     * where you want use of some number of available resources, and the only
     * thing that matters is that nothing else is using them.
     * <p>
     * This call is non-blocking and will return an empty iterator if
     * insufficient unlocked slots are available.
     *
     * @param n The number of slots to lock
     * @param favoring A bias toward favoring high or low numbered slots, or
     * both alternating - if contending with something else which is also using
     * the lock, it can be useful to bias one toward the high bits and one
     * toward the low bits so that there will only be contention if nearly all
     * slots are in use
     * @return An iterator over those slots that were acquired - if empty,
     * insufficient slots were available
     */
    @Override
    public PrimitiveIterator.OfInt acquireN(int n, Favor favoring, IntPredicate excluder) {
        if (n <= 0 || n > 64) {
            throw new IllegalArgumentException("N must be between 1 and 64: " + n);
        }
        long result = sync.acquire(n, favoring, excluder);
        return new BitsIter(result);
    }

    /**
     * Determine if some slots are currently locked; note that the return value
     * is at best <i>advice</i> - there is no guarantee that those slots will
     * still be unlocked on any future attempt at locking them.
     *
     * @param first the first slot
     * @param more additional slots to test
     * @return true if <i>all</i> of the mentioned slots are locked
     */
    @Override
    public boolean isLocked(int first, int... more) {
        return sync.anyLocked(toBits(first, more));
    }

    /**
     * Determine if some slots are currently locked; note that the return value
     * is at best <i>advice</i> - there is no guarantee that those slots will
     * still be unlocked on any future attempt at locking them.
     *
     * @param first the first slot
     * @param more additional slots to test
     * @return true if <i>all</i> of the mentioned slots are locked; returns
     * false if the passed bit set is empty
     */
    @Override
    public boolean isLocked(BitSet slots) {
        if (slots.isEmpty()) {
            return false;
        }
        return sync.anyLocked(toBits(slots));
    }

    /**
     * Determine if <i>one</i> slots are currently locked; note that the return
     * value is at best <i>advice</i> - there is no guarantee that those slots
     * will still be unlocked on any future attempt at locking them.
     *
     * @param first the first slot
     * @return true if <i>all</i> of the mentioned slots are locked
     */
    @Override
    public boolean isLocked(int ix) {
        return sync.anyLocked(1L << validate64(ix));
    }

    /**
     * Determine if <i>any</i> slots are locked.
     *
     * @return true if any slot is locked at the time of this call
     */
    @Override
    public boolean isLocked() {
        return sync.state() != 0;
    }

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     */
    @Override
    public void lockingThrowing(ThrowingRunnable run, int first, int... more) throws Exception {
        lock(first, more);
        try {
            run.run();
        } finally {
            unlock(first, more);
        }
    }

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     */
    @Override
    public void lockingThrowing(ThrowingRunnable run, BitSet slots) throws Exception {
        if (slots.isEmpty()) {
            run.run();
            return;
        }
        lock(slots);
        try {
            run.run();
        } finally {
            unlock(slots);
        }
    }

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them. The passed
     * runnable may <i>reentrantly</i> call this method, even with overlapping
     * slot values, without deadlocking, and only those slots not already locked
     * will be added to the set of locked slots, and unlocked once the runnable
     * has completed.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     * waiting to lock
     */
    @Override
    public void lockingReentrantlyThrowing(ThrowingRunnable run, int first, int... more) throws Exception {
        long val = tl.get();
        long nue = toBits(first, more);
        long masked = nue & ~val;
        try {
            tl.set(nue);
            lockBits(masked);
            run.run();
        } finally {
            unlockBits(masked);
            tl.set(val);
        }
    }

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them. The passed
     * runnable may <i>reentrantly</i> call this method, even with overlapping
     * slot values, without deadlocking, and only those slots not already locked
     * will be added to the set of locked slots, and unlocked once the runnable
     * has completed.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     * waiting to lock
     */
    @Override
    public void lockingReentrantlyThrowing(ThrowingRunnable run, BitSet bits) throws Exception {
        long val = tl.get();
        long nue = toBits(bits);
        long masked = nue & ~val;
        try {
            tl.set(nue);
            lockBits(masked);
            run.run();
        } finally {
            unlockBits(masked);
            tl.set(val);
        }
    }

    /**
     * Get the number of slots currently available.
     *
     * @return A number of slots
     */
    @Override
    public int available() {
        return sync.available();
    }

    /**
     * Get the number of slots currently locked.
     *
     * @return A number of slots
     */
    @Override
    public int inUse() {
        return sync.inUse();
    }

    private long toBits(int first, int... more) {
        long result = 1L << validate64(first);
        for (int i = 0; i < more.length; i++) {
            result |= 1L << validate64(more[i]);
        }
        return result;
    }

    private long toBits(BitSet bits) {
        long result = 0;
        for (int bit = bits.nextSetBit(0); bit >= 0; bit = bits.nextSetBit(bit + 1)) {
            validate64(bit);
            result |= 1L << bit;
        }
        return result;
    }

    private int validate64(int val) {
        if (val < 0 || val >= 64) {
            throw new IllegalArgumentException("Out of range 0-63 inclusive: " + val);
        }
        return val;
    }

    private void lockBits(long toLock) throws InterruptedException {
        sync.acquireSharedInterruptibly(toLock);
    }

    private boolean unlockBits(long toUnlock) {
        return sync.releaseShared(toUnlock);
    }

    long state() {
        return sync.state();
    }

    @Override
    public String toString() {
        return sync.toString();
    }

    /**
     * Terminate this lock, interrupting any queued threads and forcibly
     * acquiring all locks so no new threads can enter it. Note this will not
     * stop subsequent calls to <code>lock()</code> from blocking.
     *
     * @return The number of threads interrupted
     */
    @Override
    public int terminate() {
        int result = sync.terminate();
        return result;
    }

    /**
     * The internal atomic state.
     */
    private static class Sync extends AbstractQueuedLongSynchronizer {

        @Override
        public String toString() {
            return MultiLock.toString(getState());
        }

        int terminate() {
            int ct = 0;
            Set<Thread> seen = new HashSet<>();
            for (Thread t : super.getQueuedThreads()) {
                if (LockSupport.getBlocker(t) == this) {
                    seen.add(t);
                    t.interrupt();
                    ct++;
                }
            }
            setState(-1L);
            for (Thread t : super.getQueuedThreads()) {
                if (!seen.contains(t) && LockSupport.getBlocker(t) == this) {
                    t.interrupt();
                    ct++;
                }
            }
            return ct;
        }

        long state() {
            return getState();
        }

        int inUse() {
            return bitCount(getState());
        }

        int available() {
            return 64 - inUse();
        }

        boolean anyLocked(long val) {
            return (val & getState()) != 0;
        }

        long acquire(int n, Favor favor) {
            long state = getState();
            long compl = ~state;
            long availBits = bitCount(compl);
            if (availBits < n) {
                return 0;
            }
            while (availBits >= n) {
                long temp = compl;
                long test = 0;
                long leastZeroBit = favor.favor(temp, 0);
                test |= leastZeroBit;
                for (int i = 1; i < n; i++) {
                    temp &= ~leastZeroBit;
                    leastZeroBit = favor.favor(temp, i);
                    test |= leastZeroBit;
                }
                boolean enough = bitCount(test) == n;
                if (enough && compareAndSetState(state, state | test)) {
                    return test;
                } else if (enough) {
                    state = getState();
                    compl = ~state;
                    availBits = bitCount(compl);
                    continue;
                }
                availBits--;
                compl &= ~leastZeroBit;
            }
            return 0L;
        }

        int firstSetBit(long val) {
            int result = numberOfTrailingZeros(val);
            if (result == 64) {
                return -1;
            }
            return result;
        }

        long acquire(int n, Favor favor, IntPredicate tester) {
            // Acquire using the predicate to exclude some bits.
            // Get an initial copy of the state for CAS
            long state = getState();
            long compl = ~state;
            long availBits = bitCount(compl);
            if (availBits < n) {
                // Won't fit - bail out
                return 0L;
            }
            while (availBits >= n) {
                long temp = compl;
                long provisionalResult = 0;
                long leastZeroBit = favor.favor(temp, 0);
                // The first bit may be rejected by the predicate, so loop
                // until we have an initial bit
                while (!tester.test(firstSetBit(leastZeroBit))) {
                    compl &= ~leastZeroBit;
                    temp = compl;
                    availBits--;
                    if (bitCount(compl) < n) {
                        return 0L;
                    }
                    leastZeroBit = favor.favor(temp, 0);
                }
                // Merge it into the potential result
                provisionalResult |= leastZeroBit;
                // Now gather enough bits to meet the requested count
                for (int i = 1; i < n; i++) {
                    // Mask away the last bit we accepted so favor() will not find
                    // it twice
                    temp &= ~leastZeroBit;
                    // Find the next bit to potentially include
                    leastZeroBit = favor.favor(temp, i);
                    // Allow the predicate to veto it
                    if (!tester.test(numberOfTrailingZeros(leastZeroBit))) {
                        // Vetoed - mask it away
                        temp &= ~leastZeroBit;
                        // Make the next masking a no-op
                        leastZeroBit = 0;
                        if (bitCount(temp) + bitCount(provisionalResult) < n) {
                            // There are not enough bits left to possibly have
                            // a result - give up early
                            return 0L;
                        }
                        i--;
                        continue;
                    }
                    provisionalResult |= leastZeroBit;
                }
                // See if we may be done
                boolean enough = bitCount(provisionalResult) == n;
                // If the state has changed since we retrieved the value, then we must
                // throw the computed result away and retry
                if (enough && compareAndSetState(state, state | provisionalResult)) {
                    return provisionalResult;
                } else if (enough) {
                    // If we could not set the state, refresh our local copy and
                    // start over, preserving nothing
                    state = getState();
                    compl = ~state;
                    availBits = bitCount(compl);
                    continue;
                }
                availBits--;
                compl &= ~leastZeroBit;
            }
            return 0L;
        }

        @Override
        protected long tryAcquireShared(long arg) {
            long old = getState();
            if ((old & arg) != 0) {
                return -1;
            }
            boolean result = compareAndSetState(old, old | arg);
            return result ? 0 : -1;
        }

        @Override
        protected boolean tryReleaseShared(long arg) {
            boolean result;
            long st;
            do {
                st = getState();
                if ((st & arg) == 0) {
                    return false;
                }
                result = compareAndSetState(st, st & ~arg);
            } while (!result);
            return result;
        }

        @Override
        protected boolean isHeldExclusively() {
            return false;
        }
    }

    static String toString(long st) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            long mask = 1L << i;
            if ((st & mask) != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i);
            }
        }
        if (sb.length() == 0) {
            sb.append("-empty-");
        }
        return sb.toString();
    }

    private static class BitsIter implements PrimitiveIterator.OfInt {

        private long value;

        BitsIter(long value) {
            this.value = value;
        }

        public String toString() {
            return MultiLock.toString(value);
        }

        @Override
        public int nextInt() {
            if (value == 0) {
                throw new NoSuchElementException();
            }
            long val = lowestOneBit(value);
            value &= ~val;
            return numberOfTrailingZeros(val);
        }

        @Override
        public boolean hasNext() {
            return value != 0L;
        }
    }
}
