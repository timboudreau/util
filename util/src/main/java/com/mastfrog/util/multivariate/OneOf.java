/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.multivariate;

import static com.mastfrog.util.Checks.notNull;
import java.util.function.Function;

/**
 * Wrapper for two objects of different types which can be resolved to a single
 * object.
 *
 * @author Tim Boudreau
 */
public class OneOf<A, B> {

    private A a;
    private B b;

    public OneOf(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public OneOf() {

    }

    public static <A, B> OneOf<A, B> ofA(A a) {
        return new OneOf<>(notNull("a", a), null);
    }

    public static <A, B> OneOf<A, B> ofB(B b) {
        return new OneOf<>(null, notNull("b", b));
    }

    OneOf setA(A a) {
        this.a = notNull("a", a);
        return this;
    }

    OneOf setB(B b) {
        this.b = notNull("b", b);
        return this;
    }

    public Object either() {
        return a == null ? b : a;
    }

    public boolean isSet() {
        return a != null || b != null;
    }

    public <T> T get(Function<A, T> afetch, Function<B, T> bfetch) {
        if (a != null) {
            return afetch.apply(a);
        } else if (b != null) {
            return bfetch.apply(b);
        } else {
            throw new IllegalStateException("Neither a nor b set;");
        }
    }

    public A get(Function<B, A> convert) {
        if (a != null) {
            return a;
        } else if (b != null) {
            return notNull("convert", convert).apply(b);
        } else {
            throw new IllegalStateException("Neither a nor b set");
        }
    }

    @Override
    public String toString() {
        return "OneOf(a=" + a + ", b=" + b + ")";
    }
}
