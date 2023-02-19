/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent.stats.percentile;

import java.util.Arrays;
import java.util.function.LongConsumer;

/**
 * A simple growable LongStore over an array.
 */
final class LongArrayStore implements LongStore {

    int last = -1;
    private final int capacityHint;
    private final long[] data;
    private boolean sorted = true;

    LongArrayStore(int capacityHint) {
        this.capacityHint = capacityHint;
        data = allocate(capacityHint);
    }

    LongArrayStore(long[] initial) {
        capacityHint = Math.max(128, initial.length / 4);
        data = initial;
    }

    static long[] allocate(int capacityHint) {
        if (capacityHint < 4096) {
            return new long[capacityHint];
        }
        return new long[2048];
    }

    long[] addCapacity() {
        int add = Math.min(2048, data.length / 4);
        return Arrays.copyOf(data, data.length + add);
    }

    public void clear() {
        last = -1;
    }

    @Override
    public int size() {
        return last + 1;
    }

    @Override
    public boolean isSorted() {
        return sorted;
    }

    @Override
    public void sort() {
        if (!isSorted() && !isEmpty()) {
            if (size() > 65536) {
                Arrays.parallelSort(data, 0, last + 1);
            } else {
                Arrays.sort(data, 0, last + 1);
            }
            sorted = true;
        }
    }

    @Override
    public void accept(long value) {
        int index = ++last;
        if (last >= data.length) {
            addCapacity();
        }
        data[index] = value;
        if (index > 0 && sorted) {
            sorted = data[index - 1] < value;
        }
    }

    @Override
    public long applyAsLong(int value) {
        return data[value];
    }

    @Override
    public void forEach(LongConsumer c) {
        for (int i = 0; i <= last; i++) {
            c.accept(data[i]);
        }
    }

}
