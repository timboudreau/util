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
package com.mastfrog.bits;

import static com.mastfrog.bits.RLEB.BSResultType.*;
import static com.mastfrog.bits.RLEB.CellLookupResultType.*;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.LongBiConsumer;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import com.mastfrog.function.state.Obj;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
final class RLEB implements Bits {

    static final long INT_MAX_UNSIGNED = 0xFFFFFFFFL;
    private static final int SIZE_INCREMENT = 16;
    private long[] data;
    private int used;

    RLEB() {
        data = new long[SIZE_INCREMENT];
    }

    RLEB(RLEB old) {
        this(Arrays.copyOf(old.data, old.data.length), old.used);
    }

    RLEB(long[] data, int used) {
        this.data = data;
        this.used = used;
        assert checkInvariants();
    }

    static RLEB from(Bits orig) {
        Obj<long[]> data = Obj.of(new long[SIZE_INCREMENT]);
        Int used = Int.create();
        Runnable growIfNeeded = () -> {
            if (used.getAsInt() == data.get().length - 1) {
                long[] nue = Arrays.copyOf(data.get(), data.get().length + SIZE_INCREMENT);
                data.set(nue);
            }
            used.increment();
        };
        Lng prev = Lng.of(Long.MIN_VALUE);
        orig.forEachLongSetBitAscending(bit -> {
            long last = prev.get();
            if (last == Long.MIN_VALUE) {
                long val = bit & INT_MAX_UNSIGNED;
                val |= val << 32;
                data.get()[0] = val;
            } else {
                if (bit == last + 1) {
                    long curr = data.get()[0];
                    long start = curr & INT_MAX_UNSIGNED;
                    long end = (bit & INT_MAX_UNSIGNED) << 32;
                    data.get()[used.get() - 1] = start | end;
                } else {
                    growIfNeeded.run();
                    long val = bit & INT_MAX_UNSIGNED;
                    val |= val << 32;
                    data.get()[used.getAsInt() - 1] = val;
                }
            }
        });
        return new RLEB(data.get(), used.getAsInt());
    }

    @Override
    public boolean isEmpty() {
        return used == 0;
    }

    long start(int ix) {
        long val = data[ix];
        return val & INT_MAX_UNSIGNED;
    }

    long end(int ix) {
        return (data[ix] >> 32) & INT_MAX_UNSIGNED;
    }

    void startLen(int ix, LongBiConsumer c) {
        long val = data[ix];
        long start = val & INT_MAX_UNSIGNED;
        long len = ((data[ix] >> 32) & INT_MAX_UNSIGNED) - start;
        assert len >= 0;
        c.accept(start, len);
    }

    void startEnd(int ix, LongBiConsumer c) {
        long val = data[ix];
        long start = val & 0xFFFFFFFF;
        long len = ((data[ix] >> 32) & INT_MAX_UNSIGNED);
        assert len >= 0;
        c.accept(start, len);
    }

    static enum CellLookupResultType {
        EMPTY,
        EXACT,
        CONTAINED,
        ADJACENT_FORWARD,
        ADJACENT_FORWARD_MERGEABLE,
        ADJACENT_BACKWARD,
        NOT_PRESENT;

        boolean isPresent() {
            switch (this) {
                case EXACT:
                case CONTAINED:
                    return true;
                default:
                    return false;
            }
        }
    }

    private static final class CellLookupResult {

        final int index;
        final CellLookupResultType type;

        private static final CellLookupResult EMPTY = new CellLookupResult(-1, CellLookupResultType.EMPTY);

        public CellLookupResult(int index, CellLookupResultType type) {
            this.index = index;
            this.type = type;
        }
    }

    private int len() {
        return used;
    }

    private BSResult binarySearch(long bitIndex) {
        if (isEmpty()) {
            return new BSResult();
        }
        return binarySearch(bitIndex, 0, len() - 1);
    }

    private BSResult binarySearch(long bitIndex, int head, int tail) {
        long[] d = data;
        BSResult result = new BSResult();
        binarySearch(d, bitIndex, head, tail, result);
        return result;
    }

    enum BSResultType {
        HEAD_EXACT_START,
        HEAD_EXACT_END,
        TAIL_EXACT_START,
        TAIL_EXACT_END,
        WITHIN_HEAD,
        WITHIN_TAIL,
        NOT_PRESENT;

        boolean isInner() {
            switch (this) {
                case WITHIN_HEAD:
                case WITHIN_TAIL:
                    return true;
                default:
                    return false;
            }
        }

        boolean isExact() {
            switch (this) {
                case HEAD_EXACT_END:
                case HEAD_EXACT_START:
                case TAIL_EXACT_END:
                case TAIL_EXACT_START:
                    return true;
                default:
                    return false;
            }
        }

        boolean isPresent() {
            return this != NOT_PRESENT;
        }
    }

    static class BSResult {

        int index;
        BSResultType type;
        int nearestMatch = -1;
        long nearestMatchDistance = Long.MAX_VALUE;

        BSResult() {
            this.index = -1;
            this.type = BSResultType.NOT_PRESENT;
        }

        void set(int index, BSResultType type) {
            this.index = index;
            this.type = type;
        }

        void clear() {
            this.index = -1;
            this.type = BSResultType.NOT_PRESENT;
        }

        boolean isPresent() {
            return type.isPresent();
        }

        @Override
        public String toString() {
            return "BSResult{" + "index=" + index + ", type=" + type
                    + ", nearestMatch=" + nearestMatch + ", nearestMatchDistance="
                    + nearestMatchDistance + '}';
        }

    }

    private static void updateDistance(BSResult result, int arrIndex, long bitIndex, long rangeEnd) {
        long dist = bitIndex - rangeEnd;
        if (dist > 0 && dist < result.nearestMatchDistance) {
            result.nearestMatch = arrIndex;
            result.nearestMatchDistance = dist;
        }
    }

    static BSResult binarySearch(long[] d, long bitIndex, int head, int tail) {
        BSResult r = new BSResult();
        binarySearch(d, bitIndex, head, tail, r);
        return r;
    }

    static void binarySearch(long[] d, long bitIndex, int head, int tail,
            BSResult result) {

        // We get better performance by always keeping the data array in a local var
        long rawHead = d[head];
        long headStart = startFrom(rawHead);
        if (bitIndex == headStart) {
            result.set(head, BSResultType.HEAD_EXACT_START);
            return;
        }
        if (bitIndex < headStart) {
            result.clear();
            if (result.nearestMatchDistance == Long.MAX_VALUE) {
                long dist = headStart - bitIndex;
                result.nearestMatchDistance = dist;
                result.nearestMatch = head;
            }
            return;
        }

        long headEnd = endFrom(rawHead);
        if (bitIndex == headEnd) {
            result.set(head, HEAD_EXACT_END);
            return;
        }

        if (bitIndex < headEnd && bitIndex > headStart) {
            result.set(head, WITHIN_HEAD);
            return;
        }

        updateDistance(result, head, bitIndex, headEnd);

        if (head == tail) {
            result.clear();
            return;
        }

        long rawTail = d[tail];

        long tailEnd = endFrom(rawTail);
        if (bitIndex == tailEnd) {
            result.set(tail, TAIL_EXACT_END);
            return;
        }

        if (bitIndex > tailEnd) {
            result.clear();
            updateDistance(result, tail, bitIndex, tailEnd);
            return;
        }

        long tailStart = startFrom(rawTail);
        if (bitIndex == tailStart) {
            result.set(tail, TAIL_EXACT_START);
            return;
        }

        if (bitIndex < tailEnd && bitIndex > tailStart) {
            result.set(tail, WITHIN_TAIL);
            return;
        }

        if (head == tail - 1) {
            result.clear();
            return;
        }

        int newHead = head + 1;
        int mid = head + ((tail - head) / 2);
        binarySearch(d, bitIndex, newHead, mid, result);
        if (!result.isPresent()) {
            binarySearch(d, bitIndex, mid + 1, tail - 1, result);
        }
    }

    static long startFrom(long l) {
        return l & INT_MAX_UNSIGNED;
    }

    static long endFrom(long l) {
        return (l >> 32) & INT_MAX_UNSIGNED;
    }

    CellLookupResult cellForBit(long bitIndex) {
        if (used == 0) {
            return CellLookupResult.EMPTY;
        }

        BSResult br = binarySearch(bitIndex);
        if (br.isPresent()) {
            if (br.type.isExact()) {
                return new CellLookupResult(br.index, CellLookupResultType.EXACT);
            } else {
                return new CellLookupResult(br.index, CellLookupResultType.CONTAINED);
            }
        } else {
            long val = data[br.nearestMatch];
            long last = endFrom(val);
            if (bitIndex == last + 1) {
                if (used > br.nearestMatch + 1) {
                    long nextStart = startFrom(data[br.nearestMatch + 1]);
                    if (nextStart == bitIndex + 2) {
                        return new CellLookupResult(br.nearestMatch, CellLookupResultType.ADJACENT_FORWARD_MERGEABLE);
                    }
                }
                return new CellLookupResult(br.nearestMatch, CellLookupResultType.ADJACENT_FORWARD);
            } else {
                return new CellLookupResult(br.nearestMatch, CellLookupResultType.NOT_PRESENT);
            }
        }
    }

    static void write(long[] d, int arrIndex, long start, long end) { // used by tests
        // and will be needed when / if we implement MutableBits
        assert arrIndex >= 0;
        assert end >= start : "End >= start " + start + ":" + end;
        if (start > INT_MAX_UNSIGNED) {
            throw new IllegalArgumentException("Start too large: " + start);
        }
        if (start < 0) {
            throw new IllegalArgumentException("Start < 0: " + start);
        }
        if (end > INT_MAX_UNSIGNED) {
            throw new IllegalArgumentException("End too large: " + end);
        }
        if (end < 0) {
            throw new IllegalArgumentException("End < 0: " + end);
        }
        d[arrIndex] = (end << 32) | (start & INT_MAX_UNSIGNED);
    }

    /*
    private void write(int arrIndex, long start, long end) {
        write(data, arrIndex, start, end);
    }

    @Override
    public void set(int bitIndex, boolean value) {
        set((long) bitIndex, value);
    }

    @Override
    public void set(long bitIndex, boolean value) {
        CellLookupResult loc = cellForBit(bitIndex);
        if (loc.type.isPresent()) {
            return;
        }
        switch (loc.type) {
            case EMPTY:
                used = 1;
                write(0, bitIndex, bitIndex);
                break;
            case EXACT:
            case CONTAINED:
                return;
            case ADJACENT_BACKWARD:
                startEnd(loc.index, (start, end) -> {
                    assert bitIndex == start - 1;
                    write(loc.index, start - 1, end);
                });
                break;
            case ADJACENT_FORWARD:
                startEnd(loc.index, (start, end) -> {
                    assert bitIndex == end + 1;
                    write(loc.index, start, end + 1);
                });
                break;

        }
    }

    private void merge(int aix) {
        startEnd(aix, (start, oldEnd) -> {
            startEnd(aix + 1, (oldStart, end) -> {
                excise(aix + 1);
                write(aix, start, end);
            });
        });
    }

    private void excise(int arrayIndex) {
        if (used == 1 && arrayIndex == 0) {
            used = 0;
            return;
        }
        System.arraycopy(data, arrayIndex + 1, data, arrayIndex, used - arrayIndex);
    }

    private void insert(int arrayIndex) {
        ensureIndex(arrayIndex);
        System.arraycopy(data, arrayIndex, data, arrayIndex + 1, (used - arrayIndex) - 1);
        data[arrayIndex] = 0;
    }

    private void ensureIndex(int ix) {
        if (ix == 0) {
            return;
        }
        assert ix >= 0;
        if (ix < used) {
            return;
        }
        grow();
    }

    private void grow() {
        int newSize = data.length + SIZE_INCREMENT;
        data = Arrays.copyOf(data, newSize);
    }
     */
    @Override
    public int cardinality() {
        int result = 0;
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = data[i];
            long start = startFrom(val);
            long end = endFrom(val) + 1;
            result += (int) (end - start);
        }
        return result;
    }

    @Override
    public RLEB copy() {
        return new RLEB(this);
    }

    @Override
    public MutableBits mutableCopy() {
        return MutableBits.valueOf(toBitSet());
    }

    @Override
    public boolean get(long bitIndex) {
        long l = leastSetBitLong();
        if (bitIndex < l) {
            return false;
        } else if (bitIndex == l) {
            return true;
        }
        CellLookupResult cell = bitIndex < leastSetBitLong() ? firstCell() : cellForBit(bitIndex);
        return cell.type.isPresent();
    }

    public boolean get(int bitIndex) {
        return get((long) bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return (int) nextClearBit((long) fromIndex);
    }

    public long nextClearBit(long fromIndex) {
        if (isEmpty()) {
            return fromIndex;
        }
        if (fromIndex == 0) {
            long start = data[0] & INT_MAX_UNSIGNED;
            if (start > 0) {
                return 0;
            }
        }
        long max = greatestBitLong();
        if (fromIndex > max) {
            return fromIndex;
        }
        long min = leastSetBitLong();
        if (fromIndex < min) {
            return fromIndex;
        }
        CellLookupResult cell = cellForBit(fromIndex);
        if (!cell.type.isPresent()) {
            return fromIndex;
        }
        return end(cell.index) + 1;
    }

    @Override
    public long forEachLongSetBitAscending(LongConsumer consumer) {
        long result = 0;
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = first; j <= last; j++) {
                consumer.accept(j);
            }
            result += (last - first) + 1;
        }
        return result;
    }

    @Override
    public long forEachLongSetBitAscending(LongPredicate consumer) {
        long result = 0;
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = first; j <= last; j++) {
                if (!consumer.test(j)) {
                    return result + (j - first) + 1;
                }
            }
            result += (last - first) + 1;
        }
        return result;
    }

    @Override
    public void forEachLongSetBitDescending(LongConsumer consumer) {
        if (isEmpty()) {
            return;
        }
        long[] d = data;
        for (int i = used - 1; i >= 0; i--) {
            long val = d[i];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = last; j >= first; j--) {
                consumer.accept(j);
            }
        }
    }

    private CellLookupResult lastCell() {
        return new CellLookupResult(used - 1, CellLookupResultType.EXACT);
    }

    @Override
    public long forEachLongSetBitDescending(long start, LongConsumer consumer) {
        if (isEmpty()) {
            return 0;
        }
        long max = greatestBitLong();
        boolean isAfterLast = start > max;
        CellLookupResult res = isAfterLast ? lastCell() : cellForBit(start);
        long result = 0L;
        long[] d = data;
        for (int entry = res.index; entry >= 0; entry--) {
            long val = d[entry];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = last; j >= first; j--) {
                consumer.accept(j);
            }
            result += (last - first) + 1;
        }
        return result;
    }

    @Override
    public long forEachLongSetBitDescending(long start, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        long max = greatestBitLong();
        boolean isAfterLast = start > max;
        CellLookupResult res = isAfterLast ? lastCell() : cellForBit(start);
        long result = 0L;
        long[] d = data;
        for (int entry = res.index; entry >= 0; entry--) {
            long val = d[entry];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = last; j >= first; j--) {
                result++;
                if (!consumer.test(j)) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public long forEachLongSetBitDescending(long start, long downTo, LongConsumer consumer) {
        return forEachLongSetBitDescending(start, downTo, ln -> {
            consumer.accept(ln);
            return true;
        });
    }

    @Override
    public long forEachLongSetBitDescending(long start, long downTo, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        long max = greatestBitLong();
        boolean isAfterLast = start > max;
        CellLookupResult res = isAfterLast ? lastCell() : cellForBit(start);
        long result = 0L;
        long[] d = data;
        for (int entry = res.index; entry >= 0; entry--) {
            long val = d[entry];
            long first = startFrom(val);
            long last = Math.max(downTo, endFrom(val));
            for (long j = last; j >= first; j--) {
                result++;
                if (!consumer.test(j)) {
                    break;
                }
            }
            if (last == downTo) {
                break;
            }
        }
        return result;
    }

    @Override
    public long forEachLongSetBitDescending(LongPredicate consumer) {
        long result = 0;
        long[] d = data;
        for (int i = used - 1; i >= 0; i--) {
            long val = d[i];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = last; j >= first; j--) {
                if (!consumer.test(j)) {
                    return result + (j - first) + 1;
                }
            }
            result += (last - first) + 1;
        }
        return result;
    }

    @Override
    public long forEachLongSetBitAscending(long from, LongConsumer consumer) {
        return forEachLongSetBitAscending(from, bit -> {
            consumer.accept(bit);
            return true;
        });
    }

    public long forEachLongSetBitAscending(long from, LongPredicate consumer) {
        if (from <= 0 || from < leastSetBitLong()) {
            return forEachLongSetBitAscending(consumer);
        }
        CellLookupResult cell = cellForBit(from);
        long[] d = data;
        long result = 0;
        if (cell.type.isPresent()) {
            long currCellEnd = endFrom(d[cell.index]);
            for (long i = from; i <= currCellEnd; i++) {
                if (!consumer.test(i)) {
                    return (i - from) + 1;
                }
            }
            result += (currCellEnd + 1) - from;
        }
        for (int i = cell.index + 1; i < used; i++) {
            long val = d[i];
            long first = startFrom(val);
            long last = endFrom(val);
            for (long j = last; j >= first; j--) {
                if (!consumer.test(j)) {
                    return result + (j - first) + 1;
                }
            }
            result += (last - first) + 1;

        }
        return result;
    }

    private CellLookupResult firstCell() {
        return new CellLookupResult(0, CellLookupResultType.EXACT);
    }

    public long forEachLongSetBitAscending(long from, long to, LongPredicate consumer) {
        if (isEmpty()) {
            return 0L;
        }
        if (from > to) {
            return 0L;
        }
        long min = leastSetBitLong();
        boolean isBeforeStart = from < min;
        if (isBeforeStart && to > greatestBitLong()) {
            return forEachLongSetBitAscending(consumer);
        } else if (isBeforeStart) {
            from = min;
        }
        CellLookupResult cell = from == min ? firstCell() : cellForBit(from);
        long[] d = data;
        long result = 0;

        if (cell.type.isPresent()) {
            long currCellEnd = Math.min(endFrom(d[cell.index]), to - 1);
            for (long i = from; i <= currCellEnd; i++) {
                result++;
                if (!consumer.test(i)) {
                    return (i - from) + 1;
                }
            }
            if (to - 1 == currCellEnd) {
                return result;
            }
        }

        for (int i = cell.index + 1; i < used; i++) {
            long val = d[i];
            long first = startFrom(val);

            long last = Math.min(to - 1, endFrom(val));
            for (long j = first; j <= last; j++) {
                result++;
                if (!consumer.test(j)) {
                    return result + (j - first) + 1;
                }
            }
        }
        return result;
    }

    @Override
    public int forEachSetBitAscending(IntConsumer consumer) {
        return (int) this.forEachLongSetBitAscending((long val) -> {
            consumer.accept((int) val);
        });
    }

    @Override
    public int forEachSetBitDescending(IntConsumer consumer) {
        Int result = Int.create();
        this.forEachLongSetBitDescending((long val) -> {
            consumer.accept((int) val);
            result.increment();
        });
        return result.getAsInt();
    }

    @Override
    public int forEachSetBitAscending(IntPredicate consumer) {
        return (int) this.forEachLongSetBitAscending((long val) -> {
            return consumer.test((int) val);
        });
    }

    @Override
    public int forEachSetBitDescending(IntPredicate consumer) {
        return (int) this.forEachLongSetBitDescending((long val) -> {
            return consumer.test((int) val);
        });
    }

    @Override
    public boolean isNativelyLongIndexed() {
        return true;
    }

    @Override
    public Bits immutableCopy() {
        return this;
    }

    @Override
    public long cardinalityLong() {
        long result = 0;
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = data[i];
            long start = startFrom(val);
            long end = endFrom(val) + 1;
            result += end - start;
        }
        return result;
    }

    @Override
    public int min() {
        return 0;
    }

    @Override
    public int max() {
        return Integer.MAX_VALUE;
    }

    public int leastSetBit() {
        if (isEmpty()) {
            return -1;
        }
        return (int) startFrom(data[0]);
    }

    @Override
    public long leastSetBitLong() {
        if (isEmpty()) {
            return -1;
        }
        return startFrom(data[0]);
    }

    @Override
    public long minLong() {
        return 0;
    }

    @Override
    public long maxLong() {
        return INT_MAX_UNSIGNED;
    }

    public long greatestBitLong() {
        if (isEmpty()) {
            return -1;
        }
        return endFrom(data[used - 1]);
    }

    @Override
    public Bits shift(int by) {
        if (by == 0) {
            return copy();
        }
        long[] nue = new long[used];
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            long start = Math.max(min(), startFrom(val) + by);
            long stop = Math.min(max(), endFrom(val) + by);
            nue[i] = start | ((stop << 32));
        }
        return new RLEB(nue, used);
    }

    @Override
    public String toString() {
        return stringValue();
    }

    @Override
    public int hashCode() {
        return bitsHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RLEB rle) {
            if (rle.used == used) {
                if (used == 0) {
                    return true;
                }
                return Arrays.equals(data, 0, used, rle.data, 0, used);
            }
        } else if (o instanceof Bits b) {
            return contentEquals(b);
        }
        return false;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return (int) nextSetBitLong(fromIndex);
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return (int) previousClearBitLong(fromIndex);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        return (int) previousSetBitLong(fromIndex);
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        if (isEmpty()) {
            return -1;
        }
        long ml = leastSetBitLong();
        if (fromIndex <= ml) {
            return ml;
        }
        long max = greatestBitLong();
        if (fromIndex > max) {
            return -1;
        } else if (fromIndex == max) {
            return fromIndex;
        }
        CellLookupResult cell = cellForBit(fromIndex);
        if (cell.type.isPresent()) {
            return fromIndex;
        } else if (cell.index < 0 || cell.index >= used - 1) {
            return -1;
        }
        long val = data[cell.index + 1];
        return startFrom(val);
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        if (isEmpty()) {
            return -1;
        }
        long ml = leastSetBitLong();
        if (fromIndex < ml) {
            return -1;
        } else if (fromIndex == ml) {
            return ml;
        }
        long max = greatestBitLong();
        if (fromIndex > max) {
            return max;
        }
        CellLookupResult cell = cellForBit(fromIndex);
        if (cell.type.isPresent()) {
            return fromIndex;
        } else if (cell.index < 0) {
            return -1;
        }
        return endFrom(data[cell.index]);
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        if (fromIndex < 0) {
            return -1;
        } else if (fromIndex < leastSetBitLong()) {
            return fromIndex;
        }
        CellLookupResult cell = fromIndex > greatestBitLong() ? lastCell() : cellForBit(fromIndex);
        if (cell.type.isPresent()) {
            long st = startFrom(data[cell.index]);
            if (st == 0) {
                return -1;
            } else {
                return st - 1;
            }
        } else {
            return fromIndex;
        }
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        if (fromIndex > greatestBitLong()) {
            return fromIndex;
        }
        CellLookupResult cell = fromIndex < leastSetBitLong() ? firstCell() : cellForBit(fromIndex);
        if (cell.type.isPresent()) {
            long end = endFrom(cell.index);
            if (end == Long.MAX_VALUE) {
                return -1;
            }
            return end + 1;
        } else {
            return fromIndex;
        }
    }

    @Override
    public BitSet toBitSet() {
        BitSet result = new BitSet(cardinality());
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            int start = (int) startFrom(val);
            int end = (int) endFrom(val) + 1;
            result.set(start, end);
        }
        return result;
    }

    @Override
    public int forEachSetBitAscending(int from, IntConsumer consumer) {
        return (int) forEachLongSetBitAscending(from,
                bit -> {
                    consumer.accept((int) bit);
                });
    }

    @Override
    public int forEachSetBitDescending(int from, IntConsumer consumer) {
        return (int) forEachLongSetBitDescending(from,
                bit -> {
                    consumer.accept((int) bit);
                });
    }

    @Override
    public int forEachSetBitAscending(int start, IntPredicate consumer) {
        return (int) forEachLongSetBitAscending(start,
                bit -> {
                    return consumer.test((int) bit);
                });
    }

    @Override
    public int forEachSetBitDescending(int start, IntPredicate consumer) {
        return (int) forEachLongSetBitDescending(start,
                bit -> {
                    return consumer.test((int) bit);
                });
    }

    @Override
    public int forEachSetBitAscending(int from, int upTo, IntConsumer consumer) {
        return (int) forEachLongSetBitAscending(from, upTo, lng -> {
            consumer.accept((int) lng);
        });
    }

    @Override
    public int forEachSetBitAscending(int from, int upTo, IntPredicate consumer) {
        return (int) forEachLongSetBitAscending(from, upTo, lng -> {
            return consumer.test((int) lng);
        });
    }

    @Override
    public long forEachLongSetBitAscending(long from, long upTo, LongConsumer consumer) {
        return forEachLongSetBitAscending(from, upTo, lng -> {
            consumer.accept(lng);
            return true;
        });
    }

    @Override
    public boolean canContain(int index) {
        return index >= 0;
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        if (fromIndex < 0 || toIndex < 0) {
            throw new IllegalArgumentException("Negative start or to: "
                    + fromIndex + ", " + toIndex);
        }
        if (fromIndex > toIndex || isEmpty()) {
            return new RLEB();
        }
        long min = leastSetBitLong();
        CellLookupResult res = fromIndex < min
                ? firstCell()
                : cellForBit(fromIndex);

        int index = res.type.isPresent() ? res.index : res.index + 1;
        if (index >= used) {
            return new RLEB();
        }
        long[] d = data;
        long[] nue = new long[SIZE_INCREMENT];
        int newUsed = 0;
        long range = (toIndex - fromIndex) - 1;
        for (int i = index; i < used; i++) {
            long val = d[i];
            long st = startFrom(val);
            if (st >= toIndex) {
                break;
            }
            long first = Math.max(0, st - fromIndex);
            long last = Math.min(range, endFrom(val) - fromIndex);
            int currEntry = newUsed++;
            if (newUsed >= nue.length) {
                nue = Arrays.copyOf(nue, nue.length + SIZE_INCREMENT);
            }
            long value = first | (last << 32);
            nue[currEntry] = value;
            if (last + 1 > range) {
                break;
            }
        }
        return new RLEB(nue, newUsed);
    }

    @Override
    public LS asLongSupplier() {
        return new LS();
    }

    @Override
    public IntSupplier asIntSupplier() {
        return asLongSupplier();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.RLE_COMPRESSED, Characteristics.LONG_VALUED);
    }

    @Override
    public boolean intersects(Bits set) {
        if (isEmpty()) {
            return false;
        }
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            long start = startFrom(val);
            long end = endFrom(val);
            long bit = set.nextSetBitLong(start);
            if (bit <= end) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int length() {
        long result = longLength();
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    @Override
    public long longLength() {
        long mx = greatestBitLong();
        return mx == Long.MAX_VALUE ? Long.MAX_VALUE : mx + 1L;
    }

    @Override
    public int visitRanges(IntBiConsumer c) {
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            c.accept((int) startFrom(val), (int) endFrom(val));
        }
        return used;
    }

    @Override
    public long visitRangesLong(LongBiConsumer c) {
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            c.accept(startFrom(val), endFrom(val));
        }
        return used;
    }

    @Override
    public int forEachSetBitDescending(int from, int downTo, IntConsumer consumer) {
        return (int) forEachLongSetBitDescending(from, downTo, (long lng) -> {
            consumer.accept((int) lng);
        });
    }

    @Override
    public int forEachSetBitDescending(int from, int downTo, IntPredicate consumer) {
        return (int) forEachLongSetBitDescending(from, downTo, (long lng) -> {
            return consumer.test((int) lng);
        });
    }

    @Override
    public int forEachUnsetBitAscending(IntConsumer consumer) {
        Int result = Int.create();
        forEachUnsetLongBitAscending(bit -> {
            result.increment();
            consumer.accept((int) bit);
        });
        return result.getAsInt();
    }

    @Override
    public int forEachUnsetBitDescending(IntConsumer consumer) {
        Int result = Int.create();
        forEachUnsetLongBitAscending(bit -> {
            result.increment();
            consumer.accept((int) bit);
        });
        return result.getAsInt();
    }

    @Override
    public void forEachUnsetLongBitAscending(LongConsumer consumer) {
        if (isEmpty()) {
            return;
        }
        long cursor = 0;
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            long start = startFrom(val);
            for (long j = cursor; j < start; j++) {
                consumer.accept(j);
            }
            long end = endFrom(val);
            cursor = end + 1;
        }
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, LongConsumer consumer) {
        return forEachUnsetLongBitAscending(from, bit -> {
            consumer.accept(bit);
            return true;
        });
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        long max = greatestBitLong();
        if (from >= max) {
            return 0;
        }
        long min = leastSetBitLong();
        long result = 0;

        int startingCell;
        long cursor;
        if (from < min) {
            startingCell = 0;
            cursor = Math.max(minLong(), from);
        } else {
            CellLookupResult res = cellForBit(from);
            startingCell = res.index + 1;
            if (res.type.isPresent()) {
                cursor = end(res.index) + 1;
                startingCell = res.index + 1;
            } else {
                cursor = Math.max(from, end(res.index) + 1);
            }
        }
        long[] d = data;
        for (int i = startingCell; i < used; i++) {
            long val = d[i];
            long st = startFrom(val);
            while (cursor < st) {
                result++;
                if (!consumer.test(cursor)) {
                    break;
                }
                cursor++;
            }
            cursor = endFrom(val) + 1;
        }
        return result;
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, long to, LongConsumer consumer) {
        return forEachUnsetLongBitAscending(from, to, bit -> {
            consumer.accept(bit);
            return true;
        });
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, long to, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        long max = greatestBitLong();
        if (from >= max) {
            return 0;
        }
        long min = leastSetBitLong();
        long result = 0;

        int startingCell;
        long cursor;
        if (from < min) {
            startingCell = 0;
            cursor = Math.max(minLong(), from);
        } else {
            CellLookupResult res = cellForBit(from);
            startingCell = res.index + 1;
            if (res.type.isPresent()) {
                cursor = end(res.index) + 1;
                startingCell = res.index + 1;
            } else {
                cursor = Math.max(from, end(res.index) + 1);
            }
        }
        long[] d = data;
        for (int i = startingCell; i < used; i++) {
            long val = d[i];
            long st = Math.min(to, startFrom(val));
            while (cursor < st) {
                result++;
                if (!consumer.test(cursor)) {
                    break;
                }
                cursor++;
            }
            cursor = endFrom(val) + 1;
            if (cursor >= to) {
                break;
            }
        }
        return result;
    }

    @Override
    public int forEachUnsetBitAscending(int from, IntConsumer consumer) {
        return (int) forEachUnsetLongBitAscending(from, lng -> {
            consumer.accept((int) lng);
        });
    }

    @Override
    public int forEachUnsetBitAscending(int start, IntPredicate consumer) {
        return (int) forEachUnsetLongBitAscending(start, lng -> {
            return consumer.test((int) lng);
        });
    }

    @Override
    public int forEachUnsetBitAscending(int from, int upTo, IntConsumer consumer) {
        return (int) forEachUnsetLongBitAscending(from, upTo, lng -> {
            consumer.accept((int) lng);
        });
    }

    @Override
    public int forEachUnsetBitAscending(int from, int upTo, IntPredicate consumer) {
        return (int) forEachUnsetLongBitAscending(from, upTo, lng -> {
            return consumer.test((int) lng);
        });
    }

    @Override
    public Bits filter(IntPredicate pred) {
        RLEBitsBuilder bldr = RLEBitsBuilder.newRleBitsBuilder();
        forEachSetBitAscending(bit -> {
            if (pred.test(bit)) {
                bldr.withRange(bit, bit);
            }
        });
        return bldr.build();
    }

    @Override
    public void forEachUnsetLongBitDescending(LongConsumer consumer) {
        if (isEmpty()) {
            return;
        }
        long[] d = data;
        long cursor = startFrom(d[used - 1]);
        for (int i = used - 2; i >= 0; i--) {
            long val = d[i];
            long start = endFrom(val);
            for (long j = cursor; j > start; j--) {
                consumer.accept(j);
            }
            long end = startFrom(val);
            cursor = end - 1;
        }
    }

    @Override
    public long forEachUnsetLongBitDescending(LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        long result = 0;
        long[] d = data;
        long cursor = startFrom(d[used - 1]);
        for (int i = used - 2; i >= 0; i--) {
            long val = d[i];
            long start = endFrom(val);
            for (long j = cursor; j > start; j--) {
                result++;
                if (!consumer.test(j)) {
                    break;
                }
            }
            long end = startFrom(val);
            cursor = end - 1;
        }
        return result;
    }

    @Override
    public int forEachUnsetBitAscending(IntPredicate consumer) {
        return (int) forEachUnsetLongBitAscending(bit -> {
            return consumer.test((int) bit);
        });
    }

    @Override
    public int bitsHashCode() {
        long h = 1234L;
        long[] d = data;
        long currentValue = 0L;
        long lastIndexInLongArray = -1;
        for (int i = used - 1; i >= 0; i--) {
            long val = d[i];
            long end = endFrom(val);
            long start = startFrom(val);
            for (long bit = end; bit >= start; bit--) {
                long bitPosition = bit % Long.SIZE;
                long indexInLongArray = bit / Long.SIZE;
                if (indexInLongArray != lastIndexInLongArray && currentValue != 0) {
                    // At a long boundary
                    if (lastIndexInLongArray != -1) {
                        // write last value
                        h ^= currentValue * (lastIndexInLongArray + 1);
                        currentValue = 1L << bitPosition;
                    }
                } else {
                    long v = 1L << bitPosition;
                    currentValue |= v;
                }
                lastIndexInLongArray = indexInLongArray;
            }
        }
        if (currentValue != 0 && lastIndexInLongArray >= 0) {
            h ^= currentValue * (lastIndexInLongArray + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    public RLEBitsBuilder toBuilder() {
        RLEBitsBuilder rlebb = RLEBitsBuilder.newRleBitsBuilder();
        long[] d = data;
        for (int i = 0; i < used; i++) {
            long val = d[i];
            rlebb.withRange(startFrom(val), endFrom(val));
        }
        return rlebb;
    }

    @Override
    public RLEB orWith(Bits other) {
        RLEBitsBuilder rlebb = toBuilder();
        rlebb.add(other);
        return (RLEB) rlebb.build();
    }

    @Override
    public Bits andWith(Bits other) {
        if (other.isEmpty()) {
            return Bits.EMPTY;
        }

        return Bits.super.andWith(other);
    }

    @Override
    public Bits get(int fromIndex, int toIndex) {
        return get((long) fromIndex, (long) toIndex);
    }

    boolean checkInvariants() {
        String result = _checkInvariants();
        boolean ok = result == null;
        assert ok : result;
        return ok;
    }

    private String _checkInvariants() {
        if (isEmpty()) {
            return null;
        }
        long[] d = data;
        long lastStart = startFrom(d[0]);
        long lastEnd = endFrom(d[0]);
        if (lastStart > lastEnd) {
            return "Inverted range " + lastStart + ":" + lastEnd + " @ 0";
        }
        for (int i = 1; i < used; i++) {
            long val = d[i];
            long start = startFrom(val);
            long end = endFrom(val);
            if (start > end) {
                return "Inverted range " + start + ":" + end + " @ " + i;
            }
            if (start <= lastEnd) {
                return "Overlapping or adjacent ranges " + lastStart + ":" + lastEnd + " @ " + (i - 1)
                        + " and " + start + ":" + end + " should have been coalesced at creation time";
            }
        }
        return null;
    }

    private final class LS implements LongSupplier, IntSupplier {

        private int cursor;
        private long cellCursor;
        private long currLast;

        LS() {
            if (isEmpty()) {
                cursor = -1;
                cellCursor = -1;
                currLast = -1;
            } else {
                cellCursor = startFrom(data[0]);
                currLast = endFrom(data[0]);
            }
        }

        @Override
        public long getAsLong() {
            if (cursor < 0 || cursor >= used) {
                return -1;
            }
            long result = cellCursor;
            while (result != -1 && cellCursor++ > currLast && cursor >= 0) {
                if (++cursor < used) {
                    cellCursor = startFrom(data[cursor]);
                    currLast = endFrom(data[cursor]);
                    result = cellCursor;
                } else {
                    cursor = -1;
                    result = -1;
                    break;
                }
            }
            return result;
        }

        @Override
        public int getAsInt() {
            int val = (int) getAsLong();
            return val < 0 ? -1 : val;
        }
    }
}
