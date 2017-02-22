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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A source of unlikely-to-collide, hard-to-guess random URL-safe strings,
 * incorporating a system-specific component, a timestamp, an incrementing
 * counter and the MAC address of the network cards on the system.
 *
 * @author Tim Boudreau
 */
public final class UniqueIDs {

    private final AtomicInteger seq;
    private final Random random;
    private final byte[] base;
    private final int vmid;

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
        int ts = (int) (System.currentTimeMillis() - 1483853019824L + sr.nextInt(60000 * 12));
        seq = new AtomicInteger(ts);
        random = new Random(sr.nextLong());
        // Identifier for this process
        vmid = Math.abs(sr.nextInt());
        // XOR mac addresses of all network cards
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        byte[] addrBytes = new byte[6];
//        xor(addrBytes, longToBytes(System.currentTimeMillis()));
        for (NetworkInterface i : CollectionUtils.toIterable(NetworkInterface.getNetworkInterfaces())) {
            if (!i.isLoopback() && i.isUp() && !i.isVirtual()) {
                byte[] macAddress = i.getHardwareAddress();
                if (macAddress != null) {
                    xor(macAddress, addrBytes);
                }
            }
        }
        // Load or generate a created-on-first-use identifier for
        // this application
        if (appfile.exists()) {
            try (FileInputStream in = new FileInputStream(appfile)) {
                Streams.copy(in, baos, 5);
            }
        } else {
            byte[] bts = new byte[5];
            random.nextBytes(bts);
            appfile.createNewFile();
            try (FileOutputStream out = new FileOutputStream(appfile)) {
                out.write(bts);
            }
            baos.write(bts);
        }
        baos.write(intToBytes(vmid));
        baos.write(addrBytes);
        base = baos.toByteArray();
    }

    private void xor(byte[] src, byte[] dest) {
        if (src != null && dest != null) {
            for (int i = 0; i < Math.min(src.length, dest.length); i++) {
                dest[i] ^= src[i];
            }
        }
    }

    private static final char[] ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz0123456789~".toCharArray();

    private String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length - (bytes.length % 3); i += 3) {
            // Top 6 bits of byte i
            int value1 = bytes[i] >> 2;
            // Bottom 2 bits of byte i, top 4 bits of byte i+1
            int value2 = ((bytes[i] & 0x3) << 4) | (bytes[i + 1] >> 4);
            // Bottom 4 bits of byte i+1, top 2 bits of byte i+2
            int value3 = ((bytes[i + 1] & 0xf) << 2) | (bytes[i + 2] >> 6);
            // Bottom 6 bits of byte i+2
            int value4 = bytes[i + 2] & 0x3f;
            
            sb.append(ALPHA[Math.abs(value1)]).append(ALPHA[Math.abs(value2)])
                    .append(ALPHA[Math.abs(value3)]).append(ALPHA[Math.abs(value4)]);
            // Now use value1...value4, e.g. putting them into a char array.
            // You'll need to decode from the 6-bit number (0-63) to the character.
        }
        return sb.toString();
    }

    private byte[] intToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / 8);
        buffer.putInt(x);
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
        int ix = seq.addAndGet(inc);
        // Get a random value
        // Concat it all, and reverse it for better distribution of
        // values if the characters are looked at in order
        int size = (Integer.SIZE / 8) + (Long.SIZE / 8) + base.length;
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.put(reverse(intToBytes(ix))).putLong(random.nextLong()).put(base);
        return bytesToString(buf.array());
    }
    
    private byte[] reverse(byte[] b) {
        for (int i = 0; i < b.length / 2; i++) {
            byte hold = b[i];
            b[i] = b[b.length - (i+1)];
            b[b.length-(i+1)] = hold;
        }
        return b;
    }
    
    @Override
    public String toString() {
        return bytesToString(base);
    }

    public String newRandomString() {
        return newRandomString(16);
    }

    public String newRandomString(int count) {
        byte[] bytes = new byte[count];
        random.nextBytes(bytes);
        return bytesToString(bytes);
    }

    private String reversed;

    public boolean recognizes(String id) {
        Checks.notNull("id", id);
        if (reversed == null) {
            reversed = bytesToString(base);
        }
        return id.endsWith(reversed);
    }
    
    public boolean knows(String id) {
        Checks.notNull("id", id);
        if (reversed == null) {
            reversed = bytesToString(base);
        }
        return id.endsWith(reversed.substring(reversed.length() - 8, reversed.length()));        
    }

    public static void main(String[] args) throws IOException {
        UniqueIDs ids = new UniqueIDs(new File("/tmp/foo.bytes"));
        for (int i = 0; i < 100; i++) {
            String id = ids.newId();
            System.out.println(id);
        }
    }
}
