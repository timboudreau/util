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

import java.math.BigInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class Ipv6AddressTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid1() {
        assertNull(new Ipv6Address(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid2() {
        new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000:0001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid3() {
        new Ipv6Address("Hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid4() {
        new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000:0001:0002:0003:0004");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid5() {
        new Ipv6Address("0000:30000:0000:0000:0000:0000:0000:0000:0001:0002:0003:0004");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid0() {
        new Ipv6Address("0000:3000:0000:0000:0000:p00p:0000:0000:0001:0002:0003:0004");
    }

    @Test
    public void testToString() {
        Ipv6Address addr = new Ipv6Address(0, 1);
        assertEquals(1, addr.low());
        assertEquals(0, addr.high());
        assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", addr.toString());
    }

    @Test
    public void testStringConversion() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        assertEquals("2008:FEED:3c20:0128:0004:0003:0002:0001".toLowerCase(), addr.toString());
    }

    @Test
    public void testToIntArray() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        int[] ints = addr.toIntArray();
        assertEquals(8, ints.length);
    }

    @Test
    public void testToBigInteger() {
        Ipv6Address addr = new Ipv6Address("2008:FEED:3c20:0128:0004:0003:0002:0001");
        BigInteger bi = addr.toBigInteger();
        assertEquals(addr, new Ipv6Address(bi));
    }

    @Test
    public void testConversion() {
        Ipv6Address addr = new Ipv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        Ipv6Address addr2 = new Ipv6Address(addr.high(), addr.low());
        assertEquals(addr, addr2);
        Ipv6Address addr4 = new Ipv6Address(addr.toByteArray());
        assertEquals(addr, addr4);
        Ipv6Address addr3 = new Ipv6Address(addr.toIntArray());
        assertEquals(addr.low(), addr3.low());
        assertEquals(addr.high(), addr3.high());
        assertEquals(addr, addr3);
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", addr.toString());
    }

    @Test
    public void testShorthand() {
        Ipv6Address addr = new Ipv6Address("0001:0002:0003:0000:0000:0006:0007:0008");
        assertEquals("1:2:3::6:7:8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0007:0008");
        assertEquals("1:2:3:4:5:6:7:8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0001:0002:0003:0004:0005:0006:0007");
        assertEquals("::1:2:3:4:5:6:7", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0004:0000:0000:0000:0008");
        assertEquals("1:0:0:4::8", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0000:0000");
        assertEquals("1:2:3:4:5:6::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0002:0003:0004:0005:0006:0007:0000");
        assertEquals("1:2:3:4:5:6:7::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0002:0003:0004:0005:0006:0000:0000");
        assertEquals("0:2:3:4:5:6::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals("2001:db8:85a3::8a2e:370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals("0:db8:85a3::8a2e:370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:85a3:0000:0000:0000:0370:7334");
        assertEquals("0:0:85a3::370:7334", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0001");
        assertEquals("::1", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0000:0000:0000:0000:0000:0000:0000:0000");
        assertEquals("::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0000:0000:0000:0000:0000");
        assertEquals("1::", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));

        addr = new Ipv6Address("0001:0000:0000:0000:0000:0000:0000:0001");
        assertEquals("1::1", addr.toStringShorthand());
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));
    }

    @Test
    public void testToString2() {
        String base = "fe80:0000:0000:0000:184d:1cbc:f0dd:e656";
        Ipv6Address addr = new Ipv6Address(base);
        assertEquals(base, addr.toString());
        assertFalse(addr.toStringShorthand().contains("%"));
        assertEquals(addr, new Ipv6Address(addr.toString()));
        assertEquals(addr, new Ipv6Address(addr.toStringShorthand()));
        assertEquals("fe80::184d:1cbc:f0dd:e656", addr.toStringShorthand());
    }
}
