package com.mastfrog.graph;

/**
 * Visitor interface for BitSetGraph, a graph which maps integer nodes to other
 * integer nodes.
 *
 * @author Tim Boudreau
 */
public interface IntGraphVisitor {

    void enterNode(int node, int depth);

    default void exitNode(int node, int depth) {
    }
}
