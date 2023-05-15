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

import static com.mastfrog.concurrent.AtomicIntegerPair.unpackLeft;
import static com.mastfrog.concurrent.AtomicIntegerPair.unpackRight;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntBiPredicate;
import java.util.function.IntUnaryOperator;

/**
 * A pair of integers which can be operated on atomically.
 *
 * @author Tim Boudreau
 */
public interface IntegerPair {

    boolean compareAndSet(int expectedLeftValue, int expectedRightValue,
            int newLeftValue, int newRightValue);

    void fetch(IntBiConsumer pair);

    int left();

    int right();

    void set(int left, int right);

    void setLeft(int newLeft);

    void setRight(int newRight);

    long toLong();

    void update(IntUnaryOperator leftFunction, IntUnaryOperator rightFunction);

    void update(IntegerPairUpdater pairUpdater);

    default boolean evaluate(IntBiPredicate test) {
        long v = toLong();
        int left = unpackLeft(v);
        int right = unpackRight(v);
        return test.test(left, right);
    }

    default void updateLeft(IntUnaryOperator op) {
        update(op, x -> x);
    }

    default void updateRight(IntUnaryOperator op) {
        update(x -> x, op);
    }

    int getAndUpdateLeft(IntUnaryOperator op);

    int getAndUpdateRight(IntUnaryOperator op);

    int updateAndGetLeft(IntUnaryOperator op);

    int updateAndGetRight(IntUnaryOperator op);

    default UnsignedView toUnsignedView() {
        return new UnsignedViewImpl(this);
    }

    public static IntegerPair createAtomic() {
        return new AtomicIntegerPair();
    }

    public static IntegerPair createAtomic(int left, int right) {
        return new AtomicIntegerPair(left, right);
    }

    public static IntegerPair createAtomic(long combined) {
        return new AtomicIntegerPair(combined);
    }

    public interface IntegerPairUpdater {

        void update(int left, int right, IntBiConsumer update);
    }
}
