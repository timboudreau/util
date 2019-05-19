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

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Equivalent of a SocketAddress minus potential network access for equality
 * tests.
 *
 * @author Tim Boudreau
 */
public final class AddressWithPort<T extends Address> {

    private final T address;
    private final int port;

    public AddressWithPort(T address, int port) {
        Checks.notNull("address", port);
        Checks.nonNegative("port", port);
        if (port > 65535) {
            throw new IllegalArgumentException("Port value must be between "
                    + "0-65535 but is " + port);
        }
        this.address = address;
        this.port = port;
    }

    private static void checkIsAllDigits(String portPart) {
        for (int i = 0; i < portPart.length(); i++) {
            if (!Character.isDigit(portPart.charAt(i))) {
                throw new IllegalArgumentException("Port must be digits: '" + portPart + "'");
            }
        }
    }

    public static AddressWithPort<?> parse(String s) {
        s = s.trim();
        if (s.indexOf('.') > 0) {
            int ix = s.indexOf(':');
            if (ix > 0 && ix < s.length() - 1) {
                String portPart = s.substring(ix + 1);
                checkIsAllDigits(portPart);
                String addressPart = s.substring(0, ix);
                Ipv4Address addr = new Ipv4Address(addressPart);
                return new AddressWithPort<>(addr, Integer.parseInt(portPart));
            }
        } else {
            int ix = s.lastIndexOf(':');
            if (ix > 0 && ix < s.length() - 1) {
                String portPart = s.substring(ix + 1);
                checkIsAllDigits(portPart);
                String addressPart = s.substring(0, ix);
                Ipv6Address addr = new Ipv6Address(addressPart);
                return new AddressWithPort<>(addr, Integer.parseInt(portPart));
            }
        }
        throw new IllegalArgumentException("Malformed socket address '" + s + "'");
    }

    public AddressKind kind() {
        return address.kind();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof AddressWithPort<?>) {
            AddressWithPort<?> other = (AddressWithPort<?>) o;
            return port == other.port && address.equals(other.address);
        }
        return false;
    }

    public boolean isSameHost(AddressWithPort<?> other) {
        return other.address.equals(address);
    }

    public boolean isSamePort(AddressWithPort<?> other) {
        return other.port == port;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + this.address.hashCode();
        hash = 17 * hash + this.port;
        return hash;
    }

    public T address() {
        return address;
    }

    public int port() {
        return port;
    }

    public InetSocketAddress toSocketAddress() {
        try {
            return new InetSocketAddress(address.toInetAddress(), port);
        } catch (UnknownHostException ex) {
            // since we are never doing a host lookup, this will not happen
            return Exceptions.chuck(ex);
        }
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

    public String toStringShorthand() {
        return address.toStringShorthand() + ':' + port;
    }

}
