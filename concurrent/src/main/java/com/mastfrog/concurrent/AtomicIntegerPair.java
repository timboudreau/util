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
import com.mastfrog.function.IntBiPredicate;
import com.mastfrog.function.state.Lng;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.IntUnaryOperator;

/**
 * Pair of integers which can both be updated in a single atomic operation.
 *
 * @author Tim Boudreau
 */
final class AtomicIntegerPair implements IntegerPair {

    private volatile long value;

    private static final AtomicLongFieldUpdater<AtomicIntegerPair> UPD
            = AtomicLongFieldUpdater.newUpdater(AtomicIntegerPair.class, "value");

    AtomicIntegerPair() {

    }

    AtomicIntegerPair(int a, int b) {
        this.value = pack(a, b);
    }

    AtomicIntegerPair(long value) {
        this.value = value;
    }

    @Override
    public long toLong() {
        return value();
    }

    private long value() {
        return UPD.get(this);
    }

    @Override
    public void fetch(IntBiConsumer pair) {
        long val = value();
        pair.accept(unpackLeft(val), unpackRight(val));
    }

    public int[] get() {
        int[] result = new int[2];
        fetch((a, b) -> {
            result[0] = a;
            result[1] = b;
        });
        return result;
    }

    @Override
    public void update(IntUnaryOperator leftFunction, IntUnaryOperator rightFunction) {
        UPD.updateAndGet(this, old -> {
            int left = leftFunction.applyAsInt(unpackLeft(old));
            int right = rightFunction.applyAsInt(unpackRight(old));
            return pack(left, right);
        });
    }

    @Override
    public void update(IntegerPairUpdater updater) {
        Lng val = Lng.create();
        UPD.updateAndGet(this, old -> {
            val.set(old);
            updater.update(unpackLeft(old), unpackRight(old), (newLeft, newRight) -> {
                val.set(pack(newLeft, newRight));
            });
            return val.getAsLong();
        });
    }

    @Override
    public boolean compareAndSet(int expectedLeftValue, int expectedRightValue, int newLeftValue, int newRightValue) {
        long expect = pack(expectedLeftValue, expectedRightValue);
        long nue = pack(newLeftValue, newRightValue);
        return UPD.compareAndSet(this, expect, nue);
    }

    public void swap() {
        UPD.updateAndGet(this, old -> {
            int left = unpackLeft(old);
            int right = unpackRight(old);
            return pack(right, left);
        });
    }

    @Override
    public void set(int left, int right) {
        UPD.set(this, pack(left, right));
    }

    @Override
    public void setLeft(int newLeft) {
        UPD.updateAndGet(this, old -> {
            int left = newLeft;
            int right = unpackRight(old);
            return pack(left, right);
        });
    }

    @Override
    public void setRight(int newRight) {
        UPD.updateAndGet(this, old -> {
            int left = unpackLeft(old);
            int right = newRight;
            return pack(left, right);
        });
    }

    @Override
    public int left() {
        long val = value();
        return (int) (val >> 32);
    }

    @Override
    public int right() {
        return unpackRight(value());
    }

    static long pack(int left, int right) {
        return (((long) left) << 32) | (right & 0xFFFF_FFFFL);
    }

    static int unpackLeft(long value) {
        return (int) ((value >>> 32) & 0xFFFF_FFFFL);
    }

    static int unpackRight(long value) {
        return (int) (value & 0xFFFF_FFFFL);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        fetch((a, b) -> {
            sb.append(a).append(", ").append(b);
        });
        return sb.append(')').toString();
    }

    @Override
    public int getAndUpdateLeft(IntUnaryOperator op) {
        long val = UPD.getAndUpdate(this, old -> {
            int left = op.applyAsInt(unpackLeft(old));
            int right = unpackRight(old);
            return pack(left, right);
        });
        return unpackLeft(val);
    }

    @Override
    public int getAndUpdateRight(IntUnaryOperator op) {
        long val = UPD.getAndUpdate(this, old -> {
            int left = unpackLeft(old);
            int right = op.applyAsInt(unpackRight(old));
            return pack(left, right);
        });
        return unpackRight(val);
    }

    @Override
    public int updateAndGetLeft(IntUnaryOperator op) {
        long val = UPD.updateAndGet(this, old -> {
            int left = op.applyAsInt(unpackLeft(old));
            int right = unpackRight(old);
            return pack(left, right);
        });
        return unpackLeft(val);
    }

    @Override
    public int updateAndGetRight(IntUnaryOperator op) {
        long val = UPD.updateAndGet(this, old -> {
            int left = unpackLeft(old);
            int right = op.applyAsInt(unpackRight(old));
            return pack(left, right);
        });
        return unpackRight(val);
    }

}
