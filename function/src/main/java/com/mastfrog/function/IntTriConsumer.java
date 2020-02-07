package com.mastfrog.function;

import java.util.function.IntSupplier;

/**
 * A TriConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntTriConsumer {

    void accept(int a, int b, int c);

    default Runnable toRunnable(IntSupplier a, IntSupplier b, IntSupplier c) {
        return () -> {
            accept(a.getAsInt(), b.getAsInt(), c.getAsInt());
        };
    }

    default IntTriConsumer andThen(IntTriConsumer next) {
        return (a, b, c) -> {
            IntTriConsumer.this.accept(a, b, c);
            next.accept(a, b, c);
        };
    }
}
