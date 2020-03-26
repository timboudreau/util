package com.mastfrog.function;

import java.util.function.IntSupplier;

/**
 * A PetaConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntPetaConsumer {

    void accept(int a, int b, int c, int d, int e);

    default Runnable toRunnable(IntSupplier a, IntSupplier b, IntSupplier c, IntSupplier d, IntSupplier e) {
        return () -> {
            accept(a.getAsInt(), b.getAsInt(), c.getAsInt(), d.getAsInt(), e.getAsInt());
        };
    }

    default IntPetaConsumer andThen(IntPetaConsumer next) {
        return (a, b, c, d, e) -> {
            IntPetaConsumer.this.accept(a, b, c, d, e);
            next.accept(a, b, c, d, e);
        };
    }
}
