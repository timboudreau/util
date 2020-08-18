
package com.mastfrog.util.collections;

import com.mastfrog.util.cache.MapSupplier;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Types of map from subscribed-to to set-of-subscribers.
 *
 * @author Tim Boudreau
 */
public enum MapFactories implements MapFactory {
    /**
     * Use a regular java.util.HashMap.
     */
    EQUALITY,
    /**
     * Use a WeakHashMap.
     */
    WEAK,
    /**
     * Use a ConcurrentHashMap.
     */
    EQUALITY_CONCURRENT,
    /**
     * Use a standard JDK IdentityHashMap (which does strongly reference keys).
     */
    IDENTITY,
    /**
     * Creates a map with weakly referenced <i>values</i> and strongly
     * referenced keys - <b>Note:</b> do not use for Subscribable subscriber
     * sets, as the internal collections of subscribers will disappear as soon
     * as they're created.
     */
    WEAK_VALUE,
    /**
     * Creates a map with weakly referenced <i>keys</i> <u>and</u> <i>values</i>
     * and strongly referenced keys - <b>Note:</b> do not use for Subscribable
     * subscriber sets, as the internal collections of subscribers will
     * disappear as soon as they're created.
     */
    WEAK_KEYS_AND_VALUES,
    /**
     * Use a binary-search integer map mapping the identity hash code of the
     * object; no reference to the original object is retained. Behavior is
     * similar to that of WeakHashMap, but better performaing, with the caveat
     * that there is no mechanism to garbage collect references to key objects,
     * so if there are likely to be many objects listened to for a short while
     * in the JVM, the map will grow, accumulating stale hash codes unless
     * anything that gets subscribed also gets explicitly unsubscribed.
     * <p>
     * Note that the JDK does not guarantee that identity hash codes are not
     * recycled - so there is no guarantee that the same identity hash code
     * will not be reused for the same type multiple times.
     * </p>
     */
    IDENTITY_WITHOUT_REFERENCE;

    /**
     * Creates a new map.
     *
     * @param <T> The key type
     * @param <R> The value type
     * @param initialSize The initial size
     * @param threadSafe If true, create a thread-safe map instance -
     * implementations that are naturally concurrent are returned as-is, and
     * other implementations are wrapped in Collections.synchronizedMap() or
     * similar
     * @return A map
     */
    @Override
    public <T, R> Map<T, R> createMap(int initialSize, boolean threadSafe) {
        Map<T, R> result;
        switch (this) {
            case EQUALITY:
                result = new HashMap<>(initialSize);
                break;
            case EQUALITY_CONCURRENT:
                return new ConcurrentHashMap<>(initialSize);
            case IDENTITY:
                result = new IdentityHashMap<>(initialSize);
                break;
            case WEAK:
                result = new WeakHashMap<>(initialSize);
                break;
            case WEAK_VALUE:
                result = CollectionUtils.weakValueMap(EQUALITY, initialSize, WeakReference::new);
                break;
            case WEAK_KEYS_AND_VALUES:
                result = CollectionUtils.weakValueMap(WEAK, initialSize, WeakReference::new);
                break;
            case IDENTITY_WITHOUT_REFERENCE:
            default:
                throw new UnsupportedOperationException(this + " does not support map creation");
        }
        return threadSafe ? Collections.synchronizedMap(result) : result;
    }

    public <T> MapSupplier<T> toMapSupplier(int initialSize, boolean threadSafe) {
        return new MapSupplier<T>() {
            @Override
            public <V> Map<T, V> get() {
                return createMap(initialSize, threadSafe);
            }
        };
    }

    public boolean isWeakValues() {
        switch (this) {
            case WEAK_VALUE:
            case WEAK_KEYS_AND_VALUES:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isMapBased() {
        return hasKeyReference();
    }

    public boolean hasKeyReference() {
        return this != IDENTITY_WITHOUT_REFERENCE;
    }

    public boolean stronglyReferencesKeys() {
        switch (this) {
            case EQUALITY:
            case IDENTITY:
            case WEAK_VALUE:
                return true;
            default:
                return false;
        }
    }
}
