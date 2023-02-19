/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Flips between two instances of type T returned by the get() method. This is
 * useful for cases such as statistics collection, where you may allocate and
 * want to reuse very large data structures, and periodically switch to a a
 * second T for collecting subsequent data while emitting data collected in the
 * first one. This class is lockless.
 * <p>
 * This class is subject to one race condition: After a call to flip(),
 * retrieval of data from the now not-in-use instance must complete before the
 * next call to flip() occurs. Since this class is typically used for data
 * collectors with a period greater than or equal to a minute, this is generally
 * a non-problem, but may be encountered in cases where a machine is near
 * resource exhaustion.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class FlipFlop<T> {

    private final T a;
    private final T b;
    private final Consumer<T> resetter;
    private final AtomicInteger flipFlop = new AtomicInteger();

    public FlipFlop(T a, T b, Consumer<T> resetter) {
        this.a = a;
        this.b = b;
        this.resetter = resetter;
    }

    /**
     * Get the currently in-use instance of T.
     *
     * @return A T
     */
    public T get() {
        return get(flipFlop.get());
    }

    /**
     * Get the currently in use collector.
     *
     * @param val The index - must be one or 0
     * @return A collector
     */
    private T get(int val) {
        switch (val) {
            case 0:
                return a;
            case 1:
                return b;
            default:
                throw new AssertionError(val);
        }
    }

    /**
     * Switch to the other instance of T this FlipFlop provides, for future
     * calls to <code>get()</code>, returning the one which was formerly in use.
     * The resetter is called on the new value.
     *
     * @return The T which is no longer in use
     */
    public T flip() {
        // Flip the int value
        int prev = flipFlop.getAndUpdate(old -> {
            switch (old) {
                case 0:
                    return 1;
                case 1:
                    return 0;
                default:
                    throw new AssertionError(old);
            }
        });
        // get the opposite collector
        T old = get(prev);
//            System.out.println("FLIP " + prev + " ret " + old + " for " + operation);
        // reset the other collector
        switch (prev) {
            case 0:
                assert old != b;
                resetter.accept(b);
                break;
            case 1:
                assert old != a;
                resetter.accept(a);
                break;
            default:
                throw new AssertionError();
        }
        return old;
    }

    @Override
    public String toString() {
        return "FlipFlop(" + get() + ")";
    }

}
