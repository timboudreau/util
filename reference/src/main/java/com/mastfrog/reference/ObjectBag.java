/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.reference;

import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.newSetFromMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;

/**
 * A bag of objects, which can be added to and removed from locklessly, and
 * maintains an internal collection of atomic linked list partitions and prefers
 * adding to the least populated, in order to reduce overhead.
 * <p>
 * This is NOT a general-purpose collection - it's entire purpose is to have a
 * way to use atomics to hold a set of objects locklessly and with minimal
 * overhead on add and remove; it needs to be sized so that each partition never
 * grows very large.
 * </p><p>
 * The idea is to pay nearly nothing for adds, at a slight cost for removes,
 * which we amortize with memory by having a bunch of separate atomic slots.
 * </p>
 *
 * @author Tim Boudreau
 */
final class ObjectBag<T> {

    private static final int DEFAULT_PARTITIONS = 64;
    private final AtomicReferenceArray<Cell<T>> cells;

    ObjectBag(int partitions) {
        cells = new AtomicReferenceArray<>(greaterThanZero("partitions", partitions));
    }

    ObjectBag() {
        this(DEFAULT_PARTITIONS);
    }

    public static <T> ObjectBag<T> create() {
        return new ObjectBag<>();
    }

    public static <T> ObjectBag<T> create(int partitions) {
        return new ObjectBag<>(partitions);
    }

    /**
     * Take a snapshot of the contents at present.
     *
     * @return A set
     */
    public Set<T> snapshot() { // primarily for tests
        Set<T> result = newSetFromMap(new IdentityHashMap<>(64));
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            Cell<T> c = cells.get(i);
            if (c != null) {
                c.collectInto(result);
            }
        }
        return result;
    }

    /**
     * Determine if this set is empty.
     *
     * @return true if its empty
     */
    public boolean isEmpty() {
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            Cell<T> c = cells.get(i);
            if (c != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Empty this bag, returning its contents; the return set is an identity
     * hash set, so two objects which equals() each other will still be separate
     * entries.
     *
     * @return A set of items
     */
    public Set<T> drain() {
        Set<T> result = newSetFromMap(new IdentityHashMap<>(64));
        while (!isEmpty()) {
            List<Cell<T>> allCells = new ArrayList<>();
            int ct = cells.length();
            for (int i = 0; i < ct; i++) {
                Cell<T> old = cells.getAndSet(i, null);
                if (old != null) {
                    allCells.add(old);
                }
            }
            for (Cell<T> c : allCells) {
                c.collectInto(result);
            }
        }
        return result;
    }

    /**
     * Indiscriminatly empty this bag.
     */
    public void clear() {
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            cells.getAndSet(i, null);
        }
    }

    /**
     * Determine if the passed object is present in this bag.
     *
     * @param obj An object
     * @return true if it is present
     */
    public boolean contains(T obj) {
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            Cell<T> c = cells.get(i);
            if (c != null && c.contains(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the number of objects this bag believes it contains at present.
     * Note that the value may count duplicates of an object if it was added
     * more than once.
     *
     * @return An approximate count
     */
    public int size() {
        int ct = cells.length();
        int result = 0;
        for (int i = 0; i < ct; i++) {
            Cell<T> c = cells.get(i);
            if (c != null) {
                result += c.size();
            }
        }
        return result;
    }

    @Override
    public String toString() {
        int ct = cells.length();
        StringBuilder sb = new StringBuilder("AtomicObjectBag(" + ct + ":");
        for (int i = 0; i < ct; i++) {
            Cell<T> c = cells.get(i);
            sb.append("\n ").append(i).append(". ").append(c);
        }
        return sb.append(')').toString();
    }

    /**
     * Bulk-reeove all objects that match the passed predicate, in as few
     * mutations as possible. Note that if another thread is adding the same
     * object as it is being removed, whether or not the object will still be
     * present after both calls exit is luck-of-the-draw.
     *
     * @param test A test
     */
    public void removing(Predicate<T> test) {
        int ct = cells.length();
        // Intentionally traverse in reverse order for bulk removals, so we are
        // less likely to chase our tail
        for (int i = ct - 1; i >= 0; i--) {
            int ix = i;
            cells.getAndUpdate(i, old -> {
                if (old != null) {
                    Cell<T> nue = old.removing(test);
                    return nue;
                }
                return old;
            });
        }
    }

    public T removeOne() {
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            Cell<T> cell = cells.getAndUpdate(i, old -> {
                if (old != null) {
                    return old.next;
                }
                return old;
            });
            if (cell != null) {
                // ensure no duplicates
                remove(cell.obj);
                return cell.obj;
            }
        }
        return null;
    }

    private int leastPopulatedCell() {
        int ct = cells.length();
        int best = -1;
        int curr = Integer.MAX_VALUE;
        for (int i = 0; i < ct; i++) {
            int sz = cellSize(i);
            if (sz == 0) {
                curr = i;
                best = i;
                break;
            }
            if (sz < curr) {
                best = i;
                curr = sz;
            }
        }
        return best;
    }

    private int cellSize(int index) {
        Cell<T> c = cells.get(index);
        return c == null ? 0 : c.size();
    }

    /**
     * Add all members of a collection.
     *
     * @param all
     */
    public int adding(Collection<? extends T> all) {
        // We add all elements to a single cell to avoid overhead
        if (all.isEmpty()) {
            return -1;
        }
        Iterator<? extends T> iter = all.iterator();
        Cell<T> target = null;
        while (iter.hasNext()) {
            T obj = iter.next();
            if (target == null) {
                target = new Cell<>(obj);
            } else {
                target = new Cell<>(target, obj);
            }
        }
        if (target != null) {
            Cell<T> ft = target;
            int ix = leastPopulatedCell();
            cells.getAndUpdate(ix, old -> {
                if (old == null) {
                    return ft;
                }
                return old.appending(ft);
            });
            return ix;
        }
        return -1;
    }

    /**
     * Add an element - no checking whether the item is already present is
     * preformed.
     *
     * @param obj An object
     * @return An internal hint about the slot it was added to
     */
    public int add(T obj) {
        int best = leastPopulatedCell();
        cells.getAndUpdate(best, old -> {
            if (old == null) {
                return new Cell<>(obj);
            }
            return old.adding(obj);
        });
        return best;
    }

    /**
     * Remove all occurrences of an object in this bag.
     *
     * @param obj An object
     */
    public void remove(T obj) {
        notNull("obj", obj);
        int ct = cells.length();
        for (int i = 0; i < ct; i++) {
            cells.getAndUpdate(i, old -> {
                if (old != null) {
                    return old.removing(obj);
                }
                return old;
            });
        }
    }

    /**
     * A little immutable singly-linked list, which adds or removes objects by
     * copying the list.
     *
     * @param <T> The type
     */
    private static final class Cell<T> {

        private final T obj;
        private final Cell<T> next;

        Cell(T obj) {
            this.obj = obj;
            this.next = null;
        }

        Cell(Cell<T> next, T obj) {
            this.obj = obj;
            this.next = next;
        }

        void collectInto(Set<? super T> set) {
            set.add(obj);
            if (next != null) {
                next.collectInto(set);
            }
        }

        Cell<T> removing(Predicate<? super T> test) {
            Cell<T> head = this;
            if (test.test(this.obj)) {
                if (next != null) {
                    return next.removing(test);
                }
                return null;
            }
            if (next != null) {
                Cell<T> newNext = next.removing(test);
                if (newNext != next) {
                    return new Cell<>(newNext, this.obj);
                }
            }
            return this;
        }

        Cell<T> removing(T obj) {
            // Note we don't assume there is only one cell that contains
            // the requested object - we are duplicate-tolerant
            Cell<T> head = this;
            if (obj == this.obj) {
                if (next != null) {
                    return next.removing(obj);
                }
                return null;
            }
            if (next != null) {
                Cell<T> newNext = next.removing(obj);
                if (newNext != next) {
                    return new Cell<>(newNext, this.obj);
                }
            }
            return this;
        }

        Cell<T> adding(T obj) {
            T o = this.obj;
            if (obj == o) {
                return this;
            }
            Cell<T> n = next;
            if (n == null) {
                return new Cell<>(new Cell<>(obj), o);
            } else {
                return new Cell<>(n.adding(obj), o);
            }
        }

        Cell<T> appending(Cell<T> nue) {
            if (next == null) {
                return new Cell<>(nue, this.obj);
            }
            return next.appending(nue);
        }

        boolean contains(T obj) {
            if (obj == this.obj) {
                return true;
            }
            Cell<T> nx = next;
            if (nx != null) {
                return nx.contains(obj);
            }
            return false;
        }

        int size() {
            Cell<T> cell = this;
            int result = 0;
            while (cell != null) {
                result++;
                cell = cell.next;
            }
            return result;
        }

        @Override
        public String toString() {
            String result = obj.toString();
            if (next != null) {
                result += " -> " + next.toString();
            }
            return result;
        }
    }
}
