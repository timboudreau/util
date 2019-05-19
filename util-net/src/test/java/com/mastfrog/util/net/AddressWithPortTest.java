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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AddressWithPortTest {

    @Test
    public void testParseIpv4() {
        AddressWithPort<?> expected = new AddressWithPort(new Ipv4Address("127.0.0.1"), 80);
        assertArrayEquals(new byte[] {127, 0, 0, 1}, expected.address().toByteArray());
        assertEquals(80, expected.port());
        AddressWithPort<?> p = AddressWithPort.parse("127.0.0.1:80");
        assertEquals(expected, p);
        assertEquals(expected.hashCode(), p.hashCode());
    }

    @Test
    public void testParseIpv6() {
        Ipv6Address addr = new Ipv6Address(0xfe02L << (64-16), 1);
        AddressWithPort<?> expected = new AddressWithPort(addr, 80);
        AddressWithPort<?> a = AddressWithPort.parse("fe02:0000:0000:0000:0000:0000:0000:0001:80");
        assertEquals(addr, a.address());
        assertEquals(expected, a);
        AddressWithPort<?> p = AddressWithPort.parse("fe02::1:80");
        assertEquals(expected, p);
    }

}
