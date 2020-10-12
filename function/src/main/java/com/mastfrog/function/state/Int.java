package com.mastfrog.function.state;

import com.mastfrog.function.FloatSupplier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;
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

    default int decrement() {
        return decrement(1);
    }

    default int decrement(int by) {
        return increment(-by);
    }

    /**
     * Decrement the value throwing an exception on integer overflow.
     *
     * @param by The amount to decrement by
     * @return The previous value
     */
    default int decrementSafe(int by) {
        return set(Math.subtractExact(getAsInt(), by));
    }

    /**
     * Decrement the value by 1 throwing an exception on integer overflow.
     *
     * @return The previous value
     */
    default int decrementSafe() {
        return decrementSafe(1);
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

    default int incrementSafe() {
        return incrementSafe(1);
    }

    default int incrementSafe(int by) {
        return set(Math.addExact(getAsInt(), by));
    }

    default int increment() {
        return increment(1);
    }

    default int increment(int by) {
        return set(getAsInt() + by);
    }

    default Int reset() {
        set(0);
        return this;
    }

    int set(int val);

    default int min(int min) {
        int val = getAsInt();
        if (min < val) {
            set(min);
        }
        return val;
    }

    default int max(int max) {
        int val = getAsInt();
        if (max > val) {
            set(max);
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

    default LongSupplier plus(IntSupplier supp) {
        return () -> (long) getAsInt() + (long) supp.getAsInt();
    }

    default IntSupplier plusSafe(IntSupplier supp) {
        return () -> {
            return Math.addExact(getAsInt(), supp.getAsInt());
        };
    }

    default IntSupplier minusSafe(IntSupplier supp) {
        return () -> {
            return Math.subtractExact(getAsInt(), supp.getAsInt());
        };
    }

    default IntSupplier timesSafe(IntSupplier supp) {
        return () -> {
            return Math.multiplyExact(getAsInt(), supp.getAsInt());
        };
    }

    default LongSupplier minus(IntSupplier supp) {
        return plus(() -> -supp.getAsInt());
    }

    default LongSupplier times(IntSupplier supp) {
        return () -> (long) getAsInt() * (long) supp.getAsInt();
    }

    default IntSupplier dividedBy(IntSupplier supp) {
        return () -> {
            int suppVal = supp.getAsInt();
            return suppVal == 0 ? 0 : getAsInt() / suppVal;
        };
    }

    default FloatSupplier asFloatSupplier() {
        return () -> (float) getAsInt();
    }

    default DoubleSupplier asDoubleSupplier() {
        return () -> (double) getAsInt();
    }

    default LongSupplier asLongSupplier() {
        return () -> (long) getAsInt();
    }

    /**
     * Get the value as an unsigned long.
     *
     * @return The value of this int as an unsigned integer
     */
    default long unsignedValue() {
        return ((long) getAsInt()) & 0x00000000ffffffffL;
    }

    default IntSupplier combinedWith(IntSupplier otherValue, IntBinaryOperator formula) {
        return () -> formula.applyAsInt(getAsInt(), otherValue.getAsInt());
    }
}
