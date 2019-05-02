/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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
package com.mastfrog.abstractions;

import java.util.Iterator;

/**
 * Interface for things that have namesIterable.
 *
 * @author Tim Boudreau
 */
public interface Named {

    String name();

    default void appendTo(StringBuilder sb) {
        sb.append(name());
    }

    static Named fromEnumConstant(Enum<?> constant) {
        return constant::name;
    }

    static <T extends Named> Iterable<String> namesIterable(Iterable<T> objs) {
        return new NamesIterable<>(objs);
    }

    static <T extends Named> Iterator<String> namesIterator(Iterator<T> iter) {
        return new NamesIterable.NamesIterator<>(iter);
    }

    static String findName(Object o) {
        if (o instanceof Named) {
            return ((Named) o).name();
        }
        if (o instanceof Wrapper<?>) {
            Named n = ((Wrapper<?>) ((Wrapper<?>) o)).find(Named.class);
            if (n != null) {
                return n.name();
            }
        }
        return o == null ? "null" : o.toString();
    }
}
