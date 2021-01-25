package com.mastfrog.bits;

import java.util.BitSet;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the fallback implementations on Bits do the right thing.
 *
 * @author Tim Boudreau
 */
public class DefaultImplementationsTest {

    @Test
    public void testAnd() {
        a.and(b);
        aBits.and(bBits);
        assertMatch(a, aBits);
    }

    @Test
    public void testAndNot() {
        a.andNot(b);
        aBits.andNot(bBits);
        assertMatch(a, aBits);
    }

    @Test
    public void testOr() {
        a.or(b);
        aBits.or(bBits);
        assertMatch(a, aBits);
    }

    @Test
    public void testXor() {
        a.xor(b);
        aBits.xor(bBits);
        assertMatch(a, aBits);
    }

    @Test
    public void testHash() {
        assertEquals(a.hashCode(), aBits.bitsHashCode());
        assertEquals(b.hashCode(), bBits.bitsHashCode());
    }

    private static final class B implements MutableBits {

        final BitSet bits;

        public B(BitSet bits) {
            this.bits = bits;
        }

        @Override
        public BitSet toBitSet() {
            return bits;
        }

        @Override
        public void set(int bitIndex, boolean value) {
            bits.set(bitIndex, value);
        }

        @Override
        public int cardinality() {
            return bits.cardinality();
        }

        @Override
        public Bits copy() {
            return this;
        }

        @Override
        public MutableBits mutableCopy() {
            return this;
        }

        @Override
        public boolean get(int bitIndex) {
            return bits.get(bitIndex);
        }

        @Override
        public int nextClearBit(int fromIndex) {
            return bits.nextClearBit(fromIndex);
        }

        @Override
        public int nextSetBit(int fromIndex) {
            return bits.nextSetBit(fromIndex);
        }

        @Override
        public int previousClearBit(int fromIndex) {
            return bits.previousClearBit(fromIndex);
        }

        @Override
        public int previousSetBit(int fromIndex) {
            return bits.previousSetBit(fromIndex);
        }
    }

    private Random rnd;
    private BitSet a, b, aCopy, bCopy;
    private B aBits, bBits;

    @BeforeEach
    public void setup() {
        rnd = new Random(882924230947L);
        a = randomBits(100);
        b = randomBits(100);
        assertNotEquals(a, b);
        aCopy = (BitSet) a.clone();
        bCopy = (BitSet) b.clone();
        aBits = new B(aCopy);
        bBits = new B(bCopy);
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

    private static void assertMatch(BitSet expected, B got) {
        assertMatch("", expected, got);
    }

    private static void assertMatch(String msg, BitSet expected, B got) {
        if (!expected.equals(got)) {
            BitSet expectedBits = expected;
            BitSet gotBits = got.toBitSet();
            if (!expectedBits.equals(gotBits)) {
                BitSet missing = (BitSet) expectedBits.clone();
                for (int bit = gotBits.nextSetBit(0); bit >= 0; bit = gotBits.nextSetBit(bit + 1)) {
                    missing.clear(bit);
                }
                BitSet extra = (BitSet) gotBits.clone();
                for (int bit = expectedBits.nextSetBit(0); bit >= 0; bit = expectedBits.nextSetBit(bit + 1)) {
                    extra.clear(bit);
                }
                fail((msg.isEmpty() ? msg : msg + ": ") + "Sets do not match - " + (missing.isEmpty() ? "" : "missing " + missing)
                        + (extra.isEmpty() ? ""
                        : " extra " + extra)
                        + "\nexpected:\n" + expectedBits + "\ngot:\n" + gotBits
                );
            }
        }
    }

}
