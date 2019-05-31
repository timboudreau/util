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
package com.mastfrog.util.net;

import com.mastfrog.util.preconditions.Checks;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntToLongFunction;

/**
 * Implementation of exponential (or whatever) backoff strategy, with the
 * ability to repeatedly call a producer until it succeeds or max retries is
 * met.
 *
 * @author Tim Boudreau
 */
public class Backoff {

    private final IntToLongFunction backoffFunction;
    private final int maxRetries;
    public static Backoff EXPONENTIAL_BACKOFF = new Backoff();
    public static Backoff LINEAR_BACKOFF = new Backoff(linearBackoff());

    public Backoff(IntToLongFunction backoffFunction, int maxRetries) {
        this.backoffFunction = backoffFunction;
        this.maxRetries = maxRetries;
    }

    public Backoff(IntToLongFunction backoffFunction) {
        this(backoffFunction, Integer.MAX_VALUE);
    }

    public Backoff() {
        this(exponentialBackoff());
    }

    public void block(int iteration) throws InterruptedException {
        Thread.sleep(backoffFunction.applyAsLong(iteration));
    }

    /**
     * A function which produces exponential backoff with a maximum value.
     *
     * @param baseRetryInterval The base interval, which is multiplied by the
     * square of the iteration
     * @param jitterRange The amount of randomness to apply to the resulting
     * delay to avoid livelock in the case of multiple servers restarting
     * @param maxDelay The maximum delay
     * @return A function which, when passed an iteration number, will return
     * the appropriate delay for that iteration, which will never return less
     * than one.
     */
    public static IntToLongFunction exponentialBackoff(long baseRetryInterval, int jitterRange, long maxDelay) {
        return (ct) -> {
            long jitter = jitterRange == 0 ? 0 : ThreadLocalRandom.current().nextInt(jitterRange);
            long interval = Math.min(maxDelay, baseRetryInterval * (ct * ct));
            return Math.max(1, (interval - (jitter / 2)) + jitter);
        };
    }

    /**
     * Default exponential backoff - equivalent to exponentialBackoff(60, 250,
     * 25000) to do exponentially increasing increments from 60 to 25000, and
     * stabilizing there.
     *
     * @return A function
     */
    public static IntToLongFunction exponentialBackoff() {
        return Backoff.exponentialBackoff(60, 250, 25000);
    }

    /**
     * Default exponential backoff - equivalent to linearBackoff(60, 250,
     * 25000) to do linearly increasing increments from 60 to 25000, and
     * stabilizing there.
     *
     * @return A function
     */
    public static IntToLongFunction linearBackoff() {
        return Backoff.linearBackoff(60, 250, 25000);
    }

    /**
     * Default cliff backoff - apply the default exponentialBackoff for
     * 20 attempts, then fall back to a delay of 60000.
     *
     * @return A function
     */
    public static IntToLongFunction cliffBackoff() {
        return cliffBackoff(20, 60000);
    }

    public static IntToLongFunction steppedBackoff(int iterations, int... steps) {
        Checks.greaterThanZero("iterations", iterations);
        Checks.notNull("steps", steps);
        Checks.greaterThanZero("steps.length", steps.length);
        return (iter) -> {
            int step = iter / iterations;
            if (step >= steps.length) {
                step = steps[steps.length-1];
            }
            return steps[step];
        };
    }

    static IntToLongFunction cliffBackoff(int cliffIteration, long postCliffDelay) {
        return (iter) -> {
            if (iter < cliffIteration) {
                return exponentialBackoff().applyAsLong(iter);
            } else {
                return postCliffDelay + ThreadLocalRandom.current().nextLong(10000);
            }
        };
    }

    public static IntToLongFunction linearBackoff(long baseRetryInterval, int jitterRange, long maxDelay) {
        return (ct) -> {
            long jitter = jitterRange == 0 ? 0 : ThreadLocalRandom.current().nextInt(jitterRange);
            long interval = Math.min(maxDelay, baseRetryInterval * ct);
            return Math.max(10, (interval - (jitter / 2)) + jitter);
        };
    }

    public static IntToLongFunction constantDelay(long delay) {
        return (ct) -> {
            return delay;
        };
    }

    /**
     * Performs backoff where the thing to be retried completes asynchronously.
     *
     * @param <T> The type of object that indicates success
     * @param starter A consumer which triggers starting or creating the thing
     * in question (typically a network connection), and calls the BiConsumer it
     * gets passed with either an object or a Throwable to indicate failure
     * @param onStart The consumer which should be called only on success or the
     * exhaustion of the maximum number of tries
     * @param svc The executor service to schedule backed off attempts in
     */
    public <T> void asyncBackoff(Consumer<BiConsumer<Throwable, T>> starter, BiConsumer<Throwable, T> onStart, ScheduledExecutorService svc) {
        svc.submit(new Async(starter, onStart, svc));
    }

    final class Async<T> implements Runnable, BiConsumer<Throwable, T> {

        private final Consumer<BiConsumer<Throwable, T>> starter;
        private final BiConsumer<Throwable, T> onStart;
        private final ScheduledExecutorService runIn;
        private int trial = 0;

        public Async(Consumer<BiConsumer<Throwable, T>> starter, BiConsumer<Throwable, T> onStart, ScheduledExecutorService runIn) {
            this.starter = starter;
            this.onStart = onStart;
            this.runIn = runIn;
        }

        @Override
        public void run() {
            starter.accept(this);
        }

        @Override
        public void accept(Throwable t, T u) {
            if (t != null) {
                long delayMs = backoffFunction.applyAsLong(trial++);
                if (trial >= maxRetries) {
                    onStart.accept(t, u);
                    return;
                }
                runIn.schedule(this, delayMs, TimeUnit.MILLISECONDS);
            } else {
                onStart.accept(t, u);
            }
        }
    }

    public <T> void backoff(ThrowingProducer<T> producer, BiConsumer<Throwable, T> callback, Executor runIn) {
        runIn.execute(() -> {
            try {
                callback.accept(null, backoff(producer));
            } catch (Exception ex) {
                callback.accept(ex, null);
            }
        });
    }

    /**
     * Synchronous backoff - will use Thread.sleep() to produce delays as
     * needed.
     *
     * @param <T> The type of object produced (typically a network connection)
     * @param producer The producer of that object
     * @return An object
     * @throws Exception If the maximum tries are exhausted without success
     */
    public <T> T backoff(ThrowingProducer<T> producer) throws Exception {
        T result = null;
        for (int trial = 1; trial <= maxRetries;) {
            try {
                result = producer.get(trial);
                break;
            } catch (Exception e) {
                if (trial == maxRetries || !producer.shouldRetry(e)) {
                    throw e;
                }
                long delay = backoffFunction.applyAsLong(trial);
                if (delay < 0) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex2) {
                        if (!producer.shouldRetry(ex2)) {
                            e.addSuppressed(ex2);
                            throw e;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Producer for synchronous backoff execution.
     *
     * @param <T> The type of object to create
     */
    @FunctionalInterface
    public interface ThrowingProducer<T> {

        /**
         * Get the object
         *
         * @param iteration The current attempt number
         * @return An object
         * @throws Exception If production fails
         */
        T get(int iteration) throws Exception;

        /**
         * Optional method to abort on some exception types
         *
         * @param ex The exception
         * @return True if iteration should continue
         */
        default boolean shouldRetry(Exception ex) {
            return true;
        }
    }
}
