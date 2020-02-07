package com.mastfrog.function;

import java.util.function.IntSupplier;

/**
 * A QuadConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntQuadConsumer {

    void accept(int a, int b, int c, int d);

    default Runnable toRunnable(IntSupplier a, IntSupplier b, IntSupplier c, IntSupplier d) {
        return () -> {
            accept(a.getAsInt(), b.getAsInt(), c.getAsInt(), d.getAsInt());
        };
    }

    default IntQuadConsumer andThen(IntQuadConsumer next) {
        return (a, b, c, d) -> {
            IntQuadConsumer.this.accept(a, b, c, d);
            next.accept(a, b, c, d);
        };
    }
}
