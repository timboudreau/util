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

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
final class DoubleMapEmpty implements DoubleMap<Object> {

    private static final DoubleMapEmpty INSTANCE = new DoubleMapEmpty();

    @SuppressWarnings("unchecked")
    static <T> DoubleMap<T> coerce() {
        return (DoubleMap<T>) INSTANCE;
    }

    @Override
    public void put(double key, Object value) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public int valuesBetween(double a, double b, DoubleMapConsumer<? super Object> c) {
        return 0;
    }

    @Override
    public void putAll(DoubleMap<Object> map) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public Object setValueAt(int index, Object value) {
        throw new UnsupportedOperationException("Empty instance");
    }


    @Override
    public Object get(double key) {
        return null;
    }

    @Override
    public Object getOrDefault(double key, Object defaultResult) {
        return defaultResult;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean containsKey(double d) {
        return false;
    }

    @Override
    public DoubleSet keySet() {
        return DoubleSet.emptyDoubleSet();
    }

    @Override
    public double key(int index) {
        return Double.MIN_VALUE;
    }

    @Override
    public Object valueAt(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public List<Object> values() {
        return Collections.emptyList();
    }

    @Override
    public int indexOf(double key) {
        return -1;
    }

    @Override
    public void removeIndex(int index) {
        throw new UnsupportedOperationException("Read only.");
    }

    @Override
    public void removeIndices(IntSet indices) {
        throw new UnsupportedOperationException("Read only.");
    }

    @Override
    public boolean nearestValueExclusive(double approximate, double tolerance, DoubleMapConsumer c) {
        return false;
    }

    @Override
    public boolean nearestValueExclusive(double approximate, DoubleMapConsumer c) {
        return false;
    }

    @Override
    public boolean nearestValueTo(double approximate, DoubleMapConsumer c) {
        return false;
    }

    @Override
    public boolean nearestValueTo(double approximate, double tolerance, DoubleMapConsumer c) {
        return false;
    }

    @Override
    public boolean greatest(DoubleMapConsumer c) {
        return false;
    }

    @Override
    public boolean least(DoubleMapConsumer c) {
        return false;
    }

    @Override
    public void forEach(DoubleMapConsumer c) {
        // do nothing
    }

    @Override
    public int removeRange(double start, double end) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public boolean remove(double key) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void removeAll(double... keys) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void removeAll(DoubleSet set) {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Empty instance");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DoubleMap) {
            return ((DoubleMap) o).isEmpty();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 5;
    }

    public String toString() {
        return "{}";
    }

}
