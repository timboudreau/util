/*
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Utilities methods for working with input and output streams.
 * @deprecated All methods now delegate to com.mastfrog.util.streams.Streams
 */
@Deprecated
public final class Streams {

    /**
     * Writes the input stream to the output stream. Input is done without a
     * Reader object, meaning that the input is copied in its raw form.
     *
     * @param in The input stream
     * @param out The output stream
     * @param bufferSize The number of bytes to fetch in a single loop
     * @return Number of bytes copied from one stream to the other
     * @throws IOException
     */
    public static int copy(final InputStream in, final OutputStream out, int bufferSize)
            throws IOException {
        return com.mastfrog.util.streams.Streams.copy(in, out, bufferSize);
    }

    /**
     * Writes the input stream to the output stream. Input is done without a
     * Reader object, meaning that the input is copied in its raw form.
     *
     * @param in The input stream
     * @param out The output stream
     * @return Number of bytes copied from one stream to the other
     * @throws IOException
     */
    public static int copy(final InputStream in, final OutputStream out)
            throws IOException {
        return com.mastfrog.util.streams.Streams.copy(in, out);
    }

    /**
     * Writes the input stream to <i>two</i> output streams.
     *
     * @param in The input stream
     * @param out The output stream
     * @param otherOut Another output stream
     * @return the number of bytes copied
     * @throws IOException if something goes wrong
     */
    public static int copy(final InputStream in, final OutputStream out, final OutputStream otherOut)
            throws IOException {
        return com.mastfrog.util.streams.Streams.copy(in, out, otherOut);
    }

    public static String readUTF8String(InputStream in) throws IOException {
        return com.mastfrog.util.streams.Streams.readUTF8String(in);
    }

    /**
     * Reads a stream as a string.
     *
     * @param in The input stream
     * @return The string
     * @throws IOException
     */
    public static String readString(final InputStream in) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in);
    }

    /**
     * Read a string with a fixed buffer size
     * @param in An input stream
     * @param bufferSize A buffer size, non-negative;  if zero, no buffering
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, int bufferSize) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in, bufferSize);
    }

    /**
     * Read a string with a specified charset and a fixed buffer size
     * @param in An input stream
     * @param charset A character set
     * @param bufferSize A buffer size, non-negative;  if zero, no buffering
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, String charset, int bufferSize) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in, charset, bufferSize);
    }

    /**
     * Read a string with a specified charset
     * @param in An input stream
     * @param charset A character set
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, String charset) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in, charset);
    }

    /**
     * Reads SQL from an input stream. Returns statements separated by
     * semi-colons, skipping comments. The input stream will be closed by this
     * invocation.
     *
     * @param is input stream
     * @return SQL array of sql strings, possibly empty, never null
     */
    public static String[] readSql(InputStream is) {
        return com.mastfrog.util.streams.Streams.readSql(is);
    }

    /**
     * Reads a string using a character encoding.
     *
     * @param in The input
     * @param encoding The character encoding of the input data
     * @return The string
     * @throws IOException
     */
    public static String readString(final InputStream in,
            final CharSequence encoding) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in, encoding);
    }

    /**
     * Reads all input from a reader into a string.
     *
     * @param in The input
     * @return The string
     * @throws IOException
     */
    public static String readString(final Reader in) throws IOException {
        return com.mastfrog.util.streams.Streams.readString(in);
    }

    /**
     * Locate an input stream for the provided location, possibly multiple if it
     * is a resource from the classpath that can be found at multiple locations.
     *
     * @param location either a URL or a path that can be found on the class
     * path or as a file
     * @return input streams or null if not found
     */
    public static InputStream[] locate(String location) {
        return com.mastfrog.util.streams.Streams.locate(location);
    }

    public static void writeString(String s, File to) throws IOException {
        com.mastfrog.util.streams.Streams.writeString(s, to);
    }

    /**
     * Get a ByteBuffer as an InputStream. The passed buffer will be wrapped as
     * a read-only buffer. The position, mark and limit of the passed buffer
     * will remain unmodified, and the returned InputStream will read the byte
     * buffer from position 0, not its current position.
     *
     * @param buf A ByteBuffer
     * @return An InputStream for reading the byte buffer
     */
    public static InputStream asInputStream(ByteBuffer buf) {
        return com.mastfrog.util.streams.Streams.asInputStream(buf);
    }

    public static InputStream asInputStream(ReadableByteChannel channel) {
        return com.mastfrog.util.streams.Streams.asInputStream(channel);
    }

    public static ReadableByteChannel asByteChannel(InputStream in) {
        return com.mastfrog.util.streams.Streams.asByteChannel(in);
    }

    public static ByteBuffer asByteBuffer(InputStream in) throws IOException {
        return com.mastfrog.util.streams.Streams.asByteBuffer(in);
    }

    public static OutputStream nullOutputStream() {
        return com.mastfrog.util.streams.Streams.nullOutputStream();
    }

    public static InputStream asInputStream(Iterable<ByteBuffer> buffers) {
        return com.mastfrog.util.streams.Streams.asInputStream(buffers);
    }

    public static InputStream forByteBuffers(ByteBuffer... iter) {
        return com.mastfrog.util.streams.Streams.forByteBuffers(iter);
    }

    public static InputStream forByteBuffers(Iterable<ByteBuffer> iter) {
        return com.mastfrog.util.streams.Streams.forByteBuffers(iter);
    }

    public static OutputStream asOutputStream(ByteBuffer buffer) {
        return com.mastfrog.util.streams.Streams.asOutputStream(buffer);
    }

    public static void copyFile(File orig, File nue, boolean create) throws IOException {
        com.mastfrog.util.streams.Streams.copyFile(orig, nue, create);
    }

    public static InputStream streamForURL(URL url) {
        return com.mastfrog.util.streams.Streams.streamForURL(url);
    }

    public static PrintStream teeSystemOut(PrintStream other) {
        return com.mastfrog.util.streams.Streams.teeSystemOut(other);
    }

    public static PrintStream tee(PrintStream... streams) {
        return com.mastfrog.util.streams.Streams.tee(streams);
    }

    /**
     * Private to prevent instantiation.
     */
    private Streams() {
    }

    /**
     * Create a relative symbolic link between two files
     *
     * @param original The original
     * @param target The target, which should not yet exist
     * @throws IOException if something goes wrong
     */
    public static void link(File original, File target) throws IOException {
        com.mastfrog.util.streams.Streams.link(original, target);
    }
}
