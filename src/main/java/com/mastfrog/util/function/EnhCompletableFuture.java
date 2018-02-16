/*
 * The MIT License
 *
 * Copyright 2018 tim.
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
package com.mastfrog.util.function;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public class EnhCompletableFuture<T> extends CompletableFuture<T> implements EnhCompletionStage<T>, CompletionStage<T> {

    public final EnhCompletableFuture<T> listenTo(CompletionStage<?> other) {
        other.whenCompleteAsync((t, thrown) -> {
            if (thrown != null) {
                completeExceptionally(thrown);
            }
        });
        return this;
    }

    public final EnhCompletableFuture<T> attachTo(CompletionStage<T> other) {
        other.whenComplete((t, thrown) -> {
            if (thrown != null) {
                completeExceptionally(thrown);
            } else {
                complete(t);
            }
        });
        return this;
    }

    public <R> EnhCompletableFuture<R> chain(ThrowingBiConsumer<EnhCompletableFuture<R>, T> next) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                try {
                    next.apply(fut, t);
                } catch (Throwable ex) {
                    fut.completeExceptionally(ex);
                }
            }
        });
        return fut;
    }

    @SuppressWarnings("UseSpecificCatch")
    public <R> EnhCompletableFuture<R> chainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<R>, T> next) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                try {
                    if (test.test(t)) {
                        next.apply(fut, t);
                    }
                } catch (Throwable ex) {
                    fut.completeExceptionally(ex);
                }
            }
        });
        return fut;
    }

    @SuppressWarnings("UseSpecificCatch")
    public <R, S> EnhCompletableFuture<?> heteroChainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<? super R>, T> ifTrue, ThrowingBiConsumer<CompletableFuture<? super S>, T> ifFalse) {
        EnhCompletableFuture<Object> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                if (test.test(t)) {
                    try {
                        if (test.test(t)) {
                            ifTrue.apply(fut, t);
                        } else {
                            ifFalse.apply(fut, t);
                        }
                    } catch (Throwable ex) {
                        fut.completeExceptionally(ex);
                    }
                }
            }
        });
        return fut;
    }

    @SuppressWarnings("UseSpecificCatch")
    public <R, S> EnhCompletableFuture<R> chainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<R>, T> ifTrue, ThrowingBiConsumer<EnhCompletableFuture<R>, T> ifFalse) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                if (test.test(t)) {
                    try {
                        if (test.test(t)) {
                            ifTrue.apply(fut, t);
                        } else {
                            ifFalse.apply(fut, t);
                        }
                    } catch (Throwable ex) {
                        fut.completeExceptionally(ex);
                    }
                }
            }
        });
        return fut;
    }
}
