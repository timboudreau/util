package com.mastfrog.graph;

import java.util.Arrays;
import com.mastfrog.bits.Bits;
import com.mastfrog.bits.MutableBits;

/**
 * Builder for graphs.
 */
public final class IntGraphBuilder {

    private MutableBits[] outboundEdges;
    private MutableBits[] inboundEdges;
    private static final int INITIAL_SIZE = 32;
    private int greatestUsed = 0;

    IntGraphBuilder() {
        this(INITIAL_SIZE);
    }

    IntGraphBuilder(int initialSize) {
        outboundEdges = new MutableBits[initialSize];
        inboundEdges = new MutableBits[initialSize];
        for (int i = 0; i < initialSize; i++) {
            outboundEdges[i] = MutableBits.create(initialSize);
            inboundEdges[i] = MutableBits.create(initialSize);
        }
    }

    private void ensureSize(int newIndex) {
        if (newIndex > outboundEdges.length) {
            int newSize = (((newIndex / INITIAL_SIZE) + 1) * INITIAL_SIZE) + ((newIndex % INITIAL_SIZE) + 1) + INITIAL_SIZE;
            MutableBits[] newOut = Arrays.copyOf(outboundEdges, newSize);
            MutableBits[] newIn = Arrays.copyOf(inboundEdges, newSize);
            for (int i = greatestUsed; i < newSize; i++) {
                assert newIn[i] == null : "Clobbering in " + i;
                assert newOut[i] == null : "Clobbering out " + i;
                newIn[i] = MutableBits.create(newSize);
                newOut[i] = MutableBits.create(newSize);
            }
            outboundEdges = newOut;
            inboundEdges = newIn;
        }
        greatestUsed = Math.max(greatestUsed, newIndex);
    }

    public IntGraphBuilder addEdges(int[][] items) {
        for (int[] edge : items) {
            assert edge.length == 2 : "sub-array size must be 2 not " + edge.length;
            addEdge(edge[0], edge[1]);
        }
        return this;
    }

    public IntGraphBuilder addOrphan(int node) {
        ensureSize(node + 1);
        return this;
    }

    public IntGraphBuilder addEdge(int a, int b) {
        ensureSize(Math.max(a, b) + 1);
        outboundEdges[a].set(b);
        inboundEdges[b].set(a);
        return this;
    }

    public IntGraph build() {
        Bits[] ins = new Bits[greatestUsed];
        for (int i = 0; i < greatestUsed; i++) {
            ins[i] = inboundEdges[i].isEmpty() ? Bits.EMPTY : inboundEdges[i].readOnlyView();
        }
        Bits[] outs = new Bits[greatestUsed];
        for (int i = 0; i < greatestUsed; i++) {
            outs[i] = outboundEdges[i].isEmpty() ? Bits.EMPTY : outboundEdges[i].readOnlyView();
        }
        return new BitSetGraph(outs, ins);
    }
}
