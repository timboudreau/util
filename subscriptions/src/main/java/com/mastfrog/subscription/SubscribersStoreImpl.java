package com.mastfrog.subscription;

import com.mastfrog.util.collections.SetFactories;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.MapFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class SubscribersStoreImpl<K, C> implements SubscribersStore<K, C> {

    private SubscribersStore<K, C> store;

    private SubscribersStoreController<K, C> mutator;

    SubscribersStoreImpl(SubscribersStore<K, C> store, SubscribersStoreController<K, C> mutator) {
        this.store = store;
        this.mutator = mutator;
    }

    <S extends SubscribersStore<K, C> & SubscribersStoreController<K, C>> SubscribersStoreImpl(S store) {
        this(store, store);
    }

    @Override
    public Collection<? extends K> subscribedKeys() {
        return store.subscribedKeys();
    }

    @Override
    public boolean isKeysRetrievalSupported() {
        return store.isKeysRetrievalSupported();
    }

    SubscribersStoreImpl(int targetSize, MapFactory type, Supplier<Set<C>> setFactory) {
        if (type.isMapBased()) {
            Map<K, Set<C>> map = type.createMap(targetSize, true);
            MapSpi<K, C> m = new MapSpi<>(map, setFactory);
            this.store = m;
            this.mutator = m;
        } else {
            IdentityHashMapSpi<K, C> spi = new IdentityHashMapSpi<>(targetSize, setFactory);
            this.store = spi;
            this.mutator = spi;
        }
    }

    SubscribersStoreImpl(int targetSize, MapFactory type, SetFactories setFactory) {
        if (type.isMapBased()) {
            Map<K, Set<C>> map = type.createMap(targetSize, true);
            MapSpi<K, C> m = new MapSpi<>(map, setFactory.setSupplier(targetSize, true));
            this.store = m;
            this.mutator = m;
        } else {
            IdentityHashMapSpi<K, C> spi = new IdentityHashMapSpi<>(targetSize, setFactory.setSupplier(targetSize, true));
            this.store = spi;
            this.mutator = spi;
        }
    }

    SubscribersStoreController<K, C> mutator() {
        return mutator;
    }

    @Override
    public Collection<? extends C> subscribersTo(K key) {
        return store.subscribersTo(key);
    }

    static class IdentityHashMapSpi<K, C> implements SubscribersStoreController<K, C>, SubscribersStore<K, C> {

        private final IntMap<Set<C>> map;

        IdentityHashMapSpi(int size, Supplier<Set<C>> setFactory) {
            map = IntMap.create(size, true, setFactory);
        }

        @Override
        public synchronized boolean add(K key, C subscriber) {
            return map.get(System.identityHashCode(key)).add(subscriber);
        }

        @Override
        public synchronized boolean remove(K key, C subscriber) {
            return map.get(System.identityHashCode(key)).remove(subscriber);
        }

        @Override
        public synchronized void removeAll(K key) {
            map.remove(System.identityHashCode(key));
        }

        @Override
        public synchronized void clear() {
            map.clear();
        }

        @Override
        public Collection<? extends C> subscribersTo(K key) {
            Collection<? extends C> result = map.get(System.identityHashCode(key));
            return result == null ? Collections.emptySet() : result;
        }

        @Override
        public boolean isKeysRetrievalSupported() {
            return false;
        }

        @Override
        public Collection<? extends K> subscribedKeys() {
            throw new UnsupportedOperationException("Key iteration not supported.");
        }
    }

    static class MapSpi<K, C> implements SubscribersStoreController<K, C>, SubscribersStore<K, C> {

        private final Map<K, Set<C>> map;
        private final Supplier<Set<C>> setFactory;

        public MapSpi(Map<K, Set<C>> map, Supplier<Set<C>> setFactory) {
            this.map = map;
            this.setFactory = setFactory;
        }

        Set<C> getSet(K key, boolean create) {
            Set<C> result = map.get(key);
            if (result == null && create) {
                result = setFactory.get();
                map.put(key, result);
            } else if (result == null) {
                result = Collections.emptySet();
            }
            return result;
        }

        @Override
        public boolean add(K key, C subscriber) {
            return getSet(key, true).add(subscriber);
        }

        @Override
        public boolean remove(K key, C subscriber) {
            Set<C> set = getSet(key, false);
            if (!set.isEmpty()) {
                return set.remove(subscriber);
            }
            return false;
        }

        @Override
        public void removeAll(K key) {
            map.remove(key);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Collection<? extends C> subscribersTo(K key) {
            return map.getOrDefault(key, Collections.emptySet());
        }

        @Override
        public Collection<? extends K> subscribedKeys() {
            Set<K> keys = new LinkedHashSet<>(map.size());
            for (Map.Entry<K, Set<C>> e : map.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    keys.add(e.getKey());
                }
            }
            return keys;
        }
    }
}
