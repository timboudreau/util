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
public interface IOTriPredicate<In1, In2, In3> extends ThrowingTriPredicate<In1, In2, In3> {

    @Override
    boolean test(In1 a, In2 b, In3 c) throws IOException;

    default IOTriPredicate<In1, In2, In3> and(IOTriPredicate<? super In1, ? super In2, ? super In3> other) {
        return (a, b, c) -> {
            return this.test(a, b, c) && other.test(a, b, c);
        };
    }

    default IOTriPredicate<In1, In2, In3> or(IOTriPredicate<? super In1, ? super In2, ? super In3> other) {
        return (a, b, c) -> {
            return this.test(a, b, c) || other.test(a, b, c);
        };
    }

    default IOTriPredicate<In1, In2, In3> andNot(IOTriPredicate<? super In1, ? super In2, ? super In3> other) {
        return (a, b, c) -> {
            return this.test(a, b, c) && !other.test(a, b, c);
        };
    }

    default IOTriPredicate<In1, In2, In3> xor(IOTriPredicate<? super In1, ? super In2, ? super In3> other) {
        return (a, b, c) -> {
            return this.test(a, b, c) != other.test(a, b, c);
        };
    }
}
