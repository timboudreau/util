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
package com.mastfrog.util.collections;

import com.mastfrog.util.collections.CollectionUtils.IdentityComparator;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class ArrayBinarySetTest {

    String[] strings = new String[]{
        "bbb", "aaa", "ccc", "fff", "ggg", "jjj", "iii", "hhh", "kkk", "mmm", "lll"};

    String[] strings2 = new String[]{
        "bbb", "aaa", "ccc", "fff", "ggg", "jjj", "iii", "hhh", "kkk", "mmm", "lll", "nnn"};

    ArrayBinarySet<String> set = new ArrayBinarySet<>(false, true, new CollectionUtils.StringComparator(),
            strings);

    ArrayBinarySet<String> set2 = new ArrayBinarySet<>(false, true, new CollectionUtils.StringComparator(),
            strings2);

    @Test
    public void testContains() {
        assertFalse(set.contains(""));
        for (String s : strings) {
            assertTrue(s, set.contains(s));
            assertFalse(set.contains(s.substring(1)));
            assertFalse(set.contains(s + s));
        }
        for (String s : strings) {
            assertTrue(s, set.contains(s.toUpperCase()));
            assertFalse(set.contains(s.toUpperCase().substring(1)));
            assertFalse(set.contains(s.toUpperCase() + s.toUpperCase()));
        }
        for (String s : strings2) {
            assertTrue(s, set2.contains(s));
            assertFalse(set2.contains(s.substring(1)));
        }
        for (String s : strings2) {
            assertTrue(s, set2.contains(s.toUpperCase()));
            assertFalse(set2.contains(s.toUpperCase().substring(1)));
        }

        assertFalse(set.contains("bbbb"));

        assertTrue(set.contains("aAa"));
        assertTrue(set.contains("AAA"));
        assertTrue(set.contains("AaA"));
        assertFalse(set.contains("AbA"));

        assertTrue(set.contains("mmm"));
        assertTrue(set.contains("mMm"));
        assertFalse(set.contains("nNn"));
        assertFalse(set.contains("mMn"));

        assertTrue(set2.contains("mmm"));
        assertTrue(set2.contains("mMm"));
        assertTrue(set2.contains("nNn"));
        assertFalse(set2.contains("mMn"));

        assertEquals(new HashSet<>(Arrays.asList(strings)), set);
        assertEquals(set, new HashSet<>(Arrays.asList(strings)));
        assertEquals(new HashSet<>(Arrays.asList(strings)).hashCode(), set.hashCode());

        Arrays.sort(strings);
        Iterator<String> iter = set.iterator();
        for (int i = 0; i < strings.length; i++) {
            assertEquals(strings[i], iter.next());
            assertEquals(i != strings.length - 1, iter.hasNext());
        }
    }

    @Test
    public void testIdentity() {
        StringBuilder[] sb = new StringBuilder[4];
        for (int i = 0; i < sb.length; i++) {
            sb[i] = new StringBuilder("hello");
        }
        ArrayBinarySet<StringBuilder> abs = new ArrayBinarySet<>(false, true,
                new IdentityComparator(), sb);
        assertEquals(4, abs.size());

        for (int i = 0; i < sb.length; i++) {
            assertTrue(abs.contains(sb[i]));
        }

        assertFalse(abs.contains(new StringBuilder("hello")));
        assertFalse(abs.contains("hello"));
    }

//    @Test
    public void testPerf() {
        long time = 5000;
        int size = 20;
        Set<Integer> l = new HashSet<>(size);
        Random r = new Random(230);
        for (int i = 0; i < size; i++) {
            l.add(r.nextInt(Integer.MAX_VALUE - 2));
        }
        Set<Integer> a = new HashSet<>(l);
        int[] strs = ArrayUtils.toIntArray(l);
        ArrayBinarySet<Integer> b = new ArrayBinarySet<>(false, false, Comparator.naturalOrder(), l.toArray(new Integer[0]));

        ArrayUtils.shuffle(r, strs);

        int arrSetIter1 = exec2(b, time);
        int arrSetIter2 = exec2(b, time);
        int hashSetIter1 = exec2(a, time);
        int hashSetIter2 = exec2(a, time);

        System.out.println("hashSetIterations-1 " + hashSetIter1);
        System.out.println("hashSetIterations-2 " + hashSetIter2);
        System.out.println("arraSetIterations-2 " + arrSetIter1);
        System.out.println("arraSetIterations-2 " + arrSetIter2);

        int hashSet1 = exec(a, strs, time);
        int arrSet1 = exec(b, strs, time);

        int hashSet2 = exec(a, strs, time);
        int arrSet2 = exec(b, strs, time);

        System.out.println("HashSet-1 " + hashSet1);
        System.out.println("HashSet-2 " + hashSet2);
        System.out.println("ArraySet-1 " + arrSet1);
        System.out.println("ArraySet-2 " + arrSet2);
    }

    private int exec(Set<Integer> toTest, int[] testWith, long limit) {
        long start = System.currentTimeMillis();
        int count = 0;
        for (;;) {
            for (int i = 0; i < testWith.length; i++) {
                count++;
                boolean resA = toTest.contains(testWith[i]);
                boolean resB = toTest.contains(testWith[i] * -1);
                boolean resC = toTest.contains(Integer.MAX_VALUE);
                assertTrue(resA);
                assertFalse(resB);
                if (System.currentTimeMillis() - start >= limit) {
                    return count;
                }
            }
        }
    }

    private int exec2(Set<Integer> toTest, long limit) {
        long start = System.currentTimeMillis();
        int count = 0;
        for (;;) {
            count++;
            Iterator<Integer> iter = toTest.iterator();
            while(iter.hasNext()) {
                iter.next();
            }
            if (System.currentTimeMillis() - start >= limit) {
                return count;
            }
        }
    }
}
