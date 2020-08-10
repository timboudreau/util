package com.mastfrog.subscription;

import java.util.Collection;

/**
 *
 * @author Tim Boudreau
 */
public interface SubscribersStore<K, C> {

    Collection<? extends C> subscribersTo(K key);

    Collection<? extends K> subscribedKeys();

    default boolean isKeysRetrievalSupported() {
        return true;
    }
}
