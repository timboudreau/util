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
import java.util.function.Consumer;

/**
 * Reads a log structured data source and updates a persistent cursor when a
 * read is committed.  Items <u>must</u> be dealt with one-by-one, sequentially
 * (and preferably on a single thread).
 *
 * @author Tim Boudreau
 */
public interface LogStructuredReader<T> extends AutoCloseable {

    /**
     * Close this reader. Depending on the implementation, it may be reopened if
     * asked to read again.
     *
     * @throws IOException If something goes wrong
     */
    void close() throws IOException;

    /**
     * Close this reader, and, if the cursor is advanced to the end of the file,
     * delete the file; if not, move the beginning of the file to the current
     * cursor position.
     *
     * @throws IOException If something goes wrong
     */
    void deleteIfAllReadAndAdvanced() throws IOException;

    /**
     * Move the beginning of the file to the current cursor position.
     *
     * @throws IOException If something goes wrong
     */
    void discardCommitted() throws IOException;

    /**
     * If this method returns true, the persistent cursor is not at the end of
     * the file, so there are more records to read.
     *
     * @return True if there are unread items
     * @throws IOException
     */
    boolean hasUnread() throws IOException;

    /**
     * Read one element, returning an UnadvancedRead instance which can be
     * committed / advanced once the item is processed and is eligable for
     * discard.  <i>The cursor is not advanced unless the <code>advance()</code>
     * method is called on this returned item.</i> If not advanced, subsequent
     * read operations will return a new copy of the same element, and cause any
     * previously retrieved instances to throw an exception if an attempt is
     * made to advance it.
     *
     * @return An UnadvancedRead
     * @throws IOException If something goes wrong
     */
    UnadvancedRead<T> read() throws IOException;

    /**
     * Read the next item, advancing the persistent cursor
     *
     * @return An item
     * @throws IOException If something goes wrong
     */
    T readAndAdvance() throws IOException;

    /**
     * Read the next item, calling the passed consumer, and advancing the
     * persistent cursor if the consumer does not throw an exception.
     *
     * @return True if an item was read and the consumer called
     * @throws IOException If something goes wrong
     */
    boolean readAndAdvance(Consumer<T> consumer) throws IOException;

    /**
     * Move the persistent cursor position back to the beginning of the storage
     * and commit that change to the persistent cursor.
     *
     * @throws IOException If something goes wrong
     */
    void rewind() throws IOException;
}
