/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A source of random URL-safe, not particularly secure strings - replacement
 * for uses of deprecated GUIDFactory in tests that simply need random data.
 * Will not repeat for the life of the VM unless > Long.MAX_VALUE random strings are
 * requested, which would take a while :-).
 *
 * @author Tim Boudreau
 */
public class RandomStrings {

    private final Random random;
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ-abcdefghijklmnopqrstuvwxyz01234567890._".toCharArray();
    private static final AtomicLong COUNTER = new AtomicLong(Long.MIN_VALUE);

    public RandomStrings(SecureRandom sec) {
        this(new Random(sec.nextLong()));
    }

    public RandomStrings(Random random) {
        this.random = random;
    }

    public RandomStrings() {
        this(new Random(System.currentTimeMillis()));
    }

    public String get() {
        return get(24);
    }

    public String randomChars(int count) {
        char[] result = new char[count];
        char[] an = Arrays.copyOf(ALPHANUM, ALPHANUM.length);
        Strings.shuffle(random, an);
        if (result.length <= an.length) {
            System.arraycopy(an, 0, result, 0, result.length);
        } else {
            System.arraycopy(an, 0, result, 0, an.length);
            for (int i = an.length; i < result.length; i++) {
                result[i] = ALPHANUM[random.nextInt(ALPHANUM.length)];
            }
        }
        return new String(result);
    }

    public String guidLike(int segments, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            if (sb.length() != 0) {
                sb.append('-');
            }
            sb.append(randomChars(length));
        }
        return sb.toString();
    }

    public String get(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        if (length > 8) {
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(COUNTER.getAndIncrement());
        }
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ALPHANUM[Math.abs(bytes[i] % ALPHANUM.length)];
        }
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
