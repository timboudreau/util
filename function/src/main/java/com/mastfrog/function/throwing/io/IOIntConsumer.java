package com.mastfrog.function.throwing.io;

import java.io.IOException;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOIntConsumer {

    void consume(int val) throws IOException;

    default IOIntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            consume(t);
            after.accept(t);
        };
    }

    default IOIntConsumer andThen(IOIntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            consume(t);
            after.consume(t);
        };
    }

}
