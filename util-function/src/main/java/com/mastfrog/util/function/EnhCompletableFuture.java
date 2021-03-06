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

/**
 *
 * @author Tim Boudreau
 */
public class EnhCompletableFuture<T> extends CompletableFuture<T> implements EnhCompletionStage<T>, CompletionStage<T> {

    public final EnhCompletableFuture<T> listenTo(CompletionStage<?> other) {
        if (other == this) {
            throw new IllegalArgumentException("Cannot listen to self");
        }
        other.whenCompleteAsync((t, thrown) -> {
            if (thrown != null) {
                completeExceptionally(thrown);
            }
        });
        return this;
    }

    public final EnhCompletableFuture<T> attachTo(CompletionStage<? extends T> other) {
        if (other == this) {
            throw new IllegalArgumentException("Cannot attach to self");
        }
        other.whenComplete((t, thrown) -> {
            if (thrown != null) {
                completeExceptionally(thrown);
            } else {
                complete(t);
            }
        });
        return this;
    }

    public EnhCompletionStage<T> forwardExceptions(CompletableFuture<?> other) {
        EnhCompletionStage.super.forwardExceptions(other);
        return this;
    }
}
