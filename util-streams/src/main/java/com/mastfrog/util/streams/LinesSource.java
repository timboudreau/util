/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.util.streams;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
public interface LinesSource extends AutoCloseable {

    /**
     * Close this stream, returning any already read lines and
     * any partial line for which a newline had not yet been
     * encountered; if at the end of the file, this will be the
     * tail.
     *
     * @return A list of any remaining strings
     */
    List<CharSequence> closeReturningTail() throws IOException;

    /**
     * Determine if there are currently any more lines available.
     *
     * @return True if there are more lines
     * @throws IOException If something goes wrong
     */
    boolean hasMoreLines() throws IOException;

    boolean isAtEndOfFile() throws IOException;

    boolean isOpen();

    /**
     * Get the next line, if any.
     *
     * @return The next line, or null if there are none
     * @throws IOException If something goes wrong
     */
    CharSequence nextLine() throws IOException;

    /**
     * Get the current position in the file
     *
     * @return The file position
     * @throws IOException If the channel is closed or something else is wrong
     */
    long position() throws IOException;

    /**
     * Change the position for reading in the file, discarding any
     * enqueued partial or complete lines.
     *
     * @param pos The new position
     * @throws IOException If the position is invalid or something else is wrong
     */
    void position(long pos) throws IOException;

}
