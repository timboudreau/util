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

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface FloatBiConsumer {

    /**
     * Accept two floating point values.
     *
     * @param a The first float
     * @param b The second float
     */
    void accept(float a, float b);

    /**
     * Convert this to a DoubleBiConsumer; the resulting consumer will
     * throw an exception if passed values that cannot be expressed as
     * a float.
     *
     * @return A BiConsumer of doubles
     */
    default DoubleBiConsumer toDoubleBiConsumer() {
        return (double a, double b) -> {
            if (a < Float.MIN_VALUE || a > Float.MAX_VALUE || b < Float.MIN_VALUE || b > Float.MAX_VALUE) {
                throw new IllegalArgumentException("Value out of range for floats: " + a + " and " + b);
            }
            accept((float) a, (float) b);
        };
    }

    /**
     * Convert a DoubleBiConsumer to one accepting floats, for
     * use with method references.
     *
     * @param bc A bi-consumer of doubles
     * @return A bi-consumer of floats
     */
    static FloatBiConsumer fromDoubleBiConsumer(DoubleBiConsumer bc) {
        return bc::accept;
    }
}
