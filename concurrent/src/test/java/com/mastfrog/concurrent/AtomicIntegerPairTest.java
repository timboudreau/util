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
package com.mastfrog.concurrent;

import static com.mastfrog.concurrent.AtomicIntegerPair.pack;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author tiboudre
 */
public class AtomicIntegerPairTest {

    static final int INITIAL_LEFT = 33075;
    static final int INITIAL_RIGHT = 1290342;
    static final long INITIAL = pack(INITIAL_LEFT, INITIAL_RIGHT);
    AtomicIntegerPair pair = new AtomicIntegerPair(INITIAL);

    @BeforeEach
    public void before() {
        pair = new AtomicIntegerPair(INITIAL);
    }

    @Test
    public void testToLong() {
        assertEquals(INITIAL, pair.toLong());
    }

    @Test
    public void testFetch() {
        pair.fetch((l, r) -> {
            assertEquals(INITIAL_LEFT, l);
            assertEquals(INITIAL_RIGHT, r);
        });
    }

    @Test
    public void testGet() {
        int[] vals = pair.get();
        assertEquals(INITIAL_LEFT, vals[0]);
        assertEquals(INITIAL_RIGHT, vals[1]);
    }

    @Test
    public void testUpdate() {
        pair.update(old -> old * 3, old -> old * 7);
        assertEquals(INITIAL_LEFT * 3, pair.left());
        assertEquals(INITIAL_RIGHT * 7, pair.right());
    }

    @Test
    public void testCompareAndSet() {
        assertFalse(pair.compareAndSet(1, 2, 3, 4));
        assertTrue(pair.compareAndSet(INITIAL_LEFT, INITIAL_RIGHT, 5, 6));
        assertEquals(5, pair.left());
        assertEquals(6, pair.right());
    }

    @Test
    public void testSwap() {
        pair.swap();
        assertEquals(INITIAL_RIGHT, pair.left());
        assertEquals(INITIAL_LEFT, pair.right());
    }

    @Test
    public void testSet() {
        pair.set(23, 42);
        assertEquals(23, pair.left());
        assertEquals(42, pair.right());
    }

    @Test
    public void testSetLeft() {
        pair.setLeft(57);
        assertEquals(INITIAL_RIGHT, pair.right());
        assertEquals(57, pair.left());
    }

    @Test
    public void testSetRight() {
        pair.setRight(62);
        assertEquals(INITIAL_LEFT, pair.left());
        assertEquals(62, pair.right());
    }

    @Test
    public void testToString() {
        assertEquals("(" + INITIAL_LEFT + ", " + INITIAL_RIGHT + ")", pair.toString());
    }

    @Test
    public void testUnsignedView() {
        long leftHi = (long) Integer.MAX_VALUE + 7L;
        long rightHi = UnsignedView.MAX_VALUE - 1000;
        UnsignedView u = pair.toUnsignedView();

        assertEquals(INITIAL_LEFT, u.left());
        assertEquals(INITIAL_RIGHT, u.right());

        u.fetch((l, r) -> {
            assertEquals(INITIAL_LEFT, l);
            assertEquals(INITIAL_RIGHT, r);
        });

        u.set(leftHi, rightHi);
        assertEquals(leftHi, u.left());
        assertEquals(rightHi, u.right());

        u.fetch((l, r) -> {
            assertEquals(leftHi, l);
            assertEquals(rightHi, r);
        });

        u.setLeft(0);
        assertEquals(rightHi, u.right());
        assertEquals(0, u.left());

        try {
            u.setLeft(-10);
            fail("Should not be able to set a negative value");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            u.setRight(-10);
            fail("Should not be able to set a negative value");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            u.setLeft(Long.MAX_VALUE);
            fail("Should not be able to set a value > " + UnsignedView.MAX_VALUE);
        } catch (IllegalArgumentException ex) {
            // ok
        }

        try {
            u.setRight(Long.MAX_VALUE);
            fail("Should not be able to set a value > " + UnsignedView.MAX_VALUE);
        } catch (IllegalArgumentException ex) {
            // ok
        }

        u.setLeft(UnsignedView.MAX_VALUE);
        assertEquals(UnsignedView.MAX_VALUE, u.left());
        u.setRight(UnsignedView.MAX_VALUE);
        assertEquals(UnsignedView.MAX_VALUE, u.right());
    }

}
