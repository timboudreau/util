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
