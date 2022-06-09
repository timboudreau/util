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

import com.mastfrog.util.preconditions.Exceptions;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstraction for things that can be waited on for a result.
 *
 * @see AwaitableCompletionStage
 * @author Tim Boudreau
 */
public interface Awaitable<T> {

    T await() throws InterruptedException;

    T await(long amount, TimeUnit unit) throws InterruptedException;

    default T await(Duration duration) throws InterruptedException {
        return await(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Awaits, rethrowing the interrupted exception as an undeclared throwable.
     *
     * @param duration A duration
     * @return An instance of T
     */
    default T awaitQuietly(Duration duration) {
        try {
            return await(duration);
        } catch (InterruptedException ex) {
            return Exceptions.chuck(ex);
        }
    }
    
    default T awaitQuietly() {
        try {
            return await();
        } catch (InterruptedException ex) {
            return Exceptions.chuck(ex);
        }
    }

    static <T> Awaitable<T> wrap(Future<T> fut) {
        return new Awaitable<T>() {
            @Override
            public T await() throws InterruptedException {
                try {
                    return fut.get();
                } catch (ExecutionException ex) {
                    return Exceptions.chuck(ex.getCause() == null ? ex : ex.getCause());
                }
            }

            @Override
            public T await(long amount, TimeUnit unit) throws InterruptedException {
                try {
                    return fut.get(amount, unit);
                } catch (ExecutionException ex) {
                    return Exceptions.chuck(ex.getCause() == null ? ex : ex.getCause());
                } catch (TimeoutException ex) {
                    return Exceptions.chuck(ex.getCause() == null ? ex : ex.getCause());
                }
            }
        };
    }
}
