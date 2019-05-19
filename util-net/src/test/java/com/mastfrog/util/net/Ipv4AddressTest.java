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
package com.mastfrog.util.net;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
/**
 * Test of ipv4address.
 */
public class Ipv4AddressTest {

    /**
     * Test of address method, of class Ipv4Address.
     */
    @Test
    public void testAddress() {
        Ipv4Address addr = new Ipv4Address("192.168.2.1");
        int[] ints = addr.toIntArray();
        for (int i = 0; i < ints.length; i++) {
            switch (i) {
                case 0:
                    assertEquals(192, ints[i]);
                    break;
                case 1:
                    assertEquals(168, ints[i]);
                    break;
                case 2:
                    assertEquals(2, ints[i]);
                    break;
                case 3:
                    assertEquals(1, ints[i]);
                    break;
            }
        }
        assertEquals("192.168.2.1", addr.toString());
        assertEquals((int) 3232236033L, addr.intValue());
        assertEquals(3232236033L, addr.longValue());
    }

    /**
     * Test of addressParts method, of class Ipv4Address.
     */
    @Test
    public void testAddressParts() {
        Ipv4Address addr = new Ipv4Address("192.168.2.1");
        Ipv4Address addr2 = new Ipv4Address(addr.intValue());
        assertEquals(addr, addr2);
        Ipv4Address addr3 = new Ipv4Address(addr.toString());
        assertEquals(addr, addr3);
    }

    @Test
    public void testPurpose() {
        assertPurpose("127.0.0.1", AddressPurpose.LOOPBACK);
        assertPurpose("127.0.0.23", AddressPurpose.LOOPBACK);
        assertPurpose("127.0.0.255", AddressPurpose.LOOPBACK);
        assertPurpose("255.255.255.255", AddressPurpose.IPv4_BROADCAST_WILDCARD);
        assertPurpose("224.0.0.1", AddressPurpose.MULTICAST);
        assertPurpose("239.0.0.255", AddressPurpose.MULTICAST);
        assertPurpose("227.0.0.255", AddressPurpose.MULTICAST);
        assertPurpose("0.0.0.0", AddressPurpose.ALL_HOST_ADDRESSES);
        assertPurpose("169.254.1.1", AddressPurpose.LINK_LOCAL);
        assertPurpose("192.168.2.1", AddressPurpose.HOST);

    }

    static void assertPurpose(String addr, AddressPurpose p) {
        Ipv4Address a = new Ipv4Address(addr);
        assertEquals(addr, p, a.purpose());
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testParseInvalid() {
        new Ipv4Address("127.0.0.1.5");
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testParseInvalid2() {
        new Ipv4Address("");
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testParseInvalid3() {
        new Ipv4Address("1.2.3");
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testParseInvalid4() {
        new Ipv4Address("1.2.3.m");
    }
}
