package com.mastfrog.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntPathTest {

    /**
     * Join this path with another path, if they share a start/end pair, without
     * duplicating elements; if <code>eliminateOverlap</code> is passed, then
     * whichever the latter path is in the result, it will be truncated so as
     * not to include any elements also present in the other - this is commonly
     * needed when computing sets of paths that have no partial overlap; returns
     * null if the start/end pairs of neither match.
     * <p>
     * Examples:
     * <ul>
     * <li>Joining 1,2,3 with 3,4,5, gets 1,2,3,4,5 </li>
     * <li>Joining 3,4,5, with 1,2,3 also gets 1,2,3,4,5 </li>
     * <li>Joining 3,4,5,1,2 with 1,2,3 also gets 1,2,3,4,5,1,2 if
     * <code>elimimnateOverlap</code> is <b>false</b></li>
     * <li>Joining 3,4,5,1,2 with 1,2,3 also gets 1,2,3,4,5 if
     * <code>elimimnateOverlap</code> is <b>true</b></li>
     * <li>Joining 1,2,3 with 4,5,6 gets null because they don't share a start
     * or an end</li>
     * <li>Ambiguous cases such as 1,2,1 and 1,3,4,1 are joined with the
     * argument prepended to this path, resulting in 1,3,4,1,2,1</li>
     * <li>In overlap elimination, only the first overlapping element is
     * considered, so joining 1,1,1,2 and 2,2,2,1 gets 1,1,1,2,2,2</li>
     * </ul>
     *
     * </p>
     */
    @Test
    public void testJoin() {
        assertEquals(IntPath.of(1, 2, 3, 4, 5), IntPath.of(1, 2, 3).join(IntPath.of(3, 4, 5)));
        assertEquals(IntPath.of(1, 2, 3, 4, 5), IntPath.of(3, 4, 5).join(IntPath.of(1, 2, 3)));
        assertEquals(IntPath.of(1, 2, 3, 5, 1, 2), IntPath.of(3, 5, 1, 2).join(IntPath.of(1, 2, 3)));
        assertEquals(IntPath.of(1, 2, 3, 4, 5, 1, 2), IntPath.of(3, 4, 5, 1, 2).join(IntPath.of(1, 2, 3)));
        assertEquals(IntPath.of(1, 2, 3, 4, 5), IntPath.of(3, 4, 5, 1, 2).join(IntPath.of(1, 2, 3), true));
        assertEquals(IntPath.of(1, 2, 3, 4, 5), IntPath.of(1, 2, 3).join(IntPath.of(3, 4, 5, 1, 2), true));
        assertEquals(IntPath.of(1, 2, 3, 4, 5, 6, 7), IntPath.of(1, 2, 3).join(IntPath.of(3, 4, 5, 6, 7, 1, 2), true));

        assertEquals(IntPath.of(2, 1, 5, 4, 3, 7, 8, 9), IntPath.of(3, 7, 8, 9, 2, 1).join(IntPath.of(2, 1, 5, 4, 3), true));
        assertEquals(IntPath.of(1, 5, 4, 3, 7, 8), IntPath.of(2, 1, 5, 4, 3).join(IntPath.of(3, 7, 8, 1), true));

        assertEquals(IntPath.of(1, 7, 6, 5, 4, 3, 2), IntPath.of(3, 2, 1).join(IntPath.of(1, 7, 6, 5, 4, 3), true));
    }

    @Test
    public void testCanJoin() {
        assertTrue(IntPath.of(1, 2, 3).canJoin(IntPath.of(3, 4, 5)));
        assertFalse(IntPath.of(1, 2, 3).canJoin(IntPath.of(1, 2, 3)));
        assertFalse(IntPath.of(1, 2, 3).canJoin(IntPath.of(4, 5, 6)));
        assertTrue(IntPath.of(1, 1, 1).canJoin(IntPath.of(1, 2, 1)));
        assertTrue(IntPath.of(1, 1, 1).canJoin(IntPath.of(1, 1, 1)));
        assertTrue(IntPath.of(1, 1, 1).canJoin(IntPath.of(0, 1, 1)));
    }

    @Test
    public void testPrependingPath() {
        IntPath a = IntPath.of(3, 4, 5);
        IntPath b = IntPath.of(1, 2);
        IntPath ac = a.prepending(b);
        assertEquals(IntPath.of(1, 2, 3, 4, 5), ac);

        IntPath ca = b.prepending(a);
        assertEquals(IntPath.of(3, 4, 5, 1, 2), ca);
    }

    @Test
    public void testVisitAllSubpaths() {
        IntPath orig = IntPath.of(32, 31, 33, 32, 35, 34, 102);
        Set<IntPath> expected = setOf(
                IntPath.of(31, 33, 32, 35, 34, 102),
                IntPath.of(32, 31, 33, 32, 35, 34),
                IntPath.of(33, 32, 35, 34, 102),
                IntPath.of(31, 33, 32, 35, 34),
                IntPath.of(32, 31, 33, 32, 35),
                IntPath.of(32, 31, 33, 32),
                IntPath.of(33, 32, 35, 34),
                IntPath.of(32, 35, 34, 102),
                IntPath.of(31, 33, 32, 35),
                IntPath.of(32, 31, 33),
                IntPath.of(31, 33, 32),
                IntPath.of(33, 32, 35),
                IntPath.of(32, 35, 34),
                IntPath.of(35, 34, 102),
                IntPath.of(32, 31),
                IntPath.of(31, 33),
                IntPath.of(33, 32),
                IntPath.of(32, 35),
                IntPath.of(35, 34),
                IntPath.of(34, 102)
        );
        Set<IntPath> got = new HashSet<>();
        orig.visitAllSubPaths(2, got::add);
        assertEquals(expected, got, () -> {
            Set<IntPath> absent = new HashSet<>(expected);
            absent.removeAll(got);
            Set<IntPath> unexpected = new HashSet<>(got);
            unexpected.removeAll(expected);
            return "Sets do not match.  Missing: " + absent + " unexpected: " + unexpected;
        });
        Set<IntPath> expected3 = setOf(
                IntPath.of(31, 33, 32, 35, 34, 102),
                IntPath.of(32, 31, 33, 32, 35, 34),
                IntPath.of(33, 32, 35, 34, 102),
                IntPath.of(31, 33, 32, 35, 34),
                IntPath.of(32, 31, 33, 32, 35),
                IntPath.of(32, 31, 33, 32),
                IntPath.of(33, 32, 35, 34),
                IntPath.of(32, 35, 34, 102),
                IntPath.of(31, 33, 32, 35),
                IntPath.of(32, 31, 33),
                IntPath.of(31, 33, 32),
                IntPath.of(33, 32, 35),
                IntPath.of(32, 35, 34),
                IntPath.of(35, 34, 102)
        );

        Set<IntPath> got3 = new HashSet<>();
        orig.visitAllSubPaths(3, got3::add);
        assertEquals(expected3, got3, () -> {
            Set<IntPath> absent = new HashSet<>(expected3);
            absent.removeAll(got3);
            Set<IntPath> unexpected = new HashSet<>(got3);
            unexpected.removeAll(expected3);
            return "Sets do not match.  Missing: " + absent + " unexpected: " + unexpected;
        });
    }

    private Set<IntPath> setOf(IntPath... paths) {
        return new HashSet<>(Arrays.<IntPath>asList(paths));
    }

    @Test
    public void testParse() {
        IntPath a = IntPath.of(32, 31, 33, 32, 35, 34, 102);
        IntPath b = IntPath.parse(a.toString());
        assertEquals(a, b);
    }

    @Test
    public void testCreationMethodsAndGeneralFunctionality() {
        int[] vals = new int[10];
        List<Integer> ints = new ArrayList<>(vals.length);
        IntPath.Builder bldr = IntPath.builder();
        for (int i = 50; i < 60; i++) {
            vals[i - 50] = i;
            bldr.add(i);
            ints.add(i);
        }
        IntPath exp = IntPath.of(50, 51, 52, 53, 54, 55, 56, 57, 58, 59);
        assertEquals(vals.length, exp.size(), "Sanity check - size wrong");
        for (int i = 0; i < exp.size(); i++) {
            assertEquals(i + 50, exp.get(i), "Sanity check failed");
        }
        IntPath a = IntPath.of(vals);
        IntPath b = IntPath.of(ints);
        IntPath c = bldr.build();
        IntPath d = IntPath.builder(50).add(51, 52, 53, 54, 55, 56, 57, 58, 59).build();
        assertEquals(exp, a, "Created-from-array path mismatch");
        assertEquals(exp, b, "Created-from-collection path mismatch");
        assertEquals(exp, c, "Created-from-builder path mismatch");
        assertEquals(exp, d, "Created-from-builder-with-bulk-add mismatch");

        IntPath e = a.prepending(49);
        assertNotSame(e, a);
        assertEquals(vals.length + 1, e.size());
        for (int i = 0; i < 11; i++) {
            assertEquals(49 + i, e.get(i));
        }
        IntPath f = e.appending(60);
        for (int i = 0; i < 12; i++) {
            assertEquals(49 + i, f.get(i));
        }
        IntPath g = f.appending(IntPath.of(61, 62));
        assertEquals(14, g.size());
        for (int i = 0; i < 14; i++) {
            assertEquals(49 + i, g.get(i));
        }
        IntPath parents = g.copy();
        assertEquals(g, parents);
        IntPath kids = g.copy();
        assertEquals(g, parents);
        for (int i = 0; i < 14; i++) {
            int sz = parents.size();
            assertEquals(sz, kids.size());
            parents = parents.parentPath();
            kids = kids.childPath();
            assertEquals(sz - 1, kids.size());
            assertEquals(sz - 1, parents.size());
            if (i < 13) {
                int expChild = g.get(i + 1);
                assertEquals(expChild, kids.get(0));
                assertEquals(g.get(g.size() - 1), kids.get(kids.size() - 1));

                int expPar = g.get(g.size() - (2 + i));
                assertEquals(expPar, parents.get(parents.size() - 1));
                assertEquals(g.get(0), parents.get(0));
            }
        }

        for (int i = 0; i < g.size(); i++) {
            for (int j = g.size() - 1; j > i; j--) {
                IntPath sub = g.subPath(i, j);
                for (int k = 0; k < sub.size(); k++) {
                    assertEquals(g.get(i + k), sub.get(k));
                }
            }
        }
    }

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
        a.replaceFrom(1, replacement);
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
                assertTrue(a.contains(up), "Should contain " + up);
                up = up.parentPath();
            } while (!up.isEmpty());
            IntPath down = test.copy();
            do {
                assertTrue(a.contains(down), "Should contain " + down);
                down = down.childPath();
            } while (!down.isEmpty());
        }
    }

    @Test
    public void testContains() {
        IntPath pth = IntPath.of(0, 1, 2, 5, 6);
        assertContains(pth, IntPath.of(5, 6));
        assertContains(pth, IntPath.of(2, 5, 6));
        assertContains(pth, IntPath.of(1, 2, 5, 6));
        assertContains(pth, IntPath.of(0, 1, 2, 5, 6));
        assertContains(pth, IntPath.of(1, 2, 5, 6));
        assertContains(pth, IntPath.of(1, 2, 5));
        assertContains(pth, IntPath.of(1, 2, 5, 6));
        assertContains(pth, IntPath.of(0, 1, 2, 5));
        assertNotContains(pth, IntPath.of(2, 6));
        assertContains(pth, IntPath.of(0));
        assertContains(pth, IntPath.of(1));
        assertContains(pth, IntPath.of(2));
        assertContains(pth, IntPath.of(5));
        assertContains(pth, IntPath.of(6));
    }

    @Test
    public void testArrayEquals() {
        int[] ints = new int[]{0, 1, 2, 5, 6};
        assertTrue(IntPath.arraysEquals(ints, 3, 5, new int[]{5, 6}, 0, 2));
    }

    private void assertContains(IntPath container, IntPath contained) {
        assertTrue(container.contains(contained), container + " claims not to contain " + contained);
    }

    private void assertNotContains(IntPath container, IntPath contained) {
        assertFalse(container.contains(contained), container + " claims not to contain " + contained);
    }

    @Test
    public void testStartEnd() {
        IntPath a = new IntPath(true, new int[]{1, 2, 3, 4, 5});
        IntPath b = new IntPath(true, new int[]{1, 2, 3, 4});
        IntPath empty = IntPath.of();
        IntPath tail = IntPath.of(3, 4, 5);
        IntPath five = IntPath.of(5);
        IntPath four = IntPath.of(5);
        IntPath twoThree = IntPath.of(2, 3);
        IntPath twoFour = IntPath.of(2, 4);
        assertTrue(a.startsWith(b));
        assertFalse(b.startsWith(a));
        assertTrue(a.endsWith(tail));
        assertFalse(b.endsWith(tail));
        assertFalse(tail.endsWith(b));
        assertTrue(a.endsWith(five));
        assertFalse(b.endsWith(five));
        assertFalse(a.startsWith(five));
        assertFalse(b.startsWith(five));
        assertTrue(a.contains(b));
        assertFalse(b.contains(a));
        assertTrue(a.contains(twoThree));
        assertTrue(b.contains(twoThree));
        assertTrue(empty.isNotAPath());
        assertTrue(five.isNotAPath());
        assertTrue(four.isNotAPath());
        assertFalse(a.startsWith(empty));
        assertFalse(b.startsWith(empty));
        assertFalse(empty.startsWith(a));
        assertFalse(empty.startsWith(b));
    }

}
