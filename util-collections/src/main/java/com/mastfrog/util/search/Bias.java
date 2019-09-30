/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util.search;

/**
 * Bias used to decide how to resolve ambiguity, for example in a binary search
 * with a list of timestamps - if the requested timestamp lies between two
 * actual data snapshots, should we return the next, previous, nearest one, or
 * none unless there is an exact match.
 *
 * @author Tim Boudreau
 */
public enum Bias {

    /**
     * If a search result falls between two elements, prefer the next element.
     */
    FORWARD,
    /**
     * If a search result falls between two elements, prefer the previous
     * element.
     */
    BACKWARD,
    /**
     * If a search result falls between two elements, prefer the element with
     * the minimum distance.
     */
    NEAREST,
    /**
     * If a search result falls between two elements, return no element unless
     * there is an exact match.
     */
    NONE;

    /**
     * Get the inverse of this bias (only meaningful for forward and backward).
     *
     * @return The inverse bias, if that means anything for this instance
     */
    public Bias inverse() {
        switch (this) {
            case FORWARD:
                return BACKWARD;
            case BACKWARD:
                return FORWARD;
            default:
                return this;
        }
    }
}
