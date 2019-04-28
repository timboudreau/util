package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingConsumer;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * IOException specialization of ThrowingConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOConsumer<T> extends ThrowingConsumer<T> {

    void accept(T arg) throws IOException;

    default <R> IOConsumer<R> adapt(Function<R, T> conversion) {
        return (R r) -> {
            IOConsumer.this.accept(conversion.apply(r));
        };
    }

    default IOConsumer<T> andThen(IOConsumer<T> other) {
        return t -> {
            this.accept(t);
            other.accept(t);
        };
    }

    default ThrowingConsumer<T> andThen(Consumer<T> other) {
        return t -> {
            this.accept(t);
            other.accept(t);
        };
    }
}
