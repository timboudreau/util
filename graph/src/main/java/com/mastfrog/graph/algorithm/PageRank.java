package com.mastfrog.graph.algorithm;

import java.util.function.IntToDoubleFunction;
import com.mastfrog.graph.IntGraph;

/**
 *
 * @author Tim Boudreau
 */
public final class PageRank extends RankingAlgorithm<PageRank> {

    public static Algorithm.IntParameter<PageRank> MAXIMUM_ITERATIONS = Algorithm.createIntegerParameter(PageRank.class, "maximumIterations");
    public static Algorithm.DoubleParameter<PageRank> MINIMUM_DIFFERENCE = Algorithm.createDoubleParameter(PageRank.class, "minimumDifference");
    public static Algorithm.DoubleParameter<PageRank> DAMPING_FACTOR = Algorithm.createDoubleParameter(PageRank.class, "dampingFactor");
    public static Algorithm.BooleanParameter<PageRank> NORMALIZE = Algorithm.createBooleanParameter(PageRank.class, "normalize");
    private double minDifference;
    private double dampingFactor;
    private int maximumIterations;
    private boolean normalize;

    PageRank(double minDifference, double dampingFactor, int maximumIterations, boolean normalize) {
        this.minDifference = minDifference;
        this.dampingFactor = dampingFactor;
        this.maximumIterations = maximumIterations;
        this.normalize = normalize;
    }

    PageRank() {
        this(0.0000000000000004, 0.00000000001, 1000, true);
    }

    double sum(int size, IntToDoubleFunction func) {
        double result = 0D;
        for (int i = 0; i < size; i++) {
            result += func.applyAsDouble(i);
        }
        return result;
    }

    @Override
    public double[] apply(IntGraph graph) {
        double difference;
        int cnt = 0;
        int graphSize = graph.size();
        double n = graphSize;
        double[] result = new double[graphSize];
        for (int i = 0; i < result.length; i++) {
            result[i] = 1d / n;
        }
        do {
            difference = 0.0;
            double danglingFactor = 0;
            if (normalize) {
                danglingFactor = dampingFactor / n * sum(graphSize, (i) -> {
                    if (graph.children(i).isEmpty()) {
                        return result[i];
                    }
                    return 0.0;
                });
            }
            for (int i = 0; i < graphSize; i++) {
                double inputSum = graph.parents(i).sum((int j) -> {
                    double outDegree = graph.children(j).cardinality();
                    if (outDegree != 0) {
                        return result[j] / outDegree;
                    }
                    return 0.0;
                });
                double val = (1.0 - dampingFactor) / n + dampingFactor * inputSum + danglingFactor;
                difference += Math.abs(val - result[i]);
                if (result[i] < val) {
                    result[i] = val;
                }
            }
            cnt++;
        } while ((difference > minDifference) && cnt < maximumIterations);
        return result;
    }

    @Override
    public PageRank setParameter(Algorithm.BooleanParameter<PageRank> param, boolean value) {
        if (NORMALIZE == param) {
            normalize = value;
            return this;
        }
        return super.setParameter(param, value);
    }

    @Override
    public PageRank setParameter(Algorithm.DoubleParameter<PageRank> param, double value) {
        if (MINIMUM_DIFFERENCE == param) {
            minDifference = value;
            return this;
        } else if (DAMPING_FACTOR == param) {
            dampingFactor = value;
            return this;
        }
        return super.setParameter(param, value);
    }

    @Override
    public PageRank setParameter(Algorithm.IntParameter<PageRank> param, int value) {
        if (PageRank.MAXIMUM_ITERATIONS == param) {
            maximumIterations = value;
            return this;
        }
        return super.setParameter(param, value);
    }

}
