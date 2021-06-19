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
package com.mastfrog.concurrent;

import static com.mastfrog.concurrent.AtomicIntegerPair.pack;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class AtomicIntegerPairArrayTest {

    private static final int SIZE = 32;
    AtomicIntegerPairArray arr;

    @BeforeEach
    public void setup() {
        arr = AtomicIntegerPairArray.create(SIZE);
        int[] contents = new int[SIZE * 2];
        long[] longContents = new long[SIZE];
        for (int i = 0; i < SIZE; i++) {
            int first = (i + 1) * 37;
            int second = (i + 1) * 11;
            arr.set(i, first, second);
            contents[i * 2] = first;
            contents[(i * 2) + 1] = second;
            longContents[i] = pack(first, second);
        }
        assertArrayEquals(contents, arr.toIntArray());
        assertArrayEquals(longContents, arr.toLongArray());
        check();
    }

    private void check() {
        for (int i = 0; i < SIZE; i++) {
            int first = (i + 1) * 37;
            int second = (i + 1) * 11;
            int ix = i;
            assertEquals(first, arr.left(i), () -> "Wrong left value at " + ix + " in " + arr);
            assertEquals(second, arr.right(i), () -> "Wrong right value at " + ix + " in " + arr);
            arr.fetch(i, (l, r) -> {
                assertEquals(first, l, () -> "Wrong fetched left value at " + ix + " in " + arr);
                assertEquals(second, r, () -> "Wrong fetched right value at " + ix + " in " + arr);
            });
        }
    }

    @Test
    public void testCopy() {
        AtomicIntegerPairArray arr2 = arr.copy();
        assertNotNull(arr2);
        assertEquals(arr.size(), arr2.size());
        for (int i = 0; i < arr.size(); i++) {
            int l = arr.left(i);
            int r = arr.right(i);
            assertEquals(l, arr2.left(i));
            assertEquals(r, arr2.right(i));
        }
    }

    @Test
    public void testCopyOfRange() {
    }

    @Test
    public void testSetFirst() {
        arr.setFirst(-23, -42, (l, r) -> {
            return l == 37 * 5;
        });
        assertEquals(-23, arr.left(4));
        assertEquals(-42, arr.right(4));
    }

    @Test
    public void testFill() {
        arr.fill(29, 51);
        for (int i = 0; i < arr.size(); i++) {
            assertEquals(29, arr.left(i));
            assertEquals(51, arr.right(i));
        }
    }

    @Test
    public void testSize() {
        assertEquals(23, AtomicIntegerPairArray.create(23).size());
        assertEquals(11, AtomicIntegerPairArray.from(new int[22]).size());
        assertEquals(41, AtomicIntegerPairArray.from(new long[41]).size());
    }

    @Test
    public void testSetLeft() {
        for (int i = 0; i < arr.size(); i++) {
            arr.setLeft(i, -i * 101);
        }
        for (int i = 0; i < arr.size(); i++) {
            int first = -i * 101;
            int second = (i + 1) * 11;
            assertEquals(first, arr.left(i));
            assertEquals(second, arr.right(i));
            arr.fetch(i, (l, r) -> {
                assertEquals(first, l);
                assertEquals(second, r);
            });
            int[] parts = arr.toIntArray();
            assertEquals(parts[i * 2], first);
            assertEquals(parts[(i * 2) + 1], second);
        }
    }

    @Test
    public void testSetRight() {
        for (int i = 0; i < arr.size(); i++) {
            arr.setRight(i, (i + 1) * -323);
        }
        for (int i = 0; i < arr.size(); i++) {
            int first = (i + 1) * 37;
            int second = (i + 1) * -323;
            assertEquals(first, arr.left(i));
            assertEquals(second, arr.right(i));
            arr.fetch(i, (l, r) -> {
                assertEquals(first, l);
                assertEquals(second, r);
            });
            int[] parts = arr.toIntArray();
            assertEquals(parts[i * 2], first);
            assertEquals(parts[(i * 2) + 1], second);
        }
    }

    @Test
    public void testUpdate() {
        arr.update(7, old -> old * 10, old -> old * -101);
        assertEquals(8 * 37 * 10, arr.left(7));
        assertEquals(8 * 11 * -101, arr.right(7));
    }

    @Test
    public void testUpdateWithUpdater() {
        for (int i = arr.size() - 1; i >= 0; i--) {
            int ix = i;
            arr.update(ix, (l, r, upd) -> {
                upd.accept(r * -30, l * -401);
            });
        }
        for (int i = 0; i < arr.size() - 1; i++) {
            int first = (i + 1) * 11 * -30;
            int second = (i + 1) * 37 * -401;
            assertEquals(first, arr.left(i));
            assertEquals(second, arr.right(i));
        }
    }

    @Test
    public void testCompareAndSet() {
        assertFalse(arr.compareAndSet(7, 1, 1, 100, 102));
        assertTrue(arr.compareAndSet(7, 8 * 37, 8 * 11, 207, 801));
        assertEquals(207, arr.left(7));
        assertEquals(801, arr.right(7));
    }

    @Test
    public void testSet() {
        for (int i = 0; i < arr.size(); i++) {
            arr.set(i, i, (i + 1) * 10);
            assertEquals(i, arr.left(i));
            assertEquals((i + 1) * 10, arr.right(i));
        }
    }

    @Test
    public void testEach() {
        List<int[]> lrs = new ArrayList<>(arr.size());
        int ct = arr.each((l, r) -> {
            lrs.add(new int[]{l, r});
            return true;
        });
        assertEquals(SIZE - 1, ct, "Wrong size returned by each");
        assertEquals(arr.size() - 1, ct);
        assertEquals(arr.size(), lrs.size());
        for (int i = 0; i < arr.size(); i++) {
            int[] pair = lrs.get(i);
            assertEquals((i + 1) * 37, pair[0]);
            assertEquals((i + 1) * 11, pair[1]);
        }

        List<int[]> lr2 = new ArrayList<>(arr.size());
        int ct2 = arr.each((l, r) -> {
            lr2.add(new int[]{l, r});
            return lr2.size() < 8;
        });
        assertEquals(7, ct2);
        assertEquals(8, lr2.size());
    }

    @Test
    public void testPairView() {
        List<IntegerPair> pairs = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            pairs.add(arr.pairView(i));
        }
        for (int i = 0; i < arr.size(); i++) {
            int first = (i + 1) * 37;
            int second = (i + 1) * 11;
            IntegerPair pair = pairs.get(i);
            assertEquals(first, pair.left());
            assertEquals(second, pair.right());
            pair.set(second * 100, first * -100);
        }
        for (int i = 0; i < arr.size(); i++) {
            int first = (i + 1) * 11 * 100;
            int second = (i + 1) * 37 * -100;
            IntegerPair pair = pairs.get(i);
            assertEquals(first, pair.left());
            assertEquals(second, pair.right());
            assertEquals(first, arr.left(i));
            assertEquals(second, arr.right(i));
        }
        List<UnsignedView> uns = new ArrayList<>(arr.size());
        for (IntegerPair ip : pairs) {
            UnsignedView unsigned = ip.toUnsignedView();
            uns.add(unsigned);
            unsigned.set(UnsignedView.MAX_VALUE - Math.abs(ip.left()), UnsignedView.MAX_VALUE - Math.abs(ip.right()));
        }
        for (int i = 0; i < arr.size(); i++) {
            UnsignedView v = uns.get(i);
            int first = (i + 1) * 11 * 100;
            int second = (i + 1) * 37 * -100;
            long left = UnsignedView.MAX_VALUE - first;
            long right = UnsignedView.MAX_VALUE - Math.abs(second);
            assertEquals(left, v.left());
            assertEquals(right, v.right());
        }
    }
}
