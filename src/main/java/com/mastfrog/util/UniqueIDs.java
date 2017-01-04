/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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

import com.mastfrog.util.collections.CollectionUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A source of unlikely-to-collide, hard-to-guess random URL-safe strings,
 * incorporating a system-specific component, a timestamp, an incrementing
 * counter and the MAC address of the network cards on the system.
 *
 * @author Tim Boudreau
 */
public final class UniqueIDs {

    private final long FIRST = System.currentTimeMillis();
    private final AtomicLong seq = new AtomicLong(FIRST);
    private final Random random;
    private final String base;
    private final long vmid;

    /**
     * Create an instance.
     *
     * @param appfile A file to store the application-specific component in, or
     * read it from if it exists.
     *
     * @throws IOException if something goes wrong
     */
    public UniqueIDs(File appfile) throws IOException {
        SecureRandom sr = new SecureRandom();
        random = new Random(sr.nextLong());
        // Identifier for this process
        vmid = Math.abs(sr.nextLong());
        // XOR mac addresses of all network cards
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(longToBytes(vmid));
        byte[] addrBytes = new byte[6];
        for (NetworkInterface i : CollectionUtils.toIterable(NetworkInterface.getNetworkInterfaces())) {
            if (!i.isLoopback() && i.isUp() && !i.isVirtual()) {
                byte[] macAddress = i.getHardwareAddress();
                if (macAddress != null) {
                    xor(macAddress, addrBytes);
                }
            }
        }
        baos.write(addrBytes);
        // Load or generate a created-on-first-use identifier for
        // this application
        if (appfile.exists()) {
            try (FileInputStream in = new FileInputStream(appfile)) {
                Streams.copy(in, baos, 8);
            }
        } else {
            byte[] bts = new byte[8];
            random.nextBytes(bts);
            appfile.createNewFile();
            try (FileOutputStream out = new FileOutputStream(appfile)) {
                out.write(bts);
            }
            baos.write(bts);
        }
        base = bytesToString(baos.toByteArray());
    }

    private void xor(byte[] src, byte[] dest) {
        if (src != null && dest != null) {
            for (int i = 0; i < Math.min(src.length, dest.length); i++) {
                dest[i] ^= src[i];
            }
        }
    }

    private String bytesToString(byte[] b) {
        LongBuffer lb = ByteBuffer.wrap(b).asLongBuffer();
        StringBuilder sb = new StringBuilder();
        while (lb.position() < lb.capacity()) {
            long val = Math.abs(lb.get());
            sb.append(Long.toString(val, 36));
        }
        return sb.toString();
    }

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * Get a new unique string id.
     *
     * @return An id
     */
    public String newId() {
        int inc = random.nextInt(13) + 1;
        // Increment the counter by a random interval of at least 1, so
        // not strictly sequential
        long ix = seq.getAndAdd(inc);
        // Get a random value
        int val = random.nextInt(Integer.MAX_VALUE);
        // Concat it all, and reverse it for better distribution of
        // values if the characters are looked at in order
        return new StringBuilder(base)
                .append(Integer.toString(val, 36))
                .append(Long.toString(ix, 36))
                .append(newRandomString(5))
                .reverse().toString();
    }

    @Override
    public String toString() {
        return base;
    }

    public String newRandomString() {
        return newRandomString(16);
    }

    public String newRandomString(int count) {
        byte[] bytes = new byte[count];
        random.nextBytes(bytes);
        return bytesToString(bytes) + '-' + bytesToString(longToBytes(vmid));
    }

    private String reversed;

    public boolean recognizes(String id) {
        Checks.notNull("id", id);
        if (reversed == null) {
            reversed = new StringBuilder(base).reverse().toString();
        }
        return id.endsWith(reversed);
    }
}
