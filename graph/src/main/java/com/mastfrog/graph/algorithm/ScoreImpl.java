package com.mastfrog.graph.algorithm;

/**
 *
 * @author Tim Boudreau
 */
final class ScoreImpl<T> implements Score<T> {

    private final double score;
    private final int nodeId;
    private final T node;

    ScoreImpl(double score, int ruleIndex, T node) {
        this.score = score;
        this.nodeId = ruleIndex;
        this.node = node;
    }

    @Override
    public T node() {
        return node;
    }

    @Override
    public double score() {
        return score;
    }

    @Override
    public int nodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Score<?> && ((Score<?>) o).nodeId() == nodeId();
    }

    @Override
    public int hashCode() {
        return 7 * nodeId;
    }

    @Override
    public String toString() {
        return node + ".\t" + score;
    }
}
