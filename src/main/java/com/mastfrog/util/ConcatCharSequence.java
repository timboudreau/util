/* 
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Avoids memory copies.  Wraps a bunch of CharBuffers or similar
 * as a single CharSequence, and subdivides using additional
 * ConcatCharSequences.
 *
 * @author Tim Boudreau
 */
final class ConcatCharSequence implements CharSequence, Appendable {

    private final List<CharSequence> chars = new ArrayList<>(10);

    @Override
    public ConcatCharSequence append(CharSequence buffer) {
        if (buffer == this) {
            throw new IllegalArgumentException("Add to self");
        }
        chars.add(buffer);
        return this;
    }

    public boolean isEmpty() {
        return chars.isEmpty() || length() == 0;
    }

    public void clear() {
        chars.clear();
    }

    @Override
    public int length() {
        int result = 0;
        for (CharSequence cb : chars) {
            result += cb.length();
        }
        return result;
    }

    @Override
    public char charAt(int index) {
        int pos = 0;
        for (CharSequence cb : chars) {
            if (index >= pos && index < cb.length() + pos) {
                return cb.charAt(index - pos);
            }
            pos += cb.length();
        }
        throw new IndexOutOfBoundsException(index + " of " + length());
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == end) {
            return "";
        }
        ConcatCharSequence result = null;
        int position = 0;
        for (CharSequence currBuffer : chars) {
            if (start >= position && start < currBuffer.length() + position) {
                if (end <= position + currBuffer.length()) { // substring of a single buffer
                    CharSequence res = currBuffer.subSequence(start - position, end - position);
                    return res;
                } else if (start == position) { // requested subsequence contains buffer
                    if (result == null) {
                        result = new ConcatCharSequence();
                    }
                    result.append(currBuffer);
                } else {
                    if (result == null) {
                        result = new ConcatCharSequence();
                    }
                    result.append(currBuffer.subSequence(start - position, currBuffer.length()));
                }
            } else if (start <= position && end >= position + currBuffer.length()) {
                if (result == null) {
                    result = new ConcatCharSequence();
                }
                result.append(currBuffer);
            } else if (start <= position && end < position + currBuffer.length()) {
                if (result == null) {
                    result = new ConcatCharSequence();
                }
                result.append(currBuffer.subSequence(0, end - position));
                break;
            }
            position += currBuffer.length();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CharSequence cb : chars) {
            sb.append(cb);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o instanceof String ? toString().equals(o)
                : o instanceof CharSequence ? contentEquals((CharSequence) o, this) : false;
    }

    private int hash = 0;
    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            for (CharSequence cb : chars) {
                int max = cb.length();
                for (int i = 0; i < max; i++) {
                    h = 31 * h + cb.charAt(i);
                }
            }
            hash = h;
        }
        return h;
    }

    private boolean contentEquals(CharSequence a, CharSequence b) {
        boolean result = a.length() == b.length();
        if (result) {
            int max = a.length();
            for (int i = 0; i < max; i++) {
                if (a.charAt(i) != b.charAt(i)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        append(csq.subSequence(start, end));
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        CharBuffer cb = CharBuffer.allocate(1);
        cb.put(c);
        cb.flip();
        append(cb);
        return this;
    }
}
