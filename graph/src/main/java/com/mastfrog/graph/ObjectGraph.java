package com.mastfrog.graph;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Set;
import com.mastfrog.graph.algorithm.RankingAlgorithm;
import com.mastfrog.graph.algorithm.Score;

/**
 *
 * @author Tim Boudreau
 */
public interface ObjectGraph<T> {

    /**
     * Get a list of the nodes in the graph sorted by the size of their closure.
     *
     * @return A list of string node names
     */
    List<T> byClosureSize();

    /**
     * Get a list of the nodes in the graph sorted by the size of their reverse
     * closure (all paths to the top of the graph from this node).
     *
     * @return A list of string node names
     */
    List<T> byReverseClosureSize();

    /**
     * For logging convenience, get a list of strings that identify the edges
     * present in this graph.
     *
     * @return
     */
    Set<String> edgeStrings();

    /**
     * Get the set of strings which are immediate parent nodes to a given one
     * (if the graph is cyclic, this may include the node name passed).
     *
     * @param node A node name
     * @return The set of parents
     */
    Set<T> parents(T node);

    /**
     * Get the set of string node names which are immediate child nodes of a
     * given one (if the graph is cyclic, this may include the node name
     * passed).
     *
     *
     * @param node The node name
     * @return A set of child node names
     */
    Set<T> children(T node);

    /**
     * Count the number of nodes that have outbound edges to the passed node.
     * Will be zero if it has none.
     *
     * @param node The node name
     * @return A count of edges
     */
    int inboundReferenceCount(T node);

    /**
     * Count the number of nodes this node has outbound edges to.
     *
     * @param node The node name
     * @return A count of edges
     */
    int outboundReferenceCount(T node);

    /**
     * Get the set of nodes which have no inbound edges.
     *
     * @return A set of node names
     */
    Set<T> topLevelOrOrphanNodes();

    /**
     * Get the set of nodes which have no outbound edges.
     *
     * @return A set of nodes
     */
    Set<T> bottomLevelNodes();

    /**
     * Returns true if this node has no inbound edges.
     *
     * @param node A node
     * @return Whether or not it has inbound edges
     */
    boolean isUnreferenced(T node);

    /**
     * Determine the size of the closure of this node, following outbound edges
     * to the bottom of the graph, traversing each node once.
     *
     * @param node A node
     * @return The size of its closure
     */
    int closureSize(T node);

    /**
     * Determine the size of the inverse closure of this node, following inbound
     * edges to the top of the graph, traversing each node once.
     *
     * @param node
     * @return
     */
    int reverseClosureSize(T node);

    /**
     * Get the reverse closure of a node in the graph - all nodes which have an
     * outbound edge to this node, and all nodes which have an outbound edge to
     * one of those, and so forth, to the top of the graph.
     *
     * @param node A node name
     * @return A set of nodes
     */
    Set<T> reverseClosureOf(T node);

    /**
     * Get the closure of this node in the graph - all nodes which are reachable
     * following outbound edges from this node and its descendants.
     *
     * @param node A node name
     * @return A set of nodes
     */
    Set<T> closureOf(T node);

    // Original implementation does not support these and is
    // kept for testing changes to this one, so provide default
    // implementations of these methods
    /**
     * Walk the tree of nodes in some order, such that each node is only visited
     * once.
     *
     * @param v A visitor
     */
    default void walk(ObjectGraphVisitor<? super T> v) {
        throw new UnsupportedOperationException();
    }

    /**
     * Walk the tree of node definitions and node references in some order,
     * starting from the passed starting node.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    default void walk(T startingWith, ObjectGraphVisitor<? super T> v) {
        throw new UnsupportedOperationException();
    }

    /**
     * Walk the antecedents of a node.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    default void walkUpwards(T startingWith, ObjectGraphVisitor<? super T> v) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the distance along the shortest path between two node.
     *
     * @param a One node
     * @param b Another node
     * @return the distance
     */
    default int distance(T a, T b) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the eigenvector centrality - "most likely to be connected
     * *through*" - score for each node. This finds nodes which are most
     * critical - connectors - in the node graph.
     *
     * @return
     */
    default List<Score<T>> eigenvectorCentrality() {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the pagerank score of every node in the graph.
     *
     * @return
     */
    default List<Score<T>> pageRank() {
        throw new UnsupportedOperationException();
    }

    /**
     * This requires some explaining - it is used to pick the initial set of
     * colorings that are active - attempting to find nodes that are likely to
     * be ones someone would want flagged.
     *
     * The algorithm is this: Rank the nodes according to their pagerank. That
     * typically gets important but common grammar elements such as "expression"
     * or "arguments". Take the top third of those nodes - these will be the
     * most connected to nodes. Then take the closure of each of those - the set
     * of nodes it or nodes it calls touches, and xor them to get the
     * disjunction of their closures. That gives you those nodes which are
     * called indirectly or directly by *one* of the highly ranked nodes, but
     * none of the others. These have a decent chance of being unique things one
     * would like distinct syntax highlighting for.
     *
     * @return
     */
    default Set<T> disjunctionOfClosureOfHighestRankedNodes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a graph from the passed bit set graph, and a pre-sorted array of
     * unique strings where integer nodes in the passed graph correspond to
     * offsets within the array. The array must be sorted, not have duplicates,
     * and have a matching number of elements for the unique node ids in the
     * tree. If assertions are on, asserts will check that these invariants
     * hold.
     *
     * @param graph A graph
     * @param sortedArray An array of strings matching the requirements stated
     * above
     * @return A graph of strings which wraps the original graph
     */
//    public static <T> ObjectGraph<T> create(IntGraph graph, IntFunction<T> convert) {
//        return new BitSetObjectGraph<T>(graph, convert);
//    }
    /**
     * Optimized serialization support.
     *
     * @param out The output
     * @throws IOException If something goes wrong
     */
    public void save(ObjectOutput out) throws IOException;

//    public static <T> ObjectGraph<T> load(ObjectInput in) throws IOException, ClassNotFoundException {
//        return BitSetObjectGraph.load(in);
//    }
    int toNodeId(T name);

    T toNode(int index);

    List<Score<T>> apply(RankingAlgorithm<?> alg);

}
