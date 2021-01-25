package com.mastfrog.bits;

import java.util.BitSet;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class BitsHashCodeTest {

    @Test
    public void testBoundaries() {
        BitSet bs = new BitSet(100);
        bs.set(64);
        bs.set(0);
        assertHashCode(Bits.fromBitSet(bs));
    }

    @Test
    public void testHashCode() {
        assertHashCode(Bits.fromBitSet(new BitSet(0)));
        assertHashCode(Bits.fromBitSet(randomBits(10)));
        assertHashCode(Bits.fromBitSet(randomBits(30)));
        assertHashCode(Bits.fromBitSet(randomBits(66)));
        assertHashCode(Bits.fromBitSet(randomBits(2000)));
        assertHashCode(Bits.fromBitSet(randomBits(2819)));
        assertHashCode(Bits.fromBitSet(randomBits(37)));
        assertHashCode(Bits.fromBitSet(randomBits(1)));
        assertHashCode(Bits.fromBitSet(randomBits(0)));
    }

    @Test
    public void testToLongArray() {
        assertLongArray(new BitSet(0));
        BitSet bs = new BitSet();
        bs.set(1);
        assertLongArray(bs);
        assertLongArray(randomBits(36));
        assertLongArray(randomBits(200));
        assertLongArray(randomBits(3000));
        assertLongArray(randomBits(127));
        assertLongArray(randomBits(128));
        assertLongArray(randomBits(129));
        assertLongArray(randomBits(130));
    }

    private void assertLongArray(BitSet bits) {
        DummyBits dummy = new DummyBits(bits);
        assertArrayEquals(bits.toLongArray(), dummy.toLongArray());
        Bits real = Bits.fromBitSet(bits);
        assertArrayEquals(bits.toLongArray(), real.toLongArray());
        assertArrayEquals(bits.toByteArray(), dummy.toByteArray());
        assertArrayEquals(bits.toByteArray(), real.toByteArray());
    }

    private void assertHashCode(Bits bits) {
        assertEquals(bits.hashCode(), bits.bitsHashCode(), bits + "");
    }

    private Random rnd;

    @BeforeEach
    public void setup() {
        rnd = new Random(10292423094L);
    }

    private BitSet randomBits(int max) {
        BitSet result = new BitSet(max);
        for (int i = 0; i < max; i++) {
            if (rnd.nextBoolean()) {
                result.set(i);
            }
        }
        return result;
    }

    private BitSet moreRandomBits(BitSet b, int size) {
        BitSet orig = b;
        while (b.equals(orig)) {
            b = randomBits(size);
        }
        return b;
    }

    static class DummyBits implements Bits {

        private final BitSet set;

        public DummyBits(BitSet set) {
            this.set = set;
        }

        @Override
        public int cardinality() {
            return set.cardinality();
        }

        @Override
        public Bits copy() {
            return this;
        }

        @Override
        public MutableBits mutableCopy() {
            return new MutableBitSetBits(set);
        }

        @Override
        public boolean get(int bitIndex) {
            return set.get(bitIndex);
        }

        @Override
        public int nextClearBit(int fromIndex) {
            return set.nextClearBit(fromIndex);
        }

        @Override
        public int nextSetBit(int fromIndex) {
            return set.nextSetBit(fromIndex);
        }

        @Override
        public int previousClearBit(int fromIndex) {
            return set.previousClearBit(fromIndex);
        }

        @Override
        public int previousSetBit(int fromIndex) {
            return set.previousSetBit(fromIndex);
        }
    }
}
