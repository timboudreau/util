package com.mastfrog.util.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A list which can only ever contain a single item
 *
 * @author Tim Boudreau
 */
final class SingleItemList<T> implements List<T> {
    private T item;
    
    SingleItemList() {
        
    }
    
    SingleItemList(T item) {
        this.item = item;
    }
    
    @Override
    public int size() {
        return item == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return item == null;
    }

    @Override
    public boolean contains(Object o) {
        return Objects.equals(o, item);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private boolean used;
            @Override
            public boolean hasNext() {
                return item != null && !used;
            }

            @Override
            public void remove() {
                if (used) {
                    item = null;
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }
            
            @Override
            public T next() {
                if (item == null) {
                    throw new IndexOutOfBoundsException();
                }
                used = true;
                return item;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return new Object[] { item };
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a == null || a.length != 1) {
            T[] result = (T[]) Array.newInstance(item.getClass(), 1);
            result[0] = (T) item;
            return result;
        } else {
            a[0] = (T) item;
            return a;
        }
    }

    @Override
    public boolean add(T e) {
        if (item != null) {
            throw new IndexOutOfBoundsException("One item supported");
        }
        item = e;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (Objects.equals(item, o)) {
            item = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.size() == 1 && Objects.equals(item, c.iterator().next());
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            return add(t);
        }
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (item != null && c.contains(item)) {
            item = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        if (item != null && c.contains(item)) {
            return true;
        }
        item = null;
        return false;
    }

    @Override
    public void clear() {
        item = null;
    }

    @Override
    public T get(int index) {
        if (index == 0 && item != null) {
            return item;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public T set(int index, T element) {
        if (index > 0) {
            throw new IndexOutOfBoundsException(index + " out of range");
        }
        T old = item;
        if (index == 0) {
            item = element;
        }
        return old;
    }

    @Override
    public void add(int index, T element) {
        add(element);
    }

    @Override
    public T remove(int index) {
        if (index == 0 && item != null) {
            T old = item;
            item = null;
            return old;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int indexOf(Object o) {
        return Objects.equals(o, item) ? 0 : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o) == 0 ? 0 : -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return Arrays.<T>asList(item).listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return Arrays.<T>asList(item).listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return Arrays.<T>asList(item).subList(fromIndex, toIndex);
    }
    
    @Override
    public int hashCode() {
        return Arrays.<T>asList(item).hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof List) {
            List<?> l = (List<?>) o;
            if (l.isEmpty() && isEmpty()) {
                return true;
            }
            int sz = size();
            if (l.size() != sz) {
                return false;
            }
            return Objects.equals(get(0), l.get(0));
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString()).append("[");
        if (item != null) {
            sb.append(item);
        }
        sb.append(']');
        return sb.toString();
    }
}
