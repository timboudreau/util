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

package com.mastfrog.predicates;

import com.mastfrog.abstractions.Named;
import com.mastfrog.abstractions.Wrapper;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
class ListConvertedPredicate<T> implements Predicate<Iterable<T>>, Wrapper<Predicate<? super T>> {

    final Predicate<? super T> orig;
    private final boolean and;

    public ListConvertedPredicate(Predicate<? super T> orig, boolean and) {
        this.orig = orig;
        this.and = and;
    }

    @Override
    public boolean test(Iterable<T> t) {
        boolean result = and;
        for (T obj : t) {
            if (and) {
                result &= orig.test(obj);
                if (!result) {
                    break;
                }
            } else {
                result |= orig.test(obj);
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return Named.findName(orig);
    }

    @Override
    public Predicate<? super T> wrapped() {
        return orig;
    }

}
