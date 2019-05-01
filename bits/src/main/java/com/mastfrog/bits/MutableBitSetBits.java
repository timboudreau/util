package com.mastfrog.bits;

import java.util.BitSet;

/**
 *
 * @author Tim Boudreau
 */
final class MutableBitSetBits implements Bits, MutableBits, MutableBitSetBacked {

    private final BitSet bits;

    MutableBitSetBits(BitSet bits) {
        this.bits = bits;
    }

    public Bits readOnlyView() {
        return new BitSetBits(bits);
    }

    @Override
    public BitSet bitSetUnsafe() {
        return bits;
    }

    public String toString() {
        return stringValue();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return copy();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof BitSetBacked) {
            final BitSetBacked other = (BitSetBacked) obj;
            return this.bits.equals(other.bitSetUnsafe());
        } else if (obj instanceof Bits) {
            return contentEquals((Bits) obj);
        } else if (obj instanceof BitSet) {
            return bits.equals(obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return bits.hashCode();
    }

    @Override
    public MutableBits mutableCopy() {
        return new MutableBitSetBits(toBitSet());
    }
}
