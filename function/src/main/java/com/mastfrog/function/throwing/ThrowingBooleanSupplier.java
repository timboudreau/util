package com.mastfrog.function.throwing;

import com.mastfrog.util.preconditions.Exceptions;
import java.util.function.BooleanSupplier;

/**
 *
 * A BooleanSupplier which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingBooleanSupplier {

    boolean getAsBoolean() throws Exception;

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default BooleanSupplier toNonThrowing() {
        return () -> {
            try {
                return getAsBoolean();
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingBooleanSupplier invert() {
        return () -> {
            return !ThrowingBooleanSupplier.this.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier and(ThrowingBooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() && other.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier andNot(ThrowingBooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() && !other.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier or(ThrowingBooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() || other.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier and(BooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() && other.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier andNot(BooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() && !other.getAsBoolean();
        };
    }

    default ThrowingBooleanSupplier or(BooleanSupplier other) {
        return () -> {
            return ThrowingBooleanSupplier.this.getAsBoolean() || other.getAsBoolean();
        };
    }

    default void ifTrue(ThrowingRunnable run) throws Exception {
        if (getAsBoolean()) {
            run.run();
        }
    }

    default void ifFalse(ThrowingRunnable run) throws Exception {
        if (!getAsBoolean()) {
            run.run();
        }
    }
}
