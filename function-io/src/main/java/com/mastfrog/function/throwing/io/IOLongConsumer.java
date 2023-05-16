package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingLongConsumer;
import java.io.IOException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOLongConsumer extends ThrowingLongConsumer {

    @Override
    void accept(long val) throws IOException;

    @Override
    default IOLongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default IOLongConsumer andThen(IOLongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

}
