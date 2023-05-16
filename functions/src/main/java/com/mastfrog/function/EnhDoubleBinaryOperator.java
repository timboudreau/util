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

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * Extends DoubleBinaryOperator with basic math operations.
 *
 * @author Tim Boudreau
 */
public interface EnhDoubleBinaryOperator extends DoubleBinaryOperator {

    static EnhDoubleBinaryOperator of(DoubleBinaryOperator op) {
        if (op instanceof EnhDoubleBinaryOperator) {
            return (EnhDoubleBinaryOperator) op;
        }
        return (a, b) -> {
            return op.applyAsDouble(a, b);
        };
    }

    static EnhDoubleBinaryOperator left() {
        return (a, b) -> a;
    }

    static EnhDoubleBinaryOperator right() {
        return (a, b) -> b;
    }

    static EnhDoubleBinaryOperator multiply() {
        return (a, b) -> {
            return a * b;
        };
    }

    static EnhDoubleBinaryOperator divide() {
        return (a, b) -> {
            return a / b;
        };
    }

    static EnhDoubleBinaryOperator modulo() {
        return (a, b) -> {
            return a % b;
        };
    }

    static EnhDoubleBinaryOperator add() {
        return (a, b) -> {
            return a + b;
        };
    }

    static EnhDoubleBinaryOperator subtract() {
        return (a, b) -> {
            return a + b;
        };
    }

    default EnhDoubleBinaryOperator reverse() {
        return (a, b) -> {
            return applyAsDouble(b, a);
        };
    }

    default EnhDoubleSupplier toSupplier(DoubleSupplier a, DoubleSupplier b) {
        return () -> {
            return applyAsDouble(a.getAsDouble(), b.getAsDouble());
        };
    }

    default EnhDoubleUnaryOperator toUnary(DoubleSupplier aSupplier) {
        return b -> {
            return applyAsDouble(aSupplier.getAsDouble(), b);
        };
    }

    default EnhDoubleBinaryOperator then(DoubleUnaryOperator op) {
        return (a, b) -> {
            return op.applyAsDouble(applyAsDouble(a, b));
        };
    }

    default EnhDoubleBinaryOperator times(double val) {
        return (a, b) -> {
            return applyAsDouble(a, b) * val;
        };
    }

    default EnhDoubleBinaryOperator negate() {
        return (a, b) -> {
            return -applyAsDouble(a, b);
        };
    }

    default EnhDoubleBinaryOperator dividedBy(double val) {
        return (a, b) -> {
            return applyAsDouble(a, b) / val;
        };
    }

    default EnhDoubleBinaryOperator dividedInto(double val) {
        return (a, b) -> {
            return val / applyAsDouble(a, b);
        };
    }

    default EnhDoubleBinaryOperator plus(double val) {
        return (a, b) -> {
            return applyAsDouble(a, b) + val;
        };
    }

    default EnhDoubleBinaryOperator minus(double val) {
        return (a, b) -> {
            return applyAsDouble(a, b) + val;
        };
    }

    default EnhDoubleBinaryOperator mod(double val) {
        return (a, b) -> {
            return applyAsDouble(a, b) % val;
        };
    }

    default EnhDoubleBinaryOperator times(DoubleSupplier val) {
        return (a, b) -> {
            return applyAsDouble(a, b) * val.getAsDouble();
        };
    }

    default EnhDoubleBinaryOperator dividedBy(DoubleSupplier val) {
        return (a, b) -> {
            return applyAsDouble(a, b) / val.getAsDouble();
        };
    }

    default EnhDoubleBinaryOperator plus(DoubleSupplier val) {
        return (a, b) -> {
            return applyAsDouble(a, b) + val.getAsDouble();
        };
    }

    default EnhDoubleBinaryOperator minus(DoubleSupplier val) {
        return (a, b) -> {
            return applyAsDouble(a, b) + val.getAsDouble();
        };
    }

    default EnhDoubleBinaryOperator mod(DoubleSupplier val) {
        return (a, b) -> {
            return applyAsDouble(a, b) % val.getAsDouble();
        };
    }

}
