/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.graph;

import com.mastfrog.abstractions.list.IndexedResolvable;
import static com.mastfrog.graph.BitSetGraphTest.EDGES_WITH_CYCLES;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TopoSortTest {

    static int[][] EDGES_1 = new int[][]{
        {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5},
        {1, 5},
        {2, 20}, {20, 5},
        {10, 11}, {11, 12}, {12, 13},
        {3, 6}, {6, 7}, {7, 8},
        {4, 30}, {10, 31}, {31, 30}
    };

    @Test
    public void test() {
        IntGraph graph = IntGraph.builder(5).addEdges(EDGES_1).build();
        ObjectGraph<String> og = new BitSetObjectGraph<>(graph, new IxRes());
        Set<String> s = new HashSet<>(Arrays.asList("0", "1", "2", "20", "5", "6", "8", "10", "31"));
        List<String> sorted = og.topologicalSort(s);

        for (int i = 1; i < sorted.size(); i++) {
            String curr = sorted.get(i);
            Set<String> rc = og.reverseClosureOf(sorted.get(i));
            Set<String> c = og.closureOf(curr);
            List<String> remaining = sorted.subList(i, sorted.size());
            Set<String> reverseClosureOverlap = new HashSet<>(rc);
            reverseClosureOverlap.retainAll(remaining);

            if (!reverseClosureOverlap.isEmpty()) {
                fail("Item " + curr + " is followed by some of its ancestors in the topological sort: " + reverseClosureOverlap);
            }
        }
    }

    @Test
    public void testPathologicalDoesNotLoopEndlessly() {
        IntGraph graph = IntGraph.builder(5).addEdges(EDGES_WITH_CYCLES).build();
        ObjectGraph<String> og = new BitSetObjectGraph<>(graph, new IxRes());
//        Set<String> s = new HashSet<>(Arrays.asList("0", "1", "2", "20", "5", "6", "8", "10", "31"));
        Set<String> s = new HashSet<>(Arrays.asList("0", "1", "2", "20", "5", "6", "10", "31"));
        List<String> sorted = og.topologicalSort(s);

        for (int i = 1; i < sorted.size(); i++) {
            String curr = sorted.get(i);
            Set<String> rc = og.reverseClosureOf(sorted.get(i));
            Set<String> c = og.closureOf(curr);
            List<String> remaining = sorted.subList(i, sorted.size());
            Set<String> reverseClosureOverlap = new HashSet<>(rc);
            reverseClosureOverlap.remove("6");
            reverseClosureOverlap.remove("10");
            reverseClosureOverlap.retainAll(remaining);

            if (!reverseClosureOverlap.isEmpty()) {
                fail("Item " + curr + " is followed by some of its ancestors in the topological sort: " + reverseClosureOverlap);
            }
        }
    }

    static class IxRes implements IndexedResolvable<String> {

        @Override
        public String forIndex(int i) {
            return Integer.toString(i);
        }

        @Override
        public int size() {
            return 32;
        }

        @Override
        public int indexOf(Object o) {
            return Integer.parseInt(o.toString());
        }

    }
}
