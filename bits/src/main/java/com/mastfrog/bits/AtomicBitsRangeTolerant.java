/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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

package com.mastfrog.bits;

import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class AtomicBitsRangeTolerant implements AtomicBits {
    private final AtomicBits orig;

    AtomicBitsRangeTolerant(AtomicBits orig) {
        this.orig = orig;
    }

    @Override
    public int capacity() {
        return orig.capacity();
    }

    @Override
    public boolean clearing(int bit) {
        if (!orig.canContain(bit)) {
            return false;
        }
        return orig.clearing(bit);
    }

    @Override
    public boolean setting(int bit) {
        if (!orig.canContain(bit)) {
            return false;
        }
        return orig.setting(bit);
    }

    @Override
    public int settingNextClearBit(int from) {
        return orig.settingNextClearBit(from);
    }

    @Override
    public AtomicBits mutableCopy() {
        return orig.mutableCopy();
    }

    @Override
    public AtomicBits copy() {
        return new AtomicBitsRangeTolerant(orig.copy());
    }

    @Override
    public AtomicBits filter(IntPredicate pred) {
        return new AtomicBitsRangeTolerant(orig.filter(pred));
    }

    @Override
    public AtomicBits copy(int newBits) {
        return new AtomicBitsRangeTolerant(orig.copy(newBits));
    }

    @Override
    public void set(int bitIndex, boolean value) {
        if (!orig.canContain(bitIndex)) {
            return;
        }
        orig.set(bitIndex, value);
    }

    @Override
    public int cardinality() {
        return orig.cardinality();
    }

    @Override
    public boolean get(int bitIndex) {
        if (!orig.canContain(bitIndex)) {
            return false;
        }
        return orig.get(bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        if (fromIndex < 0) {
            fromIndex= 0;
        } else if (fromIndex > orig.capacity()) {
            return -1;
        }
        return orig.nextClearBit(fromIndex);
    }

    @Override
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            fromIndex= 0;
        } else if (fromIndex > orig.capacity()) {
            return -1;
        }
        return orig.nextSetBit(fromIndex);
    }

    @Override
    public int previousClearBit(int fromIndex) {
        if (fromIndex <= 0) {
            return -1;
        } else if (fromIndex >= orig.capacity()) {
            fromIndex = orig.capacity()-1;
        }
        return orig.previousClearBit(fromIndex);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        if (fromIndex <= 0) {
            return -1;
        } else if (fromIndex >= orig.capacity()) {
            fromIndex = orig.capacity()-1;
        }
        return orig.previousSetBit(fromIndex);
    }

    @Override
    public int hashCode() {
        return bitsHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        }
        return contentEquals((Bits) o);
    }

    public String toString() {
        return orig.toString();
    }
}
