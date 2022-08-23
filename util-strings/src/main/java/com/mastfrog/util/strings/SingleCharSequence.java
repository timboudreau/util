/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
 * A CharSequence that wraps a single character.
 *
 * @author Tim Boudreau
 */
final class SingleCharSequence implements CharSequence, ComparableCharSequence {

    private final char c;

    SingleCharSequence(char c) {
        this.c = c;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public char charAt(int index) {
        if (index == 0) {
            return c;
        }
        throw new StringIndexOutOfBoundsException(index + " of 1");
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == 0 && end == 0) {
            return Strings.emptyCharSequence();
        }
        if (start == 0 && end == 1) {
            return this;
        }
        throw new StringIndexOutOfBoundsException("Length is 1 but requested subsequence " + start + " to " + end);
    }

    public int hashCode() {
        // this actually follows the contract of java.lang.String.hashCode()
        return c;
    }

    public boolean equals(Object o) {
        return o == null ? false : o == this ? true
                : o instanceof CharSequence && ((CharSequence) o).length()==1
                ? ((CharSequence) o).charAt(0) == c : false;
    }

    public String toString() {
        return Character.toString(c);
    }

    @Override
    public int compareTo(CharSequence o) {
        if (o.length() == 0) {
            return 1;
        }
        char first = o.charAt(0);
        return first > c ? -1 : first == c ? 0 : -1;
    }

    public boolean startsWith(CharSequence seq) {
        return seq.length() == 1 && seq.charAt(0) == c;
    }

    public int indexOf(char c) {
        return c == this.c ? 0 : -1;
    }

    public int lastIndexOf(char c) {
        return indexOf(c);
    }
}
