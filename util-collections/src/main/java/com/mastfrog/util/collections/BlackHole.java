/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;

/**
 * Set/List implementation which silently discards all additions - for use with
 * APIs which take a set or a list and add to it, but whose contents will never
 * actually be used by the caller.
 *
 * @author Tim Boudreau
 */
abstract class BlackHole<T> {

    static final Set<Object> SET = new BlackHoleSet<Object>();
    static final List<Object> LIST = new BlackHoleList<Object>();

    static final class BlackHoleSet<T> extends BlackHole<T> implements Set<T> {

    }

    static final class BlackHoleList<T> extends BlackHole<T> implements List<T> {

    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(Object o) {
        return false;
    }

    public Iterator<T> iterator() {
        return Collections.emptyIterator();
    }

    public Object[] toArray() {
        return new Object[0];
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return (T[]) new Object[0];
    }

    public boolean add(T e) {
        // do nothing
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return false;
    }

    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public void clear() {
        // do nothing
    }

    public boolean addAll(int index, Collection<? extends T> c) {
        return false;
    }

    public T get(int index) {
        throw new NoSuchElementException();
    }

    public T set(int index, T element) {
        return null;
    }

    public void add(int index, T element) {
        // do nothing
    }

    public T remove(int index) {
        return null;
    }

    public int indexOf(Object o) {
        return -1;
    }

    public int lastIndexOf(Object o) {
        return -1;
    }

    public ListIterator<T> listIterator() {
        return Collections.<T>emptyList().listIterator();
    }

    public ListIterator<T> listIterator(int index) {
        return listIterator();
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return Collections.emptyList();
    }

    public Spliterator<T> spliterator() {
        return Collections.<T>emptySet().spliterator();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Collection<?>)) {
            return false;
        }
        return ((Collection<?>) ((Collection<?>) o)).isEmpty();
    }

    @Override
    public String toString() {
        return "[]";
    }
}
