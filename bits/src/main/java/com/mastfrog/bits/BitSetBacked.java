package com.mastfrog.bits;

import java.util.BitSet;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
interface BitSetBacked extends Bits {

    BitSet bitSetUnsafe();

    @Override
    default byte[] toByteArray() {
        return bitSetUnsafe().toByteArray();
    }

    @Override
    default long[] toLongArray() {
        return bitSetUnsafe().toLongArray();
    }

    @Override
    default Bits copy() {
        return isEmpty() ? Bits.EMPTY 
                : this instanceof MutableBits ? new MutableBitSetBits(toBitSet())
                : new BitSetBits(toBitSet());
    }

    default BitSet toBitSet() {
        return (BitSet) bitSetUnsafe().clone();
    }

    @Override
    default boolean get(int bitIndex) {
        return bitSetUnsafe().get(bitIndex);
    }

    @Override
    default Bits get(int fromIndex, int toIndex) {
        return new BitSetBits(bitSetUnsafe().get(fromIndex, toIndex));
    }

    @Override
    default int nextSetBit(int fromIndex) {
        return bitSetUnsafe().nextSetBit(fromIndex);
    }

    @Override
    default int nextClearBit(int fromIndex) {
        if (fromIndex < 0) { // overflow
            return -1;
        }
        return bitSetUnsafe().nextClearBit(fromIndex);
    }

    @Override
    default int previousSetBit(int fromIndex) {
        return bitSetUnsafe().previousSetBit(fromIndex);
    }

    @Override
    default int previousClearBit(int fromIndex) {
        return bitSetUnsafe().previousClearBit(fromIndex);
    }

    @Override
    default int length() {
        return bitSetUnsafe().length();
    }

    @Override
    default boolean isEmpty() {
        return bitSetUnsafe().isEmpty();
    }

    @Override
    default boolean intersects(Bits set) {
        if (set instanceof BitSetBacked) {
            return bitSetUnsafe().intersects(((BitSetBacked) set).bitSetUnsafe());
        }
        return Bits.super.intersects(set);
    }

    @Override
    default int cardinality() {
        return bitSetUnsafe().cardinality();
    }

    default int size() {
        return bitSetUnsafe().size();
    }

    @Override
    default long cardinalityLong() {
        return cardinality();
    }

    @Override
    default long longLength() {
        return length();
    }

    @Override
    default Bits immutableCopy() {
        return new BitSetBits(toBitSet());
    }

    @Override
    default boolean isNativelyLongIndexed() {
        return false;
    }

    @Override
    default MutableBits mutableCopy() {
        return new MutableBitSetBits(toBitSet());
    }

    @Override
    default boolean contentEquals(Bits other) {
        if (other instanceof BitSetBacked) {
            return ((BitSetBacked) other).bitSetUnsafe().equals(bitSetUnsafe());
        }
        return Bits.super.contentEquals(other);
    }

    @Override
    default Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
