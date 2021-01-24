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
package com.mastfrog.function.throwing;

import com.mastfrog.function.ByteSupplier;
import com.mastfrog.util.preconditions.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ThrowingByteSupplier {

    byte getAsByte() throws Exception;

    /**
     * Convert to a non-throwing equivalent. Note that the resulting method
     * <i>will</i> rethrow any thrown checked exceptions.
     *
     * @return An equivalent function that does not declare the exceptions which
     * it throws
     */
    default ByteSupplier toNonThrowing() {
        return () -> {
            try {
                return getAsByte();
            } catch (Exception ex) {
                return Exceptions.chuck(ex);
            }
        };
    }

    default ThrowingIntSupplier toUnsignedIntSupplier() {
        return () -> {
            return getAsByte() & 0xFF;
        };
    }

    default ThrowingIntSupplier toSignedIntSupplier() {
        return () -> {
            return getAsByte();
        };
    }
}
