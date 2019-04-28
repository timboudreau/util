package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingBiConsumer;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * IOException specialization of ThrowingBiConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOBiConsumer<T, R> extends ThrowingBiConsumer<T,R> {

    @Override
    void accept(T a, R b) throws IOException;

    @Override
    default IOBiConsumer<T, R> andThen(IOBiConsumer<T, R> iob) {
        return (a, b) -> {
            IOBiConsumer.this.accept(a, b);
            iob.accept(a, b);
        };
    }

    @Override
    default IOBiConsumer<T, R> andThen(BiConsumer<T, R> iob) {
        return (a, b) -> {
            IOBiConsumer.this.accept(a, b);
            iob.accept(a, b);
        };
    }
}
