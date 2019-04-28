package com.mastfrog.util.tree;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Tim Boudreau
 */
final class ObjectGraphImpl<T extends Comparable<T>> implements ObjectGraph<T> {

    private final T[] itemsSorted;
    private final int[] ruleIndicesForSorted;
    private final BitSetGraph tree;
    private final T[] items;

    @SuppressWarnings("unchecked")
    ObjectGraphImpl(BitSetGraph tree, List<T> initialItems, Class<T> type) {
        this.tree = tree;
        T[] init = (T[]) Array.newInstance(type, initialItems.size());
        this.items = initialItems.toArray(init);
        itemsSorted = (T[]) Arrays.copyOf(this.items, initialItems.size());
        Arrays.sort(itemsSorted);
        List<T> orig = new ArrayList<>(initialItems);
        ruleIndicesForSorted = new int[initialItems.size()];
        for (int i = 0; i < itemsSorted.length; i++) {
            ruleIndicesForSorted[i] = orig.indexOf(itemsSorted[i]);
        }
    }

    Set<T> toSet(BitSet bits) {
        if (indexedImpl == null) {
            indexedImpl = new IndexedImpl();
        }
        return new BitSetSet<T>(indexedImpl, bits);
    }

    Set<T> newSet() {
        return toSet(new BitSet(items.length));
    }

    private IndexedImpl indexedImpl;
    class IndexedImpl implements Indexed<T> {
        List<T> list = Arrays.asList(items);
        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public T get(int index) {
            return nameOf(index);
        }

        @Override
        public int size() {
            return items.length;
        }
    }

    public void walk(ObjectGraphVisitor<T> v) {
        tree.walk(new BitSetGraphVisitor() {
            @Override
            public void enterRule(int ruleId, int depth) {
                v.enterNode(nameOf(ruleId), depth);
            }

            @Override
            public void exitRule(int ruleId, int depth) {
                v.exitNode(nameOf(ruleId), depth);
            }
        });
    }

    public void walk(T start, ObjectGraphVisitor<T> v) {
        int ix = indexOf(start);
        if (ix < 0) {
            return;
        }
        tree.walk(ix, new BitSetGraphVisitor() {
            @Override
            public void enterRule(int ruleId, int depth) {
                v.enterNode(nameOf(ruleId), depth);
            }

            @Override
            public void exitRule(int ruleId, int depth) {
                v.exitNode(nameOf(ruleId), depth);
            }
        });
    }

    public void walkUpwards(T start, ObjectGraphVisitor<T> v) {
        int ix = indexOf(start);
        if (ix < 0) {
            return;
        }
        tree.walkUpwards(ix, new BitSetGraphVisitor() {
            @Override
            public void enterRule(int ruleId, int depth) {
                v.enterNode(nameOf(ruleId), depth);
            }

            @Override
            public void exitRule(int ruleId, int depth) {
                v.exitNode(nameOf(ruleId), depth);
            }
        });
    }

    @Override
    public int distance(T a, T b) {
        int ixA = indexOf(a);
        int ixB = indexOf(b);
        if (ixA < 0 || ixB < 0) {
            return Integer.MAX_VALUE;
        }
        return tree.distance(ixA, ixB);
    }

    public Set<T> disjointItems() {
        Set<Integer> all = tree.disjointItems();
        Set<T> result = new HashSet<>();
        for (int a : all) {
            result.add(nameOf(a));
        }
        return result;
    }

    public Set<T> disjunctionOfClosureOfMostCentralNodes() {
        List<Score<T>> centrality = eigenvectorCentrality();
        double sum = 0.0;
        for (int i = 0; i < centrality.size() / 2; i++) {
            sum += centrality.get(i).score();
        }
        double avg = sum / (double) (centrality.size() / 2);
        Set<T> result = new HashSet<>(centrality.size());
        centrality.stream().filter(s -> {
            return s.score() >= avg;
        }).forEach(s -> {
            result.add(s.node());
        });
        return result;
    }

    public Set<T> disjunctionOfClosureOfHighestRankedNodes() {
        List<Score<T>> centrality = pageRank();
        double sum = 0.0;
        for (int i = 0; i < centrality.size() / 2; i++) {
            sum += centrality.get(i).score();
        }
        double avg = sum / (double) (centrality.size() / 2);
        Set<T> result = new HashSet<>(centrality.size());
        centrality.stream().filter(s -> {
            return s.score() >= avg;
        }).forEach(s -> {
            result.add(s.node());
        });
        return result;
    }

    @Override
    public List<Score<T>> eigenvectorCentrality() {
        double[] centrality = tree.eigenvectorCentrality(400, 0.000001, false, true, true);
        List<Score<T>> result = new ArrayList<>(centrality.length);
        for (int i = 0; i < centrality.length; i++) {
            result.add(new ScoreImpl<>(centrality[i], i, nameOf(i)));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<Score<T>> pageRank() {
        double[] centrality = tree.pageRank(0.0000000000000004, 0.00000000001, 1000, true);
        List<Score<T>> result = new ArrayList<>(centrality.length);
        for (int i = 0; i < centrality.length; i++) {
            result.add(new ScoreImpl<>(centrality[i], i, nameOf(i)));
        }
        Collections.sort(result);
        return result;
    }

    public List<T> byClosureSize() {
        int[] cs = tree.byClosureSize();
        List<T> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(nameOf(cs[i]));
        }
        return result;
    }

    public List<T> byReverseClosureSize() {
        int[] cs = tree.byReverseClosureSize();
        List<T> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(nameOf(cs[i]));
        }
        return result;
    }

    @Override
    public Set<T> parents(T node) {
        int ix = indexOf(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return collect(tree.parents(ix));
    }

    @Override
    public Set<T> children(T node) {
        int ix = indexOf(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return collect(tree.children(ix));
    }

    public Set<String> edgeStrings() {
        Set<String> result = new TreeSet<>();
        for (int[] pair : tree.edges()) {
            result.add(nameOf(pair[0]) + ":" + nameOf(pair[1]));
        }
        return result;
    }

    private int indexOf(T name) {
        int ix = Arrays.binarySearch(itemsSorted, name);
        if (ix < 0) {
            return -1;
        }
        return ruleIndicesForSorted[ix];
    }

    private T nameOf(int index) {
        return items[index];
    }

    private Set<T> collect(BitSet set) {
        int count = set.cardinality();
        if (count == 0) {
            return Collections.emptySet();
        }
//        Set<T> into = new HashSet<>(count);
//        collect(set, into);
//        return into;
        return toSet(set);
    }

    private void collect(BitSet set, Set<T> into) {
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            into.add(nameOf(bit));
        }
    }

    @Override
    public int inboundReferenceCount(T node) {
        int ix = indexOf(node);
        return ix < 0 ? 0 : tree.inboundReferenceCount(ix);
    }

    @Override
    public int outboundReferenceCount(T node) {
        int ix = indexOf(node);
        return ix < 0 ? 0 : tree.outboundReferenceCount(ix);
    }

    @Override
    public Set<T> topLevelOrOrphanNodes() {
        return collect(tree.topLevelOrOrphanNodes());
    }

    @Override
    public Set<T> bottomLevelNodes() {
        return collect(tree.bottomLevelNodes());
    }

    @Override
    public boolean isUnreferenced(T node) {
        int ix = indexOf(node);
        return ix < 0 ? true : tree.isUnreferenced(ix);
    }

    @Override
    public int closureSize(T node) {
        int ix = indexOf(node);
        return ix < 0 ? 0 : tree.closureSize(ix);
    }

    @Override
    public int reverseClosureSize(T node) {
        int ix = indexOf(node);
        return ix < 0 ? 0 : tree.reverseClosureSize(ix);
    }

    @Override
    public Set<T> reverseClosureOf(T node) {
        int ix = indexOf(node);
        return ix < 0 ? Collections.emptySet()
                : collect(tree.reverseClosureOf(ix));
    }

    @Override
    public Set<T> closureOf(T node) {
        int ix = indexOf(node);
        return ix < 0 ? Collections.emptySet()
                : collect(tree.closureOf(ix));
    }
}
