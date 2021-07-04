/*
 * Copyright (c) 2021, Mastfrog Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.bits;

import com.mastfrog.util.preconditions.InvalidArgumentException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicBitsImplTest {

    private Random rnd;
    private static final int SIZE = (64 * 5) + 7;

    @Test
    public void testInverted() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        for (int i = 0; i < 128; i++) {
            assertFalse(bits.get(i));
            if (i % 2 == 0) {
                bits.set(i);
                assertTrue(bits.get(i));
            }
        }
        Bits inv = bits.inverted();
        for (int i = 0; i < 128; i++) {
            if (i % 2 == 0) {
                assertFalse(inv.get(i));
            } else {
                assertTrue(inv.get(i));
            }
        }
    }

    @Test
    public void testCannotCreateWithNegativeCapacity() {
        try {
            AtomicBitsImpl bits = new AtomicBitsImpl(-128);
            fail("Exception should be thrown, but got " + bits);
        } catch (InvalidArgumentException ex) {

        }
    }

    @Test
    public void testCannotCreateWithZeroCapacity() {
        try {
            AtomicBitsImpl bits = new AtomicBitsImpl(0);
            fail("Exception should be thrown, but got " + bits);
        } catch (InvalidArgumentException ex) {

        }
    }

    @Test
    public void testCannotSetValuesGreaterThanCapacity() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        assertEquals(128, bits.capacity());
        for (int i = 0; i < 128; i++) {
            assertFalse(bits.get(i));
            bits.set(i);
            assertTrue(bits.get(i));
        }
        assertFalse(bits.canContain(-2));
        assertFalse(bits.canContain(200));
        assertFalse(bits.canContain(128));
        try {
            boolean res = bits.setting(200);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // ok
        }
    }

    @Test
    public void testCannotClerValuesGreaterThanCapacity() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        assertEquals(128, bits.capacity());
        for (int i = 0; i < 128; i++) {
            assertFalse(bits.get(i));
            bits.set(i);
            assertTrue(bits.get(i));
        }
        try {
            bits.clearing(200);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            // ok
        }
    }

    @Test
    public void testCapacityCanBeIncreasedByCopy() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        assertEquals(128, bits.capacity());
        for (int i = 0; i < 128; i++) {
            assertFalse(bits.get(i));
            bits.set(i);
            assertTrue(bits.get(i));
        }
        AtomicBitsImpl larger = bits.copy(256);
        assertEquals(256, larger.capacity());
        for (int i = 0; i < 128; i++) {
            assertTrue(larger.get(i));
        }
        for (int i = 128; i < 256; i++) {
            assertFalse(larger.get(i));
            larger.set(i);
            assertTrue(larger.get(i));
        }

    }

    @Test
    public void testCannotSetNegativeValues() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        try {
            boolean x = bits.setting(-10);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
    }

    @Test
    public void testCannotClearNegativeValues() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        try {
            bits.clearing(-10);
            fail("Exception should have been thrown");
        } catch (IndexOutOfBoundsException ex) {

        }
    }

    @Test
    public void testSetAndClearGroups() {
        int size = 64 * 3;
        AtomicBitsImpl bits = new AtomicBitsImpl(size);

        bits.set(14, size - 43);
        for (int i = 0; i < size; i++) {
            if (i < 14 || i >= size - 43) {
                assertFalse(bits.get(i), "Bit " + i + " should NOT be set, in " + bits);
            } else {
                assertTrue(bits.get(i), "Bit " + i + " *should* be set, in " + bits);
            }
        }

        bits.clear((size / 2) - 10, (size / 2) + 10);
        for (int i = 0; i < size; i++) {
            if (i < 14 || i >= size - 43 || (i >= (size / 2) - 10 && i < (size / 2) + 10)) {
                assertFalse(bits.get(i), "Bit " + i + " should NOT be set, in " + bits);
            } else {
                assertTrue(bits.get(i), "Bit " + i + " *should* be set, in " + bits);
            }
        }
    }

    @Test
    public void testSerialization() throws Exception {
        MutableBits mb = MutableBits.create(SIZE);
        AtomicBitsImpl atomic = new AtomicBitsImpl(SIZE);
        Bits canonical = fill(mb, atomic, SIZE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(baos)) {
            oout.writeObject(atomic);
        }
        AtomicBitsImpl deserialized;
        try (ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (AtomicBitsImpl) oin.readObject();
        }
        assertEquals(atomic, deserialized);
        assertNotSame(atomic, deserialized);
        assertEquals(canonical, deserialized);
    }

    @Test
    public void testEqualsAndHashCode() {
        MutableBits mb = MutableBits.create(SIZE);
        AtomicBitsImpl atomic = new AtomicBitsImpl(SIZE);
        Bits canonical = fill(mb, atomic, SIZE);

        assertEquals(mb, atomic);
        assertEquals(atomic, mb);
        assertEquals(mb.hashCode(), atomic.hashCode());
        assertEquals(canonical, atomic);
    }

    @Test
    public void testSettingNextClearBitInSmallSet() {
        AtomicBitsImpl bits = new AtomicBitsImpl(46);
        for (int i = 0; i < bits.capacity(); i++) {
            int val = bits.settingNextClearBit(i);
            assertEquals(i, val, "Next clear bit in " + bits + " should be " + i + " not " + val);
        }
        assertEquals(-1, bits.settingNextClearBit(46), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(47), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(45), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(23), "Once full, settingNextClearBit() should always return -1 but didn't for 23 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(0), "Once full, settingNextClearBit() should always return -1 but didn't for 0 of " + bits.capacity());

    }

    @Test
    public void testClearedIntermediateBits() {
        AtomicBitsImpl bits = new AtomicBitsImpl(46);
        for (int i = 0; i < bits.capacity(); i++) {
            assertFalse(bits.get(i), "Bit " + i + " should not already be set");
            boolean wasSet = bits.setting(i);
            assertTrue(wasSet, "Should have returned true from set " + i + " in " + bits);
            assertTrue(bits.get(i), "Bit " + i + " should now be set");
        }
        assertTrue(bits.clearing(30), "Clear 30 should succeed");
        assertTrue(bits.clearing(33), "Clear 33 should succeed");
        assertTrue(bits.clearing(36), "Clear 36 should succeed");
        for (int i = 0; i < bits.capacity(); i++) {
            switch (i) {
                case 30:
                case 33:
                case 36:
                    assertFalse(bits.get(i), "Bit " + i + " should have been cleared");
                    break;
                default:
                    assertTrue(bits.get(i), "Bit " + i + " should not have been cleared");
            }
        }

        assertEquals(30, bits.settingNextClearBit(0));
        assertEquals(36, bits.settingNextClearBit(34));
        assertEquals(33, bits.settingNextClearBit(32));
        assertEquals(-1, bits.settingNextClearBit(0));
    }

    @Test
    public void testClearedIntermediateInLargerSet() {
        AtomicBitsImpl bits = new AtomicBitsImpl(97);
        for (int i = 0; i < bits.capacity(); i++) {
            assertFalse(bits.get(i), "Bit " + i + " should not already be set");
            boolean wasSet = bits.setting(i);
            assertTrue(wasSet, "Should have returned true from set " + i + " in " + bits);
            assertTrue(bits.get(i), "Bit " + i + " should now be set");
        }
        assertTrue(bits.clearing(30), "Clear 30 should succeed");
        assertTrue(bits.clearing(33), "Clear 33 should succeed");
        assertTrue(bits.clearing(36), "Clear 36 should succeed");
        assertTrue(bits.clearing(67), "Clear 67 should succeed");
        assertTrue(bits.clearing(68), "Clear 68 should succeed");
        assertTrue(bits.clearing(73), "Clear 73 should succeed");
        for (int i = 0; i < bits.capacity(); i++) {
            switch (i) {
                case 30:
                case 33:
                case 36:
                case 67:
                case 68:
                case 73:
                    assertFalse(bits.get(i), "Bit " + i + " should have been cleared");
                    break;
                default:
                    assertTrue(bits.get(i), "Bit " + i + " should not have been cleared");
            }
        }

        assertEquals(30, bits.settingNextClearBit(0));
        assertEquals(36, bits.settingNextClearBit(34));
        assertEquals(33, bits.settingNextClearBit(32));

        String old = bits.toBitsString() + " " + bits;
        assertEquals(73, bits.settingNextClearBit(70),
                () -> "Next clear should be " + 73 + " in " + old);
        assertEquals(68, bits.settingNextClearBit(68));
        assertEquals(67, bits.settingNextClearBit(0));

        assertTrue(bits.clearing(95));
        assertTrue(bits.clearing(0));
        assertEquals(95, bits.settingNextClearBit(1));
        assertEquals(0, bits.settingNextClearBit(0));
    }

    @Test
    public void testSettingNextClearBitInExactly64BitSet() {
        AtomicBitsImpl bits = new AtomicBitsImpl(64);
        for (int i = 0; i < bits.capacity(); i++) {
            int val = bits.settingNextClearBit(i);
            assertEquals(i, val, "Next clear bit in " + bits + " should be " + i + " not " + val);
        }
        assertEquals(-1, bits.settingNextClearBit(64), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(65), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(63), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(36), "Once full, settingNextClearBit() should always return -1 but didn't for 23 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(0), "Once full, settingNextClearBit() should always return -1 but didn't for 0 of " + bits.capacity());
    }

    @Test
    public void testSettingNextClearBitInExactly128BitSet() {
        AtomicBitsImpl bits = new AtomicBitsImpl(128);
        for (int i = 0; i < bits.capacity(); i++) {
            int val = bits.settingNextClearBit(i);
            assertEquals(i, val, "Next clear bit in " + bits + " should be " + i + " not " + val);
        }
        assertEquals(-1, bits.settingNextClearBit(128), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(127), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(129), "Once full, settingNextClearBit() should always return -1 but didn't for 46 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(64), "Once full, settingNextClearBit() should always return -1 but didn't for 23 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(63), "Once full, settingNextClearBit() should always return -1 but didn't for 23 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(65), "Once full, settingNextClearBit() should always return -1 but didn't for 23 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(0), "Once full, settingNextClearBit() should always return -1 but didn't for 0 of " + bits.capacity());
    }

    @Test
    public void testSettingNextClearBitLargerSet() {
        AtomicBitsImpl bits = new AtomicBitsImpl(96);
        for (int i = 0; i < bits.capacity(); i++) {
            int val = bits.settingNextClearBit(i);
            assertEquals(i, val, "Next clear bit in " + bits + " should be " + i + " not " + val);
        }
        assertEquals(-1, bits.settingNextClearBit(96), "Once full, settingNextClearBit() should always return -1 but didn't for 96 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(97), "Once full, settingNextClearBit() should always return -1 but didn't for 97 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(95), "Once full, settingNextClearBit() should always return -1 but didn't for 95 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(48), "Once full, settingNextClearBit() should always return -1 but didn't for 48 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(47), "Once full, settingNextClearBit() should always return -1 but didn't for 47 of " + bits.capacity());
        assertEquals(-1, bits.settingNextClearBit(0), "Once full, settingNextClearBit() should always return -1 but didn't for 0 of " + bits.capacity());
    }

    @Test
    public void testSettingNext() {
        AtomicBitsImpl ab = new AtomicBitsImpl(SIZE);
        int bit = 0;
        for (int i = 0; i < 133; i++) {
            bit = ab.settingNextClearBit(i);
            String n = i < 10 ? "0" + i : Integer.toString(i);
//            System.out.println(nf(i) + ". " + nf(bit) + "\t" + ab.toBitsString());
        }
        assertEquals(133, ab.nextClearBit(0));
    }

    @Test
    public void testSettingNextDoesNotOverflowStack() {
        for (int sz = 62; sz < 514; sz++) {
            AtomicBitsImpl ab = new AtomicBitsImpl(sz);
            int val = -1;
            do {
                int nue = ab.settingNextClearBit(val);
                if (nue != -1) {
                    assertEquals(val + 1, nue, "Wrong target for " + val);
                }
                val = nue;
            } while (val >= 0);
        }
    }

    @Test
    public void testSettingNextParallel() throws InterruptedException {
        final int size = (1024 * 1024) + 333;
        AtomicBitsImpl bits = new AtomicBitsImpl(size);
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        int threads = 5;
        Phaser phaser = new Phaser(threads + 1);
        RR[] rrs = new RR[threads];
        for (int i = 0; i < threads; i++) {
            rrs[i] = new RR(i, counter, size, bits, phaser, latch);
            Thread t = new Thread(rrs[i], "rr-" + i);
            t.setDaemon(true);
            t.start();
        }
        phaser.arriveAndDeregister();
        latch.await(30, TimeUnit.SECONDS);
        MutableBits combined = MutableBits.create(size);
        int sum = 0;
        for (RR rr : rrs) {
            sum += rr.setCount;
            Bits b1 = Bits.fromBitSet(rr.seen);
            combined.or(b1);
            for (RR rr2 : rrs) {
                if (rr == rr2) {
                    continue;
                }
                Bits b2 = Bits.fromBitSet(rr2.seen);

                assertFalse(b1.intersects(b2), "rr-" + rr.ix + " and " + rr2.ix + " set some of the same bits:\n"
                        + b1 + " vs\n" + b2);
            }
        }
    }

    static class RR implements Runnable {

        final BitSet seen = new BitSet();
        int lastBit = 0;
        private final int ix;
        private final AtomicInteger counter;
        private final int size;
        private final AtomicBitsImpl bits;
        private final Phaser phaser;
        private final CountDownLatch done;
        private int setCount;

        public RR(int ix, AtomicInteger counter, int size, AtomicBitsImpl bits, Phaser phaser, CountDownLatch done) {
            this.ix = ix;
            this.counter = counter;
            this.size = size;
            this.bits = bits;
            this.phaser = phaser;
            this.done = done;
        }

        @Override
        public void run() {
            try {
                phaser.arriveAndAwaitAdvance();
                while (counter.incrementAndGet() < size) {
                    lastBit = bits.settingNextClearBit(lastBit);
                    if (lastBit < 0 || lastBit >= size) {
                        break;
                    }
                    assertFalse(seen.get(lastBit));
                    seen.set(lastBit);
                    setCount++;
//                    Thread.yield();
                }
            } finally {
                done.countDown();
            }
        }

    }

    String nf(int val) {
        StringBuilder sb = new StringBuilder(3).append(val);
        while (sb.length() < 3) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }

    @Test
    public void testIteration() {
        MutableBits mb = MutableBits.create(SIZE);
        AtomicBitsImpl atomic = new AtomicBitsImpl(SIZE);
        Bits canonical = fill(mb, atomic, SIZE);

        String aa = stringify(mb);
        String bb = stringify(atomic);
        String cc = stringify(canonical);

        String aaR = stringifyReverse(mb, SIZE);
        String bbR = stringifyReverse(atomic, SIZE);
        String ccR = stringifyReverse(canonical, SIZE);

        String aaC = stringifyClear(mb);
        String bbC = stringifyClear(atomic);

        String aaRc = stringifyReverseClear(mb, SIZE);
        String bbRc = stringifyReverseClear(atomic, SIZE);

        assertEquals(aa, bb, "Set ascending difers");
        assertEquals(aaC, bbC, "Clear ascending differs");
        assertEquals(aaR, bbR, "Set descending differs");

        assertEquals(mb.cardinality(), atomic.cardinality(), "Cardinalities differ");
    }

    private String stringifyReverse(Bits bits, int size) {
        StringBuilder sb = new StringBuilder();
        bits.forEachSetBitDescending(size, bit -> {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(bit);
        });
        return sb.toString();
    }

    private String stringifyReverseClear(Bits bits, int size) {
        StringBuilder sb = new StringBuilder();
        bits.forEachUnsetBitDescending(size, bit -> {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(bit);
        });
        return sb.toString();
    }

    private String stringify(Bits bits) {
        StringBuilder sb = new StringBuilder();
        bits.forEachSetBitAscending(bit -> {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(bit);
        });
        return sb.toString();
    }

    private String stringifyClear(Bits bits) {
        StringBuilder sb = new StringBuilder();
        bits.forEachUnsetBitAscending(bit -> {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(bit);
            if (bit >= SIZE) {
                return false;
            }
            return true;
        });
        return sb.toString();
    }

    private Bits fill(MutableBits a, MutableBits b, int size) {
        BitSet bs = new BitSet(size);
        for (int i = 0; i < size; i++) {
            if (i / 64 == 2) {
                continue;
            }
            if (rnd.nextBoolean()) {
                a.set(i);
                b.set(i);
                bs.set(i);
            }
        }
        return Bits.fromBitSet(bs);
    }

    @BeforeEach
    public void setup() {
        rnd = new Random(12090139013L);
    }
}
