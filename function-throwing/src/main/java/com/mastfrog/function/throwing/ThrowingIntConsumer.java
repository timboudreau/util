package com.mastfrog.function.throwing;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingIntConsumer {

    void accept(int val) throws Exception;

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default IntConsumer toNonThrowing() {
        return val -> {
            try {
                accept(val);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingShortConsumer toShortConsumer() {
        return val -> accept(val);
    }

    default ThrowingIntConsumer andThen(IntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default ThrowingIntConsumer andThen(ThrowingIntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            accept(t);
            after.accept(t);
        };
    }

}
