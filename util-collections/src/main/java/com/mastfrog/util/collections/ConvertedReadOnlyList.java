/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * A simple read only view of a list processed through a conversion function.
 *
 * @author Tim Boudreau
 */
final class ConvertedReadOnlyList<T, R> implements List<R> {

    private final Function<? super T, ? extends R> converter;
    private final List<? extends T> list;

    public ConvertedReadOnlyList(Function<? super T, ? extends R> converter, List<? extends T> list) {
        this.converter = converter;
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        for (int i = 0; i < size(); i++) {
            if (Objects.equals(o, get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<R> iterator() {
        return new CVIT(list.iterator());
    }

    final class CVIT implements Iterator<R> {

        private final Iterator<? extends T> it;

        public CVIT(Iterator<? extends T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public R next() {
            return converter.apply(it.next());
        }
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = get(i);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size()) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
        }
        for (int i = 0; i < size(); i++) {
            a[i] = (T) get(i);
        }
        return a;
    }

    @Override
    public boolean add(R e) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends R> c) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public boolean addAll(int index, Collection<? extends R> c) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public R get(int index) {
        return converter.apply(list.get(index));
    }

    @Override
    public R set(int index, R element) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void add(int index, R element) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public R remove(int index) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size(); i++) {
            if (Objects.equals(get(i), o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            if (Objects.equals(o, get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<R> listIterator() {
        return new CVLI(list.listIterator());
    }

    @Override
    public ListIterator<R> listIterator(int index) {
        return new CVLI(list.listIterator(index));
    }

    @Override
    public List<R> subList(int fromIndex, int toIndex) {
        return new ConvertedReadOnlyList<>(converter, list.subList(fromIndex, toIndex));
    }

    @Override
    public String toString() {
        Iterator<R> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            R value = it.next();
            sb.append(value == this ? "(this Collection)" : value);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }

        ListIterator<R> e1 = listIterator();
        ListIterator<?> e2 = ((List<?>) o).listIterator();
        while (e1.hasNext() && e2.hasNext()) {
            R o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                return false;
            }
        }
        return !(e1.hasNext() || e2.hasNext());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (R e : this) {
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    class CVLI implements ListIterator<R> {

        private final ListIterator<? extends T> it;

        public CVLI(ListIterator<? extends T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public R next() {
            return converter.apply(it.next());
        }

        @Override
        public boolean hasPrevious() {
            return it.hasPrevious();
        }

        @Override
        public R previous() {
            return converter.apply(it.previous());
        }

        @Override
        public int nextIndex() {
            return it.nextIndex();
        }

        @Override
        public int previousIndex() {
            return it.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only");
        }

        @Override
        public void set(R e) {
            throw new UnsupportedOperationException("Read only");
        }

        @Override
        public void add(R e) {
            throw new UnsupportedOperationException("Read only");
        }
    }
}
