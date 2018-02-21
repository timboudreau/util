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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Tim Boudreau
 */
final class MergeListIterator<T> implements ListIterator<T> {

    private final ListIterator<ListIterator<T>> iterators;
    private ListIterator<T> curr;
    private int index;

    MergeListIterator(List<ListIterator<T>> iterators) {
        this(iterators.listIterator());
    }

    MergeListIterator(ListIterator<ListIterator<T>> iterators) {
        this.iterators = iterators;
        curr = iterators.hasNext() ? iterators.next() : Collections.emptyListIterator();
    }

    ListIterator<T> toNext() {
        ListIterator<?> last = curr;
        int ct = 0;
        while (curr != null && !curr.hasNext()) {
            curr = iterators.hasNext() ? iterators.next() : curr;
            if (ct == 0 && curr == last && iterators.hasNext()) {
                curr = iterators.next();
            }
            if (curr == last) {
                break;
            }
            last = curr;
            ct++;
        }
        return curr;
    }

    ListIterator<T> toPrev() {
        ListIterator<?> last = curr;
        int ct = 0;
        while (curr != null && !curr.hasPrevious()) {
            curr = iterators.hasPrevious() ? iterators.previous() : curr;
            if (ct == 0 && curr == last && iterators.hasPrevious()) {
                curr = iterators.previous();
            }
            if (curr == last) {
                break;
            }
            last = curr;
            ct++;
        }
        return curr;
    }

    private ListIterator<T> iter(boolean forward) {
        if (curr == null) {
            curr = toNext();
        }
        if (curr != null) {
            boolean usable = forward ? curr.hasNext() : curr.hasPrevious();
            if (!usable) {
                if (forward) {
                    curr = toNext();
                } else {
                    curr = toPrev();
                }
            }
        }
        return curr;
    }

    @Override
    public boolean hasNext() {
        ListIterator<T> it = iter(true);
        return it == null ? false : it.hasNext();
    }

    @Override
    public T next() {
        Iterator<T> iter = iter(true);
        if (iter == null) {
            throw new NoSuchElementException(index + " out of bounds");
        }
        index++;
        return iter.next();
    }

    @Override
    public void remove() {
        ListIterator<T> iter = curr == null ? iter(true) : curr;
        iter.remove();
    }

    @Override
    public boolean hasPrevious() {
        return iter(false).hasPrevious();
    }

    @Override
    public T previous() {
        ListIterator<T> iter = iter(false);
        if (iter == null) {
            throw new NoSuchElementException(index + " out of bounds");
        }
        index--;
        return iter.previous();
    }

    @Override
    public int nextIndex() {
        return index;
    }

    @Override
    public int previousIndex() {
        return index - 1;
    }

    @Override
    public void set(T e) {
        if (curr == null) {
            curr = iter(true);
        }
        curr.set(e);
    }

    @Override
    public void add(T e) {
        throw new UnsupportedOperationException("Not supported.");
    }
}
