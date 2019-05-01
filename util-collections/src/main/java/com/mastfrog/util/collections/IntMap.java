/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.collections;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Primitive int to object map.
 *
 * @author Tim Boudreau
 */
public interface IntMap<T> extends Iterable<Map.Entry<Integer, T>>, Map<Integer, T>, Serializable {

    boolean containsKey(int key);

    /**
     * Decrement keys in the map. Entries with negative keys will be removed.
     *
     * @param decrement Value the keys should be decremented by. Must be zero or
     * higher.
     */
    void decrementKeys(int decrement);

    Iterable<Map.Entry<Integer, T>> entries();

    T get(int key);

    int[] getKeys();

    int highestKey();

    Iterator<Integer> keysIterator();

    int lowestKey();

    int nearest(int key, boolean backward);

    T put(int key, T val);

    void set(int key, T val);

    @FunctionalInterface
    public interface IntMapConsumer<T> {

        void accept(int key, T value);
    }

    @FunctionalInterface
    public interface IntMapAbortableConsumer<T> {

        boolean accept(int key, T value);
    }

    default void forEachKey(IntConsumer cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            cons.accept(k[i]);
        }
    }

    default void forEach(IntMapConsumer<? super T> cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            T t = get(k[i]);
            cons.accept(k[i], t);
        }
    }

    default boolean forSomeKeys(IntMapAbortableConsumer<? super T> cons) {
        int[] k = getKeys();
        for (int i = 0; i < k.length; i++) {
            T t = get(k[i]);
            boolean result = cons.accept(k[i], t);
            if (!result) {
                return false;
            }
        }
        return true;
    }
}
