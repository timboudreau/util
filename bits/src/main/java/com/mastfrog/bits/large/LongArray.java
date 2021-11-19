package com.mastfrog.bits.large;

import java.io.IOException;
import java.nio.file.Path;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

/**
 * Interface to an array-like storage of primitive longs. Serveral
 * implementations are available, including java array, off-heap
 * (sun.misc.Unsafe) and memory mapped file. These are used as backing stores
 * for LongArrayBitSet / LongArrayBitSetBits.
 *
 * @author Tim Boudreau
 */
public interface LongArray extends Cloneable {

    /**
     * Create a new LongArray backed by an ordinary Java primitive array. This
     * is the default for LongArrayBitSet.
     *
     * @param size The size
     * @return An array
     */
    public static LongArray javaLongArray(int size) {
        return new JavaLongArray(size);
    }

    /**
     * Create a new LongArray backed by an ordinary Java primitive array, which
     * uses (does not copy) the passed array.
     *
     * @param content The initial content of the array, which setters will
     * modify
     * @return An array
     */
    public static LongArray javaLongArray(long[] content) {
        return new JavaLongArray(content);
    }

    /**
     * Create a new LongArray backed by off-heap memory allocated via
     * <code>sun.misc.Unsafe</code>. The resulting array's memory will be
     * disposed via a reference queue and timer if it is garbage collected.
     * <p>
     * <i>The array's initial contents will <b>not</b> be initialized to zero -
     * if you need to initialize, use the <code>clear()</code> or
     * <code>fill()</code> methods.</i>
     * </p>
     *
     * @param content The initial content which is copied into this array
     * @return An array
     */
    public static CloseableLongArray unsafeLongArray(long[] content) {
        return new UnsafeLongArray(content);
    }

    /**
     * Create a new LongArray backed by off-heap memory allocated via
     * <code>sun.misc.Unsafe</code>. The resulting array's memory will be
     * disposed via a reference queue and timer if it is garbage collected.
     * <p>
     * <i>The array's initial contents will <b>not</b> be initialized to zero -
     * if you need to initialize, use the <code>clear()</code> or
     * <code>fill()</code> methods.</i>
     * </p>
     *
     * @param size The initial size of the array
     * @return An array
     */
    public static CloseableLongArray unsafeLongArray(long size) {
        return new UnsafeLongArray(size);
    }

    /**
     * Create a new LongArray backed by a memory mapped file in the system
     * temporary directory.
     *
     * @param size The size
     * @return An array
     */
    public static CloseableLongArray mappedFileLongArray(long size) {
        return new MappedFileLongArray(size);
    }

    /**
     * Create a new LongArray backed by the passed file. If the file exists and
     * has contents, the contents will be the initial contents of the LongArray,
     * and updates will update the file. If the file does not exist, it will be
     * created.
     *
     * @param file A file
     * @return An array
     */
    public static CloseableLongArray mappedFileLongArray(Path file) {
        return new MappedFileLongArray(file, 0, true);
    }

    /**
     * Create a new LongArray backed by the passed file. If the file exists and
     * has contents, the contents will be the initial contents of the LongArray,
     * and updates will update the file. If the file does not exist, it will be
     * created and initialized with the passed size; if it does exist and is not
     * the specified size, it will be truncated or enlarged as needed.
     *
     * @param file A file
     * @return An array
     */
    public static CloseableLongArray mappedFileLongArray(Path file, long size) {
        return new MappedFileLongArray(file, size, true);
    }

    public static void saveForMapping(PrimitiveIterator.OfLong iter, Path to) throws IOException {
        MappedFileLongArray.saveForMapping(iter, to);
    }

    public static void saveForMapping(long[] longs, Path to) throws IOException {
        MappedFileLongArray.saveForMapping(longs, to);
    }

    /**
     * Create a LongArrayBitSet using the contents of this array.
     *
     * @return A Long-indexed BitSet
     */
    default LongArrayBitSet toBitSet() {
        return new LongArrayBitSet(this);
    }

    /**
     * Returns a function which can create new arrays with the same backing
     * storage mechanism as this one.
     *
     * @return A function which can be passed a target size for a new array
     */
    default LongFunction<LongArray> factory() {
        if (this instanceof MappedFileLongArray) {
            return MappedFileLongArray::new;
        } else if (this instanceof UnsafeLongArray) {
            return UnsafeLongArray::new;
        } else if (this instanceof JavaLongArray) {
            return JavaLongArray::new;
        } else {
            throw new UnsupportedOperationException(getClass().getName()
                    + " does not implement factory()");
        }
    }

    default int getInto(long from, long[] into) {
        long sz = size();
        if (from >= sz) {
            return 0;
        }
        long end = Math.min(from + into.length, sz);
        int dist = (int) (end - from);
        for (int i = 0; i < dist; i++) {
            into[i] = get(from + i);
        }
        return dist;
    }

    /**
     * The size of this array, which may be greater than Integer.MAX_VALUE for
     * some implementations.
     *
     * @return A size
     */
    long size();

    /**
     * Get the value at a specified offset.
     *
     * @param index The offset
     * @return A value
     */
    long get(long index);

    /**
     * Set the value at a specified index.
     *
     * @param index An offset, non-negative and less than size()
     * @param value A new value
     */
    void set(long index, long value);

    /**
     * Clone this array - supported by most implementations.
     *
     * @return A new LongArray
     * @throws CloneNotSupportedException If the implementation does not support
     * cloning
     */
    Object clone() throws CloneNotSupportedException;

    /**
     * Fill the array with zeros. Implementations should check
     * <code>isZeroInitialized</code> and if a zero-initialized array is needed,
     * call this method after creation.
     */
    default void clear() {
        fill(0, size(), 0);
    }

    /**
     * Add an array of longs, resizing as needed.
     *
     * @param longs
     */
    default void addAll(long[] longs) {
        resize(size() + longs.length);
        for (int i = 0; i < longs.length; i++) {
            set(i, longs[i]);
        }
    }

    /**
     * Append another LongArray to this one.
     *
     * @param longs A LongArray
     */
    default void addAll(LongArray longs) {
        long oldSize = size();
        resize(oldSize + longs.size());
        for (int i = 0; i < longs.size(); i++) {
            set(i + oldSize, longs.get(i));
        }
    }

    /**
     * Determine if newly created array instances are zero-initialized.
     * Unsafe-based ones are not.
     *
     * @return True if a newly created array with a non-zero size will appear to
     * be filled with zeros.
     */
    default boolean isZeroInitialized() {
        return true;
    }

    /**
     * Get the maximum size this set supports. This may be smaller than is
     * intuitive - for example, while sun.misc.Unsafe allows for theoretical
     * memory allocations of Long.MAX_VALUE, in practice that size allocation
     * will never work.
     *
     * @return The maximum theoretical index this long array can support
     */
    default long maxSize() {
        return Long.MAX_VALUE;
    }

    /**
     * Copy this LongArray to a new one of the same type.
     *
     * @return A copy
     */
    default LongArray copy() {
        try {
            return (LongArray) clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Logically <code>OR</code> another LongArray with this one.
     *
     * @param other Another LongArray
     */
    default void or(LongArray other) {
        long sz = Math.min(size(), other.size());
        for (long i = 0; i < sz; i++) {
            set(i, get(i) | other.get(i));
        }
    }

    /**
     * Logically <code>OR</code> another LongArray with this one.
     *
     * @param other Another LongArray
     */
    default void and(LongArray other) {
        long sz = Math.min(size(), other.size());
        for (long i = 0; i < sz; i++) {
            set(i, get(i) & other.get(i));
        }
    }

    /**
     * Logically <code>AND NOT</code> another LongArray with this one.
     *
     * @param other Another LongArray
     */
    default void andNot(LongArray other) {
        long sz = Math.min(size(), other.size());
        for (long i = 0; i < sz; i++) {
            set(i, get(i) ^ other.get(i));
        }
    }

    /**
     * File the array
     *
     * @param start
     * @param length
     * @param value
     */
    default void fill(long start, long length, long value) {
        if (start + length > size()) {
            throw new IndexOutOfBoundsException("start + length > size - "
                    + (start + length) + " > " + size());
        }
        for (long i = 0; i < length; i++) {
            set(i + start, value);
            assert get(i + start) == value : "value for " + (i + start) + " incorrect";
        }
    }

    /**
     * Update one index in the array.
     *
     * @param index The index
     * @param op
     */
    default void update(long index, LongUnaryOperator op) {
        if (index > size()) {
            throw new IndexOutOfBoundsException(index + " > " + size());
        }
        long val = get(index);
        long newValue = op.applyAsLong(val);
        if (newValue != val) {
            set(index, newValue);
        }
    }

    /**
     * Copy data from the passed LongARray into this one, resizing if needed.
     *
     * @param dest The destination index in this array
     * @param from The source array
     * @param start The start point to copy from in the original array
     * @param length The number of longs to copy
     * @param grow If true, grow the tail of this array to accomodate the
     * requested number of longs if needed
     */
    default void copy(long dest, LongArray from, long start, long length, boolean grow) {
        if (start + length > from.size()) {
            throw new IndexOutOfBoundsException("Copy past end of source array");
        }
        if (dest + length > size()) {
            if (!grow) {
                length = size() - start;
            } else {
                resize(dest + length);
            }
        }
        for (long i = 0; i < length; i++) {
            set(dest + i, from.get(start + i));
        }
    }

    /**
     * Copy this LongArray's contents into a java primitive long array.
     *
     * @return A long array
     */
    default long[] toLongArray() {
        long size = size();
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        long[] result = new long[(int) size];
        for (int i = 0; i < size; i++) {
            result[i] = get(i);
        }
        return result;
    }

    /**
     * Copy a subset of this array into a java primitive long array.
     *
     * @param len The number of longs to copy
     * @return an array of the passed length
     */
    default long[] toLongArray(long len) {
        if (len > Integer.MAX_VALUE) {
            throw new IllegalStateException();
        }
        long size = size();
        long[] result = new long[(int) len];
        long max = Math.min(len, size);
        for (int i = 0; i < max; i++) {
            result[i] = get(i);
        }
        return result;
    }

    /**
     * Resize this LongArray, either truncating or enlarging it. If the instance
     * returns false from <code>isZeroInitialized()</code> the contents from the
     * previous endpoint to the new one are undefined.
     *
     * @param size The new size
     */
    void resize(long size);

    /**
     * Resize, specifying to fill any newly created space with zeros
     * (sun.misc.Unsafe based arrays do not do this automatically).
     *
     * @param size The new size
     * @param zeroFillNewSpace If true, fill any created space with zeros
     */
    default void resize(long size, boolean zeroFillNewSpace) {
        if (!zeroFillNewSpace || size < size()) {
            resize(size);
        } else {
            long oldSize = size();
            resize(size);
            fill(oldSize, size - oldSize, 0);
        }
    }

    /**
     * Visit all longs in the array in sequential order, returning the index of
     * the first one rejected by the passed predicate, or -1L if all accepted.
     *
     * @param pred A LongPredicate
     * @return The index or -1L
     */
    default long firstMatchAscending(LongPredicate pred) {
        long sz = size();
        if (sz == 0) {
            return -1;
        }
        for (long i = 0; i < sz; i++) {
            if (pred.test(get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Visit all longs in the array in sequential order, returning the index of
     * the first one rejected by the passed predicate, or -1L if all accepted.
     *
     * @param pred A LongPredicate
     * @return The index or -1L
     */
    default long firstMatchDescending(LongPredicate pred) {
        long sz = size();
        if (sz == 0) {
            return -1;
        }
        for (long i = sz - 1; i >= 0; i--) {
            if (pred.test(get(i))) {
                return i;
            }
        }
        return -1;
    }

    default void forEach(LongConsumer lng) {
        long sz = size();
        for (long i = 0; i < sz; i++) {
            lng.accept(get(i));
        }
    }

    default void forEachReversed(LongConsumer lng) {
        long sz = size();
        for (long i = sz - 1; i >= 0; i--) {
            lng.accept(get(i));
        }
    }

    default PrimitiveIterator.OfLong iterator() {
        return new PrimitiveIterator.OfLong() {
            private long ix = -1;

            @Override
            public long nextLong() {
                return get(++ix);
            }

            @Override
            public boolean hasNext() {
                return ix + 1 < size();
            }
        };
    }

    default long firstZero() {
        return firstMatchAscending(val -> {
            return val == 0;
        });
    }

    default long firstNonZero() {
        return firstMatchAscending(val -> {
            return val != 0;
        });
    }

    default long lastZero() {
        return firstMatchDescending(val -> {
            boolean result = val == 0;
            return result;
        });
    }

    default long lastNonZero() {
        return firstMatchDescending(val -> {
            return val != 0;
        });
    }

    /**
     * For off-heap arrays, ensures that the compiler cannot
     * reorder writes this consumer performs to take effect
     * after this consumer has exited and the data is assumed
     * to be consistent.
     *
     * @param c A consumer
     */
    default void ensureWriteOrdering(Consumer<LongArray> c) {
        c.accept(this);
    }
}
