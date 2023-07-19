/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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

import static com.mastfrog.bits.RLEB.INT_MAX_UNSIGNED;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.sort;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Builder for a run-length-encoded bits which stores its contents as sets of
 * ranges.
 *
 * @author Tim Boudreau
 */
public final class RLEBitsBuilder {

    private LinkedList<BitRangeImpl> ranges = new LinkedList<>();

    private RLEBitsBuilder() {

    }

    public static RLEBitsBuilder newRleBitsBuilder() {
        return new RLEBitsBuilder();
    }

    public RLEBitsBuilder add(Bits bits) {
        bits.visitRangesLong((first, last) -> {
            withRange(first, last);
        });
        return this;
    }

    public BitRangeBuilder range(long first) {
        return new BitRangeImpl(first);
    }

    public RLEBitsBuilder withRange(long first, long last) {
        ranges.add(new BitRangeImpl(first, last));
        return this;
    }

    public Bits build() {
        if (ranges.isEmpty()) {
            return new RLEB();
        }
        normalize();
        long[] items = new long[ranges.size()];
        BitRangeImpl[] r = ranges.toArray(BitRangeImpl[]::new);
        for (int i = 0; i < r.length; i++) {
            items[i] = r[i].value();
        }
        return new RLEB(items, items.length);
    }

    private void normalize() {
        sort(ranges);

        Iterator<BitRangeImpl> iter = ranges.iterator();
        BitRangeImpl prev = iter.next();
        while (iter.hasNext()) {
            BitRangeImpl bi = iter.next();
            Optional<BitRangeImpl> opt = prev.coalesce(bi);
            if (opt.isPresent()) {
                BitRangeImpl x = opt.get();
                prev.first = x.first;
                prev.last = x.last;
                iter.remove();
            }
        }
    }

    public static abstract class BitRangeBuilder {

        BitRangeBuilder() {
            // pkg private
        }

        public abstract RLEBitsBuilder withFinalBit(long bit);
    }

    private class BitRangeImpl extends BitRangeBuilder implements Comparable<BitRangeImpl> {

        long first;
        long last;

        BitRangeImpl(long first, long last) {
            if (last < first) {
                throw new IllegalArgumentException("last " + last + " is < first " + first);
            }
            if (last < 0 || first < 0) {
                throw new IllegalArgumentException("Negative bits not supported");
            }
            this.first = first;
            this.last = last;
        }

        BitRangeImpl(long first) {
            if (first > INT_MAX_UNSIGNED) {
                throw new IllegalArgumentException("Value must be < " + INT_MAX_UNSIGNED);
            }
            this.first = this.last = first;
        }

        private boolean contains(long val) {
            if (val == first || val == last) {
                return true;
            }
            if (val > first && val < last) {
                return true;
            }
            return false;
        }

        private boolean contains(BitRangeImpl i) {
            return contains(i.first) && contains(i.last);
        }

        private boolean overlaps(BitRangeImpl b) {
            return contains(b) || b.contains(this);
        }

        Optional<BitRangeImpl> coalesce(BitRangeImpl into) {
            if (overlaps(into)) {
                return Optional.of(new BitRangeImpl(min(first, into.first), max(last, into.last)));
            }
            return Optional.empty();
        }

        long value() {
            return first | (last << 32);
        }

        long length() {
            return (last - first) + 1;
        }

        @Override
        public RLEBitsBuilder withFinalBit(long bit) {
            if (bit < first) {
                throw new IllegalArgumentException("First bit " + first + " is > last " + last);
            }
            if (first != last) {
                throw new IllegalStateException("Already used: " + first + ":" + last);
            }
            if (bit < 0) {
                throw new IllegalArgumentException("Negative bits not supported: " + bit);
            }
            if (bit > INT_MAX_UNSIGNED) {
                throw new IllegalArgumentException("Bit value must be <= " + INT_MAX_UNSIGNED);
            }
            this.last = bit;
            ranges.add(this);
            return RLEBitsBuilder.this;
        }

        @Override
        public int compareTo(BitRangeImpl o) {
            int result = Long.compare(first, o.first);
            if (result == 0) {
                result = Long.compare(o.length(), length());
            }
            return result;
        }
    }
}
