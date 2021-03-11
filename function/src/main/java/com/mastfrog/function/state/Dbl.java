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
        if (val > old) {
            accept(val);
        }
        return old;
    }

    default double min(double val) {
        double old = getAsDouble();
        if (val < old) {
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

    default DoubleSupplier combinedWith(DoubleSupplier otherValue, DoubleBinaryOperator formula) {
        return () -> formula.applyAsDouble(getAsDouble(), otherValue.getAsDouble());
    }
}
