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

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 *
 * @author Tim Boudreau
 */
public interface Address extends Comparable<Address> {

    byte[] toByteArray();

    InetSocketAddress toDnsServerAddress() throws UnknownHostException;

    InetAddress toInetAddress() throws UnknownHostException;

    /**
     * Get the address as an array of integers.
     */
    int[] toIntArray();

    InetSocketAddress toSocketAddress(int port) throws UnknownHostException;

    /**
     * Number of bytes in address.
     * 
     * @return The number of bytes needed to express an address of this type.
     */
    int size();
    
    BigInteger toBigInteger();

    AddressPurpose purpose();

    default String toStringShorthand() {
        return toString();
    }

    default String toStringBase10() {
        return toString();
    }

    default AddressWithPort<?> withPort(int port) {
        return new AddressWithPort<>(this, port);
    }

    default AddressKind kind() {
        if (this instanceof Ipv4Address) {
            return AddressKind.IPv4;
        } else if (this instanceof Ipv6Address) {
            return AddressKind.IPv6;
        } else {
            throw new AssertionError(getClass().getName() + " does not implement kind()");
        }
    }

    public static Address fromInetAddress(InetAddress addr) {
        if (addr instanceof Inet6Address) {
            return new Ipv6Address(addr.getAddress());
        } else {
            return new Ipv4Address(addr.getAddress());
        }
    }

}
