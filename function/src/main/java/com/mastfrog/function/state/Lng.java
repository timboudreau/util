package com.mastfrog.function.state;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
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

    default long decrement(long val) {
        return increment(-val);
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

    default Lng reset() {
        set(0);
        return this;
    }

    long set(long val);

    default long min(long min) {
        long val = getAsLong();
        if (min < val) {
            set(val);
        }
        return val;
    }

    default long max(long max) {
        long val = getAsLong();
        if (max > val) {
            set(val);
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
}
