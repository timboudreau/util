package com.mastfrog.graph;

/**
 * Visitor interface for traversing graphs. The visitor will visit every
 * node of the graph in some order starting from the top level nodes, such that
 * every node is visited exactly once.
 * <p>
 * If a node has inbound edges from more than one other node, it is unspecified
 * which parent the visitor will be invoked as a child of.
 * </p>
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface ObjectGraphVisitor<T> {

    /**
     * Enter a node; subsequent calls to enterNode before a call to exitNode
     * means the subsequent nodes are referenced by the previous one. EnterNode
     * will be called exactly once for each node, starting from top-level and
     * orphan nodes which have no antecedents. For nodes referenced by
     * descendants, the order they are passed to enterNode is
     * implementation-depenedent - they will appear nested under some node that
     * calls them, but not more than one.
     *
     * @param node The node
     * @param depth The depth of this node in the tree, in the traversal pattern
     * being used.
     */
    void enterNode(T node, int depth);

    /**
     * Called when the last child of a node has been visited.
     *
     * @param node The node
     * @param depth The depth of this node in the tree in the traversal pattern
     * being used
     */
    default void exitNode(T node, int depth) {
    }

}
