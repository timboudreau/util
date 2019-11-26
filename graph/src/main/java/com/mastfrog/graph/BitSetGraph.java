package com.mastfrog.graph;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import com.mastfrog.bits.Bits;
import com.mastfrog.bits.MutableBits;
import com.mastfrog.graph.algorithm.Algorithm;
import com.mastfrog.graph.algorithm.EigenvectorCentrality;
import com.mastfrog.graph.algorithm.PageRank;
import com.mastfrog.function.IntBiConsumer;
import java.util.function.BiConsumer;

/**
 * A highly compact graph based on bit sets.
 *
 * @author Tim Boudreau
 */
final class BitSetGraph implements IntGraph {

    private final Bits[] outboundEdges;
    private final Bits[] inboundEdges;
    private final Bits topLevel;
    private final Bits bottomLevel;

    BitSetGraph(BitSet[] outboundEdges, BitSet[] inboundEdges) {
        this(toBits(outboundEdges), toBits(inboundEdges));
    }

    BitSetGraph(BitSet[] edges) {
        this(toBits(edges));
    }

    BitSetGraph(Bits[] outboundEdges, Bits[] inboundEdges) {
        assert sanityCheck(outboundEdges, inboundEdges);
        this.outboundEdges = outboundEdges;
        this.inboundEdges = inboundEdges;
        Bits outboundKeys = keySet(outboundEdges);
        Bits inboundKeys = keySet(inboundEdges);
        MutableBits top = MutableBits.create(outboundEdges.length);
        MutableBits bottom = MutableBits.create(inboundEdges.length);
        top.or(outboundKeys);
        bottom.or(inboundKeys);
        top.andNot(inboundKeys);
        bottom.andNot(outboundKeys);
        topLevel = top.readOnlyView();
        bottomLevel = bottom.readOnlyView();
        checkConsistency();
    }

    private static Bits[] toBits(BitSet[] sets) {
        Bits[] bits = new Bits[sets.length];
        for (int i = 0; i < sets.length; i++) {
            bits[i] = Bits.fromBitSet(sets[i]);
        }
        return bits;
    }

    BitSetGraph(Bits[] references) {
        this(references, inverseOf(references));
    }

    @Override
    public boolean containsEdge(int a, int b) {
        if (a > outboundEdges.length || b > outboundEdges.length || a < 0 || b < 0) {
            return false;
        }
        boolean result = outboundEdges[a].get(b);
        assert !result || inboundEdges[b].get(a) : "State inconsistent for " + a + "," + b;
        return result;
    }

    void checkConsistency() {
        boolean asserts = false;
        assert asserts = true;
        assert outboundEdges.length == inboundEdges.length : "Array sizes differ";
        if (asserts) {
            for (int i = 0; i < outboundEdges.length; i++) {
                Bits set = outboundEdges[i];
                for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
                    Bits opposite = inboundEdges[bit];
                    assert opposite.get(i);
                }
            }
        }
    }

    /**
     * Save a serialized graph.
     *
     * @param out The output
     * @throws IOException If something goes wrong
     */
    public void save(ObjectOutput out) throws IOException {
        out.writeInt(1); // version
        out.writeInt(outboundEdges.length);
        for (Bits outboundEdge : outboundEdges) {
            if (outboundEdge.cardinality() > 0) {
                out.writeObject(outboundEdge.toByteArray());
            } else {
                out.writeObject(null);
            }
        }
        out.flush();
    }

    /**
     * Load a serialized graph.
     *
     * @param in The input
     * @return A graph
     * @throws IOException If something goes wrong
     * @throws ClassNotFoundException Should not happen
     */
    public static BitSetGraph load(ObjectInput in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();
        if (ver != 1) {
            throw new IOException("Unsupoorted version " + ver);
        }
        int count = in.readInt();
        Bits[] sets = new MutableBits[count];
        for (int i = 0; i < sets.length; i++) {
            byte[] vals = (byte[]) in.readObject();
            if (vals == null) {
                sets[i] = Bits.EMPTY;
            } else {
                sets[i] = MutableBits.valueOf(vals);
            }
        }
        return new BitSetGraph(sets);
    }

    private static MutableBits[] inverseOf(Bits[] outbound) {
        // Given a single set of inbound bits, create the inverse
        // set
        int size = outbound.length;
        MutableBits empty = null;
        MutableBits[] inbound = new MutableBits[size];
        for (int i = 0; i < size; i++) {
            if (outbound[i] == null) {
                if (empty == null) {
                    empty = MutableBits.create(0);
                }
                outbound[i] = empty;
            }
            for (int j = 0; j < size; j++) {
                Bits b = outbound[j];
                if (b != null) {
                    if (b.get(i)) {
                        if (inbound[i] == null) {
                            inbound[i] = MutableBits.create(size);
                        }
                        inbound[i].set(j);
                    }
                }
            }
            if (inbound[i] == null) {
                if (empty == null) {
                    empty = MutableBits.create(0);
                }
                inbound[i] = empty;
            }
        }
        return inbound;
    }

    private boolean sanityCheck(Bits[] outbound, Bits[] inbound) {
        // Ensure that the outbound and inbound edge sets are
        // mirror images of each other
        boolean asserts = false;
        assert asserts = true;
        if (!asserts) {
            return true;
        }
        assert outbound.length == inbound.length : "MutableBits array lengths do not match: "
                + outbound.length + " and " + inbound.length;
        for (int i = 0; i < outbound.length; i++) {
            int ix = i;
            outbound[i].forEachSetBitAscending(bit -> {
                Bits reverse = inbound[bit];
                assert reverse.get(ix) : "ruleReferences[" + ix + "] says it "
                        + "references " + bit + " but referencedBy[" + bit + "]"
                        + " does not have " + ix + " set - ruleReferences: " + bitSetArrayToString(outbound)
                        + " referencedBy: " + bitSetArrayToString(inbound);
            });
        }
        return true;
    }

    private String bitSetArrayToString(Bits[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            Bits b = arr[i];
            if (b.isEmpty()) {
                sb.append(i).append(": empty\n");
            } else {
                sb.append(i).append(": ").append(b).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("BitSetTree{size=")
                .append(outboundEdges.length)
                .append(", totalCardinality=").append(totalCardinality())
                .append("}\n");

        walk((id, d) -> {
            char[] c = new char[d * 2];
            Arrays.fill(c, ' ');
            sb.append(c).append(id).append('\n');
        });
        return sb.toString();
    }

    /**
     * Walk the closure of the graph from top level nodes.
     *
     * @param v A visitor
     */
    @Override
    public void walk(IntGraphVisitor v) {
        BitSet traversed = new BitSet(size());
        walk(v, topLevel, traversed, 0);
        // The top level nodes computation will miss isolated paths with
        // cycles, so ensure we've covered everything
        for (int bit = traversed.nextClearBit(0); bit >= 0 && bit < size(); bit = traversed.nextClearBit(bit + 1)) {
            walk(v, outboundEdges[bit], traversed, 0);
        }
    }

    /**
     * Walk the closure of a node.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    @Override
    public void walk(int startingWith, IntGraphVisitor v) {
        MutableBits set = MutableBits.create(size());
        set.set(startingWith);
        walk(v, set, new BitSet(size()), 0);
    }

    private void walk(IntGraphVisitor v, Bits traverse, BitSet seen, int depth) {
        traverse.forEachSetBitAscending(bit -> {
            if (!seen.get(bit)) {
                seen.set(bit);
                v.enterNode(bit, depth);
                walk(v, outboundEdges[bit], seen, depth + 1);
                v.exitNode(bit, depth);
            }
        });
    }

    /**
     * Walk the inverse closure of the bottom-most nodes in the graph. Note that
     * this will not traverse subgraphs which are entirely cyclical.
     *
     * @param v A visitor
     */
    @Override
    public void walkUpwards(IntGraphVisitor v) {
        int size = size();
        BitSet traversed = new BitSet(size);
        walkUpwards(v, bottomLevel, traversed, 0);
        for (int bit = traversed.previousClearBit(size - 1); bit >= 0 && bit < size; bit = traversed.previousClearBit(bit - 1)) {
            if (bit >= size) {
                break;
            }
            walk(v, inboundEdges[bit], traversed, 0);
        }
    }

    /**
     * Walk the inverse closure of one node, visiting each ancestor exactly
     * once.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    @Override
    public void walkUpwards(int startingWith, IntGraphVisitor v) {
        MutableBits set = MutableBits.create(size());
        set.set(startingWith);
        walkUpwards(v, set, new BitSet(size()), 0);
    }

    private void walkUpwards(IntGraphVisitor v, Bits traverse, BitSet seen, int depth) {
        traverse.forEachSetBitAscending(bit -> {
            if (!seen.get(bit)) {
                seen.set(bit);
                v.enterNode(bit, depth);
                walkUpwards(v, inboundEdges[bit], seen, depth + 1);
            }
        });
    }

    /**
     * Visit all edges in the graph.
     *
     * @param bi A consumer
     */
    @Override
    public void edges(IntBiConsumer bi) {
        for (int i = 0; i < size(); i++) {
            int index = i;
            outboundEdges[i].forEachSetBitAscending(bit -> {
                bi.accept(index, bit);
            });
        }
    }

    /**
     * Get all edges in the graph.
     *
     * @return A set of pairs of edges
     */
    @Override
    public PairSet allEdges() {
        PairSet set = new PairSet(size());
        edges(set::add);
        return set;
    }

    /**
     * Get the number of nodes in the graph.
     *
     * @return A node count
     */
    public int size() {
        return outboundEdges.length;
    }

    /**
     * Get the disjunction of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set of nodes
     */
    @Override
    public Bits closureDisjunction(int a, int b) {
        if (a == b) {
            return MutableBits.create(0);
        }
        MutableBits ca = _closureOf(a);
        MutableBits cb = _closureOf(b);
        ca.xor(cb);
        return ca;
    }

    /**
     * Get the union of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set of nodes
     */
    @Override
    public Bits closureUnion(int a, int b) {
        if (a == b) {
            return MutableBits.create(0);
        }
        MutableBits ca = _closureOf(a);
        MutableBits cb = _closureOf(b);
        ca.or(cb);
        return ca;
    }

    /**
     * Get the minimum distance between two nodes.
     *
     * @param a One node
     * @param b Another node
     * @return A distance
     */
    @Override
    public int distance(int a, int b) {
        Optional<IntPath> path = shortestPathBetween(a, b);
        if (path.isPresent()) {
            return path.get().size();
        } else {
            path = shortestPathBetween(b, a);
            if (path.isPresent()) {
                return path.get().size();
            }
            return -1;
        }
    }

    /**
     * Get the set of those nodes which exist in the closure of only one of the
     * passed list of nodes.
     *
     * @param nodes An array of nodes
     * @return A set
     */
    @Override
    public Bits closureDisjunction(int... nodes) {
        MutableBits result = MutableBits.create(size());
        for (int i = 0; i < nodes.length; i++) {
            MutableBits clos = _closureOf(nodes[i]);
            if (i == 0) {
                result.or(clos);
            } else {
                result.xor(clos);
            }
        }
        return result;
    }

    /**
     * Get the set of those nodes which exist in the closure of only one of the
     * passed list of nodes.
     *
     * @param nodes A set of nodes
     * @return A set
     */
    @Override
    public Bits closureDisjunction(Bits nodes) {
        MutableBits result = MutableBits.create(size());
        for (int bit = nodes.nextSetBit(0), count = 0; bit >= 0; bit = nodes.nextSetBit(bit + 1), count++) {
            if (bit >= size()) {
                break;
            }
            MutableBits clos = _closureOf(bit);
            if (count == 0) {
                result.or(clos);
            } else {
                result.xor(clos);
            }
        }
        return result;
    }

    /**
     * Compute the eigenvector centrality of each node in the graph - an
     * importance measure that could loosely be phrased as <i>most likely to be
     * connected <b>through</b></i> - meaning, unlike page rank, this emphasizes
     * &quot;connector&quot; nodes rather than most-linked nodes - those nodes
     * which, if removed, would break the most paths in the graph.
     *
     * @param maxIterations The maxmum number of iterations to use refining the
     * answer before assuming an optimal answer has been determined
     * @param minDiff The minimum difference in values between iterations which,
     * when reached, indicates that an optimal answer has been determined even
     * if the algorithm has refined the answer for less than
     * <code>maxIterations</code>
     * @param inEdges If true, use inbound rather than outbound edges for the
     * calculation
     * @param ignoreSelfEdges If true, do not count a node's direct cycles to
     * themselves in the score
     * @param l2norm If set, attempt to normalize the result so scores will be
     * comparable across different graphs
     * @return An array of doubles the same size as the number of nodes in the
     * graph, where the value for each node (the array index) is the score
     */
    @Override
    public double[] eigenvectorCentrality(int maxIterations, double minDiff,
            boolean inEdges, boolean ignoreSelfEdges, boolean l2norm) {
        return Algorithm.eigenvectorCentrality().setParameter(EigenvectorCentrality.MAXIMUM_ITERATIONS,
                maxIterations).setParameter(EigenvectorCentrality.MINIMUM_DIFFERENCE, minDiff)
                .setParameter(EigenvectorCentrality.USE_IN_EDGES, inEdges)
                .setParameter(EigenvectorCentrality.IGNORE_SELF_EDGES, ignoreSelfEdges)
                .setParameter(EigenvectorCentrality.NORMALIZE, l2norm).apply(this);
    }

    public double sum(IntToDoubleFunction func) {
        double result = 0.0;
        for (int i = 0; i < size(); i++) {
            result += func.applyAsDouble(i);
        }
        return result;
    }

    /**
     * Rank the nodes in this graph according to the page rank algorithm, which
     * detects most-linked-to nodes in the graph.
     *
     * @param minDifference The minimum difference after an iteration, at which
     * point the algorithm should bail out and return the answer
     * @param dampingFactor The damping factor
     * @param maximumIterations The maximum number of iterations before the
     * algorithm should assume it has computed an optimal answer and bail out
     * @param normalize If true, normalize the results so they are comparable
     * across calls to this method on different graphs
     * @return An array of doubles, where the index is the node id and the value
     * is the score
     */
    @Override
    public double[] pageRank(double minDifference, double dampingFactor,
            int maximumIterations, boolean normalize) {
        return Algorithm.pageRank().setParameter(PageRank.MINIMUM_DIFFERENCE, minDifference)
                .setParameter(PageRank.DAMPING_FACTOR, dampingFactor)
                .setParameter(PageRank.MAXIMUM_ITERATIONS, maximumIterations)
                .setParameter(PageRank.NORMALIZE, normalize).apply(this);
    }

    @Override
    public boolean isReachableFrom(int a, int b) {
        return closureOf(a).get(b);
    }

    /**
     * Determine if the closure of a includes b.
     *
     * @param a A node
     * @param b Another node
     * @return If b is a descendant of a
     */
    @Override
    public boolean isReverseReachableFrom(int a, int b) {
        return closureOf(b).get(a);
    }

    /**
     * Get the set of parent and child nodes for a node.
     *
     * @param startingNode A node
     * @return A set of nodes
     */
    @Override
    public Bits neighbors(int startingNode) {
        MutableBits result = inboundEdges[startingNode].mutableCopy();
        result.or(outboundEdges[startingNode]);
        return result;
    }

    @Override
    public void depthFirstSearch(int startingNode, boolean up, IntConsumer cons) {
        depthFirstSearch(startingNode, up, cons, MutableBits.create(size()));
    }

    @Override
    public void breadthFirstSearch(int startingNode, boolean up, IntConsumer cons) {
        breadthFirstSearch(startingNode, up, cons, MutableBits.create(size()));
    }

    private void breadthFirstSearch(int startingNode, boolean up, IntConsumer cons, MutableBits traversed) {
        Bits dests = up ? inboundEdges[startingNode] : outboundEdges[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                cons.accept(bit);
                any = true;
            }
        }
        if (!any) {
            return;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                breadthFirstSearch(bit, up, cons, traversed);
                traversed.set(bit);
            }
        }
    }

    private void depthFirstSearch(int startingNode, boolean up, IntConsumer cons, MutableBits traversed) {
        Bits dests = up ? inboundEdges[startingNode] : outboundEdges[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                depthFirstSearch(bit, up, cons, traversed);
                any = true;
            }
        }
        if (!any) {
            return;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                traversed.set(bit);
                cons.accept(bit);
            }
        }
    }

    /**
     * Do a depth-first search which takes a predicate that will abort the
     * search the first time the predicate returns false.
     *
     * @param startingNode The starting node
     * @param up Wheather to search antecedent nodes or successor nodes
     * @param cons A predicate
     * @return True if the predicate returned false at some point (i.e. the
     * thing looked for was found in the graph and no further searching was
     * needed, such as reachability tests).
     */
    @Override
    public boolean abortableDepthFirstSearch(int startingNode, boolean up, IntPredicate cons) {
        return abortableDepthFirstSearch(startingNode, up, cons, MutableBits.create(size()));
    }

    /**
     * Do a breadth-first search which takes a predicate that will abort the
     * search the first time the predicate returns false.
     *
     * @param startingNode The starting node
     * @param up Wheather to search antecedent nodes or successor nodes
     * @param cons A predicate
     * @return True if the predicate returned false at some point (i.e. the
     * thing looked for was found in the graph and no further searching was
     * needed, such as reachability tests).
     */
    @Override
    public boolean abortableBreadthFirstSearch(int startingNode, boolean up, IntPredicate cons) {
        return abortableBreadthFirstSearch(startingNode, up, cons, MutableBits.create(size()));
    }

    private boolean abortableBreadthFirstSearch(int startingNode, boolean up, IntPredicate cons, MutableBits traversed) {
        Bits dests = up ? inboundEdges[startingNode] : outboundEdges[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                if (!cons.test(bit)) {
                    return true;
                }
                any = true;
            }
        }
        if (!any) {
            return false;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                boolean res = abortableBreadthFirstSearch(bit, up, cons, traversed);
                if (res) {
                    return true;
                }
                traversed.set(bit);
            }
        }
        return false;
    }

    private boolean abortableDepthFirstSearch(int startingNode, boolean up, IntPredicate cons, MutableBits traversed) {
        Bits dests = up ? inboundEdges[startingNode] : outboundEdges[startingNode];
        boolean any = false;
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                boolean res = abortableDepthFirstSearch(bit, up, cons, traversed);
                if (res) {
                    return true;
                }
                any = true;
            }
        }
        if (!any) {
            return false;
        }
        for (int bit = dests.nextSetBit(0); bit >= 0; bit = dests.nextSetBit(bit + 1)) {
            if (!traversed.get(bit)) {
                traversed.set(bit);
                boolean res = cons.test(bit);
                if (res) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Bits connectors() {
        int sz = size();
        MutableBits result = MutableBits.create(sz);
        for (int i = 0; i < sz; i++) {
            if (!inboundEdges[i].isEmpty() && !outboundEdges[i].isEmpty()) {
                result.set(i);
            }
        }
        return result;
    }

    @Override
    public Bits orphans() {
        int sz = size();
        MutableBits result = MutableBits.create(sz);
        for (int i = 0; i < sz; i++) {
            if (inboundEdges[i].isEmpty() && outboundEdges[i].isEmpty()) {
                result.set(i);
            }
        }
        return result;
    }

    /**
     * Get the set of nodes in the graph whose closure is not shared by any
     * other nodes.
     *
     * @return A set of nodes
     */
    @Override
    public Bits disjointNodes() {
        MutableBits[] unions = new MutableBits[size()];
        for (int i = 0; i < unions.length; i++) {
            unions[i] = MutableBits.create(unions.length);
        }
        MutableBits result = MutableBits.create(size());
        for (int i = 0; i < unions.length; i++) {
            unions[i] = _closureOf(i);
            unions[i].clear(i);
            for (int j = 0; j < unions.length; j++) {
                if (i != j) {
                    unions[i].andNot(unions[j]);
                }
                if (unions[i].cardinality() == 0) {
                }
            }
            if (!unions[i].isEmpty()) {
                result.set(i);
            }
        }
        return result;
    }

    /**
     * Determine if the passed node or a descendant of it has a cycle back to
     * itself.
     *
     * @param node A node
     * @return True if any outbound path from this node directly or indirectly
     * recurses back to it
     */
    @Override
    public boolean isRecursive(int node) {
        return closureOf(node).get(node);
    }

    /**
     * Determine if a node is <i>indirectly recursive</i> - if the closure of it
     * contains a cycle back to it, not counting cycles to itself.
     *
     * @param node A node
     * @return Whether or not it is indirectly recursive
     */
    @Override
    public boolean isIndirectlyRecursive(int node) {
        MutableBits test = MutableBits.create(size());
        test.or(outboundEdges[node]);
        test.clear(node);
        closureOf(node, test, 0);
        return test.get(node);
    }

    /**
     * Return an array of all nodes in the graph, sorted by the size of their
     * closure (the number of distinct descendant nodes they have).
     *
     * @return A count of nodes
     */
    @Override
    public int[] byClosureSize() {
        Integer[] result = new Integer[outboundEdges.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        int[] cache = new int[result.length];
        Arrays.fill(cache, -1);
        Arrays.sort(result, (a, b) -> {
            int sizeA = cache[a] == -1 ? cache[a] = closureSize(a) : cache[a];
            int sizeB = cache[b] == -1 ? cache[b] = closureSize(b) : cache[b];
            return sizeA == sizeB ? 0 : sizeA > sizeB ? 1 : -1;
        });
        int[] res = new int[result.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = result[i];
        }
        return res;
    }

    /**
     * Return an array of all nodes in the graph, sorted by the size of their
     * reverse closure (the number of distinct ancestor nodes they have).
     *
     * @return A count of nodes
     */
    @Override
    public int[] byReverseClosureSize() {
        Integer[] result = new Integer[outboundEdges.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        int[] cache = new int[outboundEdges.length];
        Arrays.fill(cache, -1);
        Arrays.sort(result, (a, b) -> {
            int sizeA = cache[a] == -1 ? cache[a] = reverseClosureSize(a) : cache[a];
            int sizeB = cache[b] == -1 ? cache[b] = reverseClosureSize(b) : cache[b];
            return sizeA == sizeB ? 0 : sizeA > sizeB ? 1 : -1;
        });
        int[] res = new int[result.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = result[i];
        }
        return res;
    }

    public List<int[]> edges() {
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < outboundEdges.length; i++) {
            Bits refs = outboundEdges[i];
            for (int bit = refs.nextSetBit(0); bit >= 0; bit = refs.nextSetBit(bit + 1)) {
                result.add(new int[]{i, bit});
            }
        }
        return result;
    }

    /**
     * Determine if a direct outbound edge exits from one node to another.
     *
     * @param from The source node
     * @param to The destination node
     * @return True if the edge exists
     */
    @Override
    public boolean hasOutboundEdge(int from, int to) {
        return outboundEdges[from].get(to);
    }

    /**
     * Determine if a direct inbound edge exits from one node to another.
     *
     * @param from The source node
     * @param to The destination node
     * @return True if the edge exists
     */
    @Override
    public boolean hasInboundEdge(int from, int to) {
        return inboundEdges[from].get(to);
    }

    /**
     * Returns the number of inbound edges a node has.
     *
     * @param node A node
     * @return The edge count
     */
    @Override
    public int inboundReferenceCount(int node) {
        return inboundEdges[node].cardinality();
    }

    /**
     * Returns the number of outbound edges a node has.
     *
     * @param node A node
     * @return The edge count
     */
    @Override
    public int outboundReferenceCount(int node) {
        return outboundEdges[node].cardinality();
    }

    /**
     * Get the set of nodes which have inbound edges from the passed node.
     *
     * @param node A node
     * @return A set
     */
    @Override
    public Bits children(int node) {
        return outboundEdges[node];
    }

    /**
     * Get the set of nodes which reference the passed node.
     *
     * @param node A node
     * @return Nodes which have an outbound edge to the passed one
     */
    @Override
    public Bits parents(int node) {
        return inboundEdges[node];
    }

    private static Bits keySet(Bits[] bits) {
        BitSet nue = new BitSet(bits.length);
        for (int i = 0; i < bits.length; i++) {
            if (bits[i].cardinality() > 0) {
                nue.set(i);
            }
        }
        return Bits.fromBitSet(nue);
    }

    /**
     * Get the set of nodes which have no inbound edges - no ancestors.
     *
     * @return A set
     */
    @Override
    public Bits topLevelOrOrphanNodes() {
        return topLevel;
    }

    /**
     * Get the set of nodes which have no outbound edges.
     *
     * @return The set of nodes which have no outbound edges
     */
    @Override
    public Bits bottomLevelNodes() {
        return bottomLevel;
    }

    /**
     * Determine if a node has no inbound edges - no ancestors.
     *
     * @param node The node
     * @return true if the passed node is not referenced by any other nodes in
     * this graph
     */
    @Override
    public boolean isUnreferenced(int node) {
        return inboundEdges[node].isEmpty();
    }

    /**
     * Get the count of nodes which have the passed node as an ancestor.
     *
     * @param node A node
     * @return The number of nodes which have the passed node as an ancestor
     */
    @Override
    public int closureSize(int node) {
        return closureOf(node).cardinality();
    }

    /**
     * Get the count of nodes which have the passed node as a descendant.
     *
     * @param node A node
     * @return The number of nodes which have the passed node as a descendant
     */
    @Override
    public int reverseClosureSize(int node) {
        return reverseClosureOf(node).cardinality();
    }

    /**
     * Get the closure of this node - the set of all nodes which have this node
     * as an ancestor.
     *
     * @param node A node
     * @return A set
     */
    @Override
    public Bits closureOf(int node) {
        return _closureOf(node);
    }

    private MutableBits _closureOf(int node) {
        MutableBits result = MutableBits.create(size());
        closureOf(node, result, 0);
        return result;
    }

    /**
     * Get the shortest path between two nodes in the graph. If multiple paths
     * of the shortest length exist, one will be returned, but which is
     * unspecified.
     *
     * @param src The source node
     * @param target The target node
     * @return An optional which, if non-empty, contains a path for which no
     * shorter path between the same two nodes exists
     */
    @Override
    public Optional<IntPath> shortestPathBetween(int src, int target) {
        Iterator<IntPath> iter = pathsBetween(src, target).iterator();
        return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
    }
    /**
     * Get the shortest path between two nodes in the graph. If multiple paths
     * of the shortest length exist, one will be returned, but which is
     * unspecified.
     *
     * @param src The source node
     * @param target The target node
     * @return An optional which, if non-empty, contains a path for which no
     * shorter path between the same two nodes exists
     */
    @Override
    public Optional<IntPath> shortestUndirectedPathBetween(int src, int target) {
        Iterator<IntPath> iter = undirectedPathsBetween(src, target).iterator();
        return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
    }

    /**
     * Get a list of all paths between the source and target node, sorted low to
     * high by length.
     *
     * @param src The source node
     * @param target The target node
     * @return A list of paths
     */
    @Override
    public List<IntPath> pathsBetween(int src, int target) {
        List<IntPath> paths = new ArrayList<>();
        IntPath base = new IntPath().add(src);
        PairSet seenPairs = new PairSet(size());
        // If there is a direct edge, we will miss that, so add it now
        if (outboundEdges[src].get(target)) {
            seenPairs.add(src, target);
            paths.add(new IntPath().add(src).add(target));
        }
        pathsTo(src, target, base, paths, seenPairs);
        Collections.sort(paths);
        return paths;
    }

    private void pathsTo(int src, int target, IntPath base, List<? super IntPath> paths, PairSet seenPairs) {
        if (src == target) {
            paths.add(base.copy().add(target));
            return;
        }
        outboundEdges[src].forEachSetBitAscending(bit -> {
            if (seenPairs.contains(src, bit)) {
                return;
            }
            seenPairs.add(src, bit);
            if (bit == target) {
                IntPath found = base.copy().add(target);
                paths.add(found);
            } else {
                if (!base.contains(bit)) {
                    pathsTo(bit, target, base.copy().add(bit), paths, seenPairs);
                }
            }
        });
    }

    public List<IntPath> undirectedPathsBetween(int src, int target) {
        List<IntPath> paths = new ArrayList<>();
        IntPath base = new IntPath().add(src);
        PairSet seenPairs = new PairSet(size());
        // If there is a direct edge, we will miss that, so add it now
        if (neighbors(src).get(target)) {
            seenPairs.add(src, target);
            paths.add(new IntPath().add(src).add(target));
        }
        undirectedPathsTo(src, target, base, paths, seenPairs);
        Collections.sort(paths);
        return paths;
    }

    private void undirectedPathsTo(int src, int target, IntPath base, List<? super IntPath> paths, PairSet seenPairs) {
        if (src == target) {
            paths.add(base.copy().add(target));
            return;
        }
        neighbors(src).forEachSetBitAscending(bit -> {
            if (seenPairs.contains(src, bit)) {
                return;
            }
            seenPairs.add(src, bit);
            if (bit == target) {
                IntPath found = base.copy().add(target);
                paths.add(found);
            } else {
                if (!base.contains(bit)) {
                    pathsTo(bit, target, base.copy().add(bit), paths, seenPairs);
                }
            }
        });
    }

    private void closureOf(int node, MutableBits into, int depth) {
        if (into.get(node)) {
            return;
        }
        if (depth > 0) {
            into.set(node);
        }
        outboundEdges[node].forEachSetBitAscending(bit -> {
            if (bit != node) {
                closureOf(bit, into, depth + 1);
            }
            into.set(bit);
        });
    }

    /**
     * Collect the reverse closure of a node - the set of all nodes which have
     * an outbound edge to this one or an ancestor of those nodes.
     *
     * @param node An element in the graph
     * @return A bit set
     */
    @Override
    public Bits reverseClosureOf(int node) {
        MutableBits result = MutableBits.create(size());
        reverseClosureOf(node, result, 0);
        return result;
    }

    /**
     * Convert this graph to a PairSet, which internally uses a single
     * MutableBits of size * size bits to represent the matrix of all edges. For
     * sparse graphs, this may be a larger data structure than the original
     * graph.
     *
     * @return A PairSet
     */
    @Override
    public PairSet toPairSet() {
        PairSet result = new PairSet(size());
        for (int i = 0; i < size(); i++) {
            int index = i;
            inboundEdges[i].forEachSetBitAscending(bit -> {
                result.add(bit, index);
            });
        }
        return result;
    }

    private void reverseClosureOf(int node, MutableBits into, int depth) {
        if (into.get(node)) {
            return;
        }
        if (depth > 0) {
            into.set(node);
        }
        inboundEdges[node].forEachSetBitAscending(bit -> {
            if (bit != node /* && !into.get(bit) */) {
                reverseClosureOf(bit, into, depth + 1);
            }
            into.set(bit);
        });
    }

    public StringGraph toStringGraph(String[] names) {
        return new BitSetStringGraph(this, names);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Arrays.deepHashCode(this.outboundEdges);
        return hash;
    }

    /**
     * Get the combined cardinality of all nodes in this graph - the total
     * number of edges.
     *
     * @return The edge count
     */
    @Override
    public int totalCardinality() {
        int result = 0;
        for (Bits bs : this.inboundEdges) {
            result += bs.cardinality();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BitSetGraph other = (BitSetGraph) obj;
        return Arrays.deepEquals(this.outboundEdges, other.outboundEdges);
    }

    @Override
    public IntGraph omitting(int... items) {
        if (items.length == 0) {
            return this;
        }
        int[] finalItems = Arrays.copyOf(items, items.length);
        Arrays.sort(finalItems);
        assert noDuplicates(finalItems);
        BitSet[] newOutbound = new BitSet[outboundEdges.length - finalItems.length];
        BitSet[] newInbound = new BitSet[newOutbound.length];
        int cumulativeRemoved = 0;
        for (int i = 0; i < outboundEdges.length; i++) {
            int pos = Arrays.binarySearch(finalItems, i);
            if (pos >= 0) {
                cumulativeRemoved++;
                continue;
            }
            Bits oldOutbound = outboundEdges[i];
            Bits oldInbound = inboundEdges[i];
            BitSet newOut = new BitSet(oldOutbound.cardinality());
            BitSet newIn = new BitSet(oldInbound.cardinality());
            newOutbound[i-cumulativeRemoved] = newOut;
            newInbound[i-cumulativeRemoved] = newIn;
            oldOutbound.forEachSetBitAscending(bit -> {
                boolean isRemoved = Arrays.binarySearch(finalItems, bit) >= 0;
                if (isRemoved) {
                    return;
                }
                int indexToSet = bit - valueCountBelow(bit, finalItems);
                newOut.set(indexToSet);
            });
            oldInbound.forEachSetBitAscending(bit -> {
                boolean isRemoved = Arrays.binarySearch(finalItems, bit) >= 0;
                if (isRemoved) {
                    return;
                }
                int indexToSet = bit - valueCountBelow(bit, finalItems);
                newIn.set(indexToSet);
            });
        }
        return new BitSetGraph(newOutbound, newInbound);
    }

    static int valueCountBelow(int value, int[] sortedArray) {
        int result = 0;
        for (int i = 0; i < sortedArray.length; i++) {
            if (sortedArray[i] < value) {
                result++;
            } else {
                break;
            }
        }
        return result;
    }

    static boolean noDuplicates(int[] arr) {
        int last = -1;
        for (int i = 0; i < arr.length; i++) {
            if (i != 0 && last == arr[i]) {
                return false;
            }
            last = arr[i];
        }
        return true;
    }

    public void diff(IntGraph other, BiConsumer<IntGraph, IntGraph> c) {
        IntGraphBuilder added = new IntGraphBuilder();
        IntGraphBuilder removed = new IntGraphBuilder();
        edges((a, b) -> {
            if (!other.containsEdge(a, b)) {
                removed.addEdge(a, b);
            }
        });
        other.edges((a, b) -> {
            if (!containsEdge(a, b)) {
                added.addEdge(a, b);
            }
        });
        c.accept(added.build(), removed.build());
    }
}
