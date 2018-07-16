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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


/**
 * Represents an IPv6 address in a typed DNS record.
 */
public class Ipv6Address implements Comparable<Ipv6Address> {

    private final long low;
    private final long high;

    private static final int BYTES_SIZE = 16;

    public Ipv6Address(long high, long low) {
        this.low = low;
        this.high = high;
    }

    public Ipv6Address(int... ints) {
        notNull("ints", ints);
        ByteBuffer buf = ByteBuffer.allocate(BYTES_SIZE);
        for (int i = 0; i < ints.length; i++) {
            buf.putShort((short)ints[i]);
        }
        buf.flip();
        this.high = buf.getLong();
        this.low = buf.getLong();
    }

    public Ipv6Address(byte... bytes) {
        notNull("bytes", bytes);
        if (bytes.length != BYTES_SIZE) {
            throw new IllegalArgumentException("Array size should be " + BYTES_SIZE + " but is " + bytes.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        this.high = buf.getLong();
        this.low = buf.getLong();
    }

    public Ipv6Address(BigInteger address) {
        notNull("address", address);
        byte[] target = new byte[16];
        byte[] bytes = address.toByteArray();
        System.arraycopy(bytes, 0, target, 16 - bytes.length, bytes.length);
        ByteBuffer buf = ByteBuffer.wrap(target);
        buf.order(ByteOrder.BIG_ENDIAN);
        this.high = buf.getLong();
        this.low = buf.getLong();
    }

    public Ipv6Address(CharSequence address) {
        this(parse(address));
    }

    public static int addressFamilyNumber() {
        return 2;
    }

    private static int[] parse(CharSequence s) {
        notNull("s", s);
        if (s.length() == 0) {
            throw new IllegalArgumentException("Zero-length address");
        }
        int[] ints = new int[8];
        int length = s.length();
        int lastBoundary = -1;
        int index = 0;
        int remainderStart = -1;
        boolean expectingDecimal = false;
        for (int i = 0; i < length; i++) {
            char currChar = s.charAt(i);
            switch (currChar) {
                case ':':
                case '.':
                    if (lastBoundary < i - 1) {
                        CharSequence sub = s.subSequence(lastBoundary + 1, i);
                        if (sub.length() > 4 && !expectingDecimal) {
                            throw new IllegalArgumentException("Hexadecimal address component must be 4 "
                                    + "characters or less but is '" + sub + "' at character " + i
                                    + ", component " + index + " in '" + s + "'");
                        }
                        if (sub.length() > 5 && expectingDecimal) {
                            throw new IllegalArgumentException("Decimal address component must be 4 characters or "
                                    + "less but is '" + sub + "' at character " + i + ", component " + index
                                    + " in '" + s + "'");
                        }
                        if (index >= 8) {
                            throw new IllegalArgumentException("Too many address components in '" + s
                                    + "' at character " + i + ", component " + index
                                    + " in '" + s + "'");
                        }
                        try {
                            ints[index] = expectingDecimal?Integer.parseInt(sub.toString())
                                    :Integer.parseInt(sub.toString(), 16);
                            if (ints[index] > 65535) {
                                throw new IllegalArgumentException("Value out of range: " + ints[index]
                                        + " (" + Integer.toHexString(ints[index]) + ") - must be between "
                                        + "0 and 65335 " + " at character " + i + ", component " + index
                                        + " in '" + s + "'");
                            }
                            index++;
                            lastBoundary = i;
                        } catch (NumberFormatException nfe) {
                            throw new IllegalArgumentException("Invalid number: '" + sub
                                    + "' " + " at character " + i + ", component " + index
                                    + " in '" + s + "'", nfe);
                        }
                    } else if (lastBoundary == i - 1) {
                        remainderStart = index;
                        lastBoundary = i;
                    }
                    expectingDecimal = currChar == '.';
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'a':
                case 'B':
                case 'b':
                case 'C':
                case 'c':
                case 'D':
                case 'd':
                case 'E':
                case 'e':
                case 'F':
                case 'f':
                    break;
                default:
                    throw new IllegalArgumentException("Invalid character at " + i + " '"
                            + currChar + "' at " + i + " in '" + s + "'");
            }
        }
        if (lastBoundary < length - 1) {
            if (index >= 8) {
                throw new IllegalArgumentException("Too many address components - " + (index + 1)
                        + " in '" + s + "'");
            }
            CharSequence sub = s.subSequence(lastBoundary + 1, length);
            try {
                ints[index] = Integer.parseInt(sub.toString(), 16);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid number: '" + sub
                        + "' " + " at character " + (length - 1) + ", component " + index
                        + " in '" + s + "'", nfe);
            }

            index++;
        }
        if (index <= 7 && remainderStart >= 0) {
            int moveLen = index - remainderStart;
            int moveTo = remainderStart + (8 - index);
            int[] temp = new int[moveLen];
            System.arraycopy(ints, remainderStart, temp, 0, temp.length);
            Arrays.fill(ints, remainderStart, remainderStart + temp.length, 0);
            System.arraycopy(temp, 0, ints, moveTo, temp.length);
        }
        return ints;
    }

    public InetAddress toInetAddress() throws UnknownHostException {
        return InetAddress.getByAddress(toByteArray());
    }

    public InetSocketAddress toDnsServerAddress() throws UnknownHostException {
        return new InetSocketAddress(toInetAddress(), 53);
    }

    public byte[] toByteArray() {
        ByteBuffer buf = ByteBuffer.allocate((Long.SIZE / 8) * 2);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(high);
        buf.putLong(low);
        return buf.array();
    }

    public int[] toIntArray() {
        int count = 8;
        final int[] ints = new int[count];

        for (int i = 0; i < count; i++) {
            long curr = i < 4?high:low;
            ints[i] = (int) (((curr << i * 16) >>> 16 * (count - 1)) & 0xFFFF);
        }
        return ints;
    }

    public BigInteger toBigInteger() {
        return new BigInteger(toByteArray());
    }

    public long high() {
        return high;
    }

    public long low() {
        return low;
    }

    public long[] toLongArray() {
        return new long[]{high(), low()};
    }

    private int valueAtIntegerPosition(int position) {
        long curr = position >= 0 && position < 4?high:low;
        int s = (int) (((curr << position * 16) >>> 16 * 7) & 0xFFFF);
        return s;
    }

    @Override
    public String toString() {
        int count = 8;
        StringBuilder sb = new StringBuilder(39);

        for (int i = 0; i < count; i++) {
            sb.append(String.format("%04x", valueAtIntegerPosition(i)));
            if (i != count - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    public String toStringShorthand() {
        int count = 8;
        StringBuilder sb = new StringBuilder(39);

        int zeroCount = -1;
        int zerosAt = -1;
        int currZeroCount = 0;
        int currZerosAt = -1;
        for (int i = 0; i < count; i++) {
            int s = valueAtIntegerPosition(i);
            if (s == 0) {
                if (currZerosAt == -1) {
                    currZerosAt = i;
                    currZeroCount = 1;
                } else {
                    currZeroCount++;
                }
            } else {
                if (currZerosAt != -1) {
                    if (zerosAt == -1 || (zeroCount != -1 && currZeroCount > zeroCount)) {
                        zerosAt = currZerosAt;
                        zeroCount = currZeroCount;
                    }
                }
                currZerosAt = -1;
                currZeroCount = 0;
            }
        }
        if (currZerosAt != -1) {
            if (currZeroCount > zeroCount) {
                zerosAt = currZerosAt;
                zeroCount = currZeroCount;
            }
        }
        for (int i = 0; i < count; i++) {
            if (zerosAt != -1) {
                if (i >= zerosAt && i < zerosAt + zeroCount) {
                    if (zerosAt == i) {
                        sb.append("::");
                    }
                    continue;
                } else {
                    if (i != 0 && i != zerosAt + zeroCount) {
                        sb.append(':');
                    }
                }
            } else if (i != 0 && i != zerosAt + zeroCount) {
                sb.append(":");
            }
            int s = valueAtIntegerPosition(i);
            sb.append(Integer.toHexString(s));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ipv6Address && ((Ipv6Address) o).low == low
                && ((Ipv6Address) o).high == high;
    }

    @Override
    public int hashCode() {
        return (int) (this.low ^ (this.low >>> 32)) * 31567
                + (int) (this.high ^ (this.high >>> 32));
    }

    @Override
    public int compareTo(Ipv6Address o) {
        return o.low == low && o.high == high?0
                :toBigInteger().compareTo(o.toBigInteger());
    }
}
