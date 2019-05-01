package com.mastfrog.bits;

/**
 *
 * @author Tim Boudreau
 */
interface MutableBitSetBacked extends BitSetBacked, MutableBits {

    @Override
    default MutableBits get(int fromIndex, int toIndex) {
        return new MutableBitSetBits(bitSetUnsafe().get(fromIndex, toIndex));
    }

    @Override
    default void flip(int bitIndex) {
        bitSetUnsafe().flip(bitIndex);
    }

    @Override
    default void flip(int fromIndex, int toIndex) {
        bitSetUnsafe().flip(fromIndex, toIndex);
    }

    @Override
    default void set(int bitIndex) {
        bitSetUnsafe().set(bitIndex);
    }

    @Override
    default void set(int bitIndex, boolean value) {
        bitSetUnsafe().set(bitIndex, value);
    }

    @Override
    default void set(int fromIndex, int toIndex) {
        bitSetUnsafe().set(fromIndex, toIndex);
    }

    @Override
    default void set(int fromIndex, int toIndex, boolean value) {
        bitSetUnsafe().set(fromIndex, toIndex, value);
    }

    @Override
    default void clear(int bitIndex) {
        bitSetUnsafe().clear(bitIndex);
    }

    @Override
    default void clear(int fromIndex, int toIndex) {
        bitSetUnsafe().clear(fromIndex, toIndex);
    }

    @Override
    default void clear() {
        bitSetUnsafe().clear();
    }

    @Override
    default void and(Bits set) {
        if (set instanceof BitSetBacked) {
            bitSetUnsafe().and(((BitSetBacked) set).bitSetUnsafe());
            return;
        }
        MutableBits.super.and(set);
    }

    @Override
    default void or(Bits set) {
        if (set instanceof BitSetBacked) {
            bitSetUnsafe().or(((BitSetBacked) set).bitSetUnsafe());
            return;
        }
        MutableBits.super.or(set);
    }

    @Override
    default void xor(Bits set) {
        if (set instanceof BitSetBacked) {
            bitSetUnsafe().xor(((BitSetBacked) set).bitSetUnsafe());
            return;
        }
        MutableBits.super.xor(set);
    }

    @Override
    default void andNot(Bits set) {
        if (set instanceof BitSetBacked) {
            bitSetUnsafe().andNot(((BitSetBacked) set).bitSetUnsafe());
            return;
        }
        MutableBits.super.andNot(set);
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
}
