package com.mastfrog.util.collections;

import com.mastfrog.util.strings.Strings;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Just an implementation of List backed by a primitive array.
 *
 * @author Tim Boudreau
 */
final class IntListImpl extends AbstractList<Integer> implements IntList, Serializable {

    private int[] values;
    private int size;
    private final int initialCapacity;

    IntListImpl(int initialCapacity) {
        this.values = new int[initialCapacity];
        this.initialCapacity = initialCapacity;
    }

    IntListImpl() {
        this(96);
    }

    IntListImpl(int[] ints) {
        this(ints, false);
    }

    IntListImpl(int[] ints, boolean unsafe) {
        this.values = ints.length == 0 ? new int[16] : unsafe ? ints : Arrays.copyOf(ints, ints.length);
        this.size = ints.length;
        initialCapacity = Math.max(16, size);
    }

    @Override
    public IntListImpl copy() {
        int[] newValues = Arrays.copyOf(values, size);
        return new IntListImpl(newValues, true);
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        if (c instanceof IntListImpl) {
            if (!c.isEmpty()) {
                maybeGrow(size + c.size());
                IntListImpl il = (IntListImpl) c;
                System.arraycopy(il.values, 0, values, size, il.size);
                size += il.size;
            }
            return c.isEmpty();
        } else {
            return super.addAll(c);
        }
    }

    @Override
    public IntListImpl subList(int fromIndex, int toIndex) {
        checkIndex(fromIndex);
        checkIndex(toIndex);
        int[] nue = new int[toIndex - fromIndex];
        System.arraycopy(values, fromIndex, nue, 0, nue.length);
        return new IntListImpl(nue, true);
    }

    @Override
    public int[] toIntArray() {
        return Arrays.copyOf(values, size);
    }

    @Override
    public int getAsInt(int index) {
        checkIndex(index);
        return values[index];
    }

    @Override
    public boolean removeLast() {
        if (size > 0) {
            size--;
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void add(int value) {
        maybeGrow(size + 1);
        values[size++] = value;
    }

    @Override
    public void addArray(int... arr) {
        maybeGrow(size + arr.length);
        System.arraycopy(arr, 0, values, size, arr.length);
        size += arr.length;
    }

    @Override
    public int indexOf(int value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addAll(int... values) {
        maybeGrow(size + values.length);
        System.arraycopy(values, 0, this.values, size, values.length);
        size += values.length;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index not between 0-" + size + ": " + index);
        }
    }

    @Override
    public void removeAt(int index) {
        checkIndex(index);
        if (index != size - 1) {
            System.arraycopy(values, index + 1, values, index, size - (index + 1));
        }
        size--;
    }

    @Override
    public Integer remove(int index) {
        checkIndex(index);
        int old = values[index];
        if (index != size - 1) {
            System.arraycopy(values, index + 1, values, index, size - (index + 1));
        }
        size--;
        return old;
    }

    @Override
    public void forEach(IntConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(values[i]);
        }
    }

    private void maybeGrow(int newSize) {
        if (newSize >= values.length) {
            if (newSize % initialCapacity == 0) {
                newSize += initialCapacity;
            } else {
                newSize = initialCapacity * Math.max(initialCapacity * 2, (newSize / initialCapacity) + 1);
            }
            values = Arrays.copyOf(values, newSize);
        }
    }

    @Override
    public Integer get(int index) {
        checkIndex(index);
        return values[index];
    }

    @Override
    public void forEach(Consumer<? super Integer> action) {
        for (int i = 0; i < size; i++) {
            action.accept(values[i]);
        }
    }

    @Override
    public void addAll(int index, int... nue) {
        checkIndex(index);
        maybeGrow(size + values.length);
        System.arraycopy(values, index, values, index + nue.length, size - index);
        System.arraycopy(nue, 0, values, index, values.length);
        size += values.length;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Integer> c) {
        if (c.isEmpty()) {
            return false;
        }
        if (c.size() == 1) {
            return add(c.iterator().next());
        }
        int[] all = new int[c.size()];
        int i = 0;
        for (Iterator<? extends Integer> it = c.iterator(); it.hasNext(); i++) {
            all[i] = it.next();
        }
        addAll(index, all);
        return true;
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public int lastIndexOf(int i) {
        for (int j = size - 1; j >= 0; j--) {
            if (values[j] == i) {
                return j;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Integer)) {
            return -1;
        }
        return lastIndexOf(((Integer) o).intValue());
    }

    @Override
    public void add(int index, Integer element) {
        add(index, element.intValue());
    }

    @Override
    public void add(int index, int element) {
        int sz = size();
        if (index < 0 || index >= sz) {
            throw new IllegalArgumentException("Index out of "
                    + "range - size " + size() + " but passed " + index);
        }
        maybeGrow(sz + 1);
        System.arraycopy(values, index, values, index + 1, (values.length - index) - 1);
        values[index] = element;
        size++;
    }

    @Override
    public int set(int index, int value) {
        checkIndex(index);
        int old = values[index];
        values[index] = value;
        return old;
    }

    @Override
    public Integer set(int index, Integer element) {
        return set(index, element.intValue());
    }

    @Override
    public boolean add(Integer e) {
        add(e.intValue());
        return true;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iter();
    }

    private class Iter implements PrimitiveIterator.OfInt {

        private int pos = -1;

        @Override
        public boolean hasNext() {
            return pos + 1 < size;
        }

        @Override
        public Integer next() {
            return nextInt();
        }

        @Override
        public int nextInt() {
            if (pos >= size) {
                throw new NoSuchElementException(pos + " of " + size);
            }
            return values[++pos];
        }
    }

    @Override
    public String toString() {
        return Strings.join(',', this);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < size; i++) {
            hashCode = 31 * hashCode + values[i];
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof IntListImpl) {
            IntListImpl other = (IntListImpl) o;
            if (other.size != size) {
                return false;
            }
            if (size == 0) {
                return true;
            }
            return Arrays.equals(values, 0, size, other.values, 0, size);
        } else {
            return super.equals(o);
        }
    }

}
