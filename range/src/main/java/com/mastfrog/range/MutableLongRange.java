package com.mastfrog.range;

/**
 * Integer range subtype which has mutator methods. Synchronized and
 * unsynchronized implementations are available from static methods on Range.
 *
 * @author Tim Boudreau
 */
public interface MutableLongRange<OI extends MutableLongRange<OI>> extends LongRange<OI>, MutableRange<OI> {

    /**
     * Set the start and size of this range, modifying its internal state.
     *
     * @param start The start
     * @param size The size
     * @return True if the range was modified (if the passed values do not equal
     * the current ones)
     */
    boolean setStartAndSize(long start, long size);

    /**
     * Set the end coordinate of this range.
     *
     * @param end
     * @return
     */
    default boolean setEnd(long end) {
        if (end == end()) {
            return false;
        }
        long sz = size();
        return setStartAndSize(end - sz, sz);
    }

    /**
     * Set the stop coordinate - the last coordinate within this range,
     * or <code>end() - 1</code>, resizing this range.
     *
     * @param stop The new last coordinate
     * @return True if the range was altered
     */
    default boolean setStop(long stop) {
        return setEnd(stop + 1L);
    }

    @Override
    default boolean setStartAndSizeValues(Number start, Number size) {
        return setStartAndSize(start.longValue(), size.longValue());
    }

    /**
     * Set the start of this range, modifying its internal state.
     *
     * @param start The new start
     * @return true if the range was altered
     */
    default boolean setStart(long start) {
        assert start >= 0;
        return setStartAndSize(start, size());
    }

    /**
     * Set the size of this range, modifying its internal state.
     *
     * @param size The new size
     * @return true if the range was modified
     */
    default boolean setSize(long size) {
        assert size >= 0;
        return setStartAndSize(start(), size);
    }

    /**
     * Shift the start point of this range by the specified amount
     *
     * @param amount
     * @return true if the amount was non-zero
     * @throws IllegalArgumentException if this would result in a negative start
     * point
     */
    default void shift(long amount) {
        setStartAndSize(start() + amount, size());
    }

    /**
     * Alter this range's size by adding the specified amount.
     *
     * @param by The amount to add to the current size
     * @return true if the amount was non-zero
     * @throws IllegalArgumentException if this call would result in a negative
     * size
     */
    default void grow(long by) {
        setStartAndSize(start(), size() + by);
    }

    /**
     * Alter this range's size by subtracting the specified amount.
     *
     * @param by The amount to subtract from the current size
     * @return true if the amount was non-zero
     * @throws IllegalArgumentException if this call would result in a negative
     * size
     */
    default void shrink(long by) {
        setStartAndSize(start(), size() - by);
    }

    /**
     * Resize this range <i>only if the passed start and size match the actual
     * ones</i>.
     *
     * @param start The expected current start point
     * @param oldSize The expected current size
     * @param newSize The new size to resize to if the other parameters match
     * @return true if the state of this range was modified
     */
    default boolean resizeIfExact(long start, long oldSize, long newSize) {
        if (start() == start && oldSize == size()) {
            return setSize(newSize);
        }
        return false;
    }

}
