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

package com.mastfrog.predicates.string;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
public class StringPredicates {

    /**
     * Combine a value and an array and assert that there are no duplicates.
     *
     * @param first The first item
     * @param more More items
     * @return An array containing the first and later items
     */
    public static String[] combine(String first, String... more) {
        String[] result = new String[more.length + 1];
        result[0] = first;
        System.arraycopy(more, 0, result, 1, more.length);
        assert noDuplicates(result) : "Duplicate values in " + Arrays.toString(result);
        return result;
    }

    private static boolean noDuplicates(String[] vals) {
        return new HashSet<>(Arrays.asList(vals)).size() == vals.length;
    }

    /**
     * Create a predicate which matches any of the passed strings.
     *
     * @param first The first string
     * @param more Some more strings
     * @throws AssertionError if duplicates are present and assertions are
     * enabled
     * @return A predicate
     */
    public static Predicate<String> predicate(String first, String... more) {
        if (more.length == 0) {
            return new SingleStringPredicate(false, first);
        }
        String[] vals = StringPredicates.combine(first, more);
        Arrays.sort(vals);
        return new StringArrayPredicate(false, vals);
    }

    private StringPredicates() {
        throw new AssertionError();
    }
}
