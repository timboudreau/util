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
interface EventMap<K, E> {

    void put(K k, E e);

    void remove(K k);

    E get(K k);

}
