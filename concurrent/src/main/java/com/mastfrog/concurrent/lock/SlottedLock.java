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
import java.util.BitSet;
import java.util.PrimitiveIterator;
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
public interface SlottedLock extends MultiplyLockable {

    public static SlottedLock create() {
        return new MultiLock();
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
    PrimitiveIterator.OfInt acquireN(int n);

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
    PrimitiveIterator.OfInt acquireN(int n, Favor favoring);

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
     * @param excluder Secondary test for slots to be included in the returned
     * set of slots - only slots this predicate returns true for will actually
     * be used
     * @return An iterator over those slots that were acquired - if empty,
     * insufficient slots were available
     */
    PrimitiveIterator.OfInt acquireN(int n, Favor favoring, IntPredicate excluder);
    /**
     * Get the number of slots currently available.
     *
     * @return A number of slots
     */
    int available();

    /**
     * Get the number of slots currently locked.
     *
     * @return A number of slots
     */
    int inUse();

    /**
     * Determine if some slots are currently locked; note that the return value
     * is at best <i>advice</i> - there is no guarantee that those slots will
     * still be unlocked on any future attempt at locking them.
     *
     * @param first the first slot
     * @param more additional slots to test
     * @return true if <i>all</i> of the mentioned slots are locked
     */
    boolean isLocked(int first, int... more);

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
    boolean isLocked(BitSet slots);

    /**
     * Determine if <i>one</i> slots are currently locked; note that the return
     * value is at best <i>advice</i> - there is no guarantee that those slots
     * will still be unlocked on any future attempt at locking them.
     *
     * @param first the first slot
     * @return true if <i>all</i> of the mentioned slots are locked
     */
    boolean isLocked(int ix);

    /**
     * Determine if <i>any</i> slots are locked.
     *
     * @return true if any slot is locked at the time of this call
     */
    boolean isLocked();

    /**
     * Lock a specific slot in the lock, blocking if necessary until it is
     * unlocked if it is currently locked.
     *
     * @param index The index, from 0..63 inclusive
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    void lock(int index) throws InterruptedException;

    /**
     * Lock multiple slots in the lock, blocking if necessary until all can be
     * atomically acquired.
     *
     * @param first The first slot
     * @param more Some additional number of slots
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    void lock(int first, int... more) throws InterruptedException;

    /**
     * Lock multiple slots in the lock, blocking if necessary until all can be
     * atomically acquired.
     *
     * @param slots Slots to lock
     * @throws InterruptedException If the thread is interrupted while waiting
     */
    void lock(BitSet slots) throws InterruptedException;

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     */
//    void locking(Runnable run, int first, int... more) throws InterruptedException;

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them.
     *
     * @param run A runnable
     * @param first The first slot to lock
     * @param more Additional slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     */
//    void locking(Runnable run, BitSet slots) throws InterruptedException;

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
    void lockingReentrantlyThrowing(ThrowingRunnable run, int first, int... more) throws Exception;

    /**
     * Lock some slots, blocking interruptibly if necessary until they become
     * available, then run the passed runable and unlock them. The passed
     * runnable may <i>reentrantly</i> call this method, even with overlapping
     * slot values, without deadlocking, and only those slots not already locked
     * will be added to the set of locked slots, and unlocked once the runnable
     * has completed.
     *
     * @param run A runnable
     * @param slots Some slots to lock
     * @throws InterruptedException if the thread is interrupted while blocked
     * waiting to lock
     */
    @Override
    void lockingReentrantlyThrowing(ThrowingRunnable run, BitSet slots) throws Exception;

    /**
     * Terminate this lock, interrupting any queued threads and forcibly
     * acquiring all locks so no new threads can enter it. Note this will not
     * stop subsequent calls to <code>lock()</code> from blocking.
     *
     * @return The number of threads interrupted
     */
    int terminate();

    /**
     * Unlock a specific slot in the lock, if it is locked. Note that due to the
     * nature of this lock, it is possible for a thread that did not lock the
     * slot to unlock it - in general, that is to be avoided unless it is really
     * what you want.
     *
     * @param index The slot to unlock
     * @return True if the slot was unlocked
     */
    boolean unlock(int index);

    /**
     * Unlock multiple slots in the lock. Note that due to the nature of this
     * lock, it is possible for a thread that did not lock the slot to unlock it
     * - in general, that is to be avoided unless it is really what you want.
     *
     * @param first The first slot
     * @param more additional slots
     * @return true if any slots were unlocked
     */
    boolean unlock(int first, int... more);

    /**
     * Unlock multiple slots in the lock. Note that due to the nature of this
     * lock, it is possible for a thread that did not lock the slot to unlock it
     * - in general, that is to be avoided unless it is really what you want.
     *
     * @param slots Slots to lock
     * @return true if any slots were unlocked
     */
    boolean unlock(BitSet slots);

}
