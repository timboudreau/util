/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import com.mastfrog.util.collections.IntMap.EntryMover;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Allows very efficient bulk operations against a map; it is expected that once
 * you have begun calling methods on an instance, the map will <i>not</i> be
 * modified until the instance has been committed. If you are performing a large
 * number of add/remove/move/set operations, this class will coalesce those into
 * the minimal set of array modifications needed and perform all of them when
 * commit() is called.
 *
 * @author Tim Boudreau
 */
public final class IntMapModifier<T> {

    private final EntryMover<T> mover;
    private final ArrayIntMap<T> additions = new ArrayIntMap<>();
    private final ArrayIntMap<T> changes = new ArrayIntMap<>();
    private final IntSet removals = new IntSetImpl();
    private final IntMap<T> map;

    private IntMapModifier(IntMap<T> map) {
        this(map, new DefaultMover<>());
    }

    private IntMapModifier(IntMap<T> map, EntryMover<T> mover) {
        this.mover = mover;
        this.map = map;
    }

    public static <T> IntMapModifier<T> create(IntMap<T> forMap) {
        return new IntMapModifier<>(forMap);
    }

    public static <T> IntMapModifier<T> create(IntMap<T> forMap, EntryMover<T> mover) {
        return new IntMapModifier<>(forMap, mover);
    }

    public IntMapModifier<T> set(int key, T value) {
        int ix = map.indexOf(key);
        if (ix < 0) {
            throw new IllegalArgumentException("Not present: " + key);
        }
        changes.put(ix, value);
        return this;
    }

    private void checkConsistency(Supplier<String> op) {
        if (map instanceof ArrayIntMap<?>) {
            ArrayIntMap<?> m = (ArrayIntMap<?>) map;
            try {
                m.consistent();
            } catch (AssertionError e) {
                throw new AssertionError("Inconsistent after " + op.get(), e);
            }
        }
    }

    public final void commit() {
        removals.removeAll(changes.keySet());
        changes.forEachIndexed((int index, int key, T value) -> {
            map.setValueAt(key, value);
        });
        checkConsistency(() ->  "Applying changes " + changes);
        map.removeIndices(removals);
        checkConsistency(() ->  "Removing indices " + changes);
        map.putAll(additions);
        checkConsistency(() ->  "PutAll " + additions.keySet());
        additions.clear();
        removals.clear();
        changes.clear();
    }

    private int checkIndex(int index) {
        assert index < map.size() : "Invalid index " + index
                + " >= map size " + map.size() + " returned by "
                + map;
        return index;
    }

    public final IntMapModifier move(int from, int to) {
        int sourceIndex = checkIndex(map.indexOf(from));
        if (sourceIndex < 0) {
            throw new IllegalArgumentException("Not present: " + from);
        }
        int destIndex = checkIndex(map.indexOf(to));
        T old = map.valueAt(sourceIndex);
        T origDestValue = destIndex < 0 ? null : map.valueAt(destIndex);
        mover.onMove(from, old, to, origDestValue, (newOldValue, newNewValue) -> {
            if (newOldValue != null) {
                changes.put(sourceIndex, newOldValue);
            } else {
                removals.add(sourceIndex);
            }
            if (newNewValue != null) {
                if (destIndex < 0) {
                    additions.put(to, newNewValue);
                } else {
                    changes.put(destIndex, newNewValue);
                }
            }
        });
        return this;
    }

    public final IntMapModifier add(int key, T value) {
        int ix = checkIndex(map.indexOf(key));
        if (ix < 0) {
            additions.put(key, value);
        } else {
            changes.put(ix, value);
        }
        return this;
    }

    public final IntMapModifier remove(int key) {
        int ix = checkIndex(map.indexOf(key));
        if (ix >= 0) {
            removals.add(ix);
        }
        return this;
    }

    public final IntMapModifier removeAll(IntSet keys) {
        keys.forEachInt(val -> {
            int ix = checkIndex(map.indexOf(val));
            if (ix >= 0) {
                removals.add(ix);
            }
        });
        return this;
    }

    static class DefaultMover<T> implements EntryMover<T> {

        @Override
        public T onMove(int oldKey, T oldValue, int newKey, T newValue, BiConsumer<T, T> oldNewReceiver) {
            oldNewReceiver.accept(null, newValue);
            return newValue;
        }

    }
}
