package com.mastfrog.function;

import java.util.function.IntSupplier;

/**
 * A BiConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntBiConsumer {

    void accept(int a, int b);

    default Runnable toRunnable(IntSupplier a, IntSupplier b) {
        return () -> {
            accept(a.getAsInt(), b.getAsInt());
        };
    }

    default IntBiConsumer andThen(IntBiConsumer next) {
        return (a, b) -> {
            IntBiConsumer.this.accept(a, b);
            next.accept(a, b);
        };
    }
}
