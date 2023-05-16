package com.mastfrog.bits;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.BitSet;
import java.util.Collection;

/**
 * Extension to the read-only Bits interface with BitSet's mutation methods,
 * plus long-indexed variants. Contains default implementations for all methods
 * except set(int, boolean) which are functional but non-optimal.
 *
 * @author Tim Boudreau
 */
public interface MutableBits extends Bits {

    public static final MutableBits EMPTY_MUTABLE = new EmptyBits();

    /**
     * Set or clear a bit at the specified int bitIndex, depending on the passed
     * value.
     *
     * @param bitIndex The index of the bit
     * @param value The value
     */
    void set(int bitIndex, boolean value);

    /**
     * Return a wrapper for this MutableBits in which all methods are
     * synchronized, unless this already is such a wrapper.
     *
     * @return A MutableBits whose implementation is synchronized
     */
    default MutableBits toSynchronizedBits() {
        return new SynchronizedMutableBits(this);
    }

    /**
     * Get a read-only view that only implements Bits from this instance.
     *
     * @return A bits
     */
    default Bits readOnlyView() {
        return this;
    }

    public static MutableBits longTreeSetBits() {
        return new LongSetBits();
    }

    public static MutableBits longTreeSetBits(Collection<? extends Long> all) {
        return new LongSetBits(all);
    }

    public static MutableBits longTreeSetBits(long... values) {
        return new LongSetBits(values);
    }

    static MutableBits runLengthEncoded() {
        return new RLEBits();
    }

    public static MutableBits create(int capacity) {
        return new MutableBitSetBits(new BitSet(capacity));
    }

    public static MutableBits valueOf(long[] longs) {
        return new MutableBitSetBits(BitSet.valueOf(longs));
    }

    public static MutableBits valueOf(LongBuffer lb) {
        return new MutableBitSetBits(BitSet.valueOf(lb));
    }

    public static MutableBits valueOf(byte[] bytes) {
        return new MutableBitSetBits(BitSet.valueOf(bytes));
    }

    public static MutableBits valueOf(ByteBuffer bb) {
        return new MutableBitSetBits(BitSet.valueOf(bb));
    }

    public static MutableBits valueOf(BitSet bs) {
        return new MutableBitSetBits(bs);
    }

    /**
     * Logical or's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default MutableBits orWith(Bits other) {
        if (other == this) {
            return mutableCopy();
        }
        MutableBits copy = mutableCopy();
        copy.or(other);
        return copy;
    }

    /**
     * Logical xor's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default MutableBits xorWith(Bits other) {
        MutableBits copy = mutableCopy();
        copy.xor(other);
        return copy;
    }

    /**
     * Logical and's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default MutableBits andWith(Bits other) {
        if (other == this) {
            return mutableCopy();
        }
        MutableBits copy = mutableCopy();
        copy.and(other);
        return copy;
    }

    /**
     * Logical andNot's this and another set, returning a new instance
     * containing the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default MutableBits andNotWith(Bits other) {
        if (other == this) {
            return Bits.EMPTY.mutableCopy();
        }
        MutableBits copy = mutableCopy();
        copy.andNot(other);
        return copy;
    }

    default MutableBits get(int fromIndex, int toIndex) {
        BitSet bs = new BitSet();
        for (int i = fromIndex; i < toIndex; i++) {
            bs.set(i, get(i));
        }
        return MutableBits.valueOf(bs);
    }

    default void set(long bitIndex, boolean value) {
        if (bitIndex > Integer.MAX_VALUE || bitIndex < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Long methods are proxied "
                    + "to ints, but this value is out of range: " + bitIndex);
        }
        set((int) bitIndex, value);
    }

    default void and(Bits set) {
        int length = Math.max(length(), set.length()) + 1;
        for (int i = 0; i < length; i++) {
            if (!set.get(i)) {
                set(i, false);
            }
        }
    }

    default void andNot(Bits set) {
        int length = Math.max(length(), set.length());
        for (int i = 0; i < length; i++) {
            set(i, get(i) && !set.get(i));
        }
    }

    default void or(Bits set) {
        int length = Math.max(length(), set.length()) + 1;
        for (int i = 0; i < length; i++) {
            if (set.get(i)) {
                set(i, true);
            }
        }
    }

    default void xor(Bits set) {
        int length = Math.max(length(), set.length()) + 1;
        for (int i = 0; i < length; i++) {
            if (set.get(i) != get(i)) {
                set(i, true);
            } else {
                set(i, false);
            }
        }
    }

    default void clear() {
        int len = length();
        for (int i = 0; i < len; i++) {
            set(i, false);
        }
    }

    default void clear(int bitIndex) {
        set(bitIndex, false);
    }

    default void clear(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            set(i, false);
        }
    }

    default void flip(int bitIndex) {
        if (get(bitIndex)) {
            set(bitIndex, false);
        } else {
            set(bitIndex, true);
        }
    }

    default void flip(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; i++) {
            flip(i);
        }
    }

    default void set(int bitIndex) {
        set(bitIndex, true);
    }

    default void set(int fromIndex, int toIndex) {
        set(fromIndex, toIndex, true);
    }

    default void set(int fromIndex, int toIndex, boolean value) {
        for (int i = fromIndex; i < toIndex; i++) {
            set(i, value);
        }
    }

    default void clear(long bitIndex) {
        set(bitIndex, false);
    }

    default void clear(long fromIndex, long toIndex) {
        for (long i = fromIndex; i < toIndex; i++) {
            set(i, false);
        }
    }

    default void flip(long bitIndex) {
        if (get(bitIndex)) {
            set(bitIndex, false);
        } else {
            set(bitIndex, true);
        }
    }

    default void flip(long fromIndex, long toIndex) {
        for (long i = fromIndex; i < toIndex; i++) {
            flip(i);
        }
    }

    default void set(long bitIndex) {
        set(bitIndex, true);
    }

    default void set(long fromIndex, long toIndex) {
        set(fromIndex, toIndex, true);
    }

    default void set(long fromIndex, long toIndex, boolean value) {
        for (long i = fromIndex; i < toIndex; i++) {
            set(i, value);
        }
    }
}
