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

import com.mastfrog.function.FloatConsumer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tim Boudreau
 */
final class FltAtomic implements Flt {

    private final AtomicInteger value;

    FltAtomic(float val) {
        value = new AtomicInteger(Float.floatToIntBits(val));
    }

    FltAtomic() {
        value = new AtomicInteger();
    }

    @Override
    public void accept(float value) {
        this.value.set(Float.floatToIntBits(value));
    }

    @Override
    public float getAsFloat() {
        return Float.floatToIntBits(value.get());
    }

    @Override
    public float apply(com.mastfrog.function.FloatUnaryOperator op) {
        int result = value.getAndUpdate(longOld -> {
            return Float.floatToIntBits(op.applyAsFloat(Float.intBitsToFloat(longOld)));

        });
        return Float.intBitsToFloat(result);
    }

    @Override
    public float apply(float val, com.mastfrog.function.FloatBinaryOperator op) {
        int lng = value.getAndAccumulate(Float.floatToIntBits(val), (la, lb) -> {
            return Float.floatToIntBits(op.applyAsFloat(
                    Float.intBitsToFloat(la), Float.intBitsToFloat(lb)));
        });
        return Float.intBitsToFloat(lng);
    }

    @Override
    public float set(float val) {
        return Float.intBitsToFloat(value.getAndSet(Float.floatToIntBits(val)));
    }

    @Override
    public float add(float val) {
        return Float.intBitsToFloat(value.getAndAccumulate(Float.floatToIntBits(val), (la, lb) -> {
            float a = Float.intBitsToFloat(la);
            float b = Float.intBitsToFloat(lb);
            return Float.floatToIntBits(a + b);
        }));
    }

    @Override
    public float min(float val) {
        return Float.intBitsToFloat(value.getAndAccumulate(Float.floatToIntBits(val), (la, lb) -> {
            float a = Float.intBitsToFloat(la);
            float b = Float.intBitsToFloat(lb);
            return Float.floatToIntBits(Math.min(a, b));
        }));
    }

    @Override
    public float max(float val) {
        return Float.intBitsToFloat(value.getAndAccumulate(Float.floatToIntBits(val), (la, lb) -> {
            float a = Float.intBitsToFloat(la);
            float b = Float.intBitsToFloat(lb);
            return Float.floatToIntBits(Math.max(a, b));
        }));
    }

    @Override
    public float subtract(float val) {
        return add(-val);
    }

    @Override
    public FloatConsumer summer() {
        return this::add;
    }

    @Override
    public String toString() {
        return Float.toString(getAsFloat());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof FltAtomic) {
            FltAtomic db = (FltAtomic) o;
            return db.value.get() == value.get();
        } else if (o instanceof Flt) {
            float otherVal = ((Flt) o).getAsFloat();
            long a = value.get();
            long b = Float.floatToIntBits(otherVal + 0.0F);
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
