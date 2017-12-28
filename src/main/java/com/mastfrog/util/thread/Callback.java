/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A NodeJS style functional callback - use with Receiver.of() for older APIs
 * that take Receiver.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface Callback<T> {

    public void receive(Throwable err, T obj);

    default Callback<T> attachTo(CompletableFuture<T> fut) {
        fut.whenComplete((T t, Throwable u) -> {
            receive(u, t);
        });
        return this;
    }

    static <T> Callback<T> fromCompletableFuture(CompletableFuture<T> fut) {
        return (Throwable err, T obj) -> {
            if (err != null) {
                fut.completeExceptionally(err);
            } else {
                fut.complete(obj);
            }
        };
    }

    static <T> Callback<T> fromCompletableFuture(CompletableFuture<T> fut, Supplier<T> ifNullResult) {
        return (Throwable err, T obj) -> {
            if (err != null) {
                fut.completeExceptionally(err);
            } else {
                if (obj == null) {
                    try {
                        fut.complete(ifNullResult.get());
                    } catch (Exception ex) {
                        fut.completeExceptionally(ex);
                    }
                } else {
                    fut.complete(obj);
                }
            }
        };
    }

}
