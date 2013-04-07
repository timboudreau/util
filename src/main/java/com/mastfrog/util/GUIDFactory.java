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

import java.security.SecureRandom;
import java.util.Random;

/**
 *
 * @author tim
 */
public class GUIDFactory {

    private final Random r;
    private final char[] chars = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxqy0123456789".toCharArray();
    private static volatile GUIDFactory INSTANCE;

    // XXX use a more standard algorithm, maybe grab mac addresses
    public static GUIDFactory get() {
        if (INSTANCE == null) {
            synchronized (GUIDFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GUIDFactory();
                }
            }
        }
        return INSTANCE;
    }

    GUIDFactory() {
        //seed from a secure random, use a less expensive random to work with
        SecureRandom sr = new SecureRandom();
        shuffle(sr);
        r = new Random(sr.nextLong());
    }

    private void shuffle(SecureRandom r) {
        for (int i = 0; i < chars.length; i++) {
            int xa = r.nextInt(chars.length);
            char hold = chars[xa];
            chars[xa] = chars[i];
            chars[i] = hold;
        }
    }

    public String newGUID() {
        return newGUID(6, 5);
    }

    public String newGUID(int segments, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            segment(length, sb);
            if (i != segments - 1) {
                sb.append('-');
            }
        }
        return sb.toString();
    }

    private void segment(int length, StringBuilder sb) {
        for (int i = 0; i < length; i++) {
            sb.append(chars[r.nextInt(chars.length)]);
        }
    }
}
