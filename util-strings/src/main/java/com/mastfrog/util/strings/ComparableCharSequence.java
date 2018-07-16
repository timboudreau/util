/*
 * The MIT License
 *
 * Copyright 2016 Tim Boudreau.
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

/**
 *
 * @author Tim Boudreau
 */
public interface ComparableCharSequence extends CharSequence, Comparable<CharSequence> {

    @Override
    public default int compareTo(CharSequence o) {
        return Strings.compareCharSequences(this, o, false);
    }

    public default boolean startsWith(CharSequence seq) {
        int myLen = length();
        int seqLength = seq.length();
        if (seqLength > myLen) {
            return false;
        }
        for (int i = seqLength - 1; i >= 0; i--) {
            if (charAt(i) != seq.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public default int indexOf(char c) {
        int len = length();
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (c == charAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public default int lastIndexOf(char c) {
        int len = length();
        if (len > 0) {
            for (int i = len - 1; i >= 0; i--) {
                if (c == charAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    static final ComparableCharSequence EMPTY = new ComparableCharSequence() {
        @Override
        public int compareTo(CharSequence o) {
            return o.length() == 0 ? 0 : -1;
        }

        @Override
        public int indexOf(char c) {
            return -1;
        }

        @Override
        public int lastIndexOf(char c) {
            return -1;
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public char charAt(int index) {
            throw new ArrayIndexOutOfBoundsException("0 length string but"
                    + " requested char " + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start == 0 && end == 0) {
                return this;
            }
            throw new ArrayIndexOutOfBoundsException("0 length string but"
                    + " requested substring " + start + " -> " + end);

        }

        public boolean equals(Object o) {
            return o instanceof CharSequence && ((CharSequence) o).length() == 0;
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "";
        }
    };
}
