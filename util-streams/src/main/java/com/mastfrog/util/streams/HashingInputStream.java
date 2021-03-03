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
package com.mastfrog.util.streams;

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import static com.mastfrog.util.streams.HashingOutputStream.hashString;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wrapper for an input stream which can compute a hash as bytes are read.
 *
 * @author Tim Boudreau
 */
public final class HashingInputStream extends FilterInputStream {

    private final InputStream wrapped;

    HashingInputStream(String algorithm, InputStream wrapped) {
        super(wrapped);
        Checks.notNull("wrapped", wrapped);
        Checks.notNull("algorithm", algorithm);
        this.wrapped = wrapped;
        digest = createDigest(algorithm);
    }
    private final MessageDigest digest;
    private volatile boolean closed;

    public static final MessageDigest createDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            return Exceptions.chuck(ex);
        }
    }

    public static HashingInputStream sha1(InputStream in) {
        return new HashingInputStream("SHA-1", in);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    public byte[] getDigest() {
        if (!closed) {
            throw new IllegalStateException("Stream not closed");
        }
        return digest.digest();
    }

    @Override
    public int read() throws IOException {
        int result = wrapped.read();
        if (result >= 0) {
            digest.update((byte) result);
        }
        return result;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result = wrapped.read(b, off, len);
        if (result > 0) {
            digest.update(b, off, result);
        }
        return result;
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
    }

    public String getHashAsString() throws IOException {
        if (!closed) {
            close();
        }
        byte[] bytes = getDigest();
        return hashString(bytes);
    }
}
