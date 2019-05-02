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

import com.mastfrog.abstractions.list.LongResolvable;
import com.mastfrog.util.preconditions.Checks;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.strings.Strings;
import com.mastfrog.util.collections.MapBuilder2.HashingMapBuilder;
//import com.mastfrog.util.tree.BitSetSet;
//import com.mastfrog.util.tree.Indexed;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.emptySet;
import java.util.Comparator;
import java.util.EnumSet;
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
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles a few collections omissions
 *
 * @author Tim Boudreau
 */
public final class CollectionUtils {

    private CollectionUtils() {
        throw new AssertionError();
    }

    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori.
     *
     * @param <T>
     * @param allPossibleValues
     * @return
     */
//    public static <T extends Comparable<T>> Set<T> bitSetSet(T... allPossibleValues) {
//        BitSet set = new BitSet(allPossibleValues.length);
//        Indexed<T> indexed = new ArrayIndexedImpl<>(allPossibleValues);
//        return new BitSetSet<T>(indexed, set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     * <p>
     * Note: If you have a type that implements Comparable, or a Comparator, the
     * versions that take arrays or a comparator are faster, as they can use
     * binary search rather than iterating the entire set of items.
     * </p>
     *
     * @param <T>
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T> Set<T> bitSetSet(List<T> allPossibleValues) {
//        checkDuplicates(allPossibleValues);
//        BitSet set = new BitSet(allPossibleValues.size());
//        return new BitSetSet<T>(Indexed.forList(allPossibleValues), set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     *
     * @param <T> The type
     * @param comparator A comparator allowing binary search to be used for
     * looking up items
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T> Set<T> bitSetSet(Comparator<T> comparator, T... allPossibleValues) {
//        BitSet set = new BitSet(allPossibleValues.length);
//        return new BitSetSet<>(new ComparatorArrayIndexedImpl<>(comparator, allPossibleValues), set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     *
     * @param set A bitset. This bitset MUST NOT contain any set bits higher
     * than the length of the values array.
     * @param <T> The type
     * @param comparator A comparator allowing binary search to be used for
     * looking up items
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T> Set<T> bitSetSet(BitSet set, Comparator<T> comparator, T... allPossibleValues) {
//        Checks.notNull("set", set);
//        return new BitSetSet<>(new ComparatorArrayIndexedImpl<>(comparator, allPossibleValues), set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     *
     * @param set A bitset. This bitset MUST NOT contain any set bits higher
     * than the length of the values array.
     * @param <T> The type
     * @param comparator A comparator allowing binary search to be used for
     * looking up items
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T> Set<T> bitSetSet(BitSet set, Comparator<T> comparator, List<T> allPossibleValues) {
//        return new BitSetSet<>(new ComparatorListIndexedImpl<>(comparator, allPossibleValues), set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     *
     * @param <T> The type
     * @param comparator A comparator allowing binary search to be used for
     * looking up items
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T> Set<T> bitSetSet(Comparator<T> comparator, List<T> allPossibleValues) {
//        BitSet set = new BitSet(allPossibleValues.size());
//        return new BitSetSet<>(new ComparatorListIndexedImpl<>(comparator, allPossibleValues), set);
//    }
    /**
     * Create a BitSet-backed set for sets where all possible members are known
     * a-priori. The result is both fast and has a very small memory footprint.
     * Prefer this variant if the collection type implements Comparable, for
     * performance.
     *
     * @param <T> The type
     * @param allPossibleValues All possible values the set can contain. Must
     * not contain duplicates or nulls.
     * @return A set
     */
//    public static <T extends Comparable<T>> Set<T> bitSetSetForComparable(List<T> allPossibleValues) {
//        checkDuplicates(allPossibleValues);
//        BitSet set = new BitSet(allPossibleValues.size());
//        return new BitSetSet<>(ComparatorListIndexedImpl.create(allPossibleValues), set);
//    }

    public static <T> Set<T> weakSet() {
        return new SimpleWeakSet<>();
    }

    /**
     * Create a single Iterable which concatenates multiple iterables without
     * copying them into another collection, so iteration happens only once.
     *
     * @param <T> The iterable type
     * @param iterables A collection of iterables
     * @return An iterable
     */
    public static <T> ConcatenatedIterables<T> concatenate(Iterable<Iterable<T>> iterables) {
        return new MergeIterables<>(iterables);
    }

    /**
     * Create a single Iterable which concatenates multiple iterables without
     * copying them into another collection, so iteration happens only once.
     *
     * @param <T> The iterable type
     * @param iterables A collection of iterables
     * @return An iterable
     */
    public static <T> ConcatenatedIterables<T> concatenate(Iterable<T> a, Iterable<T> b) {
        return new MergeIterables<>(a, b);
    }

    /**
     * Create a single Iterable which concatenates multiple iterables without
     * copying them into another collection, so iteration happens only once.
     *
     * @param <T> The iterable type
     * @param iterables A collection of iterables
     * @return An iterable
     */
    public static <T> ConcatenatedIterables<T> concatenate(Iterable<T> a, Iterable<T> b, Iterable<T> c) {
        return new MergeIterables<>(a, b, c);
    }

    /**
     * Create a single Iterable which concatenates multiple iterables without
     * copying them into another collection, so iteration happens only once.
     *
     * @param <T> The iterable type
     * @param iterables A collection of iterables
     * @return An iterable
     */
    @SafeVarargs
    public static <T> ConcatenatedIterables<T> concatenate(Iterable<T>... iterables) {
        return new MergeIterables<>(iterables);
    }

    /**
     * Filter elements out of a list into a new list.
     *
     * @param <T> The type
     * @param list A list
     * @param filter The filter
     * @return A new list
     */
    public static <T> List<T> filter(List<? extends T> list, Predicate<? super T> filter) {
        List<T> result = new ArrayList<>(list.size());
        list.stream().filter((obj) -> (filter.test(obj))).forEachOrdered(result::add);
        return result;
    }

    @SuppressWarnings("AssertWithSideEffects")
    static void checkDuplicates(Collection<?> collection) {
        boolean asserts = false;
        assert asserts = true;
        if (asserts) {
            Set<Object> objs = new HashSet<>(collection);
            assert objs.size() == collection.size() :
                    "Collection may not contain duplicates: " + collection;
        }
    }

    /**
     * Create a multi-array-backed list of longs.
     *
     * @param batchSize The size of the backing arrays
     * @return A longList
     */
    public static LongList longList(int batchSize) {
        return new LongListImpl(batchSize);
    }

    /**
     * Create a multi-array-backed list of longs.
     *
     * @param longs The initial contents
     * @return A LongList
     */
    public static LongList longList(long... longs) {
        return new LongListImpl(longs);
    }

    /**
     * Create a multi-array-backed list of longs.
     *
     * @param batchSize The size of the backing arrays
     * @param longs The initial contents
     * @return A LongLIst
     */
    public static LongList longList(int batchSize, long[] longs) {
        return new LongListImpl(longs, batchSize);
    }

    /**
     * Create a multi-array-backed list of longs.
     *
     * @param batchSize The size of the backing arrays
     * @param longs The initial contents
     * @return A LongLIst
     */
    public static LongList longList(int batchSize, Collection<? extends Long> c) {
        return new LongListImpl(ArrayUtils.toLongArray(c), batchSize);
    }

    /**
     * Create a multi-array-backed list of longs.
     *
     * @return A longList
     */
    public static LongList longList() {
        return new LongListImpl();
    }

    /**
     * Create an array-backed (fast iteration and lookup, slow insert and
     * remove) mutable set which uses binary search and a comparator for
     * equality (allows for such things as case-insensitive string sets).
     * <p>
     * Note: The element arguments must be of exactly the same type.
     *
     * @param <T> The type
     * @param first The first element
     * @param more Additional elements
     * @return A set
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T extends Comparable<T>> Set<T> mutableArraySet(T first, T... more) {
        for (T m : more) {
            if (first.getClass() != m.getClass()) {
                throw new IllegalArgumentException("All elements must be of the same exact type "
                        + "to create without passing a class object, but saw both " + first.getClass().getName()
                        + " and " + m.getClass().getName());
            }
        }
        Class<T> type = (Class<T>) first.getClass();
        T[] arr = (T[]) Array.newInstance(type, more.length + 1);
        arr[0] = first;
        System.arraycopy(more, 0, arr, 1, more.length);
        return new ArrayBinarySetMutable<>(true, true, new ComparableComparator<>(), arr);
    }

    /**
     * Create an array-backed mutable set which uses the comparable contract of
     * the passed type for equality comparisons.
     *
     * @param <T> The type
     * @param type The type
     * @return A set
     */
    static <T extends Comparable<T>> Set<T> mutableArraySet(Class<T> type) {
        return mutableArraySet(type, 16);
    }

    static <T extends Comparable<T>> Set<T> mutableArraySet(Class<T> type, int initialCapacity) {
        ComparableComparator<T> cc = new ComparableComparator<>();
        return new ArrayBinarySetMutable<>(true, cc, initialCapacity, type);
    }

    static <T> Set<T> mutableArraySet(Class<T> type, Comparator<T> comp) {
        return mutableArraySet(type, comp, 16);
    }

    static <T> Set<T> mutableArraySet(Class<T> type, Comparator<T> comp, int initialCapacity) {
        return new ArrayBinarySetMutable<>(true, comp, initialCapacity, type);
    }

    /**
     * Wraps an iterator in one which does not permit mutation.
     *
     * @param <T> The type
     * @param iter An iterator
     * @return A wrapper iterator
     */
    public static <T> Iterator<T> unmodifiableIterator(Iterator<T> iter) {
        return iter instanceof UnmodifiableIterator<?> ? iter : new UnmodifiableIterator<>(iter);
    }

    /**
     * Return a generified view of a map, filtering out any key value pairs *
     * that do not match the passed types.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param map A map
     * @param t The key type
     * @param r The value type
     * @return A typed map
     */
    public static <T, R> Map<T, R> checkedMapByFilter(Map<?, ?> map, Class<T> t, Class<R> r) {
        notNull("map", map);
        Map<T, R> result = map instanceof SortedMap<?, ?> ? new TreeMap<>()
                : map instanceof LinkedHashMap<?, ?> ? new LinkedHashMap<>()
                        : new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (t.isInstance(e.getKey()) && r.isInstance(e.getValue())) {
                result.put(t.cast(e.getKey()), r.cast(e.getValue()));
            }
        }
        return result;
    }

    /**
     * Simply dangerously casts the map to the requested types.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param map The map
     * @return The same map passed in
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Map<T, R> uncheckedMap(Map<?, ?> map) {
        return (Map<T, R>) map;
    }

    /**
     * Return a generified view of a map.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param map The map
     * @param t The key type
     * @param r The value type
     * @return The same map passed in, safely typed
     * @throws ClassCastException if a type is wrong
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Map<T, R> checkedMap(Map<?, ?> map, Class<T> t, Class<R> r) {
        notNull("map", map);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (t != Object.class && e.getKey() != null) {
                if (!t.isInstance(e.getKey())) {
                    throw new ClassCastException("Key " + e.getKey() + " is not an instance of "
                            + t.getName() + " (value " + e.getValue() + ")");
                }
            }
            if (r != Object.class && e.getValue() != null) {
                if (!r.isInstance(e.getValue())) {
                    throw new ClassCastException("Value for key " + e.getKey() + " is not an instance of "
                            + r.getName() + " (value " + e.getValue() + ")");
                }
            }
        }
        return (Map<T, R>) map;
    }

    /**
     * Convert a set of objects to a list of some other type of object, using
     * the passed function as a converter.
     *
     * @param <T> The original type
     * @param <R> The output type
     * @param xform Converts T's into R's
     * @param list Some objects
     * @return a set
     */
    public static <T, R> Set<R> transform(Set<T> list, Function<T, R> xform) {
        Set<R> result = new LinkedHashSet<>(list.size());
        list.stream().forEach(t -> {
            R r = xform.apply(t);
            if (r != null) {
                result.add(r);
            }
        });
        return result;
    }

    /**
     * Convert a list of objects to a list of some other type of object, using
     * the passed function as a converter.
     *
     * @param <T> The original type
     * @param <R> The output type
     * @param xform Converts T's into R's
     * @param list Some objects
     * @return a list
     */
    public static <T, R> List<R> transform(List<T> list, Function<T, R> xform) {
        List<R> result = new ArrayList<>(list.size());
        list.stream().forEach(t -> {
            R r = xform.apply(t);
            if (r != null) {
                result.add(r);
            }
        });
        return result;
    }

    /**
     * Convert an array of objects to a list of some other type of object, using
     * the passed function as a converter.
     *
     * @param <T> The original type
     * @param <R> The output type
     * @param xform Converts T's into R's
     * @param args Some objects
     * @return a list
     */
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

    /**
     * Create a map from the values in the passed collection, using the passed
     * function to generate keys.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param coll Some values
     * @param func A key conversion function
     * @return a map
     */
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
    @SuppressWarnings("unchecked")
    public static <T> Set<T> setOf(T a, T b) {
        if (Objects.equals(a, b)) {
            return Collections.singleton(a);
        }
        return new ArraySet<>(false, a, b);
    }

    /**
     * Create a mutable set from some arguments.
     *
     * @param <T> The type
     * @return A set
     */
    @SafeVarargs
    public static <T> Set<T> mutableSetOf(T... args) {
        Set<T> result = new LinkedHashSet<>();
        for (T t : args) {
            result.add(t);
        }
        return result;
    }

    /**
     * Create a set from some arguments. The type of the returned set may vary
     * depending on the arguments, and will usually be immutable.
     *
     * @param <T> The type
     * @return A set
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> setOf(T a, T b, T c) {
        if (a instanceof String && b instanceof String && c instanceof String) {
            return (Set<T>) arraySetOf((String) a, (String) b, (String) c);
        } else if (a instanceof CharSequence && b instanceof CharSequence && c instanceof CharSequence) {
            return (Set<T>) charSequenceSetOf((CharSequence) a, (CharSequence) b, (CharSequence) c);
        } else if (a instanceof Enum<?> && b instanceof Enum<?> && c instanceof Enum<?>
                && a.getClass() == b.getClass() && a.getClass() == c.getClass()) {
            EnumSet es = EnumSet.noneOf((Class<Enum>) a.getClass());
            es.add(a);
            es.add(b);
            es.add(c);
            return es;
        }
        boolean abEqual = Objects.equals(a, b);
        boolean acEqual = Objects.equals(a, c);
        if (abEqual && acEqual) {
            return Collections.singleton(a);
        } else if (abEqual && !acEqual) {
            return new ArraySet<>(false, a, c);
        } else if (acEqual && !abEqual) {
            return new ArraySet<>(false, a, b);
        } else {
            return new ArraySet<>(false, a, b, c);
        }
    }

    static final class IdentityComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            int a = System.identityHashCode(o1);
            int b = System.identityHashCode(o2);
            return a == b ? 0 : a > b ? 1 : -1;
        }
    }

    /**
     * Create an array-backed set which uses System.identityHashCode() for
     * membership/equality tests.
     *
     * @param <T> The type
     * @param objs Some objects
     * @return A set
     */
    @SafeVarargs
    public static <T> Set<T> identitySet(T... objs) {
        return new ArrayBinarySet<>(false, true, new IdentityComparator(), objs);
    }

    /**
     * Create an array-backed immutable set which uses binary search for
     * lookups, and may use the passed comparator for membership tests (allowing
     * for creation of sets with alternate membership requirements, such as
     * case-insensitive string sets). The returned set is faster than a hash set
     * for iteration, but may be slower than a hash set for membership tests -
     * the performance difference depends on how much work the comparator does.
     *
     * @param <T>
     * @param comp
     * @param useComparatorForMembershipTest
     * @param objs
     * @return
     */
    @SafeVarargs
    public static <T> Set<T> arraySetOf(Comparator<T> comp, boolean useComparatorForMembershipTest, T... objs) {
        if (objs.length == 0) {
            return emptySet();
        }
        return new ArrayBinarySet<>(true, useComparatorForMembershipTest, comp, objs);
    }

    /**
     * Create an array-backed set of objects which uses the passed comparator
     * for equality/membership tests.
     *
     * @param <T> The type
     * @param comp A comparator
     * @param objs Some objects
     * @return A set
     */
    @SafeVarargs
    public static <T> Set<T> arraySetOf(Comparator<T> comp, T... objs) {
        return arraySetOf(comp, false, objs);
    }

    /**
     * Create an array-backed, sorted set of some objects.
     *
     * @param <T> The type
     * @param objs The objects
     * @return A set
     */
    @SafeVarargs
    public static <T extends Comparable<T>> Set<T> arraySetOf(T... objs) {
        if (objs.length == 0) {
            return emptySet();
        }
        return new ArrayBinarySet<>(true, false, new ComparableComparator<>(), objs);
    }

    /**
     * Create an immutable set of CharSequences which uses minimal memory.
     *
     * @param str An array of CharSequences
     * @return A set
     */
    public static Set<CharSequence> charSequenceSetOf(CharSequence... objs) {
        if (objs.length == 0) {
            return emptySet();
        }
        return new ArrayBinarySet<>(true, false, Strings.charSequenceComparator(false), objs);
    }

    /**
     * Create an immutable set of Strings which is case-insensitive for
     * membership tests.
     *
     * @param str An array of Strings
     * @return A set
     */
    public static Set<String> caseInsensitiveStringSet(String... str) {
        if (str.length == 0) {
            return emptySet();
        }
        return new ArrayBinarySet<>(true, true, new StringComparator(), str);
    }

    /**
     * Create an immutable set of CharSequences which is case-insensitive for
     * membership tests.
     *
     * @param str An array of CharSequences
     * @return A set
     */
    public static Set<CharSequence> caseInsensitiveCharSequenceSet(CharSequence... str) {
        if (str.length == 0) {
            return emptySet();
        }
        return new ArrayBinarySet<>(true, true, Strings.charSequenceComparator(true), str);
    }

    static final class StringComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }

    }

    static final class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            return o1.compareTo(o2);
        }

    }

    /**
     * Create a set from some arguments.
     *
     * @param <T> The type
     * @param args The arguments
     * @return A set
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Set<T> setOf(T... args) {
        if (args.length == 0) {
            return Collections.emptySet();
        }
        if (args.length == 1) {
            return Collections.singleton(args[0]);
        }
        if (args.length == 2) {
            return setOf(args[0], args[1]);
        }
        if (args.length == 3) {
            return setOf(args[0], args[1], args[2]);
        }
        if (Enum.class.isAssignableFrom(args.getClass().getComponentType())) {
            Set result = EnumSet.noneOf((Class<? extends Enum>) args.getClass().getComponentType());
            for (T t : args) {
                result.add(t);
            }
            return result;
        }
        if (Comparable.class.isAssignableFrom(args.getClass().getComponentType())) {
            if (args.length < 30) {
                return new ArrayBinarySet(true, false, new ComparableComparator(), args);
            }
        }
        if (args.length < 10) {
            return new ArraySet<>(true, args);
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

    /**
     * Create a sorted, immutable map backed by a pair of arrays (can perform
     * better for small, frequently read maps).
     *
     * @param <T> The key type
     * @param <R> The value map
     * @param map The map to copy
     * @param keyType The key type
     * @param valType The value type
     * @param func A function which converts keys to a Long for purposes of
     * sorting and binary search
     * @return A map
     */
    public static <T, R> Map<T, R> immutableArrayMap(Map<T, R> map, Class<T> keyType, Class<R> valType, LongResolvable func) {
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

    /**
     * Create a map builder which can compute a hash.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param byteConverter A function which converts any possible array
     * contents to an array of bytes suitable for hashing (e.g.
     * toString().getBytes(UTF_8)).
     *
     * @return A builder
     */
    public static <T, R> HashingMapBuilder<T, R> hashingMap(Function<Object, byte[]> byteConverter) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1", byteConverter);
    }

    /**
     * Create a map builder which can compute a hash.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param byteConverter A function which converts any possible array
     * contents to an array of bytes suitable for hashing (e.g.
     * toString().getBytes(UTF_8)).
     * @param alg The hashing algorithm for the internal MessageDigest
     *
     * @return A builder
     */
    public static <T, R> HashingMapBuilder<T, R> hashingMapWithAlgorithm(String alg, Function<Object, byte[]> byteConverter) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1");
    }

    /**
     * Create a map builder which can compute a hash and uses toString() as the
     * hash source.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param alg The algorithm to use for the internal MessageDigest
     *
     * @return A builder
     */
    public static <T, R> HashingMapBuilder<T, R> hashingMapWithAlgorithm(String alg) {
        return CollectionUtils.<T, R>map().toHashingMapBuilder("SHA-1");
    }

    /**
     * Create a map builder which can compute a hash and uses toString() as the
     * hash source for a SHA-1 hash.
     *
     * @param <T> The key type
     * @param <R> The value type
     *
     * @return A builder
     */
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
     * The equivalent of SupplierMap for primitive int keyed maps, with a
     * supplier for empty values, and the default capacity of 96.
     *
     * @param <T> The value type
     * @param emptyValues Supplies empty values
     * @return A map
     */
    public static <T> IntMap<T> intMap(Supplier<T> emptyValues) {
        return intMap(96, false, emptyValues);
    }

    /**
     * The equivalent of SupplierMap for primitive int keyed maps.
     *
     * @param <T> The value type
     * @param initialCapacity The initial array size to allocate for keys and
     * values
     * @param emptyValues Supplies empty values
     * @return A map
     */
    public static <T> IntMap<T> intMap(int initialCapacity, Supplier<T> emptyValues) {
        return intMap(initialCapacity, false, emptyValues);
    }

    /**
     * The equivalent of SupplierMap for primitive int keyed maps.
     *
     * @param <T> The value type
     * @param initialCapacity The initial array size to allocate for keys and
     * values
     * @param addSuppliedValues If true, when a non-existent key is requested
     * and the passed supplier is being used to supply a value, store the new
     * value in the map; if false, just return it, not altering the state of the
     * map. Unless the value objects are expensive to create or creating them
     * alters the state of something else, pass false.
     * @param emptyValues Supplies empty values
     * @return A map
     */
    public static <T> IntMap<T> intMap(int initialCapacity, boolean addSuppliedValues, Supplier<T> emptyValues) {
        return new ArrayIntMap<>(initialCapacity, addSuppliedValues, emptyValues);
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
        return new SingleItemList<>();
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
     * @deprecated Use Converter.reverse()
     */
    @Deprecated
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
        return new ArrayIterator<>(array);
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
        if (a == b) {
            return a instanceof Set<?> ? (Set<T>) a : new HashSet<>(a);
        }
        if (a instanceof IntSet && b instanceof IntSet) {
            return (Set<T>) ((IntSet) a).intersection((IntSet) b);
        }
        Set<T> result = new HashSet<>(a);
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
