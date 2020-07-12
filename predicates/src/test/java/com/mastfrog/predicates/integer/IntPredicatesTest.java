/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
package com.mastfrog.predicates.integer;

import java.util.Arrays;
import java.util.function.IntPredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntPredicatesTest {

    @Test
    public void testEquality() {
        int[] ints = new int[]{5, 10, 15, 20, 25, 30};
        EnhIntPredicate eh = new BitSetIntPredicate(ints);
        EnhIntPredicate arr = new ArrayPredicate(ints, false);
        for (int i = 0; i < ints.length; i++) {
            int val = ints[i];
            assertTrue(eh.test(val));
            assertFalse(eh.test(val + 1));
            assertFalse(eh.test(val - 1));
        }
        assertFalse(eh.test(-5));
        assertEquals(eh, arr);
        assertEquals(arr, eh);
        assertEquals(eh.hashCode(), arr.hashCode());
        for (int i = 0; i < 35; i++) {
            assertEquals(Integer.toString(i), eh.test(i), arr.test(i));
        }
        EnhIntPredicate eNeg = eh.negate();
        EnhIntPredicate aNeg = arr.negate();
        for (int i = 0; i < 35; i++) {
            assertEquals(Integer.toString(i), eNeg.test(i), aNeg.test(i));
        }
        assertEquals(eNeg, aNeg);
        assertEquals(aNeg, eNeg);
        assertEquals(eNeg.hashCode(), aNeg.hashCode());
    }

    @Test
    public void testConstants() {
        assertTrue(IntPredicates.alwaysTrue().test(1));
        assertFalse(IntPredicates.alwaysFalse().test(1));
        assertFalse(IntPredicates.alwaysFalse().and(IntPredicates.alwaysTrue()).test(1));
        assertFalse(IntPredicates.alwaysTrue().and(IntPredicates.alwaysFalse()).test(1));
        assertTrue(IntPredicates.alwaysTrue().and(IntPredicates.alwaysTrue()).test(1));
        assertTrue(IntPredicates.alwaysTrue().or(IntPredicates.alwaysTrue()).test(1));
        assertTrue(IntPredicates.alwaysFalse().or(IntPredicates.alwaysTrue()).test(1));
        assertTrue(IntPredicates.alwaysTrue().or(IntPredicates.alwaysFalse()).test(1));
        assertFalse(IntPredicates.nonZero().test(0));
        assertTrue(IntPredicates.nonZero().test(1));
        assertTrue(IntPredicates.nonZero().test(-1));
        EnhIntPredicate gt = IntPredicates.greaterThan(10);
        EnhIntPredicate lt = IntPredicates.lessThan(10);
        for (int i = 0; i < 20; i++) {
            if (i > 10) {
                assertTrue(gt.test(i));
            } else {
                assertFalse(gt.test(i));
            }
            if (i < 10) {
                assertTrue(lt + " " + i, lt.test(i));
            } else {
                assertFalse(lt.test(i));
            }
        }
    }

    @Test
    public void testFixed() {
        IntPredicate p = FixedIntPredicate.INT_FALSE.and(FixedIntPredicate.INT_TRUE);
        assertFalse(p.test(0));

        p = FixedIntPredicate.INT_FALSE.or(FixedIntPredicate.INT_TRUE);
        assertTrue(p.test(0));
    }

    @Test
    public void testArraysCombine() {
        ArrayPredicate p1 = new ArrayPredicate(new int[]{1, 2, 3, 4}, false);
        ArrayPredicate p2 = new ArrayPredicate(new int[]{1, 2, 3, 4, 5}, false);
        IntPredicate comb = p1.or(p2);
        assertTrue(comb instanceof ArrayPredicate);
        for (int i = 1; i < 5; i++) {
            assertTrue(comb + " returns false for " + i, comb.test(i));
        }
        assertFalse(comb.test(6));
        assertFalse(comb.test(0));

        assertSame(p1, p1.or(new SinglePredicate(false, 3)));

        comb = p1.or(new SinglePredicate(false, 5));
        assertTrue(comb instanceof ArrayPredicate);

        for (int i = 1; i < 5; i++) {
            assertTrue(comb.test(i));
        }
        assertFalse(comb.test(6));
        assertFalse(comb.test(0));
    }

    @Test
    public void testGeneral() {
        testRange(10, 11, 12);
        testRange(10, 11, 12, 13, 15, 21);
    }

    private void testRange(int a, int... bs) {
        IntPredicate pred = IntPredicates.anyOf(a, bs);
        testRange(pred, a, bs);
        IntPredicate pred2 = IntPredicates.anyOf(IntPredicates.combine(a, bs));
        testRange(pred2, a, bs);
        IntPredicate pred3 = IntPredicates.noneOf(a, bs);
        IntPredicate pred4 = pred3.negate();
        for (int i = 0; i < 100; i++) {
            if (i == a || Arrays.binarySearch(bs, i) >= 0) {
                assertFalse(pred3.getClass().getName() + " " + pred3 + " " + i + " neg should be false", pred3.test(i));
                assertTrue(pred4.getClass().getName() + " " + pred4 + " " + i + " neg should be true", pred4.test(i));
            } else {
                assertTrue(pred3.test(i));
                assertFalse(pred4.test(i));
            }
        }
    }

    private void testRange(IntPredicate pred, int a, int... bs) {
        Arrays.sort(bs);
        assertTrue(pred.test(a));
        IntPredicate neg = pred.negate();
        for (int i = 0; i < 100; i++) {
            if (i == a || Arrays.binarySearch(bs, i) >= 0) {
                assertFalse(pred.getClass().getName() + " " + neg + " " + i + " neg should be false", neg.test(i));
                assertTrue(pred.getClass().getName() + " " + pred + " " + i + " should be true", pred.test(i));
                assertTrue(pred.toString().contains(Integer.toString(i)));
            } else {
                assertFalse(pred.getClass().getName() + " " + pred + " " + i + " should be false", pred.test(i));
                assertTrue(pred.getClass().getName() + " " + neg + " " + i + " neg should be true", neg.test(i));
            }
        }
    }

}
