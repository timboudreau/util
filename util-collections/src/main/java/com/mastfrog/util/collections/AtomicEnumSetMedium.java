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
import static java.lang.Long.numberOfTrailingZeros;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Consumer;

/**
 * A thread safe enum set using atomics.
 *
 * @author Tim Boudreau
 */
final class AtomicEnumSetMedium<T extends Enum<T>> extends AbstractSet<T> implements AtomicEnumSet<T> {

    private volatile long value;
    static AtomicLongFieldUpdater upd = AtomicLongFieldUpdater.newUpdater(AtomicEnumSetMedium.class, "value");
    private final Class<T> type;
    private final int max;

    AtomicEnumSetMedium(Class<T> type, boolean none) {
        this.type = type;
        assert type.isEnum();
        max = type.getEnumConstants().length;
        if (!none) {
            for (int i = 0; i < max; i++) {
                value |= 1L << i;
            }
        }
    }

    AtomicEnumSetMedium(Class<T> type, long value) {
        this.value = value;
        this.type = type;
        max = type.getEnumConstants().length;
    }

    AtomicEnumSetMedium(Class<T> type) {
        this(type, true);
    }

    AtomicEnumSetMedium(T a, T b) {
        this(notNull("a", a).getDeclaringClass());
        value = (1L << a.ordinal()) | (1L << b.ordinal());
    }

    AtomicEnumSetMedium(T a, T b, T c) {
        this(notNull("a", a).getDeclaringClass());
        value = (1L << a.ordinal()) | (1L << b.ordinal()) | (1L << c.ordinal());
    }

    AtomicEnumSetMedium(T a, T... more) {
        this(notNull("a", a).getDeclaringClass());
        value = 1L << a.ordinal();
        for (int i = 0; i < more.length; i++) {
            value |= 1L << more[i].ordinal();
        }
    }

    @Override
    public AtomicEnumSetMedium<T> copy() {
        return new AtomicEnumSetMedium(type, get());
    }

    private boolean _contains(T obj) {
        long mask = 1L << obj.ordinal();
        return (upd.get(this) & mask) != 0L;
    }

    private boolean _add(T obj) {
        long mask = 1L << obj.ordinal();
        long prev = upd.getAndUpdate(this, old -> {
            return old | mask;
        });
        return (prev & mask) == 0;
    }

    private boolean _remove(T obj) {
        long invmask = ~(1L << obj.ordinal());
        long prev = upd.getAndUpdate(this, old -> {
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
            long old = upd.getAndUpdate(this, ignored -> 0L);
            return old != 0L;
        } else if (coll instanceof AtomicEnumSetMedium<?>) {
            AtomicEnumSetMedium<?> aess = (AtomicEnumSetMedium<?>) coll;
            long removeBits = aess.get();
            long old = upd.getAndUpdate(this, val -> {
                return val & ~removeBits;
            });
            return (removeBits & old) != 0L;
        }
        long mask = ~0L;
        for (Object obj : coll) {
            if (obj == null) {
                continue;
            }
            if (type.isInstance(obj)) {
                mask ^= 1L << type.cast(obj).ordinal();
            }
        }
        long mk = mask;
        long val = upd.getAndUpdate(this, old -> {
            return old & mk;
        });
        return (val & mk) != val;
    }

    @Override
    public boolean retainAll(Collection<?> coll) {
        if (coll == this) {
            return false;
        }
        if (coll instanceof AtomicEnumSetMedium<?>) {
            AtomicEnumSetMedium<?> e = (AtomicEnumSetMedium<?>) coll;
            if (e.type != type) {
                boolean was = isEmpty();
                clear();
                return !was;
            }
            long val = e.get();
            long prev = upd.getAndUpdate(this, old -> {
                return old & val;
            });
            return prev == val;
        }
        long mask = 0L;
        for (Object obj : coll) {
            if (obj == null) {
                continue;
            }
            if (type.isInstance(obj)) {
                mask |= 1L << type.cast(obj).ordinal();
            }
        }
        long mk = mask;
        long val = upd.getAndUpdate(this, old -> {
            return old & mk;
        });
        return (val & mk) != val;
    }

    @Override
    public boolean addAll(Collection<? extends T> coll) {
        if (coll.isEmpty()) {
            return false;
        }
        if (coll instanceof AtomicEnumSetMedium<?>) {
            AtomicEnumSetMedium<T> e = (AtomicEnumSetMedium<T>) coll;
            if (e.type != type) {
                throw new IllegalArgumentException("Not same type: " + e.type + " and " + type);
            }
            long val = e.get();
            long prev = upd.getAndUpdate(this, old -> {
                return old | val;
            });
            return (prev | val) != prev;
        }
        long mask = 0L;
        for (T obj : coll) {
            check(obj);
            mask |= 1L << obj.ordinal();
        }
        long m = mask;
        long val = upd.getAndUpdate(this, old -> {
            return old | m;
        });
        return (val & m) == val;
    }

    @Override
    public AtomicEnumSetMedium<T> complement() {
        return new AtomicEnumSetMedium<>(type, ~get());
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
        return Math.min(max, Long.bitCount(get()));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        T[] consts = type.getEnumConstants();
        long val = get();
        for (int i = 0; i < consts.length; i++) {
            if ((val & (1L << i)) != 0L) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(consts[i]);
            }
        }
        return sb.append(']').toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != AtomicEnumSetMedium.class) {
            if (obj instanceof Set<?>) {
                long val = get();
                Set<?> s = (Set<?>) obj;
                int sz = s.size();
                if (sz > 63 || ((val == 0L) != (sz != 0))) {
                    return false;
                }
                for (Object o : s) {
                    T t = (T) o;
                    if (((1L << t.ordinal()) & val) == 0) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        AtomicEnumSetMedium<?> o = (AtomicEnumSetMedium<?>) obj;
        return o.type == type && get() == o.get();
    }

    @Override
    public boolean isEmpty() {
        return upd.get(this) == 0;
    }

    long get() {
        return upd.get(this);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    @Override
    public void forEach(Consumer<? super T> c) {
        final T[] all = type.getEnumConstants();
        long val = get();
        for (long mask = ~0L, curr = (val & mask), next = Long.numberOfTrailingZeros(curr);
                curr != 0L && next < all.length;
                mask = ~0L << (next + 1), curr = val & mask, next = Long.numberOfTrailingZeros(curr)) {
            c.accept(all[(int) next]);
            if (next == 63) {
                break;
            }
        }
    }

    class Iter implements Iterator<T> {

        final T[] all = type.getEnumConstants();
        long mask = ~0L;
        long curr;

        @Override
        public boolean hasNext() {
            boolean result = ((curr = (get() & mask))) != 0L
                    && numberOfTrailingZeros(curr) < all.length;
            return result;
        }

        @Override
        public void remove() {
            if (curr == 0) {
                throw new NoSuchElementException();
            }
            upd.updateAndGet(AtomicEnumSetMedium.this, val -> {
                return val & ~(1L << numberOfTrailingZeros(curr));
            });
        }

        @Override
        public T next() {
            if (curr == 0) {
                throw new NoSuchElementException();
            }
            int next = numberOfTrailingZeros(curr);
            mask = next == 63 ? 0 : ~0L << (next + 1);
            T result = all[next];
            return result;
        }
    }
}
