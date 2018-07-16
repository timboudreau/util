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
package com.mastfrog.util.function;

import com.mastfrog.util.thread.ThreadLocalBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;
import com.mastfrog.util.thread.QuietAutoCloseable;

/**
 * A wrapper around a ReentrantReadWriteLock that exposes operations on it as
 * AutoCloseable and functional operations.
 *
 * @author Tim Boudreau
 */
public class FunctionalLock {

    private final ReentrantReadWriteLock lock;
    private final ReadLock readLock;
    private final WriteLock writeLock;
    private final ThreadLocalBoolean inReadAccess = new ThreadLocalBoolean();

    public FunctionalLock() {
        this(false);
    }

    public FunctionalLock(boolean fair) {
        lock = new ReentrantReadWriteLock(fair);
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public final boolean isFair() {
        return lock.isFair();
    }

    public int getReadLockCount() {
        return lock.getReadLockCount();
    }

    public boolean isWriteLocked() {
        return lock.isWriteLocked();
    }

    public boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
    }

    public int getWriteHoldCount() {
        return lock.getWriteHoldCount();
    }

    public int getReadHoldCount() {
        return lock.getReadHoldCount();
    }

    public final boolean hasQueuedThreads() {
        return lock.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return lock.hasQueuedThread(thread);
    }

    public final int getQueueLength() {
        return lock.getQueueLength();
    }

    public boolean hasWaiters(Condition condition) {
        return lock.hasWaiters(condition);
    }

    public int getWaitQueueLength(Condition condition) {
        return lock.getWaitQueueLength(condition);
    }

    @Override
    public String toString() {
        return lock.toString();
    }

    public boolean isReadAccessOnCurrentThread() {
        return inReadAccess.get();
    }

    public Condition newReadLockCondition() {
        return readLock.newCondition();
    }

    public Condition newWriteLockCondition() {
        return writeLock.newCondition();
    }

    public void underReadLock(ThrowingRunnable r) throws Exception {
        readLock.lock();
        inReadAccess.toggleDuring(() -> {
            try {
                r.run();
            } finally {
                readLock.unlock();
            }
        });
        Thread.currentThread().getId();
    }

    private void checkLockOrderForWriteAccess() {
        if (inReadAccess.get() && lock.getReadLockCount() > 0) {
            throw new IllegalThreadStateException("Read and write locks acquired out of "
                    + "order - going from read to write will deadlock");
        }
    }

    public void underWriteLock(ThrowingRunnable r) throws Exception {
        checkLockOrderForWriteAccess();
        writeLock.lock();
        try {
            r.run();
        } finally {
            writeLock.unlock();
        }
    }

    public void runUnderReadLock(Runnable r) {
        readLock.lock();
        inReadAccess.toggle(() -> {
            try {
                r.run();
            } finally {
                readLock.unlock();
            }
        });
    }

    public void runUnderWriteLock(Runnable r) {
        checkLockOrderForWriteAccess();
        writeLock.lock();
        try {
            r.run();
        } finally {
            writeLock.unlock();
        }
    }

    public <T> T supplyUnderReadLock(ThrowingSupplier<T> r) throws Exception {
        readLock.lock();
        return inReadAccess.toggleAndGetOrThrow(() -> {
            try {
                return r.get();
            } finally {
                readLock.unlock();
            }
        });
    }

    public <T> T supplyUnderWriteLock(ThrowingSupplier<T> r) throws Exception {
        checkLockOrderForWriteAccess();
        writeLock.lock();
        try {
            return r.get();
        } finally {
            writeLock.unlock();
        }
    }

    public <T> T getUnderReadLock(Supplier<T> r) {
        readLock.lock();
        return inReadAccess.toggleAndGet(() -> {
            try {
                return r.get();
            } finally {
                readLock.unlock();
            }
        });
    }

    public <T> T getUnderWriteLock(Supplier<T> r) {
        checkLockOrderForWriteAccess();
        writeLock.lock();
        try {
            return r.get();
        } finally {
            writeLock.unlock();
        }
    }

    public QuietAutoCloseable withReadLock() {
        boolean old = inReadAccess.get();
        inReadAccess.set(true);
        readLock.lock();
        return () -> {
            readLock.unlock();
            inReadAccess.set(old);
        };
    }

    public QuietAutoCloseable withWriteLock() {
        checkLockOrderForWriteAccess();
        writeLock.lock();
        return () -> {
            writeLock.unlock();
        };
    }

// Interruptible versions
    public void underReadLockInterruptibly(ThrowingRunnable r) throws Exception {
        readLock.lockInterruptibly();
        inReadAccess.toggleDuring(() -> {
            try {
                r.run();
            } finally {
                readLock.unlock();
            }
        });
        Thread.currentThread().getId();
    }

    public void underWriteLockInterruptibly(ThrowingRunnable r) throws Exception {
        checkLockOrderForWriteAccess();
        writeLock.lockInterruptibly();
        try {
            r.run();
        } finally {
            writeLock.unlock();
        }
    }

    public void runUnderReadLockInterruptibly(Runnable r) throws InterruptedException {
        readLock.lockInterruptibly();
        inReadAccess.toggle(() -> {
            try {
                r.run();
            } finally {
                readLock.unlock();
            }
        });
    }

    public void runUnderWriteLockInterruptibly(Runnable r) throws InterruptedException {
        checkLockOrderForWriteAccess();
        writeLock.lockInterruptibly();
        try {
            r.run();
        } finally {
            writeLock.unlock();
        }
    }

    public <T> T supplyUnderReadLockInterruptibly(ThrowingSupplier<T> r) throws Exception {
        readLock.lockInterruptibly();
        return inReadAccess.toggleAndGetOrThrow(() -> {
            try {
                return r.get();
            } finally {
                readLock.unlock();
            }
        });
    }

    public <T> T supplyUnderWriteLockInterruptibly(ThrowingSupplier<T> r) throws Exception {
        checkLockOrderForWriteAccess();
        writeLock.lockInterruptibly();
        try {
            return r.get();
        } finally {
            writeLock.unlock();
        }
    }

    public <T> T getUnderReadLockInterruptibly(Supplier<T> r) throws InterruptedException {
        readLock.lockInterruptibly();
        return inReadAccess.toggleAndGet(() -> {
            try {
                return r.get();
            } finally {
                readLock.unlock();
            }
        });
    }

    public <T> T getUnderWriteLockInterruptibly(Supplier<T> r) throws InterruptedException {
        checkLockOrderForWriteAccess();
        writeLock.lockInterruptibly();
        try {
            return r.get();
        } finally {
            writeLock.unlock();
        }
    }

    public QuietAutoCloseable withReadLockInterruptibly() throws InterruptedException {
        boolean old = inReadAccess.get();
        inReadAccess.set(true);
        readLock.lockInterruptibly();
        return () -> {
            readLock.unlock();
            inReadAccess.set(old);
        };
    }

    public QuietAutoCloseable withWriteLockInterruptibly() throws InterruptedException {
        checkLockOrderForWriteAccess();
        writeLock.lockInterruptibly();
        return () -> {
            writeLock.unlock();
        };
    }
}
