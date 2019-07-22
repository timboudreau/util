/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.function;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Just a consumer which merges two others, but which implements toString()
 * sanely for logging purposes.
 *
 * @author Tim Boudreau
 */
final class AndedConsumer<T> implements LoggableConsumer<T> {

    private final Consumer<? super T> a;
    private final Consumer<? super T> b;

    public AndedConsumer(Consumer<? super T> a, Consumer<? super T> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return a + " & " + b;
    }

    @Override
    public void accept(T t) {
        a.accept(t);
        b.accept(t);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.a);
        hash = 59 * hash + Objects.hashCode(this.b);
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
        final AndedConsumer<?> other = (AndedConsumer<?>) obj;
        if (!Objects.equals(this.a, other.a)) {
            return false;
        }
        return Objects.equals(this.b, other.b);
    }

    static LoggableConsumer NO_OP = new Noop();

    static class Noop implements LoggableConsumer {

        @Override
        public void accept(Object t) {
            // do nothing
        }

        @Override
        public Consumer andThen(Consumer after) {
            return after;
        }


    }
}
