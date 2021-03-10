/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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

import static com.mastfrog.util.preconditions.Checks.notNull;
import static java.lang.Integer.numberOfTrailingZeros;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

/**
 * A thread safe enum set using atomics.
 *
 * @author Tim Boudreau
 */
final class AtomicEnumSetSmall<T extends Enum<T>> extends AbstractSet<T> implements AtomicEnumSet<T> {

    private volatile int value;
    static AtomicIntegerFieldUpdater<AtomicEnumSetSmall> upd
            = AtomicIntegerFieldUpdater.newUpdater(AtomicEnumSetSmall.class, "value");
    private final Class<T> type;
    private final int max;

    AtomicEnumSetSmall(Class<T> type, boolean none) {
        this.type = type;
        assert type.isEnum();
        max = type.getEnumConstants().length;
        if (!none) {
            for (int i = 0; i < max; i++) {
                value |= 1 << i;
            }
        }
    }

    AtomicEnumSetSmall(Class<T> type, int value) {
        this.value = value;
        this.type = type;
        max = type.getEnumConstants().length;
    }

    AtomicEnumSetSmall(Class<T> type) {
        this(type, true);
    }

    AtomicEnumSetSmall(T a) {
        this(notNull("a", a).getDeclaringClass());
        value = a.ordinal();
    }

    AtomicEnumSetSmall(T a, T b) {
        this(notNull("a", a).getDeclaringClass());
        value = (1 << a.ordinal()) | (1 << b.ordinal());
    }

    AtomicEnumSetSmall(T a, T b, T c) {
        this(notNull("a", a).getDeclaringClass());
        value = (1 << a.ordinal()) | (1 << b.ordinal()) | (1 << c.ordinal());
    }

    AtomicEnumSetSmall(T a, T... more) {
        this(notNull("a", a).getDeclaringClass());
        value = 1 << a.ordinal();
        for (int i = 0; i < more.length; i++) {
            value |= 1 << more[i].ordinal();
        }
    }

    @Override
    public AtomicEnumSetSmall<T> copy() {
        return new AtomicEnumSetSmall<>(type, get());
    }

    private boolean _contains(T obj) {
        int mask = 1 << obj.ordinal();
        return (upd.get(this) & mask) != 0;
    }

    private boolean _add(T obj) {
        int mask = 1 << obj.ordinal();
        int prev = upd.getAndUpdate(this, old -> {
            return old | mask;
        });
        return (prev & mask) == 0;
    }

    private boolean _remove(T obj) {
        int invmask = ~(1 << obj.ordinal());
        int prev = upd.getAndUpdate(this, old -> {
            return invmask & old;
        });
        return (invmask & prev) != prev;
    }

    @Override
    public boolean add(T obj) {
        return _add(check(obj));
    }

    public boolean remove(T obj) {
        return _remove(check(obj));
    }

    public boolean contains(T obj) {
        if (obj == null || obj.getClass() != type) {
            return false;
        }
        return _contains(obj);
    }

    @Override
    public void clear() {
        upd.set(this, 0);
    }

    @Override
    public Number rawValue() {
        return get();
    }

    @Override
    public boolean removeAll(Collection<?> coll) {
        if (coll == this) {
            int old = upd.getAndUpdate(this, ignored -> 0);
            return old != 0;
        } else if (coll instanceof AtomicEnumSetSmall<?>) {
            AtomicEnumSetSmall<?> aess = (AtomicEnumSetSmall<?>) coll;
            int removeBits = aess.get();
            int old = upd.getAndUpdate(this, val -> {
                return val & ~removeBits;
            });
            return (removeBits & old) != 0;
        }
        int mask = ~0;
        for (Object obj : coll) {
            if (obj == null) {
                continue;
            }
            if (type.isInstance(obj)) {
                mask ^= 1 << type.cast(obj).ordinal();
            }
        }
        int mk = mask;
        int val = upd.getAndUpdate(this, old -> {
            return old & mk;
        });
        return (val & mk) != val;
    }

    @Override
    public boolean retainAll(Collection<?> coll) {
        if (coll == this) {
            return false;
        }
        if (coll instanceof AtomicEnumSetSmall<?>) {
            AtomicEnumSetSmall<?> e = (AtomicEnumSetSmall<?>) coll;
            if (e.type != type) {
                boolean was = isEmpty();
                clear();
                return !was;
            }
            int val = e.get();
            int prev = upd.getAndUpdate(this, old -> {
                return old & val;
            });
            return prev == val;
        }
        int mask = 0;
        for (Object obj : coll) {
            if (obj == null) {
                continue;
            }
            if (type.isInstance(obj)) {
                mask |= 1 << type.cast(obj).ordinal();
            }
        }
        int mk = mask;
        int val = upd.getAndUpdate(this, old -> {
            return old & mk;
        });
        return (val & mk) != val;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(Collection<? extends T> coll) {
        if (coll.isEmpty()) {
            return false;
        }
        if (coll instanceof AtomicEnumSetSmall<?>) {
            AtomicEnumSetSmall<T> e = (AtomicEnumSetSmall<T>) coll;
            if (e.type != type) {
                throw new IllegalArgumentException("Not same type: " + e.type + " and " + type);
            }
            int val = e.get();
            int prev = upd.getAndUpdate(this, old -> {
                return old | val;
            });
            return (prev | val) != prev;
        }
        int mask = 0;
        for (T obj : coll) {
            check(obj);
            mask |= 1 << obj.ordinal();
        }
        int m = mask;
        int val = upd.getAndUpdate(this, old -> {
            return old | m;
        });
        return (val & m) == val;
    }

    @Override
    public AtomicEnumSetSmall<T> complement() {
        return new AtomicEnumSetSmall<>(type, ~get());
    }

    @SuppressWarnings("unchecked")
    private T check(Object val) {
        if (val == null) {
            throw new IllegalArgumentException("Null not allowed");
        }
        if (type != val.getClass()) { // rawtype
            throw new IllegalArgumentException("Not an instance of " + type.getName() + ": " + val);
        }
        return (T) val;
    }

    @Override
    public int size() {
        return Math.min(max, Integer.bitCount(get()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        T[] consts = type.getEnumConstants();
        int val = get();
        for (int i = 0; i < consts.length; i++) {
            if ((val & (1 << i)) != 0) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(consts[i]);
            }
        }
        return sb.append(']').toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != AtomicEnumSetSmall.class) {
            if (obj instanceof Set<?>) {
                int val = get();
                Set<?> s = (Set<?>) obj;
                int sz = s.size();
                if (sz > 31 || ((val == 0) != (sz != 0))) {
                    return false;
                }
                for (Object o : s) {
                    T t = (T) o;
                    if (((1 << t.ordinal()) & val) == 0) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        AtomicEnumSetSmall<?> o = (AtomicEnumSetSmall<?>) obj;
        return o.type == type && get() == o.get();
    }

    @Override
    public boolean isEmpty() {
        return upd.get(this) == 0;
    }

    int get() {
        return upd.get(this);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    @Override
    public void forEach(Consumer<? super T> c) {
        final T[] all = type.getEnumConstants();
        int val = get();
        for (int mask = ~0, curr = (val & mask), next = numberOfTrailingZeros(curr);
                curr != 0 && next < all.length;
                mask = ~0 << (next + 1), curr = val & mask, next = numberOfTrailingZeros(curr)) {
            c.accept(all[next]);
            if (next == 31) {
                break;
            }
        }
    }

    class Iter implements Iterator<T> {

        final T[] all = type.getEnumConstants();
        int mask = ~0;
        int curr;

        @Override
        public boolean hasNext() {
            boolean result = ((curr = (get() & mask))) != 0
                    && numberOfTrailingZeros(curr) < all.length;
            return result;
        }

        @Override
        public void remove() {
            if (curr == 0) {
                throw new NoSuchElementException();
            }
            upd.updateAndGet(AtomicEnumSetSmall.this, val -> {
                return val & ~(1 << numberOfTrailingZeros(curr));
            });
        }

        @Override
        public T next() {
            if (curr == 0) {
                throw new NoSuchElementException();
            }
            int next = Integer.numberOfTrailingZeros(curr);
            mask = next == 31 ? 0 : ~0 << (next + 1);
            T result = all[next];
            return result;
        }
    }
}
