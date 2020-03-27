/*
 * The MIT License
 *
 * Copyright 2020 Tim Boudreau.
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

import com.mastfrog.util.search.Bias;
import static com.mastfrog.util.search.Bias.BACKWARD;
import static com.mastfrog.util.search.Bias.FORWARD;
import static com.mastfrog.util.search.Bias.NEAREST;
import static com.mastfrog.util.search.Bias.NONE;
import static java.lang.Double.doubleToLongBits;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;

/**
 * A high-performance, binary-search based Set of primitive doubles.
 *
 * @author Tim Boudreau
 */
class DoubleSetImpl implements DoubleSet {

    static final DecimalFormat FMT
            = new DecimalFormat("#####################0.########################"
                    + "##########");

    double[] data;
    private boolean clean;
    private final int initialCapacity;
    int size;

    public DoubleSetImpl() {
        this(128);
    }

    public DoubleSetImpl(int capacity) {
        data = new double[capacity];
        initialCapacity = capacity;
    }

    private DoubleSetImpl(double[] data) {
        this.data = data;
        size = data.length;
        initialCapacity = size;
        clean = true;
    }

    private DoubleSetImpl(double[] data, int size, boolean clean) {
        this.data = data;
        this.size = size;
        this.clean = clean;
        this.initialCapacity = data.length;
    }

    @Override
    public void trim() {
        if (size != data.length) {
            data = Arrays.copyOf(data, size);
        }
    }

    public static DoubleSetImpl of(Collection<? extends Number> c) {
        DoubleSetImpl set = new DoubleSetImpl(c.size());
        for (Number n : c) {
            if (n == null) {
                throw new IllegalArgumentException("Collection contains null: " + c);
            }
            set.add(n.doubleValue());
        }
        return set;
    }

    public static DoubleSetImpl ofFloats(float... floats) {
        DoubleSetImpl result = new DoubleSetImpl(floats.length);
        for (int i = 0; i < floats.length; i++) {
            result.add(floats[i]);
        }
        return result;
    }

    public static DoubleSetImpl ofDoubles(double... doubles) {
        return ofDoubles(doubles.length, doubles);
    }

    public static DoubleSetImpl ofDoubles(int capacity, double... doubles) {
        DoubleSetImpl result = new DoubleSetImpl(capacity);
        for (int i = 0; i < doubles.length; i++) {
            result.add(doubles[i]);
        }
        return result;
    }

    public void removeIndices(IntSet indices) {
        if (indices.isEmpty()) {
            return;
        }
        int start = -1;
        int len = 0;
        int[] all = indices.toIntArray();
        for (int i = all.length - 1; i >= 0; i--) {
            int val = all[i];
            if (start != -1) {
                if (val == start - 1) {
                    start = val;
                    len++;
                } else {
                    removeConsecutiveIndices(start, len);
                    start = val;
                    len = 1;
                }
            } else {
                start = val;
                len = 1;
            }
        }
        if (start != -1) {
            removeConsecutiveIndices(start, len);
        }
    }

    public void removeIndex(int index) {
        if (isEmpty() || index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Attempt to remove item "
                    + index + " of " + size);
        }
        shiftData(index + 1, index, size - index);
        size--;
    }

    public void removeConsecutiveIndices(int start, int length) {
        if (length == 1) {
            removeIndex(start);
            return;
        } else if (length < 1) {
            return;
        } else if (start < 0 || start >= size || start + length > size) {
            throw new IndexOutOfBoundsException("Attempt to remove range "
                    + start + ":" + (start + length) + " of " + size);
        }
        shiftData(start + length, start, size - (start + length));
        size -= length;
    }

    @Override
    public void clear() {
        size = 0;
        clean = true;
    }

    @Override
    public DoubleSetImpl copy() {
        double[] newData = Arrays.copyOf(data, data.length);
        return new DoubleSetImpl(newData, size, clean);
    }

    private DoubleSetImpl trimmedCopy() {
        double[] newData = Arrays.copyOf(data, size);
        return new DoubleSetImpl(newData, size, clean);
    }

    @Override
    public DoubleSet toReadOnlyCopy() {
        return new ReadOnlyDoubleSet(trimmedCopy());
    }

    @Override
    public DoubleSet unmodifiableView() {
        return new ReadOnlyDoubleSet(this);
    }

    @Override
    public boolean remove(double key) {
        int ix = indexOf(key);
        if (ix >= 0) {
            shiftData(ix + 1, ix, size - ix);
            size--;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        ensureClean();
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            sb.append(FMT.format(data[i]));
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }

    @Override
    public DoubleSetImpl[] partition(int maxPartitions) {
        ensureClean();
        if (maxPartitions < 0) {
            throw new IllegalArgumentException(
                    "Negative partitions " + maxPartitions);
        }
        if (maxPartitions == 0 || size < maxPartitions * 4) {
            return new DoubleSetImpl[]{this};
        }
        int itemsPer = size / maxPartitions;
        // Don't allocate one partition that's going to have, say,
        // three items
        if (size - ((itemsPer - 1) * size) < 5) {
            itemsPer--;
        }
        if (itemsPer <= 1) {
            return new DoubleSetImpl[]{this};
        }
        int partitions = size / itemsPer;
        if (partitions * itemsPer < size) {
            partitions++;
        }
        DoubleSetImpl[] result = new DoubleSetImpl[partitions];
        for (int i = 0; i < partitions; i++) {
            int start = (i * itemsPer);
            int length = i == partitions - 1 ? size - start : itemsPer;
            double[] copy = new double[length];
            System.arraycopy(data, start, copy, 0, length);
            result[i] = new DoubleSetImpl(copy);
        }
        return result;
    }

    void grow(int newSize) {
        data = Arrays.copyOf(data, newSize);
    }

    private void maybeGrow() {
        if (size == data.length - 1) {
            grow(data.length
                    + (initialCapacity - (initialCapacity / 3)));
        }
    }

    void sort() {
        Arrays.sort(data, 0, size);
    }

    double[] rawData() {
        return data;
    }

    void onDedup(int index) {

    }

    private void clean() {
        if (size <= 1) {
            return;
        }
        sort();
        double last = data[0];
        int dedupFrom = -1;
        for (int i = 1; i < size; i++) {
            double v = data[i];
            if (v == last) {
                dedupFrom = i;
                break;
            }
            last = v;
        }
        if (dedupFrom != -1) {
            if (dedupFrom == size - 1) {
                size--;
                return;
            }
            int removed = 1;
            for (int i = dedupFrom; i < size; i++) {
                double v = data[i];
                if (v == last && i > dedupFrom) {
                    removed++;
                } else {
//                    data[i - removed] = v;
                    moveItem(i, i - removed, v);
                }
                last = v;
            }
            size -= removed;
        }
    }

    void moveItem(int srcIndex, int targetIndex, double v) {
        data[targetIndex] = v;
    }

    @Override
    public double getAsDouble(int index) {
        ensureClean();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("" + index);
        }
        return data[index];
    }

    @Override
    public void addAll(DoubleSet set) {
        if (set == this) {
            return;
        }
        int sz = set.size();
        if (sz == 0) {
            return;
        }
        if (size + sz > data.length) {
            grow(size + sz);
        }
        clean &= greatest() < set.least();
        double[] otherData;
        if (set instanceof DoubleSetImpl) {
            otherData = ((DoubleSetImpl) set).data;
        } else if (set instanceof ReadOnlyDoubleSet && ((ReadOnlyDoubleSet) set).delegate instanceof DoubleSetImpl) {
            otherData = ((DoubleSetImpl) ((ReadOnlyDoubleSet) set).delegate).data;
        } else {
            otherData = set.toDoubleArray();
        }
        System.arraycopy(otherData, 0, data, size, sz);
        size += sz;
    }

    @Override
    public int removeRange(double least, double greatest) {
        int stix = nearestIndexTo(Math.min(least, greatest), Bias.FORWARD);
        if (stix < 0) {
            return 0;
        }
        int enix = nearestIndexTo(Math.max(least, greatest), Bias.BACKWARD);
        if (enix < 0) {
            return 0;
        }
        if (enix < stix) {
            return 0;
        }
        int len = enix - stix;
        shiftData(enix, stix, size - enix);
        size -= len;
        return len;
    }

    void ensureClean() {
        if (!clean && size > 0) {
            clean();
            clean = true;
        }
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void add(double value) {
        if (value == Double.MIN_VALUE) {
            throw new IllegalArgumentException("Illegal value "
                    + "Double.MIN_VALUE (is used to indicate null results)");
        }
        maybeGrow();
        data[size++] = value;
        if (size > 1 && data[size - 1] >= value) {
            clean = false;
        }
    }

    @Override
    public int size() {
        ensureClean();
        return size;
    }

    @Override
    public double least() {
        if (size == 0) {
            return 0;
        }
        ensureClean();
        return data[0];
    }

    @Override
    public double greatest() {
        if (size == 0) {
            return 0;
        }
        ensureClean();
        return data[size - 1];
    }

    @Override
    public void forEachDouble(DoubleConsumer dc) {
        ensureClean();
        for (int i = 0; i < size; i++) {
            dc.accept(data[i]);
        }
    }

    @Override
    public void forEachReversed(DoubleConsumer dc) {
        ensureClean();
        for (int i = size - 1; i >= 0; i--) {
            dc.accept(data[i]);
        }
    }

    @Override
    public void removeAll(DoubleSet remove) {
        if (remove.isEmpty() || isEmpty()) {
            return;
        } else if (remove.size() == 1) {
            int ix = indexOf(remove.getAsDouble(0));
            if (ix >= 0) {
                shiftData(ix + 1, ix, size - (ix + 1));
                size--;
            }
            return;
        }
        // triggers a call to ensureClean()
        if (remove.least() > greatest() || remove.greatest() < least()) {
            return;
        }
        int remStart = -1;
        int remCount = 0;
        for (int i = remove.size() - 1; i >= 0; i--) {
            int ix = indexOf(remove.getAsDouble(i));
            if (ix >= 0) {
                if (remStart == ix + remCount) {
                    remCount++;
                } else if (remStart == -1) {
                    remStart = ix;
                    remCount = 1;
                } else {
                    shiftData(remStart + 1, remStart - (remCount - 1),
                            size - (remStart + 1));
                    size -= remCount;
                    remStart = ix;
                    remCount = 1;
                }
            }
        }
        if (remCount > 0) {
            shiftData(remStart + 1, remStart - (remCount - 1),
                    size - (remStart + 1));
            size -= remCount;
        }
    }

    void shiftData(int srcIx, int destIx, int len) {
        System.arraycopy(data, srcIx, data, destIx, len);
    }

    @Override
    public double range() {
        return greatest() - least();
    }

    @Override
    public void retainAll(DoubleSet retain) {
        if (greatest() < retain.least() || least() > retain.greatest()) {
            clear();
            return;
        }
        ensureClean();
        boolean anyRetained = false;
        for (int i = size - 1; i >= 0; i--) {
            double v = data[i];
            if (!retain.contains(v)) {
                if (!anyRetained) {
                    size = i;
                } else {
                    shiftData(i + 1, i, size - (i + 1));
                    size--;
                }
            } else {
                anyRetained = true;
            }
        }
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return new It();
    }

    class It implements PrimitiveIterator.OfDouble {

        private int cursor = -1;

        @Override
        public double nextDouble() {
            return data[++cursor];
        }

        @Override
        public boolean hasNext() {
            return cursor + 1 < size;
        }
    }

    @Override
    public double[] toDoubleArray() {
        ensureClean();
        return Arrays.copyOf(data, size);
    }

    @Override
    public boolean contains(double d) {
        ensureClean();
        return Arrays.binarySearch(data, 0, size, d) >= 0;
    }

    @Override
    public int indexOf(double d) {
        ensureClean();
        int result = Arrays.binarySearch(data, 0, size, d);
        return result >= 0 ? result : -1;
    }

    @Override
    public int hashCode() {
        ensureClean();
        long result = 5 * (size + 1);
        for (int i = 0; i < size; i++) {
            result += 43 * doubleToLongBits(data[i]);
        }
        return (int) (result | (result << 32));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DoubleSetImpl) {
            DoubleSetImpl ds = (DoubleSetImpl) o;
            ensureClean();
            if (ds.size != size) {
                return false;
            }
            if (ds.greatest() != greatest()) {
                return false;
            } else {
                ensureClean();
                ds.ensureClean();
                for (int i = 0; i < size; i++) {
                    if (data[i] != ds.data[i]) {
                        return false;
                    }
                }
            }
            return true;
        } else if (o instanceof DoubleSet) {
            DoubleSet ds = (DoubleSet) o;
            if (isEmpty() && ds.isEmpty()) {
                return true;
            }
            ensureClean();
            if (size != ds.size()) {
                return false;
            } else if (ds.greatest() != greatest()) {
                return false;
            } else {
                for (int i = 0; i < size; i++) {
                    if (data[i] != ds.getAsDouble(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value closest to the specified value where the distance, positive
     * or negative, to that value is less than or equal to the passed tolerance;
     * returns Double.MIN_VALUE as null result.
     *
     * @param val A value
     * @return The nearest value to that value
     */
    @Override
    public double nearestValueTo(double val, double tolerance) {
        double result = nearestValueTo(val);
        if (Math.abs(val - result) > tolerance) {
            return Double.MIN_VALUE;
        }
        return result;
    }

    /**
     * Get the value closest to the specified value; returns Double.MIN_VALUE as
     * null result.
     *
     * @param val A value
     * @return The nearest value to that value
     */
    @Override
    public double nearestValueTo(double val) {
        if (size == 0) {
            return Double.MIN_VALUE;
        } else if (size == 1) {
            return data[0];
        }
        int ix = nearestIndexTo(val, Bias.NEAREST);
        return data[ix];
    }

    public double nearestValueExclusive(double val, double tolerance) {
        double result = nearestValueExclusive(val);
        if (Math.abs(val - result) <= tolerance) {
            return result;
        }
        return Double.MIN_VALUE;
    }

    public double nearestValueExclusive(double val) {
        if (size == 0) {
            return Double.MIN_VALUE;
        } else if (size == 1) {
            double result = data[0];
            if (result == val) {
                return Double.MIN_VALUE;
            }
            return result;
        }
        int ix = nearestIndexTo(val, Bias.NEAREST);
        double result = data[ix];
        if (result == val) {
            int prevIx = ix - 1;
            int nextIx = ix + 1;
            double prevVal = prevIx < 0 ? (Double.MAX_VALUE - (Math.abs(val) + 1)) : data[prevIx];
            double nextVal = nextIx >= size ? (Double.MAX_VALUE - (Math.abs(val) + 1)) : data[nextIx];
            if (prevIx < 0) {
                return nextVal;
            } else if (nextIx >= size) {
                return prevVal;
            }
            double distPrev = Math.abs(val - prevVal);
            double distNext = Math.abs(val - nextVal);
            if (distPrev < distNext) {
                return prevVal;
            } else {
                return nextVal;
            }
        }
        return result;
    }

    /**
     * Get the index of the value closest to the specified value; returns -1 if
     * no value can be found.
     *
     * @param value The target (possibly approximate, depending on the bias
     * choice) value
     * @param bias The bias, determining how to search for inexact matches
     * @return The index of the nearest value, or -1 if none
     */
    @Override
    public int nearestIndexTo(double value, Bias bias) {
        ensureClean();
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            double v = data[0];
            switch (bias) {
                case BACKWARD:
                    return value >= v ? 0 : -1;
                case FORWARD:
                    return value <= v ? 0 : -1;
                case NEAREST:
                    return 0;
                case NONE:
                    return value == v ? 0 : -1;
                default:
                    throw new AssertionError(bias);
            }
        }
        ensureClean();
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            double val = data[0];
            switch (bias) {
                case BACKWARD:
                    if (val <= value) {
                        return 0;
                    } else {
                        return -1;
                    }
                case FORWARD:
                case NEAREST:
                    if (val >= value) {
                        return 0;
                    } else {
                        return -1;
                    }
            }
        }
        switch (bias) {
            case NONE:
                int result = indexOf(value);
//                if (result < -1) {
//                    new IllegalStateException("Weird answer for indexOfPresumingSorted with bias none "
//                        + " " + result + " for value " + value + " in " + this).printStackTrace();
//                }
                if (result >= 0) {
                    while (result < size - 1 && data[result + 1] == data[result]) {
                        result++;
                    }
                }
                return result;
            case FORWARD:
            case BACKWARD:
                int res2 = nearestIndexTo(0, size - 1, bias, value);
                if (res2 != -1) {
                    while (res2 < size - 1 && data[res2 + 1] == data[res2]) {
                        res2++;
                    }
                }
                return res2;
            case NEAREST:
                int fwd = nearestIndexTo(0, size - 1, Bias.FORWARD, value);
                int bwd = nearestIndexTo(0, size - 1, Bias.BACKWARD, value);
                if (fwd == -1) {
                    return bwd;
                } else if (bwd == -1) {
                    return fwd;
                } else if (fwd == bwd) {
                    return fwd;
                } else {
                    double fwdDiff = Math.abs(data[fwd] - value);
                    double bwdDiff = Math.abs(data[bwd] - value);
                    if (fwdDiff == bwdDiff) {
                        return fwd;
                    } else if (fwdDiff < bwdDiff) {
                        return fwd;
                    } else {
                        return bwd;
                    }
                }
            default:
                throw new AssertionError(bias);
        }
    }

    private int nearestIndexTo(int start, int end, Bias bias, double value) {
        if (start == end) {
            double currentVal = data[start];
            if (currentVal == value) {
                return start;
            }
            switch (bias) {
                case BACKWARD:
                    if (currentVal <= value) {
                        return start;
                    } else {
                        return -1;
                    }
                case FORWARD:
                    if (currentVal >= value) {
                        return start;
                    } else {
                        return -1;
                    }
            }
        }
        double startVal = data[start];
        if (startVal == value) {
            return start;
        }
        if (startVal > value) {
            switch (bias) {
                case BACKWARD:
                    if (startVal > value) {
                        return -1;
                    }
                    return start - 1;
                case FORWARD:
                    return start;
                default:
                    return -1;
            }
        }
        double endVal = data[end];
        if (endVal == value) {
            return end;
        }
        if (endVal < value) {
            switch (bias) {
                case BACKWARD:
                    return end;
                case FORWARD:
                    int result = end + 1;
                    return result < size ? result : -1;
                default:
                    return -1;
            }
        }
        int mid = start + ((end - start) / 2);
        double midVal = data[mid];
//        while (mid < size-1 && data[mid+1] == midVal) {
//            mid++;
//        }
        if (midVal == value) {
            return mid;
        }
        // If we have an odd number of slots, we can getAsDouble into trouble here:
        if (midVal < value && endVal > value) {
            int newStart = mid + 1;
            int newEnd = end - 1;
            double nextStartValue = data[newStart];
            if (nextStartValue > value && bias == Bias.BACKWARD && (newEnd - newStart <= 1 || midVal < value)) {
                return mid;
            }
            double nextEndValue = data[newEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && newEnd - newStart <= 1) {
                return end;
            }
            return nearestIndexTo(newStart, newEnd, bias, value);
        } else if (midVal > value && startVal < value) {
            int nextEnd = mid - 1;
            int nextStart = start + 1;
            double nextEndValue = data[nextEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && nextEnd - nextStart <= 1) {
                return mid;
            }
            double newStartValue = data[nextStart];
            if (bias == Bias.BACKWARD && newStartValue > value && (startVal < value || nextEnd - nextStart <= 1)) {
                return start;
            }
            return nearestIndexTo(nextStart, nextEnd, bias, value);
        }
        return -1;
    }

    static final class ReadOnlyDoubleSet implements DoubleSet {

        private final DoubleSet delegate;

        public ReadOnlyDoubleSet(DoubleSet delegate) {
            this.delegate = delegate;
        }

        @Override
        public DoubleSet copy() {
            return this;
        }

        public void trim() {
            // do nothing
        }

        @Override
        public boolean contains(double d) {
            return delegate.contains(d);
        }

        @Override
        public void forEachDouble(DoubleConsumer dc) {
            delegate.forEachDouble(dc);
        }

        @Override
        public void forEachReversed(DoubleConsumer dc) {
            delegate.forEachReversed(dc);
        }

        @Override
        public double getAsDouble(int index) {
            return delegate.getAsDouble(index);
        }

        @Override
        public double greatest() {
            return delegate.greatest();
        }

        @Override
        public int indexOf(double d) {
            return delegate.indexOf(d);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public PrimitiveIterator.OfDouble iterator() {
            return delegate.iterator();
        }

        @Override
        public double least() {
            return delegate.least();
        }

        @Override
        public int nearestIndexTo(double approximateValue, Bias bias) {
            return delegate.nearestIndexTo(approximateValue, bias);
        }

        @Override
        public double nearestValueTo(double approximateValue, double tolerance) {
            return delegate.nearestValueTo(approximateValue, tolerance);
        }

        @Override
        public double nearestValueExclusive(double approximateValue) {
            return delegate.nearestValueExclusive(approximateValue);
        }

        @Override
        public double nearestValueExclusive(double approximateValue, double tolerance) {
            return delegate.nearestValueExclusive(approximateValue, tolerance);
        }

        @Override
        public double nearestValueTo(double approximateValue) {
            return delegate.nearestValueTo(approximateValue);
        }

        @Override
        public DoubleSet[] partition(int maxPartitions) {
            DoubleSet[] origs = delegate.partition(maxPartitions);
            DoubleSet[] result = new DoubleSet[origs.length];
            for (int i = 0; i < origs.length; i++) {
                result[i] = new ReadOnlyDoubleSet(origs[i]);
            }
            return result;
        }

        @Override
        public double range() {
            return delegate.range();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public double[] toDoubleArray() {
            return delegate.toDoubleArray();
        }

        @Override
        public void removeAll(double... doubles) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public int removeRange(double least, double greatest) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void add(double value) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public boolean remove(double value) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void addAll(DoubleSet set) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void addAll(double[] doubles) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void addAll(float[] floats) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void removeAll(DoubleSet remove) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public void retainAll(DoubleSet retain) {
            throw new UnsupportedOperationException("Read only set.");
        }

        @Override
        public DoubleSet unmodifiableView() {
            return this;
        }

        @Override
        public DoubleSet toReadOnlyCopy() {
            return new ReadOnlyDoubleSet(delegate.copy());
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            if (obj == this || obj == delegate) {
                return true;
            } else if (obj == null) {
                return false;
            }
            return delegate.equals(obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    static class SynchronizedDoubleSet implements DoubleSet {

        private final DoubleSet delegate;

        public SynchronizedDoubleSet(DoubleSet delegate) {
            this.delegate = delegate;
        }

        public synchronized void trim() {
            delegate.trim();
        }

        @Override
        public synchronized DoubleSet copy() {
            return new SynchronizedDoubleSet(delegate.copy());
        }

        @Override
        public synchronized void add(double value) {
            delegate.add(value);
        }

        @Override
        public synchronized void addAll(DoubleSet set) {
            delegate.addAll(set);
        }

        @Override
        public synchronized void addAll(double[] doubles) {
            delegate.addAll(doubles);
        }

        @Override
        public synchronized void addAll(float[] floats) {
            delegate.addAll(floats);
        }

        @Override
        public synchronized void clear() {
            delegate.clear();
        }

        @Override
        public synchronized boolean contains(double d) {
            return delegate.contains(d);
        }

        @Override
        public synchronized void forEachDouble(DoubleConsumer dc) {
            delegate.forEachDouble(dc);
        }

        @Override
        public synchronized void forEachReversed(DoubleConsumer dc) {
            delegate.forEachReversed(dc);
        }

        @Override
        public synchronized double getAsDouble(int index) {
            return delegate.getAsDouble(index);
        }

        @Override
        public synchronized double greatest() {
            return delegate.greatest();
        }

        @Override
        public synchronized int indexOf(double d) {
            return delegate.indexOf(d);
        }

        @Override
        public synchronized boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public synchronized PrimitiveIterator.OfDouble iterator() {
            return new SyIt(this, delegate.iterator());
        }

        static class SyIt implements PrimitiveIterator.OfDouble {

            private final Object lock;
            private final PrimitiveIterator.OfDouble it;

            public SyIt(Object lock, OfDouble it) {
                this.lock = lock;
                this.it = it;
            }

            @Override
            public double nextDouble() {
                synchronized (lock) {
                    return it.next();
                }
            }

            @Override
            public boolean hasNext() {
                synchronized (lock) {
                    return it.hasNext();
                }
            }

        }

        @Override
        public synchronized double least() {
            return delegate.least();
        }

        @Override
        public synchronized int nearestIndexTo(double approximateValue, Bias bias) {
            return delegate.nearestIndexTo(approximateValue, bias);
        }

        @Override
        public synchronized double nearestValueTo(double approximateValue, double tolerance) {
            return delegate.nearestValueTo(approximateValue, tolerance);
        }

        @Override
        public synchronized double nearestValueExclusive(double approximateValue) {
            return delegate.nearestValueExclusive(approximateValue);
        }

        @Override
        public synchronized double nearestValueExclusive(double approximateValue, double tolerance) {
            return delegate.nearestValueExclusive(approximateValue, tolerance);
        }

        @Override
        public synchronized double nearestValueTo(double approximateValue) {
            return delegate.nearestValueTo(approximateValue);
        }

        @Override
        public synchronized DoubleSet[] partition(int maxPartitions) {
            return delegate.partition(maxPartitions);
        }

        @Override
        public synchronized double range() {
            return delegate.range();
        }

        @Override
        public synchronized boolean remove(double val) {
            return delegate.remove(val);
        }

        @Override
        public synchronized void removeAll(double... doubles) {
            delegate.removeAll(doubles);
        }

        @Override
        public synchronized void removeAll(DoubleSet remove) {
            delegate.removeAll(remove);
        }

        @Override
        public synchronized void retainAll(DoubleSet retain) {
            delegate.retainAll(retain);
        }

        @Override
        public synchronized int size() {
            return delegate.size();
        }

        @Override
        public synchronized double[] toDoubleArray() {
            return delegate.toDoubleArray();
        }

        @Override
        public synchronized DoubleSet unmodifiableView() {
            return new ReadOnlyDoubleSet(this);
        }

        @Override
        public synchronized DoubleSet toReadOnlyCopy() {
            return delegate.toReadOnlyCopy();
        }

        @Override
        public synchronized int removeRange(double least, double greatest) {
            return delegate.removeRange(least, greatest);
        }

        @Override
        public synchronized DoubleSet subset(double least, double greatest) {
            return delegate.subset(least, greatest);
        }

        @Override
        public synchronized String toString() {
            return delegate.toString();
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            if (o == this || o == delegate) {
                return true;
            } else if (o == null) {
                return false;
            } else {
                synchronized (this) {
                    return delegate.equals(o);
                }
            }
        }

        @Override
        public synchronized int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public DoubleSet toSynchronizedSet() {
            return this;
        }
    }
}
