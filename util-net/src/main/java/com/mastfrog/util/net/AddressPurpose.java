/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.net;

/**
 *
 * @author Tim Boudreau
 */
public enum AddressPurpose {

    HOST,
    MULTICAST,
    LOOPBACK,
    IPv4_BROADCAST_WILDCARD,
    ALL_HOST_ADDRESSES,
    LINK_LOCAL;

    public static void main(String[] args) {
        Ipv6Address a = new Ipv6Address("FE80::1");
        int mask = mask(10);
        System.out.println("MASK " + Integer.toHexString(mask));
        Ipv6Address b = new Ipv6Address("FE80::" + Integer.toHexString(mask));
        System.out.println("B: " + b);
        System.out.println("AHIGH\t" + a.high() + "\t" + Long.toHexString(a.high()));
        System.out.println("ALOW\t" + a.low() + "\t" + Long.toHexString(a.low()));
        System.out.println("BHIGH\t" + b.high() + "\t" + Long.toHexString(b.high()));
        System.out.println("BLOW\t" + b.low() + "\t" + Long.toHexString(b.low()));
    }

    private static int mask(int count) {
        int val = 0;
        for (int i = 0; i < count; i++) {
            val = (val << 1) | 1;
        }
        return val;
    }

    private static boolean rangeIsZeros(int start, int stop, int[] arr) {
        for (int i = 0; i <= stop; i++) {
            if (arr[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public static AddressPurpose of(Ipv6Address a) {
        if (a.high() == 0) {
            if (a.low() > 0 && a.low() <= 0xffffffffL) {
                return LOOPBACK;
            }
        } else if (a.high() == 0xfe80000000000000L) {
            if (a.low() > 0 && (a.low() <= 0x3ff)) {
                return LINK_LOCAL;
            }
        }
        int[] arr = a.toIntArray();
        switch (arr[0]) {
            case 0xFF01:
            case 0xFF02:
            case 0xFF05:
            case 0xFF0e:
                return MULTICAST;
        }

        return HOST;
    }

    public static AddressPurpose of(Ipv4Address a) {
        int val = a.intValue();
        switch (val) {
            case 0x0:
                return ALL_HOST_ADDRESSES;
            case 0xffffffff:
                return IPv4_BROADCAST_WILDCARD;
        }
        if (val >= 0x7f000001 && val <= 0x7f0000ff) {
            return LOOPBACK;
        }
        int[] arr = a.toIntArray();
        if (arr[0] >= 224 && arr[0] < 240) {
            return MULTICAST;
        }
        if (arr[0] == 169 && arr[1] == 254) {
            return LINK_LOCAL;
        }
        return HOST;
    }
}
