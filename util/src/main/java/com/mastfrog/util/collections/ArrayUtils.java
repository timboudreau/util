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

import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.nonNegative;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.lang.reflect.Array;
import java.math.BigInteger;
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

    public static byte[][] split(byte[] arr, int splitPoint) {
        greaterThanZero("splitPoint", splitPoint);
        notNull("arr", arr);
        if (splitPoint >= arr.length) {
            throw new IllegalArgumentException("splitPoint - " + splitPoint
                    + " must be < the array's length of " + arr.length);
        }
        byte[] a = new byte[splitPoint];
        byte[] b = new byte[arr.length - splitPoint];
        System.arraycopy(arr, 0, a, 0, splitPoint);
        System.arraycopy(arr, splitPoint, b, 0, b.length);
        return new byte[][]{a, b};
    }

    public static byte[][] split(byte[] arr, int splitPoint1, int splitPoint2) {
        greaterThanZero("splitPoint1", splitPoint1);
        greaterThanZero("splitPoint2", splitPoint2);
        if (splitPoint2 <= splitPoint1) {
            throw new IllegalArgumentException("splitPoint2 must be "
                    + "> splitPoint1: " + splitPoint1 + "," + splitPoint2);
        }
        if (splitPoint1 >= notNull("arr", arr).length) {
            throw new IllegalArgumentException("splitPoint1 - " + splitPoint1
                    + " - is greater than the array size of " + arr.length);
        }
        if (splitPoint2 >= arr.length) {
            throw new IllegalArgumentException("splitPoint2 - " + splitPoint2
                    + " - is greater than the array size of " + arr.length);
        }
        byte[] a = new byte[splitPoint1];
        byte[] b = new byte[splitPoint2 - splitPoint1];
        byte[] c = new byte[arr.length - splitPoint2];
        System.arraycopy(arr, 0, a, 0, splitPoint1);
        System.arraycopy(arr, splitPoint1, b, 0, b.length);
        System.arraycopy(arr, splitPoint2, c, 0, c.length);
        return new byte[][]{a, b, c};
    }

    public static byte[][] split(byte[] arr, int splitPoint1, int splitPoint2, int splitPoint3, int... more) {
        greaterThanZero("splitPoint1", splitPoint1);
        greaterThanZero("splitPoint2", splitPoint2);
        if (splitPoint2 <= splitPoint1) {
            throw new IllegalArgumentException("splitPoint2 must be "
                    + "> splitPoint1: " + splitPoint1 + "," + splitPoint2);
        }
        if (splitPoint1 >= notNull("arr", arr).length) {
            throw new IllegalArgumentException("splitPoint1 - " + splitPoint1
                    + " - is greater than the array size of " + arr.length);
        }
        if (splitPoint2 >= arr.length) {
            throw new IllegalArgumentException("splitPoint2 - " + splitPoint2
                    + " - is greater than the array size of " + arr.length);
        }
        byte[] a = new byte[splitPoint1];
        byte[] b = new byte[splitPoint2 - splitPoint1];
        byte[] c = new byte[splitPoint3 - splitPoint2];
        byte[] d = new byte[(more.length > 0 ? more[0] : arr.length) - splitPoint3];
        System.arraycopy(arr, 0, a, 0, splitPoint1);
        System.arraycopy(arr, splitPoint1, b, 0, b.length);
        System.arraycopy(arr, splitPoint2, c, 0, c.length);
        System.arraycopy(arr, splitPoint3, d, 0, d.length);
        if (more.length == 0) {
            return new byte[][]{a, b, c, d};
        }
        List<byte[]> all = new ArrayList<>(4 + more.length);
        all.addAll(Arrays.asList(a, b, c, d));
        for (int i = 1; i < more.length; i++) {
            byte[] nue = new byte[more[i] - more[i - 1]];
            System.arraycopy(arr, more[i - 1], nue, 0, nue.length);
            all.add(nue);
            if (i == more.length - 1 && arr.length - more[i] > 0) {
                nue = new byte[arr.length - more[i]];
                System.arraycopy(arr, more[i], nue, 0, nue.length);
                all.add(nue);
            }
        }
        return all.toArray(new byte[all.size()][]);
    }

    public static byte[] concatenate(byte[] a, byte[] b, byte[]... cs) {
        int total = a.length + b.length;
        for (byte[] c : cs) {
            total += c.length;
        }
        byte[] result = new byte[total];
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (byte[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] nue = new byte[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    public static int[] concatenate(int[] a, int[] b) {
        int[] nue = new int[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    public static long[] concatenate(long[] a, long[] b) {
        long[] nue = new long[a.length + b.length];
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
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        char[] result = new char[length];
        System.arraycopy(array, start, result, 0, length);
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
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        int[] result = new int[length];
        System.arraycopy(array, start, result, 0, length);
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
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        long[] result = new long[length];
        System.arraycopy(array, start, result, 0, length);
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
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        byte[] result = new byte[length];
        System.arraycopy(array, start, result, 0, length);
        return result;
    }

    /**
     * Extract a subsequence from an array of Strings.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static String[] extract(String[] array, int start, int length) {
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        String[] result = new String[length];
        System.arraycopy(array, start, result, 0, length);
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
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        if (start + length > array.length) {
            throw new IllegalArgumentException("Extract past end of array - start="
                    + start + " + len=" + length + " = " + (start + length)
                    + " in array of length " + array.length);
        }
        T[] result;
        try {
            result = (T[]) Array.newInstance(array.getClass().getComponentType(), length);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Exception creating array of type "
                    + array.getClass().getComponentType() + " for " + array, ex);
        }
        System.arraycopy(array, start, result, 0, length);
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

    public static BigInteger[] toBigIntegers(long[] vals) {
        BigInteger[] result = new BigInteger[vals.length];
        for (int i = 0; i < vals.length; i++) {
            result[i] = BigInteger.valueOf(vals[i]);
        }
        return result;
    }

    public static BigInteger[] toBigIntegers(int[] vals) {
        BigInteger[] result = new BigInteger[vals.length];
        for (int i = 0; i < vals.length; i++) {
            result[i] = BigInteger.valueOf(vals[i]);
        }
        return result;
    }

    /**
     * Constant time equality test, designed not to be faster but to be immune
     * to timing attacks. Slower than Arrays.equals(), but useful in
     * cryptography where the time it takes to compute something can be
     * <a href="http://codahale.com/a-lesson-in-timing-attacks/">
     * used in an attack</a>.
     */
    public static boolean timingSafeEquals(byte[] first, byte[] second) {
        if (first == null) {
            return second == null;
        } else if (second == null) {
            return false;
        } else if (second.length <= 0) {
            return first.length <= 0;
        }
        byte result = (byte) ((first.length == second.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < first.length; ++i) {
            result |= first[i] ^ second[j];
            j = (j + 1) % second.length;
        }
        return result == 0;
    }

}
