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

    @Override
    boolean getAsBoolean() throws IOException;

    @Override
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

    @Override
    default IOBooleanSupplier and(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && other.getAsBoolean();
        };
    }

    @Override
    default IOBooleanSupplier andNot(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() && !other.getAsBoolean();
        };
    }

    @Override
    default IOBooleanSupplier or(BooleanSupplier other) {
        return () -> {
            return IOBooleanSupplier.this.getAsBoolean() || other.getAsBoolean();
        };
    }

    @Override
    default void ifTrue(ThrowingRunnable run) throws Exception {
        if (getAsBoolean()) {
            run.run();
        }
    }

    @Override
    default void ifFalse(ThrowingRunnable run) throws Exception {
        if (!getAsBoolean()) {
            run.run();
        }
    }
}
