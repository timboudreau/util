/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.concurrent.future;

import com.mastfrog.function.state.Obj;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CompletionStage has no way to directly wait on a result, and
 * CompletableFuture should not be directly exposed unless you want someone to
 * be able to randomly complete it. So ... this.
 *
 * @author Tim Boudreau
 */
final class AwaitableCompletionStageImpl<T> implements AwaitableCompletionStage<T> {

    private final CompletionStage<T> future;

    AwaitableCompletionStageImpl(CompletionStage<T> future) {
        this.future = notNull("future", future);
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenApply(
            Function<? super T, ? extends U> fn) {
        return AwaitableCompletionStage.of(future.thenApply(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenApplyAsync(
            Function<? super T, ? extends U> fn) {
        return AwaitableCompletionStage.of(future.thenApplyAsync(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenApplyAsync(
            Function<? super T, ? extends U> fn, Executor executor) {
        return AwaitableCompletionStage.of(future.thenApplyAsync(fn, executor));
    }

    @Override
    public final CompletionStage<Void> thenAccept(Consumer<? super T> action) {
        return future.thenAccept(action);
    }

    @Override
    public final CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
        return future.thenAcceptAsync(action);
    }

    @Override
    public final CompletionStage<Void> thenAcceptAsync(
            Consumer<? super T> action, Executor executor) {
        return future.thenAcceptAsync(action, executor);
    }

    @Override
    public final CompletionStage<Void> thenRun(Runnable action) {
        return future.thenRun(action);
    }

    @Override
    public final CompletionStage<Void> thenRunAsync(Runnable action) {
        return future.thenRunAsync(action);
    }

    @Override
    public final CompletionStage<Void> thenRunAsync(
            Runnable action, Executor executor) {
        return future.thenRunAsync(action, executor);
    }

    @Override
    public final <U, V> CompletionStage<V> thenCombine(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return future.thenCombine(other, fn);
    }

    @Override
    public final <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return future.thenCombineAsync(other, fn);
    }

    @Override
    public final <U, V> CompletionStage<V> thenCombineAsync(
            CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return future.thenCombineAsync(other, fn, executor);
    }

    @Override
    public final <U> CompletionStage<Void> thenAcceptBoth(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return future.thenAcceptBoth(other, action);
    }

    @Override
    public final <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return future.thenAcceptBothAsync(other, action);
    }

    @Override
    public final <U> CompletionStage<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return future.thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public final CompletionStage<Void> runAfterBoth(
            CompletionStage<?> other, Runnable action) {
        return future.runAfterBoth(other, action);
    }

    @Override
    public final CompletionStage<Void> runAfterBothAsync(
            CompletionStage<?> other, Runnable action) {
        return future.runAfterBothAsync(other, action);
    }

    @Override
    public final CompletionStage<Void> runAfterBothAsync(
            CompletionStage<?> other, Runnable action, Executor executor) {
        return future.runAfterBothAsync(other, action, executor);
    }

    @Override
    public final <U> CompletionStage<U> applyToEither(
            CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return future.applyToEither(other, fn);
    }

    @Override
    public final <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return future.applyToEitherAsync(other, fn);
    }

    @Override
    public final <U> CompletionStage<U> applyToEitherAsync(
            CompletionStage<? extends T> other,
            Function<? super T, U> fn, Executor executor) {
        return future.applyToEitherAsync(other, fn, executor);
    }

    @Override
    public final CompletionStage<Void> acceptEither(
            CompletionStage<? extends T> other, Consumer<? super T> action) {
        return future.acceptEither(other, action);
    }

    @Override
    public final CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action) {
        return future.acceptEitherAsync(other, action);
    }

    @Override
    public final CompletionStage<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return future.acceptEitherAsync(other, action, executor);
    }

    @Override
    public final CompletionStage<Void> runAfterEither(CompletionStage<?> other,
            Runnable action) {
        return future.runAfterEither(other, action);
    }

    @Override
    public final CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other,
            Runnable action) {
        return future.runAfterEitherAsync(other, action);
    }

    @Override
    public final CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other,
            Runnable action, Executor executor) {
        return future.runAfterEitherAsync(other, action, executor);
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return AwaitableCompletionStage.of(future.thenCompose(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn) {
        return AwaitableCompletionStage.of(future.thenComposeAsync(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return AwaitableCompletionStage.of(future.thenComposeAsync(fn, executor));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> handle(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return AwaitableCompletionStage.of(future.handle(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn) {
        return AwaitableCompletionStage.of(future.handleAsync(fn));
    }

    @Override
    public final <U> AwaitableCompletionStage<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return AwaitableCompletionStage.of(future.handleAsync(fn, executor));
    }

    @Override
    public final AwaitableCompletionStage<T> whenComplete(
            BiConsumer<? super T, ? super Throwable> action) {
        return AwaitableCompletionStage.of(future.whenComplete(action));
    }

    @Override
    public final AwaitableCompletionStage<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action) {
        return AwaitableCompletionStage.of(future.whenCompleteAsync(action));
    }

    @Override
    public final AwaitableCompletionStage<T> whenCompleteAsync(
            BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return AwaitableCompletionStage.of(future.whenCompleteAsync(action, executor));
    }

    @Override
    public final CompletionStage<T> exceptionally(
            Function<Throwable, ? extends T> fn) {
        return future.exceptionally(fn);
    }

    /*
    // JDK-17isms - if we need them, we could implement them on our own:

    @Override
    public final CompletionStage<T> exceptionallyAsync(
            Function<Throwable, ? extends T> fn)
    {
        return future.exceptionallyAsync(fn);
    }

    @Override
    public final CompletionStage<T> exceptionallyAsync(
            Function<Throwable, ? extends T> fn, Executor executor)
    {
        return future.exceptionallyAsync(fn, executor);
    }

    @Override
    public final CompletionStage<T> exceptionallyCompose(
            Function<Throwable, ? extends CompletionStage<T>> fn)
    {
        return future.exceptionallyCompose(fn);
    }

    @Override
    public final CompletionStage<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn)
    {
        return future.exceptionallyComposeAsync(fn);
    }

    @Override
    public final CompletionStage<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor)
    {
        return future.exceptionallyComposeAsync(fn, executor);
    }
     */
    @Override
    public final CompletableFuture<T> toCompletableFuture() {
        return future.toCompletableFuture();
    }

    @Override
    public final String toString() {
        return future.toString();
    }

    @Override
    @SuppressWarnings("ThrowableResultIgnored")
    public T await() throws InterruptedException {
        Obj<Throwable> rethrow = Obj.createAtomic();
        Obj<T> result = Obj.createAtomic();
        CountDownLatch latch = new CountDownLatch(1);
        future.whenCompleteAsync((obj, thrown)
                -> {
            try {
                if (thrown != null) {
                    rethrow.set(thrown);
                } else if (obj != null) {
                    result.set(obj);
                }
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        rethrow.ifNotNull(thrown
                -> {
            Exceptions.chuck(thrown);
        });
        return result.get();
    }

    @Override
    public T await(long amount, TimeUnit unit) throws InterruptedException {
        Obj<Throwable> rethrow = Obj.createAtomic();
        Obj<T> result = Obj.createAtomic();
        CountDownLatch latch = new CountDownLatch(1);
        future.whenCompleteAsync((obj, thrown)
                -> {
            try {
                result.set(obj);
            } finally {
                latch.countDown();
            }
        });
        latch.await(amount, unit);
        rethrow.ifNotNull(thrown
                -> {
            // Rethrows a checked exception without wrappering - see
            // https://timboudreau.com/blog/Unchecked_checked_exceptions
            Exceptions.chuck(thrown);
        });
        return result.get();
    }
}
