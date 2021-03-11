/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.function.state;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
final class LngAtomic implements Lng {

    private final AtomicLong value;

    LngAtomic(AtomicLong value) {
        this.value = value;
    }

    LngAtomic() {
        this(new AtomicLong());
    }

    LngAtomic(long val) {
        this(new AtomicLong(val));
    }

    @Override
    public long apply(LongUnaryOperator op) {
        return value.getAndUpdate(op);
    }

    @Override
    public long apply(long val, LongBinaryOperator op) {
        return value.getAndAccumulate(val, op);
    }

    @Override
    public boolean ifUpdate(long newVal, Runnable r) {
        if (value.getAndSet(newVal) != newVal) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public long decrement(long val) {
        return value.addAndGet(-val);
    }

    @Override
    public Long get() {
        return value.get();
    }

    @Override
    public void accept(long value) {
        this.value.set(value);
    }

    @Override
    @SuppressWarnings("UnnecessaryUnboxing")
    public void accept(Long value) {
        this.value.set(value.longValue());
    }

    @Override
    public boolean ifNotEqual(long value, Runnable r) {
        if (getAsLong() != value) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public long increment() {
        return value.getAndIncrement();
    }

    @Override
    public long increment(long val) {
        return value.getAndAdd(val);
    }

    @Override
    public Lng reset() {
        value.set(0);
        return this;
    }

    @Override
    public long set(long val) {
        return value.getAndSet(val);
    }

    @Override
    public long min(long min) {
        return value.getAndAccumulate(min, (a, b) -> {
            return Math.min(a, b);
        });
    }

    @Override
    public long max(long max) {
        return value.getAndAccumulate(max, (a, b) -> {
            return Math.max(a, b);
        });
    }

    @Override
    public LongConsumer summer() {
        return value::getAndAdd;
    }

    @Override
    public boolean equals(long val) {
        return value.get() == val;
    }

    @Override
    public long getAsLong() {
        return value.get();
    }
}
