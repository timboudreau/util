/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.subscription;

/**
 *
 * @author Tim Boudreau
 */
public interface SubscribersStoreController<K, C> {

    boolean add(K key, C subscriber);

    boolean remove(K key, C subscriber);

    void removeAll(K key);

    void clear();

    default <XK> SubscribersStoreController<XK, C> converted(KeyFactory<? super XK, ? extends K> factory) {
        return new SubscribersStoreController<XK, C>() {
            @Override
            public boolean add(XK key, C subscriber) {
                return SubscribersStoreController.this.add(factory.constructKey(key), subscriber);
            }

            @Override
            public boolean remove(XK key, C subscriber) {
                return SubscribersStoreController.this.remove(factory.constructKey(key), subscriber);
            }

            @Override
            public void removeAll(XK key) {
                SubscribersStoreController.this.removeAll(factory.constructKey(key));
            }

            @Override
            public void clear() {
                SubscribersStoreController.this.clear();
            }
        };
    }

}
