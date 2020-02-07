package com.mastfrog.function;

import java.util.function.Function;

/**
 * A BiConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntBiFunction<T> {

    T apply(int a, int b);

    default <R> IntBiFunction<R> andThen(Function<T, R> f) {
        return (a, b) -> {
            return f.apply(apply(a, b));
        };
    }
}
