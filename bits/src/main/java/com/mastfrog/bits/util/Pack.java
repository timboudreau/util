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

import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntQuadConsumer;
import com.mastfrog.function.IntTriConsumer;

/**
 * Misc utility methods for packing numbers as bit fields of larger numbers and
 * getting them back.
 *
 * @author Tim Boudreau
 */
public final class Pack {

    private Pack() {
        throw new AssertionError();
    }

    /**
     * Pack two integers into one long.
     *
     * @param left The most significant dword
     * @param right The least significant dword
     * @return A long
     */
    public static long packInts(int left, int right) {
        return (((long) left) << 32) | (right & 0xFFFF_FFFFL);
    }

    /**
     * Unpack the most significant dword of a long as an int.
     *
     * @param value A packed value
     * @return An integer
     */
    public static int unpackIntsLeft(long value) {
        return (int) ((value >>> 32) & 0xFFFF_FFFFL);
    }

    /**
     * Unpack the least significant dword of a long as an int.
     *
     * @param value A packed value
     * @return An integer
     */
    public static int unpackIntsRight(long value) {
        return (int) (value & 0xFFFF_FFFFL);
    }

    /**
     * Unpack the dwords of a long as two ints passed to the passed consumer.
     *
     * @param value A packed value
     * @return An integer
     */
    public static void unpack(long val, IntBiConsumer c) {
        c.accept(unpackIntsLeft(val), unpackIntsRight(val));
    }

    private static void check16Unsigned(int val) {
        if (val < 0 || val > 65535) {
            throw new IllegalArgumentException("Value " + val
                    + " cannot fit in 16 bits unsigned");
        }
    }

    private static void check8Unsigned(int val) {
        if (val < 0 || val > 255) {
            throw new IllegalArgumentException("Value " + val
                    + " cannot fit in 8 bits unsigned");
        }
    }

    public static long unsigned(int x) {
        return x & 0xFFFFFFFFL;
    }

    public static int unsigned(short x) {
        return x & 0xFFFF;
    }

    public static int unsigned(byte b) {
        return b & 0xFF;
    }

    /**
     * Store two integers as <i>unsigned 16-bit numbers</i> in the left and
     * right halves of an int.
     *
     * @param left the left side
     * @param right the right side
     * @return an int
     * @throws IllegalArgumentException if the values are out of range
     */
    public static int packShort(int left, int right) {
        check16Unsigned(left);
        check16Unsigned(right);
        return (left << 16)
                | (right & 0xFFFF);
    }

    /**
     * Pack two shorts as one int.
     *
     * @param left the left short
     * @param right the right short
     * @return an int
     */
    public static int packShort(short left, short right) {
        return (left << 16)
                | (right & 0xFFFF);
    }

    /**
     * Pack three values into athe right half of a long as one 32 bit value and
     * two 16 bit values.
     *
     * @param a The leftmost 32 bits
     * @param b The left 16 bits of the right 32 bits
     * @param c The right 16 bits of the right 32 bits
     * @return A long
     */
    public static long pack3(int a, int b, int c) {
        check16Unsigned(b);
        check16Unsigned(c);
        return ((long) a << 16)
                | (long) ((b << 8) & 0xFF00) | (c & 0xFF);
    }

    /**
     * Pack three values into a long as one 32 bit value and two 16 bit values.
     *
     * @param a The leftmost 32 bits
     * @param b The left 16 bits of the right 32 bits
     * @param c The right 16 bits of the right 32 bits
     * @return A long
     */
    public static long pack3(int a, short b, short c) {
        return ((long) a << 16)
                | (long) ((b << 8) & 0xFF00) | (c & 0xFF);
    }

    /**
     * Unpack three values packed with pack3().
     *
     * @param val A value
     * @param c a consumer
     */
    public static void unpack3(long val, IntTriConsumer c) {
        c.accept(unpack3Int(val), unpack3LeftByte(val), unpack3RightByte(val));
    }

    /**
     * Unpack the most significant word of an int as an unsigned 16 bit value.
     *
     * @param val A packed value
     * @return a non-negative value
     */
    public static int unpackShortLeft(int val) {
        return (val >>> 16) & 0xFFFF;
    }

    /**
     * Unpack the least significant word of an int as an unsigned 16 bit value.
     *
     * @param val A packed value
     * @return a non-negative value
     */
    public static int unpackShortRight(int val) {
        return val & 0xFFFF;
    }

    /**
     * Unpack an int as two 16 bit values into the passed consumer.
     *
     * @param val A value
     * @param c A consumer
     */
    public void unpackShort(int val, IntBiConsumer c) {
        c.accept(unpackShortLeft(val), unpackShortRight(val));
    }

    /**
     * Unpack the left 16 bits of the least significant dword of a long as an
     * unsigned int.
     *
     * @param value A value
     * @return An int
     */
    public static int unpack3Int(long value) {
        return (int) ((value >>> 16) & 0xFFFF_FFFFL);
    }

    /**
     * Unpack bits 8-15 of a long value as an unsigned short.
     *
     * @param value A value
     * @return A
     */
    public static short unpack3LeftByte(long value) {
        value = value & 0x0000_0000_0000_FF00L;
        return (short) ((value >>> 8) & 0xFFFFL);
    }

    /**
     * Unpack bits 0-7 of a long value as an unsigned short.
     *
     * @param value A value
     * @return A
     */
    public static short unpack3RightByte(long value) {
        return (short) (value & 0xFFL);
    }

    /**
     * Pack four ints into a long as 16-bit unsigned values.
     *
     * @param a an int
     * @param b an int
     * @param c an int
     * @param d an int
     * @return a long
     */
    public static long pack(int a, int b, int c, int d) {
        check16Unsigned(a);
        check16Unsigned(b);
        check16Unsigned(c);
        check16Unsigned(d);
        return ((long) a << 48)
                | (long) ((b & 0xFFFF_FFFF_FFFFL) << 32)
                | (long) ((c & 0xFFFF_FFFF_FFFFL) << 16)
                | (long) (d & 0xFFFF_FFFF_FFFFL);
    }

    /**
     * Unpack a long two four 16-bit unsigned values.
     *
     * @param val A long
     * @param c A consumer for all of the values
     */
    public static void unpack(long val, IntQuadConsumer c) {
        c.accept(unpack16A(val), unpack16B(val), unpack16C(val), unpack16D(val));
    }

    public static int unpack16(long value, int index) {
        switch (index) {
            case 0:
                return unpack16A(value);
            case 1:
                return unpack16B(value);
            case 2:
                return unpack16C(value);
            case 3:
                return unpack16D(value);
            default:
                throw new IllegalArgumentException("Index must be in 0..3 but got " + index);
        }
    }

    public static int unpack16A(long value) {
        return (int) ((value >>> 48) & 0xFFFFL);
    }

    public static int unpack16B(long value) {
        return (int) ((value >>> 32) & 0xFFFFL);
    }

    public static int unpack16C(long value) {
        return (int) ((value >>> 16) & 0xFFFFL);
    }

    public static int unpack16D(long value) {
        return (int) (value & 0xFFFFL);
    }
}
