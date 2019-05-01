package com.mastfrog.bits;

import java.util.BitSet;

/**
 *
 * @author Tim Boudreau
 */
final class BitSetBits implements BitSetBacked {

    private final BitSet bits;

    BitSetBits(BitSet bits) {
        this.bits = bits;
    }

    @Override
    public BitSet bitSetUnsafe() {
        return bits;
    }

    @Override
    public int hashCode() {
        return bits.hashCode();
    }

    @Override
    public Bits copy() {
        return BitSetBacked.super.copy(); //To change body of generated methods, choose Tools | Templates.
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
    public MutableBits mutableCopy() {
        return new MutableBitSetBits(toBitSet());
    }

    @Override
    public String toString() {
        return stringValue();
    }
}
