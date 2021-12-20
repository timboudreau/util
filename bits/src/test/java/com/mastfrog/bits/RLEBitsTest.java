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

import com.mastfrog.bits.RLEBits.Run;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class RLEBitsTest {

    @Test
    public void testRLE() {
        RLEBits b = new RLEBits();
        RLEBits n = new RLEBits();

        for (int i = 0; i < 50; i += 10) {
            for (int j = i; j < i + 5; j++) {
                b.set(j);
            }
        }
        n.set(0, 5);
        n.set(10, 15);
        n.set(20, 25);
        n.set(30, 35);
        n.set(40, 45);

        assertEquals("0-4, 10-14, 20-24, 30-34, 40-44", b.toString());
        assertEquals("0-4, 10-14, 20-24, 30-34, 40-44", n.toString());
        assertEquals(b, n);
        for (int i = 5; i <= 9; i++) {
            b.set(i);
        }
        n.set(5, 10);

        assertEquals("0-14, 20-24, 30-34, 40-44", b.toString());
        assertEquals("0-14, 20-24, 30-34, 40-44", n.toString());
        assertEquals(b, n);

        for (int i = 19; i >= 15; i--) {
            b.set(i);
        }
        n.set(15, 20);

        assertEquals("0-24, 30-34, 40-44", b.toString());
        assertEquals("0-24, 30-34, 40-44", n.toString());

        for (int i = 0; i < 24; i++) {
            assertTrue(b.get(i), i + " should be set");
            assertTrue(n.get(i), i + " should be set");
        }
        for (int i = 25; i < 30; i++) {
            assertFalse(b.get(i), i + " should not be set");
            assertFalse(n.get(i), i + " should not be set");
        }
        for (int i = 30; i < 35; i++) {
            assertTrue(b.get(i), i + " should be set");
            assertTrue(n.get(i), i + " should be set");
        }
        for (int i = 35; i < 40; i++) {
            assertFalse(b.get(i), i + " should be set");
            assertFalse(n.get(i), i + " should be set");
        }
        for (int i = 45; i < 100; i++) {
            assertFalse(b.get(i), i + " should be set");
            assertFalse(n.get(i), i + " should be set");
        }
        for (int i = 40; i < 45; i++) {
            assertTrue(b.get(i), i + " should be set");
            assertTrue(n.get(i), i + " should be set");
        }

        b.clear(10);
        b.clear(11);
        n.clear(10, 12);
        assertEquals("0-9, 12-24, 30-34, 40-44", b.toString());
        assertEquals("0-9, 12-24, 30-34, 40-44", n.toString());

        b.clear(40);
        b.clear(41);
        b.clear(42);
        b.clear(43);
        b.clear(44);
        n.clear(39, 46);

        assertEquals("0-9, 12-24, 30-34", b.toString());
        assertEquals("0-9, 12-24, 30-34", n.toString());
    }

    @Test
    public void testAsIntSupplier() {
        RLEBits bits = range();
        IntSupplier supp = bits.asIntSupplier();
        List<Integer> expect = new ArrayList<>();
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            int val = supp.getAsInt();
            found.add(val);
        }
        assertEquals(Arrays.asList(20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                -1, -1, -1), found);
    }

    @Test
    public void testNextSetBitOutOfRange() {
        RLEBits bits = range();
        assertEquals(49, bits.nextSetBit(49));
        assertEquals(-1, bits.nextSetBit(50));
        assertEquals(-1, bits.nextSetBit(51));
        assertEquals(-1, bits.nextSetBit(52));
        assertEquals(-1, bits.nextSetBit(53));
        assertEquals(-1, bits.nextSetBit(54));
    }

    @Test
    public void testHeadSet() {
        Run r = new Run(1);
        TreeSet<Run> foo = new TreeSet<>();
        foo.add(new Run(10, 20));
        foo.add(new Run(40, 50));
        foo.add(new Run(30, 35));

        Set<Run> headAbove = foo.headSet(new Run(30));
        Set<Run> tailAbove = foo.tailSet(new Run(30));

        Set<Run> headMid = foo.headSet(new Run(33));
        Set<Run> tailMid = foo.tailSet(new Run(33));

        Set<Run> headTail = foo.headSet(new Run(34));
        Set<Run> tailTail = foo.tailSet(new Run(34));

        System.out.println("HEAD ABOVE " + headAbove);
        System.out.println("TAIL ABOVE " + tailAbove);
        System.out.println("HEAD MID " + headMid);
        System.out.println("TAIL MID " + tailMid);
        System.out.println("HEAD TAIL " + headTail);
        System.out.println("TAIL TAIL " + tailTail);
    }

    @Test
    public void testForEachSetBitAscendingWithStart() {
        RLEBits bits = range();
        MutableBits canon = rangeCanon();
        compare("forEachSetBitAscending-with-start", canon, bits, b -> {
            Map<Integer, List<Integer>> result = new TreeMap<>();
            for (int i = 15; i < 55; i++) {
                List<Integer> curr = new ArrayList<>();
                result.put(i, curr);
                Int ct = Int.create();
                int ix = i;
                b.forEachSetBitAscending(i, (int bit) -> {
                    curr.add(bit);
                    if (ct.increment() > 60) {
                        fail("Iteration " + ix + " found way too many bits: " + curr);
                    }
                });
            }
            return result;
        });
    }

    @Test
    public void testForEachLongSetBitAscendingWithStart() {
        RLEBits bits = range();
        MutableBits canon = rangeCanon();
        compare("ForEachLongSetBitAscendingWithStart-with-start", canon, bits, b -> {
            Map<Long, List<Long>> result = new TreeMap<>();
            for (long i = 15; i < 55; i++) {
                List<Long> curr = new ArrayList<>();
                result.put(i, curr);
                Lng ct = Lng.create();
                long ix = i;
                b.forEachLongSetBitAscending(i, (long bit) -> {
                    curr.add(bit);
                    if (ct.increment() > 60) {
                        fail("Iteration " + ix + " found way too many bits: " + curr);
                    }
                });
            }
            return result;
        });
    }

    @Test
    public void testForEachSetBitAscendingWithStartAndEnd() {
        if (true) {
            new Error("FIXME").printStackTrace();
            return;
        }
        RLEBits bits = range();
        MutableBits canon = rangeCanon();
        compare("forEachSetBitAscending-with-start", canon, bits, b -> {
            Map<Integer, List<Integer>> result = new TreeMap<>();
            for (int i = 15; i < 52; i++) {
                List<Integer> curr = new ArrayList<>();
                result.put(i, curr);
                Int ct = Int.create();
                int ix = i;
                b.forEachSetBitAscending(i, i + 10, (int bit) -> {
                    curr.add(bit);
                    if (ct.increment() > 60) {
                        fail("Iteration " + ix + " found way too many bits in a "
                                + b.getClass().getName() + ":" + curr);
                    }
                });
            }
            return result;
        });
    }

    @Test
    public void testForEachLongSetBitAscendingWithStartAndEnd() {
        if (true) {
            new Error("FIXME").printStackTrace();
            return;
        }
        RLEBits bits = range();
        MutableBits canon = rangeCanon();
        compare("ForEachLongSetBitAscendingWithStartAndEnd", canon, bits, b -> {
            Map<Long, List<Long>> result = new TreeMap<>();
            for (long i = 15; i < 52; i++) {
                List<Long> curr = new ArrayList<>();
                result.put(i, curr);
                Int ct = Int.create();
                long ix = i;
                b.forEachLongSetBitAscending(i, i + 10, (long bit) -> {
                    curr.add(bit);
                    if (ct.increment() > 60) {
                        fail("Iteration " + ix + " found way too many bits in a "
                                + b.getClass().getSimpleName() + ":" + curr);
                    }
                });
            }
            return result;
        });
    }

    private <T extends Map<V, List<V>>, V> void compare(String name, MutableBits a, MutableBits b, Function<MutableBits, T> f) {
        T aa = f.apply(a);
        T bb = f.apply(b);
        if (!Objects.equals(aa, bb)) {
            Set<V> keysA = aa.keySet();
            Set<V> keysB = bb.keySet();
            if (!keysA.equals(keysB)) {
                fail("KeySets differ: \n" + keysA + "\n" + keysB);
            }
            StringBuilder sb = new StringBuilder("Different results for ").append(name).append("\n");
            for (V k : keysA) {
                List<V> valsA = aa.get(k);
                List<V> valsB = bb.get(k);
                if (!Objects.equals(valsA, valsB)) {
                    sb.append(k).append("-exp: ").append(valsA).append('\n');
                    sb.append(k).append("-got: ").append(valsB).append('\n');
                }
            }
            fail(sb.toString());
            System.out.println("VALUES OK");
        }
        assertEquals(aa, bb, name + " result differs - A:\n" + aa + " vs B:\n" + b);
    }

    @Test
    public void testNextSetBit() {
        RLEBits bits = range();
        for (int i = 0; i <= 20; i++) {
            assertEquals(20, bits.nextSetBit(i), "Wrong next bit at " + i);
            assertEquals(20L, bits.nextSetBitLong(i), "Wrong next bit at " + i);
        }
        for (int i = 21; i < 30; i++) {
            assertEquals(i, bits.nextSetBit(i), "Wrong next bit at " + i);
            assertEquals((long) i, bits.nextSetBitLong(i), "Wrong next bit at " + i);
        }
        for (int i = 30; i <= 40; i++) {
            assertEquals(40, bits.nextSetBit(i), "Wrong next bit at " + i);
            assertEquals(40L, bits.nextSetBitLong(i), "Wrong next bit at " + i);
        }
        for (int i = 40; i < 50; i++) {
            assertEquals(i, bits.nextSetBit(i), "Wrong next bit at " + i);
            assertEquals((long) i, bits.nextSetBitLong(i), "Wrong next bit at " + i);
        }
        for (int i = 50; i < 60; i++) {
            assertEquals(-1, bits.nextSetBit(i), "Wrong next bit at " + i);
            assertEquals(-1L, bits.nextSetBitLong(i), "Wrong next bit at " + i);
        }
    }

    @Test
    public void testPrevSetBit() {
        RLEBits bits = range();
        for (int i = 0; i < 20; i++) {
            long il = (long) i;
            assertEquals(-1, bits.previousSetBit(i), "Wrong prev bit at " + i);
            assertEquals(-1L, bits.previousSetBitLong(il), "Wrong prev bit at " + i);
        }
        for (int i = 20; i < 30; i++) {
            long li = i;
            assertEquals(i, bits.previousSetBit(i), "Wrong prev bit at " + i);
            assertEquals((long) i, bits.previousSetBitLong(li), "Wrong prev bit at " + i);
        }
        for (int i = 30; i < 40; i++) {
            assertEquals(29, bits.previousSetBit(i), "Wrong prev bit at " + i);
            assertEquals(29L, bits.previousSetBitLong(i), "Wrong prev bit at " + i);
        }
        for (int i = 40; i < 50; i++) {
            assertEquals(i, bits.previousSetBit(i), "Wrong prev bit at " + i);
            assertEquals((long) i, bits.previousSetBitLong(i), "Wrong prev bit at " + i);
        }
        for (int i = 50; i <= 60; i++) {
            assertEquals(49, bits.previousSetBit(i), "Wrong prev bit at " + i);
            assertEquals(49L, bits.previousSetBitLong(i), "Wrong prev bit at " + i);
        }
    }

    @Test
    public void testOr() {
        RLEBits bits = range();
        RLEBits other = new RLEBits();
        other.set(17, 23);
        other.set(34, 36);
        other.set(44);
        other.set(50, 52);

        RLEBits old = bits.copy();
        bits.or(other);
    }

    @Test
    public void testAscendingUnset() {
        RLEBits bits = range();
        Int ct = Int.create();
        bits.forEachUnsetLongBitAscending(0, 50, bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            if ((bit >= 20 && bit < 30) || (bit < 50 && bit >= 40)) {
                fail("Bit " + bit + " is part of " + bits);
            }
            ct.increment();
        });
        assertEquals(20, ct.getAsInt());
    }

    @Test
    public void testAscending() {
        RLEBits bits = range();
        bits.forEachLongSetBitAscending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            if (bit < 20 || (bit >= 30 && bit < 40) || (bit > 49)) {
                fail("Bit " + bit + " is not part of " + bits);
            }
        });
    }

    @Test
    public void testDescending() {
        RLEBits bits = range();
        bits.forEachLongSetBitDescending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            if (bit < 20 || (bit >= 30 && bit < 40) || (bit > 49)) {
                fail("Bit " + bit + " is not part of " + bits);
            }
        });

        bits.forEachSetBitDescending(bit -> {
            assertTrue(bit >= 0, "Negative bit " + bit);
            if (bit < 20 || (bit >= 30 && bit < 40) || (bit > 49)) {
                fail("Bit " + bit + " is not part of " + bits);
            }
        });
    }

    @Test
    public void testAddBetweenRanges() {
        RLEBits bits = range();
        bits.set(35);
        assertEquals("20-29, 35, 40-49", bits.toString());
    }

    @Test
    public void testAddSingleGroupBetweenRanges() {
        RLEBits bits = range();
        bits.set(35, 36);
        assertEquals("20-29, 35, 40-49", bits.toString());
    }

    @Test
    public void testAddWiderGroupBetweenRanges() {
        RLEBits bits = range();
        bits.set(34, 38);
        assertEquals("20-29, 34-37, 40-49", bits.toString());
    }

    @Test
    public void testAddLeftSpanningRange() {
        RLEBits bits = range();
        bits.set(19, 31);
        assertEquals("19-30, 40-49", bits.toString());
    }

    @Test
    public void testAddLeftTightSpanningRange() {
        RLEBits bits = range();
        bits.set(20, 32);
        assertEquals("20-31, 40-49", bits.toString());
    }

    @Test
    public void testAddBiSpanningRange() {
        RLEBits bits = range();
        bits.set(18, 51);
        assertEquals("18-50", bits.toString());
    }

    @Test
    public void testAddLeftOverlappingGroupBetweenRanges() {
        RLEBits bits = range();
        bits.set(28, 38);
        assertEquals("20-37, 40-49", bits.toString());
    }

    @Test
    public void testAddLeftAbuttingBetweenRanges() {
        RLEBits bits = range();
        bits.set(30, 38);
        assertEquals("20-37, 40-49", bits.toString());
    }

    @Test
    public void testAddRightOverlappingRanges() {
        RLEBits bits = range();
        bits.set(34, 42);
        assertEquals("20-29, 34-49", bits.toString());
    }

    @Test
    public void testAddRightAbuttingRanges() {
        RLEBits bits = range();
        bits.set(34, 40);
        assertEquals("20-29, 34-49", bits.toString());
    }

    private RLEBits range() {
        RLEBits bits = new RLEBits();
        bits.set(20, 30);
        bits.set(40, 50);
        assertEquals("20-29, 40-49", bits.toString());
        return bits;
    }

    private MutableBits rangeCanon() {
        MutableBits bits = MutableBits.create(60);
        bits.set(20, 30);
        bits.set(40, 50);
        return bits;
    }
}
