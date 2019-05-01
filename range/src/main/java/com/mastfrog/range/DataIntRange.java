package com.mastfrog.range;

/**
 * Interface that combines IntRange and DataRange.
 *
 * @author Tim Boudreau
 */
public interface DataIntRange<T, R extends DataIntRange<T, R>> extends DataRange<T,R>, IntRange<R> {

    @Override
    default DataIntRange<T,? extends DataIntRange<T,?>> snapshot() {
        return Range.of(start(), size(), get());
    }


}
