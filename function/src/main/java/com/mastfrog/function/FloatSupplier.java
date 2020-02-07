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

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * A supplier of floats.
 *
 * @author Tim Boudreau
 */
public interface FloatSupplier {

    /**
     * Get or compute the current value.
     *
     * @return A float
     */
    float getAsFloat();

    /**
     * Filter non-finate values, returning the passed value
     * instead.
     *
     * @param val The value to use in place of non-finite values
     * @return A float supplier that wraps this one
     */
    default FloatSupplier filterNonFinite(float val) {
        return () -> {
            float value = getAsFloat();
            if (!Float.isFinite(value)) {
                value = val;
            }
            return value;
        };
    }

    /**
     * Filter NaN values, returning the passed value
     * instead.
     *
     * @param val The value to use in place of non-finite values
     * @return A float supplier that wraps this one
     */
    default FloatSupplier filterNaN(float val) {
        return () -> {
            float value = getAsFloat();
            if (Float.isNaN(value)) {
                value = val;
            }
            return value;
        };
    }

    default DoubleSupplier toDoubleSupplier() {
        return this::getAsFloat;
    }

    static FloatSupplier fromDoubleSupplier(DoubleSupplier supp) {
        return () -> (float) supp.getAsDouble();
    }

    default BooleanSupplier toBooleanSupplier(FloatPredicate pred) {
        return () -> pred.test(getAsFloat());
    }
}
