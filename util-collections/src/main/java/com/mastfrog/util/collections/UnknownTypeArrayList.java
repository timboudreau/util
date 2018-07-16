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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
class UnknownTypeArrayList extends AbstractList<Object> {

    private final Object array;

    public UnknownTypeArrayList(Object array) {
        this.array = notNull("array", array);
        if (!array.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array: " + Objects.toString(array));
        }
    }

    @Override
    public Object get(int index) {
        return Array.get(array, index);
    }

    @Override
    public int size() {
        return Array.getLength(array);
    }
}
