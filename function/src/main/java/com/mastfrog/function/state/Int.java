package com.mastfrog.function.state;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * Wrapper for primitive ints that need to be updated from within a lambda; and
 * provides a transparent interface to atomic and non-atomic implementations.
 *
 * @author Tim Boudreau
 */
public interface Int extends IntConsumer, IntSupplier, Supplier<Integer>, Comparable<Int>, Consumer<Integer> {

    public static Int of(int initial) {
        return new IntImpl(initial);
    }

    public static Int create() {
        return new IntImpl();
    }

    public static Int of(AtomicInteger at) {
        return new IntAtomic(at);
    }

    public static Int ofAtomic(int initial) {
        return new IntAtomic(initial);
    }

    public static Int createAtomic() {
        return new IntAtomic();
    }

    public static IntWithChildren createWithChildren() {
        return new IntWithChildrenImpl();
    }

    public static IntWithChildren ofWithChildren(int initial) {
        return new IntWithChildrenImpl(initial);
    }

    default int apply(IntUnaryOperator op) {
        return set(op.applyAsInt(getAsInt()));
    }

    default int apply(int val, IntBinaryOperator op) {
        return set(op.applyAsInt(getAsInt(), val));
    }

    default int getLess(int amt) {
        return getAsInt() - amt;
    }

    default boolean ifUpdate(int newVal, Runnable r) {
        int oldVal = set(newVal);
        boolean result = oldVal != newVal;
        if (result) {
            r.run();
        }
        return result;
    }

    default int decrement(int val) {
        return increment(-val);
    }

    @Override
    default Integer get() {
        return getAsInt();
    }

    @Override
    default void accept(int value) {
        set(value);
    }

    @Override
    default void accept(Integer value) {
        set(value);
    }

    default boolean ifNotEqual(int value, Runnable r) {
        if (getAsInt() != value) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifEqual(int value, Runnable r) {
        if (getAsInt() == value) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifGreater(int than, Runnable r) {
        if (getAsInt() > than) {
            r.run();
            return true;
        }
        return false;
    }

    default boolean ifLess(int than, Runnable r) {
        if (getAsInt() < than) {
            r.run();
            return true;
        }
        return false;
    }

    default int increment() {
        return increment(1);
    }

    default int increment(int val) {
        return set(getAsInt() + val);
    }

    default Int reset() {
        set(0);
        return this;
    }

    int set(int val);

    default int min(int min) {
        int val = getAsInt();
        if (min < val) {
            set(val);
        }
        return val;
    }

    default int max(int max) {
        int val = getAsInt();
        if (max > val) {
            set(val);
        }
        return val;
    }

    default IntConsumer summer() {
        return this::increment;
    }

    @Override
    default int compareTo(Int o) {
        return Integer.compare(getAsInt(), o.getAsInt());
    }

    default BooleanSupplier toBooleanSupplier(IntPredicate pred) {
        return () -> {
            return pred.test(getAsInt());
        };
    }

    default boolean equals(int val) {
        return val == getAsInt();
    }

    default int setFrom(IntSupplier supp) {
        return set(supp.getAsInt());
    }
}
