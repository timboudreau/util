/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ArraysManagerTest {

    ArraysManager.Longs l;

    @Before
    public void setup() {
        l = create(5);
    }

    private ArraysManager.Longs create(int batchSize) {
        l = new ArraysManager.Longs(batchSize);
        l.clear();
        l.addOne();
        l.addOne();
        l.addOne();
        long[] a = l.get(0);
        long[] b = l.get(1);
        long[] c = l.get(2);
        for (int i = 0; i < batchSize; i++) {
            a[i] = i;
            b[i] = batchSize + i;
            c[i] = (batchSize * 2) + i;
        }
        return l;
    }

    @Test
    public void testShiftFinalArrayFromZeroDoesNotLoopEndlessly() {
        long[] exp = trimIt(shiftArray(3, 3));
        l.shiftOneArrayRightFromZero(3, 3, false, true);
        check2(exp, 5);
    }

    @Test
    public void testEvenBatchSizesShiftCorrectly() {
        l = create(10);
        long[] exp = trimIt(shiftArray(2, 3, 10));
        l.shiftOneArrayRightFromZero(2, 3, false, true);
        check2(exp, 10);
    }

    @Test
    public void testShiftExhaustive() {
        int ix = 0;
        for (int batch = 5; batch < 11; batch++) {
            for (int start = 0; start < batch * 3; start++) {
                for (int len = 1; len <= batch * 3; len++) {
                    l = create(batch);
                    long[] exp = trimIt(shifted(start, len, batch));
                    String msg = ix++ + ". batch " + batch + " start " + start + " len " + len
                            + " expected " + ts(exp);
                    try {
                        int arrayIndex = start / batch;
                        int at = start - (arrayIndex * batch);
                        l.shiftOneArrayRight(arrayIndex, at, len);
                    } catch (Throwable t) {
                        String m = t.getMessage() == null ? msg : t.getMessage() + "\n" + msg;
                        throw new AssertionError(m, t);
                    }
                    check2(msg, exp, batch);
                }
            }
        }
    }

    @Test
    public void testShiftRightFromZero() {
        long[] targ = l.shiftOneArrayRight(1, 0, 2);
        targ[0] = -1;
        targ[1] = -2;
        check(new long[]{0, 1, 2, 3, 4, -1, -2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 0, 0});
    }

    @Test
    public void testShiftRightFromZeroByBatchSize() {
        l.shiftOneArrayRight(1, 0, 5);
        check(new long[]{0, 1, 2, 3, 4, -1, -1, -1, -1, -1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});
    }

    @Test
    public void testShiftRightFromOne() {
        long[] targ = l.shiftOneArrayRight(1, 1, 2);
        targ[1] = -1;
        targ[2] = -2;
        check(new long[]{0, 1, 2, 3, 4, 5, -1, -2, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 0, 0});
    }

    @Test
    public void testShiftRightFromTwo() {
        long[] targ = l.shiftOneArrayRight(1, 2, 2);
        targ[2] = -1;
        targ[3] = -2;
        check(new long[]{0, 1, 2, 3, 4, 5, 6, -1, -2, 7, 8, 9, 10, 11, 12, 13, 14, 0, 0, 0});

    }

    @Test
    public void testShiftRightFromThree() {
        long[] targ = l.shiftOneArrayRight(1, 3, 2);
        targ[3] = -1;
        targ[4] = -2;
        check(new long[]{0, 1, 2, 3, 4, 5, 6, 7, -1, -2, 8, 9, 10, 11, 12, 13, 14, 0, 0, 0});
    }

    @Test
    public void testShiftRightFromFour() {
        long[] targ = l.shiftOneArrayRight(1, 4, 2);
        targ[4] = -1;
        l.get(2)[0] = -2;
        check(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -2, 9, 10, 11, 12, 13, 14, 0, 0, 0});
    }

    @Test
    public void testShiftRightByFour() {
        long[] targ = l.shiftOneArrayRight(1, 0, 4);
        targ[0] = -1;
        targ[1] = -2;
        targ[2] = -3;
        targ[3] = -4;
        check(new long[]{0, 1, 2, 3, 4, -1, -2, -3, -4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0});
    }

    @Test
    public void testShiftOneArrayRightFromZeroK() throws Exception {
        l.shiftOneArrayRightFromZero(0, 3, false, true);
        check(shiftArray(0, 3));
    }

    @Test
    public void testShiftOneArrayRightFromZeroJ() throws Exception {
        l.shiftOneArrayRightFromZero(1, 17, false, true);
        check(shiftArray(1, 17));
    }

    @Test
    public void testShiftOneArrayRightFromZeroI() throws Exception {
        l.shiftOneArrayRightFromZero(1, 16, false, true);
        check(shiftArray(1, 16));
    }

    @Test
    public void testShiftOneArrayRightFromZeroH() throws Exception {
        l.shiftOneArrayRightFromZero(1, 15, false, true);
        check(shiftArray(1, 15));
    }

    @Test
    public void testShiftOneArrayRightFromZeroG() throws Exception {
        l.shiftOneArrayRightFromZero(1, 7, false, true);
        check(shiftArray(1, 7));
    }

    @Test
    public void testShiftOneArrayRightFromZeroF() throws Exception {
        l.shiftOneArrayRightFromZero(1, 6, false, true);
        check(shiftArray(1, 6));
    }

    @Test
    public void testShiftOneArrayRightFromZeroE() throws Exception {
        l.shiftOneArrayRightFromZero(1, 5, false, true);
        check(shiftArray(1, 5));
    }

    @Test
    public void testShiftOneArrayRightFromZeroD() throws Exception {
        l.shiftOneArrayRightFromZero(1, 4, false, true);
        check(shiftArray(1, 4));
    }

    @Test
    public void testShiftOneArrayRightFromZeroC() throws Exception {
        l.shiftOneArrayRightFromZero(1, 2, false, true);
        check(shiftArray(1, 2));
    }

    @Test
    public void testShiftOneArrayRightFromZeroB() throws Exception {
        l.shiftOneArrayRightFromZero(1, 1, false, true);
        check(shiftArray(1, 1));
    }

    @Test
    public void testShiftOneArrayRightFromZeroA() throws Exception {
        l.shiftOneArrayRightFromZero(1, 3, false, true);
        check(shiftArray(1, 3));
    }

    @Test
    public void testIsSorted() throws Exception {
        long[] val = new long[]{1, 2, 3, 4, 5};
        assertTrue(Arrays.toString(val) + " should be sorted", l.isSorted(val));
        val = new long[]{1, 2, 3, 4, 5, 6};
        assertTrue(Arrays.toString(val) + " should be sorted", l.isSorted(val));

        val = new long[]{1, 2, 3, 4, 5, 0};
        assertFalse(Arrays.toString(val) + " should not be sorted", l.isSorted(val));

        val = new long[]{1, 2, 3, 4, 0};
        assertFalse(Arrays.toString(val) + " should not be sorted", l.isSorted(val));

        val = new long[]{1, 0};
        assertFalse(Arrays.toString(val) + " should not be sorted", l.isSorted(val));

        val = new long[]{0, 1};
        assertTrue(Arrays.toString(val) + " should be sorted", l.isSorted(val));

        val = new long[]{1};
        assertTrue(Arrays.toString(val) + " single element array is sorted", l.isSorted(val));

        val = new long[0];
        assertTrue(Arrays.toString(val) + " empty array is sorted", l.isSorted(val));
    }

    private static long[] shiftArray(int fiveIndex, int by) {
//        return shifted(fiveIndex * 5, by);
        return shiftArray(fiveIndex, by, 5);
    }

    private static long[] shiftArray(int batchIndex, int by, int batchSize) {
        return shifted(batchIndex * batchSize, by, batchSize);
    }

    private static long[] shifted(int pos, int by) {
        return shifted(pos, by, 5);
    }

    private static long[] trimIt(long[] l) {
        int end = l.length - 1;
        while (l[end] <= 0) {
            end--;
        }
        if (end != l.length - 1) {
            return Arrays.copyOf(l, end + 1);
        }
        return l;
    }

    private static long[] shifted(int pos, int by, int batchSize) {
        List<Long> longs = new ArrayList<>();
        for (long i = 0; i < batchSize * 3; i++) {
            if (i == pos) {
                for (int j = 0; j < by; j++) {
                    longs.add((long) -1);
                }
            }
            longs.add(i);
        }
        while (longs.size() % batchSize != 0) {
            longs.add(0L);
        }
        return ArrayUtils.toPrimitiveArray(longs.toArray(new Long[0]));
    }

    void check(long[] expect) {
        String msg = "EXP " + ts(expect) + "\nGOT "
                + ts(l.toLongArray()) + "\n";
        assertArrayEquals(msg, expect, l.toLongArray());
    }

    void check2(long[] expect, int batchSize) {
        check2(null, expect, batchSize);
    }

    void check2(String ms, long[] expect, int batchSize) {
        long[] got = trimIt(l.toLongArray());
        String msg = "EXP " + ts(expect, batchSize) + "\nGOT "
                + ts(got, batchSize) + (ms != null ? "\n" + ms : "") + "\n";
        expect = trimIt(expect);
        if (!Arrays.equals(expect, got)) {
            System.out.println(msg);
        }
        assertArrayEquals(msg, expect, got);
    }

    private static String ts(long[] l) {
        return ts(l, 5);
    }

    private static String ts(long[] l, int batchSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.length; i++) {
            if (i % batchSize == 0) {
                sb.append(" | ");
            }
            String s = Long.toString(l[i]);
            if (s.length() == 1) {
                s = " " + s;
            }
            sb.append(s);
            if (i != l.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
