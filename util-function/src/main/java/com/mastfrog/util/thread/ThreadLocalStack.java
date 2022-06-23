/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.thread;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * A stack stored in a ThreadLocal, with the ability to use AutoCloseable to
 * push an object onto the stack and have it removed at the end of a block.
 *
 * @author Tim Boudreau
 * @deprecated Use the richer, more modern version in com.mastfrog.function.threadlocal
 */
@Deprecated
public class ThreadLocalStack<T> {

    private final FactoryThreadLocal<LinkedList<T>> stack = new FactoryThreadLocal<>(() -> new LinkedList<>());
    private final Closer closer = new Closer();

    public QuietAutoCloseable push(T obj) {
        stack.get().push(obj);
        return closer;
    }

    public T peek() {
        return !stack.hasValue() ? null : stack.get().peek();
    }

    public boolean isEmpty() {
        return !stack.hasValue() || stack.get().isEmpty();
    }

    public T pop() {
        return stack.get().pop();
    }

    public int depth() {
        return stack.get().size();
    }

    public T with(T obj, Consumer<T> cons) {
        stack.get().push(obj);
        try {
            cons.accept(obj);
        } finally {
            stack.get().pop();
        }
        return obj;
    }

    public T peek(int distanceFromTop) {
        if (stack.hasValue()) {
            LinkedList<T> obj = stack.get();
            int offset = (obj.size() - 1) - distanceFromTop;
            if (offset < 0) {
                throw new IllegalArgumentException("Offset " + offset + " for dist " + distanceFromTop);
            }
            return obj.get(offset);
        }
        return null;
    }

    private final class Closer implements QuietAutoCloseable {

        @Override
        public void close() {
            if (!stack.hasValue()) {
                throw new IllegalStateException("Close called asymmetrically");
            }
            stack.get().pop();
        }
    }
}
