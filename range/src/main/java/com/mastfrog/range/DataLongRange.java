package com.mastfrog.range;

/**
 * Interface that combines IntRange and DataRange.
 *
 * @author Tim Boudreau
 */
public interface DataLongRange<T, R extends DataLongRange<T, R>> extends DataRange<T, R>, LongRange<R> {

    @Override
    default DataLongRange<T, ? extends DataLongRange<T, ?>> snapshot() {
        return Range.of(start(), size(), get());
    }
}
