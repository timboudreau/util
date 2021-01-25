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

import java.util.Random;
import java.util.function.IntBinaryOperator;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ObjSortTest {

    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    @Test
    public void testReversed() {
        char[] toSort = alphaReversed();
        CharSwapperAndComparer csac = new CharSwapperAndComparer(toSort);
        ObjSort.sortAdhoc(csac, toSort.length, csac);
        assertEquals(ALPHA, new String(toSort));
    }

    @Test
    public void testShuffled() {
        char[] toSort = alphaShuffled();
        CharSwapperAndComparer csac = new CharSwapperAndComparer(toSort);
        ObjSort.sortAdhoc(csac, toSort.length, csac);
        assertEquals(ALPHA, new String(toSort));
    }

    private char[] alphaReversed() {
        char[] result = ALPHA.toCharArray();
        for (int i = 0; i < result.length; i++) {
            int inv = result.length - (i + 1);
            char hold = result[i];
            result[i] = result[inv];
            result[inv] = hold;
        }
        return result;
    }

    Random rnd = new Random(8019201933L);

    private char[] alphaShuffled() {
        char[] result = ALPHA.toCharArray();
        CharSwapperAndComparer cmp = new CharSwapperAndComparer(result);
        for (int i = 0; i < result.length; i++) {
            int ix1 = rnd.nextInt(result.length);
            cmp.swap(i, ix1);
        }
        return result;
    }

    static final class CharSwapperAndComparer implements Swapper, IntBinaryOperator {

        private final char[] chars;

        CharSwapperAndComparer(char[] chars) {
            this.chars = chars;
        }

        @Override
        public void swap(int index1, int index2) {
            char hold = chars[index1];
            chars[index1] = chars[index2];
            chars[index2] = hold;
        }

        @Override
        public int applyAsInt(int left, int right) {
            char a = chars[left];
            char b = chars[right];
            return Character.compare(a, b);
        }
    }
}
