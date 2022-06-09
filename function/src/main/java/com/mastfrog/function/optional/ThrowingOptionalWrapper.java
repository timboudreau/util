package com.mastfrog.function.optional;

import com.mastfrog.function.throwing.ThrowingConsumer;
import com.mastfrog.function.throwing.ThrowingFunction;
import com.mastfrog.function.throwing.ThrowingPredicate;
import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.preconditions.Exceptions;
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
final class ThrowingOptionalWrapper<T> implements Supplier<T>, ThrowingOptional<T> {

    static final ThrowingOptionalWrapper<Object> EMPTY = new ThrowingOptionalWrapper<>(
            Optional.empty());

    private final Optional<T> delegate;

    ThrowingOptionalWrapper(Optional<T> delegate) {
        this.delegate = notNull("delegate", delegate);
    }

    @Override
    public Optional<T> toOptional() {
        return delegate;
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public boolean isPresent() {
        return delegate.isPresent();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean ifPresent(ThrowingConsumer<? super T> action) {
        delegate.ifPresent(action.toNonThrowing());
        return delegate.isPresent();
    }

    @Override
    public void ifPresentOrElse(ThrowingConsumer<? super T> action, ThrowingRunnable emptyAction) {
        delegate.ifPresentOrElse(action.toNonThrowing(), emptyAction.toRunnable());
    }

    @Override
    public ThrowingOptionalWrapper<T> filter(ThrowingPredicate<? super T> predicate) {
        return new ThrowingOptionalWrapper<>(delegate.filter(obj
                -> {
            try {
                return predicate.test(obj);
            } catch (Exception | Error e) {
                return Exceptions.chuck(e);
            }
        }));
    }

    @Override
    public <U> ThrowingOptionalWrapper<U> map(ThrowingFunction<? super T, ? extends U> mapper) {
        return new ThrowingOptionalWrapper<>(delegate.map(mapper.toNonThrowing()));
    }

    @Override
    public <U> ThrowingOptionalWrapper<U> flatMap(ThrowingFunction<? super T, ? extends Optional<? extends U>> mapper) {
        return new ThrowingOptionalWrapper<>(delegate.flatMap(mapper.toNonThrowing()));
    }

    @Override
    public <U> ThrowingOptionalWrapper<U> flatMapThrowing(ThrowingFunction<? super T, ? extends ThrowingOptional<? extends U>> mapper) {
        return new ThrowingOptionalWrapper<>(delegate.flatMap(obj
                -> {
            return mapper.toNonThrowing().apply(obj).toOptional();
        }));
    }

    @Override
    public ThrowingOptionalWrapper<T> or(ThrowingSupplier<? extends Optional<? extends T>> supplier) {
        return new ThrowingOptionalWrapper<>(delegate.or(()
                -> {
            try {
                return supplier.get();
            } catch (Exception | Error e) {
                return Exceptions.chuck(e);
            }
        }));
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public T orElse(T other) {
        return delegate.orElse(other);
    }

    @Override
    public T orElseGet(ThrowingSupplier<? extends T> supplier) {
        return delegate.orElseGet(()
                -> {
            try {
                return supplier.get();
            } catch (Exception | Error e) {
                return Exceptions.chuck(e);
            }
        });
    }

    @Override
    public String toString() {
        if (delegate.isPresent()) {
            return delegate.get().toString();
        }
        return "ThrowingOptional.empty()";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || o.getClass() != ThrowingOptionalWrapper.class) {
            return false;
        }
        ThrowingOptionalWrapper other = (ThrowingOptionalWrapper) o;
        return other.delegate.equals(delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
