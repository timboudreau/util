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
package com.mastfrog.concurrent;

import static com.mastfrog.concurrent.AtomicIntegerPair.pack;
import static com.mastfrog.concurrent.AtomicIntegerPair.unpackLeft;
import static com.mastfrog.concurrent.AtomicIntegerPair.unpackRight;
import com.mastfrog.concurrent.IntegerPair.IntegerPairUpdater;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntBiPredicate;
import com.mastfrog.function.state.Lng;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.IntUnaryOperator;

/**
 * An fixed-size array of pairs of ints each of which can be <i>individually</i>
 * atomically updated in a single operation - useful for lockless hash-map like
 * structures where you need to atomically update a hash *and* an index into
 * some data in a single atomic operation.
 *
 * @author Tim Boudreau
 */
public class AtomicIntegerPairArray {

    private final AtomicLongArray arr;

    private AtomicIntegerPairArray(int size) {
        arr = new AtomicLongArray(size);
    }

    private AtomicIntegerPairArray(AtomicIntegerPairArray orig) {
        int sz = orig.size();
        AtomicLongArray a = arr = new AtomicLongArray(sz);
        for (int i = 0; i < sz; i++) {
            a.setOpaque(i, orig.arr.get(i));
        }
    }

    private AtomicIntegerPairArray(int[] pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of array entries: "
                    + pairs.length);
        }
        arr = new AtomicLongArray(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            arr.setOpaque(i / 2, pack(pairs[i], pairs[i + 1]));
        }
    }

    private AtomicIntegerPairArray(long[] items) {
        arr = new AtomicLongArray(items);
    }

    public static AtomicIntegerPairArray from(long[] items) {
        return new AtomicIntegerPairArray(items);
    }

    public static AtomicIntegerPairArray from(int[] items) {
        return new AtomicIntegerPairArray(items);
    }

    public static AtomicIntegerPairArray create(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative size " + size);
        }
        return new AtomicIntegerPairArray(size);
    }

    public AtomicIntegerPairArray copy() {
        return new AtomicIntegerPairArray(this);
    }

    public AtomicIntegerPairArray copyOfRange(int start, int lengthToCopy, int size) {
        AtomicIntegerPairArray nue = new AtomicIntegerPairArray(size);
        for (int i = start; i < start + lengthToCopy && i - start < size; i++) {
            nue.arr.setOpaque(i - start, arr.get(i));
        }
        return nue;
    }

    /**
     * Set the values at the first array index that matches the passed predicate
     * to the new values.
     *
     * @param leftValue The left value
     * @param rightValue The right value
     * @param cellTest A test that returns true if the values it is passed can
     * be overwritten
     * @return The index that was updated, or -1 if none matched
     */
    public int setFirst(int leftValue, int rightValue, IntBiPredicate cellTest) {
        int sz = size();
        long newVal = pack(leftValue, rightValue);
        for (int i = 0; i < sz; i++) {
            long val = arr.get(i);
            if (cellTest.test(unpackLeft(val), unpackRight(val))) {
                if (arr.compareAndSet(i, val, newVal)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void fill(int left, int right) {
        int sz = size();
        long value = pack(left, right);
        for (int i = 0; i < sz; i++) {
            arr.set(i, value);
        }
    }

    public int size() {
        return arr.length();
    }

    public int left(int index) {
        return unpackLeft(arr.get(index));
    }

    public int right(int index) {
        return unpackRight(arr.get(index));
    }

    public int setLeft(int index, int newLeft) {
        return unpackLeft(arr.getAndUpdate(index, old -> {
            int oldRight = unpackRight(old);
            return pack(newLeft, oldRight);
        }));
    }

    public int setRight(int index, int newRight) {
        return unpackRight(arr.getAndUpdate(index, old -> {
            int oldLeft = unpackLeft(old);
            return pack(oldLeft, newRight);
        }));
    }

    public void fetch(int index, IntBiConsumer c) {
        long val = arr.get(index);
        c.accept(unpackLeft(val), unpackRight(val));
    }

    public void update(int index, IntUnaryOperator leftFunction,
            IntUnaryOperator rightFunction) {
        arr.updateAndGet(index, old -> {
            int left = leftFunction.applyAsInt(unpackLeft(old));
            int right = rightFunction.applyAsInt(unpackRight(old));
            return pack(left, right);
        });
    }

    public void update(int index, IntegerPairUpdater updater) {
        Lng val = Lng.create();
        arr.updateAndGet(index, old -> {
            val.set(old);
            updater.update(unpackLeft(old), unpackRight(old), (newLeft, newRight) -> {
                val.set(pack(newLeft, newRight));
            });
            return val.getAsLong();
        });

    }

    public boolean compareAndSet(int index, int expectedLeftValue,
            int expectedRightValue, int leftValue, int rightValue) {
        long expect = pack(expectedLeftValue, expectedRightValue);
        long nue = pack(leftValue, rightValue);
        return arr.compareAndSet(index, expect, nue);
    }

    public void set(int index, int left, int right) {
        arr.set(index, pack(left, right));
    }

    public int each(IntBiPredicate pred) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            long val = arr.get(i);
            if (!pred.test(unpackLeft(val), unpackRight(val))) {
                return i;
            }
        }
        return sz - 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size(); i++) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            long val = arr.get(i);
            sb.append('(').append(unpackLeft(val)).append(',')
                    .append(unpackRight(val)).append(')');
        }
        return sb.append(']').toString();
    }

    public int[] toIntArray() {
        int sz = size();
        int[] result = new int[sz * 2];
        for (int i = 0; i < sz; i++) {
            long val = arr.get(i);
            int offset = i * 2;
            result[offset] = unpackLeft(val);
            result[offset + 1] = unpackRight(val);
        }
        return result;
    }

    public long[] toLongArray() {
        int sz = size();
        long[] result = new long[sz];
        for (int i = 0; i < sz; i++) {
            result[i] = arr.get(i);
        }
        return result;
    }

    public IntegerPair pairView(int index) {
        if (index < 0 || index >= size()) {
            throw new IllegalArgumentException("Index " + index
                    + " is out of range 0-" + size());
        }
        return new IntegerPairView(index);
    }

    class IntegerPairView implements IntegerPair {

        private final int index;

        public IntegerPairView(int index) {
            this.index = index;
        }

        @Override
        public boolean compareAndSet(int expectedLeftValue,
                int expectedRightValue, int newLeftValue, int newRightValue) {
            return AtomicIntegerPairArray.this.compareAndSet(
                    index, expectedLeftValue, expectedRightValue,
                    newLeftValue, newRightValue);
        }

        @Override
        public void fetch(IntBiConsumer pair) {
            AtomicIntegerPairArray.this.fetch(index, pair);
        }

        @Override
        public int left() {
            return AtomicIntegerPairArray.this.left(index);
        }

        @Override
        public int right() {
            return AtomicIntegerPairArray.this.right(index);
        }

        @Override
        public void set(int left, int right) {
            AtomicIntegerPairArray.this.set(index, left, right);
        }

        @Override
        public void setLeft(int newLeft) {
            AtomicIntegerPairArray.this.setLeft(index, newLeft);
        }

        @Override
        public void setRight(int newRight) {
            AtomicIntegerPairArray.this.setRight(index, newRight);
        }

        @Override
        public long toLong() {
            return arr.get(index);
        }

        @Override
        public void update(IntUnaryOperator leftFunction, IntUnaryOperator rightFunction) {
            AtomicIntegerPairArray.this.update(index, leftFunction, rightFunction);
        }

        @Override
        public String toString() {
            return index + ":(" + left() + "," + right() + ")";
        }

        @Override
        public void update(IntegerPairUpdater pairUpdater) {
            AtomicIntegerPairArray.this.update(index, pairUpdater);
        }

        @Override
        public int getAndUpdateLeft(IntUnaryOperator op) {
            long newValue = AtomicIntegerPairArray.this.arr.getAndUpdate(index, old -> {
                int left = op.applyAsInt(unpackLeft(old));
                int right = unpackRight(old);
                return pack(left, right);
            });
            return unpackLeft(newValue);
        }

        @Override
        public int getAndUpdateRight(IntUnaryOperator op) {
            long newValue = AtomicIntegerPairArray.this.arr.getAndUpdate(index, old -> {
                int left = unpackLeft(old);
                int right = op.applyAsInt(unpackRight(old));
                return pack(left, right);

            });
            return unpackRight(newValue);
        }

        @Override
        public int updateAndGetLeft(IntUnaryOperator op) {
            long newValue = AtomicIntegerPairArray.this.arr.updateAndGet(index, old -> {
                int left = op.applyAsInt(unpackLeft(old));
                int right = unpackRight(old);
                return pack(left, right);
            });
            return unpackLeft(newValue);
        }

        @Override
        public int updateAndGetRight(IntUnaryOperator op) {
            long newValue = AtomicIntegerPairArray.this.arr.updateAndGet(index, old -> {
                int left = unpackLeft(old);
                int right = op.applyAsInt(unpackRight(old));
                return pack(left, right);

            });
            return unpackRight(newValue);
        }

    }
}
