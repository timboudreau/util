package com.mastfrog.util.tree;

import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
final class ScoreImpl<T> implements Score<T> {

    private final double score;
    private final int ruleIndex;
    private final T node;

    ScoreImpl(double score, int ruleIndex, T node) {
        this.score = score;
        this.ruleIndex = ruleIndex;
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
    public int nodeIndex() {
        return ruleIndex;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Score<?>
                && ((Score<?>) o).nodeIndex() == nodeIndex()
                && Objects.equals(((Score<?>)o).node(), node());
    }

    @Override
    public int hashCode() {
        return 7 * ruleIndex
                + Double.valueOf(Double.doubleToLongBits(score)).hashCode();
    }

    @Override
    public String toString() {
        return node + ":" + score;
    }
}
