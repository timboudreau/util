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
package com.mastfrog.util.strings;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;

/**
 * A view of a String or any CharSequence as an input stream that does
 * not require making an in-memory copy of the bytes - a settable buffer
 * size determines the amount of encoded bytes that are cached, and the
 * cache is refilled as needed until the characters have been exhausted.
 *
 * @author Tim Boudreau
 */
public abstract class CharSequenceInputStream extends InputStream {

    CharSequenceInputStream() {

    }

    /**
     * Create a new UTF-8 CharSequenceInputStream.
     *
     * @param toRead A string
     * @return A CharSequenceInputStream that wraps the original CharSequence
     * without (assuming a small buffer size) making a complete copy of the
     * original in memory.
     */
    public static CharSequenceInputStream asInputStream(CharSequence toRead) {
        return new CharSequenceInputStreamImpl(toRead);
    }

    /**
     * Create a new input stream with the specified encoding.
     *
     * @param toRead A string
     * @param encoding An encoding
     * @return A CharSequenceInputStream that wraps the original CharSequence
     * without (assuming a small buffer size) making a complete copy of the
     * original in memory.
     */
    public static CharSequenceInputStream asInputStream(CharSequence toRead,
            Charset encoding) {
        return new CharSequenceInputStreamImpl(toRead, encoding);
    }

    /**
     * Create a new input stream with the specified encoding, with a specific
     * buffer size for how many bytes from the string should be buffered during
     * the writing process.
     *
     * @param toRead A string
     * @param encoding An encoding
     * @param outBufferSize A buffer size - if &lt;= 0, the buffer size will
     * based on the character set's average bytes per character to accommodate
     * the entire string
     * @return A CharSequenceInputStream that wraps the original CharSequence
     * without (assuming a small buffer size) making a complete copy of the
     * original in memory.
     */
    public static CharSequenceInputStream asInputStream(CharSequence toRead,
            Charset encoding, int outBufferSize) {
        return new CharSequenceInputStreamImpl(toRead, encoding, outBufferSize);
    }

    /**
     * Create a new input stream with the specified encoding, with a specific
     * buffer size for how many bytes from the string should be buffered during
     * the writing process.
     *
     * @param toRead A string
     * @param encoding An encoding
     * @param outBufferSize A buffer size - if &lt;= 0, the buffer size will
     * based on the character set's average bytes per character to accommodate
     * the entire string
     * @param errorBehavior What to do if unmappable characters are encountered
     * - omit them, use a substitute character, or throw an
     * IllegalStateException to wrap the original CoderResult's exception
     * @return A CharSequenceInputStream that wraps the original CharSequence
     * without (assuming a small buffer size) making a complete copy of the
     * original in memory.
     */
    public static CharSequenceInputStream asInputStream(CharSequence toRead,
            Charset encoding, int outBufferSize, EncodingErrorBehavior errorBehavior) {
        return new CharSequenceInputStreamImpl(toRead, encoding, outBufferSize,
                errorBehavior);
    }

    /**
     * Rewind this stream to its start.
     *
     * @return this
     */
    public abstract CharSequenceInputStream rewind();

    /**
     * How the stream should behave if characters in the string are not
     * encodable in the target character set.
     */
    public enum EncodingErrorBehavior {
        /**
         * Omit unencodable characters.
         */
        OMIT,
        /**
         * Replace unencodable characters with an encodable substitute.
         */
        REPLACE,
        /**
         * Throw an exception on encoding failure (the exception will be that
         * thrown by {@link CoderResult#throwException() }).
         */
        THROW;
    }

}
