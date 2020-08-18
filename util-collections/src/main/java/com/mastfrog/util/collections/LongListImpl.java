
package com.mastfrog.util.collections;

import com.mastfrog.util.collections.ArraysManager.SortChecker;
import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.search.Bias;
import com.mastfrog.util.sort.Sort;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.UnaryOperator;

/**
 * Dynamic array of longs without requiring boxing, implemented as a list of
 * internal long arrays.
 *
 * @author Tim Boudreau
 */
final class LongListImpl extends AbstractList<Long> implements LongList {

    private final ArraysManager.Longs arrays;
    private int size = 0;
    private boolean sorted = true;

    private LongListImpl(LongListImpl copy) {
        this.arrays = new ArraysManager.Longs(copy.arrays);
        this.sorted = copy.sorted;
        this.size = copy.size;
    }

    LongListImpl() {
        this(64); // default to 512 byte arrays
    }

    LongListImpl(ArraysManager.Longs arrays) {
        this.arrays = arrays;
    }

    LongListImpl(int batchSize) {
        this(new ArraysManager.Longs(batchSize));
    }

    LongListImpl(int batchSize, int initialPoolSize, int maxPoolSize) {
        this(new ArraysManager.Longs(batchSize, maxPoolSize == 0
                ? ArraysPool.uncachedPool(batchSize)
                : ArraysPool.cachingPool(batchSize, initialPoolSize, maxPoolSize)));
    }

    LongListImpl(long[] longs) {
        this(longs, Math.max(64, longs.length));
    }

    LongListImpl(long[] longs, int batchSize) {
        this(longs, new boolean[1], batchSize);
    }

    LongListImpl(long[] longs, boolean[] sortVal, int batchSize) {
        this(longs, (sorted) -> sortVal[0] = sorted, batchSize);
        this.sorted = sortVal[0];
    }

    LongListImpl(long[] longs, SortChecker checker, int batchSize) {
        this(new ArraysManager.Longs(batchSize, checker, new long[][]{longs}));
        size = longs.length;
    }

    LongListImpl(long[][] longs, int batchSize) {
        this(longs, new boolean[1], batchSize);
    }

    LongListImpl(long[][] longs, boolean[] sortVal, int batchSize) {
        this(longs, (sorted) -> sortVal[0] = sorted, batchSize);
        this.sorted = sortVal[0];
    }

    LongListImpl(long[][] longs, SortChecker checker, int batchSize) {
        this(new ArraysManager.Longs(batchSize, checker, longs));
        size = longs.length;
    }

    LongListImpl(List<long[]> nue, int size, int batchSize, boolean sorted) {
        this(listToArray(nue), new boolean[1], batchSize);
        this.size = size;
        this.sorted = sorted;
    }

    @Override
    public LongListImpl copy() {
        return new LongListImpl(this);
    }

    static long[][] listToArray(List<long[]> l) {
        long[][] result = new long[l.size()][];
        for (int i = 0; i < l.size(); i++) {
            result[i] = l.get(i);
        }
        return result;
    }

    @Override
    public boolean add(Long e) {
        return add(e.longValue());
    }

    @Override
    public boolean add(long l) {
        if (size > 0 && sorted) {
            sorted = l > getAsLong(size - 1);
        }
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        long[] arr = arrayForIndex(size, true);
        arr[size % arrays.batchSize()] = l;
        size++;
        return true;
    }

    @Override
    public Long set(int index, Long element) {
        return set(index, element.longValue());
    }

    @Override
    public long[] toLongArray() {
        return arrays.toLongArray(size);
    }

    @Override
    public void add(int index, Long element) {
        add(index, element.longValue());
    }

    @Override
    public boolean addAll(Collection<? extends Long> c) {
        if (c.isEmpty()) {
            return false;
        }
        boolean first = true;
        long last = isEmpty() ? Long.MIN_VALUE : last();
        for (Long l : c) {
            if (l == null) {
                throw new IllegalArgumentException("Null element in array");
            }
            long val = l;
            if (val <= last) {
                sorted = false;
            }
            add(val);
            if (first) {
                first = false;
            }
            last = val;
        }
        return !c.isEmpty();
    }

    @Override
    public boolean addAll(long[] longs) {
        if (notNull("longs", longs).length == 0) {
            return false;
        }
        int batchSize = arrays.batchSize();
        int pos = size % batchSize;
        int cursor = 0;
        if (sorted) {
            sorted = arrays.isSorted(longs);
            if (sorted && size > 0) {
                long val = getAsLong(size - 1);
                if (val >= longs[0]) {
                    sorted = false;
                }
            }
        }
        while (cursor < longs.length) {
            long[] arr = arrayForIndex(size, true);
            int count = Math.min(longs.length - cursor, batchSize - pos);
            System.arraycopy(longs, cursor, arr, pos, count);
            pos = 0;
            cursor += count;
            size += count;
        }
        return true;
    }

    int currentCapacity() {
        return arrays.size() * arrays.batchSize();
    }

    private void clobberWithCollection(int index, Collection<? extends Long> c) {
        if (c instanceof LongListImpl) {
            LongListImpl ll = (LongListImpl) c;
            for (int i = 0; i < ll.size; i++) {
                set(index + i, ll.getAsLong(i));
            }
            return;
        }
        Iterator<? extends Long> it = c.iterator();
        int ix = index;
        long prev = ix == 0 ? Long.MIN_VALUE : getAsLong(ix - 1);
        long l;
        boolean first = true;
        while (it.hasNext()) {
            l = it.next();
            if (!first && prev >= l) {
                sorted = false;
            }
            set(ix++, l);
            prev = l;
            first = false;
        }
        if (ix < size && getAsLong(ix) <= prev) {
            sorted = false;
        }
    }

    @Override
    public boolean addAll(int index, long[] c) {
        if (notNull("c", c).length == 0) {
            return false;
        }
        if (index > size) {
            throw new IllegalArgumentException("Cannot insert at " + index + " in list of " + size);
        }
        if (index == size) {
            return addAll(c);
        }
        int batchSize = arrays.batchSize();
        int firstArray = index / batchSize;
        int offset = index % batchSize;
        if (offset % batchSize == 0 && c.length % batchSize == 0) {
            // Update the sorted state
            if (sorted) {
                boolean arrayIsSorted = arrays.isSorted(c);
                if (!arrayIsSorted) {
                    sorted = false;
                } else {
                    boolean hasPrevious = index > 0;
                    if (hasPrevious) {
                        long prev = getAsLong(index - 1);
                        if (prev >= c[0]) {
                            sorted = false;
                        }
                    }
                    if (sorted) {
                        boolean hasNext = index < size;
                        if (hasNext) {
                            long next = getAsLong(index);
                            if (c[c.length - 1] >= next) {
                                sorted = false;
                            }
                        }
                    }
                }
            }
            // If we will insert a series of complete arrays,
            // batch them with System.arraycopy
            for (int i = 0; i < c.length; i += batchSize) {
                long[] batch = arrays.addOne(firstArray);
                arrays.copy(c, 0, batch, i * batchSize, batchSize);
                firstArray++;
            }
            size += c.length;
            return true;
        }
        int count = c.length;
        arrays.shiftOneArrayRight(firstArray, offset, count);
        size += count;
        long last = Long.MIN_VALUE;
        for (int i = 0; i < c.length; i++) {
            long l = c[i];
            if (index >= arrays.capacity()) {
                arrays.addOne();
            }
            if (i > 0 && sorted && last >= l) {
                sorted = false;
            }
            set(index++, l);
            last = l;
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Long> c) {
        if (notNull("c", c).isEmpty()) {
            return false;
        }
        if (index > size) {
            throw new IllegalArgumentException("Cannot insert at " + index + " in list of " + size);
        }
        if (index == size) {
            return addAll(c);
        }
        int batchSize = arrays.batchSize();
        int firstArray = index / batchSize;
        int offset = index % batchSize;
        if (offset % batchSize == 0 && c.size() % batchSize == 0) {
            Iterator<? extends Long> iter = c.iterator();
            long last = size > 0 && index > 0 ? getAsLong(index - 1) : Long.MIN_VALUE;
            while (iter.hasNext()) {
                long[] batch = arrays.addOne(firstArray);
                firstArray++;
                for (int i = 0; i < batch.length; i++) {
                    Long val = iter.next();
                    if (val == null) {
                        throw new IllegalArgumentException("Collection contains "
                                + "null at " + i + ": " + c);
                    }
                    long value = val;
                    batch[i] = value;
                    if (last <= value) {
                        sorted = false;
                    }
                }
            }
            size += c.size();
            return true;
        }
        int count = c.size();
        arrays.shiftOneArrayRight(firstArray, offset, count);
        size += count;
        clobberWithCollection(index, c);
        return true;
    }

    @Override
    public void add(int index, long element) {
        if (index == size) {
            add(element);
            return;
        } else if (index > size) {
            throw new IndexOutOfBoundsException("Cannot add at " + index + " - size is " + size);
        } else if (index < 0) {
            throw new IndexOutOfBoundsException("Negative index " + index);
        }
        int firstArray = index / arrays.batchSize();
        if (size + 1 / arrays.batchSize() > arrays.size()) {
            arrays.addOne();
        }
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        long[] destArray = arrays.shiftOneArrayRight(firstArray, index % arrays.batchSize(), 1);
        destArray[index % arrays.batchSize()] = element;
        long preceding = Long.MIN_VALUE;
        long next = Long.MAX_VALUE;
        size++;
        if (sorted) {
            boolean hasPreceding = index > 0;
            boolean hasSubsequent = index < size - 1;
            if (hasPreceding) {
                preceding = getAsLong(index - 1);
            }
            if (hasSubsequent) {
                next = getAsLong(index + 1);
            }
            if ((next <= element && hasSubsequent) || (preceding >= element && hasPreceding)) {
                sorted = false;
            }
        }
        set(index, element);
    }

    @Override
    public Long remove(int index) {
        return removeAt(index);
    }

    @Override
    public void clear() {
        arrays.clear();
        size = 0;
        sorted = true;
    }

    @Override
    public void removeRange(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("Negative from index " + fromIndex);
        } else if (toIndex < 0) {
            throw new IndexOutOfBoundsException("Negative to index " + fromIndex);
        } else if (toIndex > size) {
            throw new IndexOutOfBoundsException("To index is past end: " + toIndex);
        } else if (fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("From index < toIndex " + fromIndex + " is < " + toIndex);
        } else if (fromIndex == toIndex) {
        } else if (toIndex == size) {
            size = fromIndex;
            arrays.pruneTo(size);
        } else if (fromIndex + 1 == toIndex) {
            remove(fromIndex);
        } else if (fromIndex == 0 && toIndex == size) {
            clear();
        } else {
            arrays.removeRange(fromIndex, toIndex);
            size -= (toIndex - fromIndex);
            arrays.pruneTo(size);
        }
    }

    @Override
    public long removeAt(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Invalid index " + index + " in list of " + size);
        }
        if (index == size - 1) {
            long val = getAsLong(size - 1);
            size--;
            arrays.pruneTo(size);
            if (size == 0) {
                sorted = true;
            }
            return val;
        }
        long val = getAsLong(index);
        arrays.removeAt(index);
        size--;
        arrays.pruneTo(size);
        if (size == 0) {
            sorted = true;
        }
        return val;
    }

    @Override
    public int removeValue(long val) {
        int ix = indexOf(val);
        if (ix < 0) {
            return -1;
        }
        removeAt(ix);
        return ix;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Long) {
            return removeValue((Long) o) >= 0;
        }
        return false;
    }

    @Override
    public boolean isSorted() {
        return sorted;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean sort() {
        if (!sorted) {
            Sort.multiSort(toLongArray(), 0, size, (ixa, ixb) -> {
                swapIndices(ixa, ixb);
            });
//            Collections.sort(this);
            Longerator lngs = longerator();
            boolean first = true;
            long last = Long.MIN_VALUE;
            sorted = true;
            while (lngs.hasNext()) {
                long val = lngs.next();
                if (!first) {
                    if (val == last) {
                        sorted = false;
                        break;
                    }
                }
                last = val;
                first = false;
            }
        }
        return sorted;
    }

    @Override
    public boolean removeLast() {
        if (size > 0) {
            size--;
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Longerator longerator() {
        return new Longerator() {
            int ix = -1;

            @Override
            public long next() {
                return LongListImpl.this.getAsLong(++ix);
            }

            @Override
            public boolean hasNext() {
                return ix + 1 < size;
            }
        };
    }

    @Override
    public long set(int index, long element) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("No such element " + index);
        }
        @SuppressWarnings("MismatchedReadAndWriteOfArray")
        long[] arr = arrayForIndex(index, false);
        int offset = index % arrays.batchSize();
        arr[offset] = element;
        long preceding = index == 0 ? -1 : getAsLong(index - 1);
        long subsequent = index != size - 1 ? getAsLong(index + 1) : -1;
        if (preceding != -1 && preceding >= element) {
            sorted = false;
        }
        if (subsequent != -1 && subsequent <= element) {
            sorted = false;
        }
        return 0;
    }

    @Override
    public int indexOfArray(long[] val) {
        if (val.length == 0) {
            return 0;
        } else if (val.length > size) {
            return -1;
        } else if (val.length == 1) {
            return indexOf(val[0]);
        } else if (sorted && !arrays.isSorted(val)) {
            return -1;
        }
        int first = indexOf(val[0]);
        if (first == -1) {
            return -1;
        } else if ((size - 1) - first < val.length) {
            return -1;
        }
        for (int i = first + 1; i < first + val.length; i++) {
            if (val[i - first] != getAsLong(i)) {
                break;
            }
            if (i == first + val.length - 1) {
                return first;
            }
        }
        int i, j;
        loop:
        for (i = first + 1; i < (size - 1) - val.length; i++) {
            for (j = 0; j < val.length; j++) {
                if (val[j] != getAsLong(i + j)) {
                    continue loop;
                }
                if (j == val.length - 1) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int indexOf(long test) {
        if (size == 0 || arrays.isEmpty()) {
            return -1;
        }
        int batchSize = arrays.batchSize();
        int ct = Math.min(arrays.size(), (size / batchSize) + 1);
        if (sorted) {
            if (size > 0) {
                if (getAsLong(0) > test || getAsLong(size - 1) < test) {
                    return -1;
                }
            }
            //xx could use binary search of the array head and tail to iterate
            // fewer arrays
            for (int i = 0; i < ct; i++) {
                if (i >= arrays.size()) {
                    throw new IllegalArgumentException("Bad array index " + i + " of " + ct
                            + " with arrays size " + arrays.size() + ", arrays.batchSize() " + batchSize + ", size "
                            + size + " ct computed to " + ct + " size/arrays.batchSize() " + (size / batchSize));
                }
                long[] a = arrays.get(i);
                long first = a[0];
                if (first > test) {
                    return -1;
                }
                int lastIndex;
                if (i == ct - 1) {
                    if (size > batchSize) {
                        lastIndex = (size % batchSize) - 1;
                        if (lastIndex == -1) { // size is divisible by arrays.batchSize()
                            lastIndex = batchSize - 1;
                        }
                    } else {
                        lastIndex = size - 1;
                    }
                } else {
                    lastIndex = a.length - 1;
                }
                assert lastIndex >= 0 : "Bad last index on array " + i + " of " + ct
                        + " size " + size + " arrays.batchSize() " + batchSize + " array should be "
                        + (i * batchSize) + " thru " + ((i + 1) * batchSize) + " got lastIndex " + lastIndex;
                long last = a[lastIndex];

                assert last > first : "Last <= first: " + first + " -> " + last + " in "
                        + ct + " lastIndex " + lastIndex + " size " + a.length + " arr " + Arrays.toString(a);

                if (test == first) {
                    return i * batchSize;
                } else if (test == last) {
                    return (i * batchSize) + lastIndex;
                } else if (test > first && test < last) {
                    int localIndex = Arrays.binarySearch(a, 0, lastIndex, test);
                    if (localIndex >= 0) {
                        return (i * batchSize) + localIndex;
                    } else {
                        return -1;
                    }
                }
            }
        } else {
            for (int i = 0; i < ct; i++) {
                long[] a = arrays.get(i);
                for (int j = 0; j < a.length; j++) {
                    int offset = (i * batchSize) + j;
                    if (offset > size) {
                        break;
                    }
                    long check = a[j];
                    if (check == test) {
                        return (i * batchSize) + j;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public int indexOfPresumingSorted(long value) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            return value == get(0) ? 0 : -1;
        }
        return indexOfPresumingSorted(value, 0, arrays.size() - 1);
    }

    private int indexOfPresumingSorted(long value, int startArray, int stopArray) {
        long[] start = arrays.get(startArray);
        int startOffset = startArray * arrays.batchSize();
        int lastElementInStartArray = size - startOffset;
        if (lastElementInStartArray < 0) {
            return -1;
        }
//        System.out.println("Search for " + value + " in array " + startArray + " thru " + stopArray + " last element " + lastElementInStartArray
//                + " offset " + startOffset);
//        System.out.println("  Array " + Arrays.toString(Arrays.copyOf(start, lastElementInStartArray))
//                + " ");
        if (startArray == stopArray) {
            int lastIx = Math.min(start.length - 1, lastElementInStartArray - 1);
            if (start.length == 0 || value < start[0] || value > start[lastIx]) {
                System.out.println("  Bail 1 last " + lastIx + " / " + start.length);
                return -1;
            }
            if (lastElementInStartArray == start.length) {
                int localResult = Arrays.binarySearch(start, value);
//                System.out.println("  Bail 2 " + localResult + " last " + lastIx + " / " + start.length);
                return localResult < 0 ? -1 : startOffset + localResult;
            } else {
                int localResult = Arrays.binarySearch(start, 0, lastElementInStartArray, value);
//                System.out.println("  Bail 3 " + localResult + " last " + lastIx + " / " + start.length);
                return localResult < 0 ? -1 : startOffset + localResult;
            }
        }
        if (value == start[0]) {
            return startOffset;
        } else if (value > start[0]) {
            int lastIx = Math.min(lastElementInStartArray, start.length - 1);
            if (start[lastIx] == value) {
                return startOffset + lastIx;
            } else if (value < start[lastIx]) {
                if (lastElementInStartArray == start.length) {
                    int localResult = Arrays.binarySearch(start, value);
                    return localResult < 0 ? -1 : startOffset + localResult;
                } else {
                    int localResult = Arrays.binarySearch(start, 0, lastElementInStartArray, value);
                    return localResult < 0 ? -1 : startOffset + localResult;
                }
            }
        } else {
            // value less than start of array?  done
            return -1;
        }
        int stopOffset = stopArray * arrays.batchSize();
        int lastElementInStopArray = size - stopOffset;
        // in case we have shrunk
        while (lastElementInStopArray < 0 && stopArray > startArray) {
            stopArray--;
            stopOffset = stopArray * arrays.batchSize();
            lastElementInStopArray = size - stopOffset;
        }
        if (stopArray == startArray) {
            return -1;
        }
        long[] end = arrays.get(stopArray);
        if (value > end[lastElementInStopArray]) {
            return -1;
        } else if (value == end[0]) {
            return stopOffset + lastElementInStopArray;
        } else if (value > end[0]) {
            if (lastElementInStopArray == end.length) {
                int localResult = Arrays.binarySearch(end, value);
                return localResult < 0 ? -1 : localResult + stopOffset;
            } else {
                int localResult = Arrays.binarySearch(end, 0, lastElementInStopArray, value);
                return localResult < 0 ? -1 : startOffset + localResult;
            }
        }
        if (stopArray == startArray + 1) {
            return -1;
        }
        int mid = startArray + (stopArray - startArray / 2);
        return indexOfPresumingSorted(value, mid, stopArray - 1);
    }

    @Override
    public int nearestIndexToPresumingSorted(long value, Bias bias) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            long val = getAsLong(0);
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
                    while (result < size - 1 && getAsLong(result + 1) == getAsLong(result)) {
                        result++;
                    }
                }
                return result;
            case FORWARD:
            case BACKWARD:
                int res2 = nearestIndexToPresumingSorted(0, size - 1, bias, value);
                if (res2 != -1) {
                    while (res2 < size - 1 && getAsLong(res2 + 1) == getAsLong(res2)) {
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
                    long fwdDiff = Math.abs(getAsLong(fwd) - value);
                    long bwdDiff = Math.abs(getAsLong(bwd) - value);
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

        // XXX rewrite this to narrow down the target array and just search that
        // - will be faster
        if (start == end) {
            long currentVal = getAsLong(start);
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
        long startVal = getAsLong(start);
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
        long endVal = getAsLong(end);
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
        long midVal = getAsLong(mid);
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
            long nextStartValue = getAsLong(newStart);
            if (nextStartValue > value && bias == Bias.BACKWARD && (newEnd - newStart <= 1 || midVal < value)) {
                return mid;
            }
            long nextEndValue = getAsLong(newEnd);
            if (nextEndValue < value && bias == Bias.FORWARD && newEnd - newStart <= 1) {
                return end;
            }
            return nearestIndexToPresumingSorted(newStart, newEnd, bias, value);
        } else if (midVal > value && startVal < value) {
            int nextEnd = mid - 1;
            int nextStart = start + 1;
            long nextEndValue = getAsLong(nextEnd);
            if (nextEndValue < value && bias == Bias.FORWARD && nextEnd - nextStart <= 1) {
                return mid;
            }
            long newStartValue = getAsLong(nextStart);
            if (bias == Bias.BACKWARD && newStartValue > value && (startVal < value || nextEnd - nextStart <= 1)) {
                return start;
            }
            return nearestIndexToPresumingSorted(nextStart, nextEnd, bias, value);
        }
        return -1;
    }

    @Override
    public int indexOf(Object o) {
        if (o instanceof Number) {
            long test = ((Number) o).longValue();
            return indexOf(test);
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Long) {
            long test = ((Number) o).longValue();
            return contains(test);
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(long test) {
        return indexOf(test) >= 0;
    }

    private long[] arrayForIndex(int index, boolean grow) {
        int arrayIndex = index / arrays.batchSize();
        long ct = arrays.size();
        long[] result = null;
        if (ct < arrayIndex + 1) {
            if (!grow) {
                return null;
            } else {
                while (arrays.size() < arrayIndex + 1) {
                    result = arrays.addOne();
                }
            }
        } else {
            return arrays.get(arrayIndex);
        }
        return result;
    }

    @Override
    public Long get(int index) {
        return getAsLong(index);
    }

    @Override
    public long getAsLong(int index) {
        long[] arr = arrayForIndex(index, false);
        if (arr == null) {
            throw new IndexOutOfBoundsException("No array for index " + index
                    + " in " + arrays.size() + " with batch size "
                    + arrays.batchSize() + ": " + this);
        }
        return arr[index % arrays.batchSize()];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        outer:
        for (int i = 0; i < arrays.size(); i++) {
            long[] els = arrays.get(i);
            for (int j = 0; j < els.length; j++) {
                int offset = (i * arrays.batchSize()) + j;
                if (offset == size) {
                    break outer;
                }
                long l = els[j];
                hashCode = 31 * hashCode + Long.hashCode(l);
            }
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder((size * 5) + 2).append('[');
        outer:
        for (int i = 0; i < arrays.size(); i++) {
            long[] els = arrays.get(i);
            for (int j = 0; j < els.length; j++) {
                int offset = (i * arrays.batchSize()) + j;
                if (offset == size) {
                    break outer;
                }
                long l = els[j];
                if (i != 0 || j != 0) {
                    sb.append(',');
                }
                sb.append(l);
            }
        }
        return sb.append(']').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof LongListImpl) {
            LongListImpl ll = (LongListImpl) o;
            if (ll.size() != size) {
                return false;
            }
            if (ll.arrays.batchSize() == arrays.batchSize()) {
                int ix = 0;
                Iterator<long[]> a = arrays.iterator();
                Iterator<long[]> b = ll.arrays.iterator();
                while (a.hasNext()) {
                    if (ix + arrays.batchSize() > size) {
                        long[] aa = a.next();
                        long[] bb = b.next();
                        int max = size % arrays.batchSize();
                        for (int i = 0; i < max; i++) {
                            if (aa[i] != bb[i]) {
                                return false;
                            }
                        }
                    } else {
                        if (!Arrays.equals(a.next(), b.next())) {
                            return false;
                        }
                    }
                    ix += arrays.batchSize();
                    if (ix > size) {
                        break;
                    }
                }
                return true;
            } else {
                for (int i = 0; i < size; i++) {
                    long a = getAsLong(i);
                    long b = ll.getAsLong(i);
                    if (a != b) {
                        return false;
                    }
                }
                return true;
            }
        } else if (o instanceof List<?>) {
            List<?> ll = (List<?>) o;
            if (ll.size() != size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                Object item = ll.get(i);
                if (!(item instanceof Long)) {
                    return false;
                }
                Long l = (Long) item;
                if (getAsLong(i) != l.longValue()) {
                    return false;
                }
            }
            /*
            outer:
            for (int i = 0; i < arrays.size(); i++) {
                long[] els = arrays.get(i);
                for (int j = 0; j < els.length; j++) {
                    int offset = (i * arrays.batchSize()) + j;
                    if (offset == size) {
                        break;
                    }
                    long value = els[j];
                    Object item = ll.get(offset);
                    if (item instanceof Long) {
                        Long l = (Long) item;
                        if (value != l.longValue()) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
             */
            return true;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == this) {
            return true;
        }
        for (Object o : c) {
            if (o instanceof Long) {
                long val = ((Long) o);
                if (indexOf(val) < 0) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public void forEach(Consumer<? super Long> action) {
        forEach((LongConsumer) action::accept);
    }

    @Override
    public LongListImpl subList(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return new LongListImpl();
        }
        if (fromIndex == 0 && toIndex == size) {
            return new LongListImpl(this);
        }
        if (fromIndex % arrays.batchSize() == 0) {
            int firstArray = fromIndex / arrays.batchSize();
            List<long[]> nue = new ArrayList<>();
            for (int i = firstArray; i < arrays.size(); i++) {
                long[] curr = arrays.get(i);
                nue.add(Arrays.copyOf(curr, curr.length));
                if (i * arrays.batchSize() > toIndex - fromIndex) {
                    break;
                }
            }
            int newSize = toIndex - fromIndex;
            return new LongListImpl(nue, newSize, arrays.batchSize(), sorted);
        } else {
            LongListImpl nue = new LongListImpl(arrays.batchSize());
            for (int i = fromIndex; i < toIndex; i++) {
                nue.add(getAsLong(i));
            }
            return nue;
        }
    }

    @Override
    public void forEach(LongConsumer consumer) {
        for (int i = arrays.size() - 1; i >= 0; i--) {
            long[] ll = arrays.get(i);
            int base = i * arrays.batchSize();
            for (int j = ll.length - 1; j >= 0; j--) {
                int offset = i * base + j;
                if (offset < size) {
                    long curr = ll[j];
                    consumer.accept(curr);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public void replaceAll(UnaryOperator<Long> operator) {
        for (int i = arrays.size() - 1; i >= 0; i--) {
            long[] ll = arrays.get(i);
            int base = i * arrays.batchSize();
            for (int j = ll.length - 1; j >= 0; j--) {
                int offset = i * base + j;
                if (offset < size) {
                    long curr = ll[j];
                    ll[j] = operator.apply(curr);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    public PrimitiveIterator.OfLong iterator() {
        return new PI();
    }

    final class PI implements PrimitiveIterator.OfLong {

        int ix = -1;

        @Override
        public boolean hasNext() {
            return ix < size - 1;
        }

        @Override
        public long nextLong() {
            return getAsLong(++ix);
        }

    }

    @Override
    public int lastIndexOf(long val) {
        if (sorted) {
            return indexOf(val);
        } else {
            for (int i = arrays.size() - 1; i >= 0; i--) {
                long[] ll = arrays.get(i);
                for (int j = ll.length - 1; j >= 0; j--) {
                    int offset = i * arrays.batchSize() + j;
                    if (offset < size) {
                        long curr = ll[j];
                        if (val == curr) {
                            return offset;
                        }
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Long)) {
            return -1;
        }
        long val = ((Long) o);
        return lastIndexOf(val);
    }
}
