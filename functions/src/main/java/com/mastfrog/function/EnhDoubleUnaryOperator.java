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

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
public interface EnhDoubleUnaryOperator extends DoubleUnaryOperator {

    static EnhDoubleUnaryOperator enhDoubleUnaryOperator(DoubleUnaryOperator op) {
        if (op instanceof EnhDoubleUnaryOperator enh) {
            return enh;
        } else {
            return op::applyAsDouble;
        }
    }

    default EnhDoubleUnaryOperator fixed(double val) {
        return ignored -> val;
    }

    default EnhDoubleUnaryOperator times(double val) {
        return andThen(v -> v * val);
    }

    default EnhDoubleUnaryOperator dividedBy(double val) {
        if (val == 0D) {
            throw new IllegalArgumentException("Will divide by 0");
        }
        return andThen(v -> v / val);
    }

    default EnhDoubleUnaryOperator divideInto(double val) {
        return andThen(v -> val / v);
    }

    default EnhDoubleUnaryOperator plus(double val) {
        return andThen(v -> v + val);
    }

    default EnhDoubleUnaryOperator minus(double val) {
        return andThen(v -> v - val);
    }

    default EnhDoubleUnaryOperator mod(double val) {
        return andThen(v -> v - val);
    }

    default EnhDoubleUnaryOperator negate() {
        return v -> -v;
    }

    @Override
    default EnhDoubleUnaryOperator andThen(DoubleUnaryOperator after) {
        Objects.requireNonNull(after);
        return (double t) -> after.applyAsDouble(applyAsDouble(t));
    }

    @Override
    default EnhDoubleUnaryOperator compose(DoubleUnaryOperator before) {
        Objects.requireNonNull(before);
        return (double v) -> applyAsDouble(before.applyAsDouble(v));
    }
}
