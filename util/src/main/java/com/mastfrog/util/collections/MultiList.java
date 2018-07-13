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

import static com.mastfrog.util.Checks.notNull;
import static com.mastfrog.util.collections.CollectionUtils.transform;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Tim Boudreau
 */
final class MultiList<T> extends AbstractList<T> {

    private final List<List<T>> lists;

    MultiList(List<List<T>> lists) {
        int mx = lists.size();
        for (int i = 0; i < mx; i++) {
            if (lists.get(i) == null) {
                throw new IllegalArgumentException("List of lists contains a null at " + i);
            }
        }
        this.lists = notNull("lists", lists);
    }

    @SuppressWarnings("unchecked")
    MultiList(Collection<? extends Collection<T>> coll) {
        List<List<T>> l = new ArrayList<>(notNull("coll", coll).size());
        for (Collection<T> c : coll) {
            if (c.isEmpty()) {
                continue;
            }
            l.add(c instanceof List<?> ? (List<T>) c : new ArrayList<>(c));
        }
        this.lists = Collections.unmodifiableList(l);
    }

    @Override
    public T get(int index) {
        int total = lists.size();
        int offset = 0;
        for (int i = 0; i < total; i++) {
            List<T> l = lists.get(i);
            int sz = l.size();
            if (index >= offset && index < offset + sz) {
                return l.get(index - offset);
            }
            offset += l.size();
        }
        throw new IndexOutOfBoundsException("Bad index " + index + " of " + size());
    }

    @Override
    public int size() {
        int result = 0;
        for (List<T> l : lists) {
            result += l.size();
        }
        return result;
    }

    @Override
    public Iterator<T> iterator() {
        return CollectionUtils.combine(CollectionUtils.transform(lists, l -> l.iterator()));
//        return new MergeListIterator(transform(lists, l -> l.listIterator()));
    }

    @Override
    public ListIterator<T> listIterator() {
        return new MergeListIterator<>(transform(lists, l -> l.listIterator()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(A[] a) {
        int size = size();
        Class<A> type = (Class<A>) a.getClass().getComponentType();
        if (a.length != size) {
            a = (A[]) Array.newInstance(type, size);
        }
        int max = lists.size();
        if (max == 0) {
            return a;
        }
        int pos = 0;
        for (int i = 0; i < max; i++) {
            List<T> l = lists.get(i);
            int lmx = l.size();
            A[] temp = l.toArray((A[]) Array.newInstance(type, lmx));
            System.arraycopy(temp, 0, a, pos, temp.length);
            pos += temp.length;
        }
        return a;
    }

    @Override
    public boolean contains(Object o) {
        boolean result = false;
        for (List<?> l : lists) {
            if (l.contains(o)) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }

        Iterator<T> e1 = iterator();
        Iterator<?> e2 = ((List<?>) o).iterator();
        while (e1.hasNext() && e2.hasNext()) {
            T o1 = e1.next();
            Object o2 = e2.next();
            if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                return false;
            }
        }
        return !(e1.hasNext() || e2.hasNext());
    }
}
