package com.mastfrog.bits;

import java.io.Serializable;
import java.util.BitSet;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

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
 * </p>
 *
 * @author Tim Boudreau
 */
public interface Bits extends Serializable {

    public static final Bits EMPTY = EmptyBits.INSTANCE;

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
        boolean[] result = new boolean[1];
        forEachLongSetBitAscending(bitIndex, bit -> {
            result[0] = bit == bitIndex;
            return false;
        });
        return result[0];
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
        for (int i = fromIndex; i < toIndex; i++) {
            bs.set(i, get(i));
        }
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
        boolean[] result = new boolean[1];
        set.forEachLongSetBitAscending(bit -> {
            if (get(bit)) {
                result[0] = true;
                return false;
            }
            return true; // keep going
        });
        return result[0];
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
        return previousSetBit(Integer.MAX_VALUE);
    }

    /**
     * The index of the last set bit + 1 - the logical length of this bit set.
     *
     * @return The length
     */
    default long longLength() {
        return previousSetBitLong(Long.MAX_VALUE) + 1;
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
        if (fromIndex > max()) {
            fromIndex = max();
        } else if (fromIndex < min()) {
            return -1;
//            fromIndex = min();
        }
        return previousSetBit((int) fromIndex);
    }

    default long previousClearBitLong(long fromIndex) {
        if (fromIndex > max()) {
            fromIndex = max();
        } else if (fromIndex < min()) {
//            fromIndex = min();
            return -1;
        }
        return previousClearBit((int) fromIndex);
    }

    default long nextSetBitLong(long fromIndex) {
        if (fromIndex > max()) {
            fromIndex = max();
        } else if (fromIndex < min()) {
//            fromIndex = min();
            return -1;
        }
        return nextSetBit((int) fromIndex);
    }

    default long nextClearBitLong(long fromIndex) {
        if (fromIndex > max()) {
            fromIndex = max();
        } else if (fromIndex < min()) {
            fromIndex = min();
        }
        return nextClearBit((int) fromIndex);
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
     * passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default int forEachUnsetBitAscending(IntConsumer consumer) {
        int count = 0;
        for (int bit = nextClearBit(min()); bit >= min(); bit = nextClearBit(bit + 1)) {
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
        for (int bit = previousClearBit(max()); bit >= min(); bit = previousClearBit(bit - 1)) {
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
        for (int bit = nextClearBit(min()); bit >= min(); bit = nextClearBit(bit + 1)) {
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
        for (int bit = previousClearBit(max()); bit >= min(); bit = previousClearBit(bit - 1)) {
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
        for (int bit = nextClearBit(from); bit >= min(); bit = nextClearBit(bit + 1)) {
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
        for (int bit = previousClearBit(from); bit >= min(); bit = previousClearBit(bit - 1)) {
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
        for (int bit = nextClearBit(start); bit >= min(); bit = nextClearBit(bit + 1)) {
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
        for (int bit = previousClearBit(start); bit >= min(); bit = previousClearBit(bit - 1)) {
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
        for (int bit = nextClearBit(from); bit >= min() && bit < upTo; bit = nextClearBit(bit + 1)) {
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
            downTo = min() == Integer.MIN_VALUE ? Integer.MIN_VALUE : min() - 1;
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
        for (int bit = nextClearBit(from); bit >= min() && bit < upTo; bit = nextClearBit(bit + 1)) {
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
        return Long.MAX_VALUE;
    }

    // Long variants
    /**
     * Traverse all bits which are set, starting at the lowest bit index,
     * passing them to the passed consumer.
     *
     * @param consumer A consumer
     */
    default long forEachLongSetBitAscending(LongConsumer consumer) {
        long count = 0;
        for (long bit = nextSetBitLong(minLong()); bit >= min(); bit = nextSetBitLong(bit + 1)) {
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
        for (long bit = previousSetBitLong(maxLong()); bit >= minLong(); bit = previousSetBitLong(bit - 1)) {
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
        return filter(MutableBits::create, pred).immutableCopy();
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
}
