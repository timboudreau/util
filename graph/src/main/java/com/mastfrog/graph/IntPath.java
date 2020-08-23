package com.mastfrog.graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntConsumer;
import com.mastfrog.abstractions.list.IndexedResolvable;

/**
 * A path through a graph.
 *
 * @author Tim Boudreau
 */
public final class IntPath implements Comparable<IntPath>, Iterable<Integer> {

    private static int DEFAULT_SIZE = 12;
    private int[] items;
    private int size;

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

    public IntPath prepending(int value) {
        int[] nue = new int[size + 1];
        System.arraycopy(items, 0, nue, 1, items.length);
        nue[0] = value;
        return new IntPath(nue.length, nue);
    }

    public IntPath appending(int value) {
        int[] nue = new int[size+1];
        System.arraycopy(items, 0, nue, 0, items.length);
        items[size] = value;
        return new IntPath(nue.length, nue);
    }

    public void forEachInt(IntConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(items[i]);
        }
    }

    public int first() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return items[0];
    }

    public int last() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("empty");
        }
        return items[size-1];
    }

    private void growIfNeeded() {
        if (size == items.length - 1) {
            items = Arrays.copyOf(items, items.length + DEFAULT_SIZE);
        }
    }

    public IntPath addAll(int... values) {
        if (size + values.length < items.length) {
            items = Arrays.copyOf(items, items.length + values.length);
        }
        System.arraycopy(values, 0, items, size, values.length);
        size += values.length;
        return this;
    }

    IntPath add(int item) {
        growIfNeeded();
        items[size++] = item;
        return this;
    }

    IntPath append(IntPath other) {
        int targetSize = size() + other.size();
        if (items.length < targetSize) {
            items = Arrays.copyOf(items, targetSize);
        }
        System.arraycopy(other.items, 0, items, size, other.size());
        size = targetSize;
        return this;
    }

    IntPath trim() {
        items = Arrays.copyOf(items, size);
        return this;
    }

    public IntPath childPath() {
        if (size == 0) {
            return this;
        }
        int[] nue = new int[size - 1];
        System.arraycopy(items, 1, nue, 0, nue.length);
        return new IntPath(nue.length, nue);
    }

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
        return this;
    }

    public IntPath reversed() {
        int[] nue = new int[size];
        for (int i = 0; i < size; i++) {
            nue[i] = items[size - (i + 1)];
        }
        return new IntPath(size, nue);
    }

    public int start() {
        return size == 0 ? -1 : items[0];
    }

    public int end() {
        return size == 0 ? -1 : items[size - 1];
    }

    public boolean contains(IntPath path) {
        if (path.size() > size) {
            return false;
        } else if (path.size() == size) {
            return path.equals(this);
        } else {
            for (int i = 0; i < size - path.size(); i++) {
                if (arraysEquals(items, i, i + path.size(), path.items, 0, path.size())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean arraysEquals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
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

    public int indexOf(int val) {
        for (int i = 0; i < size; i++) {
            if (get(i) == val) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(int val) {
        for (int i = 0; i < size; i++) {
            if (val == get(i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " of " + size);
        }
        return items[index];
    }

    public int[] items() {
        return size == items.length ? items : Arrays.copyOf(items, size);
    }

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
        } else if (o == null) {
            return false;
        } else if (o instanceof IntPath) {
            return Arrays.equals(items(), ((IntPath) o).items());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items());
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
