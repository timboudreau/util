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
