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
package com.mastfrog.function;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

/**
 * Miscellaneous convenience methods.
 *
 * @author Tim Boudreau
 */
public class FunctionUtils {

    /**
     * Wrap an array as an IntUnaryOperator which returns elements for indices.
     * The implementation returned will equals() any other instance returned by
     * this method over the same array. If asked for an index out of range, an
     * ArrayIndexOutOfBoundsException will be thrown.
     *
     * @param arr An array
     * @return An IntUnaryOperator
     */
    public static IntUnaryOperator asUnaryOperator(int[] arr) {
        return new ArrayIntUnaryOperator(arr);
    }

    /**
     * Wrap an array as an IntToLongFunction which returns elements for indices.
     * The implementation returned will equals() any other instance returned by
     * this method over the same array. If asked for an index out of range, an
     * ArrayIndexOutOfBoundsException will be thrown.
     *
     * @param arr An array
     * @return An IntToLongFunction
     */
    public static IntToLongFunction asFunction(long[] arr) {
        return new ArrayIntToLongFunction(arr);
    }

    private FunctionUtils() {
        throw new AssertionError();
    }

    private static final class ArrayIntToLongFunction implements IntToLongFunction, Serializable {

        private final long[] arr;

        public ArrayIntToLongFunction(long[] arr) {
            this.arr = arr;
        }

        @Override
        public long applyAsLong(int value) {
            return arr[value];
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 11 * hash + Arrays.hashCode(this.arr);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ArrayIntToLongFunction other = (ArrayIntToLongFunction) obj;
            return Arrays.equals(this.arr, other.arr);
        }

        @Override
        public String toString() {
            return Arrays.toString(arr);
        }
    }

    private static final class ArrayIntUnaryOperator implements IntUnaryOperator, Serializable {

        private final int[] arr;

        public ArrayIntUnaryOperator(int[] arr) {
            this.arr = arr;
        }

        @Override
        public int applyAsInt(int operand) {
            return arr[operand];
        }

        @Override
        public String toString() {
            return Arrays.toString(arr);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 19 * hash + Arrays.hashCode(this.arr);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ArrayIntUnaryOperator other = (ArrayIntUnaryOperator) obj;
            return Arrays.equals(this.arr, other.arr);
        }
    }
}
