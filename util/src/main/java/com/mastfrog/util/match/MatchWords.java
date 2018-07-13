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
package com.mastfrog.util.match;

import com.mastfrog.util.Strings;
import com.mastfrog.util.collections.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Given multiple strings, this {@link Predicate} tests positive, if one or
 * multiple of the strings are prefixes of the string that is tested.
 *
 * @author Tim Boudreau
 */
final class MatchWords implements Predicate<String> {

    private final List<MatchState> matchers = new ArrayList<>();
    private ThreadLocal<MatchState[]> local = new ThreadLocal<>();
    private char[] firsts;

    MatchWords(String[] strings) {
        String[] all = ArrayUtils.copyOf(strings);
        firsts = new char[all.length];
        Arrays.sort(all);
        for (int i = 0; i < all.length; i++) {
            if (all[i].isEmpty()) {
                throw new IllegalArgumentException("Cannot match on the empty string: " + Strings.join(',', strings));
            }
            matchers.add(new MatchState(all[i]));
            firsts[i] = all[i].charAt(0);
        }
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
    public boolean test(String t) {
        int max = t.length();
        MatchState[] mtchrs = matchers();
        for (MatchState mtchr : mtchrs) {
            mtchr.reset();
        }
        for (int i = 0; i < max; i++) {
            char c = t.charAt(i);
            for (int j = 0; j < mtchrs.length; j++) {
                mtchrs[j].check(c);
                if (mtchrs[j].isMatched()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String find(String t) {
        int max = t.length();
        MatchState[] mtchrs = matchers();
        for (MatchState mtchr : mtchrs) {
            mtchr.reset();
        }
        for (int i = 0; i < max; i++) {
            char c = t.charAt(i);
            for (int j = 0; j < mtchrs.length; j++) {
                mtchrs[j].check(c);
                if (mtchrs[j].isMatched()) {
                    return mtchrs[j].what;
                }
            }
        }
        return null;
    }

    private static final class MatchState implements Comparable<MatchState> {

        private final String what;
        private int matched = 0;
        private boolean failed = false;

        MatchState(String what) {
            this.what = what;
        }

        MatchState(char[] what) {
            this.what = new String(what);
        }

        public MatchState copy() {
            return new MatchState(what);
        }

        private void reset() {
            matched = 0;
            failed = false;
        }

        public String toString() {
            return what;
        }

        boolean isMatched() {
            return matched >= what.length();
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
