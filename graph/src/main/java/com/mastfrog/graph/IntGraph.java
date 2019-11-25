package com.mastfrog.graph;

import com.mastfrog.abstractions.list.IndexedResolvable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import com.mastfrog.bits.Bits;
import com.mastfrog.function.IntBiConsumer;

/**
 *
 * @author Tim Boudreau
 */
public interface IntGraph {

    public static IntGraph create(BitSet[] reverseReferences, BitSet[] references) {
        return new BitSetGraph(reverseReferences, references);
    }

    public static IntGraph create(Bits[] references) {
        return new BitSetGraph(references);
    }
    public static IntGraph create(BitSet[] references) {
        return new BitSetGraph(references);
    }

    public static IntGraph create(Bits[] reverseReferences, Bits[] references) {
        return new BitSetGraph(reverseReferences, references);
    }

    public static IntGraph load(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        return BitSetGraph.load(objectInputStream);
    }

    public static IntGraphBuilder builder() {
        return new IntGraphBuilder();
    }

    public static IntGraphBuilder builder(int expectedSize) {
        return new IntGraphBuilder(expectedSize);
    }

    default <T> ObjectGraph<T> toObjectGraph(List<T> items) {
        return new BitSetObjectGraph<>(this, IndexedResolvable.forList(items));
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
    boolean abortableBreadthFirstSearch(int startingNode, boolean up, IntPredicate cons);

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
    boolean abortableDepthFirstSearch(int startingNode, boolean up, IntPredicate cons);

    /**
     * Get all edges in the graph.
     *
     * @return A set of pairs of edges
     */
    PairSet allEdges();

    /**
     * Get the set of nodes which have no outbound edges.
     *
     * @return The set of nodes which have no outbound edges
     */
    Bits bottomLevelNodes();

    /**
     * Do a breadth-first search from the starting node, visiting all nodes in
     * its closure or reverse-closure depending on the value of <code>up</code>,
     * visiting each node in some order at least once.
     *
     * @param startingNode The starting node
     * @param up If true, traverse antecedents of the starting node, not
     * descendants
     * @param cons A consumer which will visit nodes
     */
    void breadthFirstSearch(int startingNode, boolean up, IntConsumer cons);

    /**
     * Return an array of all nodes in the graph, sorted by the size of their
     * closure (the number of distinct descendant nodes they have).
     *
     * @return A count of nodes
     */
    int[] byClosureSize();

    /**
     * Return an array of all nodes in the graph, sorted by the size of their
     * reverse closure (the number of distinct ancestor nodes they have).
     *
     * @return A count of nodes
     */
    int[] byReverseClosureSize();

    /**
     * Get the set of nodes which have inbound edges from the passed node.
     *
     * @param node A node
     * @return A set
     */
    Bits children(int node);

    /**
     * Get the disjunction of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set of nodes
     */
    Bits closureDisjunction(int a, int b);

    /**
     * Get the set of those nodes which exist in the closure of only one of the
     * passed list of nodes.
     *
     * @param nodes An array of nodes
     * @return A set
     */
    Bits closureDisjunction(int... nodes);

    /**
     * Get the set of those nodes which exist in the closure of only one of the
     * passed list of nodes.
     *
     * @param nodes A set of nodes
     * @return A set
     */
    Bits closureDisjunction(Bits nodes);

    /**
     * Get the closure of this node - the set of all nodes which have this node
     * as an ancestor.
     *
     * @param node A node
     * @return A set
     */
    Bits closureOf(int node);

    /**
     * Get the count of nodes which have the passed node as an ancestor.
     *
     * @param node A node
     * @return The number of nodes which have the passed node as an ancestor
     */
    int closureSize(int node);

    /**
     * Get the union of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set of nodes
     */
    Bits closureUnion(int a, int b);

    /**
     * Return the set of nodes in the graph which have both inbound and outbound
     * references.
     *
     * @return The connector nodes
     */
    Bits connectors();

    /**
     * Determine if an edge from a to b exists.
     *
     * @param a The first edge
     * @param b The second edge
     * @return True if the edge exists
     */
    boolean containsEdge(int a, int b);

    /**
     * Perform a depth-first search, traversing the closure or reverse-closure
     * of the starting node depth-first.
     *
     * @param startingNode The starting node
     * @param up If true, traverse the reverse-closure
     * @param cons A consumer which will visit nodes
     */
    void depthFirstSearch(int startingNode, boolean up, IntConsumer cons);

    /**
     * Get the set of nodes in the graph whose closure is not shared by any
     * other nodes.
     *
     * @return A set of nodes
     */
    Bits disjointNodes();

    /**
     * Get the minimum distance between two nodes.
     *
     * @param a One node
     * @param b Another node
     * @return A distance
     */
    int distance(int a, int b);

    /**
     * Visit all edges in the graph.
     *
     * @param bi A consumer
     */
    void edges(IntBiConsumer bi);

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
    double[] eigenvectorCentrality(int maxIterations, double minDiff, boolean inEdges, boolean ignoreSelfEdges, boolean l2norm);

    /**
     * Determine if a direct inbound edge exits from one node to another.
     *
     * @param from The source node
     * @param to The destination node
     * @return True if the edge exists
     */
    boolean hasInboundEdge(int from, int to);

    /**
     * Determine if a direct outbound edge exits from one node to another.
     *
     * @param from The source node
     * @param to The destination node
     * @return True if the edge exists
     */
    boolean hasOutboundEdge(int from, int to);

    /**
     * Returns the number of inbound edges a node has.
     *
     * @param node A node
     * @return The edge count
     */
    int inboundReferenceCount(int node);

    /**
     * Determine if a node is <i>indirectly recursive</i> - if the closure of it
     * contains a cycle back to it, not counting cycles to itself.
     *
     * @param node A node
     * @return Whether or not it is indirectly recursive
     */
    boolean isIndirectlyRecursive(int node);

    /**
     * Determine if node b is contained in the closure of node a.
     *
     * @param a A node
     * @param b Another node
     * @return True if it is reachable
     */
    boolean isReachableFrom(int a, int b);

    /**
     * Determine if the passed node or a descendant of it has a cycle back to
     * itself.
     *
     * @param node A node
     * @return True if any outbound path from this node directly or indirectly
     * recurses back to it
     */
    boolean isRecursive(int node);

    /**
     * Determine if the closure of a includes b.
     *
     * @param a A node
     * @param b Another node
     * @return If b is a descendant of a
     */
    boolean isReverseReachableFrom(int a, int b);

    /**
     * Determine if a node has no inbound edges - no ancestors.
     *
     * @param node The node
     * @return true if the passed node is not referenced by any other nodes in
     * this graph
     */
    boolean isUnreferenced(int node);

    /**
     * Get the set of parent and child nodes for a node.
     *
     * @param startingNode A node
     * @return A set of nodes
     */
    Bits neighbors(int startingNode);

    /**
     * Return the set of nodes which have no edges in or out.
     *
     * @return The set of orphan nodes
     */
    Bits orphans();

    /**
     * Returns the number of outbound edges a node has.
     *
     * @param node A node
     * @return The edge count
     */
    int outboundReferenceCount(int node);

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
    double[] pageRank(double minDifference, double dampingFactor, int maximumIterations, boolean normalize);

    /**
     * Get the set of nodes which reference the passed node.
     *
     * @param node A node
     * @return Nodes which have an outbound edge to the passed one
     */
    Bits parents(int node);

    /**
     * Get a list of all paths between the source and target node, sorted low to
     * high by length.
     *
     * @param src The source node
     * @param target The target node
     * @return A list of paths
     */
    List<IntPath> pathsBetween(int src, int target);

    /**
     * Collect the reverse closure of a node - the set of all nodes which have
     * an outbound edge to this one or an ancestor of those nodes.
     *
     * @param node An element in the graph
     * @return A bit set
     */
    Bits reverseClosureOf(int node);

    /**
     * Get the count of nodes which have the passed node as a descendant.
     *
     * @param node A node
     * @return The number of nodes which have the passed node as a descendant
     */
    int reverseClosureSize(int node);

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
    Optional<IntPath> shortestPathBetween(int src, int target);

    /**
     * Convert this graph to a PairSet, which internally uses a single Bits of
     * size * size bits to represent the matrix of all edges. For sparse graphs,
     * this may be a larger data structure than the original graph.
     *
     * @return A PairSet
     */
    PairSet toPairSet();

    /**
     * Get the set of nodes which have no inbound edges - no ancestors.
     *
     * @return A set
     */
    Bits topLevelOrOrphanNodes();

    /**
     * Get the combined cardinality of all nodes in this graph - the total
     * number of edges.
     *
     * @return The edge count
     */
    int totalCardinality();

    /**
     * Walk the closure of the graph from top level nodes.
     *
     * @param v A visitor
     */
    void walk(IntGraphVisitor v);

    /**
     * Walk the closure of a node.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    void walk(int startingWith, IntGraphVisitor v);

    /**
     * Walk the inverse closure of the bottom-most nodes in the graph. Note that
     * this will not traverse subgraphs which are entirely cyclical.
     *
     * @param v A visitor
     */
    void walkUpwards(IntGraphVisitor v);

    /**
     * Walk the inverse closure of one node, visiting each ancestor exactly
     * once.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    void walkUpwards(int startingWith, IntGraphVisitor v);

    /**
     * Get the maximum node id + 1 of this graph, or the number of nodes.
     *
     * @return The size
     */
    int size();

    /**
     * Serialize this graph.
     *
     * @param out Output
     * @throws IOException If something goes wrong
     */
    void save(ObjectOutput out) throws IOException;

    Optional<IntPath> shortestUndirectedPathBetween(int src, int target);

    List<IntPath> undirectedPathsBetween(int src, int target);

    default StringGraph toStringGraph(String[] sortedNodeNames) {
        return new BitSetStringGraph(this, sortedNodeNames);
    }
}
