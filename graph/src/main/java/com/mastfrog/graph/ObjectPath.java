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
package com.mastfrog.graph;

import com.mastfrog.abstractions.list.IndexedResolvable;
import com.mastfrog.bits.collections.BitSetSet;
import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * One path in an object graph.
 *
 * @author Tim Boudreau
 */
public final class ObjectPath<T> implements Iterable<T>, Comparable<ObjectPath> {

    private final IntPath ip;
    private final IndexedResolvable<? extends T> indexed;

    ObjectPath(IntPath ip, IndexedResolvable<? extends T> indexed) {
        this.ip = ip;
        this.indexed = indexed;
    }

    /**
     * Create a new path prepending the passed object.
     *
     * @param obj Another object, which must actually be contained in the
     * original graph this path came from, or null will be returned
     * @return A new path or null
     */
    public ObjectPath prepending(T obj) {
        int ix = indexed.indexOf(obj);
        if (ix < 0) {
            return null;
        }
        return new ObjectPath<>(ip.prepending(ix), indexed);
    }

    /**
     * Create a new path appending the passed object.
     *
     * @param obj Another object, which must actually be contained in the
     * original graph this path came from, or null will be returned
     * @return A new path or null
     */
    public ObjectPath appending(T obj) {
        int ix = indexed.indexOf(obj);
        if (ix < 0) {
            return null;
        }
        return new ObjectPath<>(ip.appending(ix), indexed);
    }

    /**
     * Get all of the contained objects as a set.
     *
     * @return A set
     */
    public Set<? extends T> contents() {
        BitSet set = new BitSet(ip.size());
        ip.forEachInt(set::set);
        return new BitSetSet<>(indexed, set);
    }

    /**
     * Get the number of elements in this path.
     *
     * @return The size
     */
    public int size() {
        return ip.size();
    }

    /**
     * Returns the first element.
     *
     * @return The first element
     * @throws NoSuchElementException if the path is empty
     */
    public T first() {
        if (ip.isEmpty()) {
            throw new NoSuchElementException("empty path");
        }
        return indexed.forIndex(ip.first());
    }

    /**
     * Returns the last element.
     *
     * @return The last element
     * @throws NoSuchElementException if the path is empty
     */
    public T last() {
        if (ip.isEmpty()) {
            throw new NoSuchElementException("empty path");
        }
        return indexed.forIndex(ip.last());
    }

    /**
     * Get the path element at index i.
     *
     * @param i An index <code>&gt;=0</code> and <code>&lt size()</code>
     * @return A path element
     */
    public T get(int i) {
        int value = ip.get(i);
        return indexed.forIndex(value);
    }

    /**
     * Determine if this path contains the sequence of elements in another path.
     *
     * @param other Another path
     * @return true if the passed path is equal to or a subpath of this one
     */
    public boolean containsPath(ObjectPath<T> other) {
        return ip.contains(other.ip);
    }

    /**
     * Determine if this path contains the passed item.
     *
     * @param obj
     * @return
     */
    public boolean contains(T obj) {
        int ix = indexOf(obj);
        return ix >= 0;
    }

    /**
     * Get the (first) index of an item in this path.
     *
     * @param obj An object
     * @return the index or -1 if not present
     */
    public int indexOf(T obj) {
        int ix = indexed.indexOf(obj);
        return ix < 0 ? -1 : ip.indexOf(ix);
    }

    /**
     * Get the (first) index of an item in this path.
     *
     * @param obj An object
     * @return the index or -1 if not present
     */
    public int lastIndexOf(T obj) {
        int ix = indexed.indexOf(obj);
        return ix < 0 ? -1 : ip.lastIndexOf(ix);
    }

    /**
     * Get a copy of this path, lopping off the first element.
     *
     * @return A new path
     */
    public ObjectPath<T> childPath() {
        if (isEmpty()) {
            return this;
        }
        return new ObjectPath<>(ip.childPath(), indexed);
    }

    /**
     * Get a copy of this path, lopping off the last element.
     *
     * @return A new path
     */
    public ObjectPath<T> parentPath() {
        if (isEmpty()) {
            return this;
        }
        return new ObjectPath<>(ip.childPath(), indexed);
    }

    /**
     * Determine if this path is empty.
     *
     * @return True if the path contains no elements
     */
    public boolean isEmpty() {
        return ip.isEmpty();
    }

    /**
     * Determine if this path does not represent an actual path - if it has one
     * element or less.
     *
     * @return True if this path does not have an endpoint and may or may not
     * have a start point
     */
    public boolean isNotAPath() {
        return ip.isNotAPath();
    }

    /**
     * Create a new path whose elements are this one's in reverse order.
     *
     * @return A new path
     */
    public ObjectPath<T> reversed() {
        return new ObjectPath<>(ip.reversed(), indexed);
    }

    public T start() {
        if (ip.isEmpty()) {
            return null;
        }
        return indexed.forIndex(ip.get(0));
    }

    public T end() {
        if (ip.isEmpty()) {
            return null;
        }
        return indexed.forIndex(ip.get(size() - 1));
    }

    @Override
    public int hashCode() {
        return ip.hashCode();
    }

    private String toString(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof Class<?>) {
            Class<?> c = (Class<?>) o;
            if (c.isArray()) {
                return c.getComponentType().getSimpleName() + "[]";
            } else {
                return c.getSimpleName();
            }
        } else if (o.getClass().isArray()) {
            StringBuilder sb = new StringBuilder(60);
            int max = Array.getLength(o);
            for (int i = 0; i < max; i++) {
                Object item = Array.get(o, i);
                sb.append(Objects.toString(item));
            }
            return sb.append(']').toString();
        }
        return o.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(60);
        for (int i = 0; i < size(); i++) {
            T obj = get(i);
            if (sb.length() != 0) {
                sb.append('/');
            }
            sb.append(toString(obj));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof ObjectPath<?>) {
            ObjectPath<?> other = (ObjectPath<?>) o;
            return other.ip.equals(ip) && other.indexed.toList().equals(indexed.toList());
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    @Override
    public int compareTo(ObjectPath o) {
        return ip.compareTo(o.ip);
    }

    final class Iter implements Iterator<T> {

        int ix = -1;

        @Override
        public boolean hasNext() {
            return ix + 1 < size();
        }

        @Override
        public T next() {
            return get(++ix);
        }

        @Override
        public String toString() {
            return "Iter(" + ObjectPath.this + " @ " + ix + ")";
        }
    }
}
