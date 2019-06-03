package com.mastfrog.util.cache;

import com.mastfrog.util.preconditions.Checks;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A straightforward cache with expiring entries, capable of conversion into a
 * bi-directional cache. Highly concurrent, but with perfect timing it is
 * possible for a value to be computed simultaneously by two threads. It is
 * assumed that such computation is idempotent. For when you can't have a
 * dependency on Guava.
 *
 * @author Tim Boudreau
 */
class TimedCacheImpl<T, R, E extends Exception> implements TimedCache<T, R, E> {

    final long timeToLive;
    private final Answerer<T, R, E> answerer;
    final Map<T, CacheEntry> cache;
    private BiConsumer<T, R> onExpire;

    TimedCacheImpl(long ttl, Answerer<T, R, E> answerer) {
        this(ttl, answerer, ConcurrentHashMap::new);
    }

    TimedCacheImpl(long ttl, Answerer<T, R, E> answerer, MapSupplier<T> supp) {
        this.timeToLive = ttl;
        this.answerer = answerer;
        cache = supp.get();
    }

    private TimedCacheImpl(TimedCacheImpl<T, R, E> other) {
        this.timeToLive = other.timeToLive;
        this.answerer = other.answerer;
        this.cache = other.cache;
        this.onExpire = other.onExpire;
    }

    @Override
    public Optional<R> cachedValue(T key) {
        CacheEntry e = cache.get(key);
        return e == null ? Optional.empty() : Optional.ofNullable(e.value);
    }

    @Override
    public boolean remove(T key) {
        return cache.remove(key) != null;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(getClass().getSimpleName())
                .append('{')).append('}').toString();
    }

    StringBuilder toString(StringBuilder sb) {
        sb.append("entries=[");
        for (Iterator<Map.Entry<T, CacheEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<T, CacheEntry> e = it.next();
            sb.append(e.getKey()).append('=').append(e.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb;
    }

    /**
     * Clear this cache, removing expiring all entries immediately. If an
     * OnExpire handler has been set, it will not be called for entries that are
     * being removed (use close() if you need that).
     *
     * @return this
     */
    @Override
    public TimedCacheImpl<T, R, E> clear() {
        cache.clear();
        caches().removeAll(cache.values());
        return this;
    }

    boolean containsKey(T key) {
        return cache.containsKey(key);
    }

    /**
     * Get a value which may be null.
     *
     * @param key The key
     * @return The value
     * @throws E If something goes wrong
     */
    @Override
    public Optional<R> getOptional(T key) throws E {
        return Optional.ofNullable(get(key));
    }

    /**
     * Shut down this cache, immediately expiring any entries which have not
     * been evicted.
     */
    @Override
    public void close() {
        for (CacheEntry e : cache.values()) {
            e.close();
        }
    }

    /**
     * Add a consumer which is called after a value has been expired from the
     * cache - this can be used to perform any cleanup work necessary.
     *
     * @param onExpire A biconsumer to call that receives they key and value
     * which have expired.
     *
     * @return this
     */
    @Override
    public TimedCacheImpl<T, R, E> onExpire(BiConsumer<T, R> onExpire) {
        if (this.onExpire != null) {
            throw new IllegalStateException("OnExpire is already "
                    + this.onExpire);
        }
        this.onExpire = onExpire;
        return this;
    }

    /**
     * Get a value from the cache, computing it if necessary.
     *
     * @param key The key to look up. May not be null.
     * @return A value or null if the answerer returned null
     * @throws E If something goes wrong
     */
    @Override
    public R get(T key) throws E {
        CacheEntry entry = cache.get(Checks.notNull("key", key));
        if (entry == null) {
            R result = answerer.answer(key);
            if (result != null) {
                entry = createEntry(key, result);
            }
        } else {
            entry.touch();
        }
        return entry == null ? null : entry.value;
    }

    CacheEntry createEntry(T key, R val) {
        // XXX should create reversed entries for bidi caches here,
        // make CacheEntry an interface and allow an entry to have
        // child entries that hold the reverse value which do not
        // get enqueued, just expired
        CacheEntry result = new CacheEntry(key, val);
        cache.put(key, result);
        caches().offer(result);
        return result;
    }

    /**
     * Convert this cache to a bi-directional cache, providing an answerer for
     * reverse queries. The returned cache will initially share its contents
     * with this cache and show changes from the original.
     *
     * @param reverseAnswerer An answerer
     * @return A bidirectional cache with the same contents as this one
     */
    @Override
    public BidiCacheImpl<T, R, E> toBidiCache(Answerer<R, T, E> reverseAnswerer) {
        return new BidiCacheImpl<>(this, reverseAnswerer);
    }

    void expireEntry(CacheEntry ce) {
        cache.remove(ce.key, ce);
        if (onExpire != null) {
            try {
                onExpire.accept(ce.key, ce.value);
            } catch (Exception e) {
                Logger.getLogger(TimedCacheImpl.class.getName())
                        .log(Level.SEVERE, "Failure in onExpire", e);
            }
        }
    }

    /**
     * A bi-directional variant of TimedCacheImpl.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param <E> The exception that may be thrown by the function which looks
     * things up
     */
    static final class BidiCacheImpl<T, R, E extends Exception> extends TimedCacheImpl<T, R, E> implements TimedBidiCache<T, R, E> {

        private final Answerer<R, T, E> reverseAnswerer;
        private final Map<R, TimedCacheImpl<T, R, E>.CacheEntry> reverseEntries = new ConcurrentHashMap<>();

        BidiCacheImpl(TimedCacheImpl<T, R, E> orig, Answerer<R, T, E> reverseAnswerer) {
            super(orig);
            this.reverseAnswerer = reverseAnswerer;
        }

        @Override
        public Optional<T> cachedKey(R value) {
            TimedCacheImpl<T, R, E>.CacheEntry e = reverseEntries.get(value);
            return Optional.ofNullable(e.key);
        }

        @Override
        StringBuilder toString(StringBuilder sb) {
            sb.append(" reverse-entries=[");
            for (Iterator<Map.Entry<R, TimedCacheImpl<T, R, E>.CacheEntry>> it = reverseEntries.entrySet().iterator(); it.hasNext();) {
                Map.Entry<R, TimedCacheImpl<T, R, E>.CacheEntry> e = it.next();
                sb.append(e.getKey()).append('=').append(e.getValue());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(']');
            return sb;
        }

        public BidiCacheImpl<T, R, E> clear() {
            reverseEntries.clear();
            super.clear();
            return this;
        }

        /**
         * On BidiCacheImpl, toBidiCache returns this
         *
         * @param reverseAnswerer ignored
         * @return this
         */
        public BidiCacheImpl<T, R, E> toBidiCache(Answerer<R, T, E> reverseAnswerer) {
            return this;
        }

        /**
         * Get the key for a value, wrapped in an Optional.
         *
         * @param value The value
         * @return The key, which will be present if contained in the cache
         * already or if the reverse anwerer returned non-null
         * @throws E
         */
        @Override
        public Optional<T> getKeyOptional(R value) throws E {
            return Optional.ofNullable(getKey(value));
        }

        boolean containsValue(R value) {
            return reverseEntries.containsKey(value);
        }

        public String toString() {
            return reverseEntries.toString();
        }

        public BidiCacheImpl<T, R, E> onExpire(BiConsumer<T, R> onExpire) {
            super.onExpire(onExpire);
            return this;
        }

        /**
         * Get the key for a value.
         *
         * @param value The value
         * @return The key, or null
         * @throws E If something goes wrong
         */
        @Override
        public T getKey(R value) throws E {
            TimedCacheImpl<T, R, E>.CacheEntry entry = reverseEntries.get(
                    Checks.notNull("value", value));
            if (entry == null) {
                T result = reverseAnswerer.answer(value);
                if (result != null) {
                    entry = createEntry(result, value);
                }
            } else {
                entry.touch();
            }
            return entry == null ? null : entry.key;
        }

        @Override
        void expireEntry(TimedCacheImpl<T, R, E>.CacheEntry ce) {
            reverseEntries.remove(ce.value, ce);
            super.expireEntry(ce);
        }

        @Override
        TimedCacheImpl<T, R, E>.CacheEntry createEntry(T key, R val) {
            TimedCacheImpl<T, R, E>.CacheEntry result = super.createEntry(key, val);
            reverseEntries.put(val, result);
            return result;
        }
    }

    final class CacheEntry implements Expirable {

        volatile long touched = System.currentTimeMillis();
        final T key;
        final R value;

        public CacheEntry(T key, R value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }

        void touch() {
            touched = System.currentTimeMillis();
        }

        void close() {
            touched = 0;
        }

        @Override
        public void expire() {
            expireEntry(this);
        }

        @Override
        public boolean isExpired() {
            return remaining() <= 0;
        }

        private long remaining() {
            return Math.max(0, timeToLive - (System.currentTimeMillis() - touched));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(remaining(), MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            long a = getDelay(MILLISECONDS);
            long b = o.getDelay(MILLISECONDS);
            return Long.compare(a, b);
        }
    }

    static Expirer caches() {
        if (EXPIRER == null) {
            EXPIRER = expirerFactory.get();
        }
        return EXPIRER;
    }

    private static Expirer EXPIRER;
    @SuppressWarnings("StaticNonFinalUsedInInitialization")
    static Supplier<Expirer> expirerFactory = () -> {
        return EXPIRER == null ? EXPIRER = new Expirer() : EXPIRER;
    };
}
