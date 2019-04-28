package com.mastfrog.util.tree;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps an instance of BitSetGraphImpl and converts its BitSets to sets of
 string rule names.
 *
 * @author Tim Boudreau
 */
public final class BitSetGraphBuilder<T extends Comparable<T>> {

    private final BitSet[] ruleReferences;
    private final BitSet[] referencedBy;
    private final int[] ruleIndicesForSorted;
    private final T[] namesSorted;
    private T currRule;
    private static final Logger LOG
            = Logger.getLogger(BitSetGraphBuilder.class.getName());

    @SuppressWarnings("unchecked")
    public BitSetGraphBuilder(Collection<T> objects, Class<T> type) {
        // A file with a partially typed rule may contain duplicate rule names;
        // we weed them out here, as they would wreak havoc with binary search
        List<T> deduped = new ArrayList<>(new LinkedHashSet<>(objects));
        if (deduped.size() != objects.size()) {
            LOG.log(Level.FINE, "Received collection with duplicates: {1}",
                    objects);
        }
        ruleReferences = new BitSet[deduped.size()];
        referencedBy = new BitSet[deduped.size()];
        ruleIndicesForSorted = new int[deduped.size()];
        T[] array = (T[]) Array.newInstance(type, deduped.size());
        namesSorted = deduped.toArray(array);
        Arrays.sort(namesSorted);
        for (int i = 0; i < namesSorted.length; i++) {
            ruleIndicesForSorted[i] = deduped.indexOf(namesSorted[i]);
            ruleReferences[i] = new BitSet(deduped.size());
            referencedBy[i] = new BitSet(deduped.size());
        }
    }

    public void addEdge(T referencer, T referenced) {
        int referencerIndex = Arrays.binarySearch(namesSorted, referencer);
        if (referencerIndex < 0) {
            LOG.log(Level.INFO, "Attempt to add an edge between rules {0} and "
                    + "{1} but {0} is not in the grammar",
                    new Object[]{referencer, referenced});
            return;
        }
        int referencedIndex = Arrays.binarySearch(namesSorted, referenced);
        if (referencedIndex < 0) {
            LOG.log(Level.INFO, "Attempt to add an edge between rules {0} and "
                    + "{1} but {1} is not in the grammar",
                    new Object[]{referencer, referenced});
            return;
        }
        ruleReferences[referencerIndex].set(referencedIndex);
        referencedBy[referencedIndex].set(referencerIndex);
    }

    public void enterItem(T item, Runnable run) {
        T old = currRule;
        currRule = item;
        try {
            if (old != null) {
                addEdge(old, item);
            }
            run.run();
        } finally {
            currRule = old;
        }
    }

    public BitSetGraph build() {
        return new BitSetGraphImpl(ruleReferences, referencedBy);
    }
}
