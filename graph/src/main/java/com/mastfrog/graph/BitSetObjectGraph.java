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
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import com.mastfrog.bits.Bits;
import com.mastfrog.bits.MutableBits;
import com.mastfrog.bits.collections.BitSetSet;
import com.mastfrog.graph.algorithm.Algorithm;
import com.mastfrog.graph.algorithm.RankingAlgorithm;
import com.mastfrog.graph.algorithm.Score;
import com.mastfrog.abstractions.list.IndexedResolvable;
import java.util.BitSet;
import java.util.function.BiConsumer;

/**
 *
 * @author Tim Boudreau
 */
class BitSetObjectGraph<T> implements ObjectGraph<T> {

    private final IntGraph graph;
    private final IndexedResolvable<? extends T> indexed;

    BitSetObjectGraph(IntGraph graph, T[] sortedArray) {
        this(graph, new IndexedImpl<>(sortedArray));
    }

    BitSetObjectGraph(IntGraph graph, IndexedResolvable<? extends T> indexedImpl) {
        this.graph = graph;
        this.indexed = indexedImpl;
    }

    BitSetObjectGraph(IntGraph graph, int size, ToIntFunction<Object> toId, IntFunction<T> toObject) {
        this.graph = graph;
        this.indexed = new FIndexed<>(size, toId, toObject);
    }

    @Override
    public void toIntGraph(BiConsumer<IndexedResolvable<? extends T>, IntGraph> consumer) {
        consumer.accept(indexed, graph);
    }

    public List<ObjectPath<T>> pathsBetween(T a, T b) {
        int aix = toNodeId(a);
        int bix = toNodeId(b);
        if (aix < 0 || bix < 0) {
            return Collections.emptyList();
        }
        List<IntPath> raw = graph.pathsBetween(aix, bix);
        List<ObjectPath<T>> result = new ArrayList<>(raw.size());
        for (IntPath ip : raw) {
            ObjectPath<T> op = new ObjectPath<>(ip, indexed);
            result.add(op);
        }
        return result;
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

    Set<T> toSet(Bits bits) {
        return new BitSetSet<>(indexed, bits);
    }

    Set<T> newSet() {
        return toSet(MutableBits.create(indexed.size()));
    }

    public void save(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(indexed);
        graph.save(out);
    }

    @SuppressWarnings({"unchecked", "rawType"})
    static BitSetObjectGraph load(ObjectInput in) throws IOException, ClassNotFoundException {
        int v = in.readInt();
        if (v != 1) {
            throw new IOException("Unsupoorted version " + v);
        }
        String[] sortedArray = (String[]) in.readObject();
        BitSetGraph tree = BitSetGraph.load(in);
        return new BitSetObjectGraph(tree, sortedArray);
    }

    static class IndexedImpl<T> implements IndexedResolvable<T> {

        private final T[] sortedItems;

        IndexedImpl(T[] sortedItems) {
            this.sortedItems = sortedItems;
        }

        @Override
        public int indexOf(Object o) {
            return Arrays.binarySearch(sortedItems, o);
        }

        @Override
        public T forIndex(int index) {
            return sortedItems[index];
        }

        @Override
        public int size() {
            return sortedItems.length;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(sortedItems);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof IndexedImpl && Arrays.equals(((IndexedImpl) o).sortedItems, sortedItems);
        }
    }

    static final class FIndexed<T> implements IndexedResolvable<T> {

        private final int size;
        private final ToIntFunction<Object> toId;
        private final IntFunction<T> toObject;

        FIndexed(int size, ToIntFunction<Object> toId, IntFunction<T> toObject) {
            this.size = size;
            this.toId = toId;
            this.toObject = toObject;
        }

        @Override
        public int indexOf(Object o) {
            return toId.applyAsInt(o);
        }

        @Override
        public T forIndex(int index) {
            return toObject.apply(index);
        }

        @Override
        public int size() {
            return size;
        }
    }

    @Override
    public void walk(ObjectGraphVisitor<? super T> v) {
        graph.walk(new IntGraphVisitor() {
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

    public void walk(T start, ObjectGraphVisitor<? super T> v) {
        int ix = toNodeId(start);
        if (ix < 0) {
            return;
        }
        graph.walk(ix, new IntGraphVisitor() {
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

    @Override
    public void walkUpwards(T start, ObjectGraphVisitor<? super T> v) {
        int ix = toNodeId(start);
        if (ix < 0) {
            return;
        }
        graph.walkUpwards(ix, new IntGraphVisitor() {
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

    @Override
    public int distance(T a, T b) {
        int ixA = toNodeId(a);
        int ixB = toNodeId(b);
        if (ixA < 0 || ixB < 0) {
            return Integer.MAX_VALUE;
        }
        return graph.distance(ixA, ixB);
    }

    public Set<T> disjointItems() {
        Bits all = graph.disjointNodes();
        return new BitSetSet<>(indexed, all);
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

    @Override
    public Set<T> disjunctionOfClosureOfHighestRankedNodes() {
        double[] items = Algorithm.pageRank().apply(graph);
        double sum = 0.0;
        for (int i = 0; i < items.length; i++) {
            sum += items[i];
        }
        double avg = sum / (double) (items.length / 2);
        MutableBits bits = MutableBits.create(graph.size());
        for (int i = 0; i < items.length; i++) {
            if (items[i] >= avg) {
                bits.set(i);
            }
        }
        return new BitSetSet<>(indexed, bits);
    }

    @Override
    public List<Score<T>> eigenvectorCentrality() {
        return apply(Algorithm.eigenvectorCentrality());
    }

    @Override
    public List<Score<T>> pageRank() {
        return apply(Algorithm.pageRank());
    }

    @Override
    public List<T> byClosureSize() {
        int[] cs = graph.byClosureSize();
        List<T> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(toNode(cs[i]));
        }
        return result;
    }

    @Override
    public List<T> byReverseClosureSize() {
        int[] cs = graph.byReverseClosureSize();
        List<T> result = new ArrayList<>(cs.length);
        for (int i = 0; i < cs.length; i++) {
            result.add(toNode(cs[i]));
        }
        return result;
    }

    @Override
    public Set<T> parents(T node) {
        int ix = toNodeId(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return setOf(graph.parents(ix));
    }

    @Override
    public Set<T> children(T node) {
        int ix = toNodeId(node);
        if (ix == -1) {
            return Collections.emptySet();
        }
        return setOf(graph.children(ix));
    }

    @Override
    public Set<String> edgeStrings() {
        Set<String> result = new TreeSet<>();
        graph.edges((a, b) -> {
            result.add(toNode(a) + ":" + toNode(b));
        });
        return result;
    }

    @Override
    public int toNodeId(T name) {
        int ix = indexed.indexOf(name);
        if (ix < 0) {
            return -1;
        }
        return ix;
    }

    @Override
    public T toNode(int index) {
        return indexed.forIndex(index);
    }

    private Set<T> setOf(Bits set) {
        return set.isEmpty() ? Collections.emptySet() : toSet(set);
    }

    @Override
    public int inboundReferenceCount(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : graph.inboundReferenceCount(ix);
    }

    @Override
    public int outboundReferenceCount(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : graph.outboundReferenceCount(ix);
    }

    @Override
    public Set<T> topLevelOrOrphanNodes() {
        return setOf(graph.topLevelOrOrphanNodes());
    }

    @Override
    public Set<T> bottomLevelNodes() {
        return setOf(graph.bottomLevelNodes());
    }

    @Override
    public boolean isUnreferenced(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? true : graph.isUnreferenced(ix);
    }

    @Override
    public int closureSize(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : graph.closureSize(ix);
    }

    @Override
    public int reverseClosureSize(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? 0 : graph.reverseClosureSize(ix);
    }

    @Override
    public Set<T> reverseClosureOf(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? Collections.emptySet()
                : setOf(graph.reverseClosureOf(ix));
    }

    @Override
    public Set<T> closureOf(T node) {
        int ix = toNodeId(node);
        return ix < 0 ? Collections.emptySet()
                : setOf(graph.closureOf(ix));
    }

    public boolean hasInboundEdge(T from, T to) {
        int f = toNodeId(from);
        int t = toNodeId(to);
        return f < 0 || t < 0 ? false : graph.hasInboundEdge(f, t);
    }

    public boolean hasOutboundEdge(T from, T to) {
        int f = toNodeId(from);
        int t = toNodeId(to);
        return f < 0 || t < 0 ? false : graph.hasOutboundEdge(f, t);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        walk((T node, int depth) -> {
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
        graph.topLevelOrOrphanNodes().forEachSetBitAscending((int i) -> {
            into.append(' ').append(toNode(i));
        });
    }

    private void bottomsString(StringBuilder into) {
        graph.bottomLevelNodes().forEachSetBitAscending((int i) -> {
            into.append(' ').append(toNode(i));
        });
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.graph);
        hash = 79 * hash + indexed.hashCode();
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
        final BitSetObjectGraph other = (BitSetObjectGraph) obj;
        if (!Objects.equals(this.graph, other.graph)) {
            return false;
        }
        return indexed.equals(other.indexed);
    }

    @Override
    public List<Score<T>> apply(RankingAlgorithm<?> alg) {
        return alg.apply(graph, this::toNode);
    }

    @Override
    public ObjectGraph<T> omitting(Set<T> items) {
        int total = 0;
        int[] indices = new int[items.size()];
        for (T item : items) {
            int ix = indexed.indexOf(item);
            if (ix >= 0) {
                indices[total++] = ix;
            }
        }
        if (total < items.size()) {
            indices = Arrays.copyOf(indices, total);
        }
        List<T> newItems = new ArrayList<>(indexed.toList());
        newItems.removeAll(items);
        return new BitSetObjectGraph<>(graph.omitting(indices),
                IndexedResolvable.forList(newItems));
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public List<T> topologicalSort(Set<T> items) {
        BitSet set = new BitSet(size());
        for (T item : items) {
            int ix = toNodeId(item);
            set.set(ix);
        }
        IntPath path = graph.topologicalSort(set);
        List<T> result = new ArrayList<>(path.size());
        path.forEachInt(ix -> {
            result.add(toNode(ix));
        });
        return result;
    }
}
