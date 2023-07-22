/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
package com.mastfrog.bits;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class SynchronizedMutableBits implements MutableBits {

    private final MutableBits delegate;

    SynchronizedMutableBits(MutableBits delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<Characteristics> characteristics() {
        Set<Characteristics> info = EnumSet.copyOf(delegate.characteristics());
        info.add(Characteristics.THREAD_SAFE);
        return info;
    }

    @Override
    public MutableBits newBits(int size) {
        return new SynchronizedMutableBits(delegate.newBits(size));
    }

    @Override
    public MutableBits newBits(long size) {
        return new SynchronizedMutableBits(delegate.newBits(size));
    }

    @Override
    public MutableBits toSynchronizedBits() {
        return this;
    }

    @Override
    public synchronized void set(int bitIndex, boolean value) {
        delegate.set(bitIndex, value);
    }

    @Override
    public synchronized Bits readOnlyView() {
        return this;
    }

    @Override
    public synchronized MutableBits orWith(Bits other) {
        return delegate.orWith(other);
    }

    @Override
    public synchronized MutableBits xorWith(Bits other) {
        return delegate.xorWith(other);
    }

    @Override
    public synchronized MutableBits andWith(Bits other) {
        return delegate.andWith(other);
    }

    @Override
    public synchronized MutableBits andNotWith(Bits other) {
        return delegate.andNotWith(other);
    }

    @Override
    public synchronized MutableBits get(int fromIndex, int toIndex) {
        return delegate.get(fromIndex, toIndex);
    }

    @Override
    public synchronized void set(long bitIndex, boolean value) {
        delegate.set(bitIndex, value);
    }

    @Override
    public synchronized void and(Bits set) {
        delegate.and(set);
    }

    @Override
    public synchronized void andNot(Bits set) {
        delegate.andNot(set);
    }

    @Override
    public synchronized void or(Bits set) {
        delegate.or(set);
    }

    @Override
    public synchronized void xor(Bits set) {
        delegate.xor(set);
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized void clear(int bitIndex) {
        delegate.clear(bitIndex);
    }

    @Override
    public synchronized void clear(int fromIndex, int toIndex) {
        delegate.clear(fromIndex, toIndex);
    }

    @Override
    public synchronized void flip(int bitIndex) {
        delegate.flip(bitIndex);
    }

    @Override
    public synchronized void flip(int fromIndex, int toIndex) {
        delegate.flip(fromIndex, toIndex);
    }

    @Override
    public synchronized void set(int bitIndex) {
        delegate.set(bitIndex);
    }

    @Override
    public synchronized void set(int fromIndex, int toIndex) {
        delegate.set(fromIndex, toIndex);
    }

    @Override
    public synchronized void set(int fromIndex, int toIndex, boolean value) {
        delegate.set(fromIndex, toIndex, value);
    }

    @Override
    public synchronized void clear(long bitIndex) {
        delegate.clear(bitIndex);
    }

    @Override
    public synchronized void clear(long fromIndex, long toIndex) {
        delegate.clear(fromIndex, toIndex);
    }

    @Override
    public synchronized void flip(long bitIndex) {
        delegate.flip(bitIndex);
    }

    @Override
    public synchronized void flip(long fromIndex, long toIndex) {
        delegate.flip(fromIndex, toIndex);
    }

    @Override
    public synchronized void set(long bitIndex) {
        delegate.set(bitIndex);
    }

    @Override
    public synchronized void set(long fromIndex, long toIndex) {
        delegate.set(fromIndex, toIndex);
    }

    @Override
    public synchronized void set(long fromIndex, long toIndex, boolean value) {
        delegate.set(fromIndex, toIndex, value);
    }

    @Override
    public synchronized int cardinality() {
        return delegate.cardinality();
    }

    @Override
    public synchronized Bits copy() {
        return delegate.copy();
    }

    @Override
    public synchronized MutableBits mutableCopy() {
        return delegate.mutableCopy();
    }

    @Override
    public synchronized BitSet toBitSet() {
        return delegate.toBitSet();
    }

    @Override
    public synchronized double sum(IntToDoubleFunction f) {
        return delegate.sum(f);
    }

    @Override
    public synchronized double sum(double[] values, int ifNot) {
        return delegate.sum(values, ifNot);
    }

    @Override
    public synchronized double sum(DoubleLongFunction f) {
        return delegate.sum(f);
    }

    @Override
    public synchronized boolean isNativelyLongIndexed() {
        return delegate.isNativelyLongIndexed();
    }

    @Override
    public synchronized Bits immutableCopy() {
        return delegate.immutableCopy();
    }

    @Override
    public synchronized long cardinalityLong() {
        return delegate.cardinalityLong();
    }

    @Override
    public synchronized boolean get(int bitIndex) {
        return delegate.get(bitIndex);
    }

    @Override
    public synchronized boolean get(long bitIndex) {
        return delegate.get(bitIndex);
    }

    @Override
    public synchronized Bits get(long fromIndex, long toIndex) {
        return delegate.get(fromIndex, toIndex);
    }

    @Override
    public synchronized boolean intersects(Bits set) {
        return delegate.intersects(set);
    }

    @Override
    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public synchronized int length() {
        return delegate.length();
    }

    @Override
    public synchronized long longLength() {
        return delegate.longLength();
    }

    @Override
    public synchronized int nextClearBit(int fromIndex) {
        return delegate.nextClearBit(fromIndex);
    }

    @Override
    public synchronized int nextSetBit(int fromIndex) {
        return delegate.nextSetBit(fromIndex);
    }

    @Override
    public synchronized int previousClearBit(int fromIndex) {
        return delegate.previousClearBit(fromIndex);
    }

    @Override
    public synchronized int previousSetBit(int fromIndex) {
        return delegate.previousSetBit(fromIndex);
    }

    @Override
    public synchronized String stringValue() {
        return delegate.stringValue();
    }

    @Override
    public synchronized long previousSetBitLong(long fromIndex) {
        return delegate.previousSetBitLong(fromIndex);
    }

    @Override
    public synchronized long previousClearBitLong(long fromIndex) {
        return delegate.previousClearBitLong(fromIndex);
    }

    @Override
    public synchronized long nextSetBitLong(long fromIndex) {
        return delegate.nextSetBitLong(fromIndex);
    }

    @Override
    public synchronized long nextClearBitLong(long fromIndex) {
        return delegate.nextClearBitLong(fromIndex);
    }

    @Override
    public synchronized boolean contentEquals(Bits other) {
        return delegate.contentEquals(other);
    }

    @Override
    public synchronized int bitsHashCode() {
        return delegate.bitsHashCode();
    }

    @Override
    public synchronized long[] toLongArray() {
        return delegate.toLongArray();
    }

    @Override
    public synchronized byte[] toByteArray() {
        return delegate.toByteArray();
    }

    @Override
    public synchronized int forEachSetBitAscending(IntConsumer consumer) {
        return delegate.forEachSetBitAscending(consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(IntConsumer consumer) {
        return delegate.forEachSetBitDescending(consumer);
    }

    @Override
    public synchronized int forEachSetBitAscending(IntPredicate consumer) {
        return delegate.forEachSetBitAscending(consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(IntPredicate consumer) {
        return delegate.forEachSetBitDescending(consumer);
    }

    @Override
    public synchronized int forEachSetBitAscending(int from, IntConsumer consumer) {
        return delegate.forEachSetBitAscending(from, consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(int from, IntConsumer consumer) {
        return delegate.forEachSetBitDescending(from, consumer);
    }

    @Override
    public synchronized int forEachSetBitAscending(int start, IntPredicate consumer) {
        return delegate.forEachSetBitAscending(start, consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(int start, IntPredicate consumer) {
        return delegate.forEachSetBitDescending(start, consumer);
    }

    @Override
    public synchronized int forEachSetBitAscending(int from, int upTo, IntConsumer consumer) {
        return delegate.forEachSetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(int from, int downTo, IntConsumer consumer) {
        return delegate.forEachSetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized int forEachSetBitAscending(int from, int upTo, IntPredicate consumer) {
        return delegate.forEachSetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized int forEachSetBitDescending(int from, int downTo, IntPredicate consumer) {
        return delegate.forEachSetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(IntConsumer consumer) {
        return delegate.forEachUnsetBitAscending(consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(IntConsumer consumer) {
        return delegate.forEachUnsetBitDescending(consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(IntPredicate consumer) {
        return delegate.forEachUnsetBitAscending(consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(IntPredicate consumer) {
        return delegate.forEachUnsetBitDescending(consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(int from, IntConsumer consumer) {
        return delegate.forEachUnsetBitAscending(from, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(int from, IntConsumer consumer) {
        return delegate.forEachUnsetBitDescending(from, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(int start, IntPredicate consumer) {
        return delegate.forEachUnsetBitAscending(start, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(int start, IntPredicate consumer) {
        return delegate.forEachUnsetBitDescending(start, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(int from, int upTo, IntConsumer consumer) {
        return delegate.forEachUnsetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(int from, int downTo, IntConsumer consumer) {
        return delegate.forEachUnsetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitAscending(int from, int upTo, IntPredicate consumer) {
        return delegate.forEachUnsetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized int forEachUnsetBitDescending(int from, int downTo, IntPredicate consumer) {
        return delegate.forEachUnsetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized int min() {
        return delegate.min();
    }

    @Override
    public synchronized int max() {
        return delegate.max();
    }

    @Override
    public synchronized long minLong() {
        return delegate.minLong();
    }

    @Override
    public synchronized long maxLong() {
        return delegate.maxLong();
    }

    @Override
    public synchronized long forEachLongSetBitAscending(LongConsumer consumer) {
        return delegate.forEachLongSetBitAscending(consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(LongConsumer consumer) {
        return delegate.forEachLongSetBitDescending(consumer);
    }

    @Override
    public synchronized long forEachLongSetBitAscending(LongPredicate consumer) {
        return delegate.forEachLongSetBitAscending(consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(LongPredicate consumer) {
        return delegate.forEachLongSetBitDescending(consumer);
    }

    @Override
    public synchronized long forEachLongSetBitAscending(long from, LongConsumer consumer) {
        return delegate.forEachLongSetBitAscending(from, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(long from, LongConsumer consumer) {
        return delegate.forEachLongSetBitDescending(from, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitAscending(long start, LongPredicate consumer) {
        return delegate.forEachLongSetBitAscending(start, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(long start, LongPredicate consumer) {
        return delegate.forEachLongSetBitDescending(start, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitAscending(long from, long upTo, LongConsumer consumer) {
        return delegate.forEachLongSetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(long from, long downTo, LongConsumer consumer) {
        return delegate.forEachLongSetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitAscending(long from, long upTo, LongPredicate consumer) {
        return delegate.forEachLongSetBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized long forEachLongSetBitDescending(long from, long downTo, LongPredicate consumer) {
        return delegate.forEachLongSetBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(LongConsumer consumer) {
        return delegate.forEachUnsetLongBitAscending(consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(LongConsumer consumer) {
        return delegate.forEachUnsetLongBitDescending(consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(LongPredicate consumer) {
        return delegate.forEachUnsetLongBitAscending(consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(LongPredicate consumer) {
        return delegate.forEachUnsetLongBitDescending(consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(long from, LongConsumer consumer) {
        return delegate.forEachUnsetLongBitAscending(from, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(long from, LongConsumer consumer) {
        return delegate.forEachUnsetLongBitDescending(from, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(long start, LongPredicate consumer) {
        return delegate.forEachUnsetLongBitAscending(start, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(long start, LongPredicate consumer) {
        return delegate.forEachUnsetLongBitDescending(start, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(long from, long upTo, LongConsumer consumer) {
        return delegate.forEachUnsetLongBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(long from, long downTo, LongConsumer consumer) {
        return delegate.forEachUnsetLongBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitAscending(long from, long upTo, LongPredicate consumer) {
        return delegate.forEachUnsetLongBitAscending(from, upTo, consumer);
    }

    @Override
    public synchronized long forEachUnsetLongBitDescending(long from, long downTo, LongPredicate consumer) {
        return delegate.forEachUnsetLongBitDescending(from, downTo, consumer);
    }

    @Override
    public synchronized Bits shift(int by) {
        return delegate.shift(by);
    }

    @Override
    public synchronized Bits filter(IntPredicate pred) {
        return delegate.filter(pred);
    }

    @Override
    public synchronized <M extends MutableBits> M filter(IntFunction<M> factory, IntPredicate pred) {
        return delegate.filter(factory, pred);
    }

    @Override
    public synchronized int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public synchronized int leastSetBit() {
        return delegate.leastSetBit();
    }

    @Override
    public synchronized long leastSetBitLong() {
        return delegate.leastSetBitLong();
    }
}
