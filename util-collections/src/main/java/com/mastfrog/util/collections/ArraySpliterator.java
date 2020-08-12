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

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A simple spliterator over an array that does not change.
 *
 * @author Tim Boudreau
 */
class ArraySpliterator<T> implements Spliterator<T> {

    private int curr;
    private int end;
    private final Object[] objects;

    ArraySpliterator(Object[] objects) {
        curr = 0;
        end = objects.length;
        this.objects = objects;
    }

    ArraySpliterator(Object[] objects, int start, int end) {
        this.curr = start;
        this.end = end;
        this.objects = objects;
    }

    private boolean hasNext() {
        return curr < end;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public boolean tryAdvance(Consumer<? super T> action) {
        if (hasNext()) {
            action.accept((T) objects[curr++]);
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {
        int remainder = end - curr;
        if (remainder < 2) {
            return null;
        }
        int half = remainder / 2;
        Spliterator<T> result = new ArraySpliterator<>(objects, curr + half, end);
        end = curr + half;
        return result;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void forEachRemaining(Consumer<? super T> action) {
        while (curr < end) {
            action.accept((T) objects[curr++]);
        }
    }

    @Override
    public long estimateSize() {
        return end - curr;
    }

    @Override
    public long getExactSizeIfKnown() {
        return estimateSize();
    }

    @Override
    public int characteristics() {
        return DISTINCT | IMMUTABLE | SIZED | ORDERED;
    }
}
