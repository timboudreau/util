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

import com.mastfrog.function.throwing.ThrowingConsumer;
import com.mastfrog.function.throwing.ThrowingFunction;
import com.mastfrog.function.throwing.ThrowingBiConsumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Adds a few useful methods to CompletionStage and CompletableFuture.
 *
 * @author Tim Boudreau
 */
public interface EnhCompletionStage<T> extends CompletionStage<T> {

    default <R> EnhCompletionStage<R> transform(ThrowingFunction<? super T, ? extends R> xform) {
        EnhCompletableFuture<R> result = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                result.completeExceptionally(thrown);
            } else {
                try {
                    result.complete(xform.apply(t));
                } catch (Throwable ex) {
                    result.completeExceptionally(ex);
                }
            }
        });
        return result;
    }

    /**
     * Pass a consumer to be called if this stage completes normally; it is
     * assumed that other code will already handle any errors.
     *
     * @param consumer A consumer
     * @return this
     */
    default EnhCompletableFuture<T> onSuccess(ThrowingConsumer<T> consumer) {
        EnhCompletableFuture<T> result = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                result.completeExceptionally(thrown);
                return;
            }
            try {
                consumer.accept(t);
                result.complete(t);
            } catch (Throwable ex) {
                result.completeExceptionally(ex);
            }
        });
        return result;
    }

    default EnhCompletionStage<T> forwardExceptions(CompletableFuture<?> other) {
        if (other == this) {
            throw new IllegalArgumentException("Cannot forward exceptions from self");
        }
        this.whenComplete((t, thrown) -> {
            if (thrown != null) {
                other.completeExceptionally(thrown);
            }
        });
        return this;
    }

    default <R> EnhCompletableFuture<R> chain(ThrowingBiConsumer<EnhCompletableFuture<R>, T> next) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                try {
                    next.accept(fut, t);
                } catch (Throwable ex) {
                    fut.completeExceptionally(ex);
                }
            }
        });
        return fut;
    }

    @SuppressWarnings("UseSpecificCatch")
    default <R> EnhCompletableFuture<R> chainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<R>, T> next) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                try {
                    if (test.test(t)) {
                        next.accept(fut, t);
                    }
                } catch (Throwable ex) {
                    fut.completeExceptionally(ex);
                }
            }
        });
        return fut;
    }

    @SuppressWarnings("UseSpecificCatch")
    default <R, S> EnhCompletableFuture<?> heteroChainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<? super R>, T> ifTrue, ThrowingBiConsumer<CompletableFuture<? super S>, T> ifFalse) {
        EnhCompletableFuture<Object> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                if (test.test(t)) {
                    try {
                        if (test.test(t)) {
                            ifTrue.accept(fut, t);
                        } else {
                            ifFalse.accept(fut, t);
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
    default <R, S> EnhCompletableFuture<R> chainConditionally(Predicate<T> test, ThrowingBiConsumer<EnhCompletableFuture<R>, T> ifTrue, ThrowingBiConsumer<EnhCompletableFuture<R>, T> ifFalse) {
        EnhCompletableFuture<R> fut = new EnhCompletableFuture<>();
        whenComplete((t, thrown) -> {
            if (thrown != null) {
                fut.completeExceptionally(thrown);
            } else {
                if (test.test(t)) {
                    try {
                        if (test.test(t)) {
                            ifTrue.accept(fut, t);
                        } else {
                            ifFalse.accept(fut, t);
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
