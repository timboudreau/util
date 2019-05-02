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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A predicte which has a name, of logging and debugging purposes.
 *
 * @author Tim Boudreau
 */
public interface NamedPredicate<T> extends Named, Predicate<T> {

    /**
     * Create a predicate from this one, which will process the contents of an
     * iterable of its type.
     *
     * @param and If true, use logical AND operations, not logical OR operations
     * over testing the Iterable's elements to construct the result
     * @return A named predicate
     */
    public default NamedPredicate<Iterable<T>> toListPredicate(boolean and) {
        return Predicates.listPredicate(and, this);
    }

    /**
     * Create a predicate from this one, which will process the contents of an
     * iterable derived from the original object, of its the passed function's
     * return type. This allows for testing collections of elements accessed via
     * a method of the original object.
     *
     * @param func A function which accepts the original object, and extracts a
     * list of some time from it
     * @return A named predicate
     */
    default <R> NamedPredicate<R> listTransform(Function<R, List<? extends T>> func) {
        return new ListTransformPredicate<>(this, func);
    }

    /**
     * Convert a named predicate to one that tests a derived object, using the
     * passed AbsenceAction to determine the result if the original or derived
     * object is null.
     *
     * @param <R> The type of the returned predicate
     * @param converter A conversion function
     * @param onNullConversion What to return if passed a null
     * @return A named predicate
     */
    default <R> NamedPredicate<R> convert(Function<R, T> converter, AbsenceAction onNullConversion) {
        return Predicates.converted(this, converter, onNullConversion);
    }

    /**
     * And in a non-named predicate, with a best effort to find a human-readable
     * name for the passed one.
     *
     * @param other Another predicate
     * @return A named predicate
     */
    @Override
    public default NamedPredicate<T> and(Predicate<? super T> other) {
        Predicate<T> myRoot = Wrapper.root(this);
        Predicate<? super T> otherRoot = Wrapper.root(other);
        Predicate<T> toInvoke = myRoot.and(otherRoot);
        String myName = name();
        String otherName = Named.findName(other);
        return new NamedWrapperPredicate<>("(" + myName + " && " + otherName + ")", toInvoke);
    }

    /**
     * Negate this predicate.
     *
     * @return A named predicate
     */
    @Override
    public default NamedPredicate<T> negate() {
        Predicate<T> myRoot = Wrapper.root(this);
        String myName = name();
        return new NamedWrapperPredicate<T>("!" + myName, myRoot) {
            @Override
            public NamedPredicate<T> negate() {
                return NamedPredicate.this;
            }
        };
    }

    /**
     * Or in another predicate, with best effort to find a human readable
     * name for it.
     *
     * @param other Another predicate
     * @return A predicate
     */
    @Override
    public default NamedPredicate<T> or(Predicate<? super T> other) {
        Predicate<T> myRoot = Wrapper.root(this);
        Predicate<? super T> otherRoot = Wrapper.root(other);
        Predicate<T> toInvoke = myRoot.or(otherRoot);
        String myName = name();
        String otherName = Named.findName(other);
        return new NamedWrapperPredicate<>("(" + myName + " || " + otherName + ")", toInvoke);
    }
}
