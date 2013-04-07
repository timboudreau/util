/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.search;

import java.util.List;

/**
 * General-purpose binary search algorithm;  you pass in an array or list
 * wrapped in an instance of <code>Indexed</code>, and an
 * <code>Evaluator</code> which converts the contents of the list into
 * numbers used by the binary search algorithm.  Note that the data
 * (as returned by the Indexed array/list) <b><i>must be in order from
 * low to high</i></b>.  The indices need not be contiguous (presumably
 * they are not or you wouldn't be using this class), but they must be
 * sorted.  If assertions are enabled, this is enforced;  if not, very
 * bad things (endless loops, etc.) can happen as a consequence of passing
 * unsorted data in.
 * <p/>
 * This class is not thread-safe and the size and contents of the <code>Indexed</code>
 * should not change while a search is being performed.
 *
 * @author Tim Boudreau
 */
public class BinarySearch<T> {

    private final Evaluator<T> eval;
    private final Indexed<T> indexed;

    /**
     * Create a new binary search.
     * 
     * @param eval The thing which converts elements into numbers
     * @param indexed A collection, list or array
     */
    public BinarySearch(Evaluator<T> eval, Indexed<T> indexed) {
        this.eval = eval;
        this.indexed = indexed;
        assert checkSorted();
    }
    
    public BinarySearch(Evaluator<T> eval, List<T> l) {
        this (eval, new ListWrap<T>(l));
    }
    
    private boolean checkSorted() {
        long val = Long.MIN_VALUE;
        long sz = indexed.size();
        for (long i=0; i < sz; i++) {
            T t = indexed.get(i);
            long nue = eval.getValue(t);
            if (val != Long.MIN_VALUE) {
                if (nue < val) {
                    throw new IllegalArgumentException("Collection is not sorted at " + i + " - " + indexed);
                }
            }
            val = nue;
        }
        return true;
    }

    public long search(long value, Bias bias) {
        return search(0, indexed.size()-1, value, bias);
    }

    public T match(T prototype, Bias bias) {
        long value = eval.getValue(prototype);
        long index = search(value, bias);
        return index == -1 ? null : indexed.get(index);
    }

    public T searchFor(long value, Bias bias) {
        long index = search(value, bias);
        return index == -1 ? null : indexed.get(index);
    }

    private long search(long start, long end, long value, Bias bias) {
        long range = end - start;
        if (range == 0) {
            return start;
        }
        if (range == 1) {
            T ahead = indexed.get(end);
            T behind = indexed.get(start);
            long v1 = eval.getValue(behind);
            long v2 = eval.getValue(ahead);
            switch (bias) {
                case BACKWARD:
                    return start;
                case FORWARD:
                    return end;
                case NEAREST:
                    if (v1 == value) {
                        return start;
                    } else if (v2 == value) {
                        return end;
                    } else {
                        if (Math.abs(v1 - value) < Math.abs(v2 - value)) {
                            return start;
                        } else {
                            return end;
                        }
                    }
                case NONE:
                    if (v1 == value) {
                        return start;
                    } else if (v2 == value) {
                        return end;
                    } else {
                        return -1;
                    }
                default:
                    throw new AssertionError(bias);

            }
        }
        long mid = start + range / 2;
        long vm = eval.getValue(indexed.get(mid));
        if (value >= vm) {
            return search(mid, end, value, bias);
        } else {
            return search(start, mid, value, bias);
        }
    }

    /**
     * Converts an object into a numeric value that is used to
     * perform binary search
     * @param <T> 
     */
    public interface Evaluator<T> {

        public long getValue(T obj);
    }

    /**
     * Abstraction for list-like things which have a length and indices
     * @param <T> 
     */
    public interface Indexed<T> {

        public T get(long index);

        public long size();
    }

    private static final class ListWrap<T> implements Indexed<T> {

        private final List<T> l;

        ListWrap(List<T> l) {
            this.l = l;
        }

        @Override
        public T get(long index) {
            return l.get((int) index);
        }

        @Override
        public long size() {
            return l.size();
        }
        
        @Override
        public String toString() {
            return super.toString() + '{' + l + '}';
        }
    }
}
