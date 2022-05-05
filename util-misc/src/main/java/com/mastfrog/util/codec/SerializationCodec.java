/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Codec implementation for Java serialization, which uses a string format
 * decoder such as base-64 for string representations.
 *
 * @author Tim Boudreau
 */
final class SerializationCodec implements Codec {

    static final SerializationCodec INSTANCE = new SerializationCodec(false,
            StringEncoding.BASE_64_DEFAULT);
    private final boolean nonClosing;
    private final StringEncoding stringEncoding;

    public SerializationCodec(boolean nonClosing, StringEncoding base64) {
        this.nonClosing = nonClosing;
        this.stringEncoding = base64;
    }

    @Override
    public <T> String writeValueAsString(T t) throws IOException {
        return stringEncoding.newEncoder().encodeToString(writeValueAsBytes(t));
    }

    @Override
    public <T> T readValue(String value, Class<T> type) throws IOException {
        return readValue(stringEncoding.newDecoder().decode(value), type);
    }

    @Override
    public <T> void writeValue(T t, OutputStream out) throws IOException {
        try ( ObjectOutputStream oout = newObjectOutputStream(out)) {
            oout.writeObject(t);
        }
    }

    @Override
    public <T> byte[] writeValueAsBytes(T t) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeValue(t, baos);
        return baos.toByteArray();
    }

    @Override
    public <T> T readValue(InputStream in, Class<T> type) throws IOException {
        try ( ObjectInputStream oin = newObjectInputStream(in)) {
            return type.cast(oin.readObject());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }

    private ObjectInputStream newObjectInputStream(InputStream delegate) throws IOException {
        return nonClosing ? new NonClosingObjectInputStream(delegate)
                : new ObjectInputStream(delegate);
    }

    private ObjectOutputStream newObjectOutputStream(OutputStream delegate) throws IOException {
        return nonClosing ? new NonClosingObjectOutputStream(delegate)
                : new ObjectOutputStream(delegate);
    }

    private static class NonClosingObjectOutputStream extends ObjectOutputStream {

        public NonClosingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.flush();
            // do nothing
        }
    }

    private static class NonClosingObjectInputStream extends ObjectInputStream {

        public NonClosingObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }
}
