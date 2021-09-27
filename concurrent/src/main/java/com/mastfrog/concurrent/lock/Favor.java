/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.concurrent.lock;

/**
 * When acuqiring an ad-hoc set of locks/permits, determines whether the least
 * available, greatest available, or alternating least and greatest are
 * preferred.
 * <p>
 * When contention is likely, if you know what you're contending with prefers
 * high bits or low bits, then preferring the opposite reduces spins and
 * blocking.
 * </p>
 */
public enum Favor {
    /**
     * Favor the least unset bits from acquireN() methods which lock arbitrary
     * bits if sufficient are available.
     */
    LEAST, /**
     * Favor the greatest unset bits from acquireN() methods which lock
     * arbitrary bits if sufficient are available.
     */
    GREATEST, /**
     * Favor the alternately least and most significant available bits from
     * acquireN() methods which lock arbitrary bits if sufficient are available.
     */
    BOTH;
    /**
     * Returns a long with one bit set, the least or greatest depending on which
     * enum constant this is, and for BOTH, whether the iteration value is odd
     * or even.
     *
     * @param value A value with some bits set
     * @param iter An interation index
     * @return A value with one of the available bits set
     */
    long favor(long value, int iter) {
        switch (this) {
            case LEAST:
                return Long.lowestOneBit(value);
            case GREATEST:
                return Long.highestOneBit(value);
            case BOTH:
                if ((iter % 2) == 0) {
                    return Long.highestOneBit(value);
                } else {
                    return Long.lowestOneBit(value);
                }
            default:
                throw new AssertionError(this);
        }
    }
}
