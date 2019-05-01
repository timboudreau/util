package com.mastfrog.bits;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class MutableBitSetBitsTest {

    private Random rnd;

    @Before
    public void setup() {
        rnd = new Random(102924230947L);
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

    private static BitSet copyOp(BitSet bs, Consumer<BitSet> c) {
        BitSet on = (BitSet) bs.clone();
        c.accept(on);
        return on;
    }

    @Test
    public void testLogicalOperations() {
        BitSet a = randomBits(200);
        BitSet b = moreRandomBits(a, 200);
        MutableBits ma = MutableBits.valueOf(a);
        MutableBits mb = MutableBits.valueOf(b);
        assertNotEquals(ma, mb);
        BitSet expectedOr = copyOp(a, bs -> {
            bs.or(b);
        });
        BitSet expectedAnd = copyOp(a, bs -> {
            bs.and(b);
        });
        BitSet expectedAndNot = copyOp(a, bs -> {
            bs.andNot(b);
        });
        BitSet expectedXor = copyOp(a, bs -> {
            bs.xor(b);
        });

        MutableBits expectedOrBits = MutableBits.valueOf(expectedOr);
        MutableBits expectedAndBits = MutableBits.valueOf(expectedAnd);
        MutableBits expectedAndNotBits = MutableBits.valueOf(expectedAndNot);
        MutableBits expectedXorBits = MutableBits.valueOf(expectedXor);

        MutableBits mbOr = ma.orWith(mb);
        assertBitsEqual(expectedOrBits, mbOr);
        assertEquals(expectedOr, mbOr.toBitSet());

        MutableBits mbAnd = ma.andWith(mb);
        assertBitsEqual(expectedAndBits, mbAnd);
        assertEquals(expectedAnd, mbAnd.toBitSet());

        MutableBits mbAndNot = ma.andNotWith(mb);
        assertBitsEqual(expectedAndNotBits, mbAndNot);
        assertEquals(expectedAndNot, mbAndNot.toBitSet());

        MutableBits mbXor = ma.xorWith(mb);
        assertBitsEqual(expectedXorBits, mbXor);
        assertEquals(expectedXor, mbXor.toBitSet());

        MutableBits mbOr2 = ma.mutableCopy();
        mbOr2.or(mb);
        assertBitsEqual(expectedOrBits, mbOr2);
        assertEquals(expectedOr, mbOr2.toBitSet());

        MutableBits mbAnd2 = ma.mutableCopy();
        mbAnd2.and(mb);
        assertBitsEqual(expectedAndBits, mbAnd2);
        assertEquals(expectedAnd, mbAnd2.toBitSet());

        MutableBits mbAndNot2 = ma.mutableCopy();
        mbAndNot2.andNot(mb);
        assertBitsEqual(expectedAndNotBits, mbAndNot2);
        assertEquals(expectedAndNot, mbAndNot2.toBitSet());

        MutableBits mbXor2 = ma.mutableCopy();
        mbXor2.xor(mb);
        assertBitsEqual(expectedXorBits, mbXor2);
        assertEquals(expectedXor, mbXor2.toBitSet());
    }

    private static void assertBitsEqual(Bits expected, Bits got) {
        if (!expected.equals(got)) {
            BitSet expectedBits = expected.toBitSet();
            BitSet gotBits = got.toBitSet();
            if (expectedBits.equals(gotBits)) {
                fail("Sets equal as BitSets but their equals contract is broken");
            } else {
                BitSet missing = (BitSet) expectedBits.clone();
                for (int bit = gotBits.nextSetBit(0); bit >= 0; bit = gotBits.nextSetBit(bit + 1)) {
                    missing.clear(bit);
                }
                BitSet extra = (BitSet) gotBits.clone();
                for (int bit = expectedBits.nextSetBit(0); bit >= 0; bit = expectedBits.nextSetBit(bit + 1)) {
                    extra.clear(bit);
                }
                fail("Sets to not match - missing " + missing + " extra " + extra);
            }
        }
        assertTrue(expected.contentEquals(got));
        assertEquals(expected, got);
    }

    @Test
    public void testIteration() {
        BitSet bits = new BitSet();
        MutableBits mb = MutableBits.create(1000);
        Set<Integer> expected = new HashSet<>();
        Set<Integer> slice = new HashSet<>();
        for (int i = 0; i < 999; i++) {
            if (i % 2 == 1) {
                bits.set(i);
                expected.add(i);
                mb.set(i);
                if (i > 500 && i < 700) {
                    slice.add(i);
                }
            }
        }
        MutableBits wrapped = MutableBits.valueOf(bits);
        assertContents(wrapped, expected);

        assertContents(mb, expected);

        assertSlice(wrapped, 500, 700, slice);
        assertSlice(mb, 500, 700, slice);

        Bits wrappedBits = Bits.fromBitSet(bits);
        assertContents(wrappedBits, expected);
        assertSlice(wrappedBits, 500, 700, slice);
    }

    @Test
    public void testBitSetBehavior() {
        // does bitset really return shifted bits for slices?
        BitSet b = new BitSet();
        for (int i = 0; i < 20; i++) {
            b.set(i);
        }
        BitSet b1 = b.get(10, 20);
        for (int i = 0; i < 20; i++) {
            if (i >= 10) {
                assertFalse(i + "", b1.get(i));
            } else {
                assertTrue(i + "", b1.get(i));
            }
        }
    }

    private void assertSlice(Bits bits, int start, int end, Set<Integer> expected) {
        for (Integer exp : expected) {
            assertTrue("Slice does not contain " + exp, bits.get(exp));
        }
        Set<Integer> got = new HashSet<>();
        int[] lastBit = new int[]{Integer.MIN_VALUE};
        int countUp = bits.forEachSetBitAscending(start, end, bit -> {
            got.add(bit);
            assertTrue("bit " + bit + ">" + lastBit[0], bit > lastBit[0]);
            lastBit[0] = bit;
        });
        assertSets(expected, got);

        got.clear();
        lastBit[0] = Integer.MAX_VALUE;
        int countDown = bits.forEachSetBitDescending(end, start, bit -> {
            got.add(bit);
            assertTrue("bit " + bit + "<" + lastBit[0], bit < lastBit[0]);
            lastBit[0] = bit;
        });
        assertSets(expected, got);

        got.clear();
        lastBit[0] = Integer.MIN_VALUE;
        long countUpLong = bits.forEachLongSetBitAscending(start, end, bit -> {
            int ib = (int) bit;
            got.add(ib);
            assertTrue("bit " + ib + ">" + lastBit[0], ib > lastBit[0]);
            lastBit[0] = ib;
        });
        assertSets(expected, got);

        got.clear();
        lastBit[0] = Integer.MAX_VALUE;
        long countDownLong = bits.forEachLongSetBitDescending(end, start, bit -> {
            got.add((int) bit);
            assertTrue("bit " + bit + "<" + lastBit[0], bit < lastBit[0]);
            lastBit[0] = (int) bit;
        });
        assertSets(expected, got);

        assertEquals(100, countUp);
        assertEquals(100, countDown);
        assertEquals(100, countUpLong);
        assertEquals(100, countDownLong);
    }

    private void assertContents(Bits bits, Set<Integer> expected) {
        for (Integer exp : expected) {
            assertTrue("Bits does not contain " + exp, bits.get(exp));
        }
        BitSet bs = bits.toBitSet();
        Set<Integer> got = new HashSet<>();
        for (int bit = bs.nextSetBit(0); bit >= 0; bit = bs.nextSetBit(bit + 1)) {
            got.add(bit);
        }
        assertSets(expected, got);
        got.clear();
        for (int bit = bits.nextSetBit(0); bit >= 0; bit = bits.nextSetBit(bit + 1)) {
            got.add(bit);
        }
        assertSets(expected, got);

        got.clear();
        for (int bit = bits.previousSetBit(Integer.MAX_VALUE); bit >= 0; bit = bits.previousSetBit(bit - 1)) {
            got.add(bit);
        }
        assertSets(expected, got);

        got.clear();
        for (long bit = bits.nextSetBitLong(0); bit >= 0; bit = bits.nextSetBitLong(bit + 1)) {
            got.add((int) bit);
        }
        assertSets(expected, got);

        got.clear();
        for (long bit = bits.previousSetBitLong(Long.MAX_VALUE); bit >= 0; bit = bits.previousSetBitLong(bit - 1)) {
            got.add((int) bit);
        }
        assertSets(expected, got);

        got.clear();
        bits.forEachSetBitAscending(bit -> {
            got.add(bit);
        });
        assertSets(expected, got);

        got.clear();
        bits.forEachSetBitDescending(bit -> {
            got.add(bit);
        });
        assertSets(expected, got);

        got.clear();
        bits.forEachLongSetBitDescending(bit -> {
            got.add((int) bit);
        });
        assertSets(expected, got);

        got.clear();
        bits.forEachLongSetBitAscending(bit -> {
            got.add((int) bit);
        });
        assertSets(expected, got);
    }

    private static void assertSets(Set<Integer> expected, Set<Integer> got) {
        List<Integer> gotSorted = new ArrayList<>(got);
        Collections.sort(gotSorted);
        if (!expected.equals(got)) {
            Set<Integer> missing = new HashSet<>(expected);
            missing.removeAll(got);
            Set<Integer> extra = new HashSet<>(got);
            extra.removeAll(expected);
            if (missing.isEmpty() && !extra.isEmpty()) {
                fail("Unexpected items: " + extra + " in " + gotSorted);
            } else if (extra.isEmpty() && !missing.isEmpty()) {
                fail("Missing items " + missing + " in " + gotSorted);
            } else if (!extra.isEmpty() && !missing.isEmpty()) {
                fail("Missing items " + missing + " and unexpected items " + extra + " in " + gotSorted);
            } else {
                fail("Huh? " + expected + " vs. " + got);
            }
        }
    }

    @Test
    public void testBasic() {
        BitSet real = randomBits(2200);
        MutableBitSetBits fake = new MutableBitSetBits(real);
        int card = real.cardinality();
        assertEquals(card, fake.cardinality());
        Set<Integer> expected = new HashSet<>();
        Set<Integer> got = new HashSet<>();
        for (int bit = real.nextSetBit(0); bit >= 0; bit = real.nextSetBit(bit + 1)) {
            expected.add(bit);
        }
        for (int bit = fake.nextSetBit(0); bit >= 0; bit = fake.nextSetBit(bit + 1)) {
            got.add(bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (long bit = fake.nextSetBitLong(0); bit >= 0; bit = fake.nextSetBitLong(bit + 1)) {
            got.add((int) bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (int bit = fake.previousSetBit(Integer.MAX_VALUE); bit >= 0; bit = fake.previousSetBit(bit - 1)) {
            got.add(bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (long bit = fake.previousSetBitLong(Long.MAX_VALUE); bit >= 0; bit = fake.previousSetBitLong(bit - 1)) {
            got.add((int) bit);
        }
        assertEquals(expected, got);

        for (int i = 0; i < 220; i++) {
            if (rnd.nextInt(10) == 3) {
                fake.set(i);
                real.set(i);
                assertTrue("Failed to set " + i, fake.get(i));
            } else if (rnd.nextInt(4) == 2) {
                real.clear(i);
                fake.clear(i);
                assertFalse("Failed to clear " + i, fake.get(i));
            }
        }
        assertEquals(fake, real);
        assertEquals(real, fake.bitSetUnsafe());

        int unset = fake.nextClearBit(0);
        fake.set(unset);
        assertTrue(fake.get(unset));

        MutableBits mb2 = fake.mutableCopy();
        assertEquals(fake, mb2);
        assertTrue(fake.contentEquals(mb2));
        mb2.clear(unset);

        assertNotEquals(fake, mb2);
        fake.clear(unset);
    }

    @Test
    public void testCompareWithBitSet() {
        BitSet aBack = randomBits(1290);
        BitSet bBack = (BitSet) aBack.clone();
        MutableBits real = new MutableBitSetBits(aBack);
        MutableBits fake = proxyMutableBits(bBack);

        real.set(10);
        fake.set(10);

        assertTrue(real.get(10));
        assertTrue(fake.get(10));
        assertTrue(real.contentEquals(fake));
        assertTrue(fake.contentEquals(real));
        assertEquals(real, fake);
        assertEquals(fake, real);
        Set<Integer> expected = new HashSet<>();
        Set<Integer> got = new HashSet<>();
        for (int bit = real.nextSetBit(0); bit >= 0; bit = real.nextSetBit(bit + 1)) {
            expected.add(bit);
        }
        for (int bit = fake.nextSetBit(0); bit >= 0; bit = fake.nextSetBit(bit + 1)) {
            got.add(bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (long bit = fake.nextSetBitLong(0); bit >= 0; bit = fake.nextSetBitLong(bit + 1)) {
            got.add((int) bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (int bit = fake.previousSetBit(Integer.MAX_VALUE); bit >= 0; bit = fake.previousSetBit(bit - 1)) {
            got.add(bit);
        }
        assertEquals(expected, got);
        got.clear();
        for (long bit = fake.previousSetBitLong(Long.MAX_VALUE); bit >= 0; bit = fake.previousSetBitLong(bit - 1)) {
            got.add((int) bit);
        }
    }

    static Bits proxyBits(BitSet set) {
        return (Bits) Proxy.newProxyInstance(MutableBitSetBitsTest.class.getClassLoader(), new Class<?>[]{Bits.class}, new BitSetProxy(set));
    }

    static MutableBits proxyMutableBits(BitSet set) {
        return (MutableBits) Proxy.newProxyInstance(MutableBitSetBitsTest.class.getClassLoader(), new Class<?>[]{MutableBits.class}, new BitSetProxy(set));
    }

    static class BitSetProxy implements InvocationHandler {

        private final BitSet bits;

        public BitSetProxy(BitSet bits) {
            this.bits = bits;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "equals":
                    return bits.equals(((BitSetBacked) args[0]).bitSetUnsafe());
                case "hashCode":
                    return bits.hashCode();
                case "toString":
                    return bits.toString();
                case "longLength":
                    return (long) bits.length();
                case "nextSetBitLong":
                    return (long) bits.nextSetBit((int) ((Long) args[0]).longValue());
                case "previousSetBitLong":
                    return (long) bits.previousSetBit((int) ((Long) args[0]).longValue());
                case "contentEquals":
                    return ((BitSetBacked) args[0]).bitSetUnsafe().equals(bits);
                case "clone":
                case "copy":
                case "min":
                    return Integer.MIN_VALUE;
                case "max":
                    return Integer.MAX_VALUE;
                case "minLong":
                    return Long.MIN_VALUE;
                case "maxLong":
                    return Long.MAX_VALUE;
                case "mutableCopy":
                    return proxy instanceof MutableBits ? proxyMutableBits((BitSet) bits.clone())
                            : proxyBits((BitSet) bits.clone());

            }
            Class<?>[] types = args == null ? new Class<?>[0] : new Class<?>[args.length];
            for (int i = 0; i < types.length; i++) {
                if (args[i] instanceof Long) {
                    types[i] = Long.TYPE;
                } else if (args[i] instanceof Integer) {
                    types[i] = Integer.TYPE;
                } else {
                    types[i] = args[i] == null ? Object.class : args[i].getClass();
                }
            }
            Method target = BitSet.class.getMethod(method.getName(), types);
            return target.invoke(bits, args);
        }
    }

    @Test
    public void testShift() {
        int size = 15;
        int shiftBy = 20;
        BitSet rnd = randomBits(size);
        MutableBits bits = MutableBits.valueOf(rnd);
        Bits shifted = bits.shift(shiftBy);
        assertEquals(shiftBy, shifted.min());
        assertEquals(Integer.MAX_VALUE, shifted.max());
        assertEquals((long) shiftBy, shifted.minLong());
        assertEquals(Long.MAX_VALUE, shifted.maxLong());
        bits.forEachSetBitAscending(bit -> {
            assertTrue(shifted.get(bit + shiftBy));
        });
        
        shifted.forEachLongSetBitAscending(bit -> {
            assertTrue(bits.get(bit - shiftBy));
        });
        for (int b1 = bits.nextSetBit(bits.min()), b2 = shifted.nextSetBit(shifted.min()); b1 >= 0 && b2 >= 0; b1 = bits.nextSetBit(b1 + 1), b2 = shifted.nextSetBit(b2 + 1)) {
            assertEquals(b1, b2 - shiftBy);
        }

        for (int b1 = bits.nextClearBit(bits.min()), b2 = shifted.nextClearBit(shifted.min()); b1 >= 0 && b2 >= 0; b1 = bits.nextSetBit(b1 + 1), b2 = shifted.nextSetBit(b2 + 1)) {
            assertEquals(b1, b2 - shiftBy);
        }
        shifted.forEachSetBitDescending(bit -> {
            assertTrue(bits.get(bit - shiftBy));
        });
    }
}
