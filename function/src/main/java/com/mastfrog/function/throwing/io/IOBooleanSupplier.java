package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.*;
import java.io.IOException;
import java.util.function.BooleanSupplier;

/**
 *
 * A BooleanSupplier which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOBooleanSupplier extends ThrowingBooleanSupplier {

    boolean getAsBoolean() throws IOException;

    default IOBooleanSupplier invert() {
        return () -> {
            return !IOBooleanSupplier.this.getAsBoolean();
        };
    }

    default IOBooleanSupplier and(IOBooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && other.getAsBoolean();
        };
    }

    default IOBooleanSupplier andNot(IOBooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && !other.getAsBoolean();
        };
    }

    default IOBooleanSupplier or(IOBooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() || other.getAsBoolean();
        };
    }

    default IOBooleanSupplier and(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && other.getAsBoolean();
        };
    }

    default IOBooleanSupplier andNot(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && !other.getAsBoolean();
        };
    }

    default IOBooleanSupplier or(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() || other.getAsBoolean();
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
