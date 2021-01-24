package com.mastfrog.function.throwing;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Throwing version of IntConsumer.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingLongConsumer {

    void accept(long val) throws Exception;

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default LongConsumer toNonThrowing() {
        return val -> {
            try {
                accept(val);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingLongConsumer andThen(LongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default ThrowingLongConsumer andThen(ThrowingLongConsumer after) {
        Objects.requireNonNull(after);
        return (long t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default ThrowingIntConsumer toIntConsumer() {
        return val -> accept(val);
    }

}
