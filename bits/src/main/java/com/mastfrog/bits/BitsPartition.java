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

import com.mastfrog.function.state.Int;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class BitsPartition implements MutableBits {

    private final int start;
    private final int length;
    private final Bits orig;

    public BitsPartition(Bits orig, int start, int length) {
        this.orig = orig;
        this.start = start;
        this.length = length;
    }

    @Override
    public int cardinality() {
        Int result = Int.create();
        orig.forEachSetBitAscending(start, start + length, bit -> {
            if (bit < start + length) {
                result.increment();
            }
        });
        return result.getAsInt();
    }

    @Override
    public Bits copy() {
        return new BitsPartition(orig.get(start, start + length), start, length);
    }

    @Override
    public MutableBits mutableCopy() {
        return new BitsPartition(orig.get(start, start + length), start, length);
    }

    @Override
    public boolean get(int bitIndex) {
        bitIndex += start;
        if (bitIndex - start > length) {
            return false;
        }
        return orig.get(bitIndex);
    }

    @Override
    public int nextClearBit(int fromIndex) {
        fromIndex += start;
        if (fromIndex - start > length) {
            return fromIndex;
        }
        return orig.nextClearBit(fromIndex) - start;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        fromIndex += start;
        if (fromIndex - start > length) {
            return -1;
        }
        return orig.nextSetBit(fromIndex) - start;
    }

    @Override
    public int previousClearBit(int fromIndex) {
        fromIndex += start;
        if (fromIndex - start > length) {
            fromIndex = start + length - 1;
        }
        int result = orig.previousClearBit(fromIndex) - start;
        return Math.max(-1, result);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        fromIndex += start;
        if (fromIndex - start > length) {
            fromIndex = start + length;
        }
        int result = orig.previousSetBit(fromIndex) - start;
        return Math.max(-1, result);
    }

    @Override
    public void set(int bitIndex, boolean value) {
        if (orig instanceof MutableBits) {
            ((MutableBits) orig).set(bitIndex + start, value);
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        }
        Bits other = (Bits) o;
        return other.contentEquals(this);
    }

    @Override
    public int forEachSetBitAscending(IntConsumer consumer) {
        Int res = Int.create();
        orig.forEachSetBitAscending(start, start + length, bit -> {
            boolean result = bit < start + length;
            if (result) {
                res.increment();
                consumer.accept(bit - start);
            }
            return result;
        });
        return res.getAsInt();
    }

    @Override
    public int forEachSetBitAscending(IntPredicate consumer) {
        Int res = Int.create();
        orig.forEachSetBitAscending(start, start + length, bit -> {
            boolean result = bit < start + length;
            if (result) {
                res.increment();
                if (!consumer.test(bit - start)) {
                    return false;
                }
            }
            return result;
        });
        return res.getAsInt();
    }

    @Override
    public int forEachSetBitDescending(IntConsumer consumer) {
        Int res = Int.create();
        orig.forEachSetBitDescending(start + length, start, bit -> {
            boolean result = bit < start + length;
            if (result) {
                res.increment();
                consumer.accept(bit - start);
            }
            return result;
        });
        return res.getAsInt();
    }

    @Override
    public int forEachSetBitDescending(IntPredicate consumer) {
        Int res = Int.create();
        orig.forEachSetBitDescending(start + length, start, bit -> {
            boolean result = bit < start + length;
            if (result) {
                res.increment();
                if (!consumer.test(bit - start)) {
                    return false;
                }
            }
            return result;
        });
        return res.getAsInt();
    }
}
