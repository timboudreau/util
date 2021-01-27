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
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongUnaryOperator;
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
 * This class is not thread-safe and the size and contents of the data
 * structuremust not change while a search is being performed.
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
        this(eval, new ListWrap<>(l));
    }

    public BinarySearch(ToLongFunction<? super Object> eval, final long count, LongFunction<T> getter) {
        this((value) -> eval.applyAsLong(value), new LongFunctionWrapper<>(count, getter));
    }

    public BinarySearch(LongIndexedResolvable<T> res) {
        this(res::indexOf, res.size(), res::forIndex);
    }

    /**
     * Binary search over a <i>sorted</i> <code>java.util.List</code>, returning
     * the index of the best match according to the passed <code>Bias</code>, or
     * -1, using the elements' natural order for comparison.
     *
     * @param <T> The type
     * @param in A list of objects
     * @param target The target to search for
     * @param bias In the case of no exact match, returns -1 for Bias.NONE, the
     * nearest value &gt; the target for Bias.FORWARD, the nearest value &lt;
     * the target if Bias.BACKWARD, and whatever comparison value, forward or
     * backward, has a lower absolute value according to the comparator
     * (requires a comparator that returns a distance, not just -1 for less-than
     * and +1 for greater-than.
     * @return The index of the best match according to the bias, or -1
     */
    public static <T extends Comparable<T>> int listBinarySearch(List<? extends T> in, T target, Bias bias) {
        return listBinarySearch(in, target, bias, Comparator.<T>naturalOrder());
    }

    /**
     * Binary search over a <i>sorted</i> <code>java.util.List</code>, returning
     * the index of the best match according to the passed <code>Bias</code>, or
     * -1.
     *
     * @param <T> The type
     * @param in A list of objects
     * @param target The target to search for
     * @param bias In the case of no exact match, returns -1 for Bias.NONE, the
     * nearest value &gt; the target for Bias.FORWARD, the nearest value &lt;
     * the target if Bias.BACKWARD, and whatever comparison value, forward or
     * backward, has a lower absolute value according to the comparator
     * (requires a comparator that returns a distance, not just -1 for less-than
     * and +1 for greater-than.
     * @param comparator A comparator - see note above if you intend to use
     * Bias.NEAREST, since that requires the comparator have certain
     * characteristics
     * @return The index of the best match according to the bias, or -1
     */
    public static <T> int listBinarySearch(List<? extends T> in, T target, Bias bias, Comparator<T> comparator) {
        IntUnaryOperator comparer = index -> comparator.compare(in.get(index), target);
        return search(0, in.size(), comparer, bias);
    }

    /**
     * Search a sorted index-space for a String - just a convenience wrapper
     * around <code>search()</code> that implements string comparison.
     *
     * @param caseInsensitive Whether or not the search is case-sensitive
     * @param forWhat The string being souguht
     * @param start The start index
     * @param end The end index
     * @param fetcher Function that will return a non-null string for any index
     * within the range start (inclusive) to end (exclusive)
     * @param bias The bias when fuzzy-matching
     * @return The index of the best match according to the bias, or -1
     */
    public static int stringSearch(boolean caseInsensitive, String forWhat,
            int start, int end, IntFunction<String> fetcher, Bias bias) {
        notNull("forWhat", forWhat);
        notNull("fetcher", fetcher);
        if (start >= end) {
            return -1;
        }
        IntUnaryOperator comparer = index -> {
            String test = fetcher.apply(index);
//            System.out.println("TEST " + index + " with " + test
//                    + " against " + forWhat + " gets "
//                    + (caseInsensitive ? forWhat.compareToIgnoreCase(test)
//                            : forWhat.compareTo(test)));
            return caseInsensitive ? forWhat.compareToIgnoreCase(test)
                    : forWhat.compareTo(test);
        };
        return search(start, end, comparer, bias);
    }

    /**
     * Performs the mechanics of binary search, with user-supplied comparisons,
     * with no assumptions about the type of the object being sought: The
     * IntUnaryOperator knows what is being sought, and can answer questions
     * about whether the object referenced by an index passed to it is greater
     * than, less than or equal to the value being sought, following the same
     * contract as values retrievd from Comparator.compare().
     * <p>
     * As with any implementation of binary search, the value-space is assumed
     * to be sorted. For example, if you were searching over a sorted List, for
     * a String, you would implement it as
     * <code>(index) -&gt; theList.get(index).compareTo(targetString)</code> -
     * we just omit the assumption of the thing being searched being a list, so
     * that it can be used over any sorted store of records that can be
     * compared.
     * </p><p>
     * Note: Bias.NEAREST will only work correctly if the value returned by the
     * comparison function is a <i>distance</i> (as String or Integer return
     * from their comparison methods); if the underlying comparator simply
     * returns 1 or -1, then whether or not the returned fuzzy result is greater
     * or less than the sought value is undefined - it may be one or the other.
     * </p><p>
     * The search space may contain duplicate elements (more than one
     * consecutive index that the IntUnaryOperator returns 0 for), in which case
     * the index of <i>some</i> one of those elements is returned, but which one
     * is unspecified.
     * </p>
     *
     * @param start The start point in an abstract sequential space of sorted
     * values
     * @param end The end point in an abstract sequential space of sorted values
     * @param comparer The thing that compares the value at an index it is
     * passed with the value being searched-for, and responds with higher, lower
     * or equal
     * @param bias The bias to apply when answering questions - whether to allow
     * inexact answers and if so, inexact in what direction
     * @return An integer offset that is either an exact match, or the nearest
     * according to the bias
     */
    public static int search(int start, int end, IntUnaryOperator comparer, Bias bias) {
        // Some of the use cases for this methd involve an IntUnaryOperator which
        // will actually read chunks of a file to compare - so if we read a value,
        // the last two parameters ensure we don't read the same value again to make
        // a comparison in the next round
        return _search(start, end - 1, comparer, bias, -1, 0);
    }

    private static int _search(int start, int stop, IntUnaryOperator comparer, Bias bias,
            int cachedComparison, int cachedComparisonDirectionType) {
        if (start < 0 || stop < 0) {
            throw new IllegalArgumentException("Negative start or stop: " + start
                    + " thru " + stop + " bias " + bias + " cachedComparisonDirectionType "
                    + cachedComparisonDirectionType + " cachedComparison " + cachedComparison);
        }
        int range = stop - start;
        // If we are coming from a previous call to _search() and the bias is not NONE,
        // we may have already looked up the start or end comparison - since in some
        // use cases, fetching objects to compare may involve expensive I/O, we cache the
        // value of the preceding lookup when we recurse, if we will need that value
        // in the next stack frame
        int startComparison = cachedComparisonDirectionType == -1
                ? cachedComparison : comparer.applyAsInt(start);
        if (range == 0) {
            // We hit the end of any possible matches - start and end are
            // the same, so return that value if it satisfies the bias
            switch (bias) {
                case NONE:
                    return startComparison == 0 ? start : -1;
                case BACKWARD:
                    return startComparison >= 0 ? start : -1;
                case FORWARD:
                    return startComparison <= 0 ? start : -1;
                case NEAREST:
                    return start;
            }
            return bias == Bias.NONE ? startComparison == 0 ? start : -1 : start;
        }
        int endComparision = cachedComparisonDirectionType == 1 ? cachedComparison : comparer.applyAsInt(stop);

        if (endComparision > 0 && startComparison < 0) {
            throw new IllegalArgumentException("Items are not sorted - start " + start + " end " + stop
                    + " but comparisons are " + startComparison + " / " + endComparision);
        }

        if (startComparison == 0) { // exact match on start
            return start;
        } else if (endComparision == 0) { // exact match on end
            return stop;
        } else if (range == 1) {
            // we are out of ranges to divide-and-conquer over
            // determine if the start or end element can satisfy the
            // constraint of the bias, and return whichever is the best
            // match, or -1 if none
            boolean differentSign = (startComparison ^ endComparision) < 0;
            switch (bias) {
                case BACKWARD:
                    if (startComparison < 0) {
                        // We ran of the end without encountering any hit
                        return -1;
                    }
                    // Return the nearest unless they point in two different
                    // directions and only one can possibly satisfy the constraint
                    return differentSign
                            ? start
                            : Math.abs(endComparision) < Math.abs(startComparison)
                            ? stop
                            : start;
                case FORWARD:
                    if (endComparision > 0) {
                        // We ran of the end without encountering any hit
                        return -1;
                    }
                    // Return the nearest unless they point in two different
                    // directions and only one can possibly satisfy the constraint
                    return differentSign
                            ? stop
                            : Math.abs(endComparision) < Math.abs(startComparison)
                            ? stop
                            : start;
                case NEAREST:
                    // Whichever is "closer" in absolute terms, according to
                    // the comparator is our best result
                    if (Math.abs(startComparison) <= Math.abs(endComparision)) {
                        return start;
                    } else {
                        return stop;
                    }
                case NONE:
                    return -1;
                default:
                    throw new AssertionError(bias);
            }
        }
        // Find the midpoint and recurse
        int mid = start + range / 2;
        int midComparison = comparer.applyAsInt(mid);

        if (midComparison == 0) {
            // We got lucky and stumbled upon a match - return it
            return mid;
        }

        // For some cases, we can avoid comparing the same element in the next
        // recursion, and tighten the range we're examining each time
        int crement;
        switch (bias) {
            // Pending: There are conditions under which it is save to set crement
            // to 1 on forward / backward searches, but we would have to examine
            // the value here and pass it, and an indication if it is the next or
            // previous value, to the next iteration, or we can skip past the
            // best match if the sign changes, and wind up scanning only indices
            // above or below it
            case NONE:
                crement = 1;
                break;
            default:
                crement = 0;
                break;
        }

        // Recurse, comparing middle to end if the middle is below the value we need,
        // and start to middle if it is above
        if (midComparison > 0) {
            // We can cut our iterations significantly on large spaces by moving
            // to the next start point if we know the end point we already have is
            // going to be a better match than the start point we're coming from,
            // so any better match lies after `mid` - if they have the same sign,
            // then we can't possibly already have the best match while subsequent
            // ones are all in the opposite direction
            if (((mid + 1) < Math.max(0, (stop - crement))) && ((midComparison > 0) == (endComparision > 0)) && Math.abs(endComparision) < Math.abs(midComparison)) {
                return _search(mid + 1, Math.max(0, stop - crement), comparer, bias, 0, 0);
            } else {
                return _search(mid, Math.max(0, stop - crement), comparer, bias, midComparison, crement == 0 ? -1 : 0);
            }
        } else {
            // Same as above for start thru mid
            if ((mid - 1) > (start + crement) && ((startComparison > 0) == (midComparison > 0)) && Math.abs(midComparison) > Math.abs(startComparison)) {
                return _search(start + crement, mid - 1, comparer, bias, midComparison, crement == 0 ? 1 : 0);
            } else {
                return _search(start + crement, mid, comparer, bias, midComparison, crement == 0 ? 1 : 0);
            }
        }
    }

    /**
     * Same implementation as search(int, int, IntUnaryOperator, Bias), for a
     * long-indexed space.
     *
     * @param start The start point in an abstract sequential space of sorted
     * values
     * @param end The end point in an abstract sequential space of sorted values
     * @param comparer The thing that compares the value at an index it is
     * passed with the value being searched-for, and responds with higher, lower
     * or equal
     * @param bias The bias to apply when answering questions - whether to allow
     * inexact answers and if so, inexact in what direction
     * @return An integer offset that is either an exact match, or the nearest
     * according to the bias
     */
    public static long search(long start, long end, LongUnaryOperator comparer, Bias bias) {
        // Some of the use cases for this methd involve an IntUnaryOperator which
        // will actually read chunks of a file to compare - so if we read a value,
        // the last two parameters ensure we don't read the same value again to make
        // a comparison in the next round
        return _search(start, end - 1, comparer, bias, -1, 0);
    }

    private static long _search(long start, long stop, LongUnaryOperator comparer, Bias bias,
            long cachedComparison, int cachedComparisonDirectionType) {
        long range = stop - start;
        long startComparison = cachedComparisonDirectionType == -1 ? cachedComparison : comparer.applyAsLong(start);
        if (range == 0) {
            switch (bias) {
                case NONE:
                    return startComparison == 0 ? start : -1;
                case BACKWARD:
                    return startComparison >= 0 ? start : -1;
                case FORWARD:
                    return startComparison <= 0 ? start : -1;
                case NEAREST:
                    return start;
            }
            return bias == Bias.NONE ? startComparison == 0 ? start : -1 : start;
        }
        long endComparision = cachedComparisonDirectionType == 1 ? cachedComparison : comparer.applyAsLong(stop);

        if (endComparision > 0 && startComparison < 0) {
            throw new IllegalArgumentException("Items are not sorted - start " + start + " end " + stop
                    + " but comparisons are " + startComparison + " / " + endComparision);
        }

        if (startComparison == 0) {
            return start;
        } else if (endComparision == 0) {
            return stop;
        } else if (range == 1) {
            boolean differentSign = (startComparison ^ endComparision) < 0;
            switch (bias) {
                case BACKWARD:
                    if (startComparison < 0) {
                        return -1;
                    }
                    return differentSign ? start : Math.abs(endComparision) < Math.abs(startComparison) ? stop : start;
                case FORWARD:
                    if (endComparision > 0) {
                        return -1;
                    }
                    return differentSign ? stop : Math.abs(endComparision) < Math.abs(startComparison) ? stop : start;
                case NEAREST:
                    if (Math.abs(startComparison) <= Math.abs(endComparision)) {
                        return start;
                    } else {
                        return stop;
                    }
                case NONE:
                    return -1;
                default:
                    throw new AssertionError(bias);
            }
        }
        long mid = start + range / 2;
        long midComparison = comparer.applyAsLong(mid);

        // We stumbled upon a match - return it
        if (midComparison == 0) {
            return mid;
        }

        int crement;
        switch (bias) {
            // Pending: There are conditions under which it is save to set crement
            // to 1 on forward / backward searches, but we would have to examine
            // the value here and pass it, and an indication if it is the next or
            // previous value, to the next iteration, or we can skip past the
            // best match if the sign changes, and wind up scanning only indices
            // above or below it
            case NONE:
                crement = 1;
                break;
            default:
                crement = 0;
                break;
        }

        if (midComparison > 0) {
            return _search(mid, stop - crement, comparer, bias, midComparison, crement == 0 ? -1 : 0);
        } else {
            return _search(start + crement, mid, comparer, bias, midComparison, crement == 0 ? 1 : 0);
        }
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

        long getValue(T obj);

        @Override
        default long applyAsLong(T value) {
            return getValue(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        default long indexOf(Object obj) {
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

        T get(long index);

        long size();
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
