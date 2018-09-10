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

import com.mastfrog.util.preconditions.Checks;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Manager for the lists of arrays that back LongList and friends.
 *
 * @author Tim Boudreau
 */
abstract class ArraysManager<T> implements Iterable<T> {

    final LinkedList<T> arrays = new LinkedList<>();
    final ArraysPool<T> pool;
    final int batchSize;

    ArraysManager(int batchSize, ArraysPool<T> pool) {
        this.batchSize = Checks.greaterThanOne("batchSize", batchSize);
        this.pool = pool;
    }

    ArraysManager(ArraysManager<T> other, ArraysPool<T> pool) {
        this.batchSize = other.batchSize();
        for (T t : other.arrays) {
            arrays.add(other.duplicate(t));
        }
        this.pool = pool;
    }

    int batchSize() {
        return batchSize;
    }

    public Iterator<T> iterator() {
        return arrays.iterator();
    }

    void clear() {
        for (T arr : arrays) {
            pool.dispose(arr);
        }
        arrays.clear();
    }

    void pruneTo(int size) {
        int targetSize = (size / batchSize) + 1;
        for (int sz = arrays.size(); sz > targetSize + 1 && sz > 0; sz = arrays.size()) {
            T arr = arrays.remove(sz - 1);
            pool.dispose(arr);
        }
    }

    boolean isEmpty() {
        return arrays.isEmpty();
    }

    T get(int index) {
        return arrays.get(index);
    }

    int size() {
        return arrays.size();
    }

    int capacity() {
        return arrays.size() * batchSize;
    }

    void remove(int index) {
        pool.dispose(arrays.remove(index));
    }

    T addOne() {
        T nue = newArray();
        arrays.add(nue);
        return nue;
    }

    T addOne(int index) {
        T nue = newArray();
        arrays.add(index, nue);
        return nue;
    }

    void ensureIndexAvailable(int index) {
        ensureCapacity(index + 1);
    }

    void ensureCapacity(int capacity) {
        while (capacity < capacity()) {
            addOne();
        }
    }

    void removeRange(int fromIndex, int toIndex) {
        if (fromIndex != toIndex) {
            int count = toIndex - fromIndex;
            int offsetIntoAffectedArray = fromIndex % batchSize;
            int firstAffectedArray = fromIndex / batchSize;
            if (count == batchSize && offsetIntoAffectedArray == 0) {
                pool.dispose(arrays.remove(firstAffectedArray));
                return;
            }

            while (count > batchSize + (batchSize - offsetIntoAffectedArray)) {
                pool.dispose(arrays.remove(firstAffectedArray + 1));
                count -= batchSize;
            }
            if (count > 0 && count <= batchSize) {
                shiftOneArrayLeft(firstAffectedArray, offsetIntoAffectedArray, count);
            } else {
                T old = get(firstAffectedArray);
                pool.dispose(arrays.remove(firstAffectedArray));
                int ct = count - batchSize;
                shiftOneArrayLeft(firstAffectedArray, offsetIntoAffectedArray, ct);
                if (offsetIntoAffectedArray > 0) {
                    copy(old, 0, get(firstAffectedArray), 0, offsetIntoAffectedArray);
                }
            }
        }
    }

    void shiftOneArrayLeft(int arrIndex, int start, int by) {
        assert by <= batchSize : "Cannot shift by " + by + " is > batch size " + batchSize;
        T curr = arrays.get(arrIndex);
        int endOffset = start + by;
        int length = batchSize - endOffset;
        if (endOffset < batchSize) {
            copy(curr, endOffset, curr, start, length);
        }
        if (arrIndex != arrays.size() - 1) {
            T next = arrays.get(arrIndex + 1);
            if (endOffset < batchSize) {
                copy(next, 0, curr, batchSize - by, by);
                shiftOneArrayLeft(arrIndex + 1, 0, by);
            } else {
                int copyFrom = by - (batchSize - start);
                int amt = (batchSize - start);
                copy(next, copyFrom, curr, start, amt);
                shiftOneArrayLeft(arrIndex + 1, 0, by);
            }
        }
    }
    static boolean blank = Boolean.getBoolean("unit.test");

    void blank(T arr, int from, int to) {
        if (blank) {
            doBlank(arr, from, to);
        }
    }

    abstract void doBlank(T arr, int from, int to);

    T shiftOneArrayRightFromZero(int arrIndex, int by, boolean shiftPreceding, boolean expand) {
        return shiftOneArrayRightFromZero(arrIndex, by, shiftPreceding, expand, false);
    }

    void removeAt(int index) {
        int arrayIndex = index / batchSize;
        if (arrayIndex >= arrays.size()) {
            throw new IndexOutOfBoundsException("Array index "
                    + arrayIndex + " > " + arrays.size() + " for index " + index
                    + " with arrays.batchSize() " + batchSize);
        }
        shiftOneArrayLeft(arrayIndex, index % batchSize, 1);
    }

    T shiftOneArrayRightFromZero(int arrIndex, int by, boolean shiftPreceding, boolean expand, boolean neverExpand) {
        if (arrIndex > size()) {
            throw new IllegalArgumentException("arrayIndex > size");
        }
        if (neverExpand) {
            expand = false;
        }
        assert by != 0 : "Shift by 0 of " + arrIndex;
        if (by >= batchSize && by % batchSize == 0) {
            // We are shifting by more than batch size from a multiple
            // of batch size, so just add arrays and get out
            int toAdd = (by / batchSize);
            T result = null;
            T realResult = null;
            for (int i = 0; i < toAdd; i++) {
                result = newArray();
                if (realResult == null) {
                    realResult = result;
                }
                blank(result, 0, batchSize);
                arrays.add(arrIndex, result);
            }
            return realResult;
        }
        if (by > batchSize) {
            // We are adding some number that is greater than batch size, but
            // not on a boundary - add any additional arrays
            int toAdd = (by / batchSize);
            shiftOneArrayRightFromZero(arrIndex, by % batchSize, shiftPreceding, expand, neverExpand);
            T result;
            for (int i = 0; i < toAdd; i++) {
                result = newArray();
                blank(result, 0, batchSize);
                arrays.add(arrIndex, result);
            }
            return get(arrIndex);
        }
        boolean last = arrIndex >= size() - 1;
        if (expand && last) {
            // May need to append arrays to make room for shifted elements
            addOne();
            shiftOneArrayRightFromZero(arrIndex + 1, by, true, false, true);
        } else if (!last) {
            // Don't need to append arrays, just shift
            shiftOneArrayRightFromZero(arrIndex + 1, by, true,
                    expand, neverExpand);
        }
        if (arrIndex >= size()) {
            return null;
        }
        T curr = get(arrIndex);
        int tailLength = batchSize - by;
        if (tailLength > 0) {
            copy(curr, 0, curr, by, tailLength);
            blank(curr, 0, by % batchSize);
        }
        if (shiftPreceding && arrIndex > 0) {
            T prev = get(arrIndex - 1);
            copy(prev, batchSize - by, curr, 0, by);
        } else if (!shiftPreceding) {
            blank(curr, 0, by);
        }
        return curr;
    }

    abstract boolean isSorted(T t);

    abstract boolean isSorted(T t, int size);

    T shiftOneArrayRight(int arrIndex, int start, int by) {
        if (start > batchSize) {
            // Normalize to correct array and offset
            arrIndex += start / batchSize;
            start -= batchSize * (start / batchSize);
        }
        // If shifting by a greater number than batch size,
        // shift the next array from zero and then copy in
        // the tail from the current array
        if (by > batchSize) {
            if (start == 0) {
                // Optimized implementation if we're starting from an array start
                return shiftOneArrayRightFromZero(arrIndex, by, false, true, false);
            }
            T arr = get(arrIndex);
            // Get the offset of the insertion end, less the distance
            // from index to the end of the current array
            int shiftForSubsequentArrays = (by - (batchSize - start));
            if (shiftForSubsequentArrays < batchSize) {
                // This branch skips only a couple of tests versus the
                // code below, and could be deleted

                // In this case, we won't be inserting any new arrays
                // - shift right and copy.
                T targetArray = shiftOneArrayRightFromZero(arrIndex + 1, by, true, true, false);
                int endOffset = ((start + by) % batchSize);
                int len = batchSize - endOffset;
                copy(arr, start, targetArray, endOffset, len);
                blank(arr, start, batchSize);
                return targetArray;
            }
            // Shift the next array right
            T targetArray = shiftOneArrayRightFromZero(arrIndex + 1, by, true, true, false);
            // Find the offset of the end of the insert
            int endOffset = ((start + by) % batchSize);
            // This is what we will copy into the target array, before the shift point
            int len = batchSize - start;
            // The target array will differ based on the number of multiples of batchsize
            // it contains
            int targetArrayIndex = arrIndex + (shiftForSubsequentArrays / batchSize) + 1;
            targetArray = get(targetArrayIndex);
            if (len > 0) {
                int copyStart = start;
                if (len + endOffset > batchSize) {
                    // The computed length won't fit, but the tail was already
                    // copied by our shift right from zero
                    len = batchSize - endOffset;
                }
                copy(arr, copyStart, targetArray, endOffset, len);
            }
            // Clear the stuff that should be cleared
            blank(targetArray, 0, endOffset);
            blank(arr, start, batchSize);
            return targetArray;
        }
        assert start <= batchSize : "Start point > batch size: " + start + " arrIndex " + arrIndex + " by " + by;
        assert by <= batchSize : "Cannot shift by " + by + " is > batch size " + batchSize;
        assert by > 0 : "Cannot shift by zero - meaningless argument.  ArrIndex " + arrIndex + " start " + start
                + " by " + by;
        if (start != 0 && start % batchSize == 0 && arrIndex > 0) {
            // On an array boundary, target is the preceding array
            arrIndex--;
        }
        T curr = get(arrIndex);
        int end = (start + by + 1);
        // Special cases
        if (start % batchSize == 0) {
            // insert at zero
            curr = shiftOneArrayRightFromZero(arrIndex, by, false, true, false);
        } else if (end == batchSize) {
            // the range to shift meets the array end; no intra-array copy
            if (end > capacity() - (batchSize * arrIndex)) {
                addOne();
            }
            shiftOneArrayRightFromZero(arrIndex + 1, by, false, true, false);
            if (arrIndex != size() - 1) {
                T next = get(arrIndex + 1);
                int copyStart = start + (batchSize - (end - 1));
                int copyLen = (batchSize - copyStart);
                copy(curr, copyStart, next, 0, copyLen);
            }
            copy(curr, start, curr, start + by, batchSize - (start + by));
        } else if (end > batchSize) {
            // the shift straddles the boundary of at least one pair of arrays
            int nextOff = (end - (batchSize * (arrIndex - 1))) - 1;
            if (nextOff < 0) {
                nextOff = (end - (batchSize * (arrIndex - 2))) - 1;
            }
            shiftOneArrayRightFromZero(arrIndex + 1, by, false, true);
            if (arrIndex != size() - 1) {
                T next = get(arrIndex + 1);
                int copyStart = start;
                int copyLen = (batchSize - copyStart);
                int dest = nextOff % batchSize;
                assert dest >= 0 : "Negative destination " + nextOff + " "
                        + nextOff + " end " + end + " batchSize " + batchSize
                        + " shifting array " + arrIndex + " of " + size()
                        + " by " + by + " boundary " + (batchSize * (arrIndex - 1));

                if (copyLen > 0) {
                    copy(curr, copyStart, next, nextOff % batchSize, copyLen);
                } else if (copyLen < 0) {
                    throw new IllegalStateException("Huh? " + copyStart + " - length " + copyLen);
                }
            }
        } else {
            // Internal shift - we shift subsequent arrays, and then copy
            // some entries to the end location
            if (arrIndex != arrays.size() - 1) {
                shiftOneArrayRightFromZero(arrIndex + 1, by, false, true);
            } else {
                addOne();
                shiftOneArrayRightFromZero(arrIndex + 1, by, false, false, true);
            }
            if (arrIndex != size() - 1) {
                // Copy remaining tail entries into the next array
                T next = get(arrIndex + 1);
                int copyStart = batchSize - by;
                int copyLen = by;
                copy(curr, copyStart, next, 0, copyLen);
            }
            copy(curr, start, curr, end - 1, batchSize - (end - 1));
        }
        int toBlank = start + by >= batchSize ? batchSize - start : by;
        blank(curr, start, start + toBlank);
        return curr;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    void copy(T src, int srcPos, T dest, int destPos, int count) {
        System.arraycopy(src, srcPos, dest, destPos, count);
    }

    final T newArray() {
        return pool.get();
    }

    abstract T duplicate(T val);

    interface SortChecker {

        void sorted(boolean val);
    }

    static final class Longs extends ArraysManager<long[]> {

        public Longs(int batchSize, ArraysPool<long[]> pool) {
            super(batchSize, pool);
        }

        public Longs(int batchSize) {
            super(batchSize, ArraysPool.cachingPool(batchSize, 3, 7));
        }

        public Longs(ArraysManager<long[]> other) {
            super(other, other.pool);
        }

        public Longs(int batchSize, SortChecker checker, long[][] contents) {
            this(batchSize, checker, contents, ArraysPool.cachingPool(batchSize, 3, 7));
        }

        public Longs(int batchSize, SortChecker checker, long[][] contents, ArraysPool<long[]> pool) {
            super(batchSize, pool);
            long[] curr = newArray();
            arrays.add(curr);
            int cursor = 0;
            long prev = Long.MIN_VALUE;
            int ix = 0;
            boolean sorted = true;
            for (long[] arr : contents) {
                for (int i = 0; i < arr.length; i++) {
                    if (cursor == curr.length) {
                        curr = newArray();
                        arrays.add(curr);
                        cursor = 0;
                    }
                    long val = arr[i];
                    curr[cursor++] = val;
                    if (ix > 0) {
                        sorted &= prev < val;
                    }
                    ix++;
                    prev = val;
                }
            }
            if (checker != null) {
                checker.sorted(sorted);
            }
        }

        @Override
        long[] duplicate(long[] val) {
            return Arrays.copyOf(val, val.length);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(size()).append(" arrays batchSize ").append(batchSize).append(":\n[");
            int len = arrays.size();
            for (int i = 0; i < len; i++) {
                long[] l = arrays.get(i);
                for (int j = 0; j < l.length; j++) {
                    String s = Long.toString(l[j]);
                    if (i != 0 && j != 0) {
                        sb.append(' ');
                    }
                    if (s.length() == 1) {
                        sb.append(' ');
                    }
                    sb.append(s);
                }
                if (i != len - 1) {
                    sb.append(" |");
                }
            }
            return sb.append(']').toString();
        }

        void copy(long[] src, int srcPos, long[] dest, int destPos, int count) {
            if (count == 0) {
                String intra = src == dest ? "intra-" : "extra-";
                throw new IllegalArgumentException("Silly argument - " + intra + "copy 0 bytes from "
                        + srcPos + " to " + destPos + " in array of " + src.length);
            }
            try {
                super.copy(src, srcPos, dest, destPos, count);
            } catch (ArrayIndexOutOfBoundsException ex) {
                String intra = src == dest ? "intra-" : "extra-";
                throw new IllegalArgumentException(intra + "copy " + count
                        + " entries from " + srcPos + " to " + destPos
                        + " in arr of " + dest.length + " destPos "
                        + destPos + " count " + count + " - srcEnd = "
                        + (srcPos + count) + ", destEnd = " + (destPos + count), ex);

            }
        }

        void doBlank(long[] arr, int from, int to) {
            Arrays.fill(arr, from, to, -1L);
        }

        long[] toLongArray() {
            return toLongArray(capacity());
        }

        long[] toLongArray(int size) {
            if (arrays.isEmpty()) {
                return new long[0];
            }
            if (arrays.size() == 1) {
                long[] arr = arrays.get(0);
                return Arrays.copyOf(arr, size);
            }
            long[] a = arrays.get(0);
            long[] b = arrays.get(1);
            if (arrays.size() == 2) {
                long[] result = ArrayUtils.concatenate(a, b);
                if (result.length > size) {
                    return Arrays.copyOf(result, size);
                } else {
                    return result;
                }
            }
            long[][] remaining = new long[arrays.size() - 2][];
            for (int i = 2; i < arrays.size(); i++) {
                remaining[i - 2] = arrays.get(i);
            }
            long[] result = ArrayUtils.concatenate(a, b, remaining);
            if (result.length > size) {
                result = Arrays.copyOf(result, size);
            }
            return result;
        }

        @Override
        boolean isSorted(long[] t) {
            return isSorted(t, t.length);
        }

        @Override
        boolean isSorted(long[] t, int size) {
            if (size <= 1) {
                return true;
            }
            if (size == 2) {
                return t[1] > t[0];
            }
            long prevLast = Long.MIN_VALUE, prevFirst = 0;
            int max = size % 2 == 0 ? (size / 2) : (size / 2) + 1;
            for (int i = 0; i < max; i++) {
                int lo = (size - 1) - i;
                if (lo <= i) {
                    return true;
                }
                long last = t[lo];
                long first = t[i];
                if (last <= first) {
                    return false;
                }
                if (lo < max && prevLast >= last) {
                    return false;
                }

                if (i > 0 && prevFirst >= first) {
                    return false;
                }
                prevLast = last;
                prevFirst = first;
            }
            return true;
        }
    }

    static final class Bytes extends ArraysManager<byte[]> {

        public Bytes(int batchSize, ArraysPool<byte[]> pool) {
            super(batchSize, pool);
        }

        public Bytes(ArraysManager<byte[]> other, ArraysPool<byte[]> pool) {
            super(other, pool);
        }

        public Bytes(int batchSize, SortChecker checker, byte[][] contents, ArraysPool<byte[]> pool) {
            super(batchSize, pool);
            byte[] curr = newArray();
            arrays.add(curr);
            int cursor = 0;
            long prev = Long.MIN_VALUE;
            int ix = 0;
            boolean sorted = true;
            for (byte[] arr : contents) {
                for (int i = 0; i < arr.length; i++) {
                    if (cursor == curr.length) {
                        curr = newArray();
                        arrays.add(curr);
                        cursor = 0;
                    }
                    byte val = arr[i];
                    curr[cursor++] = val;
                    if (ix > 0) {
                        sorted &= prev < val;
                    }
                    ix++;
                    prev = val;
                }
            }
            if (checker != null) {
                checker.sorted(sorted);
            }
        }

        byte[] duplicate(byte[] val) {
            return Arrays.copyOf(val, val.length);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(size()).append(" arrays batchSize ").append(batchSize).append(":\n[");
            int len = arrays.size();
            for (int i = 0; i < len; i++) {
                byte[] l = arrays.get(i);
                for (int j = 0; j < l.length; j++) {
                    String s = Long.toString(l[j]);
                    if (i != 0 && j != 0) {
                        sb.append(' ');
                    }
                    if (s.length() == 1) {
                        sb.append(' ');
                    }
                    sb.append(s);
                }
                if (i != len - 1) {
                    sb.append(" |");
                }
            }
            return sb.append(']').toString();
        }

        @Override
        void copy(byte[] src, int srcPos, byte[] dest, int destPos, int count) {
            if (count == 0) {
                String intra = src == dest ? "intra-" : "extra-";
                throw new IllegalArgumentException("Silly argument - " + intra + "copy 0 bytes from "
                        + srcPos + " to " + destPos + " in array of " + src.length);
            }
            try {
                super.copy(src, srcPos, dest, destPos, count);
            } catch (ArrayIndexOutOfBoundsException ex) {
                String intra = src == dest ? "intra-" : "extra-";
                throw new IllegalArgumentException(intra + "copy " + count
                        + " entries from " + srcPos + " to " + destPos
                        + " in arr of " + dest.length + " destPos "
                        + destPos + " count " + count + " - srcEnd = "
                        + (srcPos + count) + ", destEnd = " + (destPos + count), ex);

            }
        }

        @Override
        void doBlank(byte[] arr, int from, int to) {
            Arrays.fill(arr, from, to, (byte) -1);
        }

        byte[] toByteArray() {
            return toByteArray(capacity());
        }

        byte[] toByteArray(int size) {
            if (arrays.isEmpty()) {
                return new byte[0];
            }
            if (arrays.size() == 1) {
                byte[] arr = arrays.get(0);
                return Arrays.copyOf(arr, size);
            }
            byte[] a = arrays.get(0);
            byte[] b = arrays.get(1);
            if (arrays.size() == 2) {
                byte[] result = ArrayUtils.concatenate(a, b);
                if (result.length > size) {
                    return Arrays.copyOf(result, size);
                } else {
                    return result;
                }
            }
            byte[][] remaining = new byte[arrays.size() - 2][];
            for (int i = 2; i < arrays.size(); i++) {
                remaining[i - 2] = arrays.get(i);
            }
            byte[] result = ArrayUtils.concatenate(a, b, remaining);
            if (result.length > size) {
                result = Arrays.copyOf(result, size);
            }
            return result;
        }

        @Override
        boolean isSorted(byte[] t) {
            return isSorted(t, t.length);
        }

        @Override
        boolean isSorted(byte[] t, int size) {
            if (size <= 1) {
                return true;
            }
            if (size == 2) {
                return t[1] > t[0];
            }
            long prevLast = Byte.MIN_VALUE, prevFirst = 0;
            int max = size % 2 == 0 ? (size / 2) : (size / 2) + 1;
            for (int i = 0; i < max; i++) {
                int lo = (size - 1) - i;
                if (lo <= i) {
                    return true;
                }
                byte last = t[lo];
                byte first = t[i];
                if (last <= first) {
                    return false;
                }
                if (lo < max && prevLast >= last) {
                    return false;
                }

                if (i > 0 && prevFirst >= first) {
                    return false;
                }
                prevLast = last;
                prevFirst = first;
            }
            return true;
        }
    }
}
