package com.mastfrog.graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntConsumer;
import com.mastfrog.abstractions.list.IndexedResolvable;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.PrimitiveIterator;

/**
 * A finite path between two elements in a graph.
 *
 * @author Tim Boudreau
 */
public final class IntPath implements Comparable<IntPath>, Iterable<Integer>, Serializable {

    private static int DEFAULT_SIZE = 12;
    private int[] items;
    private int size;
    private transient BitSet contents;

    IntPath(int size, int[] items) {
        this.size = size;
        this.items = Arrays.copyOf(items, size + DEFAULT_SIZE);
    }

    IntPath(boolean unsafe, int[] items) {
        this(items.length, unsafe, items);
    }

    IntPath(int size, boolean unsafe, int[] items) {
        this.items = unsafe ? items : Arrays.copyOf(notNull("items", items), items.length);
        this.size = size;
        if (!unsafe) {
            if (size < items.length) {
                throw new IllegalArgumentException("Size " + size
                        + " is > array length " + items.length);
            }
            for (int i = 0; i < items.length; i++) {
                if (items[i] < 0) {
                    throw new IllegalArgumentException("Negative numbers not "
                            + "allowed");
                }
            }
        }
    }

    IntPath(int initialItems) {
        items = new int[initialItems];
    }

    IntPath() {
        this(DEFAULT_SIZE);
    }

    IntPath(Collection<? extends Integer> ints) {
        items = new int[ints.size()];
        Iterator<? extends Integer> iter = ints.iterator();
        if (iter instanceof PrimitiveIterator.OfInt) {
            PrimitiveIterator.OfInt pi = (PrimitiveIterator.OfInt) iter;
            int cursor = 0;
            while (pi.hasNext()) {
                items[cursor++] = pi.nextInt();
            }
            size = cursor;
        } else {
            int cursor = 0;
            while (iter.hasNext()) {
                items[cursor++] = notNull("Null at " + (cursor + 1), iter.next());
            }
            size = cursor;
        }
    }

    IntPath copy() {
        IntPath result = new IntPath(size, items);
        if (contents != null) {
            result.contents = (BitSet) contents.clone();
        }
        return result;
    }

    IntPath(String ser) {
        String[] parts = ser.split("\\s*?,\\s*");
        items = new int[parts.length];
        int cursor = 0;
        contents = new BitSet(parts.length);
        for (int i = 0; i < parts.length; i++) {
            int val = Integer.parseInt(parts[i]);
            if (val < 0) {
                throw new IllegalArgumentException("Path may not contain "
                        + "negative numbers");
            }
            items[cursor++] = val;
            contents.set(val);
        }
        size = cursor;
    }

    public static IntPath of(Collection<? extends Integer> ints) {
        return new IntPath(notNull("ints", ints));
    }

    /**
     * Creates an IntPath from a comma-delimited list of integers (which may
     * contain whitespace), such as the output of <code>toString()</code>.
     *
     * @param val A string
     * @return A path
     * @throws NumberFormatException If the text contains non-comma,
     * non-whitespace, non-digit characters
     */
    public static IntPath parse(String val) {
        return new IntPath(notNull("val", val));
    }

    /**
     * Create a new IntPath of this one concatenated with another.
     *
     * @param other The other path
     * @return A new IntPath
     */
    public IntPath appending(IntPath other) {
        if (other.isEmpty()) {
            return copy();
        }
        int[] nue = Arrays.copyOf(items, size + other.size());
        int[] append = other.items();
        System.arraycopy(append, 0, nue, size, other.size());
        return new IntPath(true, nue);
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
        int[] nue = items.length > size + 1 ? Arrays.copyOf(items, items.length)
                : Arrays.copyOf(items, items.length + 1);
        nue[size] = value;
        IntPath result = new IntPath(size + 1, true, nue);
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

    /**
     * Create an IntPath from an array of ints.
     *
     * @param items An array of ints
     * @return An int path
     */
    public static IntPath of(int... items) {
        return new IntPath(false, items);
    }

    /**
     * Create an IntPath from an array of ints, using the passed array directly
     * rather than copying it.
     *
     * @param items An array of ints
     * @return An int path
     */
    public static IntPath ofUnsafe(int... items) {
        return new IntPath(false, items);
    }

    /**
     * Create a builder for int paths.
     *
     * @return A builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder for int paths.
     *
     * @param first The first element
     * @return A builder
     */
    public static Builder builder(int first) {
        return new Builder(first);
    }

    /**
     * Create a builder initialized from this path.
     *
     * @return a builder
     */
    public Builder toBuilder() {
        return new Builder(copy());
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
        if (items.length > size) {
            items = Arrays.copyOf(items, size);
        }
        return this;
    }

    /**
     * Create a new path, lopping off the first element.
     *
     * @return A new path, or this path if this path is empty
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
        int[] nue = Arrays.copyOf(items, size - 1);
        return new IntPath(nue.length, true, nue);
    }

    IntPath replaceFrom(int index, IntPath other) {
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

    /**
     * Determine if this path starts with (or is equal to) another path.
     *
     * @param other Another path
     * @return
     */
    public boolean startsWith(IntPath other) {
        if (other.isEmpty() || isEmpty()) {
            return false;
        } else if (other == this) {
            return true;
        } else if (other.size > size) {
            return false;
        }
        int max = Math.min(size, other.size);
        for (int i = 0; i < max; i++) {
            if (get(i) != other.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if this path ends with (or is equal to) another path.
     *
     * @param other Another path
     * @return
     */
    public boolean endsWith(IntPath other) {
        if (other.isEmpty() || isEmpty()) {
            return false;
        } else if (other == this) {
            return true;
        } else if (other.size > size) {
            return false;
        }
        for (int mine = size - 1, theirs = other.size - 1; mine >= 0 && theirs >= 0; mine--, theirs--) {
            if (get(mine) != other.get(theirs)) {
                return false;
            } else if (mine == 0 && theirs != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a subpath.
     *
     * @param start The start index, inclusive
     * @param end The end index, exclusive
     * @return A path
     * @throws IllegalArgumentException if the arguments are out of range or
     * invalid
     */
    public IntPath subPath(int start, int end) {
        if (start >= size || end > size) {
            throw new IllegalArgumentException(start + ":" + end + " is out of range 0:" + size);
        } else if (start == 0 && end == size) {
            return this;
        } else if (start > end) {
            throw new IllegalArgumentException("Start > end: " + start + ":" + end);
        } else if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Start or end < 0: " + start + ":" + end);
        } else if (start == end) {
            return new IntPath(0, true, new int[0]);
        }
        int[] result = new int[end - start];
        System.arraycopy(items, start, result, 0, end - start);
        return new IntPath(true, result);
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
        if (isEmpty() || path.isEmpty()) {
            return false;
        } else if (path == this) {
            return true;
        } else if (path.size() > size) {
            return false;
        } else if (path.size() == size) {
            return path.equals(this);
        } else {
            BitSet ct = contents();
            if (endsWith(path)) {
                return true;
            }
            for (int i = 0; i < path.size; i++) {
                if (!ct.get(path.get(i))) {
                    return false;
                }
            }
            for (int i = 0; i < (size - path.size()); i++) {
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
     * @param from The index to start searching at
     * @param val An integer path element
     * @return the index or -1
     */
    public int indexOf(int from, int val) {
        if (!contains(val) || from >= size) {
            return -1;
        }
        for (int i = Math.max(0, from); i < size; i++) {
            if (get(i) == val) {
                return i;
            }
        }
        if (from == 0) {
            throw new AssertionError("contents bitset out of sync");
        }
        return -1;
    }

    /**
     * Get the last occurrence of the passed value in this path.
     *
     * @param from The index to stop searching at
     * @param val An integer path element
     * @return the index or -1
     */
    public int lastIndexOf(int from, int val) {
        if (!contains(val) || from < 0) {
            return -1;
        }
        for (int i = from; i >= 0; i--) {
            if (get(i) == val) {
                return i;
            }
        }
        if (from == 0) {
            throw new AssertionError("contents bitset out of sync");
        }
        return -1;
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
        for (int i = 0; i < size; i++) {
            if (get(i) != ip.get(i)) {
                return false;
            }
        }
        return true;
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

    public final int sum() {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result += items[i];
        }
        return result;
    }

    /**
     * Create an ObjectPath from this IntPath.
     *
     * @param <T> The object type
     * @param indexed A list of objects
     * @return An object path
     */
    public <T> ObjectPath<T> toObjectPath(List<T> indexed) {
        return toObjectPath(IndexedResolvable.forList(indexed));
    }

    /**
     * Create an ObjectPath from this IntPath.
     *
     * @param <T> The object type
     * @param indexed An array of comparable items
     * @return An object path
     */
    @SafeVarargs // well, more like it doesn't care
    public final <T extends Comparable<T>> ObjectPath<T> toObjectPath(T... indexed) {
        return toObjectPath(IndexedResolvable.fromArray(indexed));
    }

    /**
     * Create an ObjectPath from this IntPath.
     *
     * @param <T> The object type
     * @param indexed An IndexedResolvable (has static methods to wrap lists,
     * etc.)
     * @return An object path
     */
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

    /**
     * A builder for IntPaths.
     */
    public static final class Builder {

        private IntPath result;

        Builder() {
            result = new IntPath();
        }

        Builder(int first) {
            result = new IntPath();
            result.add(first);
        }

        Builder(Builder toCopy) {
            this.result = toCopy.result.copy();
        }

        Builder(IntPath orig) {
            result = orig.copy();
        }

        /**
         * Make a copy of this builder and its internal state.
         *
         * @return A copy of this builder
         */
        public Builder copy() {
            return new Builder(result);
        }

        /**
         * Add an int to this builder.
         *
         * @param val A value
         * @return this
         */
        public Builder add(int val) {
            result.add(val);
            return this;
        }

        /**
         * Add multiple ints to this builder.
         *
         * @param values An array of ints
         * @return this
         */
        public Builder add(int... values) {
            result.addAll(values);
            return this;
        }

        /**
         * Create an IntPath from this builder.
         *
         * @return An IntPath
         */
        public IntPath build() {
            return result.copy().trim();
        }

        /**
         * Prepend an int to the head of this builder's result.
         *
         * @param val An int
         * @return A builder
         */
        public Builder prepend(int val) {
            result = result.prepending(val);
            return this;
        }
    }
}
