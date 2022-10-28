/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.function.iteration;

import com.mastfrog.function.throwing.ThrowingBiConsumer;
import com.mastfrog.function.throwing.ThrowingBiPredicate;
import com.mastfrog.function.throwing.ThrowingConsumer;
import com.mastfrog.function.throwing.ThrowingIntConsumer;
import com.mastfrog.function.throwing.ThrowingLongConsumer;
import com.mastfrog.function.throwing.ThrowingPredicate;
import com.mastfrog.function.throwing.io.IOBiConsumer;
import com.mastfrog.function.throwing.io.IOConsumer;
import com.mastfrog.function.throwing.io.IOPredicate;
import static com.mastfrog.util.preconditions.Checks.nonZero;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

/**
 * Equivalents of <code>Map.forEach()</code> and
 * <code>Collection.forEach()</code> that take throwing functions. Note that any
 * thrown exception will still be thrown; it will just be rethrown as an
 * undeclared throwable (no wrapping in a runtime exception).
 *
 * @author Tim Boudreau
 */
public final class Iterate {

    /**
     * Equivalent of <code>Map.forEach()</code> with a bi-consumer that can
     * throw.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param map A map
     * @param consumer A bi-consumer
     */
    public static <T, R> void each(Map<? extends T, ? extends R> map,
            ThrowingBiConsumer<? super T, ? super R> consumer) {
        map.forEach(consumer.toNonThrowing());
    }

    /**
     * Equivalent of `Collection.forEach()` that takes a consumer which can
     * throw.
     *
     * @param <T> The type
     * @param coll A collection
     * @param consumer A consumer
     */
    public static <T> void each(Iterable<? extends T> coll, ThrowingConsumer<? super T> consumer) {
        coll.forEach(consumer.toNonThrowing());
    }

    /**
     * Iterate an array with a throwing consumer.
     *
     * @param <T> The type
     * @param arr An array
     * @param consumer a consumer
     */
    public static <T> void each(T[] arr, ThrowingConsumer<? super T> consumer) {
        Consumer<? super T> c = consumer.toNonThrowing();
        for (int i = 0; i < arr.length; i++) {
            c.accept(arr[i]);
        }
    }

    /**
     * Equivalent of <code>Map.forEach()</code> with a bi-consumer that can
     * throw IOException.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param map A map
     * @param consumer A bi-consumer
     */
    public static <T, R> void eachIO(Map<? extends T, ? extends R> map,
            IOBiConsumer<? super T, ? super R> consumer) {
        map.forEach(consumer.toNonThrowing());
    }

    /**
     * Equivalent of `Collection.forEach()` that takes a consumer which can
     * throw.
     *
     * @param <T> The type
     * @param coll A collection
     * @param consumer A consumer
     */
    public static <T> void eachIO(Iterable<? extends T> coll, IOConsumer<? super T> consumer) {
        coll.forEach(consumer.toNonThrowing());
    }

    /**
     * Iterate elements of the collection until an element is encountered for
     * which the passed predicate returns false.
     *
     * @param <T> The type
     * @param coll A collection
     * @param test A test
     * @return the number of elements the passed predicate returned true for
     */
    public static <T> int each(Collection<? extends T> coll, ThrowingPredicate<? super T> test) {
        Predicate<? super T> c = test.toNonThrowing();
        int result = 0;
        Iterator<? extends T> iter = coll.iterator();
        while (iter.hasNext()) {
            if (!c.test(iter.next())) {
                break;
            }
            result++;
        }
        return result;
    }

    /**
     * Iterate elements of the collection until an element is encountered for
     * which the passed predicate returns false.
     *
     * @param <T> The type
     * @param coll A collection
     * @param test A predicate
     * @return the number of elements for which the predicate returned true
     */
    public static <T> int eachIO(Collection<? extends T> coll, IOPredicate<? super T> test) {
        Predicate<? super T> c = test.toNonThrowing();
        int result = 0;
        Iterator<? extends T> iter = coll.iterator();
        while (iter.hasNext()) {
            if (!c.test(iter.next())) {
                break;
            }
            result++;
        }
        return result;
    }

    /**
     * Iterate key/value pairs of the map until a pair is encountered for which
     * the passed predicate returns false.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param coll A collection
     * @param consumer A predicate
     * @return the number of elements for which the predicate returned true
     */
    public static <T, R> int each(Map<T, R> coll, ThrowingBiPredicate<? super T, ? super R> consumer) {
        BiPredicate<? super T, ? super R> c = consumer.toNonThrowing();
        int result = 0;
        Iterator<Map.Entry<T, R>> iter = coll.entrySet().iterator();
        while (iter.hasNext()) {
            result++;
            Map.Entry<? extends T, ? extends R> e = iter.next();
            if (!c.test(e.getKey(), e.getValue())) {
                break;
            }
        }
        return result;
    }

    /**
     * Iterate a collection, passing elements to an IterationConsumer - often
     * when concatenating strings, the first and/or last elements are treated
     * specially with regard to prepending spaces, delimiters, etc.
     *
     * @param <T> A type
     * @param collection A collection of objects
     * @param consumer A consumer
     * @return the number of items the consumer was called for
     */
    public static <T> int iterate(Iterable<? extends T> collection, IterationConsumer<? super T> consumer) {
        boolean first = true;
        int result = 0;
        for (Iterator<? extends T> it = collection.iterator(); it.hasNext();) {
            T obj = it.next();
            boolean last = !it.hasNext();
            consumer.onItem(obj, first, last);
            result++;
            first = false;
        }
        return result;
    }

    /**
     * Iterate an array, passing elements to an IterationConsumer - often when
     * concatenating strings, the first and/or last elements are treated
     * specially with regard to prepending spaces, delimiters, etc.
     *
     * @param <T> A type
     * @param collection An array
     * @param consumer A consumer
     * @return the number of items the consumer was called for
     */
    public static <T> int iterate(T[] collection, IterationConsumer<? super T> consumer) {
        int result = 0;
        int last = collection.length - 1;
        for (int i = 0; i < collection.length; i++) {
            consumer.onItem(collection[i], i == 0, i == last);
        }
        return result;
    }

    public static void monotonically(int from, int toExclusive, IntConsumer ic) {
        int direction = toExclusive < from ? -1 : 1;
        for (int i = from; direction == -1 ? i > toExclusive : i < toExclusive; i += direction) {
            ic.accept(i);
        }
    }

    public static void monotonicallyThrowing(int from, int toExclusive, ThrowingIntConsumer ic) {
        int direction = toExclusive < from ? -1 : 1;
        IntConsumer c = ic.toNonThrowing();
        for (int i = from; direction == -1 ? i > toExclusive : i < toExclusive; i += direction) {
            c.accept(i);
        }
    }

    public static void longsMonotonically(long from, long toExclusive, LongConsumer ic) {
        int direction = toExclusive < from ? -1 : 1;
        for (long i = from; direction == -1 ? i > toExclusive : i < toExclusive; i += direction) {
            ic.accept(i);
        }
    }

    public static void longsMonotonicallyThrowing(long from, long toExclusive, ThrowingLongConsumer ic) {
        LongConsumer c = ic.toNonThrowing();
        int direction = toExclusive < from ? -1 : 1;
        for (long i = from; direction == -1 ? i > toExclusive : i < toExclusive; i += direction) {
            c.accept(i);
        }
    }

    public static void monotonicallyBy(int by, int from, int toExclusive, IntConsumer ic) {
        nonZero("by", by);
        for (int i = from; i < toExclusive; i += by) {
            ic.accept(i);
        }
    }

    public static void monotonicallyByThrowing(int by, int from, int toExclusive, ThrowingIntConsumer ic) {
        nonZero("by", by);
        IntConsumer c = ic.toNonThrowing();
        for (int i = from; i < toExclusive; i += by) {
            c.accept(i);
        }
    }

    public static void longsMonotonicallyBy(long by, long from, long toExclusive, LongConsumer ic) {
        nonZero("by", by);
        for (long i = from; i < toExclusive; i += by) {
            ic.accept(i);
        }
    }

    public static void longsMonotonicallyByThrowing(long by, long from, long toExclusive, ThrowingLongConsumer ic) {
        nonZero("by", by);
        LongConsumer c = ic.toNonThrowing();
        for (long i = from; i < toExclusive; i += by) {
            c.accept(i);
        }
    }

    private Iterate() {
        throw new AssertionError();
    }
}
