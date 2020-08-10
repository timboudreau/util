/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.subscription;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allow for multiple key types which all get resolved to a common type.
 *
 * @author Tim Boudreau
 */
class MultiTypeKeyFactory<K> implements KeyFactory<Object, K> {

    private final Map<Class<?>, KeyFactory<?, ? extends K>> typeMap = new IdentityHashMap<>();
    private final Map<Class<?>, Class<?>> typeShortcuts = new ConcurrentHashMap<>(20);

    private MultiTypeKeyFactory(Map<Class<?>, KeyFactory<?, ? extends K>> typeMap) {
        this.typeMap.putAll(typeMap);
    }

    MultiTypeKeyFactory() {

    }

    public <T> void add(Class<T> type, KeyFactory<? super T, ? extends K> factory) {
        typeMap.put(type, factory);
    }

    public static <K> MultiTypeKeyFactory.Builder<K> builder() {
        return new Builder<>();
    }

    @SuppressWarnings("unchecked")
    public <T> KeyFactory<? super T, ? extends K> get(Class<T> type) {
        return (KeyFactory<? super T, ? extends K>) typeMap.get(type);
    }

    public static final class Builder<K> {

        private final Map<Class<?>, KeyFactory<?, ? extends K>> typeMap = new IdentityHashMap<>();

        public <T> Builder<K> add(Class<T> type, KeyFactory<? super T, ? extends K> factory) {
            typeMap.put(type, factory);
            return this;
        }

        public KeyFactory<Object, K> build() {
            return new MultiTypeKeyFactory<>(typeMap);
        }
    }

    @Override
    public K constructKey(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Null key object");
        }
        return doConstructKey(obj);
    }

    @SuppressWarnings("unchecked")
    private <T> KeyFactory<? super T, ? extends K> searchSupertypesAndInterfaces(T obj) {
        Class<?> otype = obj.getClass();
        Class<?> shortcut = typeShortcuts.get(otype);
        if (shortcut != null) {
            return (KeyFactory<? super T, ? extends K>) typeMap.get(shortcut);
        }
        for (Class<?> iface : otype.getInterfaces()) {
            KeyFactory<?, ? extends K> kf = typeMap.get(iface);
            if (kf != null) {
                typeShortcuts.put(otype, iface);
                return (KeyFactory<? super T, ? extends K>) kf;
            }
        }
        for (Class<?> supertype = otype.getSuperclass(); supertype != null; supertype = supertype.getSuperclass()) {
            KeyFactory<?, ? extends K> kf = typeMap.get(supertype);
            if (kf != null) {
                typeShortcuts.put(otype, supertype);
                return (KeyFactory<? super T, ? extends K>) kf;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> KeyFactory<? super T, ? extends K> find(T obj) {
        KeyFactory<?, ? extends K> result = typeMap.get(obj.getClass());
        if (result == null) {
            result = searchSupertypesAndInterfaces(obj);
        }
        if (result == null) {
            throw new IllegalArgumentException("No key factory for " + obj);
        }
        return (KeyFactory<? super T, K>) result;
    }

    private <T> K doConstructKey(T obj) {
        KeyFactory<? super T, ? extends K> factory = find(obj);
        return factory.constructKey(obj);
    }
}
