package com.mastfrog.graph;

import com.mastfrog.abstractions.list.IndexedResolvable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Set;
import com.mastfrog.graph.algorithm.RankingAlgorithm;
import com.mastfrog.graph.algorithm.Score;
import java.util.function.BiConsumer;

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
     * Optimized serialization support.
     *
     * @param out The output
     * @throws IOException If something goes wrong
     */
    public void save(ObjectOutput out) throws IOException;

    /**
     * Get the integer id used internally for a node.
     *
     * @param node A node
     * @return An index or -1
     */
    int toNodeId(T node);

    /**
     * Get the node for an index.
     *
     * @param index The index
     * @return A node
     */
    T toNode(int index);

    List<Score<T>> apply(RankingAlgorithm<?> alg);

    /**
     * Get the set of paths that exist between two nodes.
     *
     * @param a A first node
     * @param b A second node
     * @return The paths
     */
    List<ObjectPath<T>> pathsBetween(T a, T b);

    /**
     * Convert this graph to its (usually internal) IntGraph, needed for
     * serialization.
     *
     * @param consumer A consumer that takes the list of items and the
     * graph.
     */
    void toIntGraph(BiConsumer<IndexedResolvable<? extends T>, IntGraph> consumer);

    /**
     * Create a copy of this graph, omitting the passed set of
     * items.
     *
     * @param items The items to omit
     * @return A new graph
     */
    ObjectGraph<T> omitting(Set<T> items);

    /**
     * Get the number of elements in this graph.
     *
     * @return The size
     */
    int size();

    /**
     * Topologically sort a set of items; note that if a graph contains cycles,
     * topological sorting is impossible - you will get <i>some</i> order that
     * roughly approximates a topological sort for any items that are not part
     * of a cycle; the order of appearance of items that participate in cycles
     * is implementation-dependent.
     *
     * @param items some items
     * @return A list
     */
    List<T> topologicalSort(Set<T> items);
}
