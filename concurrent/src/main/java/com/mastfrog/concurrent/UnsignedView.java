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
 * A view over an IntegerPair which converts values to unsigned longs from zero
 * to Integer.MAX_VALUE * 2.
 *
 * @author Tim Boudreau
 */
public interface UnsignedView {

    static final long MAX_VALUE = (long) Integer.MAX_VALUE * 2L;

    boolean compareAndSet(long expectedLeftValue, long expectedRightValue, long newLeftValue, long newRightValue);

    long left();

    long right();

    void set(long left, long right);

    void setLeft(long newLeft);

    void setRight(long newRight);

    long toLong();

    void update(LongUnaryOperator leftFunction, LongUnaryOperator rightFunction);

    void fetch(LongBiConsumer consumer);

    void update(UnsignedPairUpdater updater);

    interface UnsignedPairUpdater {

        void update(long left, long right, LongBiConsumer update);
    }
}
