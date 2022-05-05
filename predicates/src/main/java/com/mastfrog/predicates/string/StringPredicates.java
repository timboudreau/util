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

import static com.mastfrog.predicates.string.SubstringPredicate.Relation.CONTAINS;
import static com.mastfrog.predicates.string.SubstringPredicate.Relation.ENDS_WITH;
import static com.mastfrog.predicates.string.SubstringPredicate.Relation.STARTS_WITH;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

/**
 * Factory for string predicates which have a reasonable implementation of
 * <code>toString()<code>, <code>equals(Object)</code> and
 * <code>hashCode()</code> suitable for use where the logic in question is being
 * logged or otherwise rendered to be human readable.
 *
 * @author Tim Boudreau
 */
public final class StringPredicates {

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

    public static EnhStringPredicate contains(String s) {
        return new SubstringPredicate(CONTAINS, s);
    }

    public static EnhStringPredicate startsWith(String s) {
        return new SubstringPredicate(STARTS_WITH, s);
    }

    public static EnhStringPredicate endsWith(String s) {
        return new SubstringPredicate(ENDS_WITH, s);
    }

    public static EnhStringPredicate length(IntPredicate lengthTest) {
        return new LengthPredicate(lengthTest);
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
    public static EnhStringPredicate predicate(String first, String... more) {
        if (more.length == 0) {
            return new SingleStringPredicate(false, first);
        }
        String[] vals = combine(first, more);
        Arrays.sort(vals);
        return new StringArrayPredicate(false, vals);
    }

    public static EnhStringPredicate predicate(String only) {
        return new SingleStringPredicate(false, only);
    }

    public static EnhStringPredicate pattern(String pattern) {
        return new PatternPredicate(Pattern.compile(pattern));
    }

    public static EnhStringPredicate pattern(Pattern pattern) {
        return new PatternPredicate(pattern);
    }

    private StringPredicates() {
        throw new AssertionError();
    }
}
