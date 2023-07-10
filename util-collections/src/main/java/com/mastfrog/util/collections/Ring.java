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

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Interface for ring-buffer-like collections. The typical use-case is for
 * maintaining a small collection of the most recent operations a highly
 * concurrent system has been performing for purposes of logging them on-demand
 * on when an error condition is encountered.
 *
 * @author Tim Boudreau
 */
public interface Ring<T> extends Consumer<T>, Iterable<T> {

    /**
     * A lockless concurrent ring buffer - essentially a small collection or
     * list with a fixed maximum length, where once it has reached the target
     * size, each addition atomically removes the current head and appends a new
     * tail. Useful for maintaining small lists of recently performed operations
     * at low cost in highly concurrent systems for debugging purposes; or any
     * case where a concurrent, LIFO, fixed-max-size linked list is useful.
     *
     * @param size The maximum number of elements the ring can contain
     */
    public static <T> Ring<T> createAtomic(int size) {
        return new AtomicRing<>(size);
    }

    /**
     * Creates a lockless concurrent ring buffer which handles concurrency, but
     * without the guarantee that for every message passed, there will be a time
     * when it can be observed in the buffer - simply uses an array and an
     * atomic loop counter to achieve concurrency, with lower overhead (the Ring
     * created by <code>createAtomic()</code> copies the entire linked list of
     * its contents for every addition, giving it slightly more overhead in
     * exchange for every single message ever delivered to it appearing in the
     * contents at least briefly. Suitable for very high performance, highly
     * concurrent uses.
     *
     * @param <T> The type
     * @param size The fixed maximum size at which point the first element is
     * discarded
     * @return A ring
     */
    public static <T> Ring<T> createFallibleAtomic(int size) {
        return new AtomicFallibleRing<>(size);
    }

    /**
     * Synonym for push() in order to implement Consumer.
     *
     * @param val A value or null
     */
    @Override
    default void accept(T val) {
        push(val);
    }

    /**
     * Add an item, pushing out the oldest item if the ring size has been hit;
     * note, nulls are allowed here, but will simply be ignored when iterating
     * the contents - the ring will simply return one less element for each
     * null.
     *
     * @param val The new value or null
     */
    void push(T val);

    /**
     * Get the most recently added element, if any.
     *
     * @return The most recently added element, or null.
     */
    T top();

    /**
     * For cases where operations are, say, periodically written to a log file,
     * create a wrapper instance for this Ring which calls the passed consumer
     * <i>after</i> each push of a new item.
     *
     * @param otherConsumer A consumer
     * @return A ring
     */
    default Ring<T> wrap(Consumer<T> otherConsumer) {
        return new Ring<T>() {
            @Override
            public void push(T val) {
                Ring.this.push(val);
                otherConsumer.accept(val);
            }

            @Override
            public T top() {
                return Ring.this.top();
            }

            @Override
            public Iterator<T> iterator() {
                return Ring.this.iterator();
            }

        };
    }
}
