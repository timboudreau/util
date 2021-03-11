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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
final class IntAtomic implements Int {

    private final AtomicInteger value;

    IntAtomic(AtomicInteger value) {
        this.value = value;
    }

    IntAtomic() {
        this(new AtomicInteger());
    }

    IntAtomic(int val) {
        this(new AtomicInteger(val));
    }

    @Override
    public int apply(IntUnaryOperator op) {
        return value.getAndUpdate(op);
    }

    @Override
    public int apply(int val, IntBinaryOperator op) {
        return value.getAndAccumulate(val, op);
    }

    @Override
    public boolean ifUpdate(int newVal, Runnable r) {
        if (value.getAndSet(newVal) != newVal) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public int decrement(int val) {
        return value.addAndGet(-val);
    }

    @Override
    public Integer get() {
        return value.get();
    }

    @Override
    public void accept(int value) {
        this.value.set(value);
    }

    @Override
    @SuppressWarnings("UnnecessaryUnboxing")
    public void accept(Integer value) {
        this.value.set(value.intValue());
    }

    @Override
    public boolean ifNotEqual(int value, Runnable r) {
        if (getAsInt() != value) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public int increment() {
        return value.getAndIncrement();
    }

    @Override
    public int increment(int val) {
        return value.getAndAdd(val);
    }

    @Override
    public Int reset() {
        value.set(0);
        return this;
    }

    @Override
    public int set(int val) {
        return value.getAndSet(val);
    }

    @Override
    public int min(int min) {
        return value.getAndAccumulate(min, (a, b) -> {
            return Math.min(a, b);
        });
    }

    @Override
    public int max(int max) {
        return value.getAndAccumulate(max, (a, b) -> {
            return Math.max(a, b);
        });
    }

    @Override
    public IntConsumer summer() {
        return value::getAndAdd;
    }

    @Override
    public boolean equals(int val) {
        return value.get() == val;
    }

    @Override
    public int getAsInt() {
        return value.get();
    }
}
