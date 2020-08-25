package com.mastfrog.graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntConsumer;
import com.mastfrog.abstractions.list.IndexedResolvable;
import java.util.BitSet;

/**
 * A finite path between two elements in a graph.
 *
 * @author Tim Boudreau
 */
public final class IntPath implements Comparable<IntPath>, Iterable<Integer> {

    private static int DEFAULT_SIZE = 12;
    private int[] items;
    private int size;
    private BitSet contents;

    private IntPath(int size, int[] items) {
        this.size = size;
        this.items = Arrays.copyOf(items, size + DEFAULT_SIZE);
    }

    IntPath(int initialItems) {
        items = new int[initialItems];
    }

    IntPath() {
        this(DEFAULT_SIZE);
    }

    IntPath copy() {
        return new IntPath(size, items);
    }

    /**
     * Create a new path prepending the passed value.
     *
     * @param value A value
     * @return this
     * @throws IllegalArgumentException if the value is negative
     */
    public IntPath prepending(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Paths may not contain "
                    + "negative elements: " + value);
        }
        int[] nue = new int[size + 1];
        System.arraycopy(items, 0, nue, 1, items.length);
        nue[0] = value;
        IntPath result = new IntPath(nue.length, nue);
        if (contents != null) {
            BitSet newContents = (BitSet) contents.clone();
            newContents.set(value);
            result.contents = newContents;
        }
        return result;
    }

    /**
     * Create a new path appending the passed value.
     *
     * @param value A value
     * @return this
     * @throws IllegalArgumentException if the value is negative
     */
    public IntPath appending(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Paths may not contain "
                    + "negative elements: " + value);
        }
        int[] nue = new int[size + 1];
        System.arraycopy(items, 0, nue, 0, items.length);
        items[size] = value;
        IntPath result = new IntPath(nue.length, nue);
        if (contents != null) {
            BitSet newContents = (BitSet) contents.clone();
            newContents.set(value);
            result.contents = newContents;
        }
        return result;
    }

    /**
     * Iterate all elements of the path, passing them to the passed consumer.
     *
     * @param c A consumer
     */
    public void forEachInt(IntConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(items[i]);
        }
    }

    /**
     * Iterate all elements of the path in reverse order, passing them to the
     * passed consumer.
     *
     * @param c A consumer
     */
    public void forEachIntReversed(IntConsumer c) {
        for (int i = size - 1; i >= 0; i--) {
            c.accept(items[i]);
        }
    }

    /**
     * Get the first path element
     *
     * @return The first path element
     * @throws IndexOutOfBoundsException if out of range
     */
    public int first() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return items[0];
    }

    /**
     * Get the last path element
     *
     * @return The first path element
     * @throws IndexOutOfBoundsException if out of range
     */
    public int last() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("empty");
        }
        return items[size - 1];
    }

    private void growIfNeeded() {
        if (size == items.length - 1) {
            items = Arrays.copyOf(items, items.length + DEFAULT_SIZE);
        }
    }

    IntPath addAll(int... values) {
        if (values.length == 0) {
            return this;
        }
        if (size + values.length < items.length) {
            items = Arrays.copyOf(items, items.length + values.length);
        }
        System.arraycopy(values, 0, items, size, values.length);
        size += values.length;
        contents = null;
        cachedHashCode = 0;
        return this;
    }

    IntPath add(int item) {
        growIfNeeded();
        items[size++] = item;
        contents = null;
        cachedHashCode = 0;
        return this;
    }

    IntPath append(IntPath other) {
        if (other.isEmpty()) {
            return this;
        }
        int targetSize = size() + other.size();
        if (items.length < targetSize) {
            items = Arrays.copyOf(items, targetSize);
        }
        System.arraycopy(other.items, 0, items, size, other.size());
        size = targetSize;
        contents = null;
        cachedHashCode = 0;
        return this;
    }

    IntPath trim() {
        items = Arrays.copyOf(items, size);
        return this;
    }

    /**
     * Create a new path, lopping off the first element.
     *
     * @return A new path
     */
    public IntPath childPath() {
        if (size == 0) {
            return this;
        }
        int[] nue = new int[size - 1];
        System.arraycopy(items, 1, nue, 0, nue.length);
        return new IntPath(nue.length, nue);
    }

    /**
     * Create a new path, lopping off the last element.
     *
     * @return A new path
     */
    public IntPath parentPath() {
        if (size == 0) {
            return this;
        }
        int[] nue = new int[size - 1];
        System.arraycopy(items, 0, nue, 0, nue.length);
        return new IntPath(nue.length, nue);
    }

    IntPath replace(int index, IntPath other) {
        size = index;
        append(other);
        contents = null;
        cachedHashCode = 0;
        return this;
    }

    /**
     * Create a new path whose elements are this one's in reverse order.
     *
     * @return A new path
     */
    public IntPath reversed() {
        int[] nue = new int[size];
        for (int i = 0; i < size; i++) {
            nue[i] = items[size - (i + 1)];
        }
        IntPath result = new IntPath(size, nue);
        result.contents = contents;
        return result;
    }

    public int start() {
        return size == 0 ? -1 : items[0];
    }

    public int end() {
        return size == 0 ? -1 : items[size - 1];
    }

    /**
     * Determine if this path contains the subsequence in the passed path, or is
     * equal to it.
     *
     * @param path A path
     * @return true if this path contains the passed one
     */
    public boolean contains(IntPath path) {
        if (path == this) {
            return true;
        } else if (path.size() > size) {
            return false;
        } else if (path.size() == size) {
            return path.equals(this);
        } else {
            BitSet ct = contents();
            for (int i = 0; i < path.size; i++) {
                if (!ct.get(path.get(i))) {
                    return false;
                }
            }
            for (int i = 0; i < size - path.size(); i++) {
                if (arraysEquals(items, i, i + path.size(), path.items, 0, path.size())) {
                    return true;
                }
            }
        }
        return false;
    }

    private BitSet contents() {
        if (contents != null) {
            return contents;
        }
        BitSet result = new BitSet(size);
        for (int i = 0; i < items.length; i++) {
            result.set(items[i]);
        }
        return result;
    }

    static boolean arraysEquals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        // JDK 9
//        return Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (; aFromIndex < aToIndex && bFromIndex < bToIndex; aFromIndex++, bFromIndex++) {
            if (a[aFromIndex] != b[bFromIndex]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the first occurrence of the passed value in this path.
     *
     * @param val An integer path element
     * @return the index or -1
     */
    public int indexOf(int val) {
        if (!contains(val)) {
            return -1;
        }
        for (int i = 0; i < size; i++) {
            if (get(i) == val) {
                return i;
            }
        }
        throw new AssertionError("contents bitset out of sync");
    }

    /**
     * Get the first occurrence of the passed value in this path.
     *
     * @param val An integer path element
     * @return the index or -1
     */
    public int lastIndexOf(int val) {
        if (!contains(val)) {
            return -1;
        }
        for (int i = size - 1; i >= 0; i--) {
            if (get(i) == val) {
                return i;
            }
        }
        throw new AssertionError("contents bitset out of sync");
    }

    /**
     * Determine if this path contains the passed value.
     *
     * @param val A value
     * @return true if it is present
     */
    public boolean contains(int val) {
        if (val < 0) {
            throw new IllegalArgumentException("Cannot contain negative values: " + val);
        }
        return contents().get(val);
    }

    /**
     * Determine if this path is empty.
     *
     * @return True if it is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Get the number of elements in this path.
     *
     * @return The number of elements
     */
    public int size() {
        return size;
    }

    /**
     * Determine if this path does not represent an actual path - if it has one
     * element or less.
     *
     * @return True if this path does not have an endpoint and may or may not
     * have a start point
     */
    public boolean isNotAPath() {
        return size() < 2;
    }

    /**
     * Get the path element at the specified index.
     *
     * @param index The index within the path
     * @return The index or -1 if not present
     */
    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " of " + size);
        }
        return items[index];
    }

    /**
     * Get the contents of this path as an int[].
     *
     * @return An array
     */
    public int[] items() {
        return size == items.length ? items : Arrays.copyOf(items, size);
    }

    /**
     * Iterate the contents.
     *
     * @param cons
     * @deprecated use the better-named <code>forEachInt()</code>
     */
    @Deprecated
    public void iterate(IntConsumer cons) {
        for (int i = 0; i < size; i++) {
            cons.accept(get(i));
        }
        cons.accept(-1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof IntPath)) {
            return false;
        }
        IntPath ip = (IntPath) o;
        if (size != ip.size || hashCode() != ip.hashCode()) {
            return false;
        }

        return arraysEquals(items, 0, size, ip.items, 0, size);
    }

    private int cachedHashCode = 0;

    @Override
    public int hashCode() {
        if (cachedHashCode != 0) {
            return cachedHashCode;
        }
        int result = 1;
        for (int i = 0; i < size; i++) {
            result = 31 * result + this.items[i];
        }
        return cachedHashCode = result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < size(); i++) {
            int value = get(i);
            if (sb.length() != 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    public <T> ObjectPath<T> toObjectPath(IndexedResolvable<T> indexed) {
        return new ObjectPath<>(this, indexed);
    }

    @Override
    public int compareTo(IntPath o) {
        int a = size();
        int b = o.size();
        return a > b ? 1 : a < b ? -1 : 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IIt();
    }

    class IIt implements Iterator<Integer> {

        int pos = -1;

        @Override
        public boolean hasNext() {
            return pos + 1 < size();
        }

        @Override
        public Integer next() {
            return get(++pos);
        }
    }
}
