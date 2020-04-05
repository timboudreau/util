package com.mastfrog.function.state;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
final class ObjAtomic<T> implements Obj<T> {

    private final AtomicReference<T> value;

    ObjAtomic(AtomicReference<T> value) {
        this.value = value;
    }

    ObjAtomic(T initial) {
        value = initial == null ? new AtomicReference<>()
                : new AtomicReference<>(initial);
    }

    ObjAtomic() {
        value = new AtomicReference<>();
    }

    @Override
    public T get() {
        return value.get();
    }

    @Override
    public T set(T obj) {
        return value.getAndSet(obj);
    }

    @Override
    public void accept(T t) {
        set(t);
    }

    @Override
    public boolean ifUpdate(T newValue, Runnable ifChange) {
        T old = value.getAndSet(newValue);
        if (!Objects.equals(old, newValue)) {
            ifChange.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean ifNullSet(Supplier<T> supp) {
        Bool updated = Bool.createAtomic();
        value.getAndUpdate(old -> {
            if (old != null) {
                updated.set(false);
                return old;
            } else {
                updated.set(true);
                return supp.get();
            }
        });
        return updated.get();
    }

    @Override
    public T apply(UnaryOperator<T> op) {
        return value.getAndUpdate(op);
    }

    @Override
    public T apply(T other, BinaryOperator<T> op) {
        return value.getAndAccumulate(other, op);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value.get());
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o instanceof Obj && Objects.equals(value.get(),
                ((Obj) o).get());
    }

    @Override
    public String toString() {
        return Objects.toString(get());
    }
}
