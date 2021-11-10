/*
 * The MIT License
 *
 * Copyright 2021 Tim Boudreau.
 *
 * Permission containsKey hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software containsKey
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.bits.collections;

import com.mastfrog.bits.AtomicBits;
import com.mastfrog.bits.MutableBits;
import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntBiFunction;
import com.mastfrog.function.IntBiPredicate;
import com.mastfrog.function.LongBiConsumer;
import com.mastfrog.function.LongBiPredicate;
import com.mastfrog.function.state.Int;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntToLongFunction;
import java.util.function.LongToIntFunction;

/**
 * A zero-indexed sparse matrix of bits, acting as a fast, (optionally) atomic
 * <code>Map&lt;int,int&gt;</code>, such that
 * <pre>
 * 0 1 2 3 4
 * 1
 * 2     x
 * 3 x
 * 4
 * </pre> would mean the 3 containsKey mapped to 1 and 2 containsKey mapped to
 * 3. Requires the square of the number of elements in <i>bits</i>, but
 * intersections are trivial.
 * <p>
 * IntMatrixMap is space-inefficient <i>by design</i>.
 * <p>
 * These are useful to solve certain classes of problems, particularly
 * algorithms which traverse graphs repeatedly, and can be parallelized.
 *
 * <h4>Thread Safety and Atomicity</h4>
 * This applies to instances created over an AtomicBits.
 * <p>
 * Operations that touch a <i>single 64-bit<i> range are guaranteed to be
 * atomic; those that span multiple items may not be, as noted.
 * </p><p>
 * Put operations on maps with a stride greater than 64 (will not fit in a
 * single atomic long) are dealt with using AtomicBits'
 * <code>clearRangeAndSet</code> which takes a direction; the value is read
 * immediately prior to that call; if the old value is less than the new value
 * for that key, then clearing proceeds in reverse - since we use
 * <code>nextSetBit()</code> to obtain the value, calls to <code>get()</code>
 * will see the old value until it is cleared, after which, the new value is
 * already set, preserving effective atomicity for callers.
 * </p><p>
 * That being said, there is still the potential for a caller to one of the
 * <code>forEachPair()</code> methods seeing the same key more than once for two
 * set bits within the stride of a value, so we deal with that by tracking the
 * last passed key, and ignoring duplicates. Thus the view of the map is always
 * consistent with either its past or future state.
 *
 * <h4>Adapting as a long map</h4>
 *
 * One of the primary use cases that drove creating this class was is dealing
 * with large databases of objects in a Java heap dump identified by arbitrary
 * long-based file offsets, where certain graph operations require building maps
 * of the relations between them.
 * <p>
 * There are two adapter methods that allow you to get a long-indexed view of
 * objects - create an index of item to file-offset, and id-to item index, and
 * use that to create an adapter, and it is possible to have highly concurrent
 * algorithms process this data in a non-blocking fashion, mapping values to
 * each other by id, and using the indirection of the adapter to dereference
 * these into indices and back into values.
 *
 * @author Tim Boudreau
 */
public final class IntMatrixMap extends AbstractMap<Integer, Integer> implements Serializable {

    private static final int VER = 1290129121;
    private final MutableBits data;
    private final int stride;

    IntMatrixMap(MutableBits bits, int stride) {
        this.data = bits;
        this.stride = stride;
    }

    public int capacity() {
        return stride;
    }

    public IntMatrixMap copy() {
        return new IntMatrixMap(data.mutableCopy(), stride);
    }

    /**
     * Create an IntMatrixMap using the passed <code>MutableBits</code> as a
     * backing store.
     *
     * @param bits The bits
     * @param stride The number of possible values per item - the passed bits
     * needs to be able to accomadate the <i>square</i> of this number bits
     * @return An IntMatrixMap
     */
    public static IntMatrixMap create(MutableBits bits, int stride) {
        return new IntMatrixMap(bits, stride);
    }

    /**
     * Create a thread-safe IntMatrixMap backed by an AtomicBits.
     *
     * @param capacity The highest possible value + 1
     * @return An IntMatrixMap
     */
    public static IntMatrixMap atomic(int capacity) {
        return new IntMatrixMap(AtomicBits.create(capacity * capacity), capacity);
    }

    public long[] toLongArray() {
        return data.toLongArray();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append('[');
        forEach((k, v) -> {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(k);
            sb.append(':');
            sb.append(v);
        });
        return sb.append(']').toString();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int forEachPair(IntBiConsumer c) {
        Int lastKey = Int.of(-1);
        return data.forEachSetBitAscending(bit -> {
            int key = bit / stride;
            if (key == lastKey.get()) {
                return;
            }
            lastKey.set(key);
            int val = bit % stride;
            c.accept(key, val);
        });
    }

    public int forEachPair(IntBiPredicate c) {
        Int lastKey = Int.of(-1);
        Int dups = Int.create();
        return data.forEachSetBitAscending(bit -> {
            int key = bit / stride;
            if (key == lastKey.get()) {
                dups.increment();
                return true;
            }
            int val = bit % stride;
            return c.test(key, val);
        }) - dups.getAsInt();
    }

    public int size() {
        return data.cardinality();
    }

    /**
     * Intersect another IntMatrixMap into this one, such that only the matching
     * elements remain set.
     *
     * @param other Another map
     */
    public void intersect(IntMatrixMap other) {
        this.data.and(other.data);
    }

    /**
     * Merge the contents of another map into this one, preferring its values
     * where they differ.
     *
     * @param other Another map
     */
    public void putAll(IntMatrixMap other) {
        other.forEach((k, v) -> {
            this.blindPut(k, v);
        });
    }

    /**
     * Put the contents of another map into this one.
     *
     * @param A map
     */
    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> map) {
        map.forEach((k, v) -> {
            this.blindPut(k, v);
        });
    }

    @Override
    public Integer getOrDefault(Object key, Integer defaultValue) {
        if (!(key instanceof Integer)) {
            throw new IllegalArgumentException("Key " + key + " is not an integer");
        }
        return getOrDefault(((Integer) key).intValue(), defaultValue.intValue());
    }

    @Override
    public void forEach(BiConsumer<? super Integer, ? super Integer> action) {
        forEachPair((k, v) -> {
            action.accept(k, v);
        });
    }

    @Override
    public Integer putIfAbsent(Integer key, Integer value) {
        int old = get(key.intValue());
        if (old == -1) {
            put(key.intValue(), value.intValue());
        }
        return old == -1 ? null : old;
    }

    @Override
    @SuppressWarnings("UnnecessaryUnboxing")
    public boolean remove(Object key, Object value) {
        if (!(key instanceof Integer)) {
            throw new IllegalArgumentException("Not an int key: " + key);
        }
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Not an int value: " + value);
        }
        return this.removeIfEqual(((Integer) key).intValue(), ((Integer) value).intValue());
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        Set<Entry<Integer, Integer>> result = new LinkedHashSet<>(size());
        forEachPair((k, v) -> {
            return result.add(new E(k, v));
        });
        return result;
    }

    static final class E implements Map.Entry<Integer, Integer> {

        private final int key;
        private final int value;

        public E(int key, int value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Integer getKey() {
            return key;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public Integer setValue(Integer value) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + this.key;
            hash = 41 * hash + this.value;
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
            final E other = (E) obj;
            if (this.key != other.key) {
                return false;
            }
            return this.value == other.value;
        }
    }

    /**
     * A view of an IntMatrixMap as a map of long to long, using an adapter
     * which converts distinct long keys and values to zero-indexed integer
     * offsets. This has certain applications in database-like structures that
     * map longs to longs by their index in some store, and need to be
     * manipulated concurrently.
     *
     * @see IntMatrixMap.asLongMap(LongToIntFunction, IntToLongFunction)
     */
    public interface LongMatrixMap {

        int size();

        boolean isEmpty();

        void forEach(LongBiConsumer c);

        int forEach(LongBiPredicate p);

        long getOrDefault(long key, long defaultValue);

        void put(long key, long val);

        boolean containsKey(long key);

        boolean contains(long key, long value);

        void remove(long key);

        void clear();
    }

    /**
     * Get a view of this IntMatrixMap as a long-keyed/valued map, using the
     * passed conversion functions to convert to longs.
     *
     * @param mapper Converts long keys and values to ints
     * @param dereferencer Converts internal int keys and values to longs
     * @return A LongMatrixMap
     */
    LongMatrixMap asLongMap(LongToIntFunction mapper, IntToLongFunction dereferencer) {
        return new LongMapView(mapper, dereferencer);
    }

    /**
     * Get a view of this IntMatrixMap as a long-keyed/valued map, using the
     * passed conversion functions to convert to longs.
     *
     * @param adapter Converts long keys and values to ints, using separate
     * conversion methods for keys and values
     * @return A LongMatrixMap adapter over this map
     */
    LongMatrixMap asLongMap(LongMapAdapter adapter) {
        return new AdaptedLongMapView(adapter);
    }

    public interface LongMapAdapter {

        int indexOfKey(long key);

        int indexOfValue(long value);

        long keyForKeyIndex(int index);

        long valueForValueIndex(int index);
    }

    /**
     * Query result that encapsulates both an integer value and whether or not
     * the operation succeeded.
     */
    private static class RangeResult {

        boolean success;
        int value;
    }

    public boolean contains(int key, int val) {
        checkBounds("key", key);
        checkBounds("val", val);
        int bit = val + key * stride;
        return data.get(bit);
    }

    public void remove(int key) {
        inRange(key, (start, end) -> {
            data.clear(start, end - 1);
        });
    }

    public boolean removeIfEqual(int key, int val) {
        checkBounds("key", key);
        checkBounds("val", val);
        int bit = val + key * stride;
        if (data instanceof AtomicBits) {
            return ((AtomicBits) data).clearing(bit);
        } else {
            boolean old = data.get(bit);
            if (old) {
                data.set(bit, false);
            }
            return old;
        }
    }

    public void clear() {
        data.clear();
    }

    public void put(int from, int to) {
        checkBounds("From", from);
        checkBounds("To", to);
        int old = get(from);
        if (old == -1) {
            blindPut(from, to);
        } else {
            inRange(from, (start, end) -> {
                if (data instanceof AtomicBits) {
                    boolean backwards = old < to;
                    ((AtomicBits) data).clearRangeAndSet(start, end, backwards, start + to);
                } else {
                    data.clear(start, end);
                    data.set(start + to);
                }
            });
        }
    }

    void blindPut(int from, int to) {
        checkBounds("From", from);
        checkBounds("To", to);
        inRange(from, (start, end) -> {
            if (data instanceof AtomicBits) {
                ((AtomicBits) data).clearRangeAndSet(start, end, start + to);
            } else {
                data.clear(start, end);
                data.set(start + to);
            }
        });
    }

    @SuppressWarnings({"AssertWithSideEffects", "NestedAssignment"})
    int checkBounds(String name, int val) {
        boolean asserts = false;
        assert asserts = true;
        if (asserts) {
            if (val >= stride) {
                throw new IndexOutOfBoundsException(name + " > " + stride
                        + ": " + val + " > " + stride);
            }
            if (val < 0) {
                throw new IndexOutOfBoundsException(name + " is < 0: " + val);
            }
        }
        return val;
    }

    void inRange(int item, IntBiConsumer rc) {
        int firstBit = stride * item;
        int lastBit = firstBit + stride;
        rc.accept(firstBit, lastBit);
    }

    RangeResult withRange(int item, IntBiFunction<RangeResult> c) {
        int firstBit = stride * item;
        int lastBit = firstBit + stride;
        return c.apply(firstBit, lastBit);
    }

    public int get(int item) {
        return getOrDefault(item, -1) % stride;
    }

    public int getOrDefault(int item, int n) {
        RangeResult res = withRange(item, (start, end) -> {
            RangeResult result = new RangeResult();
            data.forEachSetBitAscending(start, bit -> {
                if (bit >= end) {
                    return false;
                }
                result.value = bit;
                result.success = true;
                return false;
            });
            return result;
        });
        return res.success ? res.value : n;
    }

    public boolean containsKey(int item) {
        RangeResult res = withRange(item, (start, end) -> {
            RangeResult result = new RangeResult();
            data.forEachSetBitAscending(start, bit -> {
                if (bit >= end) {
                    return false;
                }
                result.value = bit;
                result.success = true;
                return false;
            });
            return result;
        });
        return res.success;
    }

    private final class AdaptedLongMapView implements LongMatrixMap {

        private final LongMapAdapter adapter;

        public AdaptedLongMapView(LongMapAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public long getOrDefault(long key, long defaultValue) {
            int val = IntMatrixMap.this.get(adapter.indexOfKey(key));
            if (val == -1) {
                return defaultValue;
            }
            return adapter.valueForValueIndex(val);
        }

        @Override
        public void put(long key, long val) {
            int keyIndex = adapter.indexOfKey(key);
            int valIndex = adapter.indexOfValue(val);
            IntMatrixMap.this.put(keyIndex, valIndex);
        }

        @Override
        public boolean containsKey(long key) {
            int keyIndex = adapter.indexOfKey(key);
            return IntMatrixMap.this.containsKey(keyIndex);
        }

        @Override
        public int size() {
            return IntMatrixMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return IntMatrixMap.this.isEmpty();
        }

        @Override
        public void forEach(LongBiConsumer c) {
            IntMatrixMap.this.forEach((k, v) -> {
                c.accept(adapter.indexOfKey(k), adapter.indexOfValue(v));
            });
        }

        @Override
        public int forEach(LongBiPredicate p) {
            return IntMatrixMap.this.forEachPair((k, v) -> {
                return p.test(adapter.indexOfKey(k), adapter.indexOfValue(v));
            });
        }

        @Override
        public boolean contains(long key, long value) {
            return IntMatrixMap.this.contains(adapter.indexOfKey(key), adapter.indexOfValue(value));
        }

        @Override
        public void remove(long key) {
            IntMatrixMap.this.remove(adapter.indexOfKey(key));
        }

        @Override
        public void clear() {
            IntMatrixMap.this.clear();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            forEach((k, v) -> {
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append(k).append(':').append(v);
            });
            return sb.append("]").toString();
        }
    }

    private final class LongMapView implements LongMatrixMap {

        private final LongToIntFunction mapper;
        private final IntToLongFunction dereferencer;

        public LongMapView(LongToIntFunction mapper, IntToLongFunction dereferencer) {
            this.mapper = mapper;
            this.dereferencer = dereferencer;
        }

        @Override
        public long getOrDefault(long key, long defaultValue) {
            int targetKey = mapper.applyAsInt(key);
            if (targetKey < 0) {
                return defaultValue;
            }
            int match = IntMatrixMap.this.getOrDefault(targetKey, -1);
            if (match < 0) {
                return defaultValue;
            }
            return mapper.applyAsInt(match);
        }

        @Override
        public void put(long key, long val) {
            int targetKey = mapper.applyAsInt(key);
            int targetValue = mapper.applyAsInt(val);
            IntMatrixMap.this.put(targetKey, targetValue);
        }

        @Override
        public boolean containsKey(long key) {
            int targetKey = mapper.applyAsInt(key);
            System.out.println("CK " + targetKey + " for val " + key + " data " + data);
            return IntMatrixMap.this.containsKey(targetKey);
        }

        @Override
        public int size() {
            return IntMatrixMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return IntMatrixMap.this.isEmpty();
        }

        @Override
        public void forEach(LongBiConsumer c) {
            IntMatrixMap.this.forEachPair((k, v) -> {
                c.accept(dereferencer.applyAsLong(k), dereferencer.applyAsLong(v));
            });
        }

        @Override
        public int forEach(LongBiPredicate p) {
            return IntMatrixMap.this.forEachPair((k, v) -> {
                return p.test(dereferencer.applyAsLong(k), dereferencer.applyAsLong(v));
            });
        }

        @Override
        public boolean contains(long key, long value) {
            return IntMatrixMap.this.contains(mapper.applyAsInt(key), mapper.applyAsInt(value));
        }

        @Override
        public void remove(long key) {
            IntMatrixMap.this.remove(mapper.applyAsInt(key));
        }

        @Override
        public void clear() {
            IntMatrixMap.this.clear();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            forEach((k, v) -> {
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append(k).append(':').append(v);
            });
            return sb.append("]").toString();
        }
    }
}
