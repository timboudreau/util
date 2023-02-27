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
package com.mastfrog.concurrent;

import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.state.Bool;
import java.util.function.IntConsumer;

/**
 * Atomically updates minimum and maximum values.
 *
 * @author Tim Boudreau
 */
public class AtomicMinMax implements IntConsumer {

    private final AtomicIntegerPair pair = new AtomicIntegerPair(Integer.MAX_VALUE, Integer.MIN_VALUE);

    public AtomicMinMax() {

    }

    public AtomicMinMax(int initialValue) {
        pair.set(initialValue, initialValue);
    }

    public AtomicMinMax reset() {
        pair.set(Integer.MAX_VALUE, Integer.MIN_VALUE);;
        return this;
    }

    public boolean isSet() {
        return get((_ignored1, _ignored2) -> {
        });
    }

    public void accept(int newValue) {
        pair.update(oldMin -> Math.min(oldMin, newValue), oldMax -> Math.max(oldMax, newValue));
    }

    public boolean get(IntBiConsumer minMax) {
        Bool called = Bool.create();
        pair.fetch((min, max) -> {
            if (min <= max) {
                called.set();
                minMax.accept(min, max);
            }
        });
        return called.getAsBoolean();
    }

    public int min() {
        return pair.left();
    }

    public int max() {
        return pair.right();
    }

    @Override
    public String toString() {
        return "MinMax" + pair;
    }

    /**
     * Wrap the passed IntConsumer in another which this instance will track
     * minimum and maximums for.
     *
     * @param other Another int consumer
     * @return an int consumer
     */
    public IntConsumer wrap(IntConsumer other) {
        return val -> {
            accept(val);
            other.accept(val);
        };
    }
}
