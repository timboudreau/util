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
import java.util.function.IntSupplier;

/**
 *
 * @author Tim Boudreau
 */
public interface Indexed<T> extends IntAddressable<T>, IntSized {

    static <T> Indexed<T> fromList(List<T> list) {
        return new Indexed<T>() {
            @Override
            public T forIndex(int index) {
                return list.get(index);
            }

            @Override
            public int size() {
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
        return new IndexedAsIterable<>(this, this);
    }

    default Iterator<T> toIterator() {
        return new IndexedAsIterable<>(this, this);
    }

    static <T> Indexed<T> compose(int size, IntAddressable<T> lookup) {
        return compose(() -> size, lookup);
    }

    static <T> Indexed<T> compose(IntAddressable<T> lookup, IntSized sizeSupplier) {
        return compose(() -> sizeSupplier.size(), lookup);
    }

    static <T> Indexed<T> compose(IntSupplier sizeSupplier, IntAddressable<T> lookup) {
        return new Indexed<T>() {
            @Override
            public T forIndex(int index) {
                return lookup.forIndex(index);
            }

            @Override
            public int size() {
                return sizeSupplier.getAsInt();
            }
        };
    }

    default LongIndexed toLongIndexed() {
        return LongIndexed.compose(toLongSized(), toLongAddressable());
    }
}
