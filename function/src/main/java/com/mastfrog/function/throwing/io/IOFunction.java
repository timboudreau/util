package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingFunction;
import com.mastfrog.util.preconditions.Checks;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * IOException specialization of ThrowingFunction.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOFunction<In, Out> extends ThrowingFunction<In, Out> {

    @Override
    Out apply(In a) throws IOException;

    @Override
    default <V> IOFunction<In, V> andThen(Function<? super Out, ? extends V> after) {
        Objects.requireNonNull(after);
        return (In t) -> after.apply(apply(t));
    }

    @Override
    default <V> ThrowingFunction<V, Out> compose(Function<? super V, ? extends In> before) {
        Checks.notNull("before", before);
        return (V v) -> apply(before.apply(v));
    }

    default <NextOut> ThrowingFunction<In, NextOut> andThen(IOFunction<Out, NextOut> f) {
        return in -> {
            return f.apply(IOFunction.this.apply(in));
        };
    }
}
