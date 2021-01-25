/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.bits.util;

import static com.mastfrog.bits.util.Pack.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class PackTest {

    @Test
    public void testPackShort() {
        int top = (Short.MAX_VALUE * 2) - 2;
        for (int i = 0; i < top; i += 17) {
            int va  = i;
            int vb = i + 1;
            int val = packShort(va, vb);
            int ua = unpackShortLeft(val);
            int ub = unpackShortRight(val);
            assertEquals(va, ua, "Wrong value at " + i);
            assertEquals(vb, ub, "Wrong value at " + i);
        }
    }

    @Test
    public void testPack3() {
        int interval = 1903;
        for (int a = Integer.MIN_VALUE + 2; a < Integer.MAX_VALUE - (interval + 1); a += interval) {

            int aa = a;
            int bb = Math.abs(a % 256);
            int cc = Math.abs((a + 1) % 256);

            long val = pack3(a, bb, cc);
            long valA = pack3(a, 0, 0);
            long valB = pack3(0, bb, 0);
            long valC = pack3(0, 0, cc);

            assertNotEquals(0l, val, () -> " Zero at " + aa + " / " + bb + " / " + cc);
            if (bb != 0) {
                assertNotEquals(0l, valB, () -> " Zero for B at " + aa + " / " + bb + " / " + cc);
            }
            if (cc != 0) {
                assertNotEquals(0l, valC, () -> " Zero for C at " + aa + " / " + bb + " / " + cc);
            }

            int ua = unpack3Int(valA);
            int ub = unpack3LeftByte(valB);
            int uc = unpack3RightByte(valC);

            int xa = unpack3Int(val);
            int xb = unpack3LeftByte(val);
            int xc = unpack3RightByte(val);

            assertEquals(aa, ua, () -> "Mismatched iso int value at " + aa + " / " + bb + " / " + cc + " " + split(valA));
            assertEquals(bb, ub, () -> "Mismatched iso left byte value at " + aa + " / " + bb + " / " + cc + " " + split(valB));
            assertEquals(cc, uc, () -> "Mismatched iso left byte value at " + aa + " / " + bb + " / " + cc + " " + split(valC));

            assertEquals(aa, xa, () -> "Mismatched int value at " + aa + " / " + bb + " / " + cc + " " + split(val));
            assertEquals(bb, xb, () -> "Mismatched left byte value at " + aa + " / " + bb + " / " + cc + " " + split(val));
            assertEquals(cc, xc, () -> "Mismatched left byte value at " + aa + " / " + bb + " / " + cc + " " + split(val));
        }
    }

    @Test
    public void testPack2() {
        int interval = 11903;
        for (int i = Integer.MIN_VALUE + 2; i < Integer.MAX_VALUE - (interval + 1); i += interval) {
            int ia = i;
            int ib = i + 1;

            long val = packInts(ia, ib);
            long valB = packInts(0, ib);
            long valA = packInts(ia, 0);

            int ua = unpackIntsLeft(valA);
            int ub = unpackIntsRight(valB);

            int xa = unpackIntsLeft(val);
            int xb = unpackIntsRight(val);

            assertEquals(ia, ua, "Mismatch A iso at " + i);
            assertEquals(ia, xa, "Mismatch A at " + i);

            assertEquals(ib, ub, "Mismatch B iso at " + i);
            assertEquals(ib, xb, "Mismatch B at " + i);
        }
    }

    @Test
    public void testPack4() {
        for (int i = 0; i < (Short.MAX_VALUE * 2); i += 903) {
            int ia = i;
            int ib = i + 1;
            int ic = i + 2;
            int id = i + 3;
            long val = pack(ia, ib, ic, id);
            long valA = pack(ia, 0, 0, 0);
            long valB = pack(0, ib, 0, 0);
            long valC = pack(0, 0, ic, 0);
            long valD = pack(0, 0, 0, id);

            int ua = unpack16A(val);
            int ub = unpack16B(val);
            int uc = unpack16C(val);
            int ud = unpack16D(val);

            int xa = unpack16A(valA);
            int xb = unpack16B(valB);
            int xc = unpack16C(valC);
            int xd = unpack16D(valD);

//            System.out.println(" A + " + ia + " -> " + xa + " as " + split(valA));
//            System.out.println(" C + " + ia + " -> " + xc + " as " + split(valC));
//            System.out.println(" D + " + ia + " -> " + xd + " as " + split(valD));
            assertEquals(id, xd, () -> "Mismatch iso D at " + ia + " with " + ic + " packed to " + split(valD));
            assertEquals(ic, xc, () -> "Mismatch iso C at " + ia + " with " + ic + " packed to " + split(valC));
            assertEquals(ib, xb, () -> "Mismatch iso B at " + ia + " with " + ib + " packed to " + split(valB));
            assertEquals(ia, xa, () -> "Mismatch iso A at " + ia + " with " + ia + " packed to " + split(valA));

            assertEquals(ia, ua, () -> "Mismatch A at " + ia + " with " + ia + " packed to " + split(val));
            assertEquals(ib, ub, () -> "Mismatch B at " + ia + " with " + ib + " packed to " + split(val));
            assertEquals(ic, uc, () -> "Mismatch C at " + ia + " with " + ic + " packed to " + split(val));
            assertEquals(id, ud, () -> "Mismatch D at " + ia + " with " + id + " packed to " + split(val));

//            System.out.println(i + ". A " + i + " -> " + ua);
//            System.out.println(i + ". B " + (i + 1) + " -> " + ub);
//            System.out.println(i + ". C " + (i + 2) + " -> " + uc);
//            System.out.println(i + ". D " + (i + 3) + " -> " + ud);
        }
    }

    static long MASK = 0x0000_0000_0000_FFFFL;

    static int unpackC2(long value) {
//        value = value & 0x0000_0000_FFFF_0000L;
        System.out.println("UNPACK " + split(value) + " ( " + value + " )");
        long shifted = (value >>> 16);
        System.out.println("SHIFT  " + split(shifted) + " ( " + shifted + " )");
        long masked = shifted & MASK;
        System.out.println("MASK   " + split(MASK) + " ( " + MASK + " )");
        System.out.println("MASKED " + split(masked) + " ( " + masked + " )");
        int result = (int) masked;
        System.out.println("CAST   " + split(result) + " ( " + result + ")");
        return result;
    }

    private static String binaryString(long val) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(Long.toBinaryString(val));
        while (sb.length() < 64) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    private static String split(long val) {
        String res = binaryString(val);
        StringBuilder sb = new StringBuilder();
        int[] vals = new int[8];
        int cursor = 0;
        char pfx = 'a';
        sb.append(pfx).append(": ");
        for (int i = 0; i < res.length(); i++) {
            int bitIx = 15 - (i % 16);
            int or = 1 << bitIx;
            char c = res.charAt(i);
            if ('1' == c) {
                vals[cursor] |= or;
            }
            sb.append(res.charAt(i));
            if ((i + 1) % 16 == 0) {
                sb.append(' ');
                sb.append("(").append(vals[cursor]).append(") ");
                cursor++;
                if (i != res.length() - 1) {
                    sb.append(++pfx).append(": ");
                }
            }
        }
        return sb.toString();
    }
}
