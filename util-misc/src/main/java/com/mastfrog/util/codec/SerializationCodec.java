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
import java.util.Base64;

/**
 *
 * @author Tim Boudreau
 */
final class SerializationCodec implements Codec {

    static final SerializationCodec INSTANCE = new SerializationCodec();

    @Override
    public <T> String writeValueAsString(T t) throws IOException {
        return Base64.getEncoder().encodeToString(writeValueAsBytes(t));
    }

    @Override
    public <T> void writeValue(T t, OutputStream out) throws IOException {
        try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
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
        try (ObjectInputStream oin = new ObjectInputStream(in)) {
            return type.cast(oin.readObject());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }
}
