package com.mastfrog.abstractions.list;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * A minimal list-like abstraction of a List that lets items be retrieved by
 * index.
 *
 * @author Tim Boudreau
 */
public interface LongIndexedResolvable<T> extends LongIndexed<T>, LongResolvable, LongSized {

    static <T> LongIndexedResolvable<T> fromList(List<T> list) {
        Objects.requireNonNull(list, "list null");
        return new LongListAsIndexed<>(list);
    }

    static <T> LongIndexedResolvable<T> compose(LongAddressable<T> lookup, int size, LongResolvable reverseLookup) {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(lookup, "reverseLookup");
        return compose(lookup, () -> size, reverseLookup);
    }

    static <T> LongIndexedResolvable<T> compose(LongAddressable<T> lookup, LongSupplier size, LongResolvable reverseLookup) {
        return new LongIndexedResolvable<T>() {
            @Override
            public T forIndex(long index) {
                return lookup.forIndex(index);
            }

            @Override
            public long indexOf(Object obj) {
                return reverseLookup.indexOf(obj);
            }

            @Override
            public long size() {
                return size.getAsLong();
            }
        };
    }

    default Iterable<T> asIterable() {
        class I implements Iterable<T>, Iterator<T> {

            int ix = -1;

            @Override
            public Iterator<T> iterator() {
                return this;
            }

            @Override
            public boolean hasNext() {
                return ix + 1 < size();
            }

            @Override
            public T next() {
                return forIndex(++ix);
            }
        }
        return new I();
    }

    default List<T> populate(List<T> list) {
        long sz = size();
        for (int i = 0; i < sz; i++) {
            list.add(forIndex(i));
        }
        return list;
    }

    default List<T> toList() {
        if (size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size too large: " + size());
        }
        return populate(new ArrayList<>((int) size()));
    }

    static <T> LongIndexedResolvable<T> forList(List<T> list) {
        assert new HashSet<>(list).size() == list.size();
        return new LongIndexedResolvable<T>() {
            @Override
            public long indexOf(Object o) {
                return list.indexOf(o);
            }

            @Override
            public T forIndex(long index) {
                if (index > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Index too large: " + index);
                }
                return list.get((int) index);
            }

            @Override
            public long size() {
                return list.size();
            }
        };
    }

    default Collection<T> asCollection() {
        return new Collection<T>() {
            @Override
            public int size() {
                if (LongIndexedResolvable.this.size() > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Size too large");
                }
                return (int) LongIndexedResolvable.this.size();
            }

            @Override
            public boolean isEmpty() {
                return LongIndexedResolvable.this.size() == 0L;
            }

            @Override
            public boolean contains(Object o) {
                int sz = size();
                for (int i = 0; i < sz; i++) {
                    if (Objects.equals(o, LongIndexedResolvable.this.forIndex(i))) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Iterator<T> iterator() {
                return asIterable().iterator();
            }

            @Override
            public Object[] toArray() {
                Object[] objs = new Object[size()];
                for (int i = 0; i < objs.length; i++) {
                    objs[i] = forIndex(i);
                }
                return objs;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                int sz = size();
                if (sz != a.length) {
                    a = (T[]) Array.newInstance(a.getClass().getComponentType(), sz);
                }
                for (int i = 0; i < sz; i++) {
                    a[i] = (T) forIndex(i);
                }
                return a;
            }

            @Override
            @SuppressWarnings("element-type-mismatch")
            public boolean containsAll(Collection<?> c) {
                for (Object o : c) {
                    if (!contains(o)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean add(T e) {
                throw new UnsupportedOperationException("read-only");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("read-only");
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                throw new UnsupportedOperationException("read-only");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("read-only");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("read-only");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("read-only");
            }
        };
    }
}
