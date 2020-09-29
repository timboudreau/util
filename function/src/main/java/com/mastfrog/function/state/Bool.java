/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

import com.mastfrog.function.BooleanConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Holder for a primitive boolean that needs to be updated within a lambda
 * or set of recursive calls to hold transitive state.
 *
 * @author Tim Boudreau
 */
public interface Bool extends BooleanConsumer, BooleanSupplier, Supplier<Boolean>, Consumer<Boolean> {

    public static Bool create() {
        return create(false);
    }

    public static Bool create(boolean val) {
        return new BoolImpl(val);
    }

    public static Bool createAtomic() {
        return new BoolAtomic();
    }

    public static Bool createAtomic(boolean val) {
        return new BoolAtomic(val);
    }

    default boolean or(boolean val) {
        boolean old = getAsBoolean();
        set(old || val);
        return old != val;
    }

    default boolean and(boolean val) {
        boolean old = getAsBoolean();
        boolean newVal = old && val;
        if (newVal != old) {
            set(newVal);
            return true;
        }
        return false;
    }

    default boolean xor(boolean val) {
        boolean old = getAsBoolean();
        boolean nue = old ^ val;
        if (nue != old) {
            set(nue);
            return true;
        }
        return false;
    }

    default BooleanSupplier or(BooleanSupplier other) {
        return () -> getAsBoolean() || other.getAsBoolean();
    }

    default BooleanSupplier and(BooleanSupplier other) {
        return () -> getAsBoolean() && other.getAsBoolean();
    }

    default BooleanSupplier xor(BooleanSupplier other) {
        return () -> getAsBoolean() ^ other.getAsBoolean();
    }

    default boolean set() {
        return set(true);
    }

    default boolean reset() {
        return set(false);
    }

    default boolean runAndSet(Runnable r) {
        if (!getAsBoolean()) {
            r.run();
            set();
            return true;
        }
        return false;
    }

    default boolean ifTrue(Runnable r) {
        if (getAsBoolean()) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifUntrue(Runnable r) {
        if (!getAsBoolean()) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean set(boolean val) {
        boolean old = get();
        if (old != val) {
            accept(val);
        }
        return old;
    }

    default boolean toggle() {
        return set(!getAsBoolean());
    }

    @Override
    default Boolean get() {
        return getAsBoolean();
    }

    @Override
    default void accept(Boolean t) {
        accept(t.booleanValue());
    }
}
