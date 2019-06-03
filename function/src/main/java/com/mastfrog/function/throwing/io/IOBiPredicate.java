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
package com.mastfrog.function.throwing.io;

import com.mastfrog.function.throwing.*;
import java.io.IOException;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface IOBiPredicate<In1, In2> extends ThrowingBiPredicate<In1, In2> {

    @Override
    boolean test(In1 a, In2 b) throws IOException;

    default IOBiPredicate<In1, In2> and(IOBiPredicate<? super In1, ? super In2> other) {
        return (a, b) -> {
            return this.test(a, b) && other.test(a, b);
        };
    }

    default IOBiPredicate<In1, In2> or(IOBiPredicate<? super In1, ? super In2> other) {
        return (a, b) -> {
            return this.test(a, b) || other.test(a, b);
        };
    }

    default IOBiPredicate<In1, In2> andNot(IOBiPredicate<? super In1, ? super In2> other) {
        return (a, b) -> {
            return this.test(a, b) && !other.test(a, b);
        };
    }

    default IOBiPredicate<In1, In2> xor(IOBiPredicate<? super In1, ? super In2> other) {
        return (a, b) -> {
            return this.test(a, b) != other.test(a, b);
        };
    }
}
