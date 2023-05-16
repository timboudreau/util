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
 * A PrimitiveIterator for byte sequences.
 *
 * @author Tim Boudreau
 */
public interface BytePrimitiveIterator extends PrimitiveIterator<Byte, ByteConsumer> {

    byte nextByte();

    @Override
    default void forEachRemaining(ByteConsumer action) {
        while (hasNext()) {
            action.accept(nextByte());
        }
    }

    @Override
    default Byte next() {
        return nextByte();
    }

    @Override
    default void forEachRemaining(Consumer<? super Byte> action) {
        while (hasNext()) {
            action.accept(nextByte());
        }
    }

    static BytePrimitiveIterator byteIterator(byte[] bytes) {
        notNull("Bytes may not be null", bytes);
        return new BytePrimitiveIterator() {
            private int cursor = -1;

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(cursor + " / " + bytes.length);
                }
                return bytes[++cursor];
            }

            @Override
            public boolean hasNext() {
                return cursor + 1 < bytes.length;
            }
        };
    }

    default PrimitiveIterator.OfInt asInts() {
        return new PrimitiveIterator.OfInt() {
            @Override
            public int nextInt() {
                return nextByte();
            }

            @Override
            public boolean hasNext() {
                return BytePrimitiveIterator.this.hasNext();
            }
        };
    }

    default PrimitiveIterator.OfInt asUnsignedInts() {
        return new PrimitiveIterator.OfInt() {
            @Override
            public int nextInt() {
                return nextByte() & 0xFF;
            }

            @Override
            public boolean hasNext() {
                return BytePrimitiveIterator.this.hasNext();
            }
        };
    }

    default ShortPrimitiveIterator asShorts() {
        return new ShortPrimitiveIterator() {
            @Override
            public short nextShort() {
                return nextByte();
            }

            @Override
            public boolean hasNext() {
                return BytePrimitiveIterator.this.hasNext();
            }
        };
    }

    default ShortPrimitiveIterator asUnsignedShorts() {
        return new ShortPrimitiveIterator() {
            @Override
            public short nextShort() {
                return (short) (nextByte() & 0xFF);
            }

            @Override
            public boolean hasNext() {
                return BytePrimitiveIterator.this.hasNext();
            }
        };
    }
}
