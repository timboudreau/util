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

    default T setFrom(Supplier<T> supp) {
        return set(supp.get());
    }
}
