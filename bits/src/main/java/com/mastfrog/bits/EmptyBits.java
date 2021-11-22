package com.mastfrog.bits;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
final class EmptyBits implements Bits, LongSupplier, IntSupplier, MutableBits {

    static final MutableBits INSTANCE = new EmptyBits();

    EmptyBits() {
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
    public MutableBits get(int fromIndex, int toIndex) {
        return mutableCopy();
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

    @Override
    public boolean equals(Object o) {
        return o == this ? true
                : o == null ? false
                : o instanceof EmptyBits ? true
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

    @Override
    public LongSupplier asLongSupplier() {
        return this;
    }

    @Override
    public IntSupplier asIntSupplier() {
        return this;
    }

    @Override
    public int getAsInt() {
        return -1;
    }

    @Override
    public long getAsLong() {
        return -1;
    }

    @Override
    public MutableBits orWith(Bits other) {
        return other.mutableCopy();
    }

    @Override
    public MutableBits andWith(Bits other) {
        return this;
    }

    @Override
    public int bitsHashCode() {
        return 1234;
    }

    @Override
    public Bits filter(IntPredicate pred) {
        return this;
    }

    @Override
    public BitSet toBitSet() {
        return new BitSet(0);
    }

    @Override
    public MutableBits xorWith(Bits other) {
        return other.mutableCopy();
    }

    @Override
    public double sum(IntToDoubleFunction f) {
        return 0D;
    }

    @Override
    public double sum(DoubleLongFunction f) {
        return 0D;
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        return this;
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        return -1L;
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        return -1L;
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        return -1L;
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        return -1L;
    }

    @Override
    public void set(int bitIndex, boolean value) {
        throw new UnsupportedOperationException("Bits.EMPTY cannot be added to.");
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.allOf(Characteristics.class);
    }
}
