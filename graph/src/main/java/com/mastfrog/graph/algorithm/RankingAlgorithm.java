package com.mastfrog.graph.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;
import com.mastfrog.graph.IntGraph;

/**
 *
 * @author Tim Boudreau
 */
public abstract class RankingAlgorithm<A extends RankingAlgorithm> extends Algorithm<double[], A> {

    RankingAlgorithm() {
        
    }

    public <T> List<Score<T>> apply(IntGraph graph, IntFunction<T> toObject) {
        double[] scores = apply(graph);
        List<Score<T>> result = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            result.add(new ScoreImpl<T>(scores[i], i, toObject.apply(i)));
        }
        Collections.sort(result);
        return result;
    }
}
