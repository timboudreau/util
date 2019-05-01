package com.mastfrog.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntPathTest {

    @Test
    public void testCopy() {
        IntPath a = new IntPath(5).add(2).add(4).add(6).add(8).add(10).add(12).add(14);
        IntPath b = a.copy();
        assertEquals(a.size(), b.size());
        assertNotSame(a, b);
        assertEquals(a, b);
    }

    @Test
    public void testCombine() {
        IntPath a = new IntPath(5).add(2).add(4).add(6).add(8).add(10).add(12).add(14);
        IntPath b = new IntPath(5).add(1).add(3).add(5).add(7).add(9).add(11).add(13);
        IntPath e = new IntPath(5).add(2).add(4).add(6).add(8).add(10).add(12).add(14).add(1).add(3).add(5).add(7).add(9).add(11).add(13);
        assertEquals(7, a.size());
        a.append(b);
        assertEquals(14, a.size());
        assertEquals(e, a);
        IntPath replacement = new IntPath().add(3).add(4).add(5).add(6).add(7);
        IntPath exp = new IntPath().add(2).add(3).add(4).add(5).add(6).add(7);
        a.replace(1, replacement);
        assertEquals(exp, a);

        IntPath expRev = new IntPath().add(7).add(6).add(5).add(4).add(3).add(2);
        assertEquals(expRev, exp.reversed());
    }

    @Test
    public void testSimpleMethods() {
        IntPath a = new IntPath(5).add(2).add(4).add(6).add(8).add(10).add(12).add(14);
        IntPath b = new IntPath(5).add(2).add(4).add(6).add(8).add(10).add(12).add(14);
        assertEquals(a, b);
        assertEquals(a, a.copy());
        assertNotSame(a, a.copy());
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.contains(b));
        assertFalse(a.contains(new IntPath(1).add(3)));
        assertFalse(a.isEmpty());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(i, a.indexOf(a.get(i)));
        }

        for (int i = 1; i < a.size(); i++) {
            IntPath test = new IntPath();
            for (int j = 0; j < i; j++) {
                test.add(a.get(j));
            }
            IntPath up = test.copy();
            do {
                assertTrue("Should contain " + up, a.contains(up));
                up = up.parentPath();
            } while (!up.isEmpty());
            IntPath down = test.copy();
            do {
                assertTrue("Should contain " + down, a.contains(down));
                down = down.childPath();
            } while (!down.isEmpty());
        }
    }

}
