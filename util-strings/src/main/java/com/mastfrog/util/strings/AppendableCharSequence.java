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

import com.mastfrog.util.preconditions.Checks;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An appendable implementation for char sequences that does not copy bytes.
 * <i>Note</i>
 * the behavior of this class is undefined if the character sequences added are
 * mutable and get mutated after they are added here. Don't do that.
 * <p>
 * This class is not thread-safe. At all.
 *
 * @author Tim Boudreau
 */
public final class AppendableCharSequence implements Appendable, Comparable<CharSequence>, CharSequence, ComparableCharSequence, AppendingCharSequence {

    private static final int INCREMENT = 10;
    private final List<CharSequence> seqs;
    private int length;

    /**
     * Create an appendable character sequence with an anticipated
     * number of <i>components</i> - as in strings that will be appended,
     * not a character count - the original char sequences are used as-is.
     * 
     * @param components The component count
     */
    public AppendableCharSequence(int components) {
        this.seqs = new ArrayList<>(components);
    }

    public AppendableCharSequence(CharSequence... seqs) {
        Checks.notNull("seqs", seqs);
        Checks.noNullElements("seqs", seqs);
        this.seqs = new ArrayList<>(INCREMENT);
        for (CharSequence s : seqs) {
            this.seqs.add(s);
        }
    }

    @Override
    public int compareTo(CharSequence o) {
        return Strings.compareCharSequences(this, o, false);
    }

    @Override
    public int length() {
        return length;
    }

    private int lastElement = -2;
    private int lastCharAtRequested = -2;
    private int lastAggregateLength = -2;

    @Override
    public char charAt(int index) {
        // XXX this can be done more efficiently:  keep an array of the aggregate length at the end of each
        // entry, and binary search for the nearest element less than the index
        if (length == 0) {
            throw new StringIndexOutOfBoundsException("Length is 0 but requested " + index);
        }
        if (index < 0 || index >= length) {
            throw new StringIndexOutOfBoundsException(index + " out of range - length " + length);
        }
        if (index == length - 1) {
            CharSequence cs = seqs.get(seqs.size() - 1);
            return cs.charAt(cs.length() - 1);
        } else if (index == 0) {
            CharSequence cs = seqs.get(0);
            return cs.charAt(0);
        }
        // Optimizes the common case of sequential iteration, by retaining search positions
        // from the previous call and continuing from there in the case that we're asked for
        // the next character
        int max = elementCount();
        int aggLength = 0;
        int ix = 0;
        if (lastCharAtRequested >= 0 && index >= lastCharAtRequested) {
            ix = lastElement;
            aggLength = lastAggregateLength;
        }
        for (int i = ix; i < max; i++) {
            CharSequence seq = seqs.get(i);
            int len = seq.length();
            if (index >= aggLength && index < aggLength + len) {
                lastCharAtRequested = index;
                lastElement = ix;
                lastAggregateLength = aggLength;
                return seq.charAt(index - aggLength);
            }
            aggLength += len;
            ix++;
        }
        throw new StringIndexOutOfBoundsException(index + " from length " + length);
    }

    public int elementCount() {
        return seqs.size();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("Start greater than end " + start + " > " + end);
        }
        if (start == end) {
            return Strings.emptyCharSequence();
        }
        if (start == 0 && end == length) {
            return this;
        }
        int aggregateLength = 0;
        int max = elementCount();
        AppendableCharSequence result = new AppendableCharSequence();
        for (int i = 0; i < max; i++) {
            if (aggregateLength >= end + 1) {
                break;
            }
            CharSequence curr = seqs.get(i);
            int len = curr.length();
            int first = Math.max(0, start - aggregateLength);
            int last = Math.min(len, Math.min((end + 1) - start, first + (end - aggregateLength)));

            if (end >= aggregateLength + len) {
                last = len;
            } else if (end <= aggregateLength + len) {
                last = end - aggregateLength;
            }
            if (last > first) {
                CharSequence sub = curr.subSequence(first, last);
                result.append(sub);
            }
            aggregateLength += len;
        }
        return result;
    }

    @Override
    public AppendableCharSequence append(CharSequence csq) {
        Checks.notNull("seq", csq);
        if (csq instanceof AppendableCharSequence) {
            AppendableCharSequence a = (AppendableCharSequence) csq;
            for (CharSequence cs : a.seqs) {
                this.append(cs);
            }
        } else {
            int len = csq.length();
            if (len == 0) {
                return this;
            }
            if (len == 1) {
                length += 1;
                seqs.add(Strings.singleChar(csq.charAt(0)));
                return this;
            }
            length += len;
            seqs.add(csq);
        }
        return this;
    }

    @Override
    public AppendableCharSequence append(CharSequence csq, int start, int end) {
        Checks.notNull("csq", csq);
        return append(csq.subSequence(start, end));
    }

    public AppendableCharSequence consolidate() {
        return new AppendableCharSequence(toString());
    }
    
    public AppendableCharSequence append(int val) {
        return append(Integer.toString(val));
    }
    
    public AppendableCharSequence append(long val) {
        return append(Long.toString(val));
    }
    
    public AppendableCharSequence append(double val) {
        return append(Double.toString(val));
    }
    
    public AppendableCharSequence append(float val) {
        return append(Float.toString(val));
    }
    
    public AppendableCharSequence append(boolean val) {
        return append(Boolean.toString(val));
    }
    
    public AppendableCharSequence append(short val) {
        return append(Short.toString(val));
    }

    @Override
    public AppendableCharSequence append(char c) {
        return append(Strings.singleChar(c));
    }

    public AppendableCharSequence appendObject(Object o) {
        if (o instanceof CharSequence) {
            return append((CharSequence) o);
        } else {
            return append(Objects.toString(o));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(length);
        for (CharSequence seq : seqs) {
            sb.append(seq);
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof CharSequence) {
            return Strings.charSequencesEqual(this, (CharSequence) o, false);
        }
        return false;
    }

    public int hashCode() {
        return Strings.charSequenceHashCode(this);
    }
}
