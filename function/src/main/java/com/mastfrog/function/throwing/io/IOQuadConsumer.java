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
package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.ThrowingQuadConsumer;
import java.io.IOException;

/**
 * Like a BiConsumer but with four arguments, which can throw.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOQuadConsumer<A, B, C, D> extends ThrowingQuadConsumer<A, B, C, D> {

    void accept(A a, B b, C s, D u) throws IOException;

    default IOQuadConsumer<A, B, C, D> andThen(IOQuadConsumer<? super A, ? super B, ? super C, ? super D> other) {
        return (a, b, c, d) -> {
            this.accept(a, b, c, d);
            other.accept(a, b, c, d);
        };
    }
}
