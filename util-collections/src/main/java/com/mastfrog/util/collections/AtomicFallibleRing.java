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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * An alternate atomic ring implementation which has much lower overhead than
 * AtomicRing, but does so by sacrificing the guarantee that items will never be
 * dropped. Uses a plain array and an atomic looping counter to handle
 * concurrency.
 *
 * @author Tim Boudreau
 */
final class AtomicFallibleRing<T> implements Ring<T> {

    private final Object[] items;
    private final AtomicRoundRobin arr;

    AtomicFallibleRing(int size) {
        items = new Object[size];
        arr = new AtomicRoundRobin(size);
    }

    @Override
    public void push(T val) {
        int ix = arr.next();
        items[ix] = val;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T top() {
        return (T) items[arr.get()];
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super T> action) {
        int curr = arr.next(arr.last());
        int max = arr.maximum();
        for (int i = 0; i < max; i++) {
            if (items[curr] != null) {
                action.accept((T) items[curr]);
            }
            curr = arr.next(curr);
        }
    }

    @Override
    public Iterator<T> iterator() {
        List<T> all = new ArrayList<>(arr.maximum());
        forEach(all::add);
        return all.iterator();
    }

    private static final class AtomicRoundRobin {

        private final int maximum;
        private volatile int currentValue;
        private final AtomicIntegerFieldUpdater<AtomicRoundRobin> up;

        public AtomicRoundRobin(int maximum) {
            if (maximum <= 0) {
                throw new IllegalArgumentException("Maximum must be > 0");
            }
            this.maximum = maximum;
            up = AtomicIntegerFieldUpdater.newUpdater(AtomicRoundRobin.class, "currentValue");
        }

        int next(int current) {
            return current >= maximum - 1 ? 0 : current + 1;
        }

        public int last() {
            int result = get() - 1;
            return result < 0 ? maximum - 1 : result;
        }

        /**
         * Get the maximum possible value
         *
         * @return The maximum
         */
        public int maximum() {
            return maximum;
        }

        /**
         * Get the current value
         *
         * @return
         */
        public int get() {
            return up.get(this);
        }

        /**
         * Get the next value, incrementing the value or resetting it to zero
         * for the next caller.
         *
         * @return The value
         */
        public int next() {
            if (maximum == 1) {
                return 0;
            }
            for (;;) {
                int current = get();
                int next = next(current);
                if (up.compareAndSet(this, current, next)) {
                    return current;
                }
            }
        }
    }

}
