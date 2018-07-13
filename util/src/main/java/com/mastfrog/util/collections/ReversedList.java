package com.mastfrog.util.collections;

import com.mastfrog.util.Checks;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Tim Boudreau
 */
final class ReversedList<T> implements List<T> {

    private final List<T> del;

    public ReversedList(List<T> del) {
        Checks.notNull("del", del);
        this.del = del;
    }

    List<T> delegate() {
        return del;
    }

    @Override
    public int size() {
        return del.size();
    }

    @Override
    public boolean isEmpty() {
        return del.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return del.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new ReversedIterator<>(del);
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size()]);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] ensureArraySize(T[] arr) {
        if (arr.length >= size()) {
            return arr;
        }
        return (T[]) Array.newInstance(arr.getClass().getComponentType(), size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] ts) {
        int sz = size();
        ts = ensureArraySize(ts);
        for (int i = 0; i < sz; i++) {
            ts[i] = (T) get(i);
        }
        return ts;
    }

    @Override
    public boolean add(T e) {
        del.add(0, e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        return del.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> clctn) {
        return del.containsAll(clctn);
    }

    @Override
    public boolean addAll(Collection<? extends T> clctn) {
        boolean result = true;
        for (T obj : clctn) {
            del.add(0, obj);
        }
        return result;
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> clctn) {
        return del.addAll(index(i), clctn);
    }

    @Override
    public boolean removeAll(Collection<?> clctn) {
        return del.removeAll(clctn);
    }

    @Override
    public boolean retainAll(Collection<?> clctn) {
        return del.retainAll(clctn);
    }

    @Override
    public void clear() {
        del.clear();
    }

    @Override
    public T get(int i) {
        return del.get(index(i));
    }

    @Override
    public T set(int i, T e) {
        return del.set(index(i), e);
    }

    @Override
    public void add(int i, T e) {
        del.add(index(i), e);
    }

    @Override
    public T remove(int i) {
        return del.remove(index(i));
    }

    @Override
    public int indexOf(Object o) {
        return index(del.lastIndexOf(o));
    }

    @Override
    public int lastIndexOf(Object o) {
        return index(del.indexOf(o));
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ReversedIterator<>(del);
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return new ReversedIterator<>(del, index(i) + 1);
    }

    @Override
    public List<T> subList(int i, int i1) {
        return new ReversedList<>(del.subList(i, i1));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReversedList && ((ReversedList) o).del.equals(del);
    }

    @Override
    public int hashCode() {
        return del.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (T o : this) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(o);
        }
        return sb.toString();
    }

    private int index(int i) {
        if (i < 0) {
            return i;
        }
        return size() - (i + 1);
    }

    private static class ReversedIterator<T> implements Iterator<T>, ListIterator<T> {

        private final List<T> l;
        private int ix;
        private final int size;

        public ReversedIterator(List<T> l) {
            this(l, l.size());
        }

        public ReversedIterator(List<T> l, int ix) {
            this.l = l;
            this.ix = ix;
            this.size = l.size();
        }
        
        private void check() {
            if (size != l.size()) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean hasNext() {
            return ix >= 1;
        }

        @Override
        public T next() {
            return l.get(--ix);
        }

        @Override
        public void remove() {
            l.remove(ix);
        }

        @Override
        public boolean hasPrevious() {
            check();
            return ix < size;
        }

        @Override
        public T previous() {
            check();
            return l.get(++ix);
        }

        @Override
        public int nextIndex() {
            return size - (ix + 1);
        }

        @Override
        public int previousIndex() {
            check();
            return size - (ix - 1);
        }

        @Override
        public void set(T e) {
            l.set(ix, e);
        }

        @Override
        public void add(T e) {
            l.add(0, e);
        }
    }
}
