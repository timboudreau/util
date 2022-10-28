/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.function.iteration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author timb
 */
public class IterateTest {

    /**
     * Test of monotonically method, of class Iterate.
     */
    @Test
    public void testMonotonically() {
        List<Integer> up = new ArrayList<>();
        Iterate.monotonically(0, 10, up::add);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), up);

        List<Integer> down = new ArrayList<>();
        Iterate.monotonically(9, -1, down::add);
        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), down);
    }

    @Test
    public void testLongsMonotonically() {
        List<Long> up = new ArrayList<>();
        Iterate.longsMonotonically(0, 10, up::add);
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), up);

        List<Long> down = new ArrayList<>();
        Iterate.longsMonotonically(9L, -1L, down::add);
        assertEquals(Arrays.asList(9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L, 0L), down);
    }

    private static Map<String, String> map(int size) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            result.put(Integer.toString(i), Integer.toString(i * 10));
        }
        return result;
    }

    private static List<String> list(int size) {
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(Integer.toString(i * 10));
        }
        return result;
    }

    /**
     * Test of each method, of class Iterate.
     */
    @Test
    public void testEach_Map_ThrowingBiConsumer() {
        Map<String, String> map = map(25);
        Map<String, String> copy = new HashMap<>(25);
        Iterate.each(map, (k, v) -> {
            copy.put(k, v);
        });
        assertEquals(map, copy);
    }

    /**
     * Test of each method, of class Iterate.
     */
    @Test
    public void testEach_Iterable_ThrowingConsumer() {
        List<String> strings = list(33);
        List<String> nue = new ArrayList<>(33);
        Iterate.each(strings, item -> {
            nue.add(item);
        });
        assertEquals(strings, nue);
    }

    /**
     * Test of each method, of class Iterate.
     */
    @Test
    public void testEach_GenericType_ThrowingConsumer() {
    }

    /**
     * Test of eachIO method, of class Iterate.
     */
    @Test
    public void testEachIO_Map_IOBiConsumer() {
        Map<String, String> map = map(25);
        Map<String, String> copy = new HashMap<>(25);
        Iterate.eachIO(map, (k, v) -> {
            copy.put(k, v);
        });
        assertEquals(map, copy);
    }

    /**
     * Test of iterate method, of class Iterate.
     */
    @Test
    public void testIterate_Iterable_IterationConsumer() {
        List<String> l = list(33);
        List<String> nue = new ArrayList<>(33);
        Iterate.iterate(l, (item, first, last) -> {
            switch (item) {
                case "0":
                    assertTrue(first, item + " first " + first + " last " + last);
                    assertFalse(last, item + " first " + first + " last " + last);
                    break;
                case "320":
                    assertFalse(first, item + " first " + first + " last " + last);
                    assertTrue(last, item + " first " + first + " last " + last);
                    break;
                default:
                    assertFalse(first, item + " first " + first + " last " + last);
                    assertFalse(last, item + " first " + first + " last " + last);
            }
            nue.add(item);
        });
        assertEquals(l, nue);
    }

    /**
     * Test of iterate method, of class Iterate.
     */
    @Test
    public void testIterate_GenericType_IterationConsumer() {
        List<String> l = list(33);
        List<String> nue = new ArrayList<>(33);
        Iterate.iterate(l.toArray(new String[33]), (item, first, last) -> {
            switch (item) {
                case "0":
                    assertTrue(first, item + " first " + first + " last " + last);
                    assertFalse(last, item + " first " + first + " last " + last);
                    break;
                case "320":
                    assertFalse(first, item + " first " + first + " last " + last);
                    assertTrue(last, item + " first " + first + " last " + last);
                    break;
                default:
                    assertFalse(first, item + " first " + first + " last " + last);
                    assertFalse(last, item + " first " + first + " last " + last);
            }
            nue.add(item);
        });
        assertEquals(l, nue);
    }

}
