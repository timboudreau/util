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
package com.mastfrog.function.state;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

/**
 *
 * @author Tim Boudreau
 */
final class DblAtomic implements Dbl {

    private final AtomicLong value;

    DblAtomic(double val) {
        value = new AtomicLong(Double.doubleToLongBits(val));
    }

    DblAtomic() {
        value = new AtomicLong();
    }

    @Override
    public void accept(double value) {
        this.value.set(Double.doubleToLongBits(value));
    }

    @Override
    public double getAsDouble() {
        return Double.doubleToLongBits(value.get());
    }

    @Override
    public double apply(DoubleUnaryOperator op) {
        long lng = value.getAndUpdate(longOld -> {
            return Double.doubleToLongBits(op.applyAsDouble(Double.longBitsToDouble(longOld)));

        });
        return Double.longBitsToDouble(lng);
    }

    @Override
    public double apply(double val, DoubleBinaryOperator op) {
        long lng = value.getAndAccumulate(Double.doubleToLongBits(val), (la, lb) -> {
            return Double.doubleToLongBits(op.applyAsDouble(
                    Double.longBitsToDouble(la), Double.longBitsToDouble(lb)));
        });
        return Double.longBitsToDouble(lng);
    }

    @Override
    public double set(double val) {
        return Double.longBitsToDouble(value.getAndSet(Double.doubleToLongBits(val)));
    }

    @Override
    public double add(double val) {
        return Double.longBitsToDouble(value.getAndAccumulate(Double.doubleToLongBits(val), (la, lb) -> {
            double a = Double.longBitsToDouble(la);
            double b = Double.longBitsToDouble(lb);
            return Double.doubleToLongBits(a + b);
        }));
    }

    @Override
    public double min(double val) {
        return Double.longBitsToDouble(value.getAndAccumulate(Double.doubleToLongBits(val), (la, lb) -> {
            double a = Double.longBitsToDouble(la);
            double b = Double.longBitsToDouble(lb);
            return Double.doubleToLongBits(Math.min(a, b));
        }));
    }

    @Override
    public double max(double val) {
        return Double.longBitsToDouble(value.getAndAccumulate(Double.doubleToLongBits(val), (la, lb) -> {
            double a = Double.longBitsToDouble(la);
            double b = Double.longBitsToDouble(lb);
            return Double.doubleToLongBits(Math.max(a, b));
        }));
    }

    @Override
    public boolean ifUpdate(double newVal, Runnable r) {
        long lv = Double.doubleToLongBits(newVal);
        long oldValue = value.getAndAccumulate(lv, (la, lb) -> {
            return la;
        });
        if (oldValue != lv) {
            r.run();
            return true;
        }
        return false;
    }

    @Override
    public double subtract(double val) {
        return add(-val);
    }

    @Override
    public DoubleConsumer summer() {
        return this::add;
    }

    @Override
    public String toString() {
        return Double.toString(getAsDouble());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DblAtomic) {
            DblAtomic db = (DblAtomic) o;
            return db.value.get() == value.get();
        } else if (o instanceof Dbl) {
            double otherVal = ((Dbl) o).getAsDouble();
            long a = value.get();
            long b = Double.doubleToLongBits(otherVal + 0.0);
            return a == b;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long val = value.get();
        return (int) (val ^ val >> 32);
    }
}
