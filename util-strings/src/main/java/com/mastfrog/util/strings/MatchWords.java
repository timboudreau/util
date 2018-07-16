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
package com.mastfrog.util.strings;

import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.strings.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Given multiple strings, this {@link Predicate} tests positive, if one or
 * multiple of the strings are prefixes of the string that is tested.
 * <p>
 * Takes CharSequence for convenience, but assumes that the passed set of
 * CharSequences <i>do not mutate</i> for the life of this object.
 * </p>
 *
 * @author Tim Boudreau
 */
final class MatchWords implements Predicate<CharSequence> {

    private final List<MatchState> matchers = new ArrayList<>();
    private final ThreadLocal<MatchState[]> local = new ThreadLocal<>();
    private final char[] firsts;
    private final int maxLength;
    private final int minLength;
    private final boolean prefixMatch;
    private final BitSet lengths;

    MatchWords(CharSequence[] strings, boolean prefixMatch) {
        notNull("strings", strings);
        if (strings.length == 0) {
            throw new IllegalArgumentException("Zero length array of strings");
        }
        this.prefixMatch = prefixMatch;
        CharSequence[] all = Arrays.copyOf(strings, strings.length);
        char[] firsts = new char[all.length];
        Arrays.sort(all, Strings.charSequenceComparator());
        int max = 0;
        int min = Integer.MAX_VALUE;
        char lastFirst = 0;
        int firstIndex = 0;
        CharSequence prev = null;
        for (int i = 0; i < all.length; i++) {
            CharSequence curr = all[i];
            if (curr.length() == 0) {
                throw new IllegalArgumentException("Cannot match on the empty string: " + Strings.join(',', strings));
            }
            notNull("strings[" + i + "]", curr);
            if (prefixMatch && prev != null && Strings.startsWith(curr, prev)) {
                throw new IllegalArgumentException("Already added '" + prev + "' which is the prefix of '" + curr + "' - will never match '" + curr + "'");
            }
            matchers.add(new MatchState(curr));
            char first = curr.charAt(0);
            if (first != lastFirst) {
                firsts[firstIndex++] = first;
            }
            lastFirst = first;
            max = Math.max(max, curr.length());
            min = Math.min(min, curr.length());
            prev = curr;
        }
        if (firstIndex != all.length) {
            firsts = Arrays.copyOf(firsts, firstIndex);
        }
        if (!prefixMatch) {
            lengths = new BitSet(max-1);
            for (CharSequence s : all) {
                lengths.set(s.length());
            }
        } else {
            lengths = null;
        }
        Arrays.sort(firsts);
        maxLength = max;
        minLength = min;
        this.firsts = firsts;
    }

    public static Predicate<CharSequence> matchPrefixes(CharSequence... sequences) {
        return new MatchWords(sequences, true);
    }

    public static Predicate<CharSequence> matchWords(CharSequence... sequences) {
        return new MatchWords(sequences, false);
    }

    public static Function<CharSequence,CharSequence> findPrefixes(CharSequence... prefixen) {
        MatchWords mw = new MatchWords(prefixen, true);
        return mw::find;
    }

    public static Predicate<CharSequence> matchWords(String... strings) {
        return new MatchWords(strings, false);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MatchState state : this.matchers) {
            if (sb.length() != 0) {
                sb.append(',');
            }
            sb.append(state.what);
        }
        sb.insert(0, "MatchWords{").append(';');
        sb.append(" minLength=").append(minLength).append(" maxLength=").append(maxLength)
                .append(" prefix=").append(prefixMatch).append('}');
        return sb.toString();
    }

    private MatchState[] matchers() {
        MatchState[] result = local.get();
        if (result == null) {
            result = new MatchState[matchers.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = matchers.get(i).copy();
            }
            local.set(result);
        }
        return result;
    }

    @Override
    public boolean test(CharSequence t) {
        return find(t) != null;
    }

    public CharSequence find(CharSequence t) {
        int max = t.length();
        if (max == 0 || max < minLength) {
            return null;
        }
        if (max > maxLength && !prefixMatch) {
            return null;
        }
        if (!prefixMatch && !lengths.get(max)) {
            return null;
        }
        MatchState[] mtchrs = matchers();
        for (MatchState mtchr : mtchrs) {
            mtchr.reset();
        }
        int firstMatcher = 0;
        int lastMatcher = mtchrs.length;
        for (int i = 0; i < max; i++) {
            char c = t.charAt(i);
            if (i == 0) {
                int firstIndex = Arrays.binarySearch(firsts, c);
                if (firstIndex < 0) {
                    return null;
                } else {
                    // Since we prune duplicates from the firsts character array,
                    // this may return us an index before the first match; but,
                    // it can still let us skip some obvious non-matches
                    firstMatcher = firstIndex;
                }
            }
            if(firstMatcher == lastMatcher) {
                return null;
            }
            for (int j = firstMatcher; j < lastMatcher; j++) {
                if (!prefixMatch && mtchrs[j].length() != max) {
                    if (j == firstMatcher) {
                        firstMatcher++;
                    } else if (j == lastMatcher -1) {
                        lastMatcher--;
                    }
                    continue;
                }
                mtchrs[j].check(c);
                if (mtchrs[j].isMatched()) {
                    return mtchrs[j].what;
                } else if (j == firstMatcher && mtchrs[j].isFailed()) {
                    firstMatcher++;
                } else if (j == (lastMatcher-1) - 1 && mtchrs[j].isFailed()) {
                    lastMatcher--;
                }
            }
        }
        return null;
    }

    private static final class MatchState implements Comparable<MatchState> {

        private final CharSequence what;
        private int matched = 0;
        private boolean failed = false;
        private final int length;

        MatchState(CharSequence what) {
            this.what = what;
            this.length = what.length();
        }

        MatchState(char[] what) {
            this.what = new String(what);
            this.length = what.length;
        }

        public MatchState copy() {
            return new MatchState(what);
        }

        private void reset() {
            matched = 0;
            failed = false;
        }

        public String toString() {
            return what.toString();
        }

        boolean isFailed() {
            return failed;
        }

        int length() {
            return length;
        }

        boolean isMatched() {
            return matched >= length;
        }

        void check(char c) {
            if (failed || isMatched()) {
                return;
            }
            if (what.charAt(matched) == c) {
                matched++;
            } else {
                failed = true;
            }
        }

        @Override
        public int compareTo(MatchState o) {
            return toString().compareTo(o.toString());
        }
    }
}
