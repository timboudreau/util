package com.mastfrog.graph.algorithm;

import java.util.Arrays;
import com.mastfrog.bits.Bits;
import com.mastfrog.graph.IntGraph;

/**
 *
 * @author Tim Boudreau
 */
public final class EigenvectorCentrality extends RankingAlgorithm<EigenvectorCentrality> {

    public static IntParameter<EigenvectorCentrality> MAXIMUM_ITERATIONS = createIntegerParameter(EigenvectorCentrality.class, "maximumIterations");
    public static DoubleParameter<EigenvectorCentrality> MINIMUM_DIFFERENCE = createDoubleParameter(EigenvectorCentrality.class, "minimumDifference");
    public static BooleanParameter<EigenvectorCentrality> USE_IN_EDGES = createBooleanParameter(EigenvectorCentrality.class, "inEdges");
    public static BooleanParameter<EigenvectorCentrality> IGNORE_SELF_EDGES = createBooleanParameter(EigenvectorCentrality.class, "ignoreSelfEdges");
    public static BooleanParameter<EigenvectorCentrality> NORMALIZE = createBooleanParameter(EigenvectorCentrality.class, "normalize");
    private int maxIterations;
    private double minDiff;
    private boolean inEdges;
    private boolean ignoreSelfEdges;
    private boolean normalize;

    EigenvectorCentrality() {
        this(400, 0.000001, false, true, true);
    }

    EigenvectorCentrality(int maxIterations, double minDiff, boolean inEdges, boolean ignoreSelfEdges, boolean normalize) {
        this.maxIterations = maxIterations;
        this.minDiff = minDiff;
        this.inEdges = inEdges;
        this.ignoreSelfEdges = ignoreSelfEdges;
        this.normalize = normalize;
    }

    @Override
    public double[] apply(IntGraph graph) {
        int sz = graph.size();
        double[] unnormalized = new double[sz];
        double[] centrality = new double[sz];
        Arrays.fill(centrality, 1.0 / (double) sz);
        double diff = 0.0;
        int iter = 0;
        do {
            for (int i = 0; i < sz; i++) {
                Bits dests = inEdges ? graph.parents(i) : graph.neighbors(i);
                double sum = dests.sum(centrality, ignoreSelfEdges ? i : Integer.MIN_VALUE);
                unnormalized[i] = sum;
                double s;
                if (normalize) {
                    double l2sum = 0.0;
                    for (int j = 0; j < sz; j++) {
                        l2sum += unnormalized[j] * unnormalized[j];
                    }
                    s = (l2sum == 0.0) ? 1.0 : 1 / Math.sqrt(l2sum);
                } else {
                    double l1sum = 0.0;
                    for (int j = 0; j < sz; j++) {
                        l1sum += unnormalized[j];
                    }
                    s = l1sum == 0.0 ? 1.0 : 1 / l1sum;
                }
                diff = 0.0;
                for (int j = 0; j < sz; j++) {
                    double val = unnormalized[j] * s;
                    diff += Math.abs(centrality[j] - val);
                    centrality[j] = val;
                }
            }
        } while (iter++ < maxIterations && diff > minDiff);
        return centrality;
    }

    @Override
    public EigenvectorCentrality setParameter(DoubleParameter<EigenvectorCentrality> param, double value) {
        if (MINIMUM_DIFFERENCE == param) {
            minDiff = value;
            return this;
        }
        return super.setParameter(param, value);
    }

    @Override
    public EigenvectorCentrality setParameter(IntParameter<EigenvectorCentrality> param, int value) {
        if (MAXIMUM_ITERATIONS == param) {
            maxIterations = value;
            return this;
        }
        return super.setParameter(param, value);
    }

    @Override
    public EigenvectorCentrality setParameter(BooleanParameter<EigenvectorCentrality> param, boolean value) {
        if (USE_IN_EDGES == param) {
            inEdges = value;
            return this;
        } else if (IGNORE_SELF_EDGES == param) {
            ignoreSelfEdges = value;
            return this;
        } else if (NORMALIZE == param) {
            normalize = value;
            return this;
        }
        return super.setParameter(param, value);
    }

}
