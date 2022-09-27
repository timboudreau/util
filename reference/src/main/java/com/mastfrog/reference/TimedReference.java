/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.reference;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Interface for timed references, which adds a few convenience methods to the
 * raw java.lang.Reference interface.
 *
 * @author Tim Boudreau
 */
public interface TimedReference<T> {

    /**
     * Get the referent of this reference.
     *
     * @return A referent if one is available
     */
    T get();

    /**
     * Force this object to weak reference status and expire it (note that a
     * subsequent call to <code>get()</code> can revive it and restore it to
     * strong reference status).
     */
    void discard();

    /**
     * Get the object if it is non-null and passes the test of the pssed
     * predicate; does not update the expiration delay or return this reference
     * to strong status unless the predicate returns true, so objects which are
     * obsolete for some reason other than age do not get garbage collected even
     * later due to testing for obsolescence.
     *
     * @param pred A predicate
     *
     * @return The referenced object, if it still exists and if it passes the
     * predicate's test; otherwise null
     */
    T getIf(Predicate<T> pred);

    /**
     * Invoke the passed consumer with the referent if it has not been garbage
     * collected. This method will reset the expiration timer and return this
     * TimedWeakReference to strongly referencing the referent for a new timeout
     * interval, if the referent is available.
     *
     * @param consumer A consumer
     * @return true if the value was non-null and the consumer was invoked
     */
    default boolean ifPresent(Consumer<T> consumer) {
        T obj = get();
        boolean result = obj != null;
        if (result) {
            consumer.accept(obj);
        }
        return result;
    }

}
