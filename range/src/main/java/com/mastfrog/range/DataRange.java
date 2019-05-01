package com.mastfrog.range;

import java.util.function.Supplier;

/**
 * Range subtype which carries an object payload of some type which can be
 * retrieved from its <code>get()</code> method.
 *
 * @author Tim Boudreau
 */
public interface DataRange<T, R extends DataRange<T, R>> extends Range<R>, Supplier<T> {

    /**
     * Create a new range of this type with a different payload and bounds.
     *
     * @param start The start position
     * @param size The size
     * @param newObject A new payload
     * @return A range
     */
    DataRange<T, R> newRange(int start, int size, T newObject);

    /**
     * Create a new range of this type with a different payload and bounds.
     *
     * @param start The start position
     * @param size The size
     * @param newObject A new payload
     * @return A range
     */
    DataRange<T, R> newRange(long start, long size, T newObject);
}
