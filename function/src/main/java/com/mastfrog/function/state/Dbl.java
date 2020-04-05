/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

/**
 * Wrapper for primitive doubles that need to be updated inside a lambda; and
 * provides a transparent interface to atomic and non-atomic implementations.
 *
 * @author Tim Boudreau
 */
public interface Dbl extends DoubleConsumer, DoubleSupplier, Consumer<Double>, Supplier<Double> {

    public static Dbl create() {
        return new DblImpl();
    }

    public static Dbl of(double val) {
        return new DblImpl(val);
    }

    public static Dbl createAtomic() {
        return new DblAtomic();
    }

    public static Dbl ofAtomic(double val) {
        return new DblAtomic(val);
    }

    default double apply(DoubleUnaryOperator op) {
        return set(op.applyAsDouble(getAsDouble()));
    }

    default double apply(double val, DoubleBinaryOperator op) {
        return set(op.applyAsDouble(getAsDouble(), val));
    }

    default boolean ifUpdate(double newVal, Runnable r) {
        double oldVal = set(newVal);
        boolean result = oldVal != newVal;
        if (result) {
            r.run();
        }
        return result;
    }

    @Override
    default Double get() {
        return getAsDouble();
    }

    default double set(double val) {
        double old = getAsDouble();
        accept(val);
        return old;
    }

    @Override
    default void accept(Double t) {
        accept(t.doubleValue());
    }

    default double add(double val) {
        double old = getAsDouble();
        accept(val + old);
        return old;
    }

    default double subtract(double val) {
        return add(-val);
    }

    default double max(double val) {
        double old = getAsDouble();
        if (val > getAsDouble()) {
            accept(val);
        }
        return old;
    }

    default double min(double val) {
        double old = getAsDouble();
        if (val < getAsDouble()) {
            accept(val);
        }
        return old;
    }

    default double reset() {
        return set(0);
    }

    default DoubleConsumer summer() {
        return this::add;
    }

    default double floor() {
        return Math.floor(getAsDouble());
    }

    default double ceil() {
        return Math.ceil(getAsDouble());
    }

    default double round() {
        return Math.round(getAsDouble());
    }
}
