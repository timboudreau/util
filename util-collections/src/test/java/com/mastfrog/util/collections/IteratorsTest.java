/*
 * The MIT License
 *
 * Copyright 2010-2015 Tim Boudreau.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IteratorsTest {

    @Test
    public void test() {
        List<Integer> a = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> b = new ArrayList<>(Arrays.asList(5, 6, 7));
        Iterator<Integer> merged = CollectionUtils.<Integer>combine(a.iterator(), b.iterator());
        List<Integer> merge = new ArrayList<>();
        while (merged.hasNext()) {
            merge.add(merged.next());
        }
        List<Integer> expect = new ArrayList<>(a);
        expect.addAll(b);
        assertEquals(expect, merge);
    }

    @Test
    public void testSingleItemList() {
        List<Integer> theList = CollectionUtils.oneItemList();
        assertTrue(theList.isEmpty());
        assertFalse(theList.contains(1));
        assertEquals(0, theList.size());
        theList.add(1);
        assertEquals(1, theList.size());
        assertFalse(theList.isEmpty());
        assertTrue(theList.contains(1));
        assertTrue(theList.containsAll(Arrays.asList(1)));
        assertFalse(theList.containsAll(Arrays.asList(1, 2, 3)));
        theList.retainAll(Arrays.asList(1, 2, 3));
        assertFalse(theList.isEmpty());
        assertEquals(Integer.valueOf(1), theList.iterator().next());
        theList.retainAll(Arrays.asList(2, 3, 4));
        assertTrue(theList.isEmpty());

        theList.add(2);
        assertEquals(Arrays.asList(2), theList);
        assertEquals(theList, Arrays.asList(2));
        theList.clear();
        assertTrue(theList.isEmpty());
        assertEquals(theList, Arrays.asList());
        
        assertFalse(theList.remove(Integer.valueOf(23)));
        theList.add(23);
        assertTrue(theList.remove(Integer.valueOf(23)));
        assertTrue(theList.isEmpty());
        
        theList.add(42);
        try {
            theList.add(43);
            fail("IndexOutOfBoundsException should have been thrown");
        } catch (IndexOutOfBoundsException ex) {
            
        }
    }
}
