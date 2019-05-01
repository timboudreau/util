package com.mastfrog.function.throwing.io;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOLongConsumer {

    void consume(long val) throws Exception;

    default IOLongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            consume(t);
            after.accept(t);
        };
    }

    default IOLongConsumer andThen(IOLongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            consume(t);
            after.consume(t);
        };
    }

}
