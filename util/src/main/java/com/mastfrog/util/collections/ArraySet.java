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

import com.mastfrog.util.Strings;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A lighter-weight set class for small sets, where the cost of iterating an
 * array is preferable to the larger footprint of a HashSet. Use for constants
 * of a few items that are reused for the life of a class. Immutable.
 *
 * @author Tim Boudreau
 */
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

    public String toString() {
        return Strings.join(',', objs).toString();
    }
}
