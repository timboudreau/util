/*
 * The MIT License
 *
 * Copyright 2018 tim.
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
package com.mastfrog.util.thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class AtomicLinkedQueueTest {

    @Test
    public void testAtomicQueue() {
        AtomicLinkedQueue<String> q = new AtomicLinkedQueue<>();
        q.add("a").add("b").add("c");
        assertEquals(3, q.size());
        List<String> l = q.drain();
        assertEquals(Arrays.asList("a", "b", "c"), l);
        assertTrue(q.isEmpty());

        q.add("a").add("b").add("c");
        q.removeByIdentity("b");
        assertEquals(2, q.size());
        assertEquals(Arrays.asList("a", "c"), q.drain());
        assertTrue(q.isEmpty());

        q.add("a").add("b").add("c");
        assertEquals(3, q.size());
        q.pop();
        assertEquals(Arrays.asList("a", "b"), q.drain());

        q.add("a").add("b").add("c").add("d");
        q.filter(s -> {
            switch (s) {
                case "a":
                case "b":
                    return true;
                default:
                    return false;
            }
        }, (left, right) -> {
            assertEquals(Arrays.asList("a", "b"), left.drain());
            assertEquals(Arrays.asList("c", "d"), right.drain());
        });
        assertEquals(4, q.size());

        q.reverseInPlace();;
        assertEquals(Arrays.asList("d", "c", "b", "a"), q.drain());

        q.add("a").add("b").add("c").add("d");
        l = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            l.add(q.pop(() -> "x"));
        }
        assertEquals(Arrays.asList("d", "c", "b", "a", "x"), l);
    }
}
