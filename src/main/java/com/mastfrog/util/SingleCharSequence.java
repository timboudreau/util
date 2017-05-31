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

package com.mastfrog.util;

import com.mastfrog.util.Strings;

/**
 *
 * @author Tim Boudreau
 */
final class SingleCharSequence implements CharSequence {

    private final char c;

    public SingleCharSequence(char c) {
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

}
