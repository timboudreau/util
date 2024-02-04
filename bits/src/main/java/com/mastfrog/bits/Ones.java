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

import static com.mastfrog.bits.Bits.Characteristics.FIXED_SIZE;
import static com.mastfrog.bits.Bits.Characteristics.LARGE;
import static com.mastfrog.bits.Bits.Characteristics.LONG_VALUED;
import static com.mastfrog.bits.RLEBitsBuilder.newRleBitsBuilder;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.LongBiConsumer;
import java.util.BitSet;
import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.of;
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
final class Ones implements Bits {

    private static Set<Characteristics> CH = unmodifiableSet(of(LONG_VALUED, LARGE, FIXED_SIZE));
    static final Ones ONES = new Ones();

    private Ones() {
    }

    @Override
    public int cardinality() {
        return Math.max((max() - min()) + 1, max() - min());
    }

    @Override
    public Bits copy() {
        return this;
    }

    @Override
    public MutableBits mutableCopy() {
        BitSet bs = new BitSet();
        bs.set(0, Integer.MAX_VALUE);
        return MutableBits.valueOf(bs);
    }

    @Override
    public Bits orWith(Bits other) {
        return this;
    }

    @Override
    public long cardinalityLong() {
        return Math.max((maxLong() - minLong()) + 1, maxLong() - minLong());
    }

    @Override
    public boolean get(int bitIndex) {
        return true;
    }

    @Override
    public int leastSetBit() {
        return min();
    }

    @Override
    public long leastSetBitLong() {
        return minLong();
    }

    @Override
    public Bits xorWith(Bits other) {
        if (other == this) {
            return Bits.EMPTY;
        }
        return new InvertedBits(other);
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
    public boolean get(long bitIndex) {
        return bitIndex >= 0;
    }

    @Override
    public Bits get(int fromIndex, int toIndex) {
        if (toIndex - fromIndex < 64) {
            BitSet bs = new BitSet(64);
            bs.set(fromIndex, toIndex);
        }
        return newRleBitsBuilder().withRange(fromIndex, toIndex).build();
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        return newRleBitsBuilder().withRange(fromIndex, toIndex).build();
    }

    @Override
    public boolean intersects(Bits set) {
        return !set.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int length() {
        int card = cardinality();
        return Math.max(card, card + 1);
    }

    @Override
    public long longLength() {
        long card = cardinalityLong();
        return Math.max(card, card + 1);
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        return fromIndex >= 0 ? fromIndex : -1;
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        return -1;
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        if (fromIndex < 0) {
            return 0;
        }
        return fromIndex;
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        return -1;
    }

    @Override
    public int min() {
        return 0;
    }

    @Override
    public int max() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long minLong() {
        return 0L;
    }

    @Override
    public long maxLong() {
        return Long.MAX_VALUE;
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return -1;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return Math.max(0, fromIndex);
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return -1;
    }

    @Override
    public int previousSetBit(int fromIndex) {
        if (fromIndex >= 0) {
            return fromIndex;
        }
        return -1;
    }

    @Override
    public boolean contentEquals(Bits other) {
        return other == this || other.nextClearBitLong(other.minLong()) == -1;
    }

    @Override
    public int hashCode() {
        return 1234; // same as a full bit set
    }

    @Override
    public String toString() {
        return "ONES";
    }

    @Override
    public boolean canContain(int index) {
        return index >= 0;
    }

    @Override
    public boolean canContain(long index) {
        return index >= 0;
    }

    @Override
    public BitSet toBitSet() {
        BitSet bs = new BitSet();
        bs.set(0, Integer.MAX_VALUE);
        return bs;
    }

    @Override
    public String stringValue() {
        return toString();
    }

    @Override
    public int visitRanges(IntBiConsumer c) {
        c.accept(min(), max());
        return 1;
    }

    @Override
    public long visitRangesLong(LongBiConsumer c) {
        c.accept(minLong(), maxLong());
        return 1;
    }

    @Override
    public int forEachSetBitAscending(IntConsumer consumer) {
        for (int i = 0; i <= Integer.MAX_VALUE; i++) {
            consumer.accept(i);
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int forEachSetBitDescending(IntConsumer consumer) {
        for (int i = Integer.MAX_VALUE; i >= 0; i--) {
            consumer.accept(i);
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int forEachSetBitAscending(IntPredicate consumer) {
        for (int i = 0; i <= Integer.MAX_VALUE; i++) {
            if (!consumer.test(i)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int forEachSetBitDescending(IntPredicate consumer) {
        for (int i = Integer.MAX_VALUE; i >= 0; i--) {
            if (!consumer.test(i)) {
                return 1 + (Integer.MAX_VALUE - i);
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public long forEachLongSetBitAscending(LongConsumer consumer) {
        for (long i = 0; i <= Long.MAX_VALUE; i++) {
            consumer.accept(i);
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long forEachLongSetBitDescending(LongConsumer consumer) {
        for (long i = Long.MAX_VALUE; i >= 0; i--) {
            consumer.accept(i);
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long forEachLongSetBitAscending(LongPredicate consumer) {
        for (long i = 0; i <= Long.MAX_VALUE; i++) {
            if (!consumer.test(i)) {
                return i + 1;
            }
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long forEachLongSetBitDescending(LongPredicate consumer) {
        for (long i = Long.MAX_VALUE; i >= 0; i--) {
            if (!consumer.test(i)) {
                return 1 + (Long.MAX_VALUE - i);
            }
        }
        return Long.MAX_VALUE;
    }

    @Override
    public int forEachUnsetBitAscending(IntConsumer consumer) {
        return 0;
    }

    @Override
    public int forEachUnsetBitDescending(IntConsumer consumer) {
        return 0;
    }

    @Override
    public int forEachUnsetBitAscending(IntPredicate consumer) {
        return 0;
    }

    @Override
    public int forEachUnsetBitDescending(IntPredicate consumer) {
        return 0;
    }

    @Override
    public long forEachUnsetLongBitAscending(LongConsumer consumer) {
        return 0;
    }

    @Override
    public long forEachUnsetLongBitDescending(LongConsumer consumer) {
        return 0;
    }

    @Override
    public long forEachUnsetLongBitAscending(LongPredicate consumer) {
        return 0;
    }

    @Override
    public long forEachUnsetLongBitDescending(LongPredicate consumer) {
        return 0;
    }

    @Override
    public Bits shift(int by) {
        return this;
    }

    @Override
    public Bits andWith(Bits other) {
        return other;
    }

    @Override
    public Bits andNotWith(Bits other) {
        return new InvertedBits(other);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CH;
    }

    @Override
    public LongSupplier asLongSupplier() {
        return new Seq();
    }

    @Override
    public IntSupplier asIntSupplier() {
        return new Seq();
    }

    private static final class Seq implements IntSupplier, LongSupplier {

        long value = 0;

        @Override
        public int getAsInt() {
            return Math.max(-1, (int) (value++));
        }

        @Override
        public long getAsLong() {
            return Math.max(-1, (int) (value++));
        }
    }
}
