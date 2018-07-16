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

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
public interface AppendingCharSequence extends Appendable, Comparable<CharSequence>, CharSequence, ComparableCharSequence {

    @Override
    AppendingCharSequence append(CharSequence csq);

    @Override
    AppendingCharSequence append(CharSequence csq, int start, int end);

    public default AppendingCharSequence append(int val) {
        return append(Integer.toString(val));
    }

    public default AppendingCharSequence append(long val) {
        return append(Long.toString(val));
    }

    public default AppendingCharSequence append(double val) {
        return append(Double.toString(val));
    }

    public default AppendingCharSequence append(float val) {
        return append(Float.toString(val));
    }

    public default AppendingCharSequence append(boolean val) {
        return append(Boolean.toString(val));
    }

    public default AppendingCharSequence append(short val) {
        return append(Short.toString(val));
    }

    @Override
    public default AppendingCharSequence append(char c) {
        return append(Strings.singleChar(c));
    }

    public default AppendingCharSequence appendObject(Object o) {
        if (o instanceof CharSequence) {
            return append((CharSequence) o);
        } else {
            return append(Objects.toString(o));
        }
    }
}
