package com.mastfrog.util.collections;

import com.mastfrog.util.search.Bias;
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
        if (c.isEmpty()) {
            return false;
        }
        if (c instanceof IntListImpl) {
            if (!c.isEmpty()) {
                maybeGrow(size + c.size());
                IntListImpl il = (IntListImpl) c;
                System.arraycopy(il.values, 0, values, size, il.size);
                size += il.size;
            }
            return c.isEmpty();
        } else {
            Iterator<? extends Integer> ints = c.iterator();
            if (ints instanceof PrimitiveIterator.OfInt) {
                int[] nue = new int[c.size()];
                PrimitiveIterator.OfInt p = (PrimitiveIterator.OfInt) ints;
                int ix = 0;
                while (p.hasNext()) {
                    nue[ix++] = p.nextInt();
                }
                addAll(nue);
                return nue.length > 0;
            } else {
                return super.addAll(c);
            }
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

    public void sort() {
        if (size > 0) {
            Arrays.sort(values, 0, size);
        }
    }

    public int indexOfPresumingSorted(int value) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            return value == values[0] ? 0 : -1;
        }
        return Arrays.binarySearch(values, 0, size, value);
    }

    public int adjustValues(int fromIndex, int toIndex, int by) {
        if (toIndex <= fromIndex) {
            throw new IllegalArgumentException("toIndex must be > fromIndex, "
                    + "but got " + fromIndex + ", " + toIndex);
        }
        int result = 0;
        if (by != 0) {
            for (int i = Math.max(0, fromIndex); i < Math.min(size, toIndex); i++) {
                values[i] += by;
                result++;
            }
        }
        return result;
    }

    public int adjustValues(int aboveIndex, int by) {
        int result = 0;
        if (by != 0) {
            for (int i = Math.max(0, aboveIndex); i < size; i++) {
                values[i] += by;
                result++;
            }
        }
        return result;
    }

    public int nearestIndexToPresumingSorted(int value, Bias bias) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            int val = values[0];
            switch (bias) {
                case BACKWARD:
                    if (val <= value) {
                        return 0;
                    } else {
                        return -1;
                    }
                case FORWARD:
                case NEAREST:
                    if (val >= value) {
                        return 0;
                    } else {
                        return -1;
                    }
            }
        }
        switch (bias) {
            case NONE:
                int result = indexOfPresumingSorted(value);
//                if (result < -1) {
//                    new IllegalStateException("Weird answer for indexOfPresumingSorted with bias none "
//                        + " " + result + " for value " + value + " in " + this).printStackTrace();
//                }
                if (result >= 0) {
                    while (result < size - 1 && values[result + 1] == values[result]) {
                        result++;
                    }
                }
                return result;
            case FORWARD:
            case BACKWARD:
                int res2 = nearestIndexToPresumingSorted(0, size - 1, bias, value);
                if (res2 != -1) {
                    while (res2 < size - 1 && values[res2 + 1] == values[res2]) {
                        res2++;
                    }
                }
                return res2;
            case NEAREST:
                int fwd = nearestIndexToPresumingSorted(0, size - 1, Bias.FORWARD, value);
                int bwd = nearestIndexToPresumingSorted(0, size - 1, Bias.BACKWARD, value);
                if (fwd == -1) {
                    return bwd;
                } else if (bwd == -1) {
                    return fwd;
                } else if (fwd == bwd) {
                    return fwd;
                } else {
                    int fwdDiff = Math.abs(values[fwd] - value);
                    int bwdDiff = Math.abs(values[bwd] - value);
                    if (fwdDiff == bwdDiff) {
                        return fwd;
                    } else if (fwdDiff < bwdDiff) {
                        return fwd;
                    } else {
                        return bwd;
                    }
                }
            default:
                throw new AssertionError(bias);
        }
    }

    public int last() {
        if (size == 0) {
            throw new NoSuchElementException("Empty");
        }
        return values[size - 1];
    }

    public int first() {
        if (size == 0) {
            throw new NoSuchElementException("Empty");
        }
        return values[0];
    }

    private int nearestIndexToPresumingSorted(int start, int end, Bias bias, int value) {
        // duplicate tolerance
//        while (end > 0 && values[end - 1] == values[end]) {
//            end--;
//            System.out.println("adj end to " + end + " looking for " +value);
//        }
//        while (start < size - 1 && values[start + 1] == values[start]) {
//            start++;
//            System.out.println("adj start to " + start + " looking for " + value);
//        }
        if (start == end) {
            int currentVal = values[start];
            if (currentVal == value) {
                return start;
            }
            switch (bias) {
                case BACKWARD:
                    if (currentVal <= value) {
                        return start;
                    } else {
                        return -1;
                    }
                case FORWARD:
                    if (currentVal >= value) {
                        return start;
                    } else {
                        return -1;
                    }
            }
        }
        int startVal = values[start];
        if (startVal == value) {
            return start;
        }
        if (startVal > value) {
            switch (bias) {
                case BACKWARD:
                    if (startVal > value) {
                        return -1;
                    }
                    return start - 1;
                case FORWARD:
                    return start;
                default:
                    return -1;
            }
        }
        int endVal = values[end];
        if (endVal == value) {
            return end;
        }
        if (endVal < value) {
            switch (bias) {
                case BACKWARD:
                    return end;
                case FORWARD:
                    int result = end + 1;
                    return result < size ? result : -1;
                default:
                    return -1;
            }
        }
        int mid = start + ((end - start) / 2);
        int midVal = values[mid];
//        while (mid < size-1 && values[mid+1] == midVal) {
//            mid++;
//        }
        if (midVal == value) {
            return mid;
        }
        // If we have an odd number of slots, we can get into trouble here:
        if (midVal < value && endVal > value) {
            int newStart = mid + 1;
            int newEnd = end - 1;
            int nextStartValue = values[newStart];
            if (nextStartValue > value && bias == Bias.BACKWARD && (newEnd - newStart <= 1 || midVal < value)) {
                return mid;
            }
            int nextEndValue = values[newEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && newEnd - newStart <= 1) {
                return end;
            }
            return nearestIndexToPresumingSorted(newStart, newEnd, bias, value);
        } else if (midVal > value && startVal < value) {
            int nextEnd = mid - 1;
            int nextStart = start + 1;
            int nextEndValue = values[nextEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && nextEnd - nextStart <= 1) {
                return mid;
            }
            int newStartValue = values[nextStart];
            if (bias == Bias.BACKWARD && newStartValue > value && (startVal < value || nextEnd - nextStart <= 1)) {
                return start;
            }
            return nearestIndexToPresumingSorted(nextStart, nextEnd, bias, value);
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

    @Override
    public void forEachReversed(IntConsumer c) {
        for (int i = size - 1; i >= 0; i--) {
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
    public PrimitiveIterator.OfInt iterator() {
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
        StringBuilder sb = new StringBuilder(2 + (4 * size)).append('[');
        for (int i = 0; i < size; i++) {
            sb.append(get(i));
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
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
            // JDK 9
//            return Arrays.equals(values, 0, size, other.values, 0, size);
            for (int i = 0; i < size; i++) {
                if (values[i] != other.values[i]) {
                    return false;
                }
            }
            return true;
        } else {
            return super.equals(o);
        }
    }

}
