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

import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * A tree, implemented as arrays of BitSets (which typically are IDs for some
 * other sort of object).
 *
 * @author Tim Boudreau
 */
public interface BitSetGraph {

    /**
     * Wrapper this tree as an object tree, using the passed list of items. The
     * list <i>must</i> be the same size as the number of items represented in
     * the tree. It may not contain duplicates. The identity of items must not
     * change on the fly.
     * <p>
     * The items can be sorted in any order - the tree will internally sort them
     * and look up the original index in this list to map them back.
     * </p>
     *
     * @param <T> The object type
     * @param items The items
     * @param type The type of the items
     * @return An object tree
     */
    default <T extends Comparable<T>> ObjectGraph<T> toObjectTree(List<T> items, Class<T> type) {
        return new ObjectGraphImpl<T>(this, items, type);
    }

    /**
     * Get the set of children of this node. Do not modify the contents of the
     * returned bit set.
     *
     * @param node
     * @return The node
     */
    BitSet children(int node);

    /**
     * Get those nodes which have no children.
     *
     * @return A set of nodes
     */
    BitSet bottomLevelNodes();

    /**
     * Get the nodes, sorted highest to lowest, by the size of their closure.
     *
     * @return An array of node ids
     */
    int[] byClosureSize();

    /**
     * Get the nodes, sorted highest to lowest, by the size of their inverse
     * closure - by the number of paths leading to the top of the tree from
     * them.
     *
     * @return
     */
    int[] byReverseClosureSize();

    /**
     * Get the set of nodes only contained in the closure of a or of b but not
     * both.
     *
     * @param a The first node
     * @param b The second node
     * @return
     */
    BitSet closureDisjunction(int a, int b);

    /**
     * Get the set of nodes which are in the closure of only one of the passed
     * array of nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return
     */
    BitSet closureDisjunction(int... nodes);

    /**
     * Get the closure of a node, following rules to the bottom of the tree.
     *
     * @param node A node
     * @return Its closure
     */
    BitSet closureOf(int node);

    /**
     * Get the size of the closure of a node. Equivalent to
     * closureOf(node).cardinality().
     *
     * @param node A node
     * @return The size of its closure
     */
    int closureSize(int node);

    /**
     * Get the union of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set of nodes
     */
    BitSet closureUnion(int a, int b);

    /**
     * Get the intersection of the closure of two nodes.
     *
     * @param a The first node
     * @param b The second node
     * @return A set
     */
    BitSet closureIntersection(int a, int b);
    /**
     * Get those top level nodes whose closure contains no nodes in the closure
     * of any others.
     *
     * @return A set of integers
     */
    Set<Integer> disjointItems();

    /**
     * Get the distance on the shortest path from node a to node b in the
     * closure of a.
     *
     * @param a The first node
     * @param b The second node
     * @return The distance
     */
    int distance(int a, int b);

    /**
     * Get all of the edges defined in this tree as pairs of integers.
     *
     * @return A list of arrays of size 2 of integers. May include
     * self-references.
     */
    List<int[]> edges();

    /**
     * Compute the eigenvector centrality of each node, returning an array of
     * doubles[] corresponding to the nodes (i.e. result[0] is the score for the
     * node 0).
     *
     * @param maxIterations The maximum number of iterations to refine the
     * results (impacts performance)
     * @param maxDiff The maximum difference in result iterations before
     * iteration has reached the point of diminishing returns and should abort.
     * @param inEdges If true, use only inbound edges to compute the result
     * @param ignoreSelfEdges If true, ignore self-edges when computing the
     * score
     * @param l2norm If true, normalize the results - does not affect the sort
     * order, just makes the results more comparable to the results on other
     * trees - useful if multithreading and operating on a partitioned tree.
     * @return An array of scores
     */
    double[] eigenvectorCentrality(int maxIterations, double maxDiff, boolean inEdges, boolean ignoreSelfEdges, boolean l2norm);

    /**
     * Get the number of inbound references into a node.
     *
     * @param node
     * @return
     */
    int inboundReferenceCount(int node);

    /**
     * Determine if some node in the closure of a node has an edge back to this
     * node, excluding self-edges.
     *
     * @param node A node
     * @return Whether or not it is reachable from itself but not directly by
     * itself
     */
    boolean isIndirectlyRecursive(int node);

    /**
     * Determine if a rule has a self-edge or a rule in its closure has an
     * outbound edge to it.
     *
     * @param node A node
     * @return Whether or not it is recursive
     */
    boolean isRecursive(int node);

    /**
     * Determine whether a node has no inbound edges or not.
     *
     * @param node The node
     * @return Whether or not it has edges
     */
    boolean isUnreferenced(int node);

    /**
     * Get the number of outbound edges to this node.
     *
     * @param node A node
     * @return The number of edges it has outbound.
     */
    int outboundReferenceCount(int node);

    /**
     * Compute the pagerank score for a node.
     *
     * @param maxError Determines when error levels are acceptable and
     * computation can complete
     * @param dampingFactor
     * @param maximumIterations The maximum number of iterations
     * @param normalize Whether to normalize the results to be comparable to
     * those from other invocations.
     * @return An array of scores
     */
    double[] pageRank(double maxError, double dampingFactor, int maximumIterations, boolean normalize);

    /**
     * Get the set of nodes which have inbound edges to a node.
     *
     * @param node A node
     * @return The set of parents
     */
    BitSet parents(int node);

    /**
     * Get the set of nodes which can be reached from a node.
     *
     * @param startingNode The starting node
     * @return A set of bits
     */
    BitSet reachableFrom(int startingNode);

    /**
     * Get the reverse closure of a node - all nodes above it in the graph
     * reachable from its inbound edges.
     *
     * @param node A node
     * @return Its closure
     */
    BitSet reverseClosureOf(int node);

    /**
     * Get the size of the closure of a node.
     *
     * @param node A node
     * @return The size of its closure
     */
    int reverseClosureSize(int node);

    /**
     * Get the number of nodes in the graph.
     *
     * @return The count
     */
    int size();

    /**
     * Get the set of nodes which have no inbound edges.
     *
     * @return Those nodes which have no inbound edges.
     */
    BitSet topLevelOrOrphanNodes();

    /**
     * Walk the tree, depth first. Each node will be visited exactly once.
     *
     * @param v A visitor
     */
    void walk(BitSetGraphVisitor v);

    /**
     * Walk the tree from a starting node.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    void walk(int startingWith, BitSetGraphVisitor v);

    /**
     * Walk the tree upwards, following inbound edges toward the top of the
     * graph. Each node will be visited exactly once.
     *
     * @param v
     */
    void walkUpwards(BitSetGraphVisitor v);

    /**
     * Walk the tree upwards, following inbound edges toward the top of the
     * graph, from a starting node. Each node will be visited exactly once.
     *
     * @param startingWith The starting node
     * @param v A visitor
     */
    void walkUpwards(int startingWith, BitSetGraphVisitor v);
}
