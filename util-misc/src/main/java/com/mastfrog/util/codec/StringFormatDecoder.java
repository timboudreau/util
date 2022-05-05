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
package com.mastfrog.util.codec;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Mirrors the API of Base64.Decoder in an interface, so it can be implemented
 * for non-base-64 formats such as hex.
 *
 * @author Tim Boudreau
 */
public interface StringFormatDecoder {

    ByteBuffer decode(ByteBuffer buffer);

    byte[] decode(String src);

    byte[] decode(byte[] src);

    int decode(byte[] src, byte[] dst);

    InputStream wrap(InputStream is);

    public static StringFormatDecoder wrap(Base64.Decoder delegate) {
        return new Base64WrapperDecoder(delegate);
    }
}
