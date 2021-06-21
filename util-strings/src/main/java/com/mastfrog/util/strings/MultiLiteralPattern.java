/*
 * The MIT License
 *
 * Copyright 2021 Tim Boudreau.
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
package com.mastfrog.util.strings;

import java.lang.reflect.Array;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Matches a string containing any of a number of patterns in a single pass over
 * the characters much more efficiently that using individual regexes or calling
 * toString().contains("whatever").
 *
 * @author Tim Boudreau
 */
final class MultiLiteralPattern<T> implements Function<CharSequence, T> {

    private final T[] keys;
    private final CharSequence[] seqs;
    private final int[] lengths;
    private final int minPatternLength;

    @SuppressWarnings("unchecked")
    MultiLiteralPattern(Map<T, ? extends CharSequence> m, Class<T> type) {
        // For performance with repeated use, separate this all out
        // into arrays
        T[] parts = null;
        CharSequence[] literalPatterns = new CharSequence[m.size()];
        int[] lens = new int[m.size()];
        int cursor = 0;
        int min = Integer.MAX_VALUE;
        for (Map.Entry<T, ? extends CharSequence> e : m.entrySet()) {
            if (parts == null) {
                parts = (T[]) Array.newInstance(type, m.size());
            }
            parts[cursor] = e.getKey();
            int len = e.getValue().length();
            lens[cursor] = len;
            CharSequence pattern = e.getValue();
            if (pattern.length() == 0) {
                throw new IllegalArgumentException("Cannot match on the"
                        + " empty string");
            }
            literalPatterns[cursor++] = pattern;
            min = Math.min(min, len);
        }
        this.keys = parts;
        this.seqs = literalPatterns;
        this.lengths = lens;
        this.minPatternLength = min;
    }

    static <T extends Enum<T>> Function<CharSequence, T> forEnums(Class<T> enumType) {
        Map<T, CharSequence> all = new EnumMap<>(enumType);
        for (T en : enumType.getEnumConstants()) {
            all.put(en, en.toString());
        }
        return new MultiLiteralPattern<>(all, enumType);
    }

    static <T extends Enum<T>> Function<CharSequence, T> forEnums(Map<T, CharSequence> m) {
        if (m.isEmpty()) {
            return _ignored -> null;
        }
        return new MultiLiteralPattern<>(m, typeIn(m));
    }

    static <T> Function<CharSequence, Integer> forStrings(CharSequence... patterns) {
        if (patterns.length == 0) {
            return _ignored -> null;
        }
        Map<Integer, CharSequence> map = new LinkedHashMap<>(patterns.length);
        for (int i = 0; i < patterns.length; i++) {
            map.put(i, patterns[i]);
        }
        return new MultiLiteralPattern<>(map, Integer.class);
    }

    private static <T extends Enum<T>> Class<T> typeIn(Map<T, ?> in) {
        if (in.isEmpty()) {
            throw new IllegalArgumentException("Patterns map is empty");
        }
        return in.entrySet().iterator().next().getKey().getDeclaringClass();
    }

    @Override
    public T apply(CharSequence in) {
        return new PatternMatch().test(in);
    }

    final class PatternMatch {

        private final int[] matchedChars;

        PatternMatch() {
            matchedChars = new int[seqs.length];
        }

        private boolean testOnePattern(int pattern, char c, int at,
                CharSequence in, int len) {
            // If we already know there are
            if (matchedChars[pattern] < 0) {
                return false;
            }
            int currentPositionInPattern = matchedChars[pattern];
            char test = seqs[pattern].charAt(currentPositionInPattern);
            boolean charMatch = test == c;
            boolean result = charMatch && ++matchedChars[pattern]
                    == seqs[pattern].length();
            if (!charMatch) {
                matchedChars[pattern] = 0;
            } else if (matchedChars[pattern] == 1) {
                int patternLastPosition = lengths[pattern] - 1;
                int lastChar = at + patternLastPosition;
                char endChar = in.charAt(lastChar);
                char expectedEndChar = seqs[pattern].charAt(patternLastPosition);
                if (endChar != expectedEndChar) {
                    if (at + lengths[pattern] >= len) {
                        matchedChars[pattern] = -1;
                    } else {
                        matchedChars[pattern] = 0;
                    }
                    result = false;
                }
            }
            return result;
        }

        public T test(CharSequence seq) {
            int len = seq.length();
            if (len < minPatternLength) {
                return null;
            }
            final int numPatterns = matchedChars.length;
            int remainingViable = numPatterns;
            for (int i = 0; i < len; i++) {
                final char curr = seq.charAt(i);
                int remainingLengthInString = len - i;
                for (int j = 0; j < matchedChars.length; j++) {
                    final int remainingLengthInPattern
                            = lengths[j] - matchedChars[j];
                    if (remainingLengthInPattern <= remainingLengthInString) {
                        if (testOnePattern(j, curr, i, seq, len)) {
                            return keys[j];
                        }
                    } else {
                        // mark this pattern as no longer viable, and reduce
                        // the count of patterns we can still match
                        remainingViable--;
                        matchedChars[j] = -1;
                    }
                }
                if (remainingViable == 0) {
                    // If there are not enough characters left in the input
                    // to match any pattern, we're done
                    break;
                }
                remainingViable = numPatterns;
            }
            return null;
        }
    }
}
