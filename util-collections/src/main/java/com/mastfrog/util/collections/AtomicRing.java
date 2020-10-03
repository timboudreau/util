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

import static com.mastfrog.util.preconditions.Checks.greaterThanOne;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * A lockless concurrent ring buffer - essentially a small collection or list
 * with a fixed maximum length, where once it has reached the target size, each
 * addition atomically removes the current head and appends a new tail. Useful
 * for maintaining small lists of recently performed operations at low cost in
 * highly concurrent systems for debugging purposes; or any case where a
 * concurrent, LIFO, fixed-max-size linked list is useful.
 *
 * @author Tim Boudreau
 */
final class AtomicRing<T> implements Iterable<T>, Consumer<T>, Ring<T> {

    private final AtomicReference<Entry<T>> head = new AtomicReference<>();

    /**
     * Create a new ring buffer.
     *
     * @param size The buffer size
     */
    public AtomicRing(int size) {
        Entry<T> first = new Entry<>(null);
        Entry<T> last = first;
        for (int i = 0; i < greaterThanOne("size", size) - 1; i++) {
            last = new Entry<>(null, last);
        }
        head.set(last);
    }

    @Override
    public T top() {
        Entry<T> entry = head.get();
        if (entry == null) {
            return null;
        }
        entry = entry.nextWithValue();
        return entry == null ? null : entry.value;
    }

    /**
     * Add an item, pushing out the oldest item if the ring size has been hit;
     * note, nulls are allowed here, but will simply be ignored when iterating
     * the contents - the ring will simply return one less element for each
     * null.
     *
     * @param val The new value or null
     */
    @Override
    public void push(T val) {
        if (val != null) {
            // The trick that makes this all work: 
            //
            // The *only* straightforward way to make something lockless
            // with atomic references *and* guarantee you never drop a
            // message is to ensure that all updates modify THE SAME
            // SINGLE FIELD.
            //
            // If we, say, made each link an atomic reference, then one
            // thread can be adding a tail to an item while another thread
            // is lopping it of its parent (see the reliability guarantees
            // of AtomicLinkedQueue's deletions for what this looks like),
            // so we would wind up with an add that looks like it succeeded,
            // but it added to an item that was concurrently removed, so
            // the successfully added item has disappeard from the universe.
            //
            // AtomicReference and
            // friends do a small internal busy-wait when updating - meaning
            // this updater may be called *more than once* under concurrency,
            // this thread updates the value successfully.
            //
            // On each potential call, we copy the entire linked list structure
            // and that is what is returned (the new linked list will be missing
            // the head of the old one).  If the update fails, we are passed the
            // head of the list (which has changed since the previous call or
            // we would not be invoked again), and again make a copy that
            // appends our new tail item.
            //
            // In this way, we never drop an item (we will forEach called back and
            // make a new copy of the updated state, appending the new tail, until
            // our write wins).
            head.getAndUpdate(en -> {
                return en.add(val, 0);
            });
        }
    }

    /**
     * Get the contents of the ring buffer, oldest element first.
     *
     * @param c A consumer
     */
    public void forEach(Consumer<? super T> c) {
        head.get().get(c);
    }

    /**
     * Provides a late-binding iterator whose contents only become fixed on the
     * first method call on it.
     *
     * @return An iterator
     */
    @Override
    public Iterator<T> iterator() {
        return new LazyIter<>(head);
    }

    /**
     * Just a singly linked-list where an add call to the head item walks to the
     * tail, copying each item, with the replacement for the old tail being a
     * copy of it with the added item as its next item; an entry's fields are
     * final - it can only create a replacement for itself; the add call on the
     * head item returns the newly created next entry, which becomes the new
     * head, so a constant queue length is maintained.
     *
     * @param <T> The entry type
     */
    private static final class Entry<T> {

        private final Entry<T> next;
        private final T value;

        Entry(T value) {
            this.next = null;
            this.value = value;
        }

        Entry(T value, Entry<T> next) {
            this.value = value;
            this.next = next;
        }

        Entry<T> add(T obj, int index) {
            if (next == null) {
                Entry<T> nue = new Entry<>(obj);
                return new Entry<>(value, nue);
            }
            Entry<T> newNext = next.add(obj, index + 1);
            return index == 0 ? newNext : new Entry<>(value, newNext);
        }

        void get(Consumer<? super T> c) {
            // Empty items such as are added initially are simply
            // skipped
            if (value != null) {
                c.accept(value);
            }
            Entry<T> n = next;
            assert n != this : "Cannot be my own next";
            if (n != null) {
                n.get(c);
            }
        }

        // Skip forward returning the next item in the linked list that
        // has a non-null value
        Entry<T> nextWithValue() {
            if (value != null) {
                return this;
            }
            return next;
        }
    }

    private static final class LazyIter<T> implements Iterator<T> {

        private final AtomicReference<Entry<T>> ref;

        private Entry<T> curr;
        private boolean done;

        public LazyIter(AtomicReference<Entry<T>> ref) {
            this.ref = ref;
        }

        Entry<T> curr() {
            if (done) {
                throw new NoSuchElementException();
            }
            if (curr == null) {
                curr = ref.get();
                if (curr != null) {
                    curr = curr.nextWithValue();
                    if (curr == null) {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
            return curr;
        }

        @Override
        public boolean hasNext() {
            return done ? false : curr() != null;
        }

        @Override
        public T next() {
            if (done) {
                throw new NoSuchElementException();
            }
            Entry<T> c = curr();
            curr = c.next;
            if (curr == null) {
                done = true;
            } else {
                curr = curr.nextWithValue();
                if (curr == null) {
                    done = true;
                }
            }
            return c.value;
        }
    }
}
