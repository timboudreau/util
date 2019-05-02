/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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

package com.mastfrog.predicates;

import com.mastfrog.abstractions.Named;
import com.mastfrog.abstractions.Wrapper;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class ConvertingNamedPredicate<T, V> implements Wrapper<Predicate<V>>, NamedPredicate<T> {

    private final NamedPredicate<V> delegate;
    private final Function<T, V> converter;
    private final BooleanSupplier nullConversionHandler;

    public ConvertingNamedPredicate(NamedPredicate<V> delegate, Function<T, V> converter, BooleanSupplier nullConversionHandler) {
        this.delegate = delegate;
        this.converter = converter;
        this.nullConversionHandler = nullConversionHandler;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Predicate<V> wrapped() {
        return delegate;
    }

    @Override
    public boolean test(T t) {
        V v = converter.apply(t);
        if (v == null && nullConversionHandler != null) {
            if (nullConversionHandler != AbsenceAction.PASS_THROUGH) {
                return nullConversionHandler.getAsBoolean();
            }
        }
        return delegate.test(v);
    }

    public String toString() {
        return Named.findName(delegate);
    }

    @Override
    public NamedPredicate<T> and(Predicate<? super T> other) {
        return Predicates.andPredicate(this).and(other);
    }

    @Override
    public NamedPredicate<T> or(Predicate<? super T> other) {
        return Predicates.orPredicate(this).or(other);
    }

}
