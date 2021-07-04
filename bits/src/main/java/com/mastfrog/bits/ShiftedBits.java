package com.mastfrog.bits;

import java.util.BitSet;

/**
 * Bits which simply shifts offsets into the underlying bits.
 *
 * @author Tim Boudreau
 */
class ShiftedBits extends AbstractBits implements MutableBits {

    private final Bits bits;
    private final int shiftBy;

    ShiftedBits(Bits bits, int shiftBy) {
        super(false);
        this.bits = bits;
        this.shiftBy = shiftBy;
    }

    @Override
    public boolean canContain(int index) {
        int ix = shiftBy + index;
        return bits.canContain(ix);
    }

    @Override
    public int min() {
        int bmin = bits.min();
        int diff = bmin - Integer.MIN_VALUE;
        if (shiftBy < 0) {
            return bmin - Math.min(diff, -shiftBy);
        }
        return bmin + shiftBy;
    }

    @Override
    public int max() {
        int bmax = bits.max();
        int diff = Integer.MAX_VALUE - bmax;
        if (shiftBy > 0) {
            return bmax + Math.min(diff, shiftBy);
        }
        return bmax - shiftBy;
    }

    @Override
    public long minLong() {
        long bmin = bits.minLong();
        long diff = bmin - Long.MIN_VALUE;
        if (shiftBy < 0) {
            return bmin - Math.min(diff, -shiftBy);
        }
        return bmin + shiftBy;
    }

    @Override
    public long maxLong() {
        long bmax = bits.maxLong();
        long diff = Long.MAX_VALUE - bmax;
        if (shiftBy > 0) {
            return bmax + Math.min(diff, shiftBy);
        }
        return bmax + shiftBy;
    }

    private MutableBits mutableBits() {
        if (!(bits instanceof MutableBits)) {
            throw new UnsupportedOperationException("Immutable.");
        }
        return (MutableBits) bits;
    }

    private int convert(int index) {
        return index + shiftBy;
    }

    private int unconvert(int index) {
        return index - shiftBy;
    }

    private long convert(long index) {
        return index + shiftBy;
    }

    private long unconvert(long index) {
        return index - shiftBy;
    }

    @Override
    public void set(int bitIndex, boolean value) {
        mutableBits().set(unconvert(bitIndex), value);
    }

    @Override
    public int cardinality() {
        return bits.cardinality();
    }

    @Override
    public Bits copy() {
        return new ShiftedBits(bits.copy(), shiftBy);
    }

    @Override
    public MutableBits mutableCopy() {
        return new ShiftedBits(bits.mutableCopy(), shiftBy);
    }

    @Override
    public boolean get(int bitIndex) {
        return bits.get(unconvert(bitIndex));
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return convert(bits.nextClearBit(unconvert(fromIndex)));
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return convert(bits.nextSetBit(unconvert(fromIndex)));
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return convert(bits.previousClearBit(unconvert(fromIndex)));
    }

    @Override
    public int previousSetBit(int fromIndex) {
        return convert(bits.previousSetBit(unconvert(fromIndex)));
    }

    @Override
    public BitSet toBitSet() {
        BitSet bs = new BitSet(length());
        forEachSetBitAscending(bit -> {
            bs.set(bit);
        });
        return bs;
    }

    @Override
    public boolean isNativelyLongIndexed() {
        return super.isNativelyLongIndexed();
    }

    @Override
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    @Override
    public int length() {
        return bits.length() - shiftBy;
    }

    @Override
    public void set(int bitIndex) {
        mutableBits().set(bitIndex);
    }

    @Override
    public void flip(long fromIndex, long toIndex) {
        mutableBits().flip(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void flip(long bitIndex) {
        mutableBits().flip(unconvert(bitIndex));
    }

    @Override
    public void clear(long fromIndex, long toIndex) {
        mutableBits().clear(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void clear(long bitIndex) {
        mutableBits().clear(unconvert(bitIndex));
    }

    @Override
    public void flip(int fromIndex, int toIndex) {
        mutableBits().flip(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void flip(int bitIndex) {
        mutableBits().flip(unconvert(bitIndex));
    }

    @Override
    public void clear(int fromIndex, int toIndex) {
        mutableBits().clear(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void clear(int bitIndex) {
        mutableBits().clear(unconvert(bitIndex));
    }

    @Override
    public void set(long fromIndex, long toIndex, boolean value) {
        mutableBits().set(unconvert(fromIndex), unconvert(toIndex), value);
    }

    @Override
    public void set(long fromIndex, long toIndex) {
        mutableBits().set(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void set(long bitIndex) {
        mutableBits().set(unconvert(bitIndex));
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        mutableBits().set(unconvert(fromIndex), unconvert(toIndex), value);
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        mutableBits().set(unconvert(fromIndex), unconvert(toIndex));
    }

    @Override
    public void clear() {
        mutableBits().clear();
    }

    @Override
    public void set(long bitIndex, boolean value) {
        mutableBits().set(unconvert(bitIndex), value);
    }

    @Override
    public Bits readOnlyView() {
        if (bits instanceof MutableBits) {
            return new ShiftedBits(mutableBits().readOnlyView(), shiftBy);
        }
        return this;
    }

    @Override
    public Bits shift(int by) {
        if (by == -shiftBy) {
            return bits;
        }
        return new ShiftedBits(bits, shiftBy + by);
    }

    @Override
    public void xor(Bits set) {
        mutableBits().xor(new ShiftedBits(set, -shiftBy));
    }

    @Override
    public void or(Bits set) {
        mutableBits().or(new ShiftedBits(set, -shiftBy));
    }

    @Override
    public void andNot(Bits set) {
        MutableBits.super.andNot(set); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void and(Bits set) {
        mutableBits().and(new ShiftedBits(set, -shiftBy));
    }

    @Override
    public MutableBits get(int fromIndex, int toIndex) {
        return bits.get(unconvert(fromIndex), unconvert(toIndex)).mutableCopy();
    }

    @Override
    public MutableBits andNotWith(Bits other) {
        return new ShiftedBits(bits.andNotWith(new ShiftedBits(other, -shiftBy)), shiftBy);
    }

    @Override
    public MutableBits andWith(Bits other) {
        return new ShiftedBits(bits.andWith(new ShiftedBits(other, -shiftBy)), shiftBy);
    }

    @Override
    public MutableBits xorWith(Bits other) {
        return new ShiftedBits(bits.xorWith(new ShiftedBits(other, -shiftBy)), shiftBy);
    }

    @Override
    public MutableBits orWith(Bits other) {
        return new ShiftedBits(bits.orWith(new ShiftedBits(other, -shiftBy)), shiftBy);
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        return convert(bits.nextClearBitLong(unconvert(fromIndex)));
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        return convert(bits.nextSetBitLong(unconvert(fromIndex)));
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        return convert(bits.previousClearBitLong(unconvert(fromIndex)));
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        return convert(bits.previousSetBitLong(unconvert(fromIndex)));
    }

    @Override
    public long longLength() {
        return bits.longLength() - shiftBy;
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        return bits.get(unconvert(fromIndex), unconvert(toIndex)).mutableCopy();
    }

    @Override
    public boolean get(long bitIndex) {
        return bits.get(unconvert(bitIndex));
    }

    @Override
    public long cardinalityLong() {
        return bits.cardinalityLong();
    }
}
