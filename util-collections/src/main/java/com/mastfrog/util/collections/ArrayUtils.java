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

import static com.mastfrog.util.collections.CollectionUtils.genericArray;
import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.isInstance;
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
import java.util.function.DoubleUnaryOperator;

/**
 * Utility functions that operate on arrays.
 *
 * @author Tim Boudreau
 */
public final class ArrayUtils {

    private static final Object[] EMPTY = new Object[0];

    private ArrayUtils() {
        throw new AssertionError();
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
     * Concatenate multiple arrays of objects into a single new array. Note: The
     * passed arrays must have the same <i>exact</i> type returned by
     * <code>getClass().getComponentType()</code> or an exception will be
     * thrown.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of bytes comprising both
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concatenate(T[] a, T[] b, T[]... cs) {
        int total = a.length + b.length;
        Class<?> compType = null;
        for (int i = 0; i < cs.length; i++) {
            T[] c = cs[i];
            total += c.length;
            if (compType == null) {
                compType = c.getClass().getComponentType();
            } else {
                Class<?> currType = c.getClass().getComponentType();
                if (compType != currType) {
                    throw new IllegalArgumentException("Arrays passed to "
                            + "ArrayUtils.concatenate() must have the same "
                            + "*exact* component type, but found " + compType
                            + " at 0 and " + currType + " at " + i);
                }
            }
        }
        T[] result = genericArray((Class<? super T>) a.getClass().getComponentType(),
                total);
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (T[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    /**
     * Split a single array of bytes into two arrays.
     *
     * @param arr The array
     * @param splitPoint The first split point
     * @return An array of arrays of bytes
     */
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

    /**
     * Split a single array of bytes into multiple arrays.
     *
     * @param arr The array
     * @param splitPoint1 The first split point
     * @param splitPoint2 The second split point
     * @return An array of arrays of bytes
     */
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

    /**
     * Split a single array of bytes into multiple arrays.
     *
     * @param arr The array
     * @param splitPoint1 The first split point
     * @param splitPoint2 The second split point
     * @param splitPoint3 The third split point
     * @param more More split points
     * @return An array of arrays of bytes
     */
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

    /**
     * Split a single array of bytes into two arrays.
     *
     * @param arr The array
     * @param splitPoint The first split point
     * @return An array of arrays of bytes
     */
    public static double[][] split(double[] arr, int splitPoint) {
        greaterThanZero("splitPoint", splitPoint);
        notNull("arr", arr);
        if (splitPoint >= arr.length) {
            throw new IllegalArgumentException("splitPoint - " + splitPoint
                    + " must be < the array's length of " + arr.length);
        }
        double[] a = new double[splitPoint];
        double[] b = new double[arr.length - splitPoint];
        System.arraycopy(arr, 0, a, 0, splitPoint);
        System.arraycopy(arr, splitPoint, b, 0, b.length);
        return new double[][]{a, b};
    }

    /**
     * Split a single array of bytes into multiple arrays.
     *
     * @param arr The array
     * @param splitPoint1 The first split point
     * @param splitPoint2 The second split point
     * @return An array of arrays of bytes
     */
    public static double[][] split(double[] arr, int splitPoint1, int splitPoint2) {
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
        double[] a = new double[splitPoint1];
        double[] b = new double[splitPoint2 - splitPoint1];
        double[] c = new double[arr.length - splitPoint2];
        System.arraycopy(arr, 0, a, 0, splitPoint1);
        System.arraycopy(arr, splitPoint1, b, 0, b.length);
        System.arraycopy(arr, splitPoint2, c, 0, c.length);
        return new double[][]{a, b, c};
    }

    /**
     * Split a single array of bytes into multiple arrays.
     *
     * @param arr The array
     * @param splitPoint1 The first split point
     * @param splitPoint2 The second split point
     * @param splitPoint3 The third split point
     * @param more More split points
     * @return An array of arrays of bytes
     */
    public static double[][] split(double[] arr, int splitPoint1, int splitPoint2, int splitPoint3, int... more) {
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
        double[] a = new double[splitPoint1];
        double[] b = new double[splitPoint2 - splitPoint1];
        double[] c = new double[splitPoint3 - splitPoint2];
        double[] d = new double[(more.length > 0 ? more[0] : arr.length) - splitPoint3];
        System.arraycopy(arr, 0, a, 0, splitPoint1);
        System.arraycopy(arr, splitPoint1, b, 0, b.length);
        System.arraycopy(arr, splitPoint2, c, 0, c.length);
        System.arraycopy(arr, splitPoint3, d, 0, d.length);
        if (more.length == 0) {
            return new double[][]{a, b, c, d};
        }
        List<double[]> all = new ArrayList<>(4 + more.length);
        all.addAll(Arrays.asList(a, b, c, d));
        for (int i = 1; i < more.length; i++) {
            double[] nue = new double[more[i] - more[i - 1]];
            System.arraycopy(arr, more[i - 1], nue, 0, nue.length);
            all.add(nue);
            if (i == more.length - 1 && arr.length - more[i] > 0) {
                nue = new double[arr.length - more[i]];
                System.arraycopy(arr, more[i], nue, 0, nue.length);
                all.add(nue);
            }
        }
        return all.toArray(new double[all.size()][]);
    }

    /**
     * Concatenate multiple arrays of bytes into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of bytes comprising both
     */
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

    /**
     * Concatenate multiple arrays of longs into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of longs comprising both
     */
    public static long[] concatenate(long[] a, long[] b, long[]... cs) {
        int total = a.length + b.length;
        for (long[] c : cs) {
            total += c.length;
        }
        long[] result = new long[total];
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (long[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    /**
     * Concatenate multiple arrays of ints into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of ints comprising both
     */
    public static int[] concatenate(int[] a, int[] b, int[]... cs) {
        int total = a.length + b.length;
        for (int[] c : cs) {
            total += c.length;
        }
        int[] result = new int[total];
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (int[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    /**
     * Concatenate multiple arrays of ints into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of ints comprising both
     */
    public static float[] concatenate(float[] a, float[] b, float[]... cs) {
        int total = a.length + b.length;
        for (float[] c : cs) {
            total += c.length;
        }
        float[] result = new float[total];
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (float[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    /**
     * Concatenate multiple arrays of ints into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @param cs Additional arrays
     * @return An array of ints comprising both
     */
    public static double[] concatenate(double[] a, double[] b, double[]... cs) {
        int total = a.length + b.length;
        for (double[] c : cs) {
            total += c.length;
        }
        double[] result = new double[total];
        int pos = 0;
        System.arraycopy(a, pos, result, 0, a.length);
        pos += a.length;
        System.arraycopy(b, 0, result, pos, b.length);
        pos += b.length;
        for (double[] c : cs) {
            System.arraycopy(c, 0, result, pos, c.length);
            pos += c.length;
        }
        return result;
    }

    /**
     * Concatenate two arrays of bytes into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @return An array of bytes comprising both
     */
    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] nue = new byte[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    /**
     * Concatenate two arrays of ints into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @return An array of ints comprising both
     */
    public static int[] concatenate(int[] a, int[] b) {
        int[] nue = new int[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    /**
     * Concatenate two arrays of doubles into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @return An array of ints comprising both
     */
    public static double[] concatenate(double[] a, double[] b) {
        double[] nue = new double[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    /**
     * Concatenate two arrays of floats into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @return An array of ints comprising both
     */
    public static float[] concatenate(float[] a, float[] b) {
        float[] nue = new float[a.length + b.length];
        System.arraycopy(a, 0, nue, 0, a.length);
        System.arraycopy(b, 0, nue, a.length, b.length);
        return nue;
    }

    /**
     * Concatenate two arrays of longs into a single new array.
     *
     * @param a The first array
     * @param b The second array
     * @return An array of longs comprising both
     */
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
     * Convert an int array to a long array.
     *
     * @param ints An int array
     * @return A long array
     */
    public static long[] toLongArray(int[] ints) {
        long[] result = new long[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    /**
     * Convert a short array to an int array.
     *
     * @param shorts a short array
     * @return an int array
     */
    public static int[] toIntArray(short[] shorts) {
        int[] result = new int[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            result[i] = shorts[i];
        }
        return result;
    }

    /**
     * Convert a long array to a list of boxed longs.
     *
     * @param longs An array of longs
     * @return A list of longs
     */
    public static List<Long> toBoxedList(long[] longs) {
        List<Long> result = new ArrayList<>(longs.length);
        for (long l : longs) {
            result.add(l);
        }
        return result;
    }

    /**
     * Convert an array of ints to a list of boxed integers.
     *
     * @param ints An array of integers
     * @return A list
     */
    public static List<Integer> toBoxedList(int[] ints) {
        List<Integer> result = new ArrayList<>(ints.length);
        for (int l : ints) {
            result.add(l);
        }
        return result;
    }

    /**
     * Convert an array of primitive longs to an array of boxed longs.
     *
     * @param longs An array of longs
     * @return An array of boxed longs
     */
    public static Long[] toBoxedArray(long[] longs) {
        Long[] result = new Long[longs.length];
        for (int i = 0; i < longs.length; i++) {
            result[i] = longs[i];
        }
        return result;
    }

    /**
     * Convert an array of primitive integers to an array of boxed integers.
     *
     * @param ints An array of integers
     * @return An array of boxed integers
     */
    public static Integer[] toBoxedArray(int[] ints) {
        Integer[] result = new Integer[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    /**
     * Convert an array of boxed longs to an array of primitive longs. Nulls not
     * allowed.
     *
     * @param longs An array of longs
     * @return An array of primitive longs
     */
    public static long[] toPrimitiveArray(Long[] longs) {
        long[] result = new long[longs.length];
        for (int i = 0; i < longs.length; i++) {
            assert longs[i] != null : "Null in Long array at " + i
                    + Arrays.toString(longs);
            result[i] = longs[i];
        }
        return result;
    }

    /**
     * Convert an array of boxed integers to an array of primitive ints. Nulls
     * not allowed.
     *
     * @param longs An array of integers
     * @return An array of primitive integers
     */
    public static int[] toPrimitiveArray(Integer[] ints) {
        int[] result = new int[ints.length];
        for (int i = 0; i < ints.length; i++) {
            assert ints[i] != null : "null in Integer array at " + i
                    + Arrays.toString(ints);
            result[i] = ints[i];
        }
        return result;
    }

    /**
     * Convert an array of boxed shorts to an array of primitive shorts. Nulls
     * not allowed.
     *
     * @param shorts An array of integers
     * @return An array of primitive integers
     */
    public static short[] toPrimitiveArray(Short[] shorts) {
        short[] result = new short[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            assert shorts[i] != null : "null in Integer array at " + i
                    + Arrays.toString(shorts);
            result[i] = shorts[i];
        }
        return result;
    }

    /**
     * Convert an array of boxed shorts to an array of primitive shorts. Nulls
     * not allowed.
     *
     * @param bytes An array of integers
     * @return An array of primitive integers
     */
    public static byte[] toPrimitiveArray(Byte[] bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            assert bytes[i] != null : "null in Integer array at " + i
                    + Arrays.toString(bytes);
            result[i] = bytes[i];
        }
        return result;
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
    public static void shuffle(Random rnd, double[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                double hold = array[i];
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
    public static void shuffle(Random rnd, float[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                float hold = array[i];
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
        return Arrays.copyOfRange(array, start, start + length);
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
        return Arrays.copyOfRange(array, start, start + length);
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
        return Arrays.copyOfRange(array, start, start + length);
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
        return Arrays.copyOfRange(array, start, start + length);
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static double[] extract(double[] array, int start, int length) {
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        return Arrays.copyOfRange(array, start, start + length);
    }

    /**
     * Extract a subsequence from an array.
     *
     * @param array The array
     * @param start The start
     * @param length The length
     * @return an array
     */
    public static float[] extract(float[] array, int start, int length) {
        notNull("array", array);
        nonNegative("length", length);
        if (start == 0) {
            return Arrays.copyOf(array, length);
        }
        return Arrays.copyOfRange(array, start, start + length);
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
        return Arrays.copyOfRange(array, start, start + length);
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
        return Arrays.copyOfRange(array, start, start + length);
    }

    /**
     * Create a reversed copy of an array.
     *
     * @param <T> The array type
     * @param array An array
     * @return A reversed copy
     */
    public static <T> T[] reversed(T[] array) {
        return reverseInPlace(copyOf(array));
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param <T> The array type
     * @param array An array
     * @return A reversed copy
     */
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

    /**
     * Create a reversed copy of an array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static byte[] reversed(byte[] array) {
        return reverseInPlace(copyOf(array));
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param <T> The array type
     * @param array An array
     * @return A reversed copy
     */
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

    /**
     * Create a reversed copy of an array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static int[] reversed(int[] array) {
        return reverseInPlace(copyOf(array));
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param <T> The array type
     * @param array An array
     * @return A reversed copy
     */
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

    /**
     * Create a reversed copy of an array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static char[] reversed(char[] array) {
        return reverseInPlace(copyOf(array));
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param array An array
     * @return A reversed copy
     */
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

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static double[] reverseInPlace(double[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            double hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static float[] reverseInPlace(float[] array) {
        if (array.length < 2) {
            return array;
        }
        for (int i = 0; i < array.length / 2; i++) {
            float hold = array[i];
            array[i] = array[array.length - (i + 1)];
            array[array.length - (i + 1)] = hold;
        }
        return array;
    }

    /**
     * Create a reversed copy of an array.
     *
     * @param array An array
     * @return A reversed copy
     */
    public static long[] reversed(long[] array) {
        return reverseInPlace(copyOf(array));
    }

    /**
     * Reverse an array in-place, modifying the passed array.
     *
     * @param array An array
     * @return A reversed copy
     */
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

    /**
     * Create a duplicate of an array.
     *
     * @param <T> The type
     * @param array The array
     * @return A copy of the array
     */
    public static <T> T[] copyOf(T[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static int[] copyOf(int[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static double[] copyOf(double[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static float[] copyOf(float[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static long[] copyOf(long[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static char[] copyOf(char[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Create a duplicate of an array.
     *
     * @param array The array
     * @return A copy of the array
     */
    public static byte[] copyOf(byte[] array) {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * Convert an integer array to a set.
     *
     * @param ints An array of primitive ints
     * @return A set
     */
    @SuppressWarnings("unchecked")
    public static Set<Integer> toSet(int[] ints) {
        return new HashSet<>((Collection<? extends Integer>) CollectionUtils.<Integer>toList(ints));
    }

    /**
     * Convert an long array to a set.
     *
     * @param ints An array of primitive longs
     * @return A set
     */
    @SuppressWarnings("unchecked")
    public static Set<Long> toSet(long[] ints) {
        return new HashSet<>((Collection<? extends Long>) CollectionUtils.<Long>toList(ints));
    }

    /**
     * Create and fill an array.
     *
     * @param obj The object to fill with
     * @param length The length of the array
     * @return A set
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayOf(T obj, int length) {
        T[] result = (T[]) Array.newInstance(notNull("obj", obj).getClass(), length);
        Arrays.fill(result, obj);
        return result;
    }

    /**
     * Convert an array of longs to BigIntegers.
     *
     * @param vals longs
     * @return An array of BigIntegers
     */
    public static BigInteger[] toBigIntegers(long[] vals) {
        BigInteger[] result = new BigInteger[vals.length];
        for (int i = 0; i < vals.length; i++) {
            result[i] = BigInteger.valueOf(vals[i]);
        }
        return result;
    }

    /**
     * Convert an array of longs to BigIntegers.
     *
     * @param vals longs
     * @return An array of BigIntegers
     */
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
     *
     * @param first The first byte array
     * @param the second byte array
     * @return if they are equal
     */
    public static boolean timingSafeEquals(byte[] first, byte[] second) {
        if (first == null) {
            return second == null;
        } else if (second == null) {
            return false;
        } else if (second.length == 0) {
            return first.length == 0;
        }
        byte result = (byte) ((first.length == second.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < first.length; ++i) {
            result |= first[i] ^ second[j];
            j = (j + 1) % second.length;
        }
        return result == 0;
    }

    /**
     * Tests equality of two array ranges. If the two ranges are not same-sized,
     * returns false.
     *
     * @param a The first array
     * @param aFromIndex The starting index in the first array
     * @param aToIndex The ending index in the first array, exclusive
     * @param b The second array
     * @param bFromIndex The starting index in the second array
     * @param bToIndex The ending index in the first array, exclusive
     * @return true if the ranges are equal
     */
    public static boolean arraysEquals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        return Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static String[] prepend(String first, String... more) {
        String[] result = new String[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static int[] prepend(int first, int... more) {
        int[] result = new int[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static char[] prepend(char first, char... more) {
        char[] result = new char[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T[] prepend(T first, T... more) {
        notNull("more", more);
        isInstance("first", more.getClass().getComponentType(), first);
        if (more.length == 0) {
            T[] result = CollectionUtils.genericArray((Class<? super T>) more.getClass().getComponentType(), 1);
            result[0] = first;
            return result;
        }
        T[] result = CollectionUtils.genericArray((Class<? super T>) more.getClass().getComponentType(), more.length + 1);
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static long[] prepend(long first, long... more) {
        long[] result = new long[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static short[] prepend(short first, short... more) {
        short[] result = new short[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static byte[] prepend(byte first, byte... more) {
        byte[] result = new byte[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static double[] prepend(double first, double... more) {
        double[] result = new double[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value at position 0 and the remainder
     * after.
     *
     * @param first The element to prepend
     * @param more The rest of the resulting array
     * @return An array
     */
    public static float[] prepend(float first, float... more) {
        float[] result = new float[more.length + 1];
        System.arraycopy(more, 0, result, 1, more.length);
        result[0] = first;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static float[] append(float last, float... to) {
        float[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static double[] append(double last, double... to) {
        double[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static int[] append(int last, int... to) {
        int[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static long[] append(long last, long... to) {
        long[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static short[] append(short last, short... to) {
        short[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    public static byte[] append(byte last, byte... to) {
        byte[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Create a new array with the passed value appended to the passed array.
     *
     * @param last The element to append
     * @param to The preceding array contents
     * @return An array
     */
    @SafeVarargs
    public static <T> T[] append(T last, T... to) {
        T[] result = Arrays.copyOf(to, to.length + 1);
        result[result.length - 1] = last;
        return result;
    }

    /**
     * Apply an operation to the contents of an array.
     *
     * @param dbls An array
     * @param op An operation
     */
    public static void apply(double[] dbls, DoubleUnaryOperator op) {
        for (int i = 0; i < dbls.length; i++) {
            dbls[i] = op.applyAsDouble(dbls[i]);
        }
    }

    /**
     * Apply an operation to the contents of an array.
     *
     * @param floats An array
     * @param op An operation
     */
    public static void apply(float[] floats, DoubleUnaryOperator op) {
        for (int i = 0; i < floats.length; i++) {
            floats[i] = (float) op.applyAsDouble(floats[i]);
        }
    }

    /**
     * Apply an operation to a copy the contents of an array and return it.
     *
     * @param dbls An array
     * @param op An operation
     * @return A new array with the operation applied
     */
    public static float[] copyAndApply(float[] floats, DoubleUnaryOperator op) {
        floats = copyOf(floats);
        apply(floats, op);
        return floats;
    }

    /**
     * Apply an operation to a copy the contents of an array and return it.
     *
     * @param dbls An array
     * @param op An operation
     * @return A new array with the operation applied
     */
    public static double[] copyAndApply(double[] dbls, DoubleUnaryOperator op) {
        dbls = copyOf(dbls);
        apply(dbls, op);
        return dbls;
    }

    /**
     * Convert an array of floats to ints.
     *
     * @param arr an array
     * @return A new array
     */
    public static double[] toDoubleArray(float[] arr) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        return result;
    }

    /**
     * Convert an array of ints to doubles.
     *
     * @param arr an array
     * @return A new array
     */
    public static double[] toDoubleArray(int[] ints) {
        double[] result = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    /**
     * Convert an array of ints to floats.
     *
     * @param arr an array
     * @return A new array
     */
    public static float[] toFloatArray(int[] ints) {
        float[] result = new float[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    /**
     * Convert an array of doubles to floats, applying the passed operator in
     * case of overflow.
     *
     * @param arr an array
     * @return A new array
     */
    public static float[] toFloatArray(double[] dbls, DoubleUnaryOperator outOfRange) {
        float[] result = new float[dbls.length];
        for (int i = 0; i < dbls.length; i++) {
            double val = dbls[i];
            if (val < Float.MIN_VALUE || val > Float.MAX_VALUE) {
                val = outOfRange.applyAsDouble(val);
            }
            result[i] = (float) val;
        }
        return result;
    }

    /**
     * Convert a primitive array to its boxed equivalent.
     *
     * @param input An array
     * @return An array of the boxed equivalent type
     */
    public static Float[] toBoxedArray(float[] input) {
        Float[] result = new Float[notNull("floats", input).length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * Convert a primitive array to its boxed equivalent.
     *
     * @param input An array
     * @return An array of the boxed equivalent type
     */
    public static Double[] toBoxedArray(double[] input) {
        Double[] result = new Double[notNull("doubles", input).length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * Convert a primitive array to its boxed equivalent.
     *
     * @param input An array
     * @return An array of the boxed equivalent type
     */
    public static Short[] toBoxedArray(short[] input) {
        Short[] result = new Short[notNull("shorts", input).length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * Convert a primitive array to its boxed equivalent.
     *
     * @param input An array
     * @return An array of the boxed equivalent type
     */
    public static Byte[] toBoxedArray(byte[] input) {
        Byte[] result = new Byte[notNull("bytes", input).length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * Convert a primitive array to its boxed equivalent.
     *
     * @param input An array
     * @return An array of the boxed equivalent type
     */
    public static Character[] toBoxedArray(char[] input) {
        Character[] result = new Character[notNull("chars", input).length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * Convert a collection of integers to a primitive int[].
     *
     * @param input The input
     * @return An int[]
     */
    public static int[] toPrimitiveIntArray(Collection<? extends Integer> input) {
        int[] result = new int[input.size()];
        Iterator<? extends Integer> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of longs to a primitive long[].
     *
     * @param input The input
     * @return A long[]
     */
    public static long[] toPrimitiveLongArray(Collection<? extends Long> input) {
        long[] result = new long[input.size()];
        Iterator<? extends Long> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of shorts to a primitive short[].
     *
     * @param input The input
     * @return A short[]
     */
    public static short[] toPrimitiveShortArray(Collection<? extends Short> input) {
        short[] result = new short[input.size()];
        Iterator<? extends Short> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of bytes to a primitive byte[].
     *
     * @param input The input
     * @return A byte[]
     */
    public static byte[] toPrimitiveByteArray(Collection<? extends Byte> input) {
        byte[] result = new byte[input.size()];
        Iterator<? extends Byte> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of Characters to a primitive char[].
     *
     * @param input The input
     * @return A char[]
     */
    public static char[] toPrimitiveCharArray(Collection<? extends Character> input) {
        char[] result = new char[input.size()];
        Iterator<? extends Character> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of floats to a primitive float[].
     *
     * @param input The input
     * @return A float[]
     */
    public static float[] toPrimitiveFloatArray(Collection<? extends Float> input) {
        float[] result = new float[input.size()];
        Iterator<? extends Float> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }

    /**
     * Convert a collection of doubles to a primitive double[].
     *
     * @param input The input
     * @return A double[]
     */
    public static double[] toPrimitiveDoubleArray(Collection<? extends Double> input) {
        double[] result = new double[input.size()];
        Iterator<? extends Double> it = input.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = it.next();
        }
        return result;
    }
}
