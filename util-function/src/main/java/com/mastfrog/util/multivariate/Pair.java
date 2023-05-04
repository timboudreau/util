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

import com.mastfrog.function.throwing.ThrowingBiFunction;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author Tim Boudreau
 */
public final class Pair<A, B> {

    public final A a;
    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.a);
        hash = 89 * hash + Objects.hashCode(this.b);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (!Objects.equals(this.a, other.a)) {
            return false;
        }
        if (!Objects.equals(this.b, other.b)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return a + "," + b;
    }

    public <C> C with(ThrowingBiFunction<A, B, C> func) throws Exception {
        return func.apply(a, b);
    }

    public <C> C apply(BiFunction<A, B, C> func) {
        return func.apply(a, b);
    }

    public Object[] toArray() {
        return new Object[]{a, b};
    }

    public static <A, B> Function<B, Pair<A, B>> from(A a) {
        return (B b) -> {
            return new Pair<A, B>(a, b);
        };
    }

    public <C> Pair<A, C> transformB(Function<B, C> func) {
        return new Pair<A, C>(a, func.apply(b));
    }

    public <C> Pair<C, B> transformA(Function<A, C> func) {
        return new Pair<C, B>(func.apply(a), b);
    }
}
