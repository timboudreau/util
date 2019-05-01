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
package com.mastfrog.abstractions.list;

import java.util.Iterator;
import java.util.List;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
public interface LongIndexed<T> extends LongAddressable<T>, LongSized {

    static <T> LongIndexed<T> fromList(List<T> list) {
        return new LongIndexed<T>() {
            @Override
            public T forIndex(long index) {
                if (index > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Value out of range: " + index);
                }
                return list.get((int) index);
            }

            @Override
            public long size() {
                return list.size();
            }

            @Override
            public Iterable<T> toIterable() {
                return list;
            }

            @Override
            public Iterator<T> toIterator() {
                return list.iterator();
            }
        };
    }

    default Iterable<T> toIterable() {
        return new LongIndexedAsIterable<>(this, this);
    }

    default Iterator<T> toIterator() {
        return new LongIndexedAsIterable<>(this, this);
    }

    static <T> LongIndexed<T> compose(long size, LongAddressable<T> lookup) {
        return compose(() -> size, lookup);
    }

    static <T> LongIndexed<T> compose(LongAddressable<T> lookup, LongSupplier sizeSupplier) {
        return compose(() -> sizeSupplier.getAsLong(), lookup);
    }

    static <T> LongIndexed<T> compose(LongSized sizeSupplier, LongAddressable<T> lookup) {
        return new LongIndexed<T>() {
            @Override
            public T forIndex(long index) {
                return lookup.forIndex(index);
            }

            @Override
            public long size() {
                return sizeSupplier.size();
            }
        };
    }
}
