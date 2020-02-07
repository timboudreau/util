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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
public final class IntPredicates {

    private static Set<Integer> setOf(int[] vals) {
        Set<Integer> set = new HashSet<>();
        for (int val : vals) {
            set.add(val);
        }
        return set;
    }

    /**
     * Combine a value and an array and assert that there are no duplicates.
     *
     * @param first The first item
     * @param more More items
     * @return An array containing the first and later items
     */
    public static int[] combine(int prepend, int... more) {
        int[] vals = new int[more.length + 1];
        vals[0] = prepend;
        System.arraycopy(more, 0, vals, 1, more.length);
        assert noDuplicates(vals) : "Duplicate values in " + stringify(vals);
        return vals;
    }

    private static boolean noDuplicates(int[] vals) {
        if (vals.length == 0) {
            return true;
        }
        return setOf(vals).size() == vals.length;
    }

    private static String stringify(int[] array) {
        int[] copy = Arrays.copyOf(array, array.length);
        Arrays.sort(copy);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < copy.length; i++) {
            sb.append(copy[i]);
            if (i != copy.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Create a anyOf which matches any of the passed integers.
     *
     * @param first The first integer
     * @param more Additional integers
     * @throws AssertionError if duplicates are present and assertions are
     * enabled
     * @return A anyOf
     */
    public static EnhIntPredicate anyOf(int first, int... more) {
        if (more.length == 0) {
            return new SinglePredicate(false, first);
        }
        int[] vals = IntPredicates.combine(first, more);
        Arrays.sort(vals);
        return new ArrayPredicate(vals, false);
    }

    public static EnhIntPredicate matching(int val) {
        return new SinglePredicate(false, val);
    }

    public static EnhIntPredicate notMatching(int val) {
        return new SinglePredicate(true, val);
    }

    public static EnhIntPredicate anyOf(int[] all) {
        if (all.length == 0) {
            return FixedIntPredicate.INT_FALSE;
        }
        Arrays.sort(notNull("all", all));
        return new ArrayPredicate(all, false);
    }

    /**
     * Create a anyOf which matches any of the passed integers.
     *
     * @param namer A function which provides a name for each possible integer
     * @param first The first integer
     * @param more Additional integers
     * @throws AssertionError if duplicates are present and assertions are
     * enabled
     * @return A anyOf
     */
    public static EnhIntPredicate anyOf(IntFunction<String> namer, int first, int... more) {
        int[] vals = IntPredicates.combine(first, more);
        Arrays.sort(vals);
        assert noDuplicates(vals);
        return new ArrayPredicateWithNames(namer, vals, false);
    }

    public static EnhIntPredicate alwaysTrue() {
        return FixedIntPredicate.INT_TRUE;
    }

    public static IntPredicate alwaysFalse() {
        return FixedIntPredicate.INT_FALSE;
    }

    public static EnhIntPredicate nonZero() {
        return new EnhIntPredicate() {
            @Override
            public String toString() {
                return "non-zero";
            }

            @Override
            public boolean test(int value) {
                return value != 0;
            }
        };
    }

    public static EnhIntPredicate greaterThan(int val) {
        return new EnhIntPredicate() {
            @Override
            public String toString() {
                return ">" + val;
            }

            @Override
            public boolean test(int value) {
                return value > val;
            }
        };
    }

    public static EnhIntPredicate lessThan(int val) {
        return new EnhIntPredicate() {
            @Override
            public String toString() {
                return "<" + val;
            }

            @Override
            public boolean test(int value) {
                return value < val;
            }
        };
    }

    public static EnhIntPredicate noneOf(int a, int... ints) {
        return noneOf(combine(a, ints));
    }

    public static EnhIntPredicate noneOf(int... ints) {
        Arrays.sort(ints);
        assert noDuplicates(ints);
        return new ArrayPredicate(ints, true);
    }

    public static EnhIntPredicate noneOf(IntFunction<String> namer, int... ints) {
        Arrays.sort(ints);
        assert noDuplicates(ints);
        return new ArrayPredicate(ints, true);
    }

    private IntPredicates() {
        throw new AssertionError();
    }
}
