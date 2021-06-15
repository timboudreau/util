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
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * Abstraction for a stream-like construct which can decode char sequences
 * from an underlying data source with a provided decoder and minimal
 * memory-copies.
 *
 * @author Tim Boudreau
 */
public interface CharSequenceSource extends AutoCloseable {

    /**
     * Decode whatever characters are available into the passed CharBuffer. Note
     * that for multi-byte encodings, CharsetDecoders are stateful, and a
     * previous call could result in being at a byte-position that's part-way
     * through reading a character. Always pass the same decoder unless a
     * decoding error has occurred.
     *
     * @param target The charbuffer to decode results into
     * @param charsetDecoder A decoder for the desired charset
     * @return The result of decoding
     * @throws IOException If something goes wrong
     */
    CoderResult decode(CharBuffer target, CharsetDecoder charsetDecoder) throws IOException;

    /**
     * Get the postion the next read will come from
     *
     * @return The position in the file
     * @throws IOException
     */
    long position() throws IOException;

    /**
     * Change the position
     *
     * @param pos
     * @throws IOException
     */
    void position(long pos) throws IOException;

}
