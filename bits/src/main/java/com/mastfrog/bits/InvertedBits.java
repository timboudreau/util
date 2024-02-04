package com.mastfrog.bits;

import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * A simpler wrapper inverse-bits.
 *
 * @author Tim Boudreau
 */
final class InvertedBits implements Bits {

    private final Bits orig;
    private final IntSupplier capacity;

    public InvertedBits(Bits orig) {
        this(orig, () -> orig.max() - orig.min());
    }

    public InvertedBits(Bits orig, IntSupplier capacity) {
        this.orig = orig;
        this.capacity = capacity;
    }

    @Override
    public int cardinality() {
        return capacity.getAsInt() - orig.cardinality();
    }

    @Override
    public Bits copy() {
        return new InvertedBits(orig.copy(), capacity);
    }

    @Override
    public MutableBits mutableCopy() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean get(int bitIndex) {
        return !orig.get(bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return orig.nextSetBit(fromIndex);
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return orig.nextClearBit(fromIndex);
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return orig.previousSetBit(fromIndex);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        return orig.previousClearBit(fromIndex);
    }

    @Override
    public int hashCode() {
        return bitsHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        }
        if (o instanceof InvertedBits ib) {
            return ib.orig.equals(orig);
        }
        return contentEquals((Bits) o);
    }

    @Override
    public String toString() {
        return stringValue();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return orig.characteristics();
    }

    @Override
    public MutableBits newBits(int size) {
        return orig.newBits(size);
    }

    @Override
    public MutableBits newBits(long size) {
        return orig.newBits(size);
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        return orig.nextClearBitLong(fromIndex);
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        return orig.nextSetBitLong(fromIndex);
    }

    @Override
    public int min() {
        return orig.min();
    }

    @Override
    public int max() {
        return orig.max();
    }

    @Override
    public long minLong() {
        return orig.minLong();
    }

    @Override
    public long maxLong() {
        return orig.maxLong();
    }

    @Override
    public Bits andWith(Bits other) {
        return orig.andNotWith(other);
    }

    @Override
    public Bits andNotWith(Bits other) {
        return orig.andWith(other);
    }

    @Override
    public int forEachSetBitAscending(IntConsumer consumer) {
        return orig.forEachUnsetBitAscending(consumer);
    }

    @Override
    public int forEachSetBitDescending(IntConsumer consumer) {
        return orig.forEachUnsetBitDescending(consumer);
    }

    @Override
    public int forEachSetBitAscending(IntPredicate consumer) {
        return orig.forEachUnsetBitDescending(consumer);
    }

    @Override
    public int forEachSetBitDescending(IntPredicate consumer) {
        return orig.forEachUnsetBitDescending(consumer);
    }

    @Override
    public int forEachUnsetBitAscending(IntConsumer consumer) {
        return orig.forEachSetBitAscending(consumer);
    }

    @Override
    public int forEachUnsetBitAscending(IntPredicate consumer) {
        return orig.forEachSetBitAscending(consumer);
    }

    @Override
    public long forEachLongSetBitAscending(LongConsumer consumer) {
        return orig.forEachUnsetLongBitAscending(consumer);
    }

    @Override
    public long forEachLongSetBitAscending(LongPredicate consumer) {
        return orig.forEachUnsetLongBitAscending(consumer);
    }

    @Override
    public long forEachUnsetLongBitAscending(LongConsumer consumer) {
        return orig.forEachLongSetBitAscending(consumer);
    }

    @Override
    public long forEachUnsetLongBitDescending(LongConsumer consumer) {
        return orig.forEachLongSetBitDescending(consumer);
    }

    @Override
    public long forEachUnsetLongBitAscending(LongPredicate consumer) {
        return orig.forEachLongSetBitAscending(consumer);
    }

    @Override
    public long forEachUnsetLongBitDescending(LongPredicate consumer) {
        return orig.forEachLongSetBitAscending(consumer);
    }

    @Override
    public boolean get(long bitIndex) {
        return !orig.get(bitIndex);
    }

    @Override
    public boolean isEmpty() {
        if (orig.isEmpty()) {
            return false;
        }
        return orig.nextClearBitLong(orig.min()) == -1L;
    }

}
