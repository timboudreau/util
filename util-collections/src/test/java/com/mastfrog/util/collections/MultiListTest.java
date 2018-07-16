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

import com.mastfrog.util.strings.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class MultiListTest {

    List<String> a = new ArrayList<>(Arrays.asList("A", "B", "C"));
    List<String> b = new ArrayList<>(Arrays.asList("D", "E", "F", "G"));
    List<String> c = new ArrayList<>(Arrays.asList("H", "I", "J", "K"));
    List<String> d = new ArrayList<>(Arrays.asList("L"));
    List<String> e = new ArrayList<>(Arrays.asList(""));
    List<String> f = new ArrayList<>(Arrays.asList("M", "N", "O", "P", "Q"));
    List<String> g = new ArrayList<>(Arrays.asList("R", "S"));
    List<String> h = new ArrayList<>(Arrays.asList("T", "U", "V", "W", "X", "Y", "Z"));

    List<String> all = new ArrayList<>();
    String str = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
    List<List<String>> lists = new ArrayList<>();
    String[] array;
    List<ListIterator<String>> iters = new ArrayList<>();

    @Before
    public void setup() {
        all.clear();
        all.addAll(a);
        all.addAll(b);
        all.addAll(c);
        all.addAll(d);
        all.addAll(e);
        all.addAll(f);
        all.addAll(g);
        all.addAll(h);
        lists.clear();
        lists.add(a);
        lists.add(b);
        lists.add(c);
        lists.add(d);
        lists.add(e);
        lists.add(f);
        lists.add(g);
        lists.add(h);

        iters.clear();
        iters.add(a.listIterator());
        iters.add(b.listIterator());
        iters.add(c.listIterator());
        iters.add(d.listIterator());
        iters.add(e.listIterator());
        iters.add(f.listIterator());
        iters.add(g.listIterator());
        iters.add(h.listIterator());

        array = all.toArray(new String[all.size()]);

        assertEquals(str, Strings.join(',', str));
    }

    @Test
    public void testSizeAndIteration() {
        List<String> multi = CollectionUtils.combinedList(lists);
        assertEquals(all.size(), multi.size());
        Iterator<String> ai = all.iterator();
        Iterator<String> bi = multi.iterator();
        int pos = 0;
        while (ai.hasNext()) {
            assertTrue(bi.hasNext());
            String a = ai.next();
            String b = bi.next();
            String c = multi.get(pos);
            assertEquals("Differ at " + pos++, a, b);
            assertEquals("Get returns wrong value for " + pos, a, c);
        }
        assertEquals(all, multi);

        assertEquals(all.hashCode(), multi.hashCode());
    }

    @Test
    public void testToArray() {
        List<String> multi = CollectionUtils.combinedList(lists);
        String[] arr = multi.toArray(new String[0]);
        assertArrayEquals(array, arr);

        arr = multi.toArray(new String[multi.size()]);
        assertArrayEquals(array, arr);
    }

    @Test
    public void testSingleList() {
        List<String> multi = CollectionUtils.combinedList(Arrays.asList(a));
        assertEquals(a.size(), multi.size());
        assertEquals(a, multi);
    }

    @Test
    public void testMergeListIterator() {
        MergeListIterator<String> it = new MergeListIterator<String>(iters);
        ListIterator<String> x = all.listIterator();
        while (x.hasNext()) {
            assertTrue("No next, but should be ", it.hasNext());
            String a = it.next();
            String b = x.next();
            assertEquals(a, b);
            assertEquals(x.nextIndex(), it.nextIndex());
            assertEquals(x.previousIndex(), it.previousIndex());
            assertEquals(x.hasPrevious(), it.hasPrevious());
            assertEquals(x.hasNext(), it.hasNext());
        }
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("Exception should have been thrown");
        } catch (NoSuchElementException ex) {

        }

        while (x.hasPrevious()) {
            assertTrue(it.hasPrevious());
            String a = it.previous();
            String b = x.previous();
            assertEquals(a, b);
        }
        assertFalse(it.hasPrevious());
        try {
            it.previous();
            fail("Exception should have been thrown");
        } catch (NoSuchElementException ex) {

        }
        while (x.hasNext()) {
            assertTrue("No next, but should be ", it.hasNext());
            String a = it.next();
            String b = x.next();
            assertEquals(a, b);
            assertEquals(x.nextIndex(), it.nextIndex());
            assertEquals(x.previousIndex(), it.previousIndex());
            assertEquals(x.hasPrevious(), it.hasPrevious());
            assertEquals(x.hasNext(), it.hasNext());
        }
    }
}
