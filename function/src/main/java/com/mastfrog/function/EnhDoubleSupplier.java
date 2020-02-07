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

import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
public interface EnhDoubleSupplier extends DoubleSupplier {

    static EnhDoubleSupplier fixed(double val) {
        return () -> val;
    }

    default EnhDoubleSupplier andThen(DoubleUnaryOperator op) {
        return () -> op.applyAsDouble(getAsDouble());
    }

    default EnhDoubleSupplier plus(DoubleSupplier supp) {
        return () -> getAsDouble() + supp.getAsDouble();
    }

    default EnhDoubleSupplier minus(DoubleSupplier supp) {
        return () -> getAsDouble() - supp.getAsDouble();
    }

    default EnhDoubleSupplier times(DoubleSupplier supp) {
        return () -> getAsDouble() * supp.getAsDouble();
    }

    default EnhDoubleSupplier mod(DoubleSupplier supp) {
        return () -> getAsDouble() % supp.getAsDouble();
    }

    default EnhDoubleSupplier dividedBy(DoubleSupplier supp) {
        return () -> getAsDouble() / supp.getAsDouble();
    }

    default EnhDoubleSupplier dividedInto(DoubleSupplier supp) {
        return () -> supp.getAsDouble() / getAsDouble();
    }

    default EnhDoubleSupplier plus(double val) {
        return () -> getAsDouble() + val;
    }

    default EnhDoubleSupplier minus(double val) {
        return () -> getAsDouble() - val;
    }

    default EnhDoubleSupplier times(double val) {
        return () -> getAsDouble() * val;
    }

    default EnhDoubleSupplier mod(double val) {
        return () -> getAsDouble() % val;
    }

    default EnhDoubleSupplier dividedBy(double val) {
        return () -> getAsDouble() / val;
    }

    default EnhDoubleSupplier dividedInto(double val) {
        return () -> val / getAsDouble();
    }

    default EnhDoubleSupplier abs() {
        return andThen(Math::abs);
    }

    enum Conversion implements EnhDoubleUnaryOperator {
        ROUND,
        CEIL,
        FLOOR;

        @Override
        public double applyAsDouble(double operand) {
            switch (this) {
                case ROUND:
                    return Math.round(operand);
                case CEIL:
                    return Math.ceil(operand);
                case FLOOR:
                    return Math.floor(operand);
                default:
                    throw new AssertionError(this);
            }
        }

        EnhIntSupplier toIntSupplier(EnhDoubleSupplier supplier) {
            return () -> {
                return (int) applyAsDouble(supplier.getAsDouble());
            };
        }
    }

    default EnhIntSupplier toIntSupplier(Conversion conversion) {
        return conversion.toIntSupplier(this);
    }

    default EnhIntSupplier toIntSupplier() {
        return toIntSupplier(Conversion.FLOOR);
    }

    default EnhDoubleSupplier ifNonFinite(DoubleSupplier supp) {
        return () -> {
            double result = getAsDouble();
            if (!Double.isFinite(result)) {
                result = supp.getAsDouble();
            }
            return result;
        };
    }
}
