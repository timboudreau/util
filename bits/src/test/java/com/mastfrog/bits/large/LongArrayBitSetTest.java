package com.mastfrog.bits.large;

import com.mastfrog.bits.MutableBits;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * @author Tim Boudreau
 */
public class LongArrayBitSetTest {

    private LongArrayBitSet odds;
    private BitSet oddBitSet;
    private LongArrayBitSet random;
    private BitSet randomBitSet;
    private static final int SIZE = 530;
    private Random rnd;

//    @Parameters(name = "{index}:{0}")
    public static Collection<LongFunction<LongArray>> params() {
        return Arrays.asList(new UnsafeLong(), new MappedLong(), new JavaLong());
    }

    private void setupArray(LongFunction<LongArray> arrayFactory) {
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

    @ParameterizedTest
    @MethodSource("params")
    public void testBasic(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        assertMatch(randomBitSet, random);
        assertMatch(oddBitSet, odds);
        int i = randomBitSet.nextSetBit(0);
        long l = random.nextSetBit(0L);
        for (; l >= 0 && i >= 0; i = randomBitSet.nextSetBit(i + 1), l = random.nextSetBit(l + 1)) {
            int il = (int) l;
            assertEquals(i, il);
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testOr(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        odds.or(random);
        oddBitSet.or(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testXor(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        odds.xor(random);
        oddBitSet.xor(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testAnd(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        odds.and(random);
        oddBitSet.and(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testAndNot(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        odds.andNot(random);
        oddBitSet.andNot(randomBitSet);
        assertMatch(oddBitSet, odds);
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testFlip(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.flip(i);
                oddBitSet.flip((int) i);
                assertMatch(oddBitSet, odds);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testClear(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
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

    @ParameterizedTest
    @MethodSource("params")
    public void testClearMany(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.clear(0, i);
                oddBitSet.clear(0, (int) i);
                assertMatch("Iter " + i, oddBitSet, odds);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testSetMany(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
        for (long i = 0; i < odds.size(); i++) {
            if (rnd.nextBoolean()) {
                odds.set(0, i);
                oddBitSet.set(0, (int) i);
                assertMatch("Iter " + i, oddBitSet, odds);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testFlipMany(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
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

    @ParameterizedTest
    @MethodSource("params")
    public void testSet(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
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

    @ParameterizedTest
    @MethodSource("params")
    public void testInitBug(LongFunction<LongArray> arrayFactory) {
        setupArray(arrayFactory);
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

    static Set<Path> paths = ConcurrentHashMap.newKeySet();
    @AfterAll
    public static void after() {
        for (Path p : paths) {
            if (Files.exists(p)) {
                try {
                    Files.delete(p);
                } catch (IOException ex) {
                    
                }
            }
        }
    }

    static Path addPath(Path path) {
        paths.add(path);
        return path;
    }

    static final class MappedLong implements LongFunction<LongArray> {
        private static final Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        private static volatile int ix = 1;
        private static final String pfx = "MappedLong-" + Long.toString(System.currentTimeMillis(), 36) + "-";

        @Override
        public LongArray apply(long value) {
            // Ensure that in parallel tests, files don't collide
            assertTrue(Files.exists(tmp), "tmpdir does not exist");
            assertTrue(Files.isDirectory(tmp), "tmpdir not a dir");
            String nm = pfx + ix++ + ".longs";
            return new MappedFileLongArray(addPath(tmp.resolve(nm)), value, false);
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
