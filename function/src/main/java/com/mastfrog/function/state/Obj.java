package com.mastfrog.function.state;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Just a holder for an object, for use in cases that a return value needs to be
 * altered inside a lambda.
 *
 * @author Tim Boudreau
 */
public interface Obj<T> extends Supplier<T>, Consumer<T> {

    T set(T obj);

    @Override
    default void accept(T t) {
        set(t);
    }

    public static <T> Obj<T> create() {
        return new ObjImpl<>();
    }

    public static <T> Obj<T> of(T object) {
        return new ObjImpl<>(object);
    }

    public static <T> Obj<T> createAtomic() {
        return new ObjAtomic<>();
    }

    public static <T> Obj<T> ofAtomic(AtomicReference<T> ref) {
        return new ObjAtomic<>(ref);
    }

    public static <T> Obj<T> ofAtomic(T object) {
        return new ObjAtomic<>(object);
    }

    default boolean is(T obj) {
        return Objects.equals(obj, get());
    }

    default boolean isSet() {
        return get() != null;
    }

    default boolean isNull() {
        return get() == null;
    }

    boolean ifUpdate(T newValue, Runnable ifChange);

    default boolean ifNullSet(Supplier<T> supp) {
        if (!isSet()) {
            T obj = supp.get();
            if (obj != null) {
                set(supp.get());
                return true;
            }
        }
        return false;
    }

    default T apply(UnaryOperator<T> op) {
        return set(op.apply(get()));
    }

    default T apply(T other, BinaryOperator<T> op) {
        return set(op.apply(get(), other));
    }
}
