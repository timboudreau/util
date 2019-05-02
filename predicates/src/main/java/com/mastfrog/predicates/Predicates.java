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
import java.util.function.Supplier;

/**
 * Utilities for creating predicates, and more specifically, predicates
 * which are <i>loggable</i>, so it is possible to debug complex chains
 * of derivative and chained predicates.
 *
 * @author Tim Boudreau
 */
public final class Predicates {

    /**
     * Create a predicate which wraps a lazily created predicate provided
     * by the passed supplier.
     *
     * @param <T> The type
     * @param supp The supplier
     * @return A predicate
     */
    public static <T> Predicate<T> lazyPredicate(Supplier<Predicate<T>> supp) {
        return new Lazy<>(supp);
    }

    /**
     * Create a named predicate, wrapping a non-named one.
     *
     * @param <T> The predicate test type
     * @param name The name
     * @param pred A predicate
     * @return A named predicate
     */
    public static <T> NamedPredicate<T> namedPredicate(String name, Predicate<T> pred) {
        Named named = Wrapper.find(pred, Named.class);
        if (named != null && !name.equals(named.name())) {
            name = name + "<was:" + named.name() + ">";
        } else if (named != null && name.equals(named.name()) && pred instanceof NamedPredicate<?>) {
            return (NamedPredicate<T>) pred;
        }
        return new NamedWrapperPredicate<>(name, pred);
    }

    /**
     * Create a named predicate with a lazily computed name for performance
     * purposes.
     *
     * @param <T> The type
     * @param name The name
     * @param pred A predicate
     * @return A named predicate
     */
    public static <T> NamedPredicate<T> namedPredicate(Supplier<String> name, Predicate<T> pred) {
        if (pred == null) {
            throw new IllegalArgumentException("Null predicate for " + (name == null ? "null" : name.get()));
        }
        NamedPredicate<T> result;
        if (pred instanceof NamedPredicate<?>) {
            result = (NamedPredicate<T>) pred;
        } else {
            Named named = Wrapper.find(pred, Named.class);
            if (named != null && !name.equals(named.name())) {
                Supplier<String> old = name;
                name = () -> old.get() + "<was:" + named.name() + ">";
            }
            result = new NamedWrapperPredicate<>(name, pred);
        }
        return result;
    }

    /**
     * Return a named predicate by casting, finding one which is
     * wrapped, or at worst storing the current stack trace.
     *
     * @param <T> The type of the predicate
     * @param pred The original predicate
     * @return A named predicate which either is or wraps the
     * original.
     */
    public static <T> NamedPredicate<T> namedPredicate(Predicate<T> pred) {
        if (pred instanceof NamedPredicate<?>) {
            return (NamedPredicate<T>) pred;
        }
        Named n = Wrapper.find(pred, Named.class);
        if (n != null) {
            return namedPredicate(n.name(), pred);
        }
        StackTraceElement[] els = new Exception().getStackTrace();
        if (els == null || els.length == 0) {
            // compiled w/o debug info
            return namedPredicate(pred.toString(), pred);
        }
        StackTraceElement theElement = null;
        String nm = Predicates.class.getName();
        for (int i = 0; i < els.length; i++) {
            String cn = els[i].getClassName();
            if (cn == null) {
                continue;
            }
            if (nm.equals(cn) || (cn != null && (cn.startsWith(nm) || cn.startsWith("java.")))) {
                continue;
            }
            if (cn.contains("AbstractPredicateBuilder")) {
                continue;
            }
            theElement = els[i];
            break;
        }
        return namedPredicate(theElement.toString() + ":" + pred, pred);
    }

    /**
     * Create a predicate which can be added to and will logical OR the
     * results of added predicates, populated with the initial predicate.
     *
     * @param <T> The type
     * @param first The first predicate to install in the resulting list predicate
     * @return A list predicate
     */
    public static <T> ListPredicate<T> orPredicate(Predicate<? super T> first) {
        return new LogicalListPredicate<>(false, first);
    }

    /**
     * Create a predicate which can be added to and will logical OR the
     * results of added predicates.
     *
     * @param <T> The type
     * @return A list predicate
     */
    public static <T> ListPredicate<T> orPredicate() {
        return new LogicalListPredicate<>(false);
    }

    /**
     * Create a predicate which will use the passed conversion function to
     * derive an object to test with the passed predicate.
     *
     * @param <V> The derived type
     * @param <T> The original type
     * @param converter A conversion function
     * @param predicate The original predicate
     * @param onNull an AbsenceAction or other BooleanSupplier
     * @return A predicate
     */
    public static final <V, T> Predicate<T> converted(Function<T, V> converter, Predicate<V> predicate, BooleanSupplier onNull) {
        return new ConvertingPredicate<>(predicate, converter, onNull);
    }

    /**
     * Create a predicate which will use the passed conversion function to
     * derive an object to test with the passed predicate.  The resulting
     * predicate will return <code>true</code> for nulls.
     *
     * @param <V> The derived type
     * @param <T> The original type
     * @param converter A conversion function
     * @param predicate The original predicate
     * @return A predicate
     */
    public static final <V, T> Predicate<T> converted(Function<T, V> converter, Predicate<V> predicate) {
        return new ConvertingPredicate<>(predicate, converter, AbsenceAction.TRUE);
    }

    /**
     * Conversion method for named predicates, with same parameters as
     * above.
     *
     * @param <V>
     * @param <T>
     * @param predicate
     * @param converter
     * @param onNull
     * @return
     */
    public static final <V, T> NamedPredicate<T> converted(NamedPredicate<V> predicate, Function<T, V> converter, AbsenceAction onNull) {
        return new ConvertingNamedPredicate<>(predicate, converter, onNull);
    }

    /**
     * Conversion method for named predicates, with same parameters as
     * above.
     *
     * @param <V>
     * @param <T>
     * @param predicate
     * @param converter
     * @param onNull
     * @return
     */
    public static final <V, T> NamedPredicate<T> converted(NamedPredicate<V> predicate, Function<T, V> converter) {
        return new ConvertingNamedPredicate<>(predicate, converter, AbsenceAction.TRUE);
    }

    /**
     * Convert a named predicate to one which can process an iterable of
     * the objects it is typed on.
     *
     * @param <T> The type
     * @param and If true, logically and rather than or the results
     * @param orig The original predicate
     * @return A named predicate
     */
    static <T> NamedPredicate<Iterable<T>> listPredicate(boolean and, NamedPredicate<? super T> orig) {
        return new ListConvertedNamedPredicate<>(orig, and);
    }
    /**
     * Convert a predicate to one which can process an iterable of
     * the objects it is typed on.
     *
     * @param <T> The type
     * @param orig The original predicate
     * @param and If true, logically and rather than or the results
     * @return A named predicate
     */
    static <T> Predicate<Iterable<T>> listPredicate(Predicate<? super T> orig, boolean and) {
        if (orig instanceof NamedPredicate<?>) {
            return new ListConvertedNamedPredicate<>((NamedPredicate<? super T>) orig, and);
        } else {
            return new ListConvertedPredicate<>(orig, and);
        }
    }

    /**
     * Create a new list predicate which will logically AND all contained
     * predicates, populated with the initial predicate passed here.
     *
     * @param <T> The type
     * @param first The first predicate
     * @return
     */
    public static <T> ListPredicate<T> andPredicate(Predicate<? super T> first) {
        return new LogicalListPredicate<>(true, first);
    }

    /**
     * Create a new list predicate which will logically AND all contained
     * predicates.
     *
     * @param <T> The type
     * @return A list predicate
     */
    public static <T> ListPredicate<T> andPredicate() {
        return new LogicalListPredicate<>(true);
    }

    private Predicates() {
        throw new AssertionError();
    }

    @SuppressWarnings(value = "unchecked")
    public static <T> NamedPredicate<T> alwaysTrue() {
        return (NamedPredicate<T>) FixedPredicate.TRUE;
    }

    @SuppressWarnings(value = "unchecked")
    public static <T> NamedPredicate<T> alwaysFalse() {
        return (NamedPredicate<T>) FixedPredicate.FALSE;
    }
}
