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

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extended <code>Predicate&lt;String&gt;</code> which allows custom
 * null-handling and ability to trim whitespace from the string before testing,
 * and, where possible, provide reasonable and meaningful implementations of
 * toString(), equals() and hashCode().
 * <p>
 * Methods which return a logically related predicate (or, negate, and) also
 * return an EnhStringPredicate whose toString() method can similarly describe
 * what the predicate does.
 * </p>
 *
 * @author Tim Boudreau
 */
public interface EnhStringPredicate extends Predicate<String> {

    /**
     * Returns a wrapper for this predicate which will call
     * <code>String.trim()</code> on its input when non-null, and test on the
     * result of that.
     *
     * @return A predicate
     */
    default EnhStringPredicate trimmingInput() {
        return new TrimmingStringPredicate(this);
    }

    /**
     * Returns a wrapper that customizes the behavior when null is passed as an
     * argument, either passing or failing.
     *
     * @param passFail The value to return from <code>test()</code> when the
     * input is null
     * @return A predicate
     */
    default EnhStringPredicate onNull(boolean passFail) {
        return new NullHandlingStringPredicate(passFail, this);
    }

    /**
     * Returns a wrapper that returns <code>true</code> when the input is null.
     *
     * @return A predicate
     */
    default EnhStringPredicate passOnNull() {
        return onNull(true);
    }

    /**
     * Returns a wrapper that returns <code>false</code> when the input is null.
     *
     * @return A predicate
     */
    default EnhStringPredicate failOnNull() {
        return onNull(false);
    }

    /**
     * Returns a wrapper around this that uses the passed conversion function to
     * convert the input object of type T to a string which will then be tested.
     * Note the returned predicate's equality contract depends on the passed
     * function correctly implementing <code>equals(Object)</code> and
     * <code>hashCode()</code>.
     *
     * @param <T> The type
     * @param converter A conversion function
     * @return A predicate that wraps this one
     */
    default <T> Predicate<T> asType(Function<T, String> converter) {
        return new ConvertedStringPredicate<>(converter, this);
    }

    /**
     * Returns a wrapper around this that calls <code>toString()</code> to
     * convert the input object of type T to a string which will then be tested.
     * Null input is converted to the empty string.
     *
     * @param <T> A type
     * @return A predicate
     */
    default <T> Predicate<T> asToStringPredicate() {
        return ConvertedStringPredicate.<T>toStringPredicate(this);
    }

    @Override
    default EnhStringPredicate negate() {
        return new NegatedStringPredicate(this);
    }

    @Override
    default EnhStringPredicate or(Predicate<? super String> other) {
        return new LogicalStringPredicate(true, this, other);
    }

    @Override
    default EnhStringPredicate and(Predicate<? super String> other) {
        return new LogicalStringPredicate(false, this, other);
    }

    default EnhStringPredicate andNot(Predicate<? super String> other) {
        return and(other.negate());
    }
}
