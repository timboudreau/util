/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
