/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.file;

import java.nio.CharBuffer;

/**
 *
 * @author Tim Boudreau
 */
final class CharBuffersCharSequence implements CharSequence {

    private final CharBuffer[] buffers;
    private int length = -1;

    public CharBuffersCharSequence(CharBuffer[] buffers) {
        this.buffers = buffers;
    }

    @Override
    public int length() {
        if (length == -1) {
            int result = 0;
            for (int i = 0; i < buffers.length; i++) {
                result += buffers[i].length();
            }
            length = result;
        }
        return length;
    }

    private int[] bufferIndexOfPosition(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index " + index);
        }
        int pos = 0;
        for (int i = 0; i < buffers.length; i++) {
            int len = buffers[i].length();
            if (index >= pos && index < pos + len) {
                return new int[]{i, pos};
            }
            pos += len;
        }
        return new int[]{-1, -1};
    }

    @Override
    public char charAt(int index) {
        int[] bi = bufferIndexOfPosition(index);
        if (bi[0] == -1) {
            throw new IndexOutOfBoundsException(index + " of " + length());
        }
        CharBuffer buf = buffers[bi[0]];
        int offset = index - bi[1];
        return buf.charAt(offset);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // XXX this could be optimized
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        char[] c = new char[length()];
        int ix = 0;
        for (int i = 0; i < buffers.length; i++) {
            int l = buffers[i].length();
            for (int j = 0; j < l; j++) {
                c[ix++] = buffers[i].charAt(j);
            }
        }
        return new String(c);
    }

    @Override
    public int hashCode() {
        // Same computation as java.lang.String
        if (length() == 0) {
            return 0;
        }
        int result = 0;
        for (int i = 0; i < buffers.length; i++) {
            int l = buffers[i].length();
            for (int j = 0; j < l; j++) {
                result = 31 * result + buffers[i].charAt(j);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof CharSequence) {
            CharSequence cs = (CharSequence) o;
            int len = length();
            if (len != cs.length()) {
                return false;
            }
            int ix = 0;
            for (int i = 0; i < buffers.length; i++) {
                int l = buffers[i].length();
                for (int j = 0; j < l; j++) {
                    char c = buffers[i].charAt(j);
                    if (cs.charAt(ix++) != c) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

}
