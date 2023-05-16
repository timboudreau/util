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
package com.mastfrog.function;

import java.util.function.Supplier;

/**
 * A function which takes a float.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface FloatQuadFunction<T> {

    /**
     * Accept a float and return an object of type T.
     *
     * @param val The value
     * @return The object
     */
    T apply(float a, float b, float c, float d);

    /**
     * Wrap a FloatSupplier in this function to create a Supplier&lt;T&gt;.
     *
     * @param supp A supplier of floats
     * @return A supplier of objects
     */
    default Supplier<T> toSupplier(FloatSupplier sa, FloatSupplier sb,
            FloatSupplier sc, FloatSupplier sd) {
        return () -> {
            return apply(sa.getAsFloat(), sb.getAsFloat(),
                    sc.getAsFloat(), sd.getAsFloat());
        };
    }

    /**
     * Convert this function to one which takes doubles and throws an exception
     * if the passed value is out of range for floats.
     *
     * @return A double-accepting function which delegates to this one
     */
    default DoubleQuadFunction<T> toDoubleQuadFunction() {
        return (a, b, c, d) -> {
            if (a > Float.MAX_VALUE || a < Float.MIN_VALUE) {
                throw new IllegalArgumentException("Value 0 out of range for float: " + a);
            }
            if (b > Float.MAX_VALUE || b < Float.MIN_VALUE) {
                throw new IllegalArgumentException("Value 1 out of range for float: " + b);
            }
            if (c > Float.MAX_VALUE || c < Float.MIN_VALUE) {
                throw new IllegalArgumentException("Value 2 out of range for float: " + c);
            }
            if (d > Float.MAX_VALUE || d < Float.MIN_VALUE) {
                throw new IllegalArgumentException("Value 3 out of range for float: " + d);
            }
            return apply((float) a, (float) b, (float) c, (float) d);
        };
    }

    /**
     * Create a FloatFunction from a DoubleFunction.
     *
     * @param <T> The function return type
     * @param df A double function
     * @return A float function
     */
    static <T> FloatQuadFunction<T> fromDoubleFunction(DoubleQuadFunction<T> df) {
        return (a, b, c, d) -> {
            return df.apply(a, b, c, d);
        };
    }
}
