/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
abstract class ArraysPool<T> implements Supplier<T> {

    public abstract T get();

    public abstract void dispose(T arr);

    public static ArraysPool<long[]> cachingPool(int arrayLength, int initialArrays, int maxArrays) {
        return new CachingPool<>(initialArrays, maxArrays, new LongArraySupplier(arrayLength));
    }

    public static ArraysPool<long[]> uncachedPool(int arrayLength) {
        return new LongArraySupplier(arrayLength);
    }

    static final class LongArraySupplier extends ArraysPool<long[]> implements Supplier<long[]> {

        private final int length;

        public LongArraySupplier(int length) {
            this.length = length;
        }

        @Override
        public long[] get() {
            return new long[length];
        }

        @Override
        public void dispose(long[] arr) {
            // do nothing
        }
    }

    static final class CachingPool<T> extends ArraysPool<T> {

        private final Set<T> active = new HashSet<>();
        private final LinkedList<T> inactive = new LinkedList<>();
        private final int max;
        private final Supplier<T> supplier;

        CachingPool(int initial, int max, Supplier<T> supp) {
            this.max = max;
            this.supplier = supp;
            for (int i = 0; i < initial; i++) {
                inactive.add(supp.get());
            }
        }

        @Override
        public synchronized T get() {
            int inactiveSize = inactive.size();
            int count = active.size() + inactiveSize;
            T result;
            if (inactiveSize > 0) {
                result = inactive.pop();
                active.add(result);
            } else {
                result = supplier.get();
                if (count < max) {
                    active.add(result);
                }
            }
            return result;
        }

        @Override
        public synchronized void dispose(T arr) {
            active.remove(arr);
            int inactiveSize = inactive.size();
            int count = active.size() + inactiveSize;
            if (count < max) {
                inactive.push(arr);
            }
        }
    }
}
