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
import java.util.ArrayList;
import java.util.List;

/**
 * Avoids memory copies.  Wraps a bunch of CharBuffers or similar
 * as a single CharSequence, and subdivides using additional
 * ConcatCharSequences.
 *
 * @author Tim Boudreau
 */
public final class ConcatCharSequence implements CharSequence, Appendable {

    private final List<CharSequence> chars;

    public ConcatCharSequence() {
        chars = new ArrayList<>(10);
    }

    public ConcatCharSequence(int val) {
        chars = new ArrayList<>(val);
    }

    public ConcatCharSequence(char c) {
        this();
        chars.add(Strings.singleChar(c));
    }

    public ConcatCharSequence(CharSequence initial) {
        this();
        chars.add(initial);
    }

    public ConcatCharSequence append(char ch) {
        chars.add(Strings.singleChar(ch));
        hash = 0;
        return this;
    }

    /**
     * Append a CharSequence.  The passed CharSequence will remain
     * referenced by this object.
     * 
     * @param buffer The char sequence
     * @return this
     */
    @Override
    public ConcatCharSequence append(CharSequence buffer) {
        if (buffer == this) {
            throw new IllegalArgumentException("Add to self");
        }
        if (buffer instanceof ConcatCharSequence) {
            assert doesNotContainSelf((ConcatCharSequence) buffer) : "Indirectly adding self to self";
            chars.addAll(((ConcatCharSequence) buffer).chars);
            hash = 0;
            return this;
        }
        chars.add(buffer);
        hash = 0;
        return this;
    }

    private boolean doesNotContainSelf(ConcatCharSequence seq) {
        for (CharSequence cs : chars) {
            if (cs == this) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consolidate all internal CharSequences and release references
     * to them, copying the existing content to a new string.
     * @return this
     */
    public ConcatCharSequence consolidate() {
        String s = toString();
        chars.clear();
        chars.add(s);
        hash = 0;
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

    /**
     * Fetch a subsequence; the subsequence created may be a subsequence
     * of a single buffer contained here, or may be a new ConcatCharSequence
     * spanning subsequences of a number of them.  The resulting object may
     * reference buffers contained by this object.
     * 
     * @param start The start character, inclusive
     * @param end The end character, exclusive
     * @return A char sequence
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start == end) {
            return new ConcatCharSequence();
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
    public ConcatCharSequence append(CharSequence csq, int start, int end) throws IOException {
        append(csq.subSequence(start, end));
        return this;
    }
}
