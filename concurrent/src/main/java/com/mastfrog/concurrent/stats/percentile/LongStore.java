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
package com.mastfrog.concurrent.stats.percentile;

import java.util.function.IntToLongFunction;
import java.util.function.LongConsumer;

/**
 * A store for a potentially very large array of <code>long</code>s, which makes
 * as few assumptions as possible about how the data is stored or accessed, and
 * is a minimal decomposition of a sortable List&lt;Long&gt;.
 * <code>IntToLongFunction.applyAsLong(int)</code> method may be used to look up
 * values by index; the <code>LongConsumer.accept(long)</code> appends values.
 */
public interface LongStore extends LongConsumer, IntToLongFunction {

    /**
     * Get the size of this store.
     *
     * @return A size
     */
    int size();

    /**
     * Iterate all elements.
     *
     * @param c A LongConsumer
     */
    default void forEach(LongConsumer c) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            c.accept(this.applyAsLong(i));
        }
    }

    /**
     * Sort the contents of this store.
     */
    void sort();

    /**
     * Determine if the contents of this store are sorted.
     *
     * @return true if it is already sorted
     */
    boolean isSorted();

    default boolean isEmpty() {
        return size() == 0;
    }

}
