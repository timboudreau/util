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
package com.mastfrog.concurrent.future;

import com.mastfrog.function.throwing.ThrowingSupplier;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * CompletionStage has no way to do a blocking wait for a result, and
 * CompletableFuture should not be directly exposed unless you want to invite
 * thw world to be able to randomly complete it, so - this.
 *
 * @author Tim Boudreau
 */
public interface AwaitableCompletionStage<T> extends CompletionStage<T>, Awaitable<T> {

    public static <R> AwaitableCompletionStage<R> of(CompletionStage<R> stage) {
        if (stage instanceof AwaitableCompletionStage<?>) {
            return (AwaitableCompletionStage<R>) stage;
        }
        return new AwaitableCompletionStageImpl<>(notNull("stage", stage));
    }

    public static <R> AwaitableCompletionStage<R> from(ThrowingSupplier<CompletionStage<R>> supp) {
        try {
            return of(supp.get());
        } catch (Exception | Error e) {
            CompletableFuture<R> fut = new CompletableFuture<>();
            fut.completeExceptionally(e);
            return of(fut);
        }
    }

    // Override a few things we're likely to need to directly return
    // what we want.
    @Override
    <U> AwaitableCompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    @Override
    <U> AwaitableCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    @Override
    <U> AwaitableCompletionStage<U> thenApplyAsync(
            Function<? super T, ? extends U> fn, Executor executor);

    @Override
    <U> AwaitableCompletionStage<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> fn);

    @Override
    <U> AwaitableCompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn);

    @Override
    <U> AwaitableCompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn, Executor executor);

    @Override
    <U> AwaitableCompletionStage<U> handle(
            BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> AwaitableCompletionStage<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> AwaitableCompletionStage<U> handleAsync(
            BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);

    @Override
    public AwaitableCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor);

    @Override
    public AwaitableCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    @Override
    public AwaitableCompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);
}
