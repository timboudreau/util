package com.mastfrog.util.sort;

import com.mastfrog.util.preconditions.Checks;

/**
 * QuickSort multiple arrays (or anything) according to the sort order of a
 * number array.
 *
 * @author Tim Boudreau
 */
public final class Sort {

    /**
     * Sort the array passed as the first argument, simultaneously reordering
     * the second array argument to the same positions, so items correlated by
     * index remain correlated. Useful for building array-based, map-like data
     * structures which use array indices to map keys to values.
     *
     * @param <T> The type
     * @param keys The array whose values should be sorted
     * @param other The array whose contents should be reordered so that if
     * <code>keys[n] = a</code> and <code>other[n] = b</code>, that will remain
     * true even if sorting changes the value of <code>n</code>.
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     */
    public static <T> void biSort(int[] keys, T[] other, int size) {
        checkSizeInvariants(keys, other, size);
        multiSort(keys, size, Swapper.forArray(other));
    }

    /**
     * Sort the array passed as the first argument, simultaneously reordering
     * the second array argument to the same positions, so items correlated by
     * index remain correlated. Useful for building array-based, map-like data
     * structures which use array indices to map keys to values.
     *
     * @param <T> The type
     * @param keys The array whose values should be sorted
     * @param other The array whose contents should be reordered so that if
     * <code>keys[n] = a</code> and <code>other[n] = b</code>, that will remain
     * true even if sorting changes the value of <code>n</code>.
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     */
    public static <T> void biSort(double[] keys, T[] other, int size) {
        checkSizeInvariants(keys, other, size);
        multiSort(keys, size, Swapper.forArray(other));
    }

    /**
     * Sort the array passed as the first argument, simultaneously reordering
     * the second array argument to the same positions, so items correlated by
     * index remain correlated. Useful for building array-based, map-like data
     * structures which use array indices to map keys to values.
     *
     * @param <T> The type
     * @param keys The array whose values should be sorted
     * @param other The array whose contents should be reordered so that if
     * <code>keys[n] = a</code> and <code>other[n] = b</code>, that will remain
     * true even if sorting changes the value of <code>n</code>.
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     */
    public static <T> void biSort(float[] keys, T[] other, int size) {
        checkSizeInvariants(keys, other, size);
        multiSort(keys, size, Swapper.forArray(other));
    }

    /**
     * Sort the array passed as the first argument, simultaneously reordering
     * the second array argument to the same positions, so items correlated by
     * index remain correlated. Useful for building array-based, map-like data
     * structures which use array indices to map keys to values.
     *
     * @param <T> The type
     * @param keys The array whose values should be sorted
     * @param other The array whose contents should be reordered so that if
     * <code>keys[n] = a</code> and <code>other[n] = b</code>, that will remain
     * true even if sorting changes the value of <code>n</code>.
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     */
    public static <T> void biSort(long[] keys, T[] other, int size) {
        checkSizeInvariants(keys, other, size);
        multiSort(keys, size, Swapper.forArray(other));
    }

    /**
     * Sort the array passed as the first argument, simultaneously reordering
     * the second array argument to the same positions, so items correlated by
     * index remain correlated. Useful for building array-based, map-like data
     * structures which use array indices to map keys to values.
     *
     * @param <T> The type
     * @param keys The array whose values should be sorted
     * @param other The array whose contents should be reordered so that if
     * <code>keys[n] = a</code> and <code>other[n] = b</code>, that will remain
     * true even if sorting changes the value of <code>n</code>.
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     */
    public static <T> void biSort(short[] keys, T[] other, int size) {
        checkSizeInvariants(keys, other, size);
        multiSort(keys, size, Swapper.forArray(other));
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(int[] keys, int size, Swapper swapper) {
        checkSizeInvariant(keys, size);
        _multiSort(keys, size, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(int[] keys, int size, Swapper swapper) {
        if (size < 2) {
            return;
        }
        sortInts(keys, swapper, 0, size);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param fromIndex The index at which to commence sorting (must be
     * <code>&lt;=toIndex</code>)
     * @param toIndex The (exclusive) position after the last array index which
     * should be affected by the sort
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(int[] keys, int fromIndex, int toIndex, Swapper swapper) {
        checkBoundsInvariant(keys, fromIndex, toIndex);
        _multiSort(keys, fromIndex, toIndex, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(int[] keys, int fromIndex, int toIndex, Swapper swapper) {
        sortInts(keys, swapper, fromIndex, toIndex - fromIndex);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param fromIndex The index at which to commence sorting (must be
     * <code>&lt;=toIndex</code>)
     * @param toIndex The (exclusive) position after the last array index which
     * should be affected by the sort
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(long[] a, int fromIndex, int toIndex, Swapper swapper) {
        checkBoundsInvariant(a, fromIndex, toIndex);
        _multiSort(a, fromIndex, toIndex, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(long[] a, int fromIndex, int toIndex, Swapper swapper) {
        sortLongs(a, swapper, fromIndex, toIndex - fromIndex);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(long[] keys, int size, Swapper swapper) {
        checkSizeInvariant(keys, size);
        _multiSort(keys, size, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(long[] keys, int size, Swapper swapper) {
        if (size < 2) {
            return;
        }
        sortLongs(keys, swapper, 0, size);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param fromIndex The index at which to commence sorting (must be
     * <code>&lt;=toIndex</code>)
     * @param toIndex The (exclusive) position after the last array index which
     * should be affected by the sort
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(double[] keys, int fromIndex, int toIndex, Swapper swapper) {
        checkBoundsInvariant(keys, fromIndex, toIndex);
        _multiSort(keys, fromIndex, toIndex, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(double[] keys, int fromIndex, int toIndex, Swapper swapper) {
        sortDoubles(keys, swapper, fromIndex, toIndex - fromIndex);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(double[] keys, int size, Swapper swapper) {
        checkSizeInvariant(keys, size);
        _multiSort(keys, size, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(double[] keys, int size, Swapper swapper) {
        if (size < 2) {
            return;
        }
        sortDoubles(keys, swapper, 0, size);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(float[] keys, int size, Swapper swapper) {
        checkSizeInvariant(keys, size);
        _multiSort(keys, size, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(float[] keys, int size, Swapper swapper) {
        if (size < 2) {
            return;
        }
        sortFloats(keys, swapper, 0, size);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param fromIndex The index at which to commence sorting (must be
     * <code>&lt;=toIndex</code>)
     * @param toIndex The (exclusive) position after the last array index which
     * should be affected by the sort
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(float[] keys, int fromIndex, int toIndex, Swapper swapper) {
        checkBoundsInvariant(keys, fromIndex, toIndex);
        _multiSort(keys, fromIndex, toIndex, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(float[] keys, int fromIndex, int toIndex, Swapper swapper) {
        sortFloats(keys, swapper, fromIndex, toIndex - fromIndex);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param size The array position representing the index after the last
     * index which can be potentially altered by the sorting operation
     * (frequently this is <code>keys.length</code>)
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(short[] keys, int size, Swapper swapper) {
        checkSizeInvariant(keys, size);
        _multiSort(keys, size, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(short[] keys, int size, Swapper swapper) {
        if (size < 2) {
            return;
        }
        sortShorts(keys, swapper, 0, size);
    }

    /**
     * Sort the array passed as the first argument, and whenever the values at
     * two positions in the array are swapped, pass the indices to swap to the
     * passed <code>Swapper</code>.
     *
     * @param keys The array to sort
     * @param fromIndex The index at which to commence sorting (must be
     * <code>&lt;=toIndex</code>)
     * @param toIndex The (exclusive) position after the last array index which
     * should be affected by the sort
     * @param swapper A BiConsumer of ints which is passed each pair of indices
     * whose contents should be swapped as the sort proceeds
     */
    public static void multiSort(short[] keys, int fromIndex, int toIndex, Swapper swapper) {
        checkBoundsInvariant(keys, fromIndex, toIndex);
        _multiSort(keys, fromIndex, toIndex, Checks.notNull("swapper", swapper));
    }

    private static void _multiSort(short[] keys, int fromIndex, int toIndex, Swapper swapper) {
        sortShorts(keys, swapper, fromIndex, toIndex - fromIndex);
    }

    private static void sortInts(int[] x, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, swapper, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swap(x, swapper, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swap(x, swapper, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, swapper, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, swapper, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortInts(x, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortInts(x, swapper, n - s, s);
        }
    }

    private static void swap(int[] x, Swapper swapper, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;
        swapper.swap(a, b);
    }

    private static void vecswap(int[] x, Swapper swapper, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, swapper, a, b);
        }
    }

    private static int med3(int[] x, int a, int b, int c) {
        return (x[a] < x[b]
                ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    private static void sortDoubles(double[] x, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, swapper, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        double v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swap(x, swapper, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swap(x, swapper, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, swapper, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, swapper, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortDoubles(x, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortDoubles(x, swapper, n - s, s);
        }
    }

    private static void swap(double[] x, Swapper swapper, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;
        swapper.swap(a, b);
    }

    private static void vecswap(double[] x, Swapper swapper, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, swapper, a, b);
        }
    }

    private static int med3(double[] x, int a, int b, int c) {
        return (x[a] < x[b]
                ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    private static void sortFloats(float[] x, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, swapper, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        float v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swap(x, swapper, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swap(x, swapper, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, swapper, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, swapper, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortFloats(x, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortFloats(x, swapper, n - s, s);
        }
    }

    private static void swap(float[] x, Swapper swapper, int a, int b) {
        float t = x[a];
        x[a] = x[b];
        x[b] = t;
        swapper.swap(a, b);
    }

    private static void vecswap(float[] x, Swapper swapper, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, swapper, a, b);
        }
    }

    private static int med3(float[] x, int a, int b, int c) {
        return (x[a] < x[b]
                ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    private static void sortLongs(long[] x, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, swapper, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        long v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swap(x, swapper, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swap(x, swapper, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, swapper, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, swapper, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortLongs(x, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortLongs(x, swapper, n - s, s);
        }
    }

    private static void swap(long[] x, Swapper swapper, int a, int b) {
        long t = x[a];
        x[a] = x[b];
        x[b] = t;
        swapper.swap(a, b);
    }

    private static void vecswap(long[] x, Swapper swapper, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, swapper, a, b);
        }
    }

    private static int med3(long[] x, int a, int b, int c) {
        return (x[a] < x[b]
                ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    private static void sortShorts(short[] x, Swapper swapper, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, swapper, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        short v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swap(x, swapper, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swap(x, swapper, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, swapper, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, swapper, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, swapper, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortShorts(x, swapper, off, s);
        }
        if ((s = d - c) > 1) {
            sortShorts(x, swapper, n - s, s);
        }
    }

    private static void swap(short[] x, Swapper swapper, int a, int b) {
        short t = x[a];
        x[a] = x[b];
        x[b] = t;
        swapper.swap(a, b);
    }

    private static void vecswap(short[] x, Swapper swapper, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, swapper, a, b);
        }
    }

    private static int med3(short[] x, int a, int b, int c) {
        return (x[a] < x[b]
                ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    // Argument checks
    private static <T> void checkSizeInvariants(int[] keys, T[] other, int size) {
        Checks.notNull("keys", keys);
        Checks.notNull("other", other);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
        if (other.length < size) {
            throw new IllegalArgumentException("Associated array length " + other.length + "< sort limit " + size);
        }
    }

    private static <T> void checkSizeInvariants(double[] keys, T[] other, int size) {
        Checks.notNull("keys", keys);
        Checks.notNull("other", other);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
        if (other.length < size) {
            throw new IllegalArgumentException("Associated array length " + other.length + "< sort limit " + size);
        }
    }

    private static <T> void checkSizeInvariants(long[] keys, T[] other, int size) {
        Checks.notNull("keys", keys);
        Checks.notNull("other", other);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
        if (other.length < size) {
            throw new IllegalArgumentException("Associated array length " + other.length + "< sort limit " + size);
        }
    }

    private static <T> void checkSizeInvariants(float[] keys, T[] other, int size) {
        Checks.notNull("keys", keys);
        Checks.notNull("other", other);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
        if (other.length < size) {
            throw new IllegalArgumentException("Associated array length " + other.length + "< sort limit " + size);
        }
    }

    private static <T> void checkSizeInvariants(short[] keys, T[] other, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        Checks.notNull("other", other);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
        if (other.length < size) {
            throw new IllegalArgumentException("Associated array length " + other.length + "< sort limit " + size);
        }
    }

    private static void checkSizeInvariant(int[] keys, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
    }

    private static void checkSizeInvariant(double[] keys, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
    }

    private static void checkSizeInvariant(long[] keys, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
    }

    private static void checkSizeInvariant(float[] keys, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
    }

    private static void checkSizeInvariant(short[] keys, int size) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("size", size);
        if (keys.length < size) {
            throw new IllegalArgumentException("Number array length " + keys.length + " < sort limit " + size);
        }
    }

    private static void checkBoundsInvariant(int[] keys, int from, int to) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("from", from);
        if (to < from) {
            throw new IllegalArgumentException("End index " + to + " < start " + from);
        }
    }

    private static void checkBoundsInvariant(double[] keys, int from, int to) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("from", from);
        if (to < from) {
            throw new IllegalArgumentException("End index " + to + " < start " + from);
        }
    }

    private static void checkBoundsInvariant(float[] keys, int from, int to) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("from", from);
        if (to < from) {
            throw new IllegalArgumentException("End index " + to + " < start " + from);
        }
    }

    private static void checkBoundsInvariant(long[] keys, int from, int to) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("from", from);
        if (to < from) {
            throw new IllegalArgumentException("End index " + to + " < start " + from);
        }
    }

    private static void checkBoundsInvariant(short[] keys, int from, int to) {
        Checks.notNull("keys", keys);
        Checks.nonNegative("from", from);
        if (to < from) {
            throw new IllegalArgumentException("End index " + to + " < start " + from);
        }
    }

    private Sort() {
        throw new AssertionError();
    }
}
