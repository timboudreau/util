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
package com.mastfrog.util.sort;

import com.mastfrog.util.preconditions.Checks;
import java.util.List;

/**
 * Interface for sorting multiple collections (or whatever) in tandem with
 * sorting an array of numbers; is passed the indices of elements whose
 * positions must be swapped to match the new sort order as sorting progresses.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface Swapper {

    /**
     * Swap the element at <code>index1</code> with the element at      <code>index2<code>.
     *
     * @param index1 The first index, != index2
     * @param index2 The second index, != index1
     */
    void swap(int index1, int index2);

    /**
     * Create a generic swapper for object arrays.
     *
     * @param <T> The array element type
     * @param array The array, non-null
     * @return A swapper
     */
    static <T> Swapper forArray(T[] array) {
        return new ArraySwapper<>(Checks.notNull("array", array));
    }

    /**
     * Create a generic swapper for lists.
     *
     * @param <T> The list's parameter type
     * @param list The list, non-null
     * @return A swapper
     */
    static <T> Swapper forList(List<T> list) {
        return new ListSwapper<>(Checks.notNull("list", list));
    }
}
