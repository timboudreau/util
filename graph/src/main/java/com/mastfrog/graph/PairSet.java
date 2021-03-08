package com.mastfrog.graph;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.abstractions.list.IndexedResolvable;

/**
 * Tracks a set of pairs of coordinates in a coordinate space that have been
 * visited.
 *
 * @author Tim Boudreau
 */
public final class PairSet implements Iterable<int[]> {

    private final int size;
    private final BitSet set;

    PairSet(int size) {
        set = new BitSet(size * size);
        this.size = size;
    }

    private PairSet(int size, BitSet set) {
        this.size = size;
        this.set = set;
    }

    public static PairSet fromIntArray(int[][] ints) {
        PairSet set = new PairSet(ints.length);
        for (int[] pair : ints) {
            set.add(pair[0], pair[1]);
        }
        return set;
    }

    public static PairSet create(int graphSize) {
        return new PairSet(graphSize);
    }

    public boolean isEmpty() {
        return set.cardinality() == 0;
    }

    public int pairCount() {
        return set.cardinality();
    }

    public PairSet copy() {
        return new PairSet(size, (BitSet) set.clone());
    }

    public PairSet inverse() {
        return new PairSet(size, BitSetUtils.invert(set, size));
    }

    public int size() {
        return size;
    }

    public PairSet add(int x, int y) {
        set.set(positionOf(x, y));
        return this;
    }

    public PairSet remove(int x, int y) {
        set.clear(positionOf(x, y));
        return this;
    }

    public PairSet retainAll(PairSet other) {
        BitSet nue = (BitSet) set.clone();
        nue.and(other.set);
        return new PairSet(size, nue);
    }

    public PairSet removingAll(PairSet other) {
        BitSet nue = (BitSet) set.clone();
        nue.andNot(other.set);
        return new PairSet(size, nue);
    }

    public boolean intersects(PairSet other) {
        return set.intersects(other.set);
    }

    public boolean contains(int x, int y) {
        return set.get(positionOf(x, y));
    }

    int positionOf(int x, int y) {
        int result = x + (y * size);
        return result;
    }

    int[] coordinatesOf(int position) {
        int x = position % size;
        int y = position / size;
        return new int[]{x, y};
    }

    @Override
    public Iterator<int[]> iterator() {
        return new Iter();
    }

    public IntGraph toGraph() {
        IntGraphBuilder bldr = IntGraph.builder(size);
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            int x = bit % size;
            int y = bit / size;
            bldr.addEdge(x, y);
        }
        return bldr.build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            int x = bit % size;
            int y = bit / size;
            sb.append(x).append(',').append(y);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + this.size;
        hash = 71 * hash + Objects.hashCode(this.set);
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
        final PairSet other = (PairSet) obj;
        if (this.size != other.size) {
            return false;
        }
        return Objects.equals(this.set, other.set);
    }

    public <T> ObjectPairSet<T> toObjectPairSet(IndexedResolvable<T> indexed) {
        return new ObjectPairSet<>(this, indexed);
    }

    public int forEach(IntBiConsumer bi) {
        int count = 0;
        for (int position = set.nextSetBit(0); position >= 0; position = set.nextSetBit(position + 1)) {
            int x = position % size;
            int y = position / size;
            bi.accept(x, y);
            count++;
        }
        return count;
    }

    final class Iter implements Iterator<int[]> {

        int bit;
        boolean done;

        Iter() {
            findNext();
        }

        void findNext() {
            bit = set.nextSetBit(bit);
            done = bit < 0;
        }

        @Override
        public boolean hasNext() {
            findNext();
            return !done && bit >= 0;
        }

        @Override
        public int[] next() {
            if (bit < 0) {
                throw new NoSuchElementException();
            }
            findNext();
            int[] result = coordinatesOf(bit);
            bit++;
            return result;
        }
    }

    public static final class ObjectPairSet<T> implements Iterable<Map.Entry<T, T>> {

        private final PairSet pairs;
        private final IndexedResolvable<T> indexed;

        ObjectPairSet(PairSet pairs, IndexedResolvable<T> indexed) {
            this.pairs = pairs;
            this.indexed = indexed;
        }

        public int size() {
            return pairs.pairCount();
        }

        public boolean contains(T a, T b) {
            int aix = indexed.indexOf(a);
            int bix = indexed.indexOf(b);
            return pairs.contains(aix, bix);
        }

        @Override
        public Iterator<Map.Entry<T, T>> iterator() {
            return new OIter();
        }

        class OIter implements Iterator<Map.Entry<T, T>> {

            Iterator<int[]> iter = pairs.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Map.Entry<T, T> next() {
                int[] pair = iter.next();
                T a = indexed.forIndex(pair[0]);
                T b = indexed.forIndex(pair[1]);
                return new En<>(a, b);
            }

        }

        static final class En<T> implements Map.Entry<T, T> {

            private final T a;
            private final T b;

            En(T a, T b) {
                this.a = a;
                this.b = b;
            }

            @Override
            public T getKey() {
                return a;
            }

            @Override
            public T getValue() {
                return b;
            }

            @Override
            public T setValue(T value) {
                throw new UnsupportedOperationException("Read only.");
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 97 * hash + Objects.hashCode(this.a);
                hash = 97 * hash + Objects.hashCode(this.b);
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
                if (!(obj instanceof Map.Entry<?, ?>)) {
                    return false;
                }
                final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
                if (!Objects.equals(this.a, other.getKey())) {
                    return false;
                }
                return Objects.equals(this.b, other.getValue());
            }
        }
    }
}
