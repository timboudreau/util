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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A pointless subclass of LinkedHashMap, entirely so that assorted libraries
 * that want a Java <i>type</i> have something to use without resorting
 * to TypeLiterals and TypeReferences and such.
 *
 * @author Tim Boudreau
 */
public class StringObjectMap extends LinkedHashMap<String,Object> {

    public StringObjectMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public StringObjectMap(int initialCapacity) {
        super(initialCapacity);
    }

    public StringObjectMap() {
    }

    public StringObjectMap(Map<? extends String, ?> m) {
        super(m);
    }

    public StringObjectMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public StringObjectMap add(String key, Object o) {
        put(key, o);
        return this;
    }
}
