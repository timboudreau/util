/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.bits.util;

import com.mastfrog.abstractions.list.IntSized;
import static java.lang.Long.lowestOneBit;
import static java.lang.Long.numberOfTrailingZeros;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

/**
 * Factory for iterators and iterables that list the set bits of an int or long.
 *
 * @author Tim Boudreau
 */
public abstract class BitsIterable<I extends Number, I_CONS> implements Iterable<I>, IntSized {

    @Override
    public abstract PrimitiveIterator<I, I_CONS> iterator();

    public static BitsIterable<Integer, IntConsumer> of(long val) {
        return new ForLong(val);
    }

    public static BitsIterable<Integer, IntConsumer> of(int val) {
        return new ForInt(val);
    }

    public static PrimitiveIterator<Integer, IntConsumer> bitsIterator(long val) {
        return new LongBitsIter(val);
    }

    public static PrimitiveIterator<Integer, IntConsumer> bitsIterator(int val) {
        return new IntBitsIter(val);
    }

    private static final class ForLong extends BitsIterable<Integer, IntConsumer> {

        private final long val;

        public ForLong(long val) {
            this.val = val;
        }

        @Override
        public PrimitiveIterator<Integer, IntConsumer> iterator() {
            return new LongBitsIter(val);
        }

        @Override
        public int size() {
            return Long.bitCount(val);
        }
    }

    private static final class ForInt extends BitsIterable<Integer, IntConsumer> {

        private final int val;

        public ForInt(int val) {
            this.val = val;
        }

        @Override
        public PrimitiveIterator<Integer, IntConsumer> iterator() {
            return new IntBitsIter(val);
        }

        @Override
        public int size() {
            return Integer.bitCount(val);
        }
    }

    private static final class LongBitsIter implements PrimitiveIterator.OfInt {

        private long value;

        LongBitsIter(long value) {
            this.value = value;
        }

        public String toString() {
            return Long.toBinaryString(value);
        }

        @Override
        public int nextInt() {
            if (value == 0) {
                throw new NoSuchElementException();
            }
            long val = lowestOneBit(value);
            value &= ~val;
            return numberOfTrailingZeros(val);
        }

        @Override
        public boolean hasNext() {
            return value != 0L;
        }
    }

    private static final class IntBitsIter implements PrimitiveIterator.OfInt {

        private int value;

        IntBitsIter(int value) {
            this.value = value;
        }

        public String toString() {
            return Integer.toBinaryString(value);
        }

        @Override
        public int nextInt() {
            if (value == 0) {
                throw new NoSuchElementException();
            }
            int val = Integer.lowestOneBit(value);
            value &= ~val;
            return Integer.numberOfTrailingZeros(val);
        }

        @Override
        public boolean hasNext() {
            return value != 0L;
        }
    }
}
