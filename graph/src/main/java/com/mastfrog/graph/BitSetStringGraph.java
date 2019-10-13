package com.mastfrog.graph;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import com.mastfrog.bits.Bits;
import com.mastfrog.bits.MutableBits;
import com.mastfrog.bits.collections.BitSetSet;
import com.mastfrog.graph.algorithm.Algorithm;
import com.mastfrog.graph.algorithm.RankingAlgorithm;
import com.mastfrog.graph.algorithm.Score;
import com.mastfrog.abstractions.list.IndexedResolvable;

/**
 *
 * @author Tim Boudreau
 */
final class BitSetStringGraph implements StringGraph {

    private final IntGraph tree;
    private final String[] items;
    private transient IndexedImpl indexedImpl;

    BitSetStringGraph(IntGraph tree, String[] sortedArray) {
        this.tree = tree;
        this.items = sortedArray;
    }

    static boolean sanityCheckArray(String[] sortedArray) {
        assert new HashSet<>(Arrays.asList(sortedArray)).size() == sortedArray.length :
                "Array contains duplicates: " + Arrays.toString(sortedArray);
        List<String> a = Arrays.asList(sortedArray);
        List<String> b = new ArrayList<>(a);
        Collections.sort(b);
        assert a.equals(b) : "Array is not sorted: " + a;
        return true;
    }

    Set<String> toSet(Bits bits) {
        if (indexedImpl == null) {
            indexedImpl = new IndexedImpl();
        }
        return new BitSetSet<>(indexedImpl, bits);
    }

    Set<String> newSet() {
        return toSet(MutableBits.create(items.length));
    }

    public List<ObjectPath<String>> pathsBetween(String a, String b) {
        int aix = toNodeId(a);
        int bix = toNodeId(b);
        List<IntPath> raw = tree.pathsBetween(aix, bix);
        List<ObjectPath<String>> result = new ArrayList<>(raw.size());
        for (IntPath ip : raw) {
            ObjectPath<String> op = new ObjectPath<String>(ip, indexedImpl);
            result.add(op);
        }
        return result;
    }

    public void save(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(items);
        tree.save(out);
    }

    static BitSetStringGraph load(ObjectInput in) throws IOException, ClassNotFoundException {
        int v = in.readInt();
        if (v != 1) {
            throw new IOException("Unsupoorted version " + v);
        }
        String[] sortedArray = (String[]) in.readObject();
        BitSetGraph tree = BitSetGraph.load(in);
        return new BitSetStringGraph(tree, sortedArray);
    }

    class IndexedImpl implements IndexedResolvable<String> {

        List<String> list = Arrays.asList(items);

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public String forIndex(int index) {
            return toNode(index);
        }

        @Override
        public int size() {
            return items.length;
        }
    }

    @Override
    public void walk(ObjectGraphVisitor<? super String> v) {
        tree.walk(new IntGraphVisitor() {
            @Override
            public void enterNode(int node, int depth) {
                v.enterNode(toNode(node), depth);
            }

            @Override
            public void exitNode(int node, int depth) {
                v.exitNode(toNode(node), depth);
            }
        });
    }

    public void walk(String start, ObjectGraphVisitor<? super String> v) {
        int ix = toNodeId(start);
        if (ix < 0) {
            return;
        }
        tree.walk(ix, new IntGraphVisitor() {
            @Override
            public void enterNode(int node, int depth) {
                v.enterNode(toNode(node), depth);
            }

            @Override
            public void exitNode(int node, int depth) {
                v.exitNode(toNode(node), depth);
            }
        });
    }

    public void walkUpwards(String start, ObjectGraphVisitor<? super String> v) {
        int ix = toNodeId(start);
        if (ix < 0) {
            return;
        }
        tree.walkUpwards(ix, new IntGraphVisitor() {
            @Override
            public void enterNode(int node, int depth) {
                v.enterNode(toNode(node), depth);
            }

            @Override
            public void exitNode(int node, int depth) {
                v.exitNode(toNode(node), depth);
            }
        });
    }

    public int distance(String a, String b) {
        int ixA = toNodeId(a);
        int ixB = toNodeId(b);
        if (ixA < 0 || ixB < 0) {
            return Integer.MAX_VALUE;
        }
        return tree.distance(ixA, ixB);
    }

    public Set<String> disjointItems() {
        Bits all = tree.disjointNodes();
        Set<String> result = new HashSet<>();
        all.forEachSetBitAscending((int i) -> {
            result.add(toNode(i));
        });
        return result;
    }

    public Set<String> disjunctionOfClosureOfMostCentralNodes() {
        List<Score<String>> centrality = eigenvectorCentrality();
        double sum = 0.0;
        for (int i = 0; i < centrality.size() / 2; i++) {
            sum += centrality.get(i).score();
        }
        double avg = sum / (double) (centrality.size() / 2);
        Set<String> result = new HashSet<>(centrality.size());
        centrality.stream().filter(s -> {
            return s.score() >= avg;
        }).forEach(s -> {
            result.add(s.node());
        });
        return result;
    }

    @Override
    public Set<String> disjunctionOfClosureOfHighestRankedNodes() {
        List<Score<String>> centrality = pageRank();
        double sum = 0.0;
        for (int i = 0; i < centrality.size() / 2; i++) {
            sum += centrality.get(i).score();
        }
        double avg = sum / (double) (centrality.size() / 2);
        Set<String> result = new HashSet<>(centrality.size());
        centrality.stream().filter(s -> {
            return s.score() >= avg;
        }).forEach(s -> {
            result.add(s.node());
        });
        return result;
    }

    @Override
    public List<Score<String>> eigenvectorCentrality() {
        return apply(Algorithm.eigenvectorCentrality());
    }

    @Override
    public List<Score<String>> pageRank() {
        return apply(Algorithm.pageRank());
    }

    public List<String> byClosureSize() {
        int[] cs = tree.byClosureSize();
        List<String> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(toNode(cs[i]));
        }
        return result;
    }

    public List<String> byReverseClosureSize() {
        int[] cs = tree.byReverseClosureSize();
        List<String> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(toNode(cs[i]));
        }
        return result;
    }

    public Set<String> parents(String node) {
        int ix = toNodeId(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return setOf(tree.parents(ix));
    }

    public Set<String> children(String node) {
        int ix = toNodeId(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return setOf(tree.children(ix));
    }

    public Set<String> edgeStrings() {
        Set<String> result = new TreeSet<>();
        tree.edges((a, b) -> {
            result.add(toNode(a) + ":" + toNode(b));
        });
        return result;
    }

    public int toNodeId(String name) {
        int ix = Arrays.binarySearch(items, name);
        if (ix < 0) {
            return -1;
        }
        return ix;
    }

    public String toNode(int index) {
        return items[index];
    }

    private Set<String> setOf(Bits set) {
        return set.isEmpty() ? Collections.emptySet() : toSet(set);
    }

    public int inboundReferenceCount(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : tree.inboundReferenceCount(ix);
    }

    public int outboundReferenceCount(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : tree.outboundReferenceCount(ix);
    }

    public Set<String> topLevelOrOrphanNodes() {
        return setOf(tree.topLevelOrOrphanNodes());
    }

    public Set<String> bottomLevelNodes() {
        return setOf(tree.bottomLevelNodes());
    }

    public boolean isUnreferenced(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? true : tree.isUnreferenced(ix);
    }

    public int closureSize(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : tree.closureSize(ix);
    }

    public int reverseClosureSize(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : tree.reverseClosureSize(ix);
    }

    public Set<String> reverseClosureOf(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? Collections.emptySet()
                : setOf(tree.reverseClosureOf(ix));
    }

    public Set<String> closureOf(String node) {
        int ix = toNodeId(node);
        return ix < 0 ? Collections.emptySet()
                : setOf(tree.closureOf(ix));
    }

    public boolean hasInboundEdge(String from, String to) {
        int f = toNodeId(from);
        int t = toNodeId(to);
        return f < 0 || t < 0 ? false : tree.hasInboundEdge(f, t);
    }

    public boolean hasOutboundEdge(String from, String to) {
        int f = toNodeId(from);
        int t = toNodeId(to);
        return f < 0 || t < 0 ? false : tree.hasOutboundEdge(f, t);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        walk((String node, int depth) -> {
            char[] c = new char[depth * 2];
            Arrays.fill(c, ' ');
            sb.append(c).append(node).append('\n');
        });
        sb.append("Tops:");
        topsString(sb);
        sb.append('\n').append("Bottoms: ");
        bottomsString(sb);
        return sb.toString();
    }

    private void topsString(StringBuilder into) {
        tree.topLevelOrOrphanNodes().forEachSetBitAscending((int i) -> {
            into.append(' ').append(toNode(i));
        });
    }

    private void bottomsString(StringBuilder into) {
        tree.bottomLevelNodes().forEachSetBitAscending((int i) -> {
            into.append(' ').append(toNode(i));
        });
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.tree);
        hash = 79 * hash + Arrays.deepHashCode(this.items);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BitSetStringGraph other = (BitSetStringGraph) obj;
        if (!Objects.equals(this.tree, other.tree)) {
            return false;
        }
        return Arrays.deepEquals(this.items, other.items);
    }

    @Override
    public List<Score<String>> apply(RankingAlgorithm<?> alg) {
        return alg.apply(tree, this::toNode);
    }
}
