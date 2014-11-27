/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.IntBinaryOperator;

/**
 * Thread-safe, lockless AtomicInteger-like object whose value increments from 0
 * to maximum-1 and returns to 0.
 *
 * @author Tim Boudreau
 */
public final class AtomicRoundRobin {

    private final int maximum;
    private volatile int currentValue;
    private final AtomicIntegerFieldUpdater<AtomicRoundRobin> up;

    public AtomicRoundRobin(int maximum) {
        if (maximum <= 0) {
            throw new IllegalArgumentException("Maximum must be > 0");
        }
        this.maximum = maximum;
        up = AtomicIntegerFieldUpdater.newUpdater(AtomicRoundRobin.class, "currentValue");
    }

    /**
     * Get the maximum possible value
     *
     * @return The maximum
     */
    public int maximum() {
        return maximum;
    }

    /**
     * Get the current value
     *
     * @return
     */
    public int get() {
        return currentValue;
    }

    /**
     * Get the next value, incrementing the value or resetting it to zero for
     * the next caller.
     *
     * @return The value
     */
    public int next() {
        if (maximum == 1) {
            return 0;
        }
        return up.getAndAccumulate(this, currentValue, (i1, i2) -> {
            if (i1 == maximum - 1) {
                return 0;
            }
            return i1 + 1;
        });
    }
}
