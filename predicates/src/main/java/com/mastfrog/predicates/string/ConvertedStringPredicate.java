/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.predicates.string;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class ConvertedStringPredicate<T> implements Predicate<T> {

    private final Function<T, String> converter;
    private final Predicate<String> delegate;

    ConvertedStringPredicate(Function<T, String> converter, Predicate<String> delegate) {
        this.converter = converter;
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    static <T> ConvertedStringPredicate<T> toStringPredicate(Predicate<String> delegate) {
        return new ConvertedStringPredicate<>(ToStringConverter.INSTANCE.cast(), delegate);
    }

    @Override
    public boolean test(T t) {
        return delegate.test(converter.apply(t));
    }

    @Override
    public String toString() {
        return "converted(" + converter + ", " + delegate + ")";
    }

    private static final class ToStringConverter<T> implements Function<T, String> {

        static final ToStringConverter<Object> INSTANCE = new ToStringConverter<>();

        @SuppressWarnings("unchecked")
        <T> Function<T, String> cast() {
            return (Function<T, String>) this;
        }

        @Override
        public String apply(T t) {
            return t == null ? "" : t.toString();
        }

        public String toString() {
            return "toString()";
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ToStringConverter;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

}
