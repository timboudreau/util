/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.tree;

import java.util.Arrays;
import java.util.BitSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class BitSetGraphImplTest {

    private BitSetGraph graph;

    @Test
    public void sanityCheck() {
        BitSet a = s(1, 2, 3);
        BitSet b = s(1, 2, 3);
        assertTrue(a.get(1));
        assertTrue(a.get(2));
        assertTrue(a.get(3));
        assertFalse(a.get(4));
        assertFalse(a.get(0));
        assertEquals(a, b);
    }

    private void assertSet(BitSet set, int... vals) {
        BitSet nue = s(vals);
        assertEquals(nue, set);
    }

    private BitSet s(int... vals) {
        BitSet result = new BitSet();
        for (int v : vals) {
            result.set(v);
        }
        return result;
    }

    @Test
    public void testSomeMethod() {
        assertSet(graph.topLevelOrOrphanNodes(), 0, 4);
        assertSet(graph.closureOf(0), 1, 5, 3, 2, 7, 8);
        assertEquals(6, graph.closureSize(0));
        assertSet(graph.closureOf(4), 10, 6);
        assertSet(graph.reverseClosureOf(3), 5, 1, 0);
        assertSet(graph.parents(7), 2);
        assertSet(graph.children(2), 7, 8);
        assertSet(graph.reachableFrom(5), 1, 3);
        assertSet(graph.bottomLevelNodes(), 3, 6, 7, 8);
        assertSet(graph.closureOf(2), 7, 8);
        assertSet(graph.closureIntersection(0, 2), 7, 8);
        assertSet(graph.closureUnion(0, 4), 1, 5, 3, 2, 7, 8, 10, 6);
    }

    @Before
    public void createGraph() {
        BitSetGraphBuilder<Integer> b = new BitSetGraphBuilder<>(
                Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), Integer.class);

        b.enterItem(0, () -> {
            b.enterItem(1, () -> {
                b.enterItem(5, () -> {
                    b.enterItem(3, () -> {
                    });
                });
            });
            b.enterItem(2, () -> {
                b.enterItem(7, () -> {

                });
                b.enterItem(8, () -> {
                });
            });
        });
        b.enterItem(4, () -> {
            b.enterItem(10, () -> {
                b.enterItem(6, () -> {
                });
            });
        });
        graph = b.build();
        assertNotNull(graph);
        assertEquals(11, graph.size());
        System.out.println(graph);
    }
}
