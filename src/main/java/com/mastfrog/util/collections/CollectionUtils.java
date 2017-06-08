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
package com.mastfrog.util.collections;

import com.mastfrog.util.Checks;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Handles a few collections omissions
 *
 * @author Tim Boudreau
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T, R> Map<R, T> reverse(Map<T, R> map) {
        Map<R, T> result = map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>()
                : new HashMap<>();
        for (Map.Entry<T, R> e : map.entrySet()) {
            result.put(e.getValue(), e.getKey());
        }
        return result;
    }

    /**
     * Create a map builder.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @return A map builder
     */
    public static <T, R> MapBuilder2<T, R> map() {
        return new MapBuilder2Impl<T, R>();
    }

    /**
     * Create a map builder in-progress setting the first key to the passed
     * value.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param key The initial key
     * @return A ValueBuilder which will return a MapBuilder when map.
     */
    public static <T, R> MapBuilder2.ValueBuilder<T, R> map(T key) {
        return new MapBuilder2Impl<T, R>().map(key);
    }

    /**
     * Create a map which wraps another and converts its keys to some other
     * object type - useful for things like case-insensitive String maps.
     *
     * @param <From> The key type to use
     * @param <T> The original key type
     * @param <R> The value type
     * @param from The key type to use
     * @param delegate The original map
     * @param converter A converter which can interconvert objects for lookups
     * @return A map that proxies the original
     */
    public static <From, T, R> Map<From, R> convertedKeyMap(Class<From> from, Map<T, R> delegate, Converter<T, From> converter) {
        return new ConvertedMap<From, T, R, From>(from, delegate, converter);
    }

    /**
     * Creates a Map which will case-insensitively match CharSequences for its
     * keys.
     *
     * @param <R> The value type
     * @return A map
     */
    @SuppressWarnings("unchecked")
    public static <R> Map<CharSequence, R> caseInsensitiveStringMap() {
        Converter<CharSequenceKey<CharSequence>, CharSequence> converter = CharSequenceKey.<CharSequence>converter();
        return new ConvertedMap(CharSequence.class, new HashMap<>(), converter);
    }

    /**
     * Creates a Map which will case insensitively match CharSequences for its
     * keys, initially populated from the passed map.Changes in the passed map
     * are not reflected - the resulting map is independent.
     *
     * @param <T> The key type of the inbound map
     * @param <R> The value type
     * @param map A map
     * @return A new map
     */
    public static <T extends CharSequence, R> Map<CharSequence, R> caseInsensitiveStringMap(Map<T, R> map) {
        Map<CharSequence, R> result = caseInsensitiveStringMap();
        for (Map.Entry<T, R> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Get a primitive integer map backed by an array and binary search (removes
     * are expensive).
     *
     * @param <T> The value type
     * @return A map
     */
    public static <T> IntMap<T> intMap() {
        return new ArrayIntMap<>();
    }

    /**
     * Get a primitive integer map backed by an array and binary search (removes
     * are expensive).
     *
     * @param <T> The value type
     * @param toCopy A map to copy
     * @return A map
     */
    public static <T> IntMap<T> intMap(Map<Integer, T> toCopy) {
        return new ArrayIntMap<>(toCopy);
    }

    /**
     * Get a primitive integer map backed by an array and binary search (removes
     * are expensive).
     *
     * @param <T> The value type
     * @param initialCapacity The initial backing array sizes
     * @return A map
     */
    public static <T> IntMap<T> intMap(int initialCapacity) {
        return new ArrayIntMap<>(initialCapacity);
    }

    /**
     * A lightweight List implementation which can only ever contain a single
     * item. If the existing item is removed, a new one can be added, but
     * attempting to add more than one results in an IndexOutOfBoundsException.
     * <p/>
     * Useful in situations where it is known that the list will only ever
     * contain at most one item, and minimizing memory allocation is a concern.
     * <p/>
     * The returned list may not have null as a member -
     * <code>map(0, null)</code> is equivalent to clear();
     *
     * @param <T> The type
     * @return A list that can contain 0 or 1 item
     */
    public static <T> List<T> oneItemList() {
        return new SingleItemList<T>();
    }

    /**
     * A lightweight List implementation which can only ever contain a single
     * item. If the existing item is removed, a new one can be added, but
     * attempting to add more than one results in an IndexOutOfBoundsException.
     * <p/>
     * Useful in situations where it is known that the list will only ever
     * contain at most one item, and minimizing memory allocation is a concern.
     *
     * @param <T> The type
     * @param item The single item it should initially contain
     * @return A list that can contain 0 or 1 item
     */
    public static <T> List<T> oneItemList(T item) {
        return new SingleItemList<>(item);
    }

    /**
     * Create a reversed view of a list. Unlike Collections.reverse() this does
     * not modify the original list; it also does not copy the original list.
     * This does mean that modifications to the original list are visible while
     * iterating the child list, and appropriate steps should be taken to avoid
     * commodification.
     *
     * @param <T> A type
     * @param list A list
     * @return A reversed view of the passed list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> reversed(List<T> list) {
        if (list instanceof ReversedList) {
            return ((ReversedList<T>) list).delegate();
        }
        return new ReversedList<>(list);
    }

    /**
     * Create a list which wrappers another list and converts the contents on
     * demand
     *
     * @param <T> The old type
     * @param <R> The new type
     * @param list A list
     * @param converter A thing which converts between types
     * @param fromType The old type
     * @param toType The new type
     * @return A list
     */
    public static <T, R> List<R> convertedList(List<T> list, Converter<R, T> converter, Class<T> fromType, Class<R> toType) {
        return new ConvertList<>(toType, fromType, list, converter);
    }

    /**
     * Generic munging - treat a List&lt;String&gt; as an unmodifiable
     * List&lt;CharSequence&gt; and so forth.
     *
     * @param <T> The target type
     * @param l The list
     * @return An unmodifiable list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> generalize(List<? extends T> l) {
        return Collections.<T>unmodifiableList((List) l);
    }

    /**
     * Create an iterator which converts objects from one iterator into another
     * kind of object on the fly.
     *
     * @param c A thing that converts objects
     * @param iter An iterator
     * @return An iterator
     */
    public static <T, R> ListIterator<R> convertedIterator(Converter<R, T> c, Iterator<T> iter) {
        return convertedIterator(new WrapAsListIterator<>(iter), c);
    }

    /**
     * Covert a list iterator of one object type to another type of object
     *
     * @param iter An iterator
     * @param c The thing that converts objects
     * @return A list iterator
     */
    public static <T, R> ListIterator<R> convertedIterator(ListIterator<T> iter, Converter<R, T> c) {
        return new ConvertIterator<>(iter, c);
    }

    /**
     * Creates a list which uses object identity, not equals() to determine if
     * an object is a member or not, so methods such as remove() will only match
     * the same object even if two objects which equals() the object to remove
     * are available.
     * <p/>
     * The resulting List's own equals() and hashCode() methods are also
     * identity checks.
     */
    public static <T> List<T> identityList(Collection<T> collection) {
        return new IdentityList<>(collection);
    }

    /**
     * Create a List which uses identity comparisons to determine membership
     *
     * @param <T> A type
     * @return A lisst
     */
    public static <T> List<T> newIdentityList() {
        return new IdentityList<>();
    }

    /**
     * Invert a converter
     *
     * @param c A converter
     * @return A converter
     */
    public static <T, R> Converter<T, R> reverseConverter(Converter<R, T> c) {
        return new ReverseConverter<>(c);
    }

    /**
     * Wrap an iterator in JDK 6's Iterable
     *
     * @param iterator An iterator
     * @return An Iterable
     */
    public static <T> Iterable<T> toIterable(final Iterator<T> iterator) {
        return new IteratorIterable<>(iterator);
    }

    /**
     * Get an iterator which merges several iterators.
     *
     * @param <T> The type
     * @param iterators Some iterators
     * @return An iterator
     */
    public static <T> Iterator<T> combine(Collection<Iterator<T>> iterators) {
        return new MergeIterator<>(iterators);
    }

    /**
     * Combine two iterators
     *
     * @param <T> The type
     * @param a One iterator
     * @param b Another iterator
     * @return An iterator
     */
    public static <T> Iterator<T> combine(Iterator<T> a, Iterator<T> b) {
        Checks.notNull("a", a);
        Checks.notNull("b", b);
        return new MergeIterator<>(Arrays.asList(a, b));
    }

    public static <T> Iterator<T> singletonIterator(T obj) {
        return new SingletonIterator<T>(obj);
    }

    public static <T> Iterator<T> toIterator(T[] array) {
        Checks.notNull("array", array);
        return new ArrayIterator<T>(array);
    }

    public static <T> Iterable<T> toIterable(T[] array) {
        Checks.notNull("array", array);
        return toIterable(toIterator(array));
    }

    public static <T> Enumeration<T> toEnumeration(Iterable<T> iter) {
        Checks.notNull("iter", iter);
        return toEnumeration(iter.iterator());
    }

    public static <T> Enumeration<T> toEnumeration(Iterator<T> iter) {
        Checks.notNull("iter", iter);
        return new EnumerationAdapter<>(iter);
    }

    public static <T> Iterator<T> toReverseIterator(T[] array) {
        Checks.notNull("array", array);
        return new ReverseArrayIterator<T>(array);
    }

    /**
     * Get an iterator whose implementation is synchronized, for the case where
     * multiple threads will take items.
     *
     * @param <T> The type
     * @param iter The raw iterator
     * @return an AtomicIterator
     */
    public static <T> AtomicIterator<T> synchronizedIterator(Iterator<T> iter) {
        return new AtomicIteratorImpl<T>(iter);
    }

    /**
     * Iterator with a method which will do both the hasNext() and next() calls
     * in a synchronized block, for atomicity when being used across multiple
     * items.
     *
     * @param <T> The type
     */
    public interface AtomicIterator<T> extends Iterator<T> {

        /**
         * Get the next item, if any
         *
         * @return null if no next item, otherwise the next item
         */
        public T getIfHasNext();
    }

    private static final class AtomicIteratorImpl<T> implements AtomicIterator<T> {

        private final Iterator<T> iter;

        AtomicIteratorImpl(Iterator<T> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            synchronized (this) {
                return iter.hasNext();
            }
        }

        @Override
        public T next() {
            synchronized (this) {
                T result = iter.next();
                if (result == null) {
                    throw new IllegalStateException("Null elements not permitted");
                }
                return result;
            }
        }

        public T getIfHasNext() {
            synchronized (this) {
                if (iter.hasNext()) {
                    return next();
                } else {
                    return null;
                }
            }
        }

        @Override
        public void remove() {
            synchronized (this) {
                iter.remove();
            }
        }
    }

    private static final class ArrayIterator<T> implements Iterator<T> {

        private final T[] items;
        private int ix = 0;

        public ArrayIterator(T[] items) {
            this.items = items;
        }

        @Override
        public boolean hasNext() {
            return ix < items.length;
        }

        @Override
        public T next() {
            return items[ix++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot delete from an array");
        }
    }

    private static final class ReverseArrayIterator<T> implements Iterator<T> {

        private final T[] items;
        private int ix;

        public ReverseArrayIterator(T[] items) {
            this.items = items;
            ix = items.length - 1;
        }

        @Override
        public boolean hasNext() {
            return ix >= 0;
        }

        @Override
        public T next() {
            return items[ix--];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot delete from an array");
        }
    }

    private static final class MergeIterator<T> implements Iterator<T> {

        private final LinkedList<Iterator<T>> iterators = new LinkedList<>();

        MergeIterator(Collection<Iterator<T>> iterators) {
            this.iterators.addAll(iterators);
        }

        private Iterator<T> iter() {
            if (iterators.isEmpty()) {
                return null;
            }
            Iterator<T> result = iterators.get(0);
            if (!result.hasNext()) {
                iterators.remove(0);
                return iter();
            }
            return result;
        }

        @Override
        public boolean hasNext() {
            Iterator<T> curr = iter();
            return curr == null ? false : curr.hasNext();
        }

        @Override
        public T next() {
            Iterator<T> iter = iter();
            if (iter == null) {
                throw new NoSuchElementException();
            }
            return iter.next();
        }

        @Override
        public void remove() {
            Iterator<T> iter = iter();
            if (iter == null) {
                throw new NoSuchElementException();
            }
            iter.remove();
        }
    }

    private static class IteratorIterable<T> implements Iterable<T> {

        private final Iterator<T> iter;

        public IteratorIterable(Iterator<T> iter) {
            this.iter = iter;
        }

        @Override
        public Iterator<T> iterator() {
            return iter;
        }
    }

    public static <T> Iterable<T> toIterable(final Enumeration<T> enumeration) {
        return new EnumIterable<>(enumeration);
    }

    private static final class EnumIterable<T> implements Iterable<T> {

        private final Enumeration<T> en;

        public EnumIterable(Enumeration<T> en) {
            this.en = en;
        }

        @Override
        public Iterator<T> iterator() {
            return toIterator(en);
        }
    }

    public static <T> Iterator<T> toIterator(final Enumeration<T> enumeration) {
        return new EnumIterator<>(enumeration);
    }

    private static final class EnumIterator<T> implements Iterator<T>, Iterable<T> {

        private final Enumeration<T> enumeration;

        public EnumIterator(Enumeration<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
            return enumeration.nextElement();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public Iterator<T> iterator() {
            return new EnumIterator<>(enumeration);
        }
    }

    static class SingletonIterator<T> implements Iterator<T> {

        private final T object;
        private boolean done;

        public SingletonIterator(T object) {
            this.object = object;
        }

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public T next() {
            if (done) {
                throw new NoSuchElementException();
            }
            done = true;
            return object;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (!done) {
                done = true;
                action.accept(object);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static Iterator<Integer> toIterator(final int[] vals) {
        return new IntArrayIterator(vals);
    }

    public static Iterator<Long> toIterator(final long[] vals) {
        return new LongArrayIterator(vals);
    }
    
    public static Interator toInterator(final int[] vals) {
        return new ArrayInterator(vals);
    }
    
    public static Longerator toLongerator(final long[] vals) {
        return new ArrayLongerator(vals);
    }

    private static final class IntArrayIterator implements Iterator<Integer> {

        private final int[] vals;

        public IntArrayIterator(int[] vals) {
            this.vals = vals;
        }
        int ix = 0;

        @Override
        public boolean hasNext() {
            return ix < vals.length;
        }

        @Override
        public Integer next() {
            return vals[ix++];
        }

        @Override
        public void forEachRemaining(Consumer<? super Integer> action) {
            for (int i = 0; i < vals.length; i++) {
                action.accept(vals[i]);
            }
        }
    }

    private static final class LongArrayIterator implements Iterator<Long> {

        private final long[] vals;

        public LongArrayIterator(long[] vals) {
            this.vals = vals;
        }
        int ix = 0;

        @Override
        public boolean hasNext() {
            return ix < vals.length;
        }

        @Override
        public Long next() {
            return vals[ix++];
        }

        @Override
        public void forEachRemaining(Consumer<? super Long> action) {
            for (int i = 0; i < vals.length; i++) {
                action.accept(vals[i]);
            }
        }
    }

    private static final class ArrayInterator implements Interator {

        private final int[] vals;

        public ArrayInterator(int[] vals) {
            this.vals = vals;
        }
        int ix = 0;

        @Override
        public boolean hasNext() {
            return ix < vals.length;
        }

        @Override
        public int next() {
            return vals[ix++];
        }
    }

    private static final class ArrayLongerator implements Longerator {

        private final long[] vals;

        public ArrayLongerator(long[] vals) {
            this.vals = vals;
        }
        int ix =0;

        @Override
        public long next() {
            return vals[ix++];
        }

        @Override
        public boolean hasNext() {
            return ix < vals.length;
        }
    }
}
