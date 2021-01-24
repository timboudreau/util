package com.mastfrog.function.throwing;

import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.Objects;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntBiConsumer {

    void accept(int a, int b) throws Exception;

    default ThrowingShortBiConsumer toShortBiConsumer() {
        return (a, b) -> accept(a, b);
    }

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default IntBiConsumer toNonThrowing() {
        return (a, b) -> {
            try {
                accept(a, b);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingIntBiConsumer andThen(IntBiConsumer after) {
        Objects.requireNonNull(after);
        return (int a, int b) -> {
            accept(a, b);
            after.accept(a, b);
        };
    }

    default ThrowingIntBiConsumer andThen(ThrowingIntBiConsumer after) {
        Objects.requireNonNull(after);
        return (int a, int b) -> {
            accept(a, b);
            after.accept(a, b);
        };
    }

}
