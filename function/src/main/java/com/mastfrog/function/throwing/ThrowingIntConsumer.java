package com.mastfrog.function.throwing;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntConsumer {

    void consume(int val) throws Exception;

    default ThrowingIntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            consume(t);
            after.accept(t);
        };
    }

    default ThrowingIntConsumer andThen(ThrowingIntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            consume(t);
            after.consume(t);
        };
    }

}
