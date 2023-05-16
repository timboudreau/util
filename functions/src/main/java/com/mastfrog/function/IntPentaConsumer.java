package com.mastfrog.function;

import java.util.function.IntSupplier;

/**
 * A PetaConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntPentaConsumer {

    void accept(int a, int b, int c, int d, int e);

    default Runnable toRunnable(IntSupplier a, IntSupplier b, IntSupplier c, IntSupplier d, IntSupplier e) {
        return () -> {
            accept(a.getAsInt(), b.getAsInt(), c.getAsInt(), d.getAsInt(), e.getAsInt());
        };
    }

    default IntPentaConsumer andThen(IntPentaConsumer next) {
        return (a, b, c, d, e) -> {
            IntPentaConsumer.this.accept(a, b, c, d, e);
            next.accept(a, b, c, d, e);
        };
    }
}
