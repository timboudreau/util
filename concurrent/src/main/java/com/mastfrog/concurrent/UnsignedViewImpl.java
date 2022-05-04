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

import com.mastfrog.function.LongBiConsumer;
import java.util.function.LongUnaryOperator;

/**
 * Wraps an IntegerPair to returned unsigned 32-bit values.
 *
 * @author Tim Boudreau
 */
final class UnsignedViewImpl implements UnsignedView {

    private final IntegerPair delegate;

    UnsignedViewImpl(IntegerPair delegate) {
        this.delegate = delegate;
    }

    private int toInt(long value) {
        if (value < 0 || value > MAX_VALUE) {
            throw new IllegalArgumentException("Value out of range 0-" + MAX_VALUE);
        }
        return (int) value;
    }

    private long toLong(int value) {
        long unsigned = value & 0xffffffffL;
        return unsigned;
    }

    @Override
    public boolean compareAndSet(long expectedLeftValue, long expectedRightValue, long newLeftValue, long newRightValue) {
        return delegate.compareAndSet(toInt(expectedLeftValue), toInt(expectedRightValue), toInt(newLeftValue), toInt(newRightValue));
    }

    @Override
    public long left() {
        return toLong(delegate.left());
    }

    @Override
    public long right() {
        return toLong(delegate.right());
    }

    @Override
    public void set(long left, long right) {
        delegate.set(toInt(left), toInt(right));
    }

    @Override
    public void setLeft(long newLeft) {
        delegate.setLeft(toInt(newLeft));
    }

    @Override
    public void setRight(long newRight) {
        delegate.setRight(toInt(newRight));
    }

    @Override
    public long toLong() {
        return delegate.toLong();
    }

    @Override
    public void update(LongUnaryOperator leftFunction, LongUnaryOperator rightFunction) {
        delegate.update(oldLeft -> toInt(leftFunction.applyAsLong(toLong(oldLeft))),
                oldRight -> toInt(rightFunction.applyAsLong(toLong(oldRight))));
    }

    @Override
    public String toString() {
        return "(" + left() + ", " + right() + ")";
    }

    @Override
    public void fetch(LongBiConsumer consumer) {
        delegate.fetch((l, r) -> consumer.accept(toLong(l), toLong(r)));
    }

    @Override
    public void update(UnsignedPairUpdater updater) {
        delegate.update((l, r, updateReceiver) -> {
            updater.update(toLong(l), toLong(r), (a, b) -> {
                updateReceiver.accept(toInt(a), toInt(b));
            });
        });
    }
}
