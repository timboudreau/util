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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * A lighter-weight set which uses a comparator to establish membership.
 *
 *
 * @author Tim Boudreau
 */
final class ArrayBinarySet<T> extends AbstractSet<T> {

    private final boolean comparatorEquality;

    private final Comparator<? super T> comp;
    private final T[] objs;

    @SafeVarargs
    ArrayBinarySet(boolean check, boolean comparatorEquality, Comparator<? super T> comp, T... objs) {
        this.comparatorEquality = comparatorEquality;
        this.comp = comp;
        this.objs = check ? ArrayUtils.dedup(objs) : objs;
        Arrays.sort(this.objs, comp);
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
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        if (o == null || objs.length == 0) {
            return false;
        }
        if (objs.getClass().getComponentType().isInstance(o)) {
            if (comparatorEquality) {
                return binaryComparatorSearch((T) o);
            } else {
                return Arrays.binarySearch(objs, (T) o, comp) >= 0;
            }
        }
        return false;
    }

    private boolean binaryComparatorSearch(T o) {
        int start = 0;
        int end = objs.length - 1;
        return binaryComparatorSearch(o, start, end);
    }

    private boolean binaryComparatorSearch(T o, int start, int end) {
        if (start == end) {
            return false;
        }
        int startCompare = compareAt(o, start);
        if (startCompare == 0) {
            return true;
        }
        if (startCompare < 0) {
            return false;
        }
        int endCompare = compareAt(o, end);
        if (endCompare == 0) {
            return true;
        }
        if (endCompare > 0) {
            return false;
        }
        int amt = ((end - start) + 1) / 2;
        if (binaryComparatorSearch(o, start + amt, end)) {
            return true;
        } else if (binaryComparatorSearch(o, start, end - amt)) {
            return true;
        }
        return false;
    }

    private int compareAt(T o, int position) {
        return comp.compare(o, objs[position]);
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
    public String toString() {
        return Strings.join(',', objs).toString();
    }
}
