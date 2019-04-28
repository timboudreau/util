package com.mastfrog.function;

/**
 * A BiConsumer of primitive ints.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IntBiConsumer {

    void accept(int a, int b);

    default IntBiConsumer andThen(IntBiConsumer next) {
        return (a, b) -> {
            IntBiConsumer.this.accept(a, b);
            next.accept(a, b);
        };
    }
}
