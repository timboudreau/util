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

import com.mastfrog.util.strings.Strings;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * A lighter-weight set class for small sets, where the cost of iterating an
 * array is preferable to the larger footprint of a HashSet. Use for constants
 * of a few items that are reused for the life of a class, and should only be
 * constructed if the caller can guarantee the objects are not duplicate and
 * will not change their equality contract to become duplicates in their
 * lifetimes. Immutable.
 *
 * @author Tim Boudreau
 */
@SuppressWarnings("EqualsAndHashcode")
final class ArraySet<T> extends AbstractSet<T> {

    private final T[] objs;

    @SafeVarargs
    ArraySet(boolean check, T... objs) {
        this.objs = check ? ArrayUtils.dedup(objs) : objs;
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
        for (int i = 0; i < objs.length; i++) {
            action.accept(objs[i]);
        }
    }

    @Override
    public boolean contains(Object o) {
        for (int i = 0; i < objs.length; i++) {
            if (Objects.equals(o, objs[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return CollectionUtils.toIterator(objs);
    }

    @Override
    public int size() {
        return objs.length;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < objs.length; i++) {
            h += objs[i].hashCode();
        }
        return h;
    }

    @Override
    public boolean isEmpty() {
        return objs.length == 0;
    }

    @Override
    public String toString() {
        return '[' + Strings.join(',', (Object[]) objs).toString()
                + ']';
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException("Read-only");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Read-only");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Collection<?>)) {
            return false;
        }
        Collection<?> c = (Collection<?>) o;
        if (c.size() != objs.length) {
            return false;
        }
        return containsAll(c);
    }

    @Override
    public Spliterator<T> spliterator() {
        return new ArraySpliterator<>(objs);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return toArray(generator.apply(objs.length));
    }
}
