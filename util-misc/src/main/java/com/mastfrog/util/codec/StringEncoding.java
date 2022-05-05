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

import java.util.Base64;

/**
 * String encodings for which we have encoders and decoders.
 *
 * @author Tim Boudreau
 */
public enum StringEncoding {
    /**
     * Encoders using the JDK's default
     * <code>Base64.getEncoder() / Base64.getDecoder()</code>.
     */
    BASE_64_DEFAULT,
    /**
     * Encoders using the JDK's
     * <code>Base64.getMimeEncoder() / Base64.getMimeDecoder()</code>.
     */
    BASE_64_MIME_SAFE,
    /**
     * Encoders using the JDK's
     * <code>Base64.getUrlEncoder() / Base64.getUrlDecoder()</code>.
     */
    BASE_64_URL_SAFE,
    /**
     * Encodes to hexadecimal, two-characters per byte, using <i>lower-case</i>
     * <code>a</code> through <code>f</code> (decoders can read upper or lower
     * case, so for decoding, HEX_UPPER_CASE and HEX_LOWER_CASE are
     * interchangeable).
     */
    HEX_LOWER_CASE,
    /**
     * Encodes to hexadecimal, two-characters per byte, using <i>upper-case</i>
     * <code>a</code> through <code>f</code> (decoders can read upper or lower
     * case, so for decoding, HEX_UPPER_CASE and HEX_LOWER_CASE are
     * interchangeable).
     */
    HEX_UPPER_CASE;

    /**
     * Create an encoder.
     *
     * @return An encoder
     */
    public StringFormatEncoder newEncoder() {
        switch (this) {
            case BASE_64_DEFAULT:
                return StringFormatEncoder.wrap(Base64.getEncoder());
            case BASE_64_MIME_SAFE:
                return StringFormatEncoder.wrap(Base64.getMimeEncoder());
            case BASE_64_URL_SAFE:
                return StringFormatEncoder.wrap(Base64.getUrlEncoder());
            case HEX_LOWER_CASE:
                return Hex.newEncoder(true, true);
            case HEX_UPPER_CASE:
                return Hex.newEncoder(false, true);
            default:
                throw new AssertionError(this);
        }
    }

    /**
     * Create a decoder.
     *
     * @return a decoder
     */
    public StringFormatDecoder newDecoder() {
        switch (this) {
            case BASE_64_DEFAULT:
                return StringFormatDecoder.wrap(Base64.getDecoder());
            case BASE_64_MIME_SAFE:
                return StringFormatDecoder.wrap(Base64.getMimeDecoder());
            case BASE_64_URL_SAFE:
                return StringFormatDecoder.wrap(Base64.getUrlDecoder());
            case HEX_LOWER_CASE:
            case HEX_UPPER_CASE:
                return Hex.newDecoder(true);
            default:
                throw new AssertionError(this);
        }
    }
}
