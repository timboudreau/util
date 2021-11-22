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
package com.mastfrog.bits;

import static com.mastfrog.bits.Bits.Characteristics.LONG_VALUED;
import static com.mastfrog.bits.Bits.Characteristics.NEGATIVE_VALUES_ALLOWED;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 * A Bits which is not a BitSet at all, but a wrapper for a TreeSet&lt;Long&gt;
 * - useful for some cases of potentially very large, but sparse, sets.
 *
 * @author Tim Boudreau
 */
final class LongSetBits implements MutableBits {

    private final TreeSet<Long> set;

    private static final Set<Characteristics> CHARACTERISTICS =
            Collections.unmodifiableSet(EnumSet.of(NEGATIVE_VALUES_ALLOWED,
                    LONG_VALUED));

    LongSetBits(Bits bits) {
        set = new TreeSet<>();
        if (bits instanceof LongSetBits) {
            set.addAll(((LongSetBits) bits).set);
        } else {
            bits.forEachLongSetBitAscending(set::add);
        }
    }

    LongSetBits() {
        set = new TreeSet<>();
    }

    LongSetBits(long... items) {
        set = new TreeSet<>();
        for (long l : items) {
            set.add(l);
        }
    }

    LongSetBits(Collection<? extends Long> set) {
        this.set = new TreeSet<>(set);
    }

    @Override
    public MutableBits newBits(int size) {
        return new LongSetBits();
    }

    @Override
    public MutableBits newBits(long size) {
        if (size < Integer.MAX_VALUE) {
            return new LongSetBits();
        }
        return MutableBits.super.newBits(size);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CHARACTERISTICS;
    }

    public boolean hasIntegerBounds() {
        return minLong() >= Integer.MIN_VALUE && maxLong() <= Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        }
        return contentEquals((Bits) o);
    }

    @Override
    public int hashCode() {
        return bitsHashCode();
    }

    @Override
    public String toString() {
        return set.toString();
    }

    @Override
    public void set(int bitIndex, boolean value) {
        this.set((long) bitIndex, value);
    }

    @Override
    public int cardinality() {
        return set.size();
    }

    @Override
    public Bits copy() {
        TreeSet<Long> nue = new TreeSet<>(set);
        return new LongSetBits(nue);
    }

    @Override
    public MutableBits mutableCopy() {
        TreeSet<Long> nue = new TreeSet<>(set);
        return new LongSetBits(nue);
    }

    @Override
    public boolean get(int bitIndex) {
        return this.get((long) bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        Long ceil = set.ceiling((long) (fromIndex + 1));
        if (ceil == null) {
            return fromIndex + 1;
        }
        for (;;) {
            Long test = ceil + 1;
            Long nextCeil = set.ceiling(test);
            if (nextCeil == null || nextCeil > test) {
                return test.intValue();
            }
            ceil = test;
        }
    }

    @Override
    public int nextSetBit(int fromIndex) {
        Long ceil = set.ceiling((long) (fromIndex + 1));
        if (ceil == null) {
            return fromIndex - 1;
        }
        return ceil.intValue();
    }

    @Override
    public int previousClearBit(int fromIndex) {
        Long floor = set.floor((long) (fromIndex - 1));
        if (floor == null) {
            return fromIndex - 1;
        }
        for (;;) {
            Long test = floor - 1;
            Long val = set.floor(test);
            if (val == null || val < test) {
                return test.intValue();
            }
        }
    }

    @Override
    public int previousSetBit(int fromIndex) {
        Long floor = set.floor((long) (fromIndex));
        return floor == null ? -1 : floor.intValue();
    }

    @Override
    public void set(long bitIndex, boolean value) {
        set.add(bitIndex);
    }

    @Override
    public void clear(long bitIndex) {
        set.remove(bitIndex);
    }

    @Override
    public void flip(long bitIndex) {
        if (set.contains(bitIndex)) {
            set.remove(bitIndex);
        } else {
            set.add(bitIndex);
        }
    }

    @Override
    public void set(long bitIndex) {
        set.add(bitIndex);
    }

    @Override
    public boolean get(long bitIndex) {
        return set.contains(bitIndex);
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        TreeSet<Long> nue = new TreeSet<>();
        while (fromIndex < toIndex) {
            Long ceil = set.ceiling(fromIndex);
            if (ceil == null) {
                break;
            } else {
                nue.add(ceil);
                fromIndex = ceil;
            }
        }
        return new LongSetBits(nue);
    }

    @Override
    public boolean intersects(Bits other) {
        if (other instanceof LongSetBits) {
            LongSetBits lsb = (LongSetBits) other;
            for (Long bit : set) {
                if (lsb.set.contains(bit)) {
                    return true;
                }
            }
            return false;
        }
        return MutableBits.super.intersects(other);
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        Long floor = set.floor(fromIndex);
        return floor == null ? -1 : floor;
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        if (!set.contains(fromIndex)) {
            return fromIndex;
        }
        Long floor = set.floor(fromIndex - 1L);
        if (floor == null || fromIndex - floor > 1) {
            return fromIndex - 1;
        }
        for (;;) {
            Long test = floor - 1;
            Long val = set.floor(test);
            if (val == null || val < test) {
                return test;
            }
            floor = val;
        }
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        Long ceil = set.ceiling((long) (fromIndex));
        if (ceil == null || ceil < fromIndex) {
            return -1L;
        }
        return ceil;
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        if (!set.contains(fromIndex)) {
            return fromIndex;
        }
        Long ceil = set.ceiling((long) (fromIndex + 1));
        if (ceil == null || ceil - fromIndex > 1) {
            return fromIndex + 1;
        }
        for (;;) {
            Long test = ceil + 1;
            Long nextCeil = set.ceiling(test);
            if (nextCeil == null || nextCeil > test) {
                return test;
            }
            ceil = test;
        }
    }

    @Override
    public long[] toLongArray() {
        long[] result = new long[set.size()];
        Iterator<Long> it = set.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    @Override
    public long minLong() {
        return set.first();
    }

    @Override
    public long maxLong() {
        return set.last();
    }

    @Override
    public long forEachLongSetBitAscending(LongConsumer consumer) {
        if (set.isEmpty()) {
            return 0;
        }
        long ct = 0;
        for (Iterator<Long> it = set.iterator(); it.hasNext();) {
            consumer.accept(it.next());
            ct++;
        }
        return ct;
    }

    @Override
    public void forEachLongSetBitDescending(LongConsumer consumer) {
        if (set.isEmpty()) {
            return;
        }
        Long last;
        last = set.floor(Long.MAX_VALUE);
        while (last != null) {
            consumer.accept(last);
            last = set.floor(last - 1);
        }
    }

    @Override
    public long forEachLongSetBitAscending(LongPredicate consumer) {
        if (set.isEmpty()) {
            return 0;
        }
        long ct = 0;
        for (Iterator<Long> it = set.iterator(); it.hasNext();) {
            if (!consumer.test(it.next())) {
                break;
            }
            ct++;
        }
        return ct;
    }

    @Override
    public long forEachLongSetBitDescending(LongPredicate consumer) {
        if (set.isEmpty()) {
            return 0;
        }
        Long last;
        last = set.floor(Long.MAX_VALUE);
        int ct;
        for (ct = 1; last != null; ct++) {
            consumer.test(last);
            last = set.floor(last - 1);
        }
        return ct;
    }

    @Override
    public boolean canContain(int index) {
        return true;
    }

    @Override
    public MutableBits xorWith(Bits other) {
        TreeSet<Long> nue = new TreeSet<Long>();
        other.forEachLongSetBitAscending(lng -> {
            if (!get(lng)) {
                nue.add(lng);
            }
        });
        forEachLongSetBitAscending(lng -> {
            if (!other.get(lng)) {
                nue.add(lng);
            }
        });
        return new LongSetBits(nue);
    }

    @Override
    public MutableBits andWith(Bits other) {
        if (isEmpty()) {
            return new LongSetBits();
        }
        TreeSet<Long> nue = new TreeSet<Long>(set);
        if (other instanceof LongSetBits) {
            LongSetBits lsb = (LongSetBits) other;
            nue.retainAll(lsb.set);
        }
        return new LongSetBits(nue);
    }

    @Override
    public MutableBits orWith(Bits other) {
        if (isEmpty()) {
            return new LongSetBits(other);
        }
        TreeSet<Long> nue = new TreeSet<Long>(set);
        if (other instanceof LongSetBits) {
            nue.addAll(((LongSetBits) other).set);
        } else {
            other.forEachLongSetBitAscending(nue::add);
        }
        return new LongSetBits(nue);
    }

    @Override
    public MutableBits andNotWith(Bits other) {
        if (other == this || other.isEmpty()) {
            return new LongSetBits();
        } else if (other instanceof LongSetBits) {
            TreeSet<Long> set = new TreeSet<>(this.set);
            set.removeAll(((LongSetBits) other).set);
            return new LongSetBits(set);
        } else {
            TreeSet<Long> set = new TreeSet<>();
            for (Long val : this.set) {
                if (!other.get(val)) {
                    set.add(val);
                }
            }
            return new LongSetBits(set);
        }
    }

    @Override
    public void andNot(Bits other) {
        if (other == this || other.isEmpty()) {
            clear();
        } else if (other instanceof LongSetBits) {
            LongSetBits lsb = (LongSetBits) other;
            set.removeAll(lsb.set);
        } else {
            for (Iterator<Long> it=set.iterator(); it.hasNext();) {
                Long val = it.next();
                if (other.get(val)) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void and(Bits other) {
        if (other == this) {
            return;
        } else if (other.isEmpty()) {
            clear();
        } else if (other instanceof LongSetBits) {
            LongSetBits lsb = (LongSetBits) other;
            set.retainAll(lsb.set);
        } else {
            for (Iterator<Long> it = set.iterator(); it.hasNext();) {
                if (!other.get(it.next())) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void or(Bits other) {
        if (other.isEmpty() || other == this) {
            return;
        }
        if (other instanceof LongSetBits) {
            LongSetBits lsb = (LongSetBits) other;
            set.addAll(lsb.set);
        } else {
            other.forEachLongSetBitAscending(lng -> {
                set.add(lng);
            });
        }
    }

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public void clear(int bitIndex) {
        set.remove((long) bitIndex);
    }

    @Override
    public boolean isNativelyLongIndexed() {
        return true;
    }

    @Override
    public long cardinalityLong() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contentEquals(Bits other) {
        if (other instanceof LongSetBits) {
            return ((LongSetBits) other).set.equals(set);
        }
        return MutableBits.super.contentEquals(other);
    }

    @Override
    public int min() {
        long ml = minLong();
        if (ml < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) ml;
    }

    @Override
    public int max() {
        long ml = maxLong();
        if (ml > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) ml;
    }

    @Override
    public LongSetBits shift(int by) {
        if (by == 0) {
            return new LongSetBits(new TreeSet<>(set));
        }
        TreeSet<Long> nue = new TreeSet<>();
        for (Long val : set) {
            nue.add(val + by);
        }
        return new LongSetBits(nue);
    }

    @Override
    public LongSupplier asLongSupplier() {
        Iterator<Long> it = set.iterator();
        return () -> {
            if (it.hasNext()) {
                return it.next();
            }
            return -1L;
        };
    }

    @Override
    public int length() {
        if (set.isEmpty()) {
            return 0;
        }
        return set.last().intValue() + 1;
    }

    @Override
    public LongSetBits get(int fromIndex, int toIndex) {
        Long item = set.ceiling((long) fromIndex);
        if (item == null || item >= toIndex) {
            return new LongSetBits();
        }
        TreeSet<Long> result = new TreeSet<>();
        result.add(item);
        for (item = set.ceiling(item + 1); item < toIndex; item = set.ceiling(item + 1)) {
            result.add(item);
        }
        return new LongSetBits(result);
    }
}
