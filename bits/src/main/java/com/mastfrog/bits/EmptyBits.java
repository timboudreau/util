package com.mastfrog.bits;

import java.util.BitSet;

/**
 *
 * @author Tim Boudreau
 */
final class EmptyBits implements Bits {

    static final Bits INSTANCE = new EmptyBits();

    private EmptyBits() {
    }

    @Override
    public int cardinality() {
        return 0;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Bits copy() {
        return this;
    }

    @Override
    public MutableBits mutableCopy() {
        return new MutableBitSetBits(new BitSet(0));
    }

    @Override
    public boolean get(int bitIndex) {
        return false;
    }

    @Override
    public Bits get(int fromIndex, int toIndex) {
        return copy();
    }

    @Override
    public int nextClearBit(int fromIndex) {
        return -1;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        return -1;
    }

    @Override
    public int previousClearBit(int fromIndex) {
        return -1;
    }

    @Override
    public int previousSetBit(int fromIndex) {
        return -1;
    }

    @Override
    public Bits immutableCopy() {
        return this;
    }

    @Override
    public long cardinalityLong() {
        return 0L;
    }

    @Override
    public boolean intersects(Bits set) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public long longLength() {
        return 0L;
    }

    @Override
    public String stringValue() {
        return "[]";
    }

    @Override
    public boolean contentEquals(Bits other) {
        return other.isEmpty();
    }

    public boolean equals(Object o) {
        return o == this ? true
                : o == null ? false
                : o instanceof Bits && ((Bits) o).isEmpty();
    }

    @Override
    public int hashCode() {
        // Same as empty BitSet
        return 1234;
    }

    @Override
    public byte[] toByteArray() {
        return new byte[0];
    }

    @Override
    public long[] toLongArray() {
        return new long[0];
    }
}
