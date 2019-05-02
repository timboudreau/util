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

import com.mastfrog.abstractions.Wrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Tim Boudreau
 */
final class ListTransformPredicate<T, R> implements NamedPredicate<T>, Wrapper<Predicate<R>> {

    private final NamedPredicate<R> orig;
    private final Function<T, List<? extends R>> xform;
    private final boolean and;

    public ListTransformPredicate(NamedPredicate<R> orig, Function<T, List<? extends R>> xform) {
        this(orig, xform, true);
    }

    public ListTransformPredicate(NamedPredicate<R> orig, Function<T, List<? extends R>> xform, boolean and) {
        this.orig = orig;
        this.xform = xform;
        this.and = and;
    }

    @Override
    public String name() {
        StringBuilder sb = new StringBuilder();
        LogicalListPredicate.ENTRY_COUNT.accept(depth -> {
//            char[] c = new char[depth * 2];
//            sb.append(c).append("as-list:\n");
            sb.append(orig.name());
        });
        return sb.toString();
    }

    @Override
    public Predicate<R> wrapped() {
        return orig;
    }

    @Override
    public boolean test(T t) {
        List<R> l = new ArrayList<>(xform.apply(t));
        return orig.toListPredicate(and).test(l);
    }
}
