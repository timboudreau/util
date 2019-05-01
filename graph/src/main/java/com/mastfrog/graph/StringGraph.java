package com.mastfrog.graph;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import static com.mastfrog.graph.BitSetStringGraph.sanityCheckArray;

/**
 *
 * @author Tim Boudreau
 */
public interface StringGraph extends ObjectGraph<String> {

    /**
     * Create a graph from the passed bit set graph, and a pre-sorted array of
     * unique strings where integer nodes in the passed graph correspond to
     * offsets within the array. The array must be sorted, not have duplicates,
     * and have a matching number of elements for the unique node ids in the
     * tree. If assertions are on, asserts will check that these invariants
     * hold.
     *
     * @param graph A graph
     * @param sortedArray An array of strings matching the requirements stated
     * above
     * @return A graph of strings which wraps the original graph
     */
    public static StringGraph create(IntGraph graph, String[] sortedArray) {
        assert sanityCheckArray(sortedArray);
        return new BitSetStringGraph(graph, sortedArray);
    }

    /**
     * Optimized serialization support.
     *
     * @param out The output
     * @throws IOException If something goes wrong
     */
    public void save(ObjectOutput out) throws IOException;

    public static StringGraph load(ObjectInput in) throws IOException, ClassNotFoundException {
        return BitSetStringGraph.load(in);
    }

}
