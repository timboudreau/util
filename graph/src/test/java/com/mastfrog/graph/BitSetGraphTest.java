package com.mastfrog.graph;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import com.mastfrog.bits.Bits;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Tim Boudreau
 */
public class BitSetGraphTest {

    static int[][] EDGES_1 = new int[][]{
        {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5},
        {1, 5},
        {2, 20}, {20, 5},
        {10, 11}, {11, 12}, {12, 13},
        {3, 6}, {6, 7}, {7, 8},
        {4, 30}, {10, 31}, {31, 30}
    };

    static int[][] EDGES_WITH_CYCLES = new int[][]{
        {0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5},
        {1, 5},
        {2, 20}, {20, 5},
        {10, 11}, {11, 12}, {12, 13}, {13, 10},
        {3, 6}, {6, 7}, {7, 8}, {8, 6},
        {4, 30}, {10, 31}, {31, 30}
    };

    PairSet pathoEdges;
    PairSet graphEdges = PairSet.fromIntArray(EDGES_1);
    PairSet cyclesEdges = PairSet.fromIntArray(EDGES_WITH_CYCLES);

    IntGraph graph;
    IntGraph cyclesGraph;
    IntGraph pathologicalGraph;

    @Test
    public void sanityCheckGraph() {
        PairSet ps = new PairSet(graph.size());
        for (int[] edge : EDGES_1) {
            assertTrue("Edge not present: " + edge[0] + "->" + edge[1], graph.containsEdge(edge[0], edge[1]));
            ps.add(edge[0], edge[1]);
        }
    }

    @Test
    public void testPaths() {
        List<IntPath> allPaths = graph.pathsBetween(0, 5);
        List<IntPath> expected = Arrays.asList(
                new IntPath().addAll(0, 5),
                new IntPath().addAll(0, 1, 5),
                new IntPath().addAll(0, 2, 20, 5)
        );
        assertEquals(expected, allPaths);
        Optional<IntPath> opt = graph.shortestPathBetween(0, 5);
        assertTrue(opt.isPresent());
        assertEquals(opt.get(), new IntPath().addAll(0, 5));
        opt = graph.shortestPathBetween(0, 14);
        assertFalse(opt.isPresent());

        for (int i = 0; i < EDGES_1.length; i++) {
            for (int j = 0; j < EDGES_1.length; j++) {
                int dist = graph.distance(i, j);
                Optional<IntPath> pth = graph.shortestPathBetween(i, j);
                if (!pth.isPresent()) {
                    pth = graph.shortestPathBetween(j, i);
                }
                if (dist == -1) {
                    assertFalse(pth.isPresent());
                } else {
                    assertTrue(pth.isPresent());
                    assertEquals(dist, pth.get().size());
                }
            }
        }
    }

    @Test
    public void testClosure() {
        for (int i = 0; i < EDGES_1.length; i++) {
            BitSet closure = computeClosureSlow(i, EDGES_1);
            if (closure.cardinality() > 0) {
                Bits graphClosure = graph.closureOf(i);
                assertEquals("Closures for " + i + " differ", closure, graphClosure.toBitSet());
            }
        }
    }

    @Test
    public void testReverseClosure() {
        for (int i = 0; i < EDGES_1.length; i++) {
            BitSet closure = computeReverseClosureSlow(i, EDGES_1);
            if (closure.cardinality() > 0) {
                Bits graphClosure = graph.reverseClosureOf(i);
                assertEquals("Closures for " + i + " differ", closure, graphClosure.toBitSet());
            }
        }
    }

    @Test
    public void testWalkTouchesAllEdges() {
        // Graph walking only visits each node once, so if a node is incorporated
        // into several paths, there will be unvisited pairs
        PairSet edges = graphEdges.copy();
        edges.remove(0, 5).remove(14, 31);
        assertAllEdgesWalked(graphEdges, graph, new PairSet(graphEdges.size()).add(0, 5).add(14, 31));

        edges = cyclesEdges.copy();
        assertAllEdgesWalked(edges, cyclesGraph, new PairSet(cyclesEdges.size())
                .add(0, 5).add(1, 6).add(8, 6).add(10, 11).add(12, 31)
        );
    }

    @Test
    public void testPairSetConversion() {
        PairSet set = graph.toPairSet();
        IntGraph revived = set.toGraph();
        assertEquals(graph, revived);
    }

    private void assertAllEdgesWalked(PairSet expectedPairs, IntGraph graph, PairSet expectedUnvisited) {
        PairSet testPairs = expectedPairs.copy();
        PairSet foundPairs = new PairSet(graph.size());
        graph.walk(new IntGraphVisitor() {
            LinkedList<Integer> last = new LinkedList<>();

            {
                last.push(-1);
            }

            @Override
            public void enterNode(int edge, int depth) {
                int parent = last.peek();
                if (parent != -1) {
                    foundPairs.add(parent, edge);
                    testPairs.remove(parent, edge);
                }
                last.push(edge);
            }

            @Override
            public void exitNode(int ruleId, int depth) {
                last.pop();
            }
        });
        assertEquals("Visited" + foundPairs + " but should have visited " + expectedPairs,
                expectedUnvisited, testPairs);
    }

    private BitSet computeClosureSlow(int of, int[][] edges) {
        BitSet set = new BitSet(edges.length);
        for (int[] pair : edges) {
            if (pair[0] == of) {
                set.set(pair[1]);
            }
        }
        int cardinality;
        do {
            cardinality = set.cardinality();
            for (int[] pair : edges) {
                if (set.get(pair[0])) {
                    set.set(pair[1]);
                }
            }
        } while (cardinality != set.cardinality());
        return set;
    }

    private BitSet computeReverseClosureSlow(int of, int[][] edges) {
        BitSet set = new BitSet(edges.length);
        for (int[] pair : edges) {
            if (pair[1] == of) {
                set.set(pair[0]);
            }
        }
        int cardinality;
        do {
            cardinality = set.cardinality();
            for (int[] pair : edges) {
                if (set.get(pair[1])) {
                    set.set(pair[0]);
                }
            }
        } while (cardinality != set.cardinality());
        return set;
    }

    @Test
    public void testInvert() {
        BitSet set = new BitSet();
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 1) {
                set.set(i);
            }
        }
        BitSet inverted = BitSetUtils.invert(set);
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 1) {
                assertFalse("Should not be set: " + i, inverted.get(i));
            } else {
                assertTrue("Should be set: " + i, inverted.get(i));
            }
        }
        for (int i = 100; i < 140; i++) {
            assertFalse("Bits after end should not be set but " + i + " is, in " + inverted, inverted.get(i));
        }

    }

    @Before
    public void buildGraph() {
        graph = IntGraph.builder(5).addEdges(EDGES_1).build();
        cyclesGraph = IntGraph.builder(5).addEdges(EDGES_WITH_CYCLES).build();
        IntGraphBuilder bldr = IntGraph.builder();
        pathoEdges = new PairSet(20);
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                bldr.addEdge(i, j);
                pathoEdges.add(i, j);
            }
        }
        pathologicalGraph = bldr.build();
    }

    private IntGraph pyramid() {
        return IntGraph.builder(5).addEdge(0, 1).addEdge(0, 2)
                .addEdge(1, 3).addEdge(1, 4).addEdge(2, 5).addEdge(2, 6)
                .addEdge(3, 7).addEdge(3, 8).addEdge(4, 9).addEdge(4, 10).addEdge(5, 11).addEdge(5, 12).addEdge(6, 13).addEdge(6, 14)
                .build();
    }

    private IntGraph circle(int size) {
        IntGraphBuilder bldr = IntGraph.builder();
        for (int i = 1; i < size; i++) {
            bldr.addEdge(i - 1, i);
        }
        bldr.addEdge(size - 1, 0);
        return bldr.build();
    }

    private IntGraph pathologicalSingle() {
        return IntGraph.builder().addEdge(0, 0).build();
    }

    private IntGraph pathologicalOrphans(int size) {
        IntGraphBuilder bldr = IntGraph.builder();
        for (int i = 0; i < size; i++) {
            bldr.addOrphan(i);
        }
        return bldr.build();
    }

    @Test
    public void testOrphans() {
        IntGraph orphs = pathologicalOrphans(5);
        assertEquals(5, orphs.orphans().cardinality());
        assertEquals(0, orphs.connectors().cardinality());
    }

    @Test
    public void testPathological1() {
        IntGraph sing = pathologicalSingle();
        assertEquals(1, sing.closureOf(0).cardinality());
        assertEquals(0, sing.orphans().cardinality());
    }

    @Test
    public void testReachability() {
        IntGraph graph = pyramid();
        for (int i = 1; i < 15; i++) {
            assertTrue(graph.isReachableFrom(0, i));
        }
        assertFalse(graph.isReachableFrom(1, 12));
        for (int i = 0; i < 15; i++) {
            Bits closure = graph.closureOf(i);
            int index = i;
            closure.forEachSetBitAscending(node -> {
                assertTrue(graph.isReachableFrom(index, node));
            });
        }

        IntGraph circ = circle(5);
        BitSet all = new BitSet();
        for (int i = 0; i < 5; i++) {
            all.set(i);
        }
        for (int i = 0; i < circ.size(); i++) {
            for (int j = 0; j < circ.size(); j++) {
                assertTrue(circ.isReachableFrom(i, j));
            }
            Bits clos = circ.closureOf(i);
            assertEquals(all, clos.toBitSet());
            Bits rclos = circ.reverseClosureOf(i);
            assertEquals(all, rclos.toBitSet());
        }
        assertTrue(circ.topLevelOrOrphanNodes().isEmpty());
    }

    @Test
    public void testPathologicalWalk() {
        IntGraph graph = circle(5);
        BitSet visited = new BitSet(5);
        graph.walk(0, (node, depth) -> {
            visited.set(node);
        });
        assertFalse(visited.isEmpty());
        assertEquals(5, visited.cardinality());
    }

    @Test
    public void testSearch() {
        IntGraph graph = pyramid();

        BitSet traversed = new BitSet();
        graph.breadthFirstSearch(0, false, node -> {
            traversed.set(node);
        });
        for (int i = 1; i <= 14; i++) {
            assertTrue(traversed.get(i));
        }

        traversed.clear();
        graph.depthFirstSearch(0, false, node -> {
            traversed.set(node);
        });
        for (int i = 1; i <= 14; i++) {
            assertTrue(traversed.get(i));
        }

        traversed.clear();
        graph.breadthFirstSearch(3, false, node -> {
            traversed.set(node);
        });
        assertEquals(2, traversed.cardinality());
        assertTrue(traversed.get(7));
        assertTrue(traversed.get(8));
        traversed.clear();
        graph.breadthFirstSearch(3, true, node -> {
            traversed.set(node);
        });
        assertEquals(2, traversed.cardinality());
        assertTrue(traversed.get(0));
        assertTrue(traversed.get(1));

        Bits clo = graph.closureOf(3);
        assertEquals(2, clo.cardinality());
        assertTrue(clo.get(7));
        assertTrue(clo.get(8));

        Bits rclo = graph.reverseClosureOf(3);
        assertEquals(2, rclo.cardinality());
        assertTrue(rclo.get(0));
        assertTrue(rclo.get(1));

        Bits conn = graph.connectors();
        assertEquals(6, conn.cardinality());
        for (int i = 1; i <= 6; i++) {
            assertTrue(conn.get(i));
        }

        assertTrue(graph.orphans().isEmpty());
    }
}
