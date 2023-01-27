package com.mastfrog.bits.large;

import com.mastfrog.bits.Bits.Characteristics;
import com.mastfrog.bits.MutableBits;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * Provides the same API as java.util.BitSet, but long-indexed instead of
 * int-indexed, and capable of utilizing memory mapped files or off-heap memory
 * from sun.misc.Unsafe as a backing store, depending on the LongArray factory
 * function provided to it.
 *
 * @author Tim Boudreau
 */
public class LongArrayBitSet implements AutoCloseable, Serializable {

    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private final static int BIT_INDEX_MASK = BITS_PER_WORD - 1;
    private static final long WORD_MASK = 0xffffffffffffffffL;
    private static final long serialVersionUID = 1L;
    private LongArray words;
    private transient long wordsInUse = 0;
    private transient boolean stickySize = false;
    private final LongFunction<LongArray> arrayFactory;

    public LongArrayBitSet() {
        this(JavaLongArray::new);
    }

    public LongArrayBitSet(LongFunction<LongArray> arrayFactory) {
        this.arrayFactory = arrayFactory;
        initWords(BITS_PER_WORD);
        stickySize = false;
    }

    public LongArrayBitSet(long nbits) {
        this(nbits, JavaLongArray::new);
    }

    public LongArrayBitSet(long nbits, LongFunction<LongArray> arrayFactory) {
        this.arrayFactory = arrayFactory;
        // nbits can't be negative; size 0 is OK
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }

        initWords(nbits);
        stickySize = true;
    }

    private LongArrayBitSet(long[] words) {
        this(words, JavaLongArray::new);
    }

    private LongArrayBitSet(long[] words, LongFunction<LongArray> arrayFactory) {
        this.arrayFactory = arrayFactory;
        this.words = arrayFactory.apply(0);
        this.words.addAll(words);
        this.wordsInUse = this.words.lastNonZero() + 1;
        sanityCheck();
    }

    public LongArrayBitSet(LongArray array) {
        this.words = array;
        this.arrayFactory = array.factory();
        this.wordsInUse = words.lastNonZero() + 1;
        sanityCheck();
    }

    public LongArrayBitSet(BitSet orig, LongFunction<LongArray> arrayFactory) {
        this(orig.toLongArray(), arrayFactory);
    }

    Set<Characteristics> characteristics() {
        Set<Characteristics> result = EnumSet.of(Characteristics.LARGE,
                Characteristics.LONG_VALUED);
        return result;
    }

    public MutableBits toBits() {
        return new LongArrayBitSetBits(this);
    }

    private static long wordIndex(long bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public long size() {
        return words.size() * BITS_PER_WORD;
    }

    public long length() {
        return wordsInUse == 0 ? 0 : BITS_PER_WORD * (wordsInUse - 1)
                + (BITS_PER_WORD - Long.numberOfLeadingZeros(word(wordsInUse - 1)));
    }

    long wordsInUse() {
        return wordsInUse;
    }

    public long cardinality() {
        long sum = 0;
        for (long i = 0; i < wordsInUse; i++) {
            sum += Long.bitCount(word(i));
        }
        return sum;
    }

    private void trim() {
        if (wordsInUse != words.size()) {
            words.resize(wordsInUse);
            sanityCheck();
        }
    }

    public boolean isEmpty() {
        return wordsInUse == 0;
    }

    @Override
    public void close() {
        if (words instanceof CloseableLongArray) {
            ((CloseableLongArray) words).close();
        }
    }

    long word(long index) {
        return words.get(index);
    }

    private void recalculateWordsInUse() {
        long i;
        for (i = wordsInUse - 1; i >= 0; i--) {
            if (words.get(i) != 0) {
                break;
            }
        }

        wordsInUse = i + 1;
    }

    private void initWords(long nbits) {
        long length = wordIndex(nbits - 1) + 1;
        words = arrayFactory.apply(length);
        if (!words.isZeroInitialized()) {
            words.ensureWriteOrdering(wds -> {
                wds.fill(0, length, 0);
            });
        }
    }

    public BitSet toBitSet() throws IllegalStateException {
        if (length() > Integer.MAX_VALUE) {
            throw new IllegalStateException("Too large");
        }
        BitSet bs = new BitSet();
        for (long bit = nextSetBit(0L); bit >= 0; bit = nextSetBit(bit + 1)) {
            bs.set((int) bit);
        }
        return bs;
    }

    public byte[] toByteArray() {
        if (wordsInUse > Integer.MAX_VALUE) {
            throw new IllegalStateException("Too large");
        }
        int n = (int) wordsInUse;
        if (n == 0) {
            return new byte[0];
        }
        int len = 8 * (n - 1);
        for (long x = word(n - 1); x != 0; x >>>= 8) {
            len++;
        }
        byte[] bytes = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n - 1; i++) {
            bb.putLong(word(i));
        }
        for (long x = word(n - 1); x != 0; x >>>= 8) {
            bb.put((byte) (x & 0xff));
        }
        return bytes;
    }

    public long[] toLongArray() {
        return words.toLongArray(wordsInUse);
    }

    private void maybeGrow(long wordsRequired) {
        if (words.size() < wordsRequired) {
            // Allocate larger of doubled size or required size
            long request = Math.max(2 * words.size(), wordsRequired);
            words.resize(request);
            stickySize = false;
        }
    }

    private void ensureSize(long wordIndex) {
        long wordsRequired = wordIndex + 1;
        if (wordsInUse < wordsRequired) {
            maybeGrow(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }

    public void flip(long bit) {
        if (bit < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bit);
        }
        long word = wordIndex(bit);
        ensureSize(word);

        long wd = word(word);
        wd ^= (1L << bit);
        words.set(word, wd);

        recalculateWordsInUse();
        sanityCheck();
    }

    public void flip(long fromIndex, long toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) {
            return;
        }
        long startIndex = wordIndex(fromIndex);
        long endIndex = wordIndex(toIndex - 1);
        ensureSize(endIndex);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startIndex == endIndex) {
            // Case 1: One word
            long wd = word(startIndex);
            wd ^= (firstWordMask & lastWordMask);
            words.set(startIndex, wd);
        } else {
            long wd = word(startIndex);

            wd ^= firstWordMask;
            words.set(startIndex, wd);

            for (long i = startIndex + 1; i < endIndex; i++) {
                wd = word(i);
                wd ^= WORD_MASK;
                words.set(i, wd);
            }
            wd = word(endIndex);
            wd ^= lastWordMask;
            words.set(endIndex, wd);
        }

        recalculateWordsInUse();
        sanityCheck();
    }

    public void set(long bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        long wordIndex = wordIndex(bitIndex);
        ensureSize(wordIndex);
        long wd = word(wordIndex);
        wd |= (1L << bitIndex);
        words.set(wordIndex, wd);

        sanityCheck();
    }

    public void set(long bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    public void set(long fromIndex, long toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) {
            return;
        }
        long startWordIndex = wordIndex(fromIndex);
        long endWordIndex = wordIndex(toIndex - 1);
        ensureSize(endWordIndex);
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            long wd = word(startWordIndex);
            wd |= (firstWordMask & lastWordMask);
            words.set(startWordIndex, wd);
        } else {
            long wd = word(startWordIndex);
            wd |= firstWordMask;
            words.set(startWordIndex, wd);
            if (endWordIndex - startWordIndex > 1) {
                words.fill(startWordIndex + 1, (endWordIndex - startWordIndex) - 1, WORD_MASK);
            }
            wd = words.get(endWordIndex);
            wd |= lastWordMask;
            words.set(endWordIndex, wd);
        }
        sanityCheck();
    }

    public void set(long fromIndex, long toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    public void clear(long bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        long wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse) {
            return;
        }

        long wd = words.get(wordIndex);
        wd &= ~(1L << bitIndex);
        words.set(wordIndex, wd);

        recalculateWordsInUse();
        sanityCheck();
    }

    public void clear(long fromIndex, long toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) {
            return;
        }
        long startWordIndex = wordIndex(fromIndex);
        if (startWordIndex >= wordsInUse) {
            return;
        }
        long endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= wordsInUse) {
            toIndex = length();
            endWordIndex = wordsInUse - 1;
        }

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            long wd = words.get(startWordIndex);
            wd &= ~(firstWordMask & lastWordMask);
            words.set(startWordIndex, wd);
        } else {
            words.update(startWordIndex, val -> {
                return val & ~firstWordMask;
            });

            if (startWordIndex + 1 < endWordIndex) {
                words.fill(startWordIndex + 1, (endWordIndex - startWordIndex) - 1, 0);
            }

            words.update(endWordIndex, val -> {
                return val & ~lastWordMask;
            });
        }
        recalculateWordsInUse();
        sanityCheck();
    }

    public void clear() {
        words.clear();
        wordsInUse = 0;
    }

    public boolean get(long bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }
        sanityCheck();
        long wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse)
                && ((word(wordIndex) & (1L << bitIndex)) != 0);
    }

    public LongArrayBitSet get(long fromIndex, long toIndex) {
        checkRange(fromIndex, toIndex);
        sanityCheck();
        long len = length();
        if (len <= fromIndex || fromIndex == toIndex) {
            return new LongArrayBitSet(0, arrayFactory);
        }
        if (toIndex > len) {
            toIndex = len;
        }
        LongArrayBitSet result = new LongArrayBitSet(toIndex - fromIndex, arrayFactory);
        long targetWords = wordIndex(toIndex - fromIndex - 1) + 1;
        long sourceIndex = wordIndex(fromIndex);
        boolean wordAligned = ((fromIndex & BIT_INDEX_MASK) == 0);

        for (long i = 0; i < targetWords - 1; i++, sourceIndex++) {
            if (wordAligned) {
                result.words.set(i, word(sourceIndex));
            } else {
                long wd1 = word(sourceIndex);
                long wd2 = word(sourceIndex + 1);
                long val = (wd1 >>> fromIndex) | (wd2 << -fromIndex);
                result.words.set(i, val);
            }
        }

        long lastWordMask = WORD_MASK >>> -toIndex;
        long val;
        if (((toIndex - 1) & BIT_INDEX_MASK) < (fromIndex & BIT_INDEX_MASK)) {
            val = (word(sourceIndex) >>> fromIndex)
                    | ((word(sourceIndex + 1) & lastWordMask) << -fromIndex);
        } else {
            val = (word(sourceIndex) & lastWordMask) >>> fromIndex;
        }
        result.words.set(targetWords - 1, val);
        // Set wordsInUse correctly
        result.wordsInUse = targetWords;
        result.recalculateWordsInUse();
        result.sanityCheck();

        return result;
    }

    public long nextSetBit(long startingBit) {
        if (startingBit < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + startingBit);
        }
        sanityCheck();
        long u = wordIndex(startingBit);
        if (u >= wordsInUse) {
            return -1;
        }
        long word = word(u) & (WORD_MASK << startingBit);
        for (;;) {
            if (word != 0) {
                return (u * (long) BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return -1;
            }
            word = word(u);
        }
    }

    public long nextClearBit(long startingBit) {
        if (startingBit < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + startingBit);
        }
        sanityCheck();
        long u = wordIndex(startingBit);
        if (u >= wordsInUse) {
            return startingBit;
        }
        long wm = WORD_MASK;
        long word = ~word(u) & (wm << startingBit);
        for (;;) {
            if (word != 0) {
                // XXX - don't we want LEADING zeros?
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return wordsInUse * BITS_PER_WORD;
            }
            word = ~word(u);
        }
    }

    public long previousSetBit(long fromBit) {
        if (fromBit < 0) {
            if (fromBit == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromBit);
        }
        sanityCheck();
        long u = wordIndex(fromBit);
        if (u >= wordsInUse) {
            return length() - 1;
        }
        long word = word(u) & (WORD_MASK >>> -(fromBit + 1));
        for (;;) {
            if (word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = word(u);
        }
    }

    public long previousClearBit(long fromBit) {
        if (fromBit < 0) {
            if (fromBit == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromBit);
        }

        sanityCheck();
        long u = wordIndex(fromBit);
        if (u >= wordsInUse) {
            return fromBit;
        }
        long word = ~word(u) & (WORD_MASK >>> -(fromBit + 1));

        for (;;) {
            if (word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = ~word(u);
        }
    }

    public boolean intersects(LongArrayBitSet set) {
        for (long i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            if ((word(i) & set.word(i)) != 0) {
                return true;
            }
        }
        return false;
    }

    public void andNot(LongArrayBitSet set) {
        for (long i = Math.min(wordsInUse, set.wordsInUse) - 1; i >= 0; i--) {
            long index = i;
            words.update(i, val -> {
                return val & ~set.word(index);
            });
        }
        recalculateWordsInUse();
        sanityCheck();
    }

    public void and(LongArrayBitSet set) {
        if (this == set) {
            return;
        }
        while (wordsInUse > set.wordsInUse) {
            words.set(--wordsInUse, 0);
        }
        for (long i = 0; i < wordsInUse; i++) {
            long index = i;
            words.update(i, val -> {
                return val & set.word(index);
            });
        }
        recalculateWordsInUse();
        sanityCheck();
    }

    public void xor(LongArrayBitSet set) {
        long wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse) {
            maybeGrow(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (long i = 0; i < wordsInCommon; i++) {
            long index = i;
            words.update(i, val -> {
                return val ^ set.word(index);
            });
        }
        if (wordsInCommon < set.wordsInUse) {
            words.copy(wordsInCommon, set.words, wordsInCommon, wordsInUse - wordsInCommon, true);
        }
        recalculateWordsInUse();
        sanityCheck();
    }

    public void or(LongArrayBitSet set) {
        if (this == set) {
            return;
        }
        long wordsInCommon = Math.min(wordsInUse, set.wordsInUse);
        if (wordsInUse < set.wordsInUse) {
            maybeGrow(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }
        for (long i = 0; i < wordsInCommon; i++) {
            long index = i;
            words.update(i, val -> {
                return val | set.word(index);
            });
        }
        if (wordsInCommon < set.wordsInUse) {
            words.copy(wordsInCommon, set.words, wordsInCommon, wordsInUse - wordsInCommon, true);
        }
        sanityCheck();
    }

    @Override
    public int hashCode() {
        long h = 1234;
        for (long i = wordsInUse; --i >= 0;) {
            h ^= word(i) * (i + 1);
        }

        return (int) ((h >> 32) ^ h);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LongArrayBitSet)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        LongArrayBitSet set = (LongArrayBitSet) obj;

        sanityCheck();
        set.sanityCheck();

        if (wordsInUse != set.wordsInUse) {
            return false;
        }

        // Check words in use by both BitSets
        for (long i = 0; i < wordsInUse; i++) {
            if (word(i) != set.word(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Object clone() {
        if (!stickySize) {
            trim();
        }
        try {
            LongArrayBitSet result = (LongArrayBitSet) super.clone();
            result.words = (LongArray) words.clone();
            result.sanityCheck();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public String toString() {
        sanityCheck();

        long numBits = (wordsInUse > 128)
                ? cardinality() : wordsInUse * BITS_PER_WORD;
        long sizeFactor = 6 * numBits + 2;
        StringBuilder b = sizeFactor < Integer.MAX_VALUE
                ? new StringBuilder((int) sizeFactor) : new StringBuilder(1000);
        b.append('{');

        long i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) {
                    break;
                }
                if ((i = nextSetBit(i)) < 0) {
                    break;
                }
                long endOfRun = nextClearBit(i);
                do {
                    b.append(", ").append(i);
                } while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    public LongStream stream() {
        class BitSetIterator implements PrimitiveIterator.OfLong {

            long next = nextSetBit(0);

            @Override
            public boolean hasNext() {
                return next != -1;
            }

            @Override
            public long nextLong() {
                if (next != -1) {
                    long ret = next;
                    next = nextSetBit(next + 1);
                    return ret;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        return StreamSupport.longStream(
                () -> Spliterators.spliterator(
                        new BitSetIterator(), cardinality(),
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED),
                Spliterator.SIZED | Spliterator.SUBSIZED
                | Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED,
                false);
    }

    @SuppressWarnings("empty-statement")
    public static LongArrayBitSet valueOf(long[] longs, LongFunction<LongArray> factory) {
        int n;
        for (n = longs.length; n > 0 && longs[n - 1] == 0; n--)
            ;
        return new LongArrayBitSet(Arrays.copyOf(longs, n), factory);
    }

    @SuppressWarnings("empty-statement")
    public static LongArrayBitSet valueOf(LongBuffer lb, LongFunction<LongArray> factory) {
        lb = lb.slice();
        int n;
        for (n = lb.remaining(); n > 0 && lb.get(n - 1) == 0; n--)
            ;
        long[] words = new long[n];
        lb.get(words);
        return new LongArrayBitSet(words, factory);
    }

    private static void checkRange(long fromIndex, long toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        if (toIndex < 0) {
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex
                    + " > toIndex: " + toIndex);
        }
    }

    private void sanityCheck() {
        boolean wordsInUseSane = (wordsInUse == 0 || word(wordsInUse - 1) != 0);
        if (!wordsInUseSane) {
            assert (wordsInUse == 0 || word(wordsInUse - 1) != 0) : "wordsInUse: " + wordsInUse + " lastWord " + (wordsInUse == 0 ? -1 : word(wordsInUse - 1)) + " " + words.toString();
        }
        assert (wordsInUse >= 0 && wordsInUse <= words.size()) : "wordsInUse " + wordsInUse + " words size " + words.size();
        assert (wordsInUse == words.size() || word(wordsInUse) == 0) : "Words in use " + wordsInUse + " word(wordsInUse) " + word(wordsInUse);
    }
}
