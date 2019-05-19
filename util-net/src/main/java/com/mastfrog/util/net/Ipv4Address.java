/*
 * Copyright 2017 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mastfrog.util.net;

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps an integer that represents an IP v4 address in typed dns records, and
 * provides methods to read and write as various Java types.
 */
public final class Ipv4Address implements Address {

    private final int address;

    public Ipv4Address(long address) {
        this.address = (int) address;
    }

    public Ipv4Address(int address) {
        this.address = address;
    }

    public Ipv4Address(String addr) {
        this.address = parse(notNull("addr", addr));
    }

    public Ipv4Address(int... parts) {
        this.address = pack(notNull("parts", parts));
    }

    public Ipv4Address(byte... parts) {
        this.address = pack(notNull("parts", parts));
    }

    public int intValue() {
        return address;
    }

    public static int addressFamilyNumber() {
        return 1;
    }

    public int size() {
        return 4;
    }

    public BigInteger toBigInteger() {
        return BigInteger.valueOf(address);
    }

    /**
     * Get the address as an array of integers.
     */
    @Override
    public int[] toIntArray() {
        return toInts(intValue());
    }

    @Override
    public byte[] toByteArray() {
        return toBytes(intValue());
    }

    public long longValue() {
        return address & 0x00000000ffffffffL;
    }

    @Override
    public int hashCode() {
        return this.address;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Ipv4Address
                && ((Ipv4Address) obj).intValue() == intValue();
    }

    @Override
    public String toString() {
        return addressToString(address);
    }

    @Override
    public Inet4Address toInetAddress() throws UnknownHostException {
        return (Inet4Address) InetAddress.getByAddress(toBytes(address));
    }

    @Override
    public InetSocketAddress toDnsServerAddress() throws UnknownHostException {
        return toSocketAddress(53);
    }

    @Override
    public AddressPurpose purpose() {
        return AddressPurpose.of(this);
    }

    public AddressWithPort<Ipv4Address> withPort(int port) {
        return new AddressWithPort<>(this, port);
    }

    @Override
    public InetSocketAddress toSocketAddress(int port) throws UnknownHostException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number " + port);
        }
        return new InetSocketAddress(toInetAddress(), port);
    }

    private static String addressToString(int addr) {
        StringBuilder sb = new StringBuilder(32);
        int[] ints = toInts(addr);
        for (int i = 0; i < ints.length; i++) {
            sb.append(ints[i]);
            if (i != ints.length - 1) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    private static int[] toInts(int val) {
        int[] ret = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= (val >>> 8 * (3 - j)) & 0xff;
        }
        return ret;
    }

    private static byte[] toBytes(int val) {
        byte[] ret = new byte[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= (val >>> 8 * (3 - j)) & 0xff;
        }
        return ret;
    }

    static CharSequence[] split(char delimiter, CharSequence seq) {
        List<CharSequence> result = new ArrayList<>(5);
        int max = seq.length();
        int start = 0;
        for (int i = 0; i < max; i++) {
            char c = seq.charAt(i);
            boolean last = i == max - 1;
            if (c == delimiter || last) {
                result.add(seq.subSequence(start, last ? c == delimiter ? i : i + 1 : i));
                start = i + 1;
            }
        }
        return result.toArray(new CharSequence[result.size()]);
    }

    private static int parse(CharSequence ipv4address) {
        CharSequence[] nums = split('.', ipv4address);
        if (nums.length != 4) {
            throw new IllegalArgumentException("Not an ipv4 address: " + ipv4address);
        }
        int[] result = new int[4];
        for (int i = 0; i < nums.length; i++) {
            result[i] = Integer.parseInt(nums[i].toString());
        }
        return pack(result);
    }

    private static int pack(byte... bytes) {
        int[] ints = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            ints[i] = bytes[i] & 0xFF;
        }
        return pack(ints);
    }

    private static int pack(int... bytes) {
        int val = 0;
        for (int i = 0; i < bytes.length; i++) {
            val <<= 8;
            val |= bytes[i] & 0xff;
        }
        return val;
    }

    @Override
    public int compareTo(Address o) {
        if (o instanceof Ipv4Address) {
            Ipv4Address a = (Ipv4Address) o;
            long val = longValue();
            long aval = a.longValue();
            return val > aval ? 1 : val == aval ? 0 : -1;
        }
        return toBigInteger().compareTo(o.toBigInteger());
    }
}
