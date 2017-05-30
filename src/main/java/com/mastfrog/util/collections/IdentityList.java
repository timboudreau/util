/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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

import com.mastfrog.util.Strings;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list which uses object identity, not equals() to determine if an object
 * is a member or not, so methods such as remove() will only match the same
 * object even if two objects which equals() the object to remove are available.
 * <p/>
 * IdentityLists own equals() and hashCode() methods are also identity checks.
 *
 * @author Tim Boudreau
 */
final class IdentityList<T> implements List<T> {

    private final List<Identity<T>> l = new ArrayList<>();

    public IdentityList() {
    }
    
    private IdentityList(boolean ignored, List<Identity<T>> l) {
        //ignored param just ensures that the erasure is not the same
        //as the method taking a collection
        this.l.addAll(l);
    }
    
    public IdentityList(Collection<? extends T> c) {
        for (T t : c) {
            add(t);
        }
    }

    @Override
    public int size() {
        return l.size();
    }

    @Override
    public boolean isEmpty() {
        return l.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        for (Identity<?> u : l) {
            if (u.obj == o) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
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
        if (a == null) {
            throw new NullPointerException();
        }
        if (a.length < size()) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
        }
        int sz = size();
        for (int i = 0; i < sz; i++) {
            a[i] = (T) l.get(i).get();
        }
        return a;
    }

    @Override
    public boolean add(T e) {
        return l.add(new Identity<>(e));
    }

    @Override
    public boolean remove(Object o) {
        for (Iterator<Identity<T>> it = l.iterator(); it.hasNext();) {
            T t = it.next().get();
            if (t == o) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean containsAll(Collection<?> c) {
        boolean result = !c.isEmpty();
        for (Object o : c) {
            result &= contains(o);
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        List<Identity<T>> ll = new ArrayList<>();
        for (T t : c) {
            ll.add(new Identity<>(t));
        }
        return l.addAll(ll);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        List<Identity<T>> ll = new ArrayList<>();
        for (T t : c) {
            ll.add(new Identity<>(t));
        }
        return l.addAll(index, ll);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        List<Identity<Object>> ll = new ArrayList<>();
        for (Object t : c) {
            ll.add(new Identity<>(t));
        }
        return l.removeAll(ll);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<Identity<Object>> ll = new ArrayList<>();
        for (Object t : c) {
            ll.add(new Identity<>(t));
        }
        return l.retainAll(ll);
    }

    @Override
    public void clear() {
        l.clear();
    }

    @Override
    public T get(int index) {
        return l.get(index).get();
    }

    @Override
    public T set(int index, T element) {
        Identity<T> u = new Identity<T>(element);
        Identity<T> old = l.set(index, u);
        return old == null ? null : old.get();
    }

    @Override
    public void add(int index, T element) {
        l.add(index, new Identity<T>(element));
    }

    @Override
    public T remove(int index) {
        Identity<T> result = l.remove(index);
        return result == null ? null : result.get();
    }

    @Override
    public int indexOf(Object o) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            if (o == get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int sz = size();
        for (int i = sz - 1; i >= 0; i--) {
            if (o == get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ConvertIterator<>(l.listIterator(), new C<T>());
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ConvertIterator<>(l.listIterator(index), new C<T>());
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new IdentityList<>(true, l.subList(fromIndex, toIndex));
    }
    
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return o == this;
    }
    
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    
    @Override
    public String toString() {
        return Strings.join(',', this);
    }

    private static final class Identity<T> {

        private final T obj;

        Identity(T obj) {
            this.obj = obj;
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            return o == obj || o == this;
        }

        @Override
        public int hashCode() {
            return obj == null ? System.identityHashCode(this) : System.identityHashCode(obj);
        }

        @Override
        public String toString() {
            return obj == null ? "null" : obj.toString();
        }

        public T get() {
            return obj;
        }
    }

    private static final class C<T> implements Converter<T, Identity<T>> {

        @Override
        public Identity<T> unconvert(T r) {
            return new Identity<T>(r);
        }

        @Override
        public T convert(Identity<T> t) {
            return t == null ? null : t.get();
        }
    }
}
