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

import com.mastfrog.bits.RLEB.BSResult;
import com.mastfrog.bits.RLEB.BSResultType;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import java.util.BitSet;
import java.util.Random;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Tim Boudreau
 */
public class RLEBTest {

    private final long[] data3 = new long[3];
    private final long[] data10 = new long[10];
    private final BitSet bits3 = new BitSet();
    private final BitSet bits10 = new BitSet(100);

    @Test
    public void testBinarySeachHeadMatchEvenSize() {
        for (int i = 1; i < 9; i++) {
            BSResult result = RLEB.binarySearch(data10, i, 0, data10.length - 1);
            assertEquals(0, result.index, "For " + i);
            assertSame(BSResultType.WITHIN_HEAD, result.type, "For " + i);
            assertTrue(result.isPresent(), "For " + i);
        }
        BSResult lastResult = RLEB.binarySearch(data10, 9, 0, data10.length - 1);
        assertEquals(0, lastResult.index);
        assertSame(BSResultType.HEAD_EXACT_END, lastResult.type);
        assertTrue(lastResult.isPresent());

        BSResult firstResult = RLEB.binarySearch(data10, 0, 0, data10.length - 1);
        assertEquals(0, firstResult.index);
        assertSame(BSResultType.HEAD_EXACT_START, firstResult.type);
        assertTrue(firstResult.isPresent());
    }

    @Test
    public void testBinarySeachTailMatchEvenSize() {
        for (int i = 901; i < 909; i++) {
            BSResult result = RLEB.binarySearch(data10, i, 0, data10.length - 1);
            assertSame(BSResultType.WITHIN_TAIL, result.type, "For " + i);
            assertEquals(9, result.index, "For " + i);
            assertTrue(result.isPresent(), "For " + i);
        }
        BSResult lastResult = RLEB.binarySearch(data10, 909, 0, data10.length - 1);
        assertEquals(9, lastResult.index);
        assertSame(BSResultType.TAIL_EXACT_END, lastResult.type);
        assertTrue(lastResult.isPresent());

        BSResult firstResult = RLEB.binarySearch(data10, 900, 0, data10.length - 1);
        assertEquals(9, firstResult.index);
        assertSame(BSResultType.TAIL_EXACT_START, firstResult.type);
        assertTrue(firstResult.isPresent());
    }

    @Test
    public void testBinarySeachMidMatchEvenSize() {
        for (int seg = 1; seg < 10; seg++) {
            int innerStart = (seg * 100);
            int innerLast = innerStart + 9;
            for (int i = innerStart + 1; i < innerLast; i++) {
                BSResult result = RLEB.binarySearch(data10, i, 0, data10.length - 1);
                assertTrue(result.type.isInner(), "For " + i + " @ " + seg);
                assertEquals(seg, result.index, "For " + i + " @ " + seg);
                assertTrue(result.isPresent(), "For " + i + " @ " + seg);
            }
            BSResult lastResult = RLEB.binarySearch(data10, innerLast, 0, data10.length - 1);
            assertEquals(seg, lastResult.index);
            assertTrue(lastResult.isPresent());
            assertFalse(lastResult.type.isInner());

            BSResult firstResult = RLEB.binarySearch(data10, innerStart, 0, data10.length - 1);
            assertEquals(seg, firstResult.index);
            assertTrue(firstResult.isPresent());
            assertFalse(firstResult.type.isInner());

            for (int i = innerLast + 1; i < innerStart + 100; i++) {
                BSResult absentResult = RLEB.binarySearch(data10, i, 0, data10.length - 1);
                assertFalse(absentResult.isPresent(), "Should not be present: " + i + " @ " + seg);
                int expectedIndex = i / 100;
                assertEquals(expectedIndex, absentResult.nearestMatch, "Wrong nearest less index for " + i + " @ " + seg);
                int expectedDistance = i - innerLast;
                assertEquals(expectedDistance, absentResult.nearestMatchDistance, "Wrong distance for " + i + " @ " + seg);
            }
        }
    }

    @Test
    public void testBinarySeachHeadMatchOddSize() {
        BSResult result1 = RLEB.binarySearch(data3, 11, 0, data3.length - 1);
        assertEquals(0, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 12, 0, data3.length - 1);
        assertEquals(0, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 13, 0, data3.length - 1);
        assertEquals(0, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        BSResult lastResult = RLEB.binarySearch(data3, 14, 0, data3.length - 1);
        assertEquals(0, lastResult.index);
        assertSame(BSResultType.HEAD_EXACT_END, lastResult.type);
        assertTrue(lastResult.isPresent());

        BSResult firstResult = RLEB.binarySearch(data3, 10, 0, data3.length - 1);
        assertEquals(0, firstResult.index);
        assertSame(BSResultType.HEAD_EXACT_START, firstResult.type);
        assertTrue(firstResult.isPresent());
    }

    @Test
    public void testBinarySeachMidMatchOddSize() {
        BSResult result1 = RLEB.binarySearch(data3, 26, 0, data3.length - 1);
        assertEquals(1, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 27, 0, data3.length - 1);
        assertEquals(1, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 28, 0, data3.length - 1);
        assertEquals(1, result1.index);
        assertSame(BSResultType.WITHIN_HEAD, result1.type);
        assertTrue(result1.isPresent());

        BSResult lastResult = RLEB.binarySearch(data3, 29, 0, data3.length - 1);
        assertEquals(1, lastResult.index);
        assertSame(BSResultType.HEAD_EXACT_END, lastResult.type);
        assertTrue(lastResult.isPresent());

        BSResult firstResult = RLEB.binarySearch(data3, 25, 0, data3.length - 1);
        assertEquals(1, firstResult.index);
        assertSame(BSResultType.HEAD_EXACT_START, firstResult.type);
        assertTrue(firstResult.isPresent());
    }

    @Test
    public void testBinarySeachEndMatchOddSize() {
        BSResult result1 = RLEB.binarySearch(data3, 62, 0, data3.length - 1);
        assertEquals(2, result1.index);
        assertSame(BSResultType.WITHIN_TAIL, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 67, 0, data3.length - 1);
        assertEquals(2, result1.index);
        assertSame(BSResultType.WITHIN_TAIL, result1.type);
        assertTrue(result1.isPresent());

        result1 = RLEB.binarySearch(data3, 77, 0, data3.length - 1);
        assertEquals(2, result1.index);
        assertSame(BSResultType.WITHIN_TAIL, result1.type);
        assertTrue(result1.isPresent());

        BSResult lastResult = RLEB.binarySearch(data3, 78, 0, data3.length - 1);
        assertEquals(2, lastResult.index);
        assertSame(BSResultType.TAIL_EXACT_END, lastResult.type);
        assertTrue(lastResult.isPresent());

        BSResult firstResult = RLEB.binarySearch(data3, 61, 0, data3.length - 1);
        assertEquals(2, firstResult.index);
        assertSame(BSResultType.TAIL_EXACT_START, firstResult.type);
        assertTrue(firstResult.isPresent());
    }

    @Test
    public void testBinarySearchPastEndOddSize() {

        BSResult result1 = RLEB.binarySearch(data3, 88, 0, data3.length - 1);

        assertSame(result1.type, BSResultType.NOT_PRESENT);
        assertFalse(result1.isPresent());
        assertEquals(2, result1.nearestMatch);
        assertEquals(10, result1.nearestMatchDistance);
    }

    @Test
    public void testBinarySearchMidOneOddSize() {
        for (int i = 15; i < 25; i++) {
            BSResult res = RLEB.binarySearch(data3, i, 0, data3.length - 1);
            assertSame(res.type, BSResultType.NOT_PRESENT, "For " + i);
            assertEquals(0, res.nearestMatch, "For " + i);
            assertEquals(i - 14, res.nearestMatchDistance, "For " + i);
        }
    }

    @Test
    public void testBinarySearchMidTwoOddSize() {
        for (int i = 30; i < 61; i++) {
            BSResult res = RLEB.binarySearch(data3, i, 0, data3.length - 1);
            assertSame(res.type, BSResultType.NOT_PRESENT, "For " + i);
            assertEquals(1, res.nearestMatch, "For " + i);
            assertEquals(i - 29, res.nearestMatchDistance, "For " + i);
        }
    }

    @Test
    public void testBinarySearchBeforeStartOddSize() {
        for (int i = 1; i < 2; i++) {
            BSResult res = RLEB.binarySearch(data3, i, 0, data3.length - 1);
            assertSame(res.type, BSResultType.NOT_PRESENT, "For " + i);
            assertEquals(0, res.nearestMatch, "For " + i);
            assertEquals(10 - i, res.nearestMatchDistance, "For " + i);
        }
    }

    @Test
    public void testExtractValues() {
        long st = RLEB.startFrom(data3[0]);
        long en = RLEB.endFrom(data3[0]);
        assertEquals(10L, st);
        assertEquals(14L, en);

        st = RLEB.startFrom(data3[1]);
        en = RLEB.endFrom(data3[1]);

        assertEquals(25L, st);
        assertEquals(29L, en);

        st = RLEB.startFrom(data3[2]);
        en = RLEB.endFrom(data3[2]);

        assertEquals(61L, st);
        assertEquals(78L, en);
    }

    @Test
    public void testCardinality() {
        RLEB rleb10 = new RLEB(data10, data10.length);
        assertEquals(bits10.cardinality(), rleb10.cardinality(), "Wrong cardinality for bits10");

        RLEB rleb3 = new RLEB(data3, data3.length);
        assertEquals(bits3.cardinality(), rleb3.cardinality(), "Wrong cardinality for bits3");
    }

    @Test
    public void testGet() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int bit = bits3.nextSetBit(0); bit >= 0; bit = bits3.nextSetBit(bit + 1)) {
            assertTrue(rleb3.get(bit), "B3 Should be set: " + bit);
        }

        RLEB rleb10 = new RLEB(data10, data10.length);
        for (int bit = bits10.nextSetBit(0); bit >= 0; bit = bits10.nextSetBit(bit + 1)) {
            assertTrue(rleb10.get(bit), "B10 Should be set: " + bit);
        }
    }

    @Test
    public void testLongAscending() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        BitSet bs3 = new BitSet(rleb3.cardinality());
        BitSet bs3p = new BitSet(rleb3.cardinality());
        long ct = rleb3.forEachLongSetBitAscending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3.set((int) bit);
        });
        assertEquals(bits3, bs3);
        assertEquals(rleb3.cardinality(), ct);

        ct = rleb3.forEachLongSetBitAscending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3p.set((int) bit);
            return true;
        });
        assertEquals(bits3, bs3p);
        assertEquals(rleb3.cardinality(), ct);

        RLEB rleb10 = new RLEB(data10, data10.length);
        BitSet bs10 = new BitSet(rleb3.cardinality());
        rleb10.forEachLongSetBitAscending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs10.set((int) bit);
        });
        assertEquals(bits10, bs10);
    }

    @Test
    public void testLongDescending() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        BitSet bs3 = new BitSet(rleb3.cardinality());
        BitSet bs3p = new BitSet(rleb3.cardinality());
        Lng prev = Lng.of(Long.MAX_VALUE);
        Int c = Int.create();
        rleb3.forEachLongSetBitDescending(bit -> {
            c.increment();
            if (prev.getAsLong() <= bit) {
                fail("Wrong direction at " + bit + " - prev bit was " + prev.getAsLong());
            }
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3.set((int) bit);
            prev.set(bit);
        });
        assertEquals(bits3, bs3);
        assertEquals(rleb3.cardinality(), c.getAsInt());

        prev.set(Long.MAX_VALUE);

        long ct = rleb3.forEachLongSetBitDescending(bit -> {
            if (prev.getAsLong() <= bit) {
                fail("Wrong direction at " + bit);
            }
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3p.set((int) bit);
            prev.set(bit);
            return true;
        });
        assertEquals(bits3, bs3p);
        assertEquals(rleb3.cardinality(), ct);

        RLEB rleb10 = new RLEB(data10, data10.length);
        BitSet bs10 = new BitSet(rleb3.cardinality());
        rleb10.forEachLongSetBitDescending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs10.set((int) bit);
        });
        assertEquals(bits10, bs10);
    }

    @Test
    public void testLongAscendingWithStart() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int start = 0; start < 81; start++) {
            BitSet bs3 = new BitSet();
            for (int bit = bits3.nextSetBit(start); bit >= 0; bit = bits3.nextSetBit(bit + 1)) {
                bs3.set(bit);
            }
            BitSet got = new BitSet();
            BitSet gotp = new BitSet();
            long ct = rleb3.forEachLongSetBitAscending(start, bit -> {
                got.set((int) bit);
            });
            long ct2 = rleb3.forEachLongSetBitAscending(start, bit -> {
                gotp.set((int) bit);
                return true;
            });
            assertEquals(bs3, got, "Starting from " + start);
            assertEquals(bs3, gotp, "Starting from " + start);
            assertEquals(bs3.cardinality(), (int) ct, "Starting from " + start);
            assertEquals(bs3.cardinality(), (int) ct2, "Starting from " + start);
        }
    }

    @Test
    public void testNextSetBit() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int i = 0; i < 81; i++) {
            int expect = bits3.nextSetBit(i);
            int got = rleb3.nextSetBit(i);

            assertEquals(expect, got, "For bit " + i);
        }

        RLEB rleb10 = new RLEB(data10, data10.length);
        long max = bits10.previousSetBit(Integer.MAX_VALUE);
        for (int i = 0; i < max + 2; i++) {
            int expect = bits10.nextSetBit(i);
            int got = rleb10.nextSetBit(i);
            assertEquals(expect, got, "For bit " + i);
        }
    }

    @Test
    public void testPrevSetBit() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int i = 81; i >= 0; i--) {
            int expect = bits3.previousSetBit(i);
            int got = rleb3.previousSetBit(i);

            assertEquals(expect, got, "For bit " + i);
        }

        RLEB rleb10 = new RLEB(data10, data10.length);
        long max = bits10.previousSetBit(Integer.MAX_VALUE);
        for (long i = max + 2; i >= 0; i--) {
            long expect = bits10.previousSetBit((int) i);
            long got = rleb10.previousSetBitLong(i);
            assertEquals(expect, got, "For bit " + i);
        }
    }

    @Test
    public void testLongDescending2() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        BitSet bs3 = new BitSet(rleb3.cardinality());
        BitSet bs3p = new BitSet(rleb3.cardinality());
        Lng cx = Lng.create();
        rleb3.forEachLongSetBitDescending(bit -> {
            cx.increment();
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3.set((int) bit);
        });
        long ct = cx.get();
        assertEquals(rleb3.cardinality(), ct);
        assertEquals(bits3, bs3);

        ct = rleb3.forEachLongSetBitDescending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs3p.set((int) bit);
            return true;
        });
        assertEquals(bits3, bs3p);
        assertEquals(rleb3.cardinality(), ct);

        RLEB rleb10 = new RLEB(data10, data10.length);
        BitSet bs10 = new BitSet(rleb3.cardinality());
        rleb10.forEachLongSetBitDescending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            bs10.set((int) bit);
        });
        assertEquals(bits10, bs10);
    }

    @Test
    public void testLongAscendingWithStartEnd() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int start = 0; start < 40; start++) {
            for (int end = 40; end < 80; end++) {
                BitSet bs3 = new BitSet();

                Bits w = Bits.fromBitSet(bits3);
                w.forEachSetBitAscending(start, end, bit -> {
                    bs3.set(bit);
                });
                BitSet got = new BitSet();
                BitSet gotp = new BitSet();
                long ct = rleb3.forEachLongSetBitAscending(start, end, bit -> {
                    got.set((int) bit);
                });
                long ct2 = rleb3.forEachLongSetBitAscending(start, end, bit -> {
                    gotp.set((int) bit);
                    return true;
                });
                assertEquals(bs3, got, "Starting from " + start + " to " + end);

                assertEquals(bs3, gotp, "Starting from " + start + " to " + end);
                assertEquals(bs3.cardinality(), (int) ct, "Starting from " + start + " to " + end);
                assertEquals(bs3.cardinality(), (int) ct2, "Starting from " + start + " to " + end);
            }
        }
    }

    @Test
    public void testBuilderAddingBits() {
        Random r = new Random(12345);
        BitSet bs = new BitSet();
        for (int i = 0; i < 112; i++) {
            int v = r.nextInt(256) * 2;
            bs.set(v);
            bs.set(v + 1);
        }
        Bits bits = Bits.fromBitSet(bs);
        RLEBitsBuilder rbb = RLEBitsBuilder.newRleBitsBuilder();
        RLEB rleb = (RLEB) rbb.add(bits).build();
        assertEquals(bits, rleb);
        assertEquals(bits.hashCode(), rleb.hashCode());
        assertEquals(bits.toString(), rleb.toString());

        rbb = RLEBitsBuilder.newRleBitsBuilder();
        rbb.add(rleb);
        assertEquals(rleb, rbb.build());
    }

    @Test
    public void testUnset() {
        BitSet bs1 = new BitSet();
        long realCount = Bits.fromBitSet(bits3).forEachUnsetBitAscending(bit -> {
            bs1.set(bit);
        });
        BitSet bs2 = new BitSet();
        RLEB rleb3 = new RLEB(data3, data3.length);
        long gotCount = rleb3.forEachUnsetBitAscending(bit -> {
            bs2.set(bit);
        });
        assertEquals(bs1, bs2, "Unset bit iteration does not match");
        assertEquals(Bits.fromBitSet(bits3).hashCode(), rleb3.hashCode());
        assertEquals(realCount, gotCount);
    }

    @Test
    public void testUnsetDescending() {
        BitSet bs1 = new BitSet();
        long realCount = Bits.fromBitSet(bits3).forEachUnsetBitAscending(bit -> {
            bs1.set(bit);
        });
        BitSet bs2 = new BitSet();
        RLEB rleb3 = new RLEB(data3, data3.length);
        long gotCount = rleb3.forEachUnsetBitDescending(bit -> {
            bs2.set(bit);
        });
        assertEquals(bs1, bs2, "Unset bit iteration does not match");
        assertEquals(realCount, gotCount);
    }

    @Test
    public void testUnsetWithStart() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int start = 0; start < 80; start++) {
            BitSet bs1 = new BitSet();
            BitSet bs2 = new BitSet();
            int ct = Bits.fromBitSet(bits3).forEachUnsetBitAscending(start, bit -> {
                bs1.set(bit);
            });
            int ct2 = rleb3.forEachUnsetBitAscending(start, bit -> {
                bs2.set(bit);
            });
            assertEquals(bs1, bs2, "Unset bit iteration does not match for " + start + ":\nA:" + bs1 + "\nB:" + bs2 + "\n");
            assertEquals(ct, ct2, "Returned bit counts doe not match for start " + start);
        }
    }

    @Test
    public void testUnsetWithStartAndEnd() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        for (int start = 0, end = 40; start <= 40 && end <= 80; start++, end++) {
            BitSet bs1 = new BitSet();
            BitSet bs2 = new BitSet();
            int ct = Bits.fromBitSet(bits3).forEachUnsetBitAscending(start, end, bit -> {
                bs1.set(bit);
            });
            int ct2 = rleb3.forEachUnsetBitAscending(start, end, bit -> {
                bs2.set(bit);
            });
            assertEquals(bs1, bs2, "Unset bit iteration does not match for " + start
                    + ":" + end + ":\nA:" + bs1 + "\nB:" + bs2 + "\n");
            assertEquals(ct, ct2, "Returned bit counts doe not match for start "
                    + start + " end " + end);
        }
    }

    @Test
    public void testFiltered() {
        RLEB rleb3 = new RLEB(data3, data3.length);

        IntPredicate filter = bit -> {
            return (bit / 2) % 2 == 0;
        };

        Bits expected = Bits.fromBitSet(bits3).filter(filter);
        Bits got = rleb3.filter(filter);
        assertEquals(expected, got);
    }

    @Test
    public void testGet2() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            boolean expected = bits3.get(i);
            boolean got = rleb3.get(i);
            if (expected != got) {
                sb.append(i).append("=").append(got).append("\n");
            }
        }
        if (!sb.isEmpty()) {
            fail(sb.toString());
        }
    }

    @Test
    public void testShifted() {
        RLEB rleb3 = new RLEB(data3, data3.length);
        assertFalse(rleb3.get(0));
        Bits bits = rleb3.shift(5);
        bits.forEachSetBitAscending(bit -> {
            long unshifted = bit - 5;
            assertTrue(rleb3.get(unshifted), "Bit " + bit + " unshifted " + unshifted);
        });

        bits.forEachUnsetBitAscending(bit -> {
            long unshifted = bit - 5;
            assertFalse(rleb3.get(unshifted), "Bit " + bit + " unshifted " + unshifted);
        });
    }

    @Test
    public void testLongSupplier() {
        Bits b1 = Bits.fromBitSet(bits3);
        RLEB rleb3 = new RLEB(data3, data3.length);
        LongSupplier canonical = b1.asLongSupplier();
        LongSupplier ours = rleb3.asLongSupplier();

        for (int i = 0;; i++) {
            long c = canonical.getAsLong();
            long o = ours.getAsLong();
            assertEquals(c, o, "LongSupplier bits differ at " + i + " for " + bits3);
            if (c < 0) {
                break;
            }
        }
    }

    @Test
    public void testGetRange() {
        Bits b1 = Bits.fromBitSet(bits3);
        RLEB rleb3 = new RLEB(data3, data3.length);
        assertEquals(b1.length(), rleb3.length(), "Lengths differ");
        for (int start = 0; start < (b1.length() / 2) - 1; start++) {
            for (int end = b1.length(); end >= (b1.length() / 2) + 1; end--) {
                int st = start;
                Bits expected = b1.get(start, end);
                expected.forEachSetBitAscending(bit -> {
                    // sanity check
                    assertTrue(b1.get(bit + st), "Subset has bit " + bit + " set which is not in " + b1);
                });
                Bits got = rleb3.get(start, end);
                assertTrue(got instanceof RLEB);
                assertEquals(expected, got, "Failed for range " + start + ":" + end);
            }
        }
    }

    @BeforeEach
    public void setup() {
        RLEB.write(data3, 0, 10, 14);
        RLEB.write(data3, 1, 25, 29);
        RLEB.write(data3, 2, 61, 78);
        for (int i = 10; i <= 14; i++) {
            bits3.set(i);
        }
        for (int i = 25; i <= 29; i++) {
            bits3.set(i);
        }
        for (int i = 61; i <= 78; i++) {
            bits3.set(i);
        }

        RLEB.write(data10, 0, 0, 9);
        RLEB.write(data10, 1, 100, 109);
        RLEB.write(data10, 2, 200, 209);
        RLEB.write(data10, 3, 300, 309);
        RLEB.write(data10, 4, 400, 409);
        RLEB.write(data10, 5, 500, 509);
        RLEB.write(data10, 6, 600, 609);
        RLEB.write(data10, 7, 700, 709);
        RLEB.write(data10, 8, 800, 809);
        RLEB.write(data10, 9, 900, 909);

        for (int i = 0; i < 10; i++) {
            int start = i * 100;
            int end = start + 10;
            for (int j = start; j < end; j++) {
                bits10.set(j);
            }
        }
    }
}