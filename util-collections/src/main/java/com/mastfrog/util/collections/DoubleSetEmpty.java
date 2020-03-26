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

package com.mastfrog.util.collections;

import com.mastfrog.util.search.Bias;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 *
 * @author Tim Boudreau
 */
final class DoubleSetEmpty implements DoubleSet {

    public static DoubleSetEmpty INSTANCE = new DoubleSetEmpty();
    private DoubleSetEmpty() {

    }

    @Override
    public DoubleSet copy() {
        return this;
    }

    @Override
    public void add(double value) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void addAll(DoubleSet set) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public boolean contains(double d) {
        return false;
    }

    @Override
    public void forEachDouble(DoubleConsumer dc) {
        // do nothing
    }

    @Override
    public void forEachReversed(DoubleConsumer dc) {
        // do nothing
    }

    @Override
    public double getAsDouble(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public double greatest() {
        return Double.MIN_VALUE;
    }

    @Override
    public int indexOf(double d) {
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public PrimitiveIterator.OfDouble iterator() {
        return new PrimitiveIterator.OfDouble() {
            @Override
            public double nextDouble() {
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasNext() {
                return false;
            }
        };
    }

    @Override
    public double least() {
        return Double.MIN_VALUE;
    }

    @Override
    public int nearestIndexTo(double approximateValue, Bias bias) {
        return -1;
    }

    @Override
    public double nearestValueTo(double approximateValue, double tolerance) {
        return Double.MIN_VALUE;
    }

    @Override
    public double nearestValueExclusive(double approximateValue) {
        return Double.MIN_VALUE;
    }

    @Override
    public double nearestValueExclusive(double approximateValue, double tolerance) {
        return Double.MIN_VALUE;
    }

    @Override
    public double nearestValueTo(double approximateValue) {
        return Double.MIN_VALUE;
    }

    @Override
    public DoubleSet[] partition(int maxPartitions) {
        return new DoubleSet[] {this};
    }

    @Override
    public double range() {
        return 0;
    }

    @Override
    public void removeAll(DoubleSet remove) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void retainAll(DoubleSet retain) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public double[] toDoubleArray() {
        return new double[0];
    }

    @Override
    public DoubleSet unmodifiableView() {
        return this;
    }

    @Override
    public DoubleSet toReadOnlyCopy() {
        return this;
    }

    @Override
    public int removeRange(double least, double greatest) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public boolean remove(double key) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public DoubleSet subset(double least, double greatest) {
        return this;
    }

    @Override
    public DoubleSet toSynchronizedSet() {
        return this;
    }

    @Override
    public void forEach(Consumer<? super Double> action) {
        // do nothing
    }

    @Override
    public int hashCode() {
        return 5;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else {
            return o instanceof DoubleSet && ((DoubleSet) o).isEmpty();
        }
    }

    @Override
    public String toString() {
        return "{}";
    }
}
