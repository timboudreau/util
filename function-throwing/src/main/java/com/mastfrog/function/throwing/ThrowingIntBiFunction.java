package com.mastfrog.function.throwing;

import java.util.function.Function;

/**
 * A BiConsumer of primitive ints that throws.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntBiFunction<T> {

    T apply(int a, int b) throws Exception;

    default <R> ThrowingIntBiFunction<R> andThen(Function<T, R> f) {
        return (a, b) -> {
            return f.apply(apply(a, b));
        };
    }

    default ThrowingShortBiFunction<T> toShortBiFunction() {
        return (a, b) -> apply(a, b);
    }
}
