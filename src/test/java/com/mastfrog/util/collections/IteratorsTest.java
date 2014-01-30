package com.mastfrog.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IteratorsTest {

    @Test
    public void test() {
        List<Integer> a = new ArrayList<>(Arrays.asList(1,2,3,4));
        List<Integer> b = new ArrayList<>(Arrays.asList(5,6,7));
        Iterator<Integer> merged = CollectionUtils.<Integer>combine(a.iterator(), b.iterator());
        List<Integer> merge = new ArrayList<>();
        while (merged.hasNext()) {
            merge.add(merged.next());
        }
        List<Integer> expect = new ArrayList<>(a);
        expect.addAll(b);
        assertEquals(expect, merge);
    }
    
}
