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
final class DelegatingNotifier<K, E> implements SubscribableNotifier<K, E> {

    private SubscribableNotifier<? super K, ? super E> delegate;

    void setDelegate(SubscribableNotifier<? super K, ? super E> delegate) {
        assert this.delegate == null;
        this.delegate = delegate;
    }

    @Override
    public void onEvent(K key, E event) {
        delegate.onEvent(key, event);
    }

}
