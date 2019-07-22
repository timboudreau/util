package com.mastfrog.bits.large;

import com.mastfrog.bits.MutableBits;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Random;
import java.util.function.LongFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(Parameterized.class)
public class LongArrayBitSetTest {

    private LongArrayBitSet odds;
    private BitSet oddBitSet;
    private LongArrayBitSet random;
    private BitSet randomBitSet;
    private static final int SIZE = 530;
    private Random rnd;
    private final LongFunction<LongArray> arrayFactory;

    @Parameters(name = "{index}:{0}")
    public static Collection<LongFunction<LongArray>> params() {
        return Arrays.asList(new UnsafeLong(), new MappedLong(), new JavaLong());
    }

    public LongArrayBitSetTest(LongFunction<LongArray> arrayFactory) {
        this.arrayFactory = arrayFactory;
    }

    @Before
    public void setup() {
        rnd = new Random(2398203973213L);
        odds = new LongArrayBitSet(SIZE, arrayFactory);
        assertTrue(odds.isEmpty());
        oddBitSet = new BitSet(SIZE);
        for (int i = 0; i < SIZE; i++) {
            if (i % 2 == 1) {
                odds.set(i);
                oddBitSet.set(i);
            } else {
                odds.clear(i);
                oddBitSet.clear(i);
            }
        }
        assertFalse(odds.isEmpty());
        random = new LongArrayBitSet(SIZE, arrayFactory);
        assertTrue(random.isEmpty());
        randomBitSet = new BitSet(SIZE);
        for (int i = 0; i < SIZE; i++) {
            if (rnd.nextBoolean()) {
                random.set(i);
                randomBitSet.set(i);
            }
        }
        assertFalse(random.isEmpty());
    }

    @Test
    public void testBasic() {
        assertMatch(randomBitSet, random);
        assertMatch(oddBitSet, odds);
        int i = randomBitSet.nextSetBit(0);
        long l = random.nextSetBit(0L);
        for (; l >= 0 && i >= 0; i = randomBitSet.nextSetBit(i + 1), l = random.nextSetBit(l + 1)) {
            int il = (int) l;
            assertEquals(i, il);
        }
    }

    @Test
    public void testOr() {
        odds.or(random);
        oddBitSet.or(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @Test
    public void testXor() {
        odds.xor(random);
        oddBitSet.xor(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @Test
    public void testAnd() {
        odds.and(random);
        oddBitSet.and(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @Test
    public void testAndNot() {
        odds.andNot(random);
        oddBitSet.andNot(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @Test
    public void testFlip() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.flip(i);
                oddBitSet.flip((int) i);
                assertMatch(oddBitSet, odds);
            }
        }
    }

    @Test
    public void testClear() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.clear(i);
                oddBitSet.clear((int) i);
                assertMatch("After clear of bit " + i
                        + " which should be " + odds.get(i)
                        + " sets do not matchh", oddBitSet, odds);
            }
        }
    }

    @Test
    public void testClearMany() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.clear(0, i);
                oddBitSet.clear(0, (int) i);
                assertMatch("Iter " + i, oddBitSet, odds);
            }
        }
    }

    @Test
    public void testSetMany() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.set(0, i);
                oddBitSet.set(0, (int) i);
                assertMatch("Iter " + i, oddBitSet, odds);
            }
        }
    }

    @Test
    public void testFlipMany() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.flip(0, i);
                oddBitSet.flip(0, (int) i);
                assertMatch("After flip of bit " + i
                        + " which should be " + odds.get(i)
                        + " sets do not matchh", oddBitSet, odds);
            }
        }
    }

    @Test
    public void testSet() {
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.set(i);
                oddBitSet.set((int) i);
                assertMatch(oddBitSet, odds);
                assertEquals(oddBitSet.cardinality(), (int) odds.cardinality());
                assertEquals(oddBitSet.length(), (int) odds.length());
            }
        }
    }

    @Test
    public void testInitBug() {
        int ct = 990;
        LongArrayBitSet labs = new LongArrayBitSet(ct, arrayFactory);
        MutableBits labs2 = MutableBits.createLarge(990);
        labs.set(2);
        labs2.set(2);
    }

    static final class UnsafeLong implements LongFunction<LongArray> {

        @Override
        public LongArray apply(long value) {
            LongArray result = new UnsafeLongArray(value);
            result.clear();
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    static final class MappedLong implements LongFunction<LongArray> {

        private static final Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        private static volatile int ix = 1;
        private static final String pfx = "MappedLong-" + Long.toString(System.currentTimeMillis(), 36) + "-";

        @Override
        public LongArray apply(long value) {
            // Ensure that in parallel tests, files don't collide
            assertTrue("tmpdir does not exist", Files.exists(tmp));
            assertTrue("tmpdir not a dir", Files.isDirectory(tmp));
            String nm = pfx + ix++ + ".longs";
            return new MappedFileLongArray(tmp.resolve(nm), value);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    static final class JavaLong implements LongFunction<LongArray> {

        @Override
        public LongArray apply(long value) {
            return new JavaLongArray(value);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private static void assertMatch(BitSet expected, LongArrayBitSet got) {
        assertMatch("", expected, got);
    }

    private static void assertMatch(String msg, BitSet expected, LongArrayBitSet got) {
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
