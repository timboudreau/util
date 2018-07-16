/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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

import java.io.IOException;

/**
 * Restricts all access to another object to single-threaded access, for
 * imposing thread-safety over thread-unsafe objects.
 * <p/>
 * Assuming the object passed to the constructor is not referenced anywhere
 * else, and minimal discipline is maintained to obtain a reference to the
 * object and pass it outside the barrier, this class will guarantee that the
 * object passed to its constructor is only accessed while holding a lock.
 *
 * @author Tim Boudreau
 * @deprecated Use one of the functional locks instead
 */
@Deprecated
public final class Barrier<T> {
    private final Object lock;
    private final T object;
    private String name;

    /**
     * Create a new barrier, passing in the lock object (for use constructing a
     * shared lock between multiple barriers)
     *
     * @param object The object to hide behind this barrier
     * @param lock The object to use as a lock
     */
    public Barrier(T object, Object lock) {
        this.object = object;
        this.lock = lock;
    }

    /**
     * Create a new barrier
     *
     * @param object The object to hide behind this barrier
     */
    public Barrier(T object) {
        this(object, new Object());
    }

    public boolean isInBarrier() {
        return Thread.holdsLock(lock);
    }

    public void checkAccess() {
        if (!isInBarrier()) {
            throw new IllegalThreadStateException("Access " + this + "outside of lock");
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        if (name == null) {
            return super.toString();
        }
        return super.toString() + ":" + name;
    }

    public <T> void access(T obj, Receiver<T> receiver) {
        synchronized (lock) {
            receiver.receive(obj);
        }
    }

    /**
     * Access the object hidden behind this barrier, using the passed
     * ResultAccessor, which can return a result.
     *
     * @param <R> The result type
     * @param accessor An accessor
     * @return A result
     */
    public <R> R access(ResultAccessor<? super T, R> accessor) {
        synchronized (lock) {
            R result = accessor.withObject(object);
            //minimal sanity check
            if (result == object) {
                throw new IllegalStateException("Attempt to escape barrier " + this + " by " + accessor);
            }
            return result;
        }
    }

    /**
     * Access the object hidden behind this barrier, using the passed
     * ResultAccessor, which can return a result.
     *
     * @param <R> The result type
     * @param accessor An accessor
     * @return A result
     */
    public <R> R access(ExceptionAccessor<? super T, R> accessor) throws IOException {
        synchronized (lock) {
            R result = accessor.withObject(object);
            //minimal sanity check
            if (result == object) {
                throw new IllegalStateException("Attempt to escape barrier by " + accessor);
            }
            return result;
        }
    }

    /**
     * Access the object hidden behind this barrier using the passed accessor.
     *
     * @param accessor A callback which will be passed the object hidden behind
     * this barrier
     */
    public void access(Accessor<? super T> accessor) {
        synchronized (lock) {
            accessor.withObject(object);
        }
    }

    /**
     * Convenience accessor class for @link{com.dv.util.Barrier}, which returns
     * a result.
     *
     * @param <T> The object type
     * @param <R> The result type
     * @see Barrier
     * @see Accessor
     */
    @FunctionalInterface
    public interface ResultAccessor<T, R> {
        public R withObject(T object);
    }

    /**
     * Callback which is passed the object hidden behind this barrier while
     * holding its lock.
     *
     * @param <T> The object type
     * @see Barrier
     * @see ResultAccessor
     */
    @FunctionalInterface
    public interface Accessor<T> {
        public void withObject(T object);
    }

    @FunctionalInterface
    public interface ExceptionAccessor<T, R> {
        public R withObject(T object) throws IOException;
    }
}
