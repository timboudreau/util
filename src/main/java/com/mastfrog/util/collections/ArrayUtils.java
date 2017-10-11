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
package com.mastfrog.util.collections;

import static com.mastfrog.util.Checks.notNull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Utility functions that operate on arrays.
 *
 * @author Tim Boudreau
 */
public class ArrayUtils {

    private static final Object[] EMPTY = new Object[0];

    private ArrayUtils() {
    }

    /**
     * Returns an empty array if the passed array is null.
     *
     * @param array An array or null
     * @return An array
     */
    public static Object[] emptyForNull(Object[] array) {
        return array == null ? EMPTY : array;
    }

    /**
     * Concatenate two arrays.
     *
     * @param <T> The type
     * @param a The first array
     * @param b The second array
     * @return A new array combining both
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatenate(T[] a, T[] b) {
        T[] nue = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    /**
     * Concatenate multiple arrays to the first one.
     *
     * @param a The first array, must be non null
     * @param b Additional arrays, nulls allowed
     * @return A new array concatenating all the passed ones
     */
    @SuppressWarnings("unchecked")
    public static Object[] concatenateAll(Object[] a, Object[]... b) {
        int total = a.length;
        for (int i = 0; i < b.length; i++) {
            if (b[i] != null) {
                total += b[i].length;
            }
        }
        Object[] result = new Object[total];
        System.arraycopy(a, 0, result, 0, a.length);
        int cursor = a.length;
        for (int i = 0; i < b.length; i++) {
            if (b[i] != null) {
                System.arraycopy(b[i], 0, result, cursor, b[i].length);
                cursor += b[i].length;
            }
        }
        return result;
    }

    /**
     * Deduplicate an array of some type, removing objects whose equality
     * matches.
     *
     * @param <T> The type
     * @param a An array
     * @return An array with duplicate items removed
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] dedup(T[] a) {
        Set<T> result = new LinkedHashSet<>(Arrays.asList(a));
        if (result.size() == a.length) {
            return a;
        }
        return result.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), result.size()));
    }

    /**
     * Deduplicate an array <i>by type</i>, meaning that the resulting array
     * will contain only one of any type. Starts from the end, meaning that if
     * two elements of the same type exist in the array, the later occurring one
     * will be retained.
     *
     * @param <T> The type
     * @param a An array
     * @return An array with all but the last of identically-typed items removed
     */
    public static Object[] dedupByType(Object[] a) {
        List<Object> result = new ArrayList<>();
        Set<Class<?>> types = new HashSet<>();
        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] == null) {
                continue;
            }
            if (!types.contains(a[i].getClass())) {
                types.add(a[i].getClass());
                result.add(a[i]);
            }
        }
        Collections.reverse(result);
        return result.toArray();
    }

    /**
     * Flatten an array of objects, extracting any nested object arrays into the
     * resulting array.
     *
     * @param o The array
     * @return An array
     */
    public static Object[] flatten(Object[] o) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < o.length; i++) {
            if (o[i].getClass().isArray()) {
                dumpArray(o[i], result);
            } else {
                result.add(o[i]);
            }
        }
        return result.toArray();
    }

    private static void dumpArray(Object array, List<Object> into) {
        int size = Array.getLength(array);
        for (int j = 0; j < size; j++) {
            Object found = Array.get(array, j);
            if (found != null && found.getClass().isArray() && found != array) {
                dumpArray(found, into);
            } else {
                into.add(found);
            }
        }
    }

    /**
     * Convert a collection of boxed number objects to a primitive int[] array.
     *
     * @param <T> The type
     * @param coll The collection
     * @return An array
     */
    public static <T extends Number> int[] toIntArray(Collection<T> coll) {
        int[] result = new int[coll.size()];
        Iterator<T> it = coll.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next().intValue();
        }
        return result;
    }

    /**
     * Convert a collection of boxed number objects to a primitive long[] array.
     *
     * @param <T> The type
     * @param coll The collection
     * @return An array
     */
    public static <T extends Number> long[] toLongArray(Collection<T> coll) {
        long[] result = new long[coll.size()];
        Iterator<T> it = coll.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next().longValue();
        }
        return result;
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param <T> The type
     * @param rnd A random
     * @param array An array
     */
    public static <T> void shuffle(Random rnd, T[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                T hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param rnd A random
     * @param array An array
     */
    public static void shuffle(Random rnd, long[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                long hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param rnd A random
     * @param array An array
     */
    public static void shuffle(Random rnd, int[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                int hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param rnd A random
     * @param array An array
     */
    public static void shuffle(Random rnd, char[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                char hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param rnd A random
     * @param array An array
     */
    public static void shuffle(Random rnd, byte[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                byte hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static char[] extract(char[] array, int start, int length) {
        char[] result = new char[length];
        System.arraycopy(array, 0, result, start, length);
        return result;
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static int[] extract(int[] array, int start, int length) {
        int[] result = new int[length];
        System.arraycopy(array, 0, result, start, length);
        return result;
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static long[] extract(long[] array, int start, int length) {
        long[] result = new long[length];
        System.arraycopy(array, 0, result, start, length);
        return result;
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static byte[] extract(byte[] array, int start, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, 0, result, start, length);
        return result;
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] extract(T[] array, int start, int length) {
        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType());
        System.arraycopy(array, 0, result, start, length);
        return result;
    }

    public static <T> T[] reversed(T[] array) {
        return reverseInPlace(copyOf(array));
    }

    public static <T> T[] reverseInPlace(T[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            T hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    public static byte[] reversed(byte[] array) {
        return reverseInPlace(copyOf(array));
    }

    public static byte[] reverseInPlace(byte[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            byte hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    public static int[] reversed(int[] array) {
        return reverseInPlace(copyOf(array));
    }

    public static int[] reverseInPlace(int[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            int hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    public static char[] reversed(char[] array) {
        return reverseInPlace(copyOf(array));
    }

    public static char[] reverseInPlace(char[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            char hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    public static long[] reversed(long[] array) {
        return reverseInPlace(copyOf(array));
    }

    public static long[] reverseInPlace(long[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            long hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    public static <T> T[] copyOf(T[] array) {
        return Arrays.copyOf(array, array.length);
    }

    public static int[] copyOf(int[] array) {
        return Arrays.copyOf(array, array.length);
    }

    public static long[] copyOf(long[] array) {
        return Arrays.copyOf(array, array.length);
    }

    public static char[] copyOf(char[] array) {
        return Arrays.copyOf(array, array.length);
    }

    public static byte[] copyOf(byte[] array) {
        return Arrays.copyOf(array, array.length);
    }

    @SuppressWarnings("unchecked")
    public static Set<Integer> toSet(int[] ints) {
        return new HashSet<>((Collection<? extends Integer>) CollectionUtils.<Integer>toList(ints));
    }

    @SuppressWarnings("unchecked")
    public static Set<Long> toSet(long[] ints) {
        return new HashSet<>((Collection<? extends Long>) CollectionUtils.<Long>toList(ints));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayOf(T obj, int length) {
        T[] result = (T[]) Array.newInstance(notNull("obj", obj).getClass(), length);
        Arrays.fill(result, obj);
        return result;
    }
}
