package com.mastfrog.util.collections;

import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.search.Bias;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * Alternate implementation of LongList, which is backed by a single array, so
 * insertions are more expensive, but iteration and other operations are less
 * complex and cheaper.
 *
 * @author Tim Boudreau
 */
final class LongListSimple extends AbstractList<Long> implements LongList, Serializable, Trimmable {

    private long[] values;
    private int size;
    private final int initialCapacity;

    LongListSimple(int initialCapacity) {
        this.values = new long[Math.max(16, initialCapacity)];
        this.initialCapacity = initialCapacity;
    }

    LongListSimple() {
        this(48);
    }

    LongListSimple(long[] ints) {
        this(ints, false);
    }

    LongListSimple(long[] ints, boolean unsafe) {
        this.values = ints.length == 0 ? new long[16] : unsafe ? ints : Arrays.copyOf(ints, ints.length);
        this.size = ints.length;
        initialCapacity = Math.max(16, size);
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex > size " + toIndex
                    + " vs " + size);
        } else if (fromIndex < 0) {
            throw new IllegalArgumentException("Start index < 0: "
                    + fromIndex);
        } else if (fromIndex > toIndex) {
            throw new IllegalArgumentException("Start index > end index: "
                    + fromIndex + " > " + toIndex);
        } else if (fromIndex == toIndex) {
            return;
        } else if (fromIndex == 0 && toIndex == size) {
            clear();
            return;
        }
        if (toIndex == size) {
            size = fromIndex;
        } else {
            int len = toIndex - fromIndex;
            System.arraycopy(values, toIndex, values, fromIndex,
                    size - toIndex);
            size -= len;
        }
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Long) || isEmpty()) {
            return -1;
        }
        long val = (Long) o;
        for (int i = 0; i < size; i++) {
            if (values[i] == val) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void trim() {
        if (values.length != size) {
            values = Arrays.copyOf(values, size);
        }
    }

    @Override
    public LongListSimple copy() {
        long[] newValues = Arrays.copyOf(values, size);
        return new LongListSimple(newValues, true);
    }

    @Override
    public boolean addAll(Collection<? extends Long> c) {
        if (c.isEmpty()) {
            return false;
        }
        if (c instanceof LongListSimple) {
            if (!c.isEmpty()) {
                maybeGrow(size + c.size());
                LongListSimple il = (LongListSimple) c;
                System.arraycopy(il.values, 0, values, size, il.size);
                size += il.size;
            }
            return c.isEmpty();
        } else {
            Iterator<? extends Long> ints = c.iterator();
            if (ints instanceof PrimitiveIterator.OfLong) {
                long[] nue = new long[c.size()];
                PrimitiveIterator.OfLong p = (PrimitiveIterator.OfLong) ints;
                int ix = 0;
                while (p.hasNext()) {
                    nue[ix++] = p.nextLong();
                }
                addAll(nue);
                return true;
            } else {
                while (ints.hasNext()) {
                    add(ints.next().longValue());
                }
            }
            return true;
        }

    }

    @Override
    public LongListSimple subList(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return new LongListSimple(new long[0], true);
        }
        if (fromIndex == 0 && toIndex == size) {
            return copy();
        }
        checkIndex(fromIndex);
        checkIndex(toIndex - 1);
        long[] nue = new long[toIndex - fromIndex];
        System.arraycopy(values, fromIndex, nue, 0, nue.length);
        return new LongListSimple(nue, true);
    }

    @Override
    public boolean isSorted() {
        // XXX pending
        return isEmpty();
    }

    @Override
    public int removeValue(long val) {
        int ix = indexOf(val);
        if (ix >= 0) {
            removeAt(ix);
        }
        return ix;
    }

    @Override
    public int indexOfArray(long[] longs) {
        if (notNull("longs", longs).length == 0) {
            return -1;
        }
        if (longs.length == 1) {
            return indexOf(longs[0]);
        }
        int first = indexOf(longs[0]);
        if (first == -1) {
            return -1;
        } else if ((size - 1) - first < longs.length) {
            return -1;
        }
        for (int i = first + 1; i < first + longs.length; i++) {
            if (longs[i - first] != getAsLong(i)) {
                break;
            }
            if (i == first + longs.length - 1) {
                return first;
            }
        }
        int i, j;
        loop:
        for (i = first + 1; i < (size - 1) - longs.length; i++) {
            for (j = 0; j < longs.length; j++) {
                if (longs[j] != getAsLong(i + j)) {
                    continue loop;
                }
                if (j == longs.length - 1) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Longerator longerator() {
        return new Lng();
    }

    @SuppressWarnings("deprecation")
    final class Lng implements Longerator {

        private int ix = -1;

        @Override
        public long next() {
            return values[++ix];
        }

        @Override
        public boolean hasNext() {
            return ix + 1 < size;
        }
    }

    @Override
    public long[] toLongArray() {
        return Arrays.copyOf(values, size);
    }

    @Override
    public long getAsLong(int index) {
        if (size == 0) {
            throw new IndexOutOfBoundsException(index + " of " + size);
        }
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
    public boolean add(long value) {
        maybeGrow(size + 1);
        values[size++] = value;
        return true;
    }

    @Override
    public int indexOf(long value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean sort() {
        if (size > 0) {
            Arrays.sort(values, 0, size);
            return true;
        }
        return false;
    }

//    @Override
    @Override
    public int indexOfPresumingSorted(long value) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            return value == values[0] ? 0 : -1;
        }
        return Arrays.binarySearch(values, 0, size, value);
    }

    public int adjustValues(int fromIndex, int toIndex, long by) {
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

    public int adjustValues(int aboveIndex, long by) {
        int result = 0;
        if (by != 0) {
            for (int i = Math.max(0, aboveIndex); i < size; i++) {
                values[i] += by;
                result++;
            }
        }
        return result;
    }

    @Override
    public int nearestIndexToPresumingSorted(long value, Bias bias) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            long val = values[0];
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
                    long fwdDiff = Math.abs(values[fwd] - value);
                    long bwdDiff = Math.abs(values[bwd] - value);
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

    @Override
    public long last() {
        if (size == 0) {
            throw new NoSuchElementException("Empty");
        }
        return values[size - 1];
    }

    @Override
    public long first() {
        if (size == 0) {
            throw new NoSuchElementException("Empty");
        }
        return values[0];
    }

    @Override
    @SuppressWarnings("null")
    public boolean startsWith(List<Long> others) {
        if (notNull("others", others).isEmpty() || isEmpty()) {
            return false;
        }
        if (others instanceof LongList) {
            return startsWithLongList((LongList) others);
        }
        for (int i = 0; i < others.size(); i++) {
            if (getAsLong(i) != others.get(i).longValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("null")
    public boolean endsWith(List<Long> others) {
        if (notNull("others", others).isEmpty() || isEmpty()) {
            return false;
        }
        if (others instanceof LongList) {
            return endsWithLongList((LongList) others);
        }
        if (others.size() == size()) {
            return false;
        }
        for (int i = size - 1, j = others.size() - 1; i > 0 && j >= 0; j--, i--) {
            if (getAsLong(i) != others.get(j).longValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithLongList(LongList other) {
        if (isEmpty() || other.isEmpty() || other.size() >= size || other == this) {
            return false;
        }
        if (other instanceof LongListSimple) {
            return _startsWith((LongListSimple) other);
        }
        for (int i = 0; i < other.size(); i++) {
            if (other.getAsLong(i) != values[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean _startsWith(LongListSimple other) {
        int last = other.size - 1;
        if (values[0] != other.values[0]) {
            return false;
        }
        if (values[last] != other.values[last]) {
            return false;
        }
        if (last == 1) {
            return true;
        }
//        return Arrays.equals(values, 1, last-1, other.values, 1, last-1); // XXX JDK9
        for (int i = 1; i < last - 1; i++) {
            if (values[last] != other.values[last]) {
                return false;
            }
        }
        return true;
    }

    private boolean endsWithLongList(LongList other) {
        if (other.size() >= size()) {
            return false;
        }
        if (other instanceof LongListSimple) {
            return _endsWith((LongListSimple) other);
        }
        for (int i = size - 1, j = other.size() - 1; i > 0 && j >= 0; j--, i--) {
            if (values[i] != other.get(j)) {
                return false;
            }
        }
        return true;
    }

    private boolean _endsWith(LongListSimple other) {
//        return Arrays.equals(values, size-other.size(), size, other.values, 0, other.size(); // XXX JDK9        
        for (int i = size - 1, j = other.size() - 1; i > 0 && j >= 0; j--, i--) {
            if (values[i] != other.values[j]) {
                return false;
            }
        }
        return true;
    }

    private int nearestIndexToPresumingSorted(int start, int end, Bias bias, long value) {
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
            long currentVal = values[start];
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
        long startVal = values[start];
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
        long endVal = values[end];
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
        long midVal = values[mid];
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
            long nextStartValue = values[newStart];
            if (nextStartValue > value && bias == Bias.BACKWARD && (newEnd - newStart <= 1 || midVal < value)) {
                return mid;
            }
            long nextEndValue = values[newEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && newEnd - newStart <= 1) {
                return end;
            }
            return nearestIndexToPresumingSorted(newStart, newEnd, bias, value);
        } else if (midVal > value && startVal < value) {
            int nextEnd = mid - 1;
            int nextStart = start + 1;
            long nextEndValue = values[nextEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && nextEnd - nextStart <= 1) {
                return mid;
            }
            long newStartValue = values[nextStart];
            if (bias == Bias.BACKWARD && newStartValue > value && (startVal < value || nextEnd - nextStart <= 1)) {
                return start;
            }
            return nearestIndexToPresumingSorted(nextStart, nextEnd, bias, value);
        }
        return -1;
    }

    @Override
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(long... values) {
        maybeGrow(size + values.length);
        System.arraycopy(values, 0, this.values, size, values.length);
        size += values.length;
        return true;
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
    public long removeAt(int index) {
        checkIndex(index);
        long old = values[index];
        if (index != size - 1) {
            System.arraycopy(values, index + 1, values, index, size - (index + 1));
        }
        size--;
        return old;
    }

    @Override
    public Long remove(int index) {
        checkIndex(index);
        long old = values[index];
        if (index != size - 1) {
            System.arraycopy(values, index + 1, values, index, size - (index + 1));
        }
        size--;
        return old;
    }

    @Override
    public void forEach(LongConsumer c) {
        for (int i = 0; i < size; i++) {
            c.accept(values[i]);
        }
    }

    @Override
    public void forEachReversed(LongConsumer c) {
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
    public Long get(int index) {
        checkIndex(index);
        return values[index];
    }

    @Override
    public void forEach(Consumer<? super Long> action) {
        for (int i = 0; i < size; i++) {
            action.accept(values[i]);
        }
    }

    @Override
    public boolean addAll(int index, long... nue) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index out of range 0-" + size + ": " + index);
        } else if (nue.length == 0) {
            return false;
        } else if (isEmpty()) {
            if (index == 0) {
                System.out.println("0-add " + Arrays.toString(nue));
                addAll(nue);
                return true;
            } else {
                throw new IndexOutOfBoundsException("Add at " + index + " in empty list");
            }
        } else if (index == size) {
            return addAll(nue);
        }
        maybeGrow(size + nue.length + 1);
        System.arraycopy(values, index, values, index + nue.length, size - index);
        System.arraycopy(nue, 0, values, index, nue.length);
        size += nue.length;
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Long> c) {
        if (c.isEmpty()) {
            return false;
        }
        if (c.size() == 1) {
            add(index, c.iterator().next().longValue());
            return true;
        }
        if (isEmpty()) {
            return addAll(c);
        }
        long[] all = new long[c.size()];
        int i = 0;
        for (Iterator<? extends Long> it = c.iterator(); it.hasNext(); i++) {
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
    public int lastIndexOf(long value) {
        for (int j = size - 1; j >= 0; j--) {
            if (values[j] == value) {
                return j;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Long)) {
            return -1;
        }
        return lastIndexOf(((Long) o).longValue());
    }

    @Override
    public void add(int index, Long element) {
        add(index, element.longValue());
    }

    @Override
    public void add(int index, long element) {
        int sz = size();
        if (index == sz) {
            add(element);
            return;
        }
        if ((index < 0 || index > sz) && !(index == 0 && sz == 0)) {
            throw new IndexOutOfBoundsException("Index out of "
                    + "range - size " + size() + " but passed " + index);
        }
        maybeGrow(sz + 1);
        System.arraycopy(values, index, values, index + 1, (values.length - index) - 1);
        values[index] = element;
        size++;
    }

    @Override
    public long set(int index, long value) {
        checkIndex(index);
        long old = values[index];
        values[index] = value;
        return old;
    }

    @Override
    public Long set(int index, Long element) {
        return set(index, element.longValue());
    }

    @Override
    public boolean add(Long e) {
        add(e.longValue());
        return true;
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        return new Iter();
    }

    private class Iter implements PrimitiveIterator.OfLong {

        private int pos = -1;

        @Override
        public boolean hasNext() {
            return pos + 1 < size;
        }

        @Override
        public Long next() {
            return nextLong();
        }

        @Override
        public long nextLong() {
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
        outer:
        for (int i = 0; i < size; i++) {
            long l = values[i];
            hashCode = 31 * hashCode + Long.hashCode(l);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof LongListSimple) {
            LongListSimple other = (LongListSimple) o;
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
