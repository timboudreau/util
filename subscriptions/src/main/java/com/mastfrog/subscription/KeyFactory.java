package com.mastfrog.subscription;

import java.util.function.Function;

/**
 * Converts a key a client will listen on or an event will occur in, into the
 * single form it is stored in the backing storage as - this allows subscribers
 * and event providers to support subscribing to multiple object types which all
 * represent the same underlying thing.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface KeyFactory<T, K> {

    K constructKey(T obj);

    default <S> KeyFactory<S, K> adapt(Function<S, T> converter) {
        return s -> constructKey(converter.apply(s));
    }

    static <K> KeyFactory<K, K> identity() {
        return key -> key;
    }
}
