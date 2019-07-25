/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.logstructured;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Represents a read of one record from a log structured file; if the advance()
 * method is called, the file will return the next item (if any) on subsequent
 * calls to read().
 *
 * @author Tim Boudreau
 */
public interface UnadvancedRead<T> extends Supplier<T> {

    /**
     * Mark the item wrapped by this UnadvancedRead as consumed (and potentially
     * safe to delete) - only call this after you are <i>sure</i> the item is
     * safely stored elsewhere or consumed in some fashion such that its loss is
     * acceptable.
     *
     * @throws IOException
     */
    void advance() throws IOException;

    /**
     * Mark the item wrapped by this UnadvancedRead as <i>not consumed</i> - it
     * should not be deleted and use of it may be attempted again.
     *
     * @throws IOException
     */
    void rollback() throws IOException;

    /**
     * Advance the underlying channel (lease in a FileChannelPool) to the next
     * item, marking this read as consumed.
     *
     * @return True if this was the current item
     * @throws IOException
     */
    default boolean advanceIfCurrent() throws IOException {
        if (isCurrent()) { // XXX this still can race
            advance();
            return true;
        }
        return false;
    }

    /**
     * Roll back the underlying channel (lease in a FileChannelPool) to the next
     * item, marking this read as discarded.
     *
     * @return True if this was the current item
     * @throws IOException
     */
    default boolean rollbackIfCurrent() throws IOException {
        if (isCurrent()) { // XXX this still can race
            rollback();
            return true;
        }
        return false;
    }

    /**
     * Determine if this is the most recently read item from the backing
     * storage, or if another read has been attempted since this one was
     * created.
     *
     * @return true if this object represents the most recent read
     */
    boolean isCurrent();
}
