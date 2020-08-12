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
import java.util.function.LongConsumer;

/**
 * A simple spliterator over an array that does not change.
 *
 * @author Tim Boudreau
 */
class ArrayLongSpliterator implements Spliterator.OfLong {

    private int curr;
    private int end;
    private final long[] objects;

    ArrayLongSpliterator(long[] objects) {
        curr = 0;
        end = objects.length;
        this.objects = objects;
    }

    ArrayLongSpliterator(long[] objects, int start, int end) {
        this.curr = start;
        this.end = end;
        this.objects = objects;
    }

    private boolean hasNext() {
        return curr < end;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public boolean tryAdvance(Consumer<? super Long> action) {
        if (hasNext()) {
            action.accept(objects[curr++]);
            return true;
        }
        return false;
    }

    @Override
    public ArrayLongSpliterator trySplit() {
        int remainder = end - curr;
        if (remainder < 2) {
            return null;
        }
        int half = remainder / 2;
        ArrayLongSpliterator result = new ArrayLongSpliterator(objects, curr + half, end);
        end = curr + half;
        return result;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void forEachRemaining(Consumer<? super Long> action) {
        while (curr < end) {
            action.accept(objects[curr++]);
        }
    }

    @Override
    public boolean tryAdvance(LongConsumer action) {
        if (hasNext()) {
            action.accept(objects[curr++]);
            return true;
        }
        return false;
    }

    @Override
    public void forEachRemaining(LongConsumer action) {
        while (curr < end) {
            action.accept(objects[curr++]);
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
