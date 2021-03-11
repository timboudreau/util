/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
        if (val > old) {
            accept(val);
        }
        return old;
    }

    default float min(float val) {
        float old = getAsFloat();
        if (val < old) {
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

    default FloatSupplier combinedWith(FloatSupplier otherValue, FloatBinaryOperator formula) {
        return () -> formula.applyAsFloat(getAsFloat(), otherValue.getAsFloat());
    }
}
