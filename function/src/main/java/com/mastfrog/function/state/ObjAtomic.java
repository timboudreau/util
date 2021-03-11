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
