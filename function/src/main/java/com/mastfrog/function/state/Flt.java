package com.mastfrog.function.state;

import com.mastfrog.function.FloatConsumer;
import com.mastfrog.function.FloatSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Wrapper for primitive floats that need to be updated inside a lambda; and
 * provides a transparent interface to atomic and non-atomic implementations.
 *
 * @author Tim Boudreau
 */
public interface Flt extends FloatConsumer, FloatSupplier, Consumer<Float>, Supplier<Float> {

    public static Flt create() {
        return new FltImpl();
    }

    public static Flt of(float val) {
        return new FltImpl(val);
    }

    public static Flt createAtomic() {
        return new FltAtomic();
    }

    public static Flt ofAtomic(float val) {
        return new FltAtomic(val);
    }

    default float apply(FloatUnaryOperator op) {
        return set(op.applyAsFloat(getAsFloat()));
    }

    default float apply(float val, FloatBinaryOperator op) {
        return set(op.applyAsFloat(getAsFloat(), val));
    }

    @Override
    default Float get() {
        return getAsFloat();
    }

    default float set(float val) {
        float old = getAsFloat();
        accept(val);
        return old;
    }

    @Override
    default void accept(Float t) {
        accept(t.floatValue());
    }

    default float add(float val) {
        float old = getAsFloat();
        accept(val + old);
        return old;
    }

    default float subtract(float val) {
        return add(-val);
    }

    default float max(float val) {
        float old = getAsFloat();
        if (val > getAsFloat()) {
            accept(val);
        }
        return old;
    }

    default float min(float val) {
        float old = getAsFloat();
        if (val < getAsFloat()) {
            accept(val);
        }
        return old;
    }

    default float reset() {
        return set(0);
    }

    default FloatConsumer summer() {
        return this::add;
    }

    default float floor() {
        return (float) Math.floor(getAsFloat());
    }

    default float ceil() {
        return (float) Math.ceil(getAsFloat());
    }

    default int round() {
        return Math.round(getAsFloat());
    }
}
