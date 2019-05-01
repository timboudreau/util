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

import com.mastfrog.abstractions.list.LongIndexed;
import com.mastfrog.abstractions.list.LongIndexedResolvable;
import com.mastfrog.abstractions.list.LongResolvable;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * General-purpose binary search algorithm; you pass in an array or list wrapped
 * in an instance of <code>Indexed</code>, and an <code>Evaluator</code> which
 * converts the contents of the list into numbers used by the binary search
 * algorithm. Note that the data (as returned by the Indexed array/list)
 * <b><i>must be in order from low to high</i></b>. The indices need not be
 * contiguous (presumably they are not or you wouldn't be using this class), but
 * they must be sorted. If assertions are enabled, this is enforced; if not,
 * very bad things (endless loops, etc.) can happen as a consequence of passing
 * unsorted data in.
 * <p/>
 * This class is not thread-safe and the size and contents of the
 * data structuremust not change while a search is being performed.
 *
 * @author Tim Boudreau
 */
public class BinarySearch<T> {

    private final LongResolvable eval;
    private final LongIndexed<T> indexed;

    /**
     * Create a new binary search.
     *
     * @param eval The thing which converts elements into numbers
     * @param indexed A collection, list or array
     */
    public BinarySearch(LongResolvable eval, LongIndexed<T> indexed) {
        this.eval = eval;
        this.indexed = indexed;
        assert checkSorted();
    }

    public BinarySearch(LongResolvable eval, List<T> l) {
        this(eval, new ListWrap<T>(l));
    }

    public BinarySearch(ToLongFunction<? super Object> eval, final long count, LongFunction<T> getter) {
        this((value) -> {
            return eval.applyAsLong(value);
        }, new LongFunctionWrapper<T>(count, getter));
    }

    public BinarySearch(LongIndexedResolvable<T> res) {
        this(res::indexOf, res.size(), res::forIndex);
    }

    private static final class LongFunctionWrapper<T> implements LongIndexed<T> {

        private final long count;
        private final LongFunction<T> getter;

        public LongFunctionWrapper(long count, LongFunction<T> getter) {
            this.count = count;
            this.getter = getter;
        }

        @Override
        public T forIndex(long index) {
            return getter.apply(index);
        }

        @Override
        public long size() {
            return count;
        }
    }

    public static <R extends Number> BinarySearch<R> binarySearch(LongFunction<R> indexer, long count) {
        LongIndexedResolvable<R> ix = new LongIndexedResolvable<R>() {
            @Override
            public R forIndex(long index) {
                return indexer.apply(index);
            }

            @Override
            public long size() {
                return count;
            }

            @Override
            public long indexOf(Object obj) {
                return ((Number) obj).longValue();
            }
        };
        return new BinarySearch<>(ix, ix);
    }

    public static <R extends Number> BinarySearch<R> binarySearch(long count, Function<Long, R> indexer) {
        Evaluator<R> eval = Number::longValue;
        LongIndexed<R> ix = new LongIndexed<R>() {
            @Override
            public R forIndex(long index) {
                return indexer.apply(index);
            }

            @Override
            public long size() {
                return count;
            }
        };
        return new BinarySearch<>(eval, ix);
    }

    private boolean checkSorted() {
        long val = Long.MIN_VALUE;
        long sz = indexed.size();
        for (long i = 0; i < sz; i++) {
            T t = indexed.forIndex(i);
            long nue = eval.indexOf(t);
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
        return search(0, indexed.size() - 1, value, bias);
    }

    public T match(T prototype, Bias bias) {
        long value = eval.indexOf(prototype);
        long index = search(value, bias);
        return index == -1 ? null : indexed.forIndex(index);
    }

    public T searchFor(long value, Bias bias) {
        long index = search(value, bias);
        return index == -1 ? null : indexed.forIndex(index);
    }

    private long search(long start, long end, long value, Bias bias) {
        long range = end - start;
        if (range == 0) {
            return start;
        }
        if (range == 1) {
            T ahead = indexed.forIndex(end);
            T behind = indexed.forIndex(start);
            long v1 = eval.indexOf(behind);
            long v2 = eval.indexOf(ahead);
            if (value == v1) {
                return start;
            } else if (value == v2) {
                return end;
            }
            switch (bias) {
                case BACKWARD:
                    if (v2 < value) {
                        return end;
                    }
                    if (v1 > value) {
                        return -1;
                    }
                    return start;
                case FORWARD:
                    if (v1 > value) {
                        return start;
                    }
                    if (v2 < value) {
                        return -1;
                    }
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
        long vm = eval.indexOf(indexed.forIndex(mid));
        if (value >= vm) {
            return search(mid, end, value, bias);
        } else {
            return search(start, mid, value, bias);
        }
    }

    /**
     * Converts an object into a numeric value that is used to perform binary
     * search
     *
     * @param <T>
     * @deprecated Use LongResolvable
     */
    @Deprecated
    public interface Evaluator<T> extends ToLongFunction<T>, LongResolvable {

        public long getValue(T obj);

        @Override
        public default long applyAsLong(T value) {
            return getValue(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public default long indexOf(Object obj) {
            return getValue((T) obj);
        }
    }

    /**
     * Abstraction for list-like things which have a length and indices
     *
     * @param <T>
     * @deprecated use LongIndexed
     */
    @Deprecated
    public interface Indexed<T> extends LongIndexed<T> {

        public T get(long index);

        public long size();
    }

    private static final class ListWrap<T> implements LongIndexed<T> {

        private final List<T> l;

        ListWrap(List<T> l) {
            this.l = l;
        }

        @Override
        public T forIndex(long index) {
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
