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
import java.util.BitSet;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/**
 *
 * @author Tim Boudreau
 */
final class IntSetReadOnly extends IntSet {

    final IntSet delegate;

    IntSetReadOnly(IntSet other) {
        this.delegate = other;
    }

    @SuppressWarnings("unchecked")
    static <T> Collection<T> unwrap(Collection<T> set) {
        while (set instanceof IntSetReadOnly) {
            set = (Collection<T>) ((IntSetReadOnly) set).delegate;
        }
        return set;
    }

    @Override
    public int indexOf(int value) {
        return delegate.indexOf(value);
    }

    @Override
    public int nearestIndexTo(int value, Bias bias) {
        return delegate.nearestIndexTo(value, bias);
    }

    @Override
    public int nearestValueTo(int value, Bias bias) {
        return delegate.nearestValueTo(value, bias);
    }

    @Override
    BitSet bitsUnsafe() {
        return delegate.bitsUnsafe();
    }

    @Override
    public IntSet readOnlyView() {
        return this;
    }

    @Override
    public IntSet intersection(IntSet other) {
        return delegate.intersection(other);
    }

    @Override
    public IntSet or(IntSet other) {
        return delegate.or(other);
    }

    @Override
    public IntSet xor(IntSet other) {
        return delegate.xor(other);
    }

    @Override
    public IntSet addAll(int... ints) {
        return delegate.addAll(ints);
    }

    @Override
    public BitSet toBits() {
        return delegate.toBits();
    }

    @Override
    public int pick(Random r) {
        return delegate.pick(r);
    }

    @Override
    public boolean sameContents(Set<? extends Integer> other) {
        return delegate.sameContents(other);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void forEach(IntConsumer cons) {
        delegate.forEach(cons);
    }

    @Override
    public void forEachReversed(IntConsumer cons) {
        delegate.forEachReversed(cons);
    }

    @Override
    public int[] toIntArray() {
        return delegate.toIntArray();
    }

    @Override
    public Integer pick(Random r, Set<Integer> notIn) {
        return delegate.pick(r, notIn);
    }

    @Override
    public int first() {
        return delegate.first();
    }

    @Override
    public int last() {
        return delegate.last();
    }

    @Override
    public int max() {
        return delegate.max();
    }

    @Override
    public boolean removeAll(IntSet ints) {
        return delegate.removeAll(ints);
    }

    @Override
    public boolean contains(int val) {
        return delegate.contains(val);
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new RoPI(delegate.iterator());
    }

    @Override
    public boolean isArrayBased() {
        return delegate.isArrayBased();
    }

    @Override
    public int visitConsecutiveIndicesReversed(ConsecutiveItemsVisitor v) {
        return delegate.visitConsecutiveIndicesReversed(v);
    }

    @Override
    public int visitConsecutiveIndices(ConsecutiveItemsVisitor v) {
        return delegate.visitConsecutiveIndices(v);
    }

    @Override
    public int valueAt(int index) {
        return delegate.valueAt(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public Spliterator<Integer> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public Stream<Integer> stream() {
        return delegate.stream();
    }

    @Override
    public Stream<Integer> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super Integer> action) {
        delegate.forEach(action);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    boolean _add(int val) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int removeLast() {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean remove(int bit) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int removeFirst() {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public IntSet copy() {
        return delegate.copy();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(Integer e) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("read-only");
    }

    static final class RoPI implements PrimitiveIterator.OfInt {

        private final PrimitiveIterator.OfInt delegate;

        public RoPI(OfInt delegate) {
            this.delegate = delegate;
        }

        @Override
        public int nextInt() {
            return delegate.nextInt();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }
    }
}
