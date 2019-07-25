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

import com.mastfrog.file.channels.LeaseException;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

/**
 * Serializes and deserializes an object type to a file.
 *
 * @author Tim Boudreau
 */
public interface Serde<T> {

    /**
     * Write an object to the passed channel.
     *
     * @param <C> The type of the channel
     * @param obj An object to serialize
     * @param channel A channel
     * @throws IOException If something fails
     */
    <C extends WritableByteChannel & SeekableByteChannel> void serialize(T obj, C channel) throws IOException;

    /**
     * Deserialize one object from the passed channel and return it.
     *
     * @param <C> The type of channel
     * @param in A file path, for logging purposes
     * @param channel The file channel
     * @return An object deserialized from the channel
     * @throws LeaseException If deserialization fails
     * @throws IOException If something else goes wrong
     */
    <C extends ReadableByteChannel & SeekableByteChannel> T deserialize(Path in, C channel) throws LeaseException, IOException;

    /**
     * Version field implementations can use as they wish to compare with
     * written data.
     *
     * @return A version (default 0)
     */
    @SuppressWarnings("SameReturnValue")
    default int version() {
        return 0;
    }

    /**
     * Convenience method to throw a LeaseException which will be populated with
     * information about the calling context's Lease.
     *
     * @param <Ignored> Whatever type the caller is expected to return, so any
     * method can call <code>return fail();</code> in the event no reasonable
     * result can be returned.
     * @return nothing, an exception is thrown
     * @throws LeaseException
     */
    default <Ignored> Ignored fail() throws LeaseException {
        throw new LeaseException();
    }

    /**
     * Convenience method to throw a LeaseException which will be populated with
     * information about the calling context's Lease.
     *
     * @param <Ignored> Whatever type the caller is expected to return, so any
     * method can call <code>return fail();</code> in the event no reasonable
     * result can be returned.
     * @param message The exception message
     * @return nothing, an exception is thrown
     * @throws LeaseException
     */
    default <Ignored> Ignored fail(String message) throws LeaseException {
        throw new LeaseException(message);
    }

    /**
     * Convenience method to throw a LeaseException which will be populated with
     * information about the calling context's Lease.
     *
     * @param <Ignored> Whatever type the caller is expected to return, so any
     * method can call <code>return fail();</code> in the event no reasonable
     * result can be returned.
     * @param message The exception message
     * @param cause The cause to initialize the exception with
     * @return nothing, an exception is thrown
     * @throws LeaseException
     */
    default <Ignored> Ignored fail(String message, Throwable cause) throws LeaseException {
        throw new LeaseException(message, cause);
    }

    /**
     * Convenience method to throw a LeaseException which will be populated with
     * information about the calling context's Lease.
     *
     * @param <Ignored> Whatever type the caller is expected to return, so any
     * method can call <code>return fail();</code> in the event no reasonable
     * result can be returned.
     * @return nothing, an exception is thrown
     * @throws LeaseException
     */
    default <Ignored> Ignored fail(Throwable cause) throws LeaseException {
        throw new LeaseException(cause);
    }
}
