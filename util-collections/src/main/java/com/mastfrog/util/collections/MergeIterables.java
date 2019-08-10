package com.mastfrog.util.collections;

import com.mastfrog.util.strings.Strings;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Handles creating iterators that merge several iterables.
 *
 * @author Tim Boudreau
 */
final class MergeIterables<T> implements ConcatenatedIterables<T> {

    private final List<Iterable<T>> all = new LinkedList<>();

    MergeIterables(Iterable<T> a, Iterable<T> b) {
        all.add(a);
        all.add(b);
    }

    MergeIterables(Iterable<T> a, Iterable<T> b, Iterable<T> c) {
        all.add(a);
        all.add(b);
        all.add(c);
    }

    @SafeVarargs
    MergeIterables(Iterable<T>... iterables) {
        all.addAll(Arrays.asList(iterables));
    }

    MergeIterables(Iterable<Iterable<T>> all) {
        for (Iterable<T> iter : all) {
            this.all.add(iter);
        }
    }

    MergeIterables() {

    }

    @Override
    public void add(Iterable<T> iterable) {
        all.add(iterable);
    }

    @Override
    public Iterator<T> iterator() {
        if (all.isEmpty()) {
            return Collections.emptyIterator();
        } else if (all.size() == 1) {
            return all.iterator().next().iterator();
        }
        LinkedList<Iterator<T>> iterators = new LinkedList<>();
        for (Iterable<T> iterable : all) {
            iterators.add(iterable.iterator());
        }
        return new MergeIterator<>(iterators);
    }

    @Override
    public String toString() {
        return Strings.join(", ", this);
    }

    private static final class MergeIterator<T> implements Iterator<T> {

        private final LinkedList<Iterator<T>> iterators;

        MergeIterator(LinkedList<Iterator<T>> iterators) {
            this.iterators = iterators;
        }

        private Iterator<T> iter() {
            if (iterators.isEmpty()) {
                return null;
            }
            Iterator<T> result = iterators.get(0);
            if (!result.hasNext()) {
                iterators.remove(0);
                return iter();
            }
            return result;
        }

        @Override
        public boolean hasNext() {
            Iterator<T> curr = iter();
            return curr != null && curr.hasNext();
        }

        @Override
        public T next() {
            Iterator<T> iter = iter();
            if (iter == null) {
                throw new NoSuchElementException();
            }
            return iter.next();
        }

        @Override
        public void remove() {
            Iterator<T> iter = iter();
            if (iter == null) {
                throw new NoSuchElementException();
            }
            iter.remove();
        }
    }
}
