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

/**
 * Appends elements to a log-structured storage.
 *
 * @author Tim Boudreau
 */
public interface LogStructuredAppender<T> extends AutoCloseable {

    /**
     * Append an item, persisting it.
     *
     * @param env An item
     * @return The item added, unaltered
     * @throws IOException
     */
    T append(T env) throws IOException;

    /**
     * Returns true if the underlying file or other storage is open.
     *
     * @return True if the storage is writable
     */
    boolean isOpen();

    /**
     * Add a runnable to call after each write (to, say, trigger an asynchronous
     * reader on another thread that may be positioned at the previous tail of
     * the file). If this method is called multiple times, all passed runnables
     * will be invoked on a write.
     *
     * @param onWrite A runnable
     */
    void onWrite(Runnable onWrite);

    /**
     * Get the size in bytes of the backing storage for this storage.
     *
     * @return A size, or zero if empty
     * @throws IOException If something goes wrong
     */
    long size() throws IOException;

    /**
     * Close this appender, indicating that no further items will be written and
     * any associated file descriptors may safely be closed as far is this
     * appender is concerned.
     *
     * @throws IOException If something goes wrong
     */
    void close() throws IOException;
}
