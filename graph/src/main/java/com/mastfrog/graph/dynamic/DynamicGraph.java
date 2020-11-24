/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.graph.dynamic;

import com.mastfrog.abstractions.list.IndexedResolvable;
import com.mastfrog.function.throwing.io.IOFunction;
import com.mastfrog.function.throwing.io.IOToIntBiFunction;
import com.mastfrog.function.throwing.io.IOTriConsumer;
import com.mastfrog.graph.BitSetUtils;
import com.mastfrog.graph.IntGraph;
import com.mastfrog.graph.ObjectGraph;
import com.mastfrog.graph.ObjectGraphVisitor;
import com.mastfrog.graph.ObjectPath;
import com.mastfrog.graph.algorithm.RankingAlgorithm;
import com.mastfrog.graph.algorithm.Score;
import static com.mastfrog.util.preconditions.Checks.nonNegative;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.IOException;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * Implements ObjectGraph over a snapshot which can be recomputed when updated -
 * useful for implementing dynamic graphs whose contents change occasionally but
 * rarely. Can be stored to a file channel. Internally, uses a non-fair
 * read/write lock to manage thread-safe updates. Delegates to a graph instance
 * which is created on-demand and retained until the next modification.
 *
 * @author Tim Boudreau
 */
public class DynamicGraph<T> implements ObjectGraph<T> {

    private static int MAGIC = 713732;
    private final Set<T> contents;
    private final List<T> contentsList;
    private final List<BitSet> inboundReferences;
    private final List<BitSet> outboundReferences;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private int revision;
    private int hc;
    private ObjectGraph<T> graph;

    /**
     * Create a graph with the default initial capacity of 128.
     */
    public DynamicGraph() {
        this(128);
    }

    private DynamicGraph(LinkedHashSet<T> items, List<BitSet> inbound, List<BitSet> outbound) {
        this.contents = items;
        this.contentsList = new ArrayList<>(items);
        this.inboundReferences = inbound;
        this.outboundReferences = outbound;
        graph = createGraph();
    }

    /**
     * Create a graph with the passed initial capacity.
     *
     * @param targetSize
     */
    public DynamicGraph(int targetSize) {
        contents = new LinkedHashSet<>(nonNegative("targetSize", targetSize));
        contentsList = new ArrayList<>(targetSize);
        inboundReferences = new ArrayList<>(targetSize);
        outboundReferences = new ArrayList<>(targetSize);
    }

    @Override
    public void toIntGraph(BiConsumer<IndexedResolvable<? extends T>, IntGraph> consumer) {
        graph().toIntGraph(consumer);
    }

    @Override
    public ObjectGraph<T> omitting(Set<T> items) {
        return graph().omitting(items);
    }

    /**
     * Returns the number of items belonging to this graph.
     *
     * @return The count
     */
    public int size() {
        return contents.size();
    }

    /**
     * Determine if this graph contains the item passed.
     *
     * @param item The item
     * @return true if it is present
     */
    public boolean contains(T item) {
        lock.readLock().lock();
        try {
            return contents.contains(item);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void snapshot(BiConsumer<List<T>, List<BitSet>> c) {
        List<T> copy;
        List<BitSet> bits;
        lock.readLock().lock();
        try {
            copy = new ArrayList<>(contentsList);
            bits = new ArrayList<>(outboundReferences);
        } finally {
            lock.readLock().unlock();
        }
        c.accept(copy, bits);
    }

    private void snapshot(IOTriConsumer<List<T>, List<BitSet>, List<BitSet>> c) throws IOException {
        List<T> copy;
        List<BitSet> outbound;
        List<BitSet> inbound;
        lock.readLock().lock();
        try {
            copy = new ArrayList<>(contents);
            outbound = new ArrayList<>(outboundReferences);
            inbound = new ArrayList<>(inboundReferences);
        } finally {
            lock.readLock().unlock();
        }
        c.accept(copy, outbound, inbound);
    }

    void contentsChanged() {
        synchronized (this) {
            graph = null;
            revision++;
        }
    }

    /**
     * Get the number of changes that have been made to this graph since its
     * creation; typically used to determine if the graph has changed since some
     * computation was performed.
     *
     * @return The revision
     */
    public synchronized int revision() {
        return revision;
    }

    private void updateHashCode() {
        hc = outboundReferences.hashCode() + 73 * contentsList.hashCode();
    }

    /**
     * Clear the contents of the graph.
     */
    public void clear() { // for tests
        lock.writeLock().lock();
        try {
            contents.clear();
            contentsList.clear();
            inboundReferences.clear();
            outboundReferences.clear();
            hc = 0;
        } finally {
            lock.writeLock().unlock();
            contentsChanged();
        }
    }

    /**
     * Get the internal graph implementation we delegate to.
     *
     * @return The graph
     */
    private ObjectGraph<T> graph() {
        ObjectGraph<T> graphLocal = this.graph;
        if (graphLocal == null) {
            synchronized (this) {
                graphLocal = this.graph;
            }
            // Do graph creation outside the synchronized block
            // and, rarely, create the graph twice if two threads are in here.
            // Preferable to double-locking on
            // this and the read lock for liveness, and graph creation is
            // comparatively cheap
            if (graphLocal == null) {
                graphLocal = createGraph();
                synchronized (this) {
                    if (this.graph == null) {
                        this.graph = graphLocal;
                    } else {
                        graphLocal = this.graph;
                    }
                }
            }
        }
        return graphLocal;
    }

    /**
     * Set the outbound edges from an item, removing any edges that previously
     * existed which are not present in the passed set.
     *
     * @param item
     * @param dependencies
     * @return
     */
    public boolean setOutboundEdges(T item, Set<T> dependencies) {
        lock.writeLock().lock();
        boolean changed = false;
        try {
            ObjectGraph<T> graph = graph();
            Set<T> deps = graph.children(item);
            if (deps.equals(dependencies)) {
                return false;
            }
            Set<T> added = new HashSet<>(dependencies);
            added.removeAll(deps);
            Set<T> removed = new HashSet<>(deps);
            removed.removeAll(dependencies);
            int pathIndex = add(item);
            BitSet outbound = outboundReferences.get(pathIndex);
            for (T dep : removed) {
                int ix = contentsList.indexOf(dep);
                if (ix >= 0) {
                    changed = true;
                    outbound.clear(ix);
                    BitSet inbound = inboundReferences.get(ix);
                    inbound.clear(pathIndex);
                }
            }
            for (T dep : added) {
                int ix = add(dep);
                outbound.set(ix);
                BitSet inbound = inboundReferences.get(ix);
                inbound.set(pathIndex);
                changed = true;
            }
            return changed;
        } finally {
            lock.writeLock().unlock();
            if (changed) {
                contentsChanged();
            }
        }
    }

    /**
     * Ad an edge from one node to another. Self-edges are ignored.
     *
     * @param depender The first item
     * @param dependee The second item
     * @return True if a new edge was created
     */
    public boolean addEdge(T depender, T dependee) {
        if (notNull("depender", depender).equals(notNull("dependee", dependee))) {
            return false;
        }
        if (hasEdge(depender, dependee)) {
            return false;
        }
        lock.writeLock().lock();
        boolean added = false;
        try {
            added = _addEdge(depender, dependee);
        } finally {
            lock.writeLock().unlock();
            if (added) {
                contentsChanged();
            }
        }
        return added;
    }

    private boolean _addEdge(T depender, T dependee) {
        assert lock.writeLock().isHeldByCurrentThread() : "not locked";
        int dependerIndex = add(depender);
        int dependeeIndex = add(dependee);
        assert dependerIndex >= 0 : "state inconsistent " + depender
                + " in " + contentsList + " vs " + contents;
        assert dependeeIndex >= 0 : "state inconsistent " + dependee
                + " in " + contentsList + " vs " + contents;
        BitSet dependerOutbound = outboundReferences.get(dependerIndex);
        BitSet dependeeInbound = inboundReferences.get(dependeeIndex);
        boolean result = false;
        if (!dependerOutbound.get(dependeeIndex)) {
            dependerOutbound.set(dependeeIndex);
            result = true;
        }
        if (!dependeeInbound.get(dependerIndex)) {
            dependeeInbound.set(dependerIndex);
            result = true;
        }
        if (result) {
            updateHashCode();
        }
        return result;
    }

    /**
     * Remove an item and all edges connected to it from the graph.
     *
     * @param item An item
     * @return true if the graph was changed
     */
    public boolean removeAllReferencesTo(T item) {
        notNull("item", item);
        lock.readLock().lock();
        try {
            if (!contents.contains(item)) {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        boolean changed = false;
        try {
            int itemIndex = contentsList.indexOf(item);
            if (itemIndex < 0) {
                // another thread got here first
                return false;
            }
            changed = true;
            contents.remove(item);
            contentsList.remove(itemIndex);
            outboundReferences.remove(itemIndex);
            inboundReferences.remove(itemIndex);
            for (BitSet set : inboundReferences) {
                set.clear(itemIndex);
                for (int bit = set.nextSetBit(itemIndex + 1); bit >= 0; bit = set.nextSetBit(bit + 1)) {
                    set.clear(bit);
                    set.set(bit - 1);
                }
            }
            for (BitSet set : outboundReferences) {
                set.clear(itemIndex);
                for (int bit = set.nextSetBit(itemIndex + 1); bit >= 0; bit = set.nextSetBit(bit + 1)) {
                    set.clear(bit);
                    set.set(bit - 1);
                }
            }
            updateHashCode();
        } finally {
            lock.writeLock().unlock();
            if (changed) {
                contentsChanged();
            }
        }
        return true;
    }

    /**
     * Remove an edge between two items.
     *
     * @param depender The item with the outbound edge
     * @param dependee The item with the inbound edge
     * @return true if the graph changed
     */
    public boolean removeEdge(T depender, T dependee) {
        if (notNull("depender", depender).equals(notNull("dependee", dependee))) {
            return false;
        }
        if (!hasEdge(depender, dependee)) {
            return false;
        }
        lock.writeLock().lock();
        boolean changed = false;
        try {
            int dependerIndex = contentsList.indexOf(depender);
            int dependeeIndex = contentsList.indexOf(dependee);
            if (dependerIndex < 0 || dependeeIndex < 0) {
                // another thread got here before us
                return false;
            }
            BitSet dependerOutbound = outboundReferences.get(dependerIndex);
            BitSet dependeeInbound = inboundReferences.get(dependeeIndex);
            changed = dependerOutbound.get(dependeeIndex)
                    || dependeeInbound.get(dependerIndex);
            if (changed) {
                dependerOutbound.clear(dependeeIndex);
                dependeeInbound.clear(dependerIndex);
            }
        } finally {
            lock.writeLock().unlock();
            if (changed) {
                contentsChanged();
            }
        }
        return changed;
    }

    /**
     * Determine if an outbound edge is present from depender to dependee.
     *
     * @param depender One node
     * @param dependee Another node
     * @return whether the edge exists
     */
    public boolean hasEdge(T depender, T dependee) {
        lock.readLock().lock();
        try {
            if (contents.contains(depender) && contents.contains(dependee)) {
                int dependerIndex = contentsList.indexOf(depender);
                int dependeeIndex = contentsList.indexOf(dependee);
                assert dependerIndex >= 0 : "state inconsistent " + depender
                        + " in " + contentsList + " vs " + contents;
                assert dependeeIndex >= 0 : "state inconsistent " + dependee
                        + " in " + contentsList + " vs " + contents;
                BitSet dependerOutbound = outboundReferences.get(dependerIndex);
//                BitSet dependeeInbound = inboundReferences.get(dependeeIndex);
                updateHashCode();
                return dependerOutbound.get(dependeeIndex) /* && dependeeInbound.get(dependerIndex) */;
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    private int add(T path) {
        if (contents.add(path)) {
            assert lock.writeLock().isHeldByCurrentThread() : "not write locked";
            inboundReferences.add(new BitSet(contents.size()));
            outboundReferences.add(new BitSet(contents.size()));
            contentsList.add(path);
            contentsChanged();
            return contents.size() - 1;
        } else {
            return contentsList.indexOf(path);
        }
    }

    private ObjectGraph<T> createGraph() {
        List<T> localContents;
        BitSet[] inbound;
        BitSet[] outbound;
        lock.readLock().lock();
        try {
            localContents = new ArrayList<>(this.contents);
            int size = localContents.size();
            inbound = new BitSet[size];
            outbound = new BitSet[size];
            for (int i = 0; i < size; i++) {
                inbound[i] = BitSetUtils.copyOf(inboundReferences.get(i));
                outbound[i] = BitSetUtils.copyOf(outboundReferences.get(i));
            }
        } finally {
            lock.readLock().unlock();
        }
        assert inbound.length == localContents.size();
        assert outbound.length == localContents.size();
        IntGraph ig = IntGraph.create(inbound, outbound);
        ObjectGraph<T> pathGraph = ig.toObjectGraph(Collections.unmodifiableList(localContents));
        return pathGraph;
    }

    // delegating implementation of ObjectGraph below here:
    @Override
    public List<T> byClosureSize() {
        return graph().byClosureSize();
    }

    @Override
    public List<T> byReverseClosureSize() {
        return graph().byReverseClosureSize();
    }

    @Override
    public Set<String> edgeStrings() {
        return graph().edgeStrings();
    }

    @Override
    public Set<T> parents(T node) {
        return graph().parents(node);
    }

    @Override
    public Set<T> children(T node) {
        return graph().children(node);
    }

    @Override
    public int inboundReferenceCount(T node) {
        return graph().inboundReferenceCount(node);
    }

    @Override
    public int outboundReferenceCount(T node) {
        return graph().outboundReferenceCount(node);
    }

    @Override
    public Set<T> topLevelOrOrphanNodes() {
        return graph().topLevelOrOrphanNodes();
    }

    @Override
    public Set<T> bottomLevelNodes() {
        return graph().bottomLevelNodes();
    }

    @Override
    public boolean isUnreferenced(T node) {
        return graph().isUnreferenced(node);
    }

    @Override
    public int closureSize(T node) {
        return graph().closureSize(node);
    }

    @Override
    public int reverseClosureSize(T node) {
        return graph().reverseClosureSize(node);
    }

    @Override
    public Set<T> reverseClosureOf(T node) {
        return graph().reverseClosureOf(node);
    }

    @Override
    public Set<T> closureOf(T node) {
        return graph().closureOf(node);
    }

    @Override
    public void walk(ObjectGraphVisitor<? super T> v) {
        graph().walk(v);
    }

    @Override
    public void walk(T startingWith, ObjectGraphVisitor<? super T> v) {
        graph().walk(startingWith, v);
    }

    @Override
    public void walkUpwards(T startingWith, ObjectGraphVisitor<? super T> v) {
        graph().walkUpwards(startingWith, v);
    }

    @Override
    public int distance(T a, T b) {
        return graph().distance(a, b);
    }

    @Override
    public List<Score<T>> eigenvectorCentrality() {
        return graph().eigenvectorCentrality();
    }

    @Override
    public List<Score<T>> pageRank() {
        return graph().pageRank();
    }

    @Override
    public Set<T> disjunctionOfClosureOfHighestRankedNodes() {
        return graph().disjunctionOfClosureOfHighestRankedNodes();
    }

    @Override
    public void save(ObjectOutput out) throws IOException {
        graph().save(out);
    }

    @Override
    public int toNodeId(T name) {
        return graph().toNodeId(name);
    }

    @Override
    public T toNode(int index) {
        return graph().toNode(index);
    }

    @Override
    public List<Score<T>> apply(RankingAlgorithm<?> alg) {
        return graph().apply(alg);
    }

    @Override
    public List<ObjectPath<T>> pathsBetween(T a, T b) {
        return graph().pathsBetween(a, b);
    }

    @Override
    public List<T> topologicalSort(Set<T> items) {
        return graph().topologicalSort(items);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof DynamicGraph<?>) {
            DynamicGraph<?> g1 = (DynamicGraph<?>) obj;
            boolean[] result = new boolean[1];
            snapshot((cts1, items1) -> {
                g1.snapshot((cts2, items2) -> {
                    result[0] = cts1.equals(cts2)
                            && items1.equals(items2);
                });
            });
            return result[0];
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        snapshot((items, outs) -> {
            int sz = items.size();
            for (int i = 0; i < sz; i++) {
                T t = items.get(i);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(t).append(":{");
                BitSet out = outs.get(i);
                BitSetUtils.forEach(out, ix -> {
                    sb.append(items.get(ix)).append(',');
                    // will leave a trailing comma.  Oh well.
                });
                sb.append('}');
            }
        });
        return sb.toString();
    }

    /**
     * Load a graph from a FileChannel or similar.
     *
     * @param <C> The channel type
     * @param <T> The item type
     * @param channel The channel
     * @param func A function which can read one element of type T from
     * a ByteBuffer, leaving it positioned at the first byte subsequent to
     * the item
     * @return A graph
     * @throws IOException If something goes wrong
     */
    public static <C extends WritableByteChannel & SeekableByteChannel, T> DynamicGraph<T> load(C channel,
            IOFunction<ByteBuffer, T> func) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 2);
        channel.read(buffer);
        buffer.flip();
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid magic number, expected "
                    + Integer.toHexString(MAGIC) + " got "
                    + Integer.toHexString(magic));
        }
        int recordSize = buffer.getInt();
        if (recordSize < 0 || recordSize > 65536 * 16) {
            throw new IOException("Absurdly large size - file probably corrupt: " + recordSize);
        }
        buffer = ByteBuffer.allocate(recordSize);
        int readBytes = channel.read(buffer);
        buffer.flip();

        if (readBytes != recordSize) {
            throw new IOException("Should have read " + recordSize + " bytes but got " + readBytes);
        }
        int recordCount = buffer.getInt();
        if (recordCount < 0 || recordCount > 8192) {
            throw new IOException("Absurd record count " + recordCount + " file is probably corrupted");
        }
        LinkedHashSet<T> contents = new LinkedHashSet<>(recordCount);
        List<BitSet> inboundReferences = new ArrayList<>(recordCount);
        List<BitSet> outboundReferences = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            // We could only save one list of bit sets and save space,
            // but reconstructing the opposite set is O^N.
            T obj = func.apply(buffer);
            BitSet outs = readBitSet(buffer, 65535);
            BitSet ins = readBitSet(buffer, 65535);

            contents.add(obj);
            outboundReferences.add(outs);
            inboundReferences.add(ins);
        }
        return new DynamicGraph<>(contents, inboundReferences, outboundReferences);
    }

    /**
     * Store this graph to a FileChannel or similar.
     *
     * @param <C> The channel type
     * @param channel The channel
     * @param itemWriter A function which can write one item of type T to
     * a channel, leaving the channel's position immediately after the last
     * byte written, and returning the exact number of bytes the function
     * wrote (typical is to serialize the item to a byte array, write the
     * length and then the item bytes)
     * @throws IOException If something goes wrong
     */
    public <C extends WritableByteChannel & SeekableByteChannel> void store(C channel, IOToIntBiFunction<T, C> itemWriter) throws IOException {
        snapshot((items, outbound, inbound) -> {
            ByteBuffer numBuffer = ByteBuffer.allocate(Integer.BYTES);
            writeNumber(MAGIC, channel, numBuffer);
            long sizePosition = channel.position();
            writeNumber(0, channel, numBuffer);
            int written = 0;
            int size = items.size();
            written += writeNumber(items.size(), channel, numBuffer);
            for (int i = 0; i < size; i++) {
                written += itemWriter.applyAsInt(items.get(i), channel);
                BitSet set = outbound.get(i);
                written += writeBitSet(set, channel, numBuffer);
                set = inbound.get(i);
                written += writeBitSet(set, channel, numBuffer);
            }
            long currPosition = channel.position();
            try {
                channel.position(sizePosition);
                writeNumber(written, channel, numBuffer);
            } finally {
                channel.position(currPosition);
            }
        });
    }

    private static <T extends WritableByteChannel & SeekableByteChannel> int writeNumber(int num, T channel, ByteBuffer numberBuf) throws IOException {
        numberBuf.putInt(num);
        numberBuf.flip();
        int result = channel.write(numberBuf);
        numberBuf.rewind();
        return result;
    }

    public static BitSet readBitSet(ByteBuffer buffer, int limit) throws IOException {
        int byteCount = buffer.getInt();
        if (byteCount < 0 || byteCount > limit) {
            throw new IOException("Absurd byte count for bitset: "
                    + byteCount + " at " + (buffer.position() - Integer.BYTES));
        }
        byte[] bts = new byte[byteCount];
        buffer.get(bts);
        BitSet outs = BitSet.valueOf(bts);
        return outs;
    }

    public static <C extends WritableByteChannel & SeekableByteChannel> int writeBitSet(BitSet set, C channel, ByteBuffer numBuffer) throws IOException {
        byte[] bts = set.toByteArray();
        int written = writeNumber(bts.length, channel, numBuffer);
        if (bts.length > 0) {
            written += channel.write(ByteBuffer.wrap(bts));
        }
        return written;
    }
}
