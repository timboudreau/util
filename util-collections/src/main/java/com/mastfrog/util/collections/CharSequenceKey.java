/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.collections;

import com.mastfrog.util.strings.Strings;
import java.util.stream.IntStream;

/**
 * Wrapper for a CharSequence which implements case-insensitive equality.
 *
 * @author Tim Boudreau
 */
final class CharSequenceKey<T extends CharSequence> implements CharSequence {

    private final T value;

    private CharSequenceKey(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T extends CharSequence> CharSequenceKey<T> create(CharSequence seq) {
        return seq instanceof CharSequenceKey<?> ? (CharSequenceKey<T>) seq : new CharSequenceKey(seq);
    }

    static <T extends CharSequence> Converter<CharSequenceKey<T>, T> converter() {
        return new Conv<>();
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new CharSequenceKey<>(value.subSequence(start, end));
    }

    @Override
    public IntStream chars() {
        return value.chars();
    }

    @Override
    public IntStream codePoints() {
        return value.codePoints();
    }

    @Override
    public int hashCode() {
        return Strings.charSequenceHashCode(value, true);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharSequence && Strings.charSequencesEqual(value, (CharSequence) o, true);
    }

    @Override
    public String toString() {
        return value.toString();
    }

    static class Conv<T extends CharSequence> implements Converter<CharSequenceKey<T>, T> {

        @Override
        public CharSequenceKey<T> convert(T r) {
            return new CharSequenceKey<>(r);
        }

        @Override
        public T unconvert(CharSequenceKey<T> t) {
            return t.value;
        }
    }
}
