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
 *
 * @author Tim Boudreau
 */
final class Base64WrapperDecoder implements StringFormatDecoder {

    private final Base64.Decoder delegate;

    Base64WrapperDecoder(Base64.Decoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] decode(byte[] src) {
        return delegate.decode(src);
    }

    @Override
    public byte[] decode(String src) {
        return delegate.decode(src);
    }

    @Override
    public int decode(byte[] src, byte[] dst) {
        return delegate.decode(src, dst);
    }

    @Override
    public ByteBuffer decode(ByteBuffer buffer) {
        return delegate.decode(buffer);
    }

    @Override
    public InputStream wrap(InputStream is) {
        return delegate.wrap(is);
    }
}
