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
package com.mastfrog.util;

import com.mastfrog.util.thread.Callback;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToLongFunction;

/**
 * Implementation of exponential (or whatever) backoff strategy, with the ability to repeatedly call a producer until it
 * succeeds or max retries is met.
 *
 * @author Tim Boudreau
 */
public class Backoff {

    private final IntToLongFunction backoffFunction;
    private final int maxRetries;
    public static Backoff EXPONENTIAL_BACKOFF = new Backoff();
    public static Backoff LINEAR_BACKOFF = new Backoff( linearBackoff() );

    public Backoff(IntToLongFunction backoffFunction, int maxRetries) {
        this.backoffFunction = backoffFunction;
        this.maxRetries = maxRetries;
    }

    public Backoff(IntToLongFunction backoffFunction) {
        this( backoffFunction, Integer.MAX_VALUE );
    }

    public Backoff() {
        this( exponentialBackoff() );
    }

    public void block(int iteration) throws InterruptedException {
        Thread.sleep( backoffFunction.applyAsLong( iteration ) );
    }

    public static IntToLongFunction exponentialBackoff(long baseRetryInterval, int jitterRange, long maxDelay) {
        return (ct) -> {
            long jitter = ThreadLocalRandom.current().nextInt( jitterRange );
            long interval = Math.min( 15000, baseRetryInterval * ( ct * ct ) );
            return Math.max( 10, ( interval - ( jitter / 2 ) ) + jitter );
        };
    }

    public static IntToLongFunction exponentialBackoff() {
        return Backoff.exponentialBackoff( 60, 250, 25000 );
    }

    public static IntToLongFunction linearBackoff() {
        return Backoff.linearBackoff( 60, 250, 25000 );
    }

    public static IntToLongFunction cliffBackoff() {
        return cliffBackoff(2000, 60000);
    }

    static IntToLongFunction cliffBackoff(int cliffIteration, long postCliffDelay) {
        return (iter) -> {
            if (iter < cliffIteration) {
                return exponentialBackoff().applyAsLong( iter );
            } else {
                return 60000 + ThreadLocalRandom.current().nextLong( 10000);
            }
        };
    }

    public static IntToLongFunction linearBackoff(long baseRetryInterval, int jitterRange, long maxDelay) {
        return (ct) -> {
            long jitter = ThreadLocalRandom.current().nextInt( jitterRange );
            long interval = Math.min( 15000, baseRetryInterval * ct );
            return Math.max( 10, ( interval - ( jitter / 2 ) ) + jitter );
        };
    }

    public static IntToLongFunction constantDelay(long delay) {
        return (ct) -> {
            return delay;
        };
    }

    public <T> void backoff(ThrowingProducer<T> producer, Callback<T> callback, Executor runIn) {
        runIn.execute( () -> {
            try {
                callback.receive( null, backoff( producer ) );
            } catch ( Exception ex ) {
                callback.receive( ex, null );
            }
        } );
    }

    public <T> T backoff(ThrowingProducer<T> producer) throws Exception {
        T result = null;
        for ( int trial = 1; trial <= maxRetries; ) {
            try {
                result = producer.get( trial );
                break;
            } catch ( Exception e ) {
                if ( trial == maxRetries || !producer.shouldRetry( e ) ) {
                    throw e;
                }
                long delay = backoffFunction.applyAsLong( trial );
                if ( delay < 0 ) {
                    throw e;
                } else {
                    try {
                        Thread.sleep( delay );
                    } catch ( InterruptedException ex2 ) {
                        if ( !producer.shouldRetry( ex2 ) ) {
                            e.addSuppressed( ex2 );
                            throw e;
                        }
                    }
                }
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface ThrowingProducer<T> {

        T get(int iteration) throws Exception;

        default boolean shouldRetry(Exception ex) {
            return true;
        }
    }
}
