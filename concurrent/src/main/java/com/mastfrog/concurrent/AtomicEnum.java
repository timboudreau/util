/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.UnaryOperator;

/**
 * An atomic enum, using an AtomicInteger under the hood.
 *
 * @author Tim Boudreau
 */
public final class AtomicEnum<T extends Enum<T>> {

    private static final AtomicIntegerFieldUpdater<AtomicEnum> UPD
            = AtomicIntegerFieldUpdater.newUpdater(AtomicEnum.class, "value");
    private final Class<T> type;
    private final T[] values;
    private volatile int value;

    public AtomicEnum(Class<T> type) {
        this.type = Objects.requireNonNull(type, "Null type");
        this.values = type.getEnumConstants();
        if (values.length == 0) {
            throw new IllegalArgumentException(type.getName() + " has no enum values");
        }
    }

    public AtomicEnum(T initialValue) {
        this.type = Objects.requireNonNull(initialValue, "Initial value may not be null")
                .getDeclaringClass();
        values = type.getEnumConstants();
        if (values.length == 0) {
            throw new IllegalArgumentException(type.getName() + " has no enum values");
        }
        value = initialValue == null ? 0 : initialValue.ordinal();
    }

    /**
     * Set the value.
     *
     * @param obj The new value, non-null
     */
    public void set(T obj) {
        UPD.set(this, Objects.requireNonNull(obj,
                "Enum value may not be null").ordinal());
    }

    /**
     * Set the value lazily, using the same contract as
     * <code>{@link AtomicIntegerFieldUpdater#lazySet(java.lang.Object, int)}</code>.
     *
     * @param value A new value, non-null
     */
    public void lazySet(T value) {
        UPD.lazySet(this, Objects.requireNonNull(value,
                "Value may not be null").ordinal());
    }

    /**
     * Set the value, returning the old value.
     *
     * @param obj The new value, non-null
     */
    public void getAndSet(T obj) {
        UPD.getAndSet(this, Objects.requireNonNull(obj,
                "Enum value may not be null").ordinal());
    }

    /**
     * Update the value using the passed unary operator, returning the old
     * value.
     *
     * @param op A unary operator
     * @return An enum value
     */
    public T getAndUpdate(UnaryOperator<T> op) {
        return values[UPD.getAndUpdate(this, old -> {
            return op.apply(values[old]).ordinal();
        })];
    }

    /**
     * Update the value using the passed unary operator, returning the new
     * value.
     *
     * @param op A unary operator
     * @return An enum value
     */
    public T updateAndGet(UnaryOperator<T> op) {
        return values[UPD.updateAndGet(this, old -> {
            return op.apply(values[old]).ordinal();
        })];
    }

    /**
     * Update the value if the current value matches the <code>expected</code>
     * argument.
     *
     * @param expected The expected value
     * @param value A new value
     * @return true if the value was updated
     */
    public boolean compareAndSet(T expected, T value) {
        return UPD.compareAndSet(this,
                Objects.requireNonNull(expected, "Expected value null").ordinal(),
                Objects.requireNonNull(value, "Value null").ordinal());
    }

    /**
     * Update the value if the current value matches the <code>expected</code>
     * argument, according to the contract of
     * <code>{@link AtomicIntegerFieldUpdater#weakCompareAndSet(java.lang.Object, int, int)}</code>.
     *
     * @param expected The expected value
     * @param value A new value
     * @return true if the value was updated
     */
    public boolean weakCompareAndSet(T expected, T value) {
        return UPD.weakCompareAndSet(this, expected.ordinal(), value.ordinal());
    }

    /**
     * Get the ordinal of the current value.
     *
     * @return An ordinal
     */
    public int ordinal() {
        return UPD.get(this);
    }

    /**
     * Get the current value.
     *
     * @return An enum constant
     */
    public T get() {
        return values[UPD.get(this)];
    }

    /**
     * Get the name of the current value.
     *
     * @return A name
     */
    public String name() {
        return get().name();
    }

    /**
     * Update the enum value, setting it to and returning the enum value with
     * the next greatest ordinal, or the 0th upon reaching the maximum.
     *
     * @return the new value
     */
    public T next() {
        return values[UPD.updateAndGet(this, old -> (old + 1) % values.length)];
    }

    /**
     * Update the enum value, setting it to and returning the enum value with
     * the next lower ordinal, or that with the greatest upon passing zero.
     *
     * @return the new value
     */
    public T prev() {
        return values[UPD.updateAndGet(this, old -> {
            if (old == 0) {
                return values.length - 1;
            }
            return old - 1;
        })];
    }

    @Override
    public String toString() {
        return get().toString();
    }

}
