/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * A primitive long iterator for high performance uses; this interface has been
 * deprecated since PrimitiveIterator.OfLong was introduced in JDK 8.
 *
 * @author Tim Boudreau
 * @deprecated Implementations should use PrimitiveIterator.OfLong
 */
@Deprecated
public interface Longerator extends LongSupplier {

    long next();

    boolean hasNext();

    @Override
    default long getAsLong() {
        return hasNext() ? next() : -1;
    }

    default void forEachRemaining(LongConsumer consumer) {
        while (hasNext()) {
            consumer.accept(next());
        }
    }

    default void forEachRemaining(Consumer<? super Long> action) {
        while (hasNext()) {
            action.accept(next());
        }
    }

    default Longerator of(long[] longs) {
        return new Longerator() {
            int ix = -1;

            @Override
            public long next() {
                return longs[++ix];
            }

            @Override
            public boolean hasNext() {
                return ix + 1 < longs.length;
            }
        };
    }

    default Longerator of(Collection<? extends Long> coll) {
        return new Longerator() {
            private final Iterator<? extends Long> iter = coll.iterator();

            @Override
            public long next() {
                Long result = iter.next();
                if (result == null) {
                    throw new IllegalArgumentException("Collection contains nulls: " + coll);
                }
                return result;
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }
        };
    }

    default Iterator<Long> toIterator() {
        return new Iterator<Long>() {
            @Override
            public boolean hasNext() {
                return Longerator.this.hasNext();
            }

            @Override
            public Long next() {
                return Longerator.this.next();
            }

            @Override
            public void forEachRemaining(Consumer<? super Long> action) {
                Longerator.this.forEachRemaining(action);
            }
        };
    }
}
