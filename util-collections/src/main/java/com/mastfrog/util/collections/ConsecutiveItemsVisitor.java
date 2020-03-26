/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.util.collections;

/**
 * In set-like interfaces, visits the indices of values which are consecutive -
 * for use in bulk-copy operations.
 */
public interface ConsecutiveItemsVisitor {

    /**
     * Called repeatedly for groups of <i>indices</i> within a collection which
     * are consecutive (whatever that means for the particular collection type).
     *
     * @param first The first index of a run of consecutive items
     * @param last The last index (inclusive0 in a run of consecutive items
     * @param count The number of items, equal to
     * <code>(last - first) + 1</code> for convenience
     */
    void items(int first, int last, int count);
}
