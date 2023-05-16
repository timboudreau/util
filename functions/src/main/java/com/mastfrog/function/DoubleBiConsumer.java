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
 * Bi-consumer for primitive doubles.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface DoubleBiConsumer {

    /**
     * Accept two double values
     *
     * @param a The first value
     * @param b The second value
     */
    void accept(double a, double b);

    /**
     * Chain another bi consumer to this one.
     *
     * @param next
     * @return
     */
    default DoubleBiConsumer andThen(DoubleBiConsumer next) {
        return (a, b) -> {
            accept(a, b);
            next.accept(a, b);
        };
    }

    /**
     * Convert this to a bi-consumer of floats.
     *
     * @return A float bi-consumer
     */
    default FloatBiConsumer toFloatBiConsumer() {
        return FloatBiConsumer.fromDoubleBiConsumer(this);
    }
}
