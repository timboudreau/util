package com.mastfrog.function.throwing;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingLongConsumer {

    void accept(long val) throws Exception;

    default ThrowingLongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default ThrowingLongConsumer andThen(ThrowingLongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

}
