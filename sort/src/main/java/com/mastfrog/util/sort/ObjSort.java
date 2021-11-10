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
package com.mastfrog.util.sort;

import java.util.Comparator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;

/**
 * Generic sorting - loosely based on a modified version of JDK 6's modified
 * merge sort - which, unlike JDK 7 and up's TimSort (fewer comparisons) can be
 * adapted to sort things without needing the ability to make temporary lists of
 * items, which can't be done easily for all use cases (ex: sorting regions of a
 * fixed-record-length file in place where the whole point is not to pull the
 * whole file onto the heap).
 *
 * @author Tim Boudreau
 */
final class ObjSort {

    private interface Cmp {

        int cmp(int a, int b);

        default boolean gt(int a, int b) {
            return cmp(a, b) > 0;
        }

        default boolean lt(int a, int b) {
            return cmp(a, b) < 0;
        }

        default boolean eq(int a, int b) {
            return cmp(a, b) == 0;
        }

        default boolean lte(int a, int b) {
            return cmp(a, b) <= 0;
        }

        default boolean gte(int a, int b) {
            return cmp(a, b) >= 0;
        }
    }

    private static class CmpImpl<T> implements Cmp {

        private final IntFunction<T> f;
        private final Comparator<T> cmp;

        public CmpImpl(IntFunction<T> f, Comparator<T> cmp) {
            this.f = f;
            this.cmp = cmp;
        }

        public int cmp(int a, int b) {
            T aa = f.apply(a);
            T bb = f.apply(b);

            return cmp.compare(aa, bb);
        }
    }

    /**
     * Sort some collection of objects which can be looked up by the passed
     * IntFunction, using the passed comparator, with the passed Swapper
     * (immediately) reolocating elements.
     *
     * @param <T> The object type
     * @param items Function that can look up objects by integer index
     * @param comp A comparator
     * @param swapper Function that can transponse the location of two items
     * @param off The starting offset
     * @param len The length
     */
    static <T> void sortObjects(IntFunction<T> items, Comparator<T> comp, Swapper swapper, int off, int len) {
        sortAdhoc(new CmpImpl<>(items, comp), swapper, off, len);
    }

    /**
     * Sort <i>whatever</i>, with the thing being sorted completely abstracted -
     * you supply a swapper which can transpose elements, and an
     * IntBinaryOperator which supplies similar output to a Comparator for a
     * pair of indices.
     *
     * @param <T>
     * @param swapper Function that can transponse the location of two items
     * @param len The length to sort
     * @param cmp Comparator like function that can give the result of comparing
     * the contents of two indices
     */
    static <T> void sortAdhoc(Swapper swapper, int len, IntBinaryOperator cmp) {
        sortAdhoc(swapper, 0, len, cmp);
    }

    /**
     * Sort <i>whatever</i>, with the thing being sorted completely abstracted -
     * you supply a swapper which can transpose elements, and an
     * IntBinaryOperator which supplies similar output to a Comparator for a
     * pair of indices.
     *
     * @param <T>
     * @param swapper Function that can transponse the location of two items
     * @param off The starting offset
     * @param len The length to sort
     * @param cmp Comparator like function that can give the result of comparing
     * the contents of two indices
     */
    static void sortAdhoc(Swapper swapper, int off, int len, IntBinaryOperator cmp) {
        sortAdhoc(cmp::applyAsInt, swapper, off, len);
    }

    static void sortAdhoc(Cmp cmp, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && cmp.gt(j - 1, j); j--) {
                    swapper.swap(j, j - 1);
                }
            }
            return;
        }

        int m = off + (len >> 1);
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {
                int s = len / 8;
                l = m3(cmp, l, l + s, l + 2 * s);
                m = m3(cmp, m - s, m, m + s);
                n = m3(cmp, n - 2 * s, n - s, n);
            }
            m = m3(cmp, l, m, n);
        }

        int a = off, b = a, c = off + len - 1, d = c;
        for (;;) {
            while (b <= c && cmp.lte(b, m)) {
                if (cmp.eq(b, m)) {
                    if (b == m) {
                        m = a;
                    } else if (a == m) {
                        m = b;
                    }
                    swapper.swap(a++, b);
                }
                b++;
            }
            while (c >= b && cmp.gte(c, m)) {
                if (cmp.eq(c, m)) {
                    if (c == m) {
                        m = d;
                    } else if (d == m) {
                        m = c;
                    }
                    swapper.swap(c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            if (m == b) {
                m = c;
            } else if (m == c) {
                m = b;
            }
            swapper.swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        swapper.bulkSwap(off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        swapper.bulkSwap(b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortAdhoc(cmp, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortAdhoc(cmp, swapper, n - s, s);
        }
    }

    private static int m3(Cmp cmp, int a, int b, int c) {
        return cmp.lt(a, b)
                ? (cmp.lt(b, c) ? b : cmp.lt(a, c) ? c : a)
                : (cmp.gt(b, c) ? b : cmp.gt(a, c) ? c : a);
    }
}
