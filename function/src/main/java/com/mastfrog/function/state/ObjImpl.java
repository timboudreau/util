package com.mastfrog.function.state;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class ObjImpl<T> implements Obj<T> {

    private T obj;

    ObjImpl() {

    }

    ObjImpl(T initial) {
        this.obj = initial;
    }

    @Override
    public T get() {
        return obj;
    }

    @Override
    public T set(T obj) {
        T old = this.obj;
        this.obj = obj;
        return old;
    }

    @Override
    public boolean ifUpdate(T newValue, Runnable ifChange) {
        if (!Objects.equals(obj, newValue)) {
            obj = newValue;
            ifChange.run();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return Objects.toString(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(obj);
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o instanceof Obj && Objects.equals(obj,
                ((Obj) o).get());
    }
}
