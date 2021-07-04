package com.mastfrog.bits;

import java.util.function.IntSupplier;

/**
 * A simpler wrapper inverse-bits.
 *
 * @author Tim Boudreau
 */
final class InvertedBits implements Bits {

    private final Bits orig;
    private final IntSupplier capacity;

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
        return contentEquals((Bits) o);
    }

    @Override
    public String toString() {
        return stringValue();
    }
}
