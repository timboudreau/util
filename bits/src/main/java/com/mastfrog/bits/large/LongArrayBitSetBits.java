package com.mastfrog.bits.large;

import com.mastfrog.bits.Bits;
import com.mastfrog.bits.CloseableBits;
import com.mastfrog.bits.MutableBits;
import java.util.BitSet;
import java.util.Set;

/**
 * Bits wrapper for a LongArrayBitSet.
 *
 * @author Tim Boudreau
 */
public final class LongArrayBitSetBits implements CloseableBits {

    private final LongArrayBitSet bits;

    public LongArrayBitSetBits(LongArrayBitSet bits) {
        this.bits = bits;
    }

    public Set<Characteristics> characteristics() {
        return bits.characteristics();
    }

    public void close() {
        bits.close();
    }

    @Override
    public void set(int bitIndex, boolean value) {
        bits.set(bitIndex, value);
    }

    @Override
    public int cardinality() {
        return (int) bits.cardinality();
    }

    @Override
    public Bits copy() {
        return new LongArrayBitSetBits((LongArrayBitSet) bits.clone());
    }

    @Override
    public MutableBits mutableCopy() {
        return (MutableBits) copy();
    }

    @Override
    public boolean get(int bitIndex) {
        return bits.get(bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return (int) bits.nextClearBit(fromIndex);
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return (int) bits.nextSetBit(fromIndex);
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return (int) bits.previousClearBit(fromIndex);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        return (int) bits.previousSetBit(fromIndex);
    }

    @Override
    public Bits readOnlyView() {
        return this;
    }

    @Override
    public MutableBits get(int fromIndex, int toIndex) {
        return new LongArrayBitSetBits(bits.get(fromIndex, toIndex));
    }

    @Override
    public void set(long bitIndex, boolean value) {
        bits.set(bitIndex, value);
    }

    @Override
    public void and(Bits set) {
        if (set instanceof LongArrayBitSetBits) {
            bits.and(((LongArrayBitSetBits) set).bits);
        }
        CloseableBits.super.and(set);
    }

    @Override
    public void andNot(Bits set) {
        if (set instanceof LongArrayBitSetBits) {
            bits.andNot(((LongArrayBitSetBits) set).bits);
        }
        CloseableBits.super.andNot(set);
    }

    @Override
    public void or(Bits set) {
        if (set instanceof LongArrayBitSetBits) {
            bits.or(((LongArrayBitSetBits) set).bits);
        }
        CloseableBits.super.or(set);
    }

    @Override
    public void xor(Bits set) {
        if (set instanceof LongArrayBitSetBits) {
            bits.xor(((LongArrayBitSetBits) set).bits);
        }
        CloseableBits.super.xor(set);
    }

    @Override
    public void clear() {
        bits.clear();
    }

    @Override
    public void clear(int bitIndex) {
        bits.clear(bitIndex);
    }

    @Override
    public void clear(int fromIndex, int toIndex) {
        bits.clear(fromIndex, toIndex);
    }

    @Override
    public void flip(int bitIndex) {
        bits.flip(bitIndex);
    }

    @Override
    public void flip(int fromIndex, int toIndex) {
        bits.flip(fromIndex, toIndex);
    }

    @Override
    public void set(int bitIndex) {
        bits.set(bitIndex);
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        bits.set(fromIndex, toIndex);
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        bits.set(fromIndex, toIndex, value);
    }

    @Override
    public void clear(long bitIndex) {
        bits.clear(bitIndex);
    }

    @Override
    public void clear(long fromIndex, long toIndex) {
        bits.clear(fromIndex, toIndex);
    }

    @Override
    public void flip(long bitIndex) {
        bits.flip(bitIndex);
    }

    @Override
    public void flip(long fromIndex, long toIndex) {
        bits.flip(fromIndex, toIndex);
    }

    @Override
    public void set(long bitIndex) {
        bits.set(bitIndex);
    }

    @Override
    public void set(long fromIndex, long toIndex) {
        bits.set(fromIndex, toIndex);
    }

    @Override
    public void set(long fromIndex, long toIndex, boolean value) {
        bits.set(fromIndex, toIndex, value);
    }

    @Override
    public BitSet toBitSet() {
        return bits.toBitSet();
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
        return bits.cardinality();
    }

    @Override
    public boolean get(long bitIndex) {
        return bits.get(bitIndex);
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        return new LongArrayBitSetBits(bits.get(fromIndex, toIndex));
    }

    @Override
    public boolean intersects(Bits set) {
        if (set instanceof LongArrayBitSetBits) {
            return bits.intersects(((LongArrayBitSetBits) set).bits);
        }
        return CloseableBits.super.intersects(set);
    }

    @Override
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    @Override
    public int length() {
        return (int) bits.length();
    }

    @Override
    public long longLength() {
        return bits.length();
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        return bits.previousSetBit(fromIndex);
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        return bits.previousClearBit(fromIndex);
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        return bits.nextSetBit(fromIndex);
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        return bits.nextClearBit(fromIndex);
    }

    @Override
    public long[] toLongArray() {
        return bits.toLongArray();
    }

    @Override
    public byte[] toByteArray() {
        return bits.toByteArray();
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
        return 0;
    }

    @Override
    public long maxLong() {
        return Long.MAX_VALUE;
    }
}
