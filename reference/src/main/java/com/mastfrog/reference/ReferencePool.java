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
package com.mastfrog.reference;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An object pool that can use phantom references to return objects to the pool
 * for reuse after once an "owner" object has been garbage collected; also
 * allows explict removal and return of objects.
 * <p>
 * If an object is returned twice but only borrowed once, bad things may happen;
 * use the method that takes a consumer, which ensures that won't happen, where
 * possible, and ensure the object does not escape its closure).
 * </p><p>
 * Note - to get some debug logging out of the pool, set the system property
 * <code>phantom.reference.pool.debug</code> to true.
 * </p>
 *
 * @author Tim Boudreau
 */
public interface ReferencePool<T> {

    /**
     * System property, which, if set to true, will cause PhantomReferencePool
     * to emit some debug output about its contents.
     */
    public static final String SYS_PROP_PHANTOM_REFERENCE_POOL_DEBUG
            = "phantom.reference.pool.debug";

    /**
     * Borrow an object from the pool, passing it to the consumer and returning
     * it to the pool as soon as the consumer has completed. The consumer
     * <i>must not</i> retain a reference to the pooled object or pass it to
     * other code that might.
     *
     * @param consumer A consumer
     */
    void borrow(Consumer<T> consumer);

    /**
     * Take an object from the pool for use by an object which has not yet been
     * constructed; the BiFunction will be called back with the pooled object
     * and a consumer to accept the owning object once it is contructed, and
     * return whatever the function returns (typically the constructed object).
     * This is necessary for cases where the object whose garbage collection
     * should be tracked cannot be constructed without passing in the pooled
     * object.
     * <p>
     * Note that failing to call the passed consumer permanently removes the
     * object passed to the BiConsumer from the pool.
     * </p>
     *
     * @param <R> The type of object the function will return
     * @param c A BiFunction which will be passed the pooled object and a
     * consumer to associate it with the lifecycle of the object passed to it.
     * @return The return value of the passed function.
     */
    <R> R lazyTakeFromPool(BiFunction<T, Consumer<Object>, R> c);

    /**
     * Return an object to the pool which was take using the no-argument
     * <code>takeFromPool()</code> method.
     *
     * @throws AssertionError if the object passed here is already associated
     * with some object's lifecycle, as that could cause a pooled item to be in
     * use by more than one object concurrently.
     * @param obj The object to return to the pool
     */
    void returnToPool(T obj);

    /**
     * Take an object from the pool, <b>not associating it with any object's
     * lifecycle</b> because you will return it by explicitly calling
     * returnToPool with it when done with it. This allows quick temporary use
     * of an object from the pool inside a try/finally block. Failing to call
     * returnToPool() with the object returned here means that object is
     * permanently lost to the pool.
     *
     * @return The object
     */
    T takeFromPool();

    /**
     * Take an item from the pool, associating it with the passed object, and
     * returning it to the pool only if and when that object is garbage
     * collected.
     *
     * @param owner The owner object
     * @return An item from the pool if one is available, or a newly created one
     * if not
     */
    T takeFromPool(Object owner);

    /**
     * Create a new pool. The cleanup thread's priority will be
     * <code>Thread.NORM_PRIORITY - 1</code>.
     *
     * @param <T> The type
     * @param poolName The name of the pool (used for logging and by the
     * reclamation thread)
     * @param maxSize The maximum pool size (if less than or equal to zero, a
     * no-op pool that always calls the constructor is created).
     * @param constructor Constructs new objects for callers
     * @return A pool
     */
    public static <T> ReferencePool<T> create(String poolName, int maxSize, Supplier<T> constructor) {
        return create(Thread.NORM_PRIORITY - 1, poolName, maxSize, constructor);
    }

    /**
     * Create a new pool.
     *
     * @param <T> The type
     * @param cleanupThreadPriority The priority for the cleanup thread. Note
     * that in a busy application, you don't want to set this too low, or heavy
     * use can pile up objects leading to an OutOfMemoryError, but you likely
     * want it low enough not to steal time from foreground threads.
     * @param poolName The name of the pool (used for logging and by the
     * reclamation thread)
     * @param maxSize The maximum pool size (if less than or equal to zero, a
     * no-op pool that always calls the constructor is created).
     * @param constructor Constructs new objects for callers
     * @return A pool
     */
    public static <T> ReferencePool<T> create(int cleanupThreadPriority, String poolName,
            int maxSize, Supplier<T> constructor) {
        if (maxSize <= 0) {
            return new ReferencePool<T>() {
                @Override
                public void borrow(Consumer<T> consumer) {
                    consumer.accept(constructor.get());
                }

                @Override
                public <R> R lazyTakeFromPool(BiFunction<T, Consumer<Object>, R> c) {
                    return c.apply(constructor.get(), ignored -> {
                    });
                }

                @Override
                public void returnToPool(T obj) {
                    // do nothing
                }

                @Override
                public T takeFromPool() {
                    return constructor.get();
                }

                @Override
                public T takeFromPool(Object owner) {
                    return constructor.get();
                }
            };
        }
        return new PhantomReferencePool<>(cleanupThreadPriority, poolName, maxSize, constructor);
    }

}
