/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.function.character.stateful;

import static java.lang.Math.max;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Stateful predicate which matches prefixes in a string, returning true during
 * matching and if any was matched, continuing to once past the length of all of
 * them, otherwise returning false as soon as all the strings have failed to
 * match. It is the equivalent of calling
 * <code>String.startsWith("someText") || String.startsWith("otherText")</code>
 * for an arbitrary number of strings, but only iterating the characters once to
 * match or fail to.
 *
 * @author Tim Boudreau
 */
public final class WordPrefix implements StatefulCharPredicate<WordPrefix> {

    private final String[] words;
    private final boolean[] inPlay;
    private int position;
    private boolean done;
    private boolean lastResult;

    WordPrefix(String... words) {
        this.words = words;
        this.inPlay = new boolean[words.length];
        Arrays.fill(inPlay, true);
    }

    public static WordPrefix of(String... words) {
        return new WordPrefix(Arrays.copyOf(words, words.length));
    }

    @Override
    public WordPrefix copy() {
        return new WordPrefix(words);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean test(char c) {
        if (done) {
            return lastResult;
        }
        int pos = position++;
        boolean anyInPlay = false;
        boolean result = false;
        for (int i = 0; i < words.length; i++) {
            int len = words[i].length();
            if (inPlay[i]) {
                if (pos >= len) {
                    inPlay[i] = false;
                } else {
                    anyInPlay = true;
                    char test = words[i].charAt(pos);
                    if (test == c) {
                        result = lastResult = true;
                        if (pos == len - 1) {
                            done = true;
                        }
                    }
                }
            }
        }
        if (anyInPlay) {
            return result;
        } else {
            done = true;
            return lastResult;
        }
    }

    @Override
    public WordPrefix reset() {
        position = 0;
        Arrays.fill(inPlay, true);
        done = false;
        return this;
    }

    int maxLen() {
        int result = 0;
        for (String word : words) {
            result = max(word.length(), result);
        }
        return result;
    }

    public Predicate<CharSequence> toStringPredicate() {
        return new WPStringPredicate(this);
    }

    @Override
    public String toString() {
        return "WordPrefix(" + Arrays.toString(words) + ")";
    }

    static class WPStringPredicate implements Predicate<CharSequence> {

        private boolean failed;
        private final WordPrefix pfx;

        WPStringPredicate(WordPrefix pfx) {
            this.pfx = pfx.copy();
        }

        @Override
        public boolean test(CharSequence text) {
            if (text == null) {
                return false;
            }
            if (failed) {
                return false;
            }
            if (pfx.isDone()) {
                return !failed;
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean result = pfx.test(c);
                if (!result) {
                    failed = true;
                    break;
                }
                if (pfx.isDone()) {
                    break;
                }
            }
            return !failed;
        }

        @Override
        public String toString() {
            return "WPStringPredicate(" + pfx + ")";
        }
    }

}
