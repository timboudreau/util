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

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A spliterator which lazily binds to a delegate.
 *
 * @author Tim Boudreau
 */
final class LateBindingSpliterator<T> implements Spliterator<T> {

    private volatile Spliterator<T> delegate;
    private final Supplier<Spliterator<T>> supplier;

    LateBindingSpliterator(Supplier<Spliterator<T>> supplier) {
        this.supplier = supplier;
    }

    Spliterator<T> delegate() {
        Spliterator<T> del = delegate;
        if (del == null) {
            synchronized(this) {
                del = delegate;
                if (del == null) {
                    del = delegate = supplier.get();
                }
            }
        }
        return del;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        return delegate().tryAdvance(action);
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        delegate().forEachRemaining(action);
    }

    @Override
    public Spliterator<T> trySplit() {
        return delegate().trySplit();
    }

    @Override
    public long estimateSize() {
        return delegate().estimateSize();
    }

    @Override
    public long getExactSizeIfKnown() {
        return delegate().getExactSizeIfKnown();
    }

    @Override
    public int characteristics() {
        return delegate().characteristics();
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        return delegate().hasCharacteristics(characteristics);
    }

    @Override
    public Comparator<? super T> getComparator() {
        return delegate().getComparator();
    }
}
