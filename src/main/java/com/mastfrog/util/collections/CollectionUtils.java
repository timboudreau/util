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
import static com.mastfrog.util.Checks.notNull;
import com.mastfrog.util.collections.MapBuilder2.HashingMapBuilder;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Handles a few collections omissions
 *
 * @author Tim Boudreau
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T, R> Set<R> transform(Set<T> list, Function<T, R> xform) {
        Set<R> result = new LinkedHashSet<>();
        list.stream().forEach(t -> {
            R r = xform.apply(t);
            if (r != null) {
                result.add(r);
            }
        });
        return result;
    }

    public static <T, R> List<R> transform(List<T> list, Function<T, R> xform) {
        List<R> result = new ArrayList<>();
        list.stream().forEach(t -> {
            R r = xform.apply(t);
            if (r != null) {
                result.add(r);
            }
        });
        return result;
    }

    @SafeVarargs
    public static <T, R> List<R> transform(Function<T, R> xform, T... args) {
        List<R> result = new ArrayList<>();
        for (T obj : args) {
            R r = xform.apply(obj);
            if (r != null) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Convert a set of objects to a Map where the value for each entry in the
     * set is <code>true</code>.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> Map<T, Boolean> toMap(Set<T> set) {
        Map<T, Boolean> result = new HashMap<>();
        for (T t : set) {
            result.put(t, true);
        }
        return result;
    }

    public static <T, R> Map<T, R> toMap(Collection<R> coll, Function<R, T> func) {
        Map<T, R> result = new LinkedHashMap<>();
        coll.stream().forEach(r -> {
            T key = func.apply(r);
            result.put(key, r);
        });
        return result;
    }

    /**
     * Convert a map of objects to booleans to a set of those keys for which the
     * value is <code>true</code>.
     *
     * @param <T> The type
     * @param map A map
     * @return A set
     */
    public static <T> Set<T> toSet(Map<T, Boolean> map) {
        Set<T> result = new HashSet<>(map.size());
        for (Map.Entry<T, Boolean> e : map.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * Create a set from some arguments.
     *
     * @param <T> The type
     * @param args The arguments
     * @return A set
     */
    public static <T> Set<T> setOf(T a) {
        return Collections.singleton(a);
    }

    /**
     * Create a set from some arguments.
     *
     * @param <T> The type
     * @param args The arguments
     * @return A set
     */
    public static <T> Set<T> setOf(T a, T b) {
        Set<T> set = new LinkedHashSet<>(2);
        set.add(a);
        set.add(b);
        return set;
    }

    /**
     * Create a set from some arguments.
     *
     * @param <T> The type
     * @return A set
     */
    public static <T> Set<T> setOf(T a, T b, T c) {
        Set<T> set = new LinkedHashSet<>(2);
        set.add(a);
        set.add(b);
        set.add(c);
        return set;
    }

    /**
     * Create a set from some arguments.
     *
     * @param <T> The type
     * @param args The arguments
     * @return A set
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... args) {
        if (args.length == 0) {
            return Collections.emptySet();
        }
        if (args.length == 1) {
            return Collections.singleton(args[0]);
        }
        Set<T> set = new LinkedHashSet<>();
        for (T t : args) {
            set.add(t);
        }
        return set;
    }

    /**
     * Create a map that, when a call to get() would return null, uses a
     * supplier to create a new value, adds it and returns that.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param valueSupplier The supplier of values
     * @return a map
     */
    public static <T, R> Map<T, R> supplierMap(Supplier<R> valueSupplier) {
        return new SupplierMap<>(valueSupplier);
    }

    /**
     * Create a map that, when a call to get() would return null, uses a
     * supplier to create a new value, adds it and returns that.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param valueSupplier The supplier of values
     * @return a map
     */
    public static <T, R> Map<T, R> concurrentSupplierMap(Supplier<R> valueSupplier) {
        return new SupplierMap<>(valueSupplier, new ConcurrentHashMap<>());
    }

    /**
     * Filter a map by value, returning a new writable map. Will return a
     * LinkedHashMap if the input argument is one, to maintain sort order.
     *
     * @param <T> Key type
     * @param <R> Value type
     * @param map Original map
     * @param test The test to perform on values
     * @return A new map
     */
    public static <T, R> Map<T, R> filterByKey(Map<T, R> map, Predicate<T> test) {
        Map<T, R> result = map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>() : new HashMap<>();
        for (Map.Entry<T, R> e : map.entrySet()) {
            if (test.test(e.getKey())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Filter a map by value, returning a new writable map. Will return a
     * LinkedHashMap if the input argument is one, to maintain sort order.
     *
     * @param <T> Key type
     * @param <R> Value type
     * @param map Original map
     * @param test The test to perform on values
     * @return A new map
     */
    public static <T, R> Map<T, R> filterByValue(Map<T, R> map, Predicate<R> test) {
        Map<T, R> result = map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>() : new HashMap<>();
        for (Map.Entry<T, R> e : map.entrySet()) {
            if (test.test(e.getValue())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Convert (or cast) the values in a map to some other type.
     *
     * @param <T> The key type
     * @param <R> The incoming value type
     * @param <X> The outgoing value type
     * @param map The original map
     * @param conversion A function to convert the map values - items that
     * return null will not be included in the result
     * @return A new map
     */
    public static <T, R, X> Map<T, X> convertValues(Map<T, R> map, Function<R, X> conversion) {
        Map<T, X> result = map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>() : new HashMap<>();
        for (Map.Entry<T, R> e : map.entrySet()) {
            X val = conversion.apply(e.getValue());
            if (val != null) {
                result.put(e.getKey(), val);
            }
        }
        return result;
    }

    /**
     * Generify a raw list.
     *
     * @param <T> The member type
     * @param l The raw list
     * @param type The type
     * @param filter If filter, simply omit any non-matching elements
     * @return A new list
     */
    public static <T> List<T> checkedListByCopy(List<?> l, Class<T> type, boolean filter) {
        List<T> result = new ArrayList<>(l.size());
        for (Object o : l) {
            if (filter) {
                if (type.isInstance(o)) {
                    result.add(type.cast(o));
                }
            } else {
                result.add(type.cast(o));
            }
        }
        return result;
    }

    /**
     * Convert an array of any type into a list; has the side effect, with
     * primitive arrays of converting them to their boxed type.
     */
    public static List<?> toList(Object array) {
        return new UnknownTypeArrayList(array);
    }

    /**
     * Turn an Iterable into a List.
     */
    public static <T> List<T> iterableToList(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        for (T t : notNull("iterable", iterable)) {
            result.add(t);
        }
        return result;
    }

    /**
     * Turn an Iterable into a List.
     */
    public static <T> Set<T> iterableToSet(Iterable<T> iterable) {
        Set<T> result = new LinkedHashSet<>();
        for (T t : notNull("iterable", iterable)) {
            result.add(t);
        }
        return result;
    }

    public static <T, R> Map<T, R> immutableArrayMap(Map<T, R> map, Class<T> keyType, Class<R> valType, ToLongFunction<T> func) {
        return new ImmutableArrayMap<>(map, keyType, valType, func);
    }

    /**
     * Version of reifiedList() which returns an empty list if null is passed.
     *
     * @param <T>
     * @param list
     * @param type
     * @return
     */
    public static <T> List<T> reifiedListFromPossiblyNulList(List<?> list, Class<? super T> type) {
        if (list == null) {
            return Collections.emptyList();
        }
        return reifiedList(list, type);
    }

    /**
     * Allows converting a List&lt:?&gt; to a List&lt;Type&gt;.  <i>It is
     * possible to generate class cast exceptions here - the list's contents
     * will be type cast using the passed type.</i>. The signature takes a
     * supertype, so that it is possible to pass Foo.class for the non-existent
     * <code>Foo&lt;X&gt;</code> class object.
     * <p>
     * The returned list is a wrapper over the original and will reflect changes
     * in it.
     *
     * @param <T> The type to reify to
     * @param list A list. If null, this method will return null.
     * @param type The type to reify to
     * @return A list parameterized on the type in question.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> reifiedList(List<?> list, Class<? super T> type) {
        if (type == Object.class) {
            throw new IllegalArgumentException("Must be refining type - Object.class cannot be");
        }
        if (list == null) {
            return null;
        }
        return new ConvertList<>((Class<T>) type, Object.class, (List<Object>) list, new ReifyingConverter<>(type));
    }

    static final class ReifyingConverter<T, R extends T> implements Converter<R, Object> {

        private final Class<R> type;

        @SuppressWarnings("unchecked")
        public ReifyingConverter(Class<? super T> type) {
            this.type = (Class<R>) type;
        }

        @Override
        public R convert(Object r) {
            return type.cast(r);
        }

        @Override
        public Object unconvert(R t) {
            return t;
        }
    }

    /**
     * Create a reversed copy of the passed map, which <i>must not</i> contain
     * duplicate values.
     *
     * @param <T> The original key type
     * @param <R> The original key type
     * @param map A map
     * @return A map with the keys and values swapped
     * @throws IllegalArgumentException if the passed map contains duplicate
     * values, which would result in one of the key/value pairs being thrown
     * away.
     */
    public static <T, R> Map<R, T> reverse(Map<T, R> map) {
        Map<R, T> result = map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>()
                : new HashMap<>();
        for (Map.Entry<T, R> e : map.entrySet()) {
            T old = result.put(e.getValue(), e.getKey());
            if (old != null) {
                throw new IllegalArgumentException("Duplicate values " + e.getValue() + " but "
                        + "resulting map cannot contain duplicate keys - would lose data");
            }
        }
        return result;
    }

    /**
     * Create a generic array using Array.newInstance() and returning an array
     * with a generic type, which is illegal in plain java code.
     *
     * @param <T> The type
     * @param type The type
     * @param length The array length
     * @return An array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] genericArray(Class<? super T> type, int length) {
        Checks.nonNegative("length", length);
        Checks.notNull("type", type);
        return (T[]) Array.newInstance(type, length);
    }

    /**
     * Convert a generified collection to an array of the matching type.
     *
     * @param <T>
     * @param coll
     * @param type The type
     * @return An array
     */
    public static <T> T[] toArray(Collection<T> coll, Class<? super T> type) {
        return coll.<T>toArray(genericArray(type, coll.size()));
    }

    public static <T, R> HashingMapBuilder<T, R> hashingMap(Function<Object, byte[]> byteConverter) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1", byteConverter);
    }

    public static <T, R> HashingMapBuilder<T, R> hashingMapWithAlgorithm(String alg, Function<Object, byte[]> byteConverter) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1");
    }

    public static <T, R> HashingMapBuilder<T, R> hashingMapWithAlgorithm(String alg) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1");
    }

    public static <T, R> HashingMapBuilder<T, R> hashingMap() {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1");
    }

    /**
     * Create a map builder which
     *
     * @param <T>
     * @param <R>
     * @param key
     * @return
     */
    public static <T, R> HashingMapBuilder.HashingValueBuilder<T, R> hashingMap(T key) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1").map(key);
    }

    /**
     * Create a map builder.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @return A map builder
     */
    public static <T, R> MapBuilder2<T, R> map() {
        return new MapBuilder2Impl<>();
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
        return new ConvertedMap<>(from, delegate, converter);
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
     * Represent a collection of list iterators as one without copying data.
     *
     * @param <T> The list type
     * @param iterators Some iterators
     * @return A list iterator
     */
    public static <T> ListIterator<T> combineListIterators(List<ListIterator<T>> iterators) {
        return iterators.isEmpty() ? Collections.emptyListIterator() : new MergeListIterator<>(iterators);
    }

    /**
     * Merge together two list iterators without copying data.
     *
     * @param <T> The type
     * @param a One iterator
     * @param b Another iterator
     * @return A single iterator wrapping both
     */
    public static <T> ListIterator<T> combineListIterators(ListIterator<T> a, ListIterator<T> b) {
        return new MergeListIterator<>(Arrays.asList(a, b));
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

    /**
     * Combines two lists into a single list that proxies both, without copying
     * data.
     *
     * @param <T> The list type
     * @param a The first list
     * @param b The second list
     * @return A list that combines both arguments
     */
    public static <T> List<T> combinedList(List<T> a, List<T> b) {
        return combinedList(Arrays.asList(a, b));
    }

    /**
     * Combines multiple lists into a single list that proxies both, without
     * copying data.
     *
     * @param <T> The list type
     * @param lists A list of lists
     * @return A list that combines both arguments
     */
    public static <T> List<T> combinedList(List<List<T>> lists) {
        return notNull("lists", lists).isEmpty() ? Collections.emptyList() : new MultiList<>(lists);
    }

    /**
     * Combines multiple collections into a single list that proxies both,
     * without copying data.
     *
     * @param <T>
     * @param lists
     * @return
     */
    public static <T> List<T> combinedList(Collection<? extends Collection<T>> lists) {
        return notNull("lists", lists).isEmpty() ? Collections.emptyList() : new MultiList<>(lists);
    }


    /**
     * Create an iterator that contains exactly one object.
     *
     * @param <T> The type
     * @param obj The object
     * @return An iterator
     */
    public static <T> Iterator<T> singletonIterator(T obj) {
        return new SingletonIterator<>(obj);
    }

    /**
     * Create an iterator from an array of type T.
     *
     * @param <T> The type
     * @param array An array
     * @return An iterator
     */
    public static <T> Iterator<T> toIterator(T[] array) {
        Checks.notNull("array", array);
        return new ArrayIterator<T>(array);
    }

    /**
     * Create an iterable from an array of type T.
     *
     * @param <T> The type
     * @param array An array
     * @return An iterator
     */
    public static <T> Iterable<T> toIterable(T[] array) {
        Checks.notNull("array", array);
        return toIterable(toIterator(array));
    }

    /**
     * Create an Enumeration from an iterable
     *
     * @param <T> The type
     * @param iter The iterable
     * @return An enumeration
     */
    public static <T> Enumeration<T> toEnumeration(Iterable<T> iter) {
        Checks.notNull("iter", iter);
        return toEnumeration(iter.iterator());
    }

    /**
     * Create an Enumeration from an iterator
     *
     * @param <T> The type
     * @param iter The iterator
     * @return An enumeration
     */
    public static <T> Enumeration<T> toEnumeration(Iterator<T> iter) {
        Checks.notNull("iter", iter);
        return new EnumerationAdapter<>(iter);
    }

    /**
     * Create a reversed iterator over a reversed array
     *
     * @param <T> The type
     * @param array The array
     * @return The iterator
     */
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
        return new AtomicIteratorImpl<>(iter);
    }

    /**
     * Compute the intersection of a set and a collection.
     *
     * @param <T> The type
     * @param a The first set
     * @param b The second collection
     * @return A set containing the intersection
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> intersection(Collection<T> a, Collection<T> b) {
        if (a instanceof IntSet && b instanceof IntSet) {
            return (Set<T>) ((IntSet) a).intersection((IntSet) b);
        }
        Set<T> result = new HashSet<>(a);
        if (a == b) {
            return result;
        }
        result.retainAll(b);
        return result;
    }

    /**
     * Compute the disjunction of two collections, returning the set of elements
     * from <code>b</code> which are not in <code>a</code>.
     *
     * @param <T> The type
     * @param a A set
     * @param b A set
     * @return The elements of b which are not in a
     */
    public static <T> Set<T> disjunction(Collection<T> a, Collection<T> b) {
        if (a == b) {
            return Collections.emptySet();
        }
        Set<T> bs = new HashSet<>(b);
        bs.removeAll(a);
        return bs;
    }

    /**
     * Determine if a set and a collection intersect
     *
     * @param <T> The type
     * @param a The first set
     * @param b The second collection
     * @return The intersection
     */
    public static <T> boolean intersects(Collection<T> a, Collection<T> b) {
        return !intersection(a, b).isEmpty();
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

        @Override
        public String toString() {
            return "Iterable wrapper for " + iter;
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
        int ix = 0;

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
