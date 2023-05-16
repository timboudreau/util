package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingTriFunction;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * IOException specialization of ThrowingTriFunction.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOTriFunction<In1, In2, In3, Out> extends ThrowingTriFunction<In1, In2, In3, Out> {

    @Override
    Out apply(In1 a, In2 b, In3 c) throws IOException;

    default <NextOut> IOTriFunction<In1, In2, In3, NextOut> andThen(Function<? super Out, ? extends NextOut> after) {
        Objects.requireNonNull(after);
        return (In1 t, In2 u, In3 v) -> after.apply(apply(t, u, v));
    }
}
