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
package com.mastfrog.function;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;

/**
 * A primitive iterator for shorts.
 *
 * @author Tim Boudreau
 */
public interface ShortPrimitiveIterator extends PrimitiveIterator<Short, ShortConsumer> {

    short nextShort();

    @Override
    default void forEachRemaining(ShortConsumer action) {
        while (hasNext()) {
            action.accept(nextShort());
        }
    }

    @Override
    default Short next() {
        return nextShort();
    }

    @Override
    default void forEachRemaining(Consumer<? super Short> action) {
        while (hasNext()) {
            action.accept(nextShort());
        }
    }

    default PrimitiveIterator.OfInt toInts() {
        return new PrimitiveIterator.OfInt() {
            @Override
            public int nextInt() {
                return ShortPrimitiveIterator.this.nextShort();
            }

            @Override
            public boolean hasNext() {
                return ShortPrimitiveIterator.this.hasNext();
            }
        };
    }

    default PrimitiveIterator.OfInt toUnsignedInts() {
        return new PrimitiveIterator.OfInt() {
            @Override
            public int nextInt() {
                return ShortPrimitiveIterator.this.nextShort() & 0xFFFF;
            }

            @Override
            public boolean hasNext() {
                return ShortPrimitiveIterator.this.hasNext();
            }
        };
    }

    static ShortPrimitiveIterator shortIterator(short[] shorts) {
        notNull("shorts may not be null", shorts);
        return new ShortPrimitiveIterator() {
            private int cursor = -1;

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(cursor + " / " + shorts.length);
                }
                return shorts[++cursor];
            }

            @Override
            public boolean hasNext() {
                return cursor + 1 < shorts.length;
            }
        };
    }

}
