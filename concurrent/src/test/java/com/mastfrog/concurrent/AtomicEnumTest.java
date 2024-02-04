/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.concurrent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AtomicEnumTest {

    @Test
    public void testInit() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        assertSame(N.ONE, en.get());

        en = new AtomicEnum<>(N.THREE);
        assertSame(N.THREE, en.get());

        en.set(N.TWO);
        assertSame(N.TWO, en.get());

        assertEquals("two", en.toString());
        assertEquals(1, en.ordinal());
        assertEquals("TWO", en.name());
    }

    @Test
    public void testGetAndUpdate() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        N n = en.getAndUpdate(old -> {
            return N.FOUR;
        });
        assertEquals(N.ONE, n);
        assertEquals(N.FOUR, en.get());
    }

    @Test
    public void testUpdateAndGet() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        N n = en.updateAndGet(old -> {
            return N.FOUR;
        });
        assertEquals(N.FOUR, n);
        assertEquals(N.FOUR, en.get());
    }

    @Test
    public void testNext() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        for (int i = 0; i < 8; i++) {
            int currOrd = (i + 1) % N.values().length;
            N nxt = en.next();
            assertEquals(currOrd, nxt.ordinal());
        }
    }

    @Test
    public void testPrev() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        assertEquals(N.FOUR, en.prev());
        assertEquals(N.THREE, en.prev());
        assertEquals(N.TWO, en.prev());
        assertEquals(N.ONE, en.prev());
        assertEquals(N.FOUR, en.prev());
    }

    @Test
    public void testNulls() {
        assertNPE(() -> new AtomicEnum<N>((Class<N>) null));
        assertNPE(() -> new AtomicEnum<N>((N) null));
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        assertNPE(() -> en.set(null));
        assertNPE(() -> en.lazySet(null));
        assertNPE(() -> en.compareAndSet(null, N.THREE));
        assertNPE(() -> en.compareAndSet(N.THREE, null));
    }

    @Test
    public void testCompareAndSet() {
        AtomicEnum<N> en = new AtomicEnum<>(N.class);
        assertFalse(en.compareAndSet(N.THREE, N.FOUR));
        assertEquals(N.ONE, en.get());
        assertTrue(en.compareAndSet(N.ONE, N.TWO));
        assertFalse(en.compareAndSet(N.ONE, N.ONE));
        assertEquals(N.TWO, en.get());
    }

    private static void assertNPE(Runnable r) {
        try {
            r.run();
            fail("NPE should have been thrown");
        } catch (NullPointerException e) {
            // do nothing
        }
    }

    enum N {
        ONE,
        TWO,
        THREE,
        FOUR;

        public String toString() {
            return name().toLowerCase();
        }
    }

}
