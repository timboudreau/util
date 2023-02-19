/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent.stats;

import com.mastfrog.function.LongQuadConsumer;
import java.util.LongSummaryStatistics;
import java.util.function.LongConsumer;

/**
 * Implementation of LongStatisticCollector over a collection of int statistics
 * treated as unsigned long.
 *
 * @author Tim Boudreau
 */
final class ConcurrentUnsignedIntStats implements LongConsumer, LongStatisticCollector {

    private final ConcurrentIntegerStats delegate;
    private final long MAX = (long) Integer.MAX_VALUE * 2L;
    private volatile boolean hasOutOfBoundsValues;

    public ConcurrentUnsignedIntStats(ConcurrentIntegerStats delegate) {
        this.delegate = delegate;
    }

    @Override
    public void accept(long value) {
        if (value > MAX || value < 0) {
            boolean old = hasOutOfBoundsValues;
            hasOutOfBoundsValues = true;
            if (!old) {
                new IllegalArgumentException("Received out-of-bounds value "
                        + value + ".  This will only be logged on the first "
                        + "occurrence.")
                        .printStackTrace(System.err);
            }
            return;
        }
        delegate.accept(toInt(value));
    }

    public boolean hasReceivedOutOfBoundsValues() {
        return hasOutOfBoundsValues;
    }

    @Override
    public LongSummaryStatistics toStatistics() {
        LongSummaryStatistics result = new LongSummaryStatistics();
        delegate.forEach(intValue -> result.accept(toLong(intValue)));
        return result;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public int capacity() {
        return delegate.capacity();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public double average() {
        return toStatistics().getAverage();
    }

    @Override
    public int forEach(LongConsumer consumer) {
        return delegate.forEach(intValue -> consumer.accept(intValue));
    }

    @Override
    public boolean withStats(LongQuadConsumer con) {
        return delegate.withStats((a, b, c, d) -> {
            con.accept(toLong(a), b, c, d);
        });
    }

    @Override
    public boolean withStatsAndValues(LongConsumer valueVisitor, LongQuadConsumer statsConsumer) {
        return delegate.withStatsAndValues(val -> valueVisitor.accept(toLong(val)),
                (a, b, c, d) -> statsConsumer.accept(toLong(a), toLong(b), toLong(c), toLong(d)));
    }

    @Override
    public boolean median(LongConsumer c) {
        return delegate.median(med -> c.accept(toLong(med)));
    }

    @Override
    public void accept(int value) {
        delegate.accept(toInt(value));
    }

    private static long toLong(int val) {
        return val & -1;
    }

    private int toInt(long val) {
        return (int) val;
    }

    // for tests
    int addValue(long val) {
        return delegate.addValue(toInt(val));
    }

    int sampleCount() {
        return delegate.sampleCount();
    }
}
