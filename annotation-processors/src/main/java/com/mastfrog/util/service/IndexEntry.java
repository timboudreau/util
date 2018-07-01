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
package com.mastfrog.util.service;

import javax.lang.model.element.Element;

/**
 * An entry in an index written to disk by an IndexGeneratingProcessor.  <b>Note:</b>
 * the equals() and hashCode() test must reflect the data to be written sans
 * any ordering constraint, or duplicate entries will be created.
 *
 * @author Tim Boudreau
 */
public interface IndexEntry extends Comparable<IndexEntry> {

    /**
     * The source elements this entry is associated with.
     *
     * @return The elements
     */
    Element[] elements();

    /**
     * Associate additional elements with this entry (used for coalescing duplicates).
     *
     * @param els The elements to add
     */
    void addElements(Element... els);

}
