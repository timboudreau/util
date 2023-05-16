package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.*;
import java.io.IOException;
import java.util.function.Function;

/**
 * A BiConsumer of primitive ints that throws IOException.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOIntBiFunction<T> extends ThrowingIntBiFunction<T> {

    @Override
    T apply(int a, int b) throws IOException;

    default <R> IOIntBiFunction<R> andThen(Function<T, R> f) {
        return (a, b) -> {
            return f.apply(apply(a, b));
        };
    }
}
