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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A better map builder.
 *
 * @author Tim Boudreau
 */
final class MapBuilder2Impl<T, R> implements MapBuilder2<T, R> {

    private final Map<T, R> data = new LinkedHashMap<>();

    @Override
    public ValueBuilder<T, R> map(T key) {
        return new ValueBuilderImpl<>(key, this);
    }

    @Override
    public Map<T, R> build() {
        return new HashMap<>(data);
    }

    @Override
    public Map<T, R> buildLinkedHashMap() {
        return new LinkedHashMap<>(data);
    }

    @Override
    public Map<T, R> buildImmutableMap() {
        if (data.isEmpty()) {
            return Collections.emptyMap();
        }
        if (data.size() == 1) {
            Map.Entry<T, R> e = data.entrySet().iterator().next();
            return Collections.singletonMap(e.getKey(), e.getValue());
        }
        return Collections.unmodifiableMap(build());
    }

    static class ValueBuilderImpl<T, R> implements ValueBuilder<T, R> {

        private final T key;
        private final MapBuilder2Impl<T, R> parent;

        ValueBuilderImpl(T key, MapBuilder2Impl<T, R> parent) {
            this.key = key;
            this.parent = parent;
        }

        @Override
        public MapBuilder2<T, R> to(R value) {
            parent.data.put(key, value);
            return parent;
        }

        @Override
        public Map<T, R> finallyTo(R value) {
            return to(value).build();
        }
    }
}
