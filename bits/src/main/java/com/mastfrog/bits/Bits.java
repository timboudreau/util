package com.mastfrog.bits;

import static com.mastfrog.bits.Bits.Characteristics.LONG_VALUED;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.LongBiConsumer;
import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 * Read only interface to a BitSet or BitSet-like data structure, which is
 * api-compatible with the read-only methods of BitSet. Also supports
 * long-indexed bit sets for very large bit sets; the default implementation
 * simply casts to int, converting to Integer.MAX_VALUE or Integer.MIN_VALUE
 * where passed values are out of range or throws an exception.
 * <p>
 * Implementations which are long-indexed should implement *all* of the methods
 * in this class; the default implementations of long-based methods are
 * sufficient for int-based variants, though a specific implementation might be
 * able to perform them more efficiently.
 * </p><p>
 * The main purpose of this interface is to provide an immutable interface to
 * BitSet, and allow for long-indexed and off-heap implementations.
 * </p><p>
 * The return value of <code>hashCode()</code> for a Bits should be identical to
 * that of a BitSet with the same bits set; a default implementation which
 * computes that without creating large arrays is provided via the method
 * <code>bitsHashCode()</code>.
 *
 * @author Tim Boudreau
 */
public interface Bits extends Serializable {

    public static final Bits EMPTY = new EmptyBits();

    default boolean canContain(int index) {
        return index > 0;
    }

    /**
     * Get the number of set bits.
     *
     * @return The number of set bits
     */
    int cardinality();

    /**
     * Create a read-only copy of this bit set.
     *
     * @return A copy
     */
    Bits copy();

    /**
     * Create a mutable copy of this bit set.
     *
     * @return A copy
     */
    MutableBits mutableCopy();

    /**
     * Convert to an ordinary Java BitSet.
     *
     * @return
     */
    default BitSet toBitSet() {
        BitSet result = new BitSet(length());
        for (int i = 0; i < length(); i++) {
            if (get(i)) {
                result.set(i);
            }
        }
        return result;
    }

    /**
     * Create a read-only Bits from a Java BitSet.
     *
     * @param bitSet A BitSet
     * @return A new Bits instance wrapping it
     */
    public static Bits fromBitSet(BitSet bitSet) {
        return new BitSetBits(bitSet);
    }

    default int leastSetBit() {
        return nextSetBit(min());
    }

    default long leastSetBitLong() {
        return nextSetBitLong(minLong());
    }

    /**
     * Logical or's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default Bits orWith(Bits other) {
        if (other == this) {
            return copy();
        }
        MutableBits copy = mutableCopy();
        copy.or(other);
        return copy;
    }

    /**
     * Logical xor's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default Bits xorWith(Bits other) {
        MutableBits copy = mutableCopy();
        copy.xor(other);
        return copy;
    }

    /**
     * Logical and's this and another set, returning a new instance containing
     * the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default Bits andWith(Bits other) {
        if (other == this) {
            return copy();
        }
        MutableBits copy = mutableCopy();
        copy.and(other);
        return copy;
    }

    /**
     * Logical andNot's this and another set, returning a new instance
     * containing the result.
     *
     * @param other Another bit set
     * @return A new Bits which containing the result
     */
    default Bits andNotWith(Bits other) {
        if (other == this) {
            return Bits.EMPTY;
        }
        MutableBits copy = mutableCopy();
        copy.andNot(other);
        return copy;
    }

    /**
     * Compute a sum over all set bits. Useful for implementing various graph
     * scoring algorithms.
     *
     * @param f A function
     * @return The sum of calling f.apply() for each set bit
     */
    public default double sum(IntToDoubleFunction f) {
        double result = 0D;
        for (int bit = nextSetBit(0); bit >= 0; bit = nextSetBit(bit + 1)) {
            result += f.applyAsDouble(bit);
        }
        return result;
    }

    /**
     * Compute a sum using set bits as indexes into the passed array. Useful for
     * implementing various graph scoring algorithms.
     *
     * @param values
     * @param ifNot A bit to skip summing
     * @return The sum of the value in the array at the index of each set bit
     */
    default double sum(double[] values, int ifNot) {
        double result = 0.0;
        for (int bit = nextSetBit(0); bit >= 0; bit = nextSetBit(bit + 1)) {
            if (bit != ifNot) {
                result += values[bit];
            }
        }
        return result;
    }

    default double sum(DoubleLongFunction f) {
        double result = 0D;
        for (long bit = nextSetBitLong(0); bit >= 0; bit = nextSetBitLong(bit + 1)) {
            result += f.apply(bit);
        }
        return result;
    }

    default boolean isNativelyLongIndexed() {
        return false;
    }

    /**
     * Create an immutable copy of these bits.
     *
     * @return A copy
     */
    default Bits immutableCopy() {
        return copy();
    }

    default long cardinalityLong() {
        return forEachLongSetBitAscending(ignored -> {
        });
    }

    /**
     * Get the state of the bit at the specified index.
     *
     * @param bitIndex The bit index
     * @return A bit
     */
    boolean get(int bitIndex);

    /**
     * Get the state of the bit at the specified index.
     *
     * @param bitIndex The bit index
     * @return A bit
     */
    default boolean get(long bitIndex) {
        Bool result = Bool.create();
        forEachLongSetBitAscending(bitIndex, bit -> {
            result.set(bit == bitIndex);
            return false;
        });
        return result.getAsBoolean();
    }

    /**
     * Get a copy of a region of bits from this bit set.
     *
     * @param fromIndex The start index
     * @param toIndex The end index
     * @return A set of bits
     */
    default Bits get(int fromIndex, int toIndex) {
        BitSet bs = new BitSet();
        forEachSetBitAscending(fromIndex, toIndex, bit -> {
            bs.set(bit);
        });
        return Bits.fromBitSet(bs);
    }

    /**
     * Get a copy of a region of bits from this bit set.
     *
     * @param fromIndex The start index
     * @param toIndex The end index
     * @return A set of bits
     */
    default Bits get(long fromIndex, long toIndex) {
        if (fromIndex < Integer.MIN_VALUE || fromIndex > Integer.MAX_VALUE
                || toIndex < Integer.MIN_VALUE || toIndex > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("This implementation does not "
                    + "actually support long indexed values, and the passed "
                    + "value cannot be represented as an int");
        }
        return get((int) fromIndex, (int) toIndex);
    }

    /**
     * Determine if this Bits intersects another
     *
     * @param set A bit set
     * @return Whether or not they intersect
     */
    default boolean intersects(Bits set) {
        Bool result = Bool.create();
        set.forEachLongSetBitAscending(bit -> {
            if (get(bit)) {
                result.set();
                return false;
            }
            return true; // keep going
        });
        return result.getAsBoolean();
    }

    /**
     * Determine if no bits are set.
     *
     * @return True if no bits are set
     */
    default boolean isEmpty() {
        return cardinalityLong() == 0L;
    }

    /**
     * The length of this bit set, per the contract of BitSet.
     *
     * @return A length
     */
    default int length() {
        return previousSetBit(max());
    }

    /**
     * The index of the last set bit + 1 - the logical length of this bit set.
     *
     * @return The length
     */
    default long longLength() {
        long result = previousSetBitLong(max());
        return result == Long.MAX_VALUE ? Long.MAX_VALUE : result + 1;
    }

    /**
     * Get the next unset bit at or after the passed index.
     *
     * @param fromIndex An index
     * @return A bit index or -1
     */
    int nextClearBit(int fromIndex);

    /**
     * Get the next set bit.
     *
     * @param fromIndex The starting index
     * @return An index or -1 if none
     */
    int nextSetBit(int fromIndex);

    /**
     * Get the first clear bit preceding the passed index.
     *
     * @param fromIndex An index
     * @return A bit index or -1 if none
     */
    int previousClearBit(int fromIndex);

    /**
     * Get the first set bit preceding the passed index, per the contract of
     * BitSet.previousSetBit().
     *
     * @param fromIndex An index
     * @return An index or -1
     */
    int previousSetBit(int fromIndex);

    default String stringValue() {
        StringBuilder sb = new StringBuilder(80).append('[');
        forEachLongSetBitAscending(bit -> {
            sb.append(bit).append(',');
        });
        return sb.append(']').toString();
    }

    default long previousSetBitLong(long fromIndex) {
        long max = maxLong();
        if (fromIndex > max) {
            fromIndex = max;
        } else if (fromIndex < min()) {
            return -1;
        }
        return previousSetBit((int) fromIndex);
    }

    default long previousClearBitLong(long fromIndex) {
        long max = maxLong();
        if (fromIndex > max) {
            fromIndex = max;
        } else if (fromIndex < min()) {
            return fromIndex;
        }
        return previousClearBit((int) fromIndex);
    }

    default long nextSetBitLong(long fromIndex) {
        long max = maxLong();
        long min;
        if (fromIndex > max) {
            fromIndex = max;
        } else if (fromIndex < (min = minLong())) {
            return min;
        }
        return nextSetBit((int) fromIndex);
    }

    default long nextClearBitLong(long fromIndex) {
        long max = maxLong();
        long min;
        if (fromIndex > max) {
            fromIndex = max;
        } else if (fromIndex < (min = minLong())) {
            fromIndex = min;
        }
        return nextClearBit((int) fromIndex);
    }

    default int visitRanges(IntBiConsumer c) {
        if (isEmpty()) {
            return 0;
        }
        Int start = Int.of(Integer.MIN_VALUE);
        Int end = Int.of(Integer.MIN_VALUE);
        Int result = Int.create();
        IntConsumer emit = (nue) -> {
            int st = start.getAsInt();
            int en = end.getAsInt();
            if (st >= 0 && en >= 0) {
                result.increment();
                c.accept(st, en);
            }
            start.set(nue);
            end.set(nue);
        };
        forEachSetBitAscending(bit -> {
            if (start.getAsInt() == -1) {
                start.set(bit);
                end.set(bit);
            } else {
                if (bit == start.getAsInt() + 1) {
                    end.increment();
                } else {
                    emit.accept(bit);
                }
            }
        });
        emit.accept(-1);
        return result.getAsInt();
    }

    default long visitRangesLong(LongBiConsumer c) {
        if (isEmpty()) {
            return 0;
        }
        Lng start = Lng.of(Integer.MIN_VALUE);
        Lng end = Lng.of(Integer.MIN_VALUE);
        Lng result = Lng.create();
        LongConsumer emit = (nue) -> {
            long st = start.getAsLong();
            long en = end.getAsLong();
            if (st >= 0 && en >= 0) {
                result.increment();
                c.accept(st, en);
            }
            start.set(nue);
            end.set(nue);
        };
        forEachLongSetBitAscending(bit -> {
            if (start.getAsLong() == -1) {
                start.set(bit);
                end.set(bit);
            } else {
                if (bit == start.getAsLong() + 1) {
                    end.increment();
                } else {
                    emit.accept(bit);
                }
            }
        });
        emit.accept(-1);
        return result.getAsLong();
    }

    /**
     * Default implementation for equals that will work with all
     * implementations, but specific implementations should compare backing
     * stores when possible.
     *
     * @param other
     * @return
     */
    default boolean contentEquals(Bits other) {
        if (other == this) {
            return true;
        } else if (other == null) {
            return false;
        }
        long myLength = longLength();
        long otherLength = other.longLength();
        if (myLength != otherLength) {
            return false;
        }
        for (long a = nextSetBitLong(0), b = other.nextSetBitLong(0), c = previousSetBitLong(Long.MAX_VALUE), d = other.previousSetBitLong(Long.MAX_VALUE);
                a >= min() && b >= other.min() && c >= min() && d != other.min() && a > c && b > d;
                a = nextSetBitLong(a + 1), b = other.nextSetBitLong(b + 1), c = previousSetBitLong(c - 1), d = other.previousSetBitLong(d - 1)) {
            if (a != b || c != d) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes a hash code for the bits in this bits identically to
     * BitSet.hashCode(), without creating a giant array in the process.
     * Implementations based on BitSet should use BitSet.hashCode() instead.
     *
     * @return
     */
    default int bitsHashCode() {
        long h = 1234;
        long lastBit = previousSetBitLong(Long.MAX_VALUE);
        long currentValue = 0;
        long lastIndexInLongArray = -1;
        long previousBit = -1;
        for (long bit = lastBit; bit >= 0; bit = previousSetBitLong(bit - 1)) {
            assert bit != previousBit : "Same bit twice: " + bit;
            previousBit = bit;
            long bitPosition = (bit % Long.SIZE);
            long indexInLongArray = (bit / Long.SIZE);
            if (indexInLongArray != lastIndexInLongArray && currentValue != 0) {
                // At a long boundary
                if (lastIndexInLongArray != -1) {
                    // write last value
                    h ^= currentValue * (lastIndexInLongArray + 1);
                    currentValue = 1L << bitPosition;
                }
            } else {
                long v = 1L << bitPosition;
                currentValue |= v;
            }
            lastIndexInLongArray = indexInLongArray;
        }
        if (currentValue != 0 && lastIndexInLongArray >= 0) {
            h ^= currentValue * (lastIndexInLongArray + 1);
        }
        return (int) ((h >> 32) ^ h);
    }

    /**
     * Convert this Bits to an array of Longs (if possible).
     *
     * @return A long array
     */
    default long[] toLongArray() {
        if (isEmpty()) {
            return new long[0];
        }
        long ls = this.longLength();
        if (ls == 0) {
            return new long[0];
        } else if (ls > Integer.MAX_VALUE) {
            throw new IllegalStateException("Array size would exceed "
                    + "Integer.MAX_VALUE");
        }
        long arrayLength = (ls / Long.SIZE);
        if (ls % Long.SIZE != 0) {
            arrayLength++;
        }
        if (arrayLength > Integer.MAX_VALUE) {
            throw new IllegalStateException("Required array size too large");
        }
        long[] result = new long[(int) arrayLength];
        forEachSetBitAscending(bit -> {
            int position = bit % Long.SIZE;
            int offset = bit / Long.SIZE;
            result[offset] |= 1L << position;
        });
        return result;
    }

    default byte[] toByteArray() {
        if (isEmpty()) {
            return new byte[0];
        }
        long ls = this.longLength();
        if (ls == 0) {
            return new byte[0];
        }
        long arrayLength = (ls / Byte.SIZE);
        if (ls % Byte.SIZE != 0) {
            arrayLength++;
        }
        if (arrayLength > Integer.MAX_VALUE) {
            throw new IllegalStateException("Required array size too large");
        }
        byte[] result = new byte[(int) arrayLength];
        forEachSetBitAscending(bit -> {
            int position = bit % Byte.SIZE;
            int offset = bit / Byte.SIZE;
            result[offset] |= 1L << position;
        });
        return result;
    }

    /**
     * Traverse all bits which are set, starting at the lowest bit index,
     * passing them to the passed consumer.
     *
     * @param consumer A consumer
     * @return the number of bit indices traversed
     */
    default int forEachSetBitAscending(IntConsumer consumer) {
        int count = 0;
        for (int bit = nextSetBit(min()); bit >= min(); bit = nextSetBit(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all bits which are set in reverse, starting at the
     * <i>highest</i>
     * bit index, passing them to the passed consumer.
     *
     * @param consumer A consumer
     * @return the number of bit indices traversed
     */
    default int forEachSetBitDescending(IntConsumer consumer) {
        int count = 0;
        for (int bit = previousSetBit(max()); bit >= min(); bit = previousSetBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all bits which are set, stopping if the predicate returns false.
     *
     * @param consumer A consumer
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitAscending(IntPredicate consumer) {
        int count = -1;
        for (int bit = nextSetBit(min()); bit >= min(); bit = nextSetBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are set in reverse, stopping if the predicate
     * returns false.
     *
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitDescending(IntPredicate consumer) {
        int count = -1;
        for (int bit = previousSetBit(max()); bit >= min(); bit = previousSetBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed index, returning the count
     * of bits traversed.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return The number of bits that were passed to the consumer
     */
    default int forEachSetBitAscending(int from, IntConsumer consumer) {
        int count = 0;
        for (int bit = nextSetBit(from); bit >= min(); bit = nextSetBit(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the starting point, passing them to the
     * passed consumer.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return the number of bit indices passed to the consumer
     */
    default int forEachSetBitDescending(int from, IntConsumer consumer) {
        int count = 0;
        for (int bit = previousSetBit(from); bit >= min(); bit = previousSetBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed starting point, stopping if
     * the passed predicate returns false.
     *
     * @param start A start point
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitAscending(int start, IntPredicate consumer) {
        int count = -1;
        for (int bit = nextSetBit(start); bit >= min(); bit = nextSetBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or before the passed starting point, passing
     * them to the passed predicate and aborting if it returns false.
     *
     * @param start The starting bit
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitDescending(int start, IntPredicate consumer) {
        int count = -1;
        for (int bit = previousSetBit(start); bit >= min(); bit = previousSetBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed starting point which are
     * <i>less than</i> the passed stopping point.
     *
     * @param from The starting point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A consumer
     * @return The number of bit indices passed to the consumer
     */
    default int forEachSetBitAscending(int from, int upTo, IntConsumer consumer) {
        int count = 0;
        for (int bit = nextSetBit(from); bit >= min() && bit < upTo; bit = nextSetBit(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse set bit indices equal to or less than the passed starting point,
     * which are also greater than the passed stopping point.
     *
     * @param from The start point, inclusive
     * @param downTo The stop point, exclusive
     * @param consumer A consumer
     * @return The number of bits passed to the consumer
     */
    default int forEachSetBitDescending(int from, int downTo, IntConsumer consumer) {
        if (from < downTo) {
            return 0;
        }
        if (downTo < 0) {
            downTo = downTo == Integer.MIN_VALUE ? Integer.MIN_VALUE : downTo - 1;
        }
        int count = 0;
        for (int bit = previousSetBit(from); bit > downTo; bit = previousSetBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse set bits at or after the passed starting point, which are less
     * than the passed stopping point, aborting if the passed predicate returns
     * false, returning the total number of bits passed to the predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitAscending(int from, int upTo, IntPredicate consumer) {
        int count = -1;
        for (int bit = nextSetBit(from); bit >= min() && bit < upTo; bit = nextSetBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse set bits at or below the passed starting point, which are
     * greater than than the passed stopping point, aborting if the passed
     * predicate returns false, returning the total number of bits passed to the
     * predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachSetBitDescending(int from, int downTo, IntPredicate consumer) {
        int count = -1;
        for (int bit = previousSetBit(from); bit > downTo; bit = previousSetBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are unset, starting at the lowest bit index,
     * passing them to the passed consumer.  <b>Note:</b> this method will not
     * iterate any bits beyond the greatest set bit, since that would cause the
     * common case to be to continue iterating up to
     * <code>Integer.MAX_VALUE</code>.
     *
     * @param consumer A consumer
     */
    default int forEachUnsetBitAscending(IntConsumer consumer) {
        int count = 0;
        int len = length();
        int min = min();
        for (int bit = nextClearBit(min); bit >= min && bit < len; bit = nextClearBit(bit + 1)) {
            if (bit < 0) { // overflow
                break;
            }
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all bits which are unset in reverse, starting at the
     * <i>highest</i>
     * bit index, passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default int forEachUnsetBitDescending(IntConsumer consumer) {
        int count = 0;
        int min = min();
        for (int bit = previousClearBit(max()); bit >= min; bit = previousClearBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all bits which are unset, stopping if the predicate returns
     * false.
     *
     * @param consumer A consumer
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitAscending(IntPredicate consumer) {
        int count = -1;
        int len = length();
        int min = min();
        for (int bit = nextClearBit(min); bit >= min && bit < len; bit = nextClearBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are unset in reverse, stopping if the predicate
     * returns false.
     *
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitDescending(IntPredicate consumer) {
        int count = -1;
        int min = min();
        for (int bit = previousClearBit(max()); bit >= min; bit = previousClearBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed index, returning the count
     * of bits traversed.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return The number of bits that were passed to the consumer
     */
    default int forEachUnsetBitAscending(int from, IntConsumer consumer) {
        int count = 0;
        int min = min();
        int max = previousSetBit(max());
        for (int bit = nextClearBit(from); bit >= min; bit = nextClearBit(bit + 1)) {
            if (bit > max) {
                break;
            }
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the starting point, passing them to
     * the passed consumer.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return the number of bit indices passed to the consumer
     */
    default int forEachUnsetBitDescending(int from, IntConsumer consumer) {
        int count = 0;
        int min = min();
        for (int bit = previousClearBit(from); bit >= min; bit = previousClearBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed starting point, stopping
     * if the passed predicate returns false.
     *
     * @param start A start point
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitAscending(int start, IntPredicate consumer) {
        int count = -1;
        int min = min();
        int len = length();
        for (int bit = nextClearBit(start); bit >= min && bit < len; bit = nextClearBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or before the passed starting point, passing
     * them to the passed predicate and aborting if it returns false.
     *
     * @param start The starting bit
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitDescending(int start, IntPredicate consumer) {
        int count = -1;
        int min = min();
        for (int bit = previousClearBit(start); bit >= min; bit = previousClearBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed starting point which are
     * <i>less than</i> the passed stopping point.
     *
     * @param from The starting point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A consumer
     * @return The number of bit indices passed to the consumer
     */
    default int forEachUnsetBitAscending(int from, int upTo, IntConsumer consumer) {
        int count = 0;
        int min = min();
        int max = Math.min(upTo, previousSetBit(Integer.MAX_VALUE));
        for (int bit = nextClearBit(from); bit >= min && bit < max; bit = nextClearBit(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse unset bit indices equal to or less than the passed starting
     * point, which are also greater than the passed stopping point.
     *
     * @param from The start point, inclusive
     * @param downTo The stop point, exclusive
     * @param consumer A consumer
     * @return The number of bits passed to the consumer
     */
    default int forEachUnsetBitDescending(int from, int downTo, IntConsumer consumer) {
        if (from < downTo) {
            return 0;
        }
        if (downTo < 0) {
            int min = min();
            downTo = min == Integer.MIN_VALUE ? Integer.MIN_VALUE : min - 1;
        }
        int count = 0;
        for (int bit = previousClearBit(from); bit > downTo; bit = previousClearBit(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse unset bits at or after the passed starting point, which are less
     * than the passed stopping point, aborting if the passed predicate returns
     * false, returning the total number of bits passed to the predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitAscending(int from, int upTo, IntPredicate consumer) {
        int count = -1;
        int min = min();
        for (int bit = nextClearBit(from); bit >= min && bit < upTo; bit = nextClearBit(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse unset bits at or below the passed starting point, which are
     * greater than than the passed stopping point, aborting if the passed
     * predicate returns false, returning the total number of bits passed to the
     * predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default int forEachUnsetBitDescending(int from, int downTo, IntPredicate consumer) {
        int count = -1;
        for (int bit = previousClearBit(from); bit > downTo; bit = previousClearBit(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get the minimum value a bit can have (this is used when creating shifted
     * Bits instances to correctly test upper and lower bounds).
     *
     * @return zero by default
     */
    default int min() {
        return 0;
    }

    /**
     * Get the maximum value a bit can have (this is used when creating shifted
     * Bits instances to correctly test upper and lower bounds). Note that while
     * some long-indexed versions may exceed Integer.MAX_VALUE, all backing
     * store implementations (memory mapped int-indexed ByteBuffers, and even
     * sun.misc.Unsafe) preclude actually going as high as Long.MAX_VALUE.
     *
     * @return Integer.MAX_VALUE by default
     */
    default int max() {
        return Integer.MAX_VALUE;
    }

    /**
     * Get the minimum value a bit can have (this is used when creating shifted
     * Bits instances to correctly test upper and lower bounds).
     *
     * @return zero by default
     */
    default long minLong() {
        return 0L;
    }

    /**
     * Get the maximum value a bit can have (this is used when creating shifted
     * Bits instances to correctly test upper and lower bounds). Note that while
     * some long-indexed versions may exceed Integer.MAX_VALUE, all backing
     * store implementations (memory mapped int-indexed ByteBuffers, and even
     * sun.misc.Unsafe) preclude actually going as high as Long.MAX_VALUE.
     *
     * @return Long.MAX_VALUE by default
     */
    default long maxLong() {
        return isNativelyLongIndexed() ? Long.MAX_VALUE : max();
    }

    // Long variants
    /**
     * Traverse all bits which are set, starting at the lowest bit index,
     * passing them to the passed consumer.<b>Note:</b> this method will not
     * iterate any bits beyond the greatest set bit, since that would cause the
     * common case to be to continue iterating up to
     * <code>Integer.MAX_VALUE</code>.
     *
     * @param consumer A consumer
     */
    default long forEachLongSetBitAscending(LongConsumer consumer) {
        long count = 0;
        long min = minLong();
        long max = this.previousSetBitLong(Long.MAX_VALUE) + 1;
        for (long bit = nextSetBitLong(min); bit >= min && bit < max; bit = nextSetBitLong(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all bits which are set in reverse, starting at the
     * <i>highest</i>
     * bit index, passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default void forEachLongSetBitDescending(LongConsumer consumer) {
        long initial = nextSetBitLong(minLong());
        for (long bit = previousSetBitLong(maxLong()); bit >= initial; bit = previousSetBitLong(bit - 1)) {
            consumer.accept(bit);
        }
    }

    /**
     * Traverse all bits which are set, stopping if the predicate returns false.
     *
     * @param consumer A consumer
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were tested
     */
    default long forEachLongSetBitAscending(LongPredicate consumer) {
        long count = -1;
        for (long bit = nextSetBitLong(minLong()); bit >= minLong(); bit = nextSetBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are set in reverse, stopping if the predicate
     * returns false.
     *
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default long forEachLongSetBitDescending(LongPredicate consumer) {
        long count = -1;
        for (long bit = previousSetBitLong(maxLong()); bit >= minLong(); bit = previousSetBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed index, returning the count
     * of bits traversed.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return The number of bits that were passed to the consumer
     */
    default long forEachLongSetBitAscending(long from, LongConsumer consumer) {
        long count = 0;
        for (long bit = nextSetBitLong(from); bit >= minLong(); bit = nextSetBitLong(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the starting point, passing them to the
     * passed consumer.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return the number of bit indices passed to the consumer
     */
    default long forEachLongSetBitDescending(long from, LongConsumer consumer) {
        long count = 0;
        for (long bit = previousSetBitLong(from); bit >= minLong(); bit = previousSetBitLong(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed starting point, stopping if
     * the passed predicate returns false.
     *
     * @param start A start point
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default long forEachLongSetBitAscending(long start, LongPredicate consumer) {
        long count = -1;
        for (long bit = nextSetBitLong(start); bit >= minLong(); bit = nextSetBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or before the passed starting point, passing
     * them to the passed predicate and aborting if it returns false.
     *
     * @param start The starting bit
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachLongSetBitDescending(long start, LongPredicate consumer) {
        long count = -1;
        for (long bit = previousSetBitLong(start); bit >= minLong(); bit = previousSetBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all set bits at or after the passed starting point which are
     * <i>less than</i> the passed stopping point.
     *
     * @param from The starting point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A consumer
     * @return The number of bit indices passed to the consumer
     */
    default long forEachLongSetBitAscending(long from, long upTo, LongConsumer consumer) {
        long count = 0;
        for (long bit = nextSetBitLong(from); bit >= minLong() && bit < upTo; bit = nextSetBitLong(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse set bit indices equal to or less than the passed starting point,
     * which are also greater than the passed stopping point.
     *
     * @param from The start point, inclusive
     * @param downTo The stop point, exclusive
     * @param consumer A consumer
     * @return The number of bits passed to the consumer
     */
    default long forEachLongSetBitDescending(long from, long downTo, LongConsumer consumer) {
        if (from < downTo) {
            return 0;
        }
        if (downTo < 0) {
            downTo = minLong();
        }
        long count = 0;
        for (long bit = previousSetBitLong(from); bit > downTo; bit = previousSetBitLong(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse set bits at or after the passed starting point, which are less
     * than the passed stopping point, aborting if the passed predicate returns
     * false, returning the total number of bits passed to the predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true for, or -1 if no
     * bits were found to call the predicate with
     */
    default long forEachLongSetBitAscending(long from, long upTo, LongPredicate consumer) {
        long count = -1;
        for (long bit = nextSetBitLong(from); bit >= minLong() && bit < upTo; bit = nextSetBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse set bits at or below the passed starting point, which are
     * greater than than the passed stopping point, aborting if the passed
     * predicate returns false, returning the total number of bits passed to the
     * predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachLongSetBitDescending(long from, long downTo, LongPredicate consumer) {
        long count = -1;
        for (long bit = previousSetBitLong(from); bit > downTo; bit = previousSetBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are unset, starting at the lowest bit index,
     * passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default void forEachUnsetLongBitAscending(LongConsumer consumer) {
        for (long bit = nextClearBitLong(minLong()); bit >= minLong(); bit = nextClearBitLong(bit + 1)) {
            consumer.accept(bit);
        }
    }

    /**
     * Traverse all bits which are unset in reverse, starting at the
     * <i>highest</i>
     * bit index, passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default void forEachUnsetLongBitDescending(LongConsumer consumer) {
        for (long bit = previousClearBitLong(maxLong()); bit >= minLong(); bit = previousClearBitLong(bit - 1)) {
            consumer.accept(bit);
        }
    }

    /**
     * Traverse all bits which are unset, stopping if the predicate returns
     * false.
     *
     * @param consumer A consumer
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitAscending(LongPredicate consumer) {
        long count = -1;
        for (long bit = nextClearBitLong(minLong()); bit >= minLong(); bit = nextClearBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all bits which are unset in reverse, stopping if the predicate
     * returns false.
     *
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitDescending(LongPredicate consumer) {
        long count = -1;
        for (long bit = previousClearBitLong(maxLong()); bit >= maxLong(); bit = previousClearBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed index, returning the count
     * of bits traversed.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return The number of bits that were passed to the consumer
     */
    default long forEachUnsetLongBitAscending(long from, LongConsumer consumer) {
        long count = 0;
        for (long bit = nextClearBitLong(from); bit >= minLong(); bit = nextClearBitLong(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the starting point, passing them to
     * the passed consumer.
     *
     * @param from The starting bit
     * @param consumer A consumer
     * @return the number of bit indices passed to the consumer
     */
    default long forEachUnsetLongBitDescending(long from, LongConsumer consumer) {
        long count = 0;
        for (long bit = previousClearBitLong(from); bit >= minLong(); bit = previousClearBitLong(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed starting point, stopping
     * if the passed predicate returns false.
     *
     * @param start A start point
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitAscending(long start, LongPredicate consumer) {
        long count = -1;
        for (long bit = nextClearBitLong(start); bit >= minLong(); bit = nextClearBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or before the passed starting point, passing
     * them to the passed predicate and aborting if it returns false.
     *
     * @param start The starting bit
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitDescending(long start, LongPredicate consumer) {
        long count = -1;
        for (long bit = previousClearBitLong(start); bit >= minLong(); bit = previousClearBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse all unset bits at or after the passed starting point which are
     * <i>less than</i> the passed stopping point.
     *
     * @param from The starting point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A consumer
     * @return The number of bit indices passed to the consumer
     */
    default long forEachUnsetLongBitAscending(long from, long upTo, LongConsumer consumer) {
        long count = 0;
        for (long bit = nextClearBitLong(from); bit >= minLong() && bit < upTo; bit = nextClearBitLong(bit + 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse unset bit indices equal to or less than the passed starting
     * point, which are also greater than the passed stopping point.
     *
     * @param from The start point, inclusive
     * @param downTo The stop point, exclusive
     * @param consumer A consumer
     * @return The number of bits passed to the consumer
     */
    default long forEachUnsetLongBitDescending(long from, long downTo, LongConsumer consumer) {
        if (from < downTo) {
            return 0;
        }
        if (downTo < 0) {
            downTo = -1;
        }
        long count = 0;
        for (long bit = previousClearBitLong(from); bit > downTo; bit = previousClearBitLong(bit - 1)) {
            consumer.accept(bit);
            count++;
        }
        return count;
    }

    /**
     * Traverse unset bits at or after the passed starting point, which are less
     * than the passed stopping point, aborting if the passed predicate returns
     * false, returning the total number of bits passed to the predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitAscending(long from, long upTo, LongPredicate consumer) {
        long count = -1;
        for (long bit = nextClearBitLong(from); bit >= minLong() && bit < upTo; bit = nextClearBitLong(bit + 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Traverse unset bits at or below the passed starting point, which are
     * greater than than the passed stopping point, aborting if the passed
     * predicate returns false, returning the total number of bits passed to the
     * predicate.
     *
     * @param from The start point, inclusive
     * @param upTo The end point, exclusive
     * @param consumer A predicate
     * @return The number of bits the predicate returned true, or -1 if no bits
     * were found to call the predicate with
     */
    default long forEachUnsetLongBitDescending(long from, long downTo, LongPredicate consumer) {
        long count = -1;
        for (long bit = previousClearBitLong(from); bit > downTo; bit = previousClearBitLong(bit - 1)) {
            if (!consumer.test(bit)) {
                if (count == -1) {
                    count = 0;
                }
                break;
            } else {
                if (count == -1) {
                    count = 1;
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Create a view of this Bits where the first and last bits are shifted.
     *
     * @param by The amount, positive or negative, to shift by
     * @return A bits
     */
    default Bits shift(int by) {
        return by == 0 ? this : new ShiftedBits(this, by);
    }

    /**
     * Filter a subset of this bits using a predicate.
     *
     * @param pred The predicate
     * @return A bits
     */
    default Bits filter(IntPredicate pred) {
        return filter(this::newBits, pred).immutableCopy();
    }

    /**
     * Filter a subset of this Bits using a predicate and the passed factory
     * function to create the resulting Bits object.
     *
     * @param <M> The type of Bits to create
     * @param factory The factory
     * @param pred The predicate
     * @return A bits
     */
    default <M extends MutableBits> M filter(IntFunction<M> factory, IntPredicate pred) {
        M result = factory.apply(this.length());
        forEachSetBitAscending(bit -> {
            if (pred.test(bit)) {
                result.set(bit);
            }
        });
        return result;
    }

    /**
     * Returns a LongSupplier which will iterate through the set bits, returning
     * -1 after the last bit and then cycling through all the bits again, ad
     * infinitum. Useful for things like resuming the sieving of primes, where
     * you simply need to iterate all previously set bits once before
     * continuing.
     *
     * @return A LongSupplier
     */
    default LongSupplier asLongSupplier() {
        if (isEmpty()) {
            return () -> -1L;
        }
        return new LongSupplier() {
            private long pos = nextSetBitLong(minLong());

            @Override
            public long getAsLong() {
                long old = pos;
                pos = nextSetBitLong(pos + 1L);
                return old;
            }
        };
    }

    /**
     * Returns an IntSupplier which will iterate through the set bits, returning
     * -1 after the last bit and then cycling through all the bits again, ad
     * infinitum. Useful for things like resuming the sieving of primes, where
     * you simply need to iterate all previously set bits once before
     * continuing.
     *
     * @return An IntSupplier
     */
    default IntSupplier asIntSupplier() {
        if (isEmpty()) {
            return () -> -1;
        }
        return new IntSupplier() {
            private int pos = nextSetBit(min());

            @Override
            public int getAsInt() {
                int old = pos;
                pos = nextSetBit(pos + 1);
                return old;
            }
        };
    }

    /**
     * Create a new bits that uses the same backing storage as this one (where
     * possible). This variant, that takes an integer argument, delegates to the
     * one which takes a long argument, which by default returns a BitSet-backed
     * Bits if the size is under Integer.MAX_VALUE, and otherwise returns a
     * long-array backed instance.
     *
     * @param size The number of bits that should be representable
     * @return A mutable bits
     */
    default MutableBits newBits(int size) {
        return newBits((long) size);
    }

    /**
     * Create a new bits that uses the same backing storage as this one (where
     * possible). Implementations should override this method to return their
     * own type; those that cannot handle > Integer.MAX_VALUE bits may call the
     * super method for larger values.
     *
     * @param size The number of bits the returned Bits should be able to
     * accomodate.
     * @return A MutableBits
     */
    default MutableBits newBits(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative size");
        }
        if (size <= Integer.MAX_VALUE) {
            return MutableBits.create((int) size);
        }
        throw new IllegalArgumentException("Not implemented for > Integer.MAX_VALUE - use bits-large");
//        return MutableBits.createLarge(size);
    }

    default Set<Characteristics> characteristics() {
        if (isNativelyLongIndexed()) {
            return EnumSet.of(LONG_VALUED);
        } else {
            return Collections.emptySet();
        }
    }

    public enum Characteristics {
        FIXED_SIZE,
        THREAD_SAFE,
        ATOMIC,
        LARGE,
        LONG_VALUED,
        NEGATIVE_VALUES_ALLOWED,
        OFF_HEAP,
        RLE_COMPRESSED
    }
}
