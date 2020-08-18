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
package com.mastfrog.util.collections;

import java.util.ListIterator;

/**
 * A ListIterator which converts between types.  
 *
 * @author Tim Boudreau
 */
final class ConvertIterator<T, R> implements ListIterator<R> {
    private final ListIterator<T> orig;
    private final Converter<R,T> converter;
 
    ConvertIterator(ListIterator<T> orig, Converter<R,T> converter) {
        this.orig = orig;
        this.converter = converter;
    }

    @Override
    public boolean hasNext() {
        return orig.hasNext();
    }

    @Override
    public R next() {
        return converter.convert(orig.next());
    }

    @Override
    public boolean hasPrevious() {
        return orig.hasPrevious();
    }

    @Override
    public R previous() {
        return converter.convert(orig.previous());
    }

    @Override
    public int nextIndex() {
        return orig.nextIndex();
    }

    @Override
    public int previousIndex() {
        return orig.previousIndex();
    }

    @Override
    public void remove() {
        orig.remove();
    }

    @Override
    public void set(R e) {
        orig.set(converter.unconvert(e));
    }

    @Override
    public void add(R e) {
        orig.add(converter.unconvert(e));
    }

    @Override
    public String toString() {
        return "ConvertIterator with " + converter + " over " + orig;
    }
}
