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
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Objects;

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

    public int size() {
        return ip.size();
    }

    public T first() {
        return indexed.forIndex(ip.first());
    }

    public T last() {
        return indexed.forIndex(ip.last());
    }

    public T get(int i) {
        int value = ip.get(i);
        return indexed.forIndex(value);
    }

    public boolean containsPath(ObjectPath<T> other) {
        return ip.contains(other.ip);
    }

    public boolean contains(T obj) {
        int ix = indexOf(obj);
        return ix >= 0;
    }

    public int indexOf(T obj) {
        int ix = indexed.indexOf(obj);
        return ix < 0 ? -1 : ip.indexOf(ix);
    }

    public boolean isEmpty() {
        return ip.isEmpty();
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
