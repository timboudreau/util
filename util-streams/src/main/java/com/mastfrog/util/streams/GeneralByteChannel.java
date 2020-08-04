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

package com.mastfrog.util.streams;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Encapsulates ReadableByteChannel, SeekableByteChannel, WritableByteChannel
 * to have a single type to return from Streams.channel() which is compatible
 * with APIs that take the types implemented by FileChannel.
 *
 * @author Tim Boudreau
 */
public interface GeneralByteChannel extends ReadableByteChannel, SeekableByteChannel, WritableByteChannel{

    @Override
    GeneralByteChannel truncate(long size) throws IOException;

    @Override
    GeneralByteChannel position(long newPosition) throws IOException;

    /**
     * Returns the same value as position(), but as an int, since Java arrays
     * do not support &gt; Integer.MAX_VALUE arrays, so it will always fit.
     *
     * @return The position of the channel at present
     */
    int cursorPosition();

    /**
     * Reset the channel's position and closed state (if possible); useful for
     * avoiding copies in the case of a channel that is used for writes and then
     * reads.
     *
     * @return The channel position at the time of reset
     * @throws UnsupportedOperationException if unsupported
     */
    int reset();
}
