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
import java.util.BitSet;
import static java.util.Collections.sort;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Builder for a run-length-encoded bits which stores its contents as sets of
 * ranges. An RLE bits can support bit-indexes from 0 to 4,294,967,295
 * (Integer.MAX_VALUE unsigned or <code>0xFFFFFFFFL</code>).
 * <p>
 * Bit ranges can be added to the builder in any order, and will be coalesced.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class RLEBitsBuilder {

    private LinkedList<BitRangeImpl> ranges = new LinkedList<>();

    private RLEBitsBuilder() {

    }

    /**
     * Create a new RLE bits builder.
     *
     * @return A builder
     */
    public static RLEBitsBuilder newRleBitsBuilder() {
        return new RLEBitsBuilder();
    }

    /**
     * Add the contents of an existing Bits to this builder.
     *
     * @param bits A bits
     * @return this
     */
    public RLEBitsBuilder add(Bits bits) {
        bits.visitRangesLong((first, last) -> {
            withRange(first, last);
        });
        return this;
    }

    /**
     * Start a range that will be added to this builder.
     *
     * @param first The first bit
     * @return A <code>{@link BitRangeBuilder}</code>
     */
    public BitRangeBuilder range(long first) {
        return new BitRangeImpl(first);
    }

    /**
     * Add a bit range to this builder.
     *
     * @param first The first bit - must be <code>&lt;= last</code>
     * @param last The last bit, inclusive
     * @return this
     */
    public RLEBitsBuilder withRange(long first, long last) {
        BitRangeImpl newRange = new BitRangeImpl(first, last);
        if (ranges.contains(newRange)) {
            return this;
        }
        if (!ranges.isEmpty()) {
            sort(ranges);
            BitRangeImpl l = ranges.getLast();
            if (last > l.last && l.contains(first)) {
                l.last = last;
                return this;
            } else if (first < l.first && l.contains(last)) {
                l.first = first;
                return this;
            }
        }
        ranges.add(newRange);
        return this;
    }

    /**
     * Build a bits.
     *
     * @return A bits
     */
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
            if (bi.equals(prev)) {
                iter.remove();
                continue;
            }
            Optional<BitRangeImpl> opt = prev.coalesce(bi);
            if (opt.isPresent()) {
                BitRangeImpl x = opt.get();
                prev.first = x.first;
                prev.last = x.last;
                iter.remove();
            }
        }
    }

    public static void main(String[] args) {
        // 001111000011
        // 000011100101
        BitSet bsa = fromBinaryString("001111000011");
        BitSet bsb = fromBinaryString("000011100101");

        BitSet expectedXor = fromBinaryString("001100100110");

        BitSet tgt = (BitSet) bsa.clone();
        tgt.xor(bsb);

        System.out.println("BSA: " + bsa);
        System.out.println("BSB: " + bsb);
        System.out.println("XXO: " + expectedXor);
        System.out.println("REA: " + tgt);

        Bits xo = xor(Bits.fromBitSet(bsa), Bits.fromBitSet(bsb));

        System.out.println("XOR: " + xo);

    }

    static BitSet fromBinaryString(String s) {
        BitSet bs = new BitSet(s.length());
        int cursor = 0;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '0':
                    cursor++;
                    continue;
                case '1':
                    bs.set(cursor++);
                    continue;
                default:
                // do nothing
            }
        }
        return bs;
    }

    static Bits xor(Bits a, Bits b) {
        if (a == b) {
            return Bits.EMPTY;
        }
        if (a.isEmpty()) {
            return b;
        } else if (b.isEmpty()) {
            return a;
        }
        RLEBitsBuilder rbb = new RLEBitsBuilder();

        long aix = a.leastSetBitLong();
        long bix = b.leastSetBitLong();
        boolean set = true;
        for (;;) {
            long min = min(aix, bix);
            long max = max(aix, bix);
            if (aix != bix) {
//                System.out.println((set ? "SET" : "UNS") +  "-MM " + aix + ", " + bix);
                boolean isA = min == bix;
                boolean reduced;
                if (isA) {
                    if (b.get(max) == set) {
                        max--;
                        reduced = true;
                    } else {
                        reduced = false;
                        if (a.get(max + 1) != b.get(max + 1)) {
                            max++;
                        }
                    }
                } else {
                    if (a.get(max) == set) {
                        reduced = true;
                        max--;
                    } else {
                        reduced = false;
                        if (a.get(max + 1) != b.get(max + 1)) {
                            max++;
                        }
                    }
                }
//                System.out.println("ADD " + min + "-" + max);
                rbb.withRange(min, max);
//                if (reduced) {
//                    System.out.println("REDU " + min + ", " + max);
//                }
            }
            long oaix = aix;
            long obix = bix;
            do {
                max++;
                set = !set;
                aix = set ? a.nextSetBitLong(max) : a.nextClearBitLong(max);
                bix = set ? b.nextSetBitLong(max) : b.nextClearBitLong(max);
                if (Math.min(aix, bix) == -1) {
                    break;
                }
            } while (aix == bix);
            if (aix == -1 && bix != -1) {
                rbb.withRange(oaix, Math.max(aix, bix));
                long last = b.previousSetBit(b.max());
                for (long i = max; i <= last; i++) {
                    if (a.get(i) != b.get(i)) {
                        rbb.withRange(i, i);
                    }
                }
//                System.out.println("A");
                // XXX run through the remaining set ranges in A and set
                // them on the result
                break;
            } else if (aix != -1 && bix == -1) {
                rbb.withRange(oaix, Math.max(aix, bix));
                long last = a.previousSetBit(a.max());
                for (long i = max; i <= last; i++) {
                    if (a.get(i) != b.get(i)) {
                        rbb.withRange(i, i);
                    }
                }
//                System.out.println("B");
                break;
            } else if (aix == -1 && bix == -1) {
//                System.out.println("C");
                break;
            }
            if (aix < oaix || bix < obix) {
//                System.out.println("D aix " + aix + " oaix " + oaix
//                        + " bix " + bix + " obix " + obix + " min " + min + " max " + max);
                break;
            }
        }
        return rbb.build();
    }

//    static Bits xor(Bits a, Bits b) {
//        if (a == b) {
//            return Bits.EMPTY;
//        }
//        if (a.isEmpty()) {
//            return b;
//        } else if (b.isEmpty()) {
//            return a;
//        }
//        RLEBitsBuilder rbb = new RLEBitsBuilder();
//
//        long minA = a.leastSetBitLong();
//        long minB = b.leastSetBitLong();
//
//        long maxA = a.previousSetBitLong(a.maxLong());
//        long maxB = b.previousSetBitLong(b.maxLong());
//
//        boolean clear = true;
//        long rangeStart = min(minA, minB);
//        long rangeEnd = max(minA, minB);
//
//        for (;;) {
//            if (minA != minB) {
//                rbb.withRange(rangeStart, rangeEnd);
//            } else {
//
//            }
//
//            clear = !clear;
//            break;
//        }
//
//        return rbb.build();
//    }
    static Bits and(Bits a, Bits b) {
        RLEBitsBuilder rbb = new RLEBitsBuilder();
        long leastPossible = max(a.leastSetBitLong(), b.leastSetBitLong());
        long greatestPossible = min(a.previousSetBitLong(a.maxLong()), b.previousSetBitLong(b.maxLong()));
        if (greatestPossible == -1) {
            return Bits.EMPTY;
        }
        a.visitRangesLong((first, last) -> {
            if (last >= leastPossible && first <= greatestPossible) {
                first = max(leastPossible, first);
                last = min(greatestPossible, last);
                long currStart = -1;
                long currEnd = -1;
                for (long l = first; l <= last; l++) {
                    boolean isSet = b.get(l);
                    if (isSet) {
                        if (currStart == -1) {
                            currStart = currEnd = l;
                        } else {
                            currEnd = l;
                        }
                    } else {
                        if (currStart != -1) {
                            rbb.withRange(currStart, currEnd);
                            currStart = currEnd = -1;
                        }
                    }
                }
                if (currStart != -1) {
                    rbb.withRange(currStart, currEnd);
                    currStart = currEnd = -1;
                }
            }
        });
        return rbb.build();
    }

    /**
     * Sub-builder which receives the final bit of a bit-range.
     */
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

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + (int) (this.first ^ (this.first >>> 32));
            hash = 23 * hash + (int) (this.last ^ (this.last >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BitRangeImpl other = (BitRangeImpl) obj;
            if (this.first != other.first) {
                return false;
            }
            return this.last == other.last;
        }

    }
}
