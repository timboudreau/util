/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.util.collections;

import com.mastfrog.util.collections.Trimmable.TrimmableSet;
import java.lang.ref.Reference;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * A weak (or something) set where the Reference objects used can be provided by a function,
 * and the hashing function that determines set membership can also be provided.
 *
 * @author Tim Boudreau
 */
final class ReferenceFactorySet<T> extends AbstractSet<T> implements TrimmableSet<T> {

    private final IntMap<Reference<T>> im;
    private final Function<? super T, ? extends Reference<T>> referenceFactory;

    public static ToIntFunction<Object> IDENTITY_HASH_CODE = o -> o == null ? 0 : System.identityHashCode(o);
    public static ToIntFunction<Object> OBJECT_HASH_CODE = o -> o == null ? 0 : o.hashCode();
    private final ToIntFunction<Object> hasher;

    ReferenceFactorySet(Function<? super T, ? extends Reference<T>> referenceFactory, int initialSize) {
        this(IDENTITY_HASH_CODE, referenceFactory, initialSize);
    }

    ReferenceFactorySet(Function<? super T, ? extends Reference<T>> referenceFactory) {
        this(IDENTITY_HASH_CODE, referenceFactory, 16);
    }

    ReferenceFactorySet(ToIntFunction<Object> hasher, Function<? super T, ? extends Reference<T>> referenceFactory, int initialSize) {
        this.referenceFactory = referenceFactory;
        this.hasher = hasher;
        im = IntMap.create(Math.max(16, initialSize));
    }

    private int hash(Object obj) {
        return hasher.applyAsInt(obj);
    }

    @Override
    public void trim() {
        gc();
        im.trim();
    }

    @Override
    public boolean add(T e) {
        if (e == null) {
            throw new IllegalArgumentException("Null not supported");
        }
        int idhash = hash(e);
        if (im.containsKey(idhash)) {
            return false;
        }
        Reference<T> ref = referenceFactory.apply(e);
        im.put(idhash, ref);
        return true;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        return im.containsKey(hash(o));
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        Reference<T> ref = im.remove(hash(o));
        return ref != null;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (c.isEmpty()) {
            return false;
        }
        IntMap<Reference<T>> allToAdd = IntMap.create(c.size());
        for (T t : c) {
            allToAdd.put(hash(t), referenceFactory.apply(t));
        }
        int oldsize = im.size();
        im.putAll(allToAdd);
        return im.size() != oldsize;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c.isEmpty()) {
            return false;
        }
        IntSet is = IntSet.bitSetBased(c.size());
        boolean anyLive = false;
        for (Object o : c) {
            int hc = hash(o);
            int ix = im.indexOf(hc);
            if (ix >= 0) {
                is.add(ix);
                if (!anyLive) {
                    Reference<T> ref = im.valueAt(ix);
                    anyLive = ref.get() != null;
                }
            }
        }
        // Deleteme - fixed bug in 2.6.12 - toIntArray does not force sorting
        // if the set is unsorted - this call ensures the IntSet's contents are
        // sorted, so the right items are removed
        is.indexOf(100000);
        if (is.isEmpty()) {
            return false;
        }
        boolean result = im.removeIndices(is) > 0;
        if (result && !anyLive) {
            gc();
        }
        return result && anyLive;
    }

    void gc() {
        im.removeIf(ref -> ref.get() == null);
    }

    @Override
    public void clear() {
        im.clear();
    }

    @Override
    public boolean isEmpty() {
        return im.isEmpty();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        im.forEachValue(ref -> {
            T obj = ref.get();
            if (obj != null) {
                action.accept(obj);
            }
        });
    }

    @Override
    public Iterator<T> iterator() {
        List<T> result = new ArrayList<>();
        im.forEachValue(ref -> {
            T obj = ref.get();
            if (obj != null) {
                result.add(obj);
            }
        });
        return result.iterator();
    }

    @Override
    public int size() {
        int sz = im.size();
        int[] result = new int[] {0};
        im.forEachValue(ref -> {
            if (ref.get() != null) {
                result[0]++;
            }
        });
        if (result[0] == 0 && sz > 0 && im.size() == sz) {
            im.clear();
        }
        return result[0];
    }
}
