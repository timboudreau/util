package com.mastfrog.concurrent.random;

import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.FIVE;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.FOUR;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.ONE;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.SEVEN;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.SIX;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.THREE;
import static com.mastfrog.concurrent.random.SampleProbability.MaskBits.TWO;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Factory for fast, thread-safe, lockless <code>BooleanSupplier</code>s
 * producing a random distribution of true to false bits matching the percentage
 * described by the enum constant.
 * <p>
 * For collecting statistics over very large numbers of events, in order to
 * collect a distribution of values for high-traffic requests over a time
 * period, this type allows for controlling the perccentage of requests that are
 * sampled where collecting all values is memory-cost-prohibitive.
 * <p>
 * Use the <code>supplier()</code> method to get a <code>BooleanSupplier</code>
 * which will emit the requested distribution of bits from its
 * <code>getAsBoolean()</code> method.
 * </p>
 * <p>
 * This class collects at least <code>sampleCount</code> bits up-front rather
 * than going back to the <code>Random</code> for each bit, refreshing the bits
 * when they are exhausted.
 * </p>
 * <h2>Implementation</h2>
 * <p>
 * Under the hood, what the BooleanSuppliers this class produces do is:
 * </p>
 * <ul>
 * <li>On each new cycle through N bits, populate a byte array of at least N
 * bits with random bytes - this gives 50/50 probability of true of false</li>
 * <li>If <code>this != FIFTY_PERCENT</code>, generate random <code>mask</code>
 * with M bits set, where M corresponds to the percentage of 8 bits described by
 * <code>this</code></li>
 * <li>Iterate all of the random bytes, rotating <code>mask</code> by a random
 * number of bits and then applying it - using <code>&amp;</code> if
 * <code>this.targetProbability() &lt; 0.5</code> and <code>|</code> if it is
 * greater</li>
 * <li>Proceed to iterate using an <code>AtomicInteger</code> bit cursor to
 * provide bits as output until the bits are exhausted, then repeat</li>
 * </ul>
 * <p>
 * As the unit tests for this class demonstrate, the result is a distribution of
 * true to false values that match a tolerance of 0.0008 for 1,000,000 samples.
 * </p>
 * <h2>Providing your own Random</h2>
 *
 * The methods providing a <code>BooleanSupplier</code> have overloads which
 * take a <code>Random</code>. These overloads are for use in tests where
 * deterministic output is required - do not use them in production code, as
 * <code>Random</code> inherently has performance issues in multi-threaded code
 * - if none is provided, then <code>ThreadLocalRandom.current()</code> is used,
 * which is appropriate for concurrent environments.
 */
public enum SampleProbability {
    /**
     * 6.25%
     */
    SIX_PERCENT(ONE, false, 0.0625),
    /**
     * 12.5%
     */
    TWELVE_PERCENT(TWO, false, 0.125),
    /**
     * 18.75%
     */
    NINETEEN_PERCENT(THREE, false, 0.1875),
    /**
     * 25%
     */
    TWENTY_FIVE_PERCENT(FOUR, false, 0.25),
    /**
     * 31.25%
     */
    THIRTY_ONE_PERCENT(FIVE, false, 0.3125),
    /**
     * 37.5%
     */
    THIRTY_EIGHT_PERCENT(SIX, false, 0.375),
    /**
     * 43.75%
     */
    FORTY_FOUR_PERCENT(SEVEN, false, 0.4375),
    /**
     * 50%
     */
    FIFTY_PERCENT(MaskBits.NONE, false, 0.5),
    /**
     * 56.25%
     */
    FIFTY_SIX_PERCENT(ONE, true, 0.5625),
    /**
     * 62.5%
     */
    SIXTY_THREE_PERCENT(TWO, true, 0.625),
    /**
     * 68.75%
     */
    SIXTY_NINE_PERCENT(THREE, true, 0.6875),
    /**
     * 75%
     */
    SEVENTY_FIVE_PERCENT(FOUR, true, 0.75),
    /**
     * 81.25%
     */
    EIGHTY_ONE_PERCENT(FIVE, true, 0.8125),
    /**
     * 87.5%
     */
    EIGHTY_EIGHT_PERCENT(SIX, true, 0.875),
    /**
     * 93.75%
     */
    NINETY_FOUR_PERCENT(SEVEN, true, 0.9375);
    private final OddsAdjuster oddsAdjuster;
    private final double target;

    public BooleanSupplier supplier() {
        return supplier(4096);
    }

    /**
     * Create a BooleanSupplier which will emit the percentage of true's that
     * this SampleProbability expects, given sufficient samples.
     *
     * @param expectedSamples The number of samples expected; this is used to
     * size an internal byte array which is used to periodically grab some new
     * input. It is not required that this be the same number as the number of
     * samples the user is expecting (in fact, the internal size in bits will be
     * the nearest greater multiple of 8). But it is a good idea to make
     * turnover an infrequent operation, timed to line up with getting a new set
     * of samples.
     *
     * @return A BooleanSupplier
     */
    public BooleanSupplier supplier(int expectedSamples) {
        return supplier(expectedSamples, null);
    }

    /**
     * Create a BooleanSupplier which will emit the percentage of true's that
     * this SampleProbability expects, given sufficient samples.
     *
     * @param expectedSamples The number of samples expected; this is used to
     * size an internal byte array which is used to periodically grab some new
     * input. It is not required that this be the same number as the number of
     * samples the user is expecting (in fact, the internal size in bits will be
     * the nearest greater multiple of 8). But it is a good idea to make
     * turnover an infrequent operation, timed to line up with getting a new set
     * of samples.
     * @param rnd A random or null. Production code should use null or the
     * <code>supplier(int)</code> method - this overload is useful for tests
     * which need to pass in a random with a known state in order to have
     * deterministic results.
     *
     * @return A BooleanSupplier
     */
    public BooleanSupplier supplier(int expectedSamples, Random rnd) {
        return new RandomlySample(expectedSamples, rnd, this);
    }

    /**
     * Given a double between 0 and 1, returns the probability value nearest to
     * the passed one.
     *
     * @param val A double between 0 and one
     * @return A SampleProbabiility, never null
     */
    public static SampleProbability nearest(double val) {
        if (val >= NINETY_FOUR_PERCENT.target) {
            return NINETY_FOUR_PERCENT;
        } else if (val <= SIX_PERCENT.target) {
            return SIX_PERCENT;
        }
        SampleProbability[] all = SampleProbability.values();
        for (int i = 1; i < all.length; i++) {
            double v1 = all[i - 1].target;
            double v2 = all[i].target;
            if (val >= v1 && val <= v2) {
                double midpoint = v1 + ((v2 - v1) / 2D);
                if (val < midpoint) {
                    return all[i - 1];
                } else {
                    return all[i];
                }
            }
        }
        return NINETY_FOUR_PERCENT;
    }

    SampleProbability(MaskBits bits, boolean unmask, double target) {
        this.oddsAdjuster = new OddsAdjuster(bits, unmask);
        this.target = target;
    }

    public double targetProbability() {
        return target;
    }

    public void apply(Random gen, byte[] bytes) {
        oddsAdjuster.apply(gen, bytes);
    }

    @Override
    public String toString() {
        return (target * 100D) + "%";
    }

    /**
     * Encapsulates a mask bits and whether to logically or or and the values.
     */
    private static class OddsAdjuster {

        static final OddsAdjuster NONE = new OddsAdjuster(MaskBits.NONE, false);
        private final MaskBits bits;
        private final boolean unmask;

        OddsAdjuster(MaskBits bits, boolean unmask) {
            this.bits = bits;
            this.unmask = unmask;
        }

        void apply(Random gen, byte[] bytes) {
            if (bits == MaskBits.NONE) {
                return;
            }
            // There is no point in synchronizing here - racing over mutations
            // to an array of random bytes with n bits set is harmless 
            byte apply = bits.mask(gen);
            for (int i = 0; i < bytes.length; i++) {
                // Randomly rotate so we do not always mask the same bits
                apply = rotateRight(apply, gen.nextInt(8));
                if (unmask) {
                    bytes[i] = (byte) ((bytes[i] | apply) & 0xFF);
                } else {
                    bytes[i] = (byte) (bytes[i] & apply & 0xFF);
                }
            }
        }

        private static byte rotateRight(byte value, int rotateBy) {
            return (byte) (((value & 0xff) >>> rotateBy) | ((value & 0xff) << (8 - rotateBy)));
        }
    }

    enum MaskBits {
        NONE, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN;
        // Internal array of indices, which we copy and shuffle to choose
        // bits to mask
        private static final int[] INDICES = new int[]{0, 1, 2, 3, 4, 5, 6, 7};

        /**
         * Generate a new mask, using the passed random to determine which bits
         * are masked.
         *
         * @param rnd
         * @return
         */
        byte mask(Random rnd) {
            byte result = 0;
            switch (this) {
                case NONE:
                    return result;
                case ONE:
                    return (byte) (1 << rnd.nextInt(8));
                case SEVEN:
                    return (byte) (~ONE.mask(rnd) & 0xFF);
                default:
                    // A safe way to create a random list of bits to mask,
                    // without any potentially endless loops
                    int[] ixs = Arrays.copyOf(INDICES, INDICES.length);
                    // Fischer-yates shuffle
                    shuffle(rnd, ixs);
                    for (int i = 0; i < ordinal(); i++) {
                        result = (byte) (result | 1 << ixs[i]);
                    }
            }
            return result;
        }
    }

    /**
     * Supplier for whether or not to record a sample - this is used by
     * LongStatisticCollector.intermittendlySampling() to allow us to collect
     * representative data from larger numbers of requests than is reasonable to
     * collect exact samples for over a period.
     * <p>
     * Since the code in question is heavily concurrent, and Randoms do not
     * behave well with concurrent access, we take the much cheaper approach of
     * precomputing an array of bits which are used as our randomness,
     * recomputing them on wrap around.
     * </p>
     */
    static class RandomlySample implements BooleanSupplier {

        // package private for tests
        private final byte[] bytes;
        private final AtomicInteger bitCursor = new AtomicInteger();
        private final Random random;
        private SampleProbability adj;

        RandomlySample(int samples) {
            this(samples, null);
        }

        RandomlySample(int samples, Random random) {
            this(samples, random, SampleProbability.FIFTY_PERCENT);
        }

        RandomlySample(int samples, Random random, SampleProbability adj) {
            int byteCount = samples / 8;
            if (samples % 8 != 0) {
                byteCount++;
            }
            this.adj = adj;
            this.bytes = new byte[byteCount];
            this.random = random;
        }

        private Random random() {
            // For tests, we need deterministic output
            return random == null ? ThreadLocalRandom.current() : random;
        }

        private int cursor() {
            int result = bitCursor.getAndUpdate(old -> {
                if (old >= (bytes.length * 8) - 1) {
                    return 0;
                }
                return old + 1;
            });
            // If we have rolled around to zero
            if (result == 0) {
                // Get a new set of bits following the same distribution
                Random r = random();
                r.nextBytes(bytes);
                adj.apply(r, bytes);
            }
            return result;
        }

        @Override
        public boolean getAsBoolean() {
            int cur = cursor();
            byte b = bytes[cur / 8];
            int mask = 1 << cur % 8;
            return (b & mask) != 0;
        }
    }

    /**
     * Does a
     * <a href="https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Fisher-Yates
     * shuffle</a>.
     *
     * @param rnd A random
     * @param array An array
     */
    private static void shuffle(Random rnd, int[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                int hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

}
