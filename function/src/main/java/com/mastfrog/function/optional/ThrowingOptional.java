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
package com.mastfrog.function.optional;

import com.mastfrog.function.throwing.ThrowingConsumer;
import com.mastfrog.function.throwing.ThrowingFunction;
import com.mastfrog.function.throwing.ThrowingPredicate;
import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Because optional is unusable with code that routinely does I/O or waits on
 * things, without becoming littered with try/catch blocks if you try to use
 * functional operations. Same API as Optional, but functional methods take
 * functions which can throw exceptions, which are rethrown as undeclared
 * throwable exceptions via
 * {@link com.mastfrog.util.preconditions.Exceptions#chuck(java.lang.Throwable)}.
 *
 * @author Tim Boudreau
 */
public interface ThrowingOptional<T> extends Supplier<T> {

    /**
     * Create an instance over an existing Optional.
     * @param <T> The type
     * @param delegate The optional
     * @return A ThrowingOptional
     */
    public static <T> ThrowingOptional<T> from(Optional<T> delegate) {
        return new ThrowingOptionalWrapper<>(notNull("delegate", delegate));
    }

    /**
     * Get the empty instance.
     * @param <T> A type
     * @return A ThrowingOptional.
     */
    @SuppressWarnings("unchecked")
    public static <T> ThrowingOptional<T> empty() {
        return (ThrowingOptionalWrapper<T>) ThrowingOptionalWrapper.EMPTY;
    }

    /**
     * Get an instance over the passed non-null object.
     * 
     * @param <T> The type
     * @param obj The object, not null
     * @return A ThrowingOptional.
     */
    public static <T> ThrowingOptional<T> of(T obj) {
        return new ThrowingOptionalWrapper<>(Optional.of(obj));
    }

    /**
     * Get an instance over an object which may be null.
     * @param <T> The type
     * @param obj An object
     * @return A ThrowingOptional
     */
    @SuppressWarnings("unchecked")
    public static <T> ThrowingOptional<T> ofNullable(T obj) {
        if (obj == null) {
            return (ThrowingOptionalWrapper<T>) ThrowingOptionalWrapper.EMPTY;
        }
        return new ThrowingOptionalWrapper<>(Optional.ofNullable(obj));
    }

    ThrowingOptional<T> filter(ThrowingPredicate<? super T> predicate);

    <U> ThrowingOptional<U> flatMap(
            ThrowingFunction<? super T, ? extends Optional<? extends U>> mapper);

    default <U> ThrowingOptional<U> flatMapThrowing(
            ThrowingFunction<? super T, ? extends ThrowingOptional<? extends U>> mapper) {
        return flatMap(t -> mapper.apply(t).toOptional());
    }

    @Override
    T get();

    boolean ifPresent(ThrowingConsumer<? super T> action);

    void ifPresentOrElse(ThrowingConsumer<? super T> action, ThrowingRunnable emptyAction);

    boolean isEmpty();

    boolean isPresent();

    <U> ThrowingOptional<U> map(ThrowingFunction<? super T, ? extends U> mapper);

    ThrowingOptional<T> or(ThrowingSupplier<? extends Optional<? extends T>> supplier);

    default ThrowingOptional<T> or(ThrowingOptional<? extends T> supplier) {
        return or(() -> supplier.toOptional());
    }

    T orElse(T other);

    T orElseGet(ThrowingSupplier<? extends T> supplier);

    Stream<T> stream();

    Optional<T> toOptional();
}
