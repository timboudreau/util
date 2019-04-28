package com.mastfrog.function.throwing;

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
