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

import com.mastfrog.function.FloatSupplier;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * Wrapper for primitive ints that need to be updated from within a lambda; and
 * provides a transparent interface to atomic and non-atomic implementations.
 *
 * @author Tim Boudreau
 */
public interface Lng extends LongConsumer, LongSupplier, Supplier<Long>, Comparable<Lng>, Consumer<Long> {

    public static Lng of(long initial) {
        return new LngImpl(initial);
    }

    public static Lng create() {
        return new LngImpl();
    }

    public static Lng ofAtomic(long initial) {
        return new LngAtomic(initial);
    }

    public static Lng createAtomic() {
        return new LngAtomic();
    }

    default long apply(LongUnaryOperator op) {
        return set(op.applyAsLong(getAsLong()));
    }

    default long apply(long val, LongBinaryOperator op) {
        return set(op.applyAsLong(getAsLong(), val));
    }

    default long getLess(long amt) {
        return getAsLong() - amt;
    }

    default boolean ifUpdate(long newVal, Runnable r) {
        long oldVal = set(newVal);
        boolean result = oldVal != newVal;
        if (result) {
            r.run();
        }
        return result;
    }

    default long decrement() {
        return decrement(1);
    }

    default long decrement(long val) {
        return increment(-val);
    }

    default long decrementSafe(long by) {
        long val = getAsLong();
        return set(Math.subtractExact(val, by));
    }

    @Override
    default Long get() {
        return getAsLong();
    }

    @Override
    default void accept(long value) {
        set(value);
    }

    @Override
    default void accept(Long value) {
        set(value);
    }

    default boolean ifNotEqual(long value, Runnable r) {
        if (getAsLong() != value) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifEqual(long value, Runnable r) {
        if (getAsLong() == value) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifGreater(long than, Runnable r) {
        if (getAsLong() > than) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifLess(long than, Runnable r) {
        if (getAsLong() < than) {
            r.run();
            return true;
        }
        return false;
    }

    default long increment() {
        return increment(1);
    }

    default long increment(long val) {
        return set(getAsLong() + val);
    }

    default long incrementSafe(long by) {
        return set(Math.addExact(getAsLong(), by));
    }

    default Lng reset() {
        set(0);
        return this;
    }

    long set(long val);

    default long min(long min) {
        long val = getAsLong();
        if (min < val) {
            set(min);
        }
        return val;
    }

    default long max(long max) {
        long val = getAsLong();
        if (max > val) {
            set(max);
        }
        return val;
    }

    default LongConsumer summer() {
        return this::increment;
    }

    @Override
    default int compareTo(Lng o) {
        return Long.compare(getAsLong(), o.getAsLong());
    }

    default BooleanSupplier toBooleanSupplier(LongPredicate pred) {
        return () -> {
            return pred.test(getAsLong());
        };
    }

    default boolean equals(long val) {
        return val == getAsLong();
    }

    default LongSupplier plus(LongSupplier supp) {
        return () -> getAsLong() + supp.getAsLong();
    }

    default LongSupplier minus(LongSupplier supp) {
        return () -> getAsLong() - supp.getAsLong();
    }

    default LongSupplier times(LongSupplier supp) {
        return () -> getAsLong() * supp.getAsLong();
    }

    default LongSupplier dividedBy(LongSupplier supp) {
        return () -> {
            long suppVal = supp.getAsLong();
            return suppVal == 0 ? 0 : getAsLong() / supp.getAsLong();
        };
    }

    default FloatSupplier asFloatSupplier() {
        return () -> (float) getAsLong();
    }

    default DoubleSupplier asDoubleSupplier() {
        return () -> (double) getAsLong();
    }

    default LongSupplier asLongSupplier() {
        return () -> (long) getAsLong();
    }

    default LongSupplier combinedWith(LongSupplier otherValue, LongBinaryOperator formula) {
        return () -> formula.applyAsLong(getAsLong(), otherValue.getAsLong());
    }

}
