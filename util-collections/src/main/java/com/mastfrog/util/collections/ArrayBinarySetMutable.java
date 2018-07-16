/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import static com.mastfrog.util.preconditions.Checks.notNull;
import com.mastfrog.util.strings.Strings;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A lighter-weight set which uses a comparator to establish membership.
 *
 *
 * @author Tim Boudreau
 */
class ArrayBinarySetMutable<T> extends AbstractSet<T> {

    private final boolean comparatorEquality;

    final Comparator<? super T> comp;
    T[] objs;
    private boolean needSort = true;
    int end = -1;
    int modCount;

    ArrayBinarySetMutable(boolean comparatorEquality, Comparator<? super T> comp, Class<T> type) {
        this(comparatorEquality, comp, 20, type);
    }

    @SuppressWarnings("unchecked")
    ArrayBinarySetMutable(boolean comparatorEquality, Comparator<? super T> comp, int initialCapacity, Class<T> type) {
        this.comp = comp;
        this.objs = (T[]) Array.newInstance(type, initialCapacity);
        this.comparatorEquality = comparatorEquality;
    }

    @SafeVarargs
    ArrayBinarySetMutable(boolean check, boolean comparatorEquality, Comparator<? super T> comp, T... objs) {
        this.comparatorEquality = comparatorEquality;
        this.comp = comp;
        this.objs = check ? ArrayUtils.dedup(objs) : objs;
        Arrays.sort(this.objs, comp);
        end = objs.length - 1;
    }

    @SuppressWarnings("unchecked")
    ArrayBinarySetMutable(Collection<T> set, Class<T> type, Comparator<? super T> comp) {
        this(false, true, comp, set.toArray((T[]) Array.newInstance(type, set.size())));
    }

    ArrayBinarySetMutable(Class<T> type, Comparator<? super T> comp) {
        this(true, comp, INCREMENT, type);
    }

    ArrayBinarySetMutable(ArrayBinarySet<T> set) {
        this.comparatorEquality = set.comparatorEquality;
        this.objs = ArrayUtils.copyOf(set.objs);
        this.comp = set.comp;
        this.end = set.size() - 1;
        this.needSort = false;
    }

    ArrayBinarySetMutable(ArrayBinarySetMutable<T> set) {
        this.comparatorEquality = set.comparatorEquality;
        this.objs = ArrayUtils.copyOf(set.objs);
        this.comp = set.comp;
        this.end = set.end;
        this.needSort = set.needSort;
    }

    static <T extends Comparable<T>> ArrayBinarySetMutable<T> create(Class<T> type) {
        Comparator<T> comp = new CollectionUtils.ComparableComparator<>();
        return new ArrayBinarySetMutable<>(type, comp);
    }

    static <T extends Comparable<T>> ArrayBinarySetMutable<T> create(Class<T> type, Collection<T> orig) {
        Comparator<T> comp = new CollectionUtils.ComparableComparator<>();
        return new ArrayBinarySetMutable<>(orig, type, comp);
    }

    void checkSort() {
        if (needSort) {
            reSort();
            needSort = false;
        }
    }

    void reSort() {
        if (end > 0) {
            Arrays.sort(objs, 0, end + 1, comp);
        }
        needSort = false;
    }

    private void needSort() {
        modCount++;
        needSort = true;
    }

    @Override
    public Object[] toArray() {
        return (Object[]) objs.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length == objs.length) {
            System.arraycopy(objs, 0, a, 0, objs.length);
            return a;
        }
        a = (T[]) Array.newInstance(a.getClass().getComponentType(), objs.length);
        System.arraycopy(objs, 0, a, 0, objs.length);
        return a;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i <= end; i++) {
            action.accept(objs[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        if (o == null || end == -1) {
            return false;
        }
        if (objs.getClass().getComponentType().isInstance(o)) {
            if (end == 0) {
                return comparatorEquality ? comp.compare((T) o, objs[0]) == 0 : Objects.equals(o, objs[0]);
            }
            checkSort();
            return binaryComparatorSearch((T) o);
        }
        return false;
    }

    private boolean binaryComparatorSearch(T o) {
        if (end < 0) {
            return false;
        } else if (end == 0) {
            return comparatorEquality ? comp.compare(o, objs[0]) == 0 : Objects.equals(o, objs[0]);
        }
        int ix = ArrayBinarySet.binaryComparatorSearch(comparatorEquality, o, objs, 0, this.end, comp);
        return ix >= 0;
    }

    @SuppressWarnings("unchecked")
    int indexOf(Object key) {
        if (objs.getClass().getComponentType().isInstance(key)) {
            return ArrayBinarySet.binaryComparatorSearch(comparatorEquality, (T) key, objs, 0, this.end, comp);
        }
        return -1;
    }

    @Override
    public Iterator<T> iterator() {
        checkSort();
        return new It();
    }

    @Override
    public int size() {
        return end + 1;
    }

    @Override
    public int hashCode() {
        checkSort();
        int h = 0;
        for (int i = 0; i < end + 1; i++) {
            h += objs[i].hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= end; i++) {
            sb.append(objs[i]);
            if (i != end) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        if (o instanceof Collection<?>) {
            Collection<?> c = (Collection<?>) o;
            if (c.size() == size()) {
                return containsAll(c);
            }
        }
        return false;
    }

    @Override
    public void clear() {
        end = -1;
        modCount++;
        Arrays.fill(objs, null);
    }

    private static final int INCREMENT = 10;

    private void checkSize(int needed) {
        if (objs.length < needed) {
            int newSize = ((needed % INCREMENT) + 1) * INCREMENT;
            grow(newSize);
        }
    }

    @SuppressWarnings("unchecked")
    void grow(int newSize) {
        T[] nue = (T[]) Array.newInstance(objs.getClass().getComponentType(), newSize);
        System.arraycopy(objs, 0, nue, 0, objs.length);
        objs = nue;
    }

    boolean greaterThanEnd(T obj) {
        if (end == -1) {
            return true;
        }
        T test = objs[end];
        int comparison = comp.compare(obj, test);
        if (comparison > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean add(T e) {
        if (e == null) {
            throw new NullPointerException("Nulls not allowed");
        }
        if (greaterThanEnd(e)) {
            end++;
            checkSize(end + 1);
            objs[end] = e;
            return true;
        }
        if (contains(e)) {
            return false;
        }
        needSort();
        end++;
        checkSize(end + 1);
        objs[end] = e;
        return true;
    }

    void rangeRemoved(int index, int size, int origEnd) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        if (o == null || end == -1 || !objs.getClass().getComponentType().isInstance(o)) {
            return false;
        }
        if (end == 0) {
            boolean eq = comparatorEquality ? comp.compare(objs[0], (T) o) == 0 : Objects.equals(o, objs[0]);
            if (eq) {
                clear();
                return true;
            } else {
                return false;
            }
        }
        checkSort();
        int index = ArrayBinarySet.binaryComparatorSearch(comparatorEquality, (T) o, objs, 0, this.end, comp);
        if (index < 0) {
            return false;
        }
        modCount++;
        System.arraycopy(objs, index + 1, objs, index, (end - index));
        rangeRemoved(index, 1, end);
        end--;
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(Collection<?> c) {
        if (notNull("c", c).isEmpty()) {
            return false;
        }
        return removeIf((o) -> c.contains(o));
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        boolean changed = false;
        int rangeStart = -1;
        int rangeLength = 0;
        boolean anyKeep = false;
        int origEnd = end;
        for (int i = end; i >= 0; i--) {
            boolean shouldRemove = filter.test(objs[i]);
            if (shouldRemove) {
                if (!changed && !anyKeep) {
                    end--;
                } else {
                    if (rangeStart == -1) {
                        rangeStart = i;
                        rangeLength = 1;
                    } else {
                        rangeStart--;
                        rangeLength++;
                    }
                }
            }
            if (!shouldRemove || i == 0) {
                if (rangeStart >= 0) {
                    int rangeEnd = rangeStart + rangeLength;
                    int dest = shouldRemove ? i : i + 1;
                    int num = (end - rangeEnd) + 1;
                    System.arraycopy(objs, rangeStart + rangeLength, objs, dest, num);
                    rangeRemoved(rangeStart, rangeLength, end);
                    end -= rangeLength;
                    rangeStart = -1;
                    rangeLength = 0;
                    changed = true;
                    modCount++;
                }
            }
            anyKeep |= !shouldRemove;
        }
        return changed || end != origEnd;
    }

    private boolean eq(T a, T b) {
        return ((a == null) != (b == null)) ? false : comparatorEquality ? comp.compare(a, b) == 0 : Objects.equals(a, b);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(Collection<? extends T> c) {
        notNull("c", c);
        if (c.isEmpty()) {
            return false;
        }
        T[] nue = c.toArray((T[]) Array.newInstance(objs.getClass().getComponentType(), c.size()));
        if (nue.length == 1) {
            return add(nue[0]);
        }
        Arrays.sort(nue);
        boolean anyAdded = false;
        int rangeStart = -1;
        int rangeLength = 0;
        int origEnd = end;
        T last = null;
        if (end == -1) {
            for (int i = 0; i < nue.length; i++) {
                T o = nue[i];
                if (eq(last, o)) {
                    continue;
                }
                objs[++end] = o;
                last = o;
            }
            needSort();
            return true;
        }
        checkSort();
        for (int i = 0; i < nue.length; i++) {
            T o = nue[i];
            if (o == null) {
                throw new NullPointerException("Null in " + Strings.join(',', nue));
            }
            int origIndex = origEnd == 0 ? -1 : ArrayBinarySet.binaryComparatorSearch(comparatorEquality, o, objs, 0, origEnd, comp);
            boolean alreadyAdded = eq(last, o);
            boolean skip = alreadyAdded || origIndex != -1;
            if (skip || i == nue.length - 1) {
                if (!skip) {
                    rangeLength++;
                    if (rangeStart == -1) {
                        rangeStart = i;
                    }
                }
                if (rangeLength > 0 && rangeStart != -1) {
                    checkSize(end + rangeLength + 1);
                    if (rangeLength == 1) {
                        objs[end + 1] = nue[rangeStart];
                    } else {
//                        System.out.("COPY RANGE " + rangeStart + " of " + rangeLength + " starting at " + (end + 1) + ": "
//                                + Strings.join(',', ArrayUtils.extract(nue, rangeStart, rangeLength)));
                        System.arraycopy(nue, rangeStart, objs, end + 1, rangeLength);
                    }
                    end += rangeLength;
                    rangeStart = -1;
                    rangeLength = 0;
                    anyAdded = true;
                }
            } else {
                rangeLength++;
                if (rangeStart == -1) {
                    rangeStart = i;
                }
            }
            last = o;
        }
        if (anyAdded) {
            needSort();
        }
        return anyAdded;
    }

    @Override
    public boolean isEmpty() {
        return end < 0;
    }

    void shiftLeft(int to, int from, int rangeLength) {
        T[] ext = ArrayUtils.extract(objs, from, rangeLength);
        System.out.println("shift left " + from + " (" + objs[from] + ") to " + to + " rangeLength " + rangeLength + " clobbering " + objs[to] + " sliding " + Strings.join(',', ext));
        System.arraycopy(objs, from, objs, to, rangeLength);
        rangeRemoved(from, from - to, end);
        end -= from - to;
        assert noDuplicates();
        System.out.println("I AM NOW " + this);
    }

    private boolean noDuplicates() {
        Set<T> s = new HashSet<>();
        for (T key : this) {
            if (s.contains(key)) {
                throw new AssertionError("Duplicate key " + key + ": " + this);
            }
            s.add(key);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        notNull("c", c);
        boolean wasEmpty = end < 0;
        if (c.isEmpty()) {
            clear();
            return !wasEmpty;
        }
        boolean changed = false;
        int rangeEnd = -1;
        boolean hasRetained = false;
        for (int i = end; i >= 0; i--) {
            T o = objs[i];
            boolean retain = c.contains(o);
            if (!retain) {
                if (rangeEnd == -1) {
                    rangeEnd = i;
                }
                if (i == 0) {
                    if (!changed) {
                        end = -1;
                        break;
                    }
                    shiftLeft(i, rangeEnd + 1, end - rangeEnd);
                    changed = true;
                }
            } else if (rangeEnd != -1) {
                if (!changed && !hasRetained) {
                    end = i;
                } else {
                    shiftLeft(i + 1, rangeEnd + 1, (end - rangeEnd) + 1);
                }
                rangeEnd = -1;
                changed = true;
            }
            hasRetained |= retain;
        }
        return changed;
    }

    class It implements Iterator<T> {

        private int ix = -1;
        private int mc = modCount;

        private void checkModification() {
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean hasNext() {
            return ix < end;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            for (int i = ix + 1; i <= end; i++) {
                action.accept(objs[i]);
            }
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IndexOutOfBoundsException("" + ix);
            }
            checkModification();
            return objs[++ix];
        }

        public String toString() {
            return "It over " + ArrayBinarySetMutable.this + " at " + ix + " hasNext()? " + hasNext();
        }

        @Override
        public void remove() {
            checkModification();
            System.arraycopy(objs, ix + 1, objs, ix, end - ix);
            rangeRemoved(ix, 1, end);
            mc = ++modCount;
            objs[end] = null;
            end--;
            ix--;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        super.containsAll(c);
        if (c.isEmpty()) {
            return false;
        }
        if (isEmpty()) {
            return false;
        }
        if (size() < c.size()) {
            return false;
        }
        boolean result = true;
        for (Object o : c) {
            result = contains(o);
            if (!result) {
                break;
            }
        }
        return result;
    }
}
