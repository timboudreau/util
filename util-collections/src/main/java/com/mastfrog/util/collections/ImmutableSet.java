/*
 * The MIT License
 *
 * Copyright 2020 Tim Boudreau.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 *
 * @author Tim Boudreau
 */
final class ImmutableSet<T> implements Set<T> {

    private final int[] hashes;
    private final Object[] objects;
    private final ToIntFunction<Object> hasher;
    private final int hashCode;
    private final int membershipMask;

    ImmutableSet(Collection<? extends T> of, boolean identity) {
        hasher = identity ? System::identityHashCode : Object::hashCode;
        IntMap<T> im = IntMap.create(Math.max(1, of.size()));
        int hc = 0;
        int mask = 0;
        for (T obj : of) {
            int key = hash(obj);
            T oldDup = im.put(key, obj);
            if (oldDup == null) {
                hc += key;
                mask = mask | key;
            } else if (!identity && oldDup != null && !oldDup.equals(obj)) {
                throw new IllegalArgumentException("Both " + oldDup + " and "
                        + obj + " have the hash code " + key + " but they are "
                        + "not the equals() each other");
            }
        }
        hashes = im.keysArray();
        objects = im.valuesArray();
        this.hashCode = hc;
        // Create a mask of set bits for every bit not used by any
        // hash code we encountered - gives us a fast non-membership test
        // for some cases
        membershipMask = ~mask;
    }

    ImmutableSet(boolean identity, T[] objs) {
        hasher = identity ? System::identityHashCode : Object::hashCode;
        IntMap<T> im = IntMap.create(Math.max(1, objs.length));
        int hc = 0;
        int mask = 0;
        for (T obj : objs) {
            int key = hash(obj);
            T oldDup = im.put(key, obj);
            if (oldDup == null) {
                hc += key;
                mask = mask | key;
            } else if (!identity && oldDup != null && !oldDup.equals(obj)) {
                throw new IllegalArgumentException("Both " + oldDup + " and "
                        + obj + " have the hash code " + key + " but they are "
                        + "not the equals() each other");
            }
        }
        hashes = im.keysArray();
        objects = im.valuesArray();
        this.hashCode = hc;
        // Create a mask of set bits for every bit not used by any
        // hash code we encountered - gives us a fast non-membership test
        // for some cases
        membershipMask = ~mask;
    }

    @SafeVarargs
    public static <T> Set<T> of(boolean identity, T... objs) {
        switch (objs.length) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(objs[0]);
            default:
                return new ImmutableSet<>(identity, objs);
        }
    }

    public static <T> Set<T> of(boolean identity, Collection<? extends T> objs) {
        switch (objs.size()) {
            case 0:
                return Collections.emptySet();
            case 1:
                return Collections.singleton(objs.iterator().next());
            default:
                return new ImmutableSet<>(objs, identity);
        }
    }

    @Override
    public int size() {
        return hashes.length;
    }

    @Override
    public boolean isEmpty() {
        return hashes.length == 0;
    }

    private int hash(Object o) {
        return o == null ? 0 : hasher.applyAsInt(o);
    }

    @Override
    public boolean contains(Object o) {
        if (hashes.length == 0) {
            return false;
        }
        int hash = hash(o);
        // A fast test whether the hash code contains any bits
        // that are not set in the hash code of any of the contents
        // This actually works better for types with a lousy implementation
        // of hashCode() that does not produce a broad distribution -
        // ... such as java.lang.String
        if ((membershipMask & hash) != 0) {
            return false;
        }
        int ix = Arrays.binarySearch(hashes, hash);
        if (ix >= 0) {
            return Objects.equals(objects[ix], o);
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return (Iterator<T>) CollectionUtils.toIterator(objects);
    }

    @Override
    public Object[] toArray() {
        return objects == null ? new Object[0] : Arrays.copyOf(objects, hashes.length);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length != hashes.length) {
            a = CollectionUtils.genericArray(
                    (Class<T>) a.getClass().getComponentType(), hashes.length);
        }
        if (hashes.length > 0) {
            System.arraycopy(objects, 0, a, 0, a.length);
        }
        return a;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c.size() > hashes.length && c instanceof Set<?>) {
            return false;
        }
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Set<?>)) {
            return false;
        } else if (o instanceof ImmutableSet<?>) {
            ImmutableSet<?> imm = (ImmutableSet<?>) o;
            if (hashes.length == 0) {
                return imm.hashes.length == 0;
            } else if (hashes.length != imm.hashes.length) {
                return false;
            }
            if (imm.hasher.equals(hasher)) {
                return Arrays.equals(objects, (((ImmutableSet<?>) o)).objects);
            }
        }
        Collection<?> c = (Collection<?>) o;
        if (c.size() != size()) {
            return false;
        }
        return containsAll(c);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        if (objects.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(2 + (objects.length * 32));
        sb.append('[');
        for (int i = 0;; i++) {
            Object e = objects[i];
            sb.append(e == this ? "(this Collection)" : e);
            if (i == objects.length - 1) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return new ArraySpliterator<>(objects);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i < objects.length; i++) {
            action.accept((T) objects[i]);
        }
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return toArray(generator.apply(size()));
    }


    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Immutable.");
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException("Immutable.");
    }
}
