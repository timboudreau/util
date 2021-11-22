/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.graph.algorithm;

/**
 * Implementation over strings of the scores as returned by page rank and
 * eigenvector centrality algorithms.
 */
public interface Score<T> extends Comparable<Score<?>> {

    /**
     * The name of graph node
     *
     * @return The name
     */
    public T node();

    /**
     * The score of the graph node, relative to others.
     *
     * @return A score
     */
    public double score();

    /**
     * The integer index of the node in the underlying graph.
     *
     * @return
     */
    public int nodeId();

    /**
     * Compares scores, sorting <i>higher</i> scores to the top.
     *
     * @param o Another score
     * @return a comparison
     */
    @Override
    public default int compareTo(Score<?> o) {
        if (o == this) {
            return 0;
        }
        double a = score();
        double b = o.score();
        return a > b ? -1 : a == b ? 0 : 1;
    }

    public static <T> Score<T> create(double score, int index, T val) {
        return new ScoreImpl<>(score, index, val);
    }

}
