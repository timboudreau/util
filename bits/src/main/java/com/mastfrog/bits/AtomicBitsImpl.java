/*
 * Copyright (c) 2021, Mastfrog Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.bits;

import static com.mastfrog.bits.Bits.Characteristics.ATOMIC;
import static com.mastfrog.bits.Bits.Characteristics.FIXED_SIZE;
import static com.mastfrog.bits.Bits.Characteristics.THREAD_SAFE;
import com.mastfrog.function.state.Int;
import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import static java.lang.Long.lowestOneBit;
import static java.lang.Long.numberOfLeadingZeros;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/**
 * An non-blocking, thread-safe, atomic implementation of MutableBits; this
 * differs from other Bits implementations in a few ways:
 * <ul>
 * <li>It exposes atomic operations (setting(), clearing(),
 * settingNextClearBit())</li>
 * <li>The capacity of the Bits is fixed at creation time, and it is not
 * expanded dynamically as higher bits are set as BitSet-based implementations
 * do.</li>
 * </ul>
 * <p>
 * This class can be very useful for parallelizing work where you have a
 * collection of items addressed by index, and want multiple threads to be able
 * to grab the next item to process without colliding and processing the same
 * item, and without the rather expensive overhead of invoking deletion methods
 * from concurrent queues or lists, which are usually expensive and can create
 * liveness issues in the code you are trying to parallelize.
 * </p>
 * Bulk operations (set(start, end), clear(start, end)) are atomic only when
 * they do not span a 64-bit boundary.
 *
 * @author Tim Boudreau
 */
final class AtomicBitsImpl implements Externalizable, AtomicBits {

    static final int BITS_PER_ENTRY = Long.BYTES * 8;
    private static final long[] FULL_MASKS = new long[BITS_PER_ENTRY];
    private static final int VER = 1;
    private static final Set<Characteristics> CHARACTERISTICS
                = Collections.unmodifiableSet(EnumSet.of(ATOMIC, THREAD_SAFE, FIXED_SIZE));

    static {
        // Avoids an inner loop when handling the final
        // long in a bits whose capacity is not a multiple of 64s
        for (int i = 0; i < FULL_MASKS.length; i++) {
            long val = 0;
            for (int j = 0; j < i + 1; j++) {
                val |= 1L << j;
            }
            FULL_MASKS[i] = val;
        }
    }

    @Override
    public MutableBits newBits(int size) {
        return new AtomicBitsImpl(size);
    }

    @Override
    public MutableBits newBits(long size) {
        if (size <= Integer.MAX_VALUE) {
            return new AtomicBitsImpl((int) size);
        }
        return AtomicBits.super.newBits(size);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CHARACTERISTICS;
    }

    private static String bstr(long val) {
        StringBuilder sb = new StringBuilder(Long.toBinaryString(val));
        sb.reverse();
        while (sb.length() < BITS_PER_ENTRY) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static boolean equal(AtomicLongArray a, AtomicLongArray b) {
        int ml = Math.min(a.length(), b.length());
        for (int i = 0; i < ml; i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    // XXX the capacity in long-indices is really
    // Integer.MAX_VALUE * 8, but the indexing logic would need to be
    // changed to long-based to take advantage of it
    private final AtomicLongArray arr;
    private final int capacity;

    /**
     * Creates an AtomicBits with a fixed size of 64 bits; this constructor is
     * only public to enable the implementation of java.io.Externalizable to
     * find a constructor, and is not generally useful.
     */
    public AtomicBitsImpl() {
        this(64);
    }

    AtomicBitsImpl(int capacity) {
        this.capacity = greaterThanZero("totalBits", capacity);
        int longs = capacity / BITS_PER_ENTRY;
        if (capacity % BITS_PER_ENTRY != 0) {
            longs++;
        }
        arr = new AtomicLongArray(longs);
    }

    AtomicBitsImpl(AtomicLongArray toCopy, int capacity, boolean unsafe) {
        this.capacity = capacity;
        arr = unsafe ? toCopy : new AtomicLongArray(greaterThanZero("toCopy.length()",
                notNull("toCopy", toCopy).length()));
        for (int i = 0; i < toCopy.length(); i++) {
            arr.set(i, toCopy.get(i));
        }
    }

    @Override
    public void clear(int bitIndex) {
        set(bitIndex, false);
    }

    @Override
    public long cardinalityLong() {
        return cardinality();
    }

    @Override
    public boolean get(long bitIndex) {
        if (bitIndex < 0 || bitIndex > Integer.MAX_VALUE) {
            return false;
        }
        return get((int) bitIndex);
    }

    @Override
    public byte[] toByteArray() {
        ByteBuffer buf = ByteBuffer.allocate(arr.length() * Long.BYTES);
        for (int i = 0; i < arr.length(); i++) {
            buf.putLong(arr.get(i));
        }
        return buf.array();
    }

    @Override
    public AtomicBitsImpl filter(IntPredicate pred) {
        AtomicBitsImpl result = new AtomicBitsImpl(new AtomicLongArray(arr.length()),
                capacity, true);
        forEachSetBitAscending(bit -> {
            if (pred.test(bit)) {
                result.set(bit);
            }
        });
        return result;
    }

    @Override
    public boolean isNativelyLongIndexed() {
        return false;
    }

    @Override
    public boolean intersects(Bits set) {
        if (set instanceof AtomicBitsImpl) {
            AtomicBitsImpl ato = (AtomicBitsImpl) set;
            int len = Math.min(ato.arr.length(), arr.length());
            for (int i = 0; i < len; i++) {
                long a = ato.arr.get(i);
                long b = arr.get(i);
                if ((a & b) != 0) {
                    return true;
                }
            }
            return false;
        }
        return AtomicBits.super.intersects(set);
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        change(fromIndex, toIndex, value);
    }

    @Override
    public long[] toLongArray() {
        long[] result = new long[arr.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = arr.get(i);
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < arr.length(); i++) {
            if (arr.get(i) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < arr.length(); i++) {
            arr.lazySet(i, 0L);
        }
    }

    /**
     * Returns this - AtomicBitsImpl is already thread-safe, so synchronization
     * is superfluous.
     *
     * @return An AtomicBitsImpl
     */
    @Override
    public AtomicBitsImpl toSynchronizedBits() {
        return this;
    }

    @Override
    public void or(Bits set) {
        if (set == this) {
            return;
        }
        if (set instanceof AtomicBitsImpl) {
            AtomicBitsImpl other = (AtomicBitsImpl) set;
            int max = Math.min(other.arr.length(), arr.length());
            for (int i = 0; i < max; i++) {
                long val = other.arr.get(i);
                arr.updateAndGet(i, old -> old | val);
            }
            return;
        }
        AtomicBits.super.or(set);
    }

    @Override
    public void andNot(Bits set) {
        if (set == this) {
            clear();
            return;
        }
        if (set instanceof AtomicBitsImpl) {
            AtomicBitsImpl other = (AtomicBitsImpl) set;
            int max = Math.min(other.arr.length(), arr.length());
            for (int i = 0; i < max; i++) {
                long val = other.arr.get(i);
                arr.updateAndGet(i, old -> old & ~val);
            }
            return;
        }
        AtomicBits.super.andNot(set);
    }

    @Override
    public void and(Bits set) {
        if (set == this) {
            clear();
            return;
        }
        if (set instanceof AtomicBitsImpl) {
            AtomicBitsImpl other = (AtomicBitsImpl) set;
            int max = Math.min(other.arr.length(), arr.length());
            for (int i = 0; i < max; i++) {
                long val = other.arr.get(i);
                arr.updateAndGet(i, old -> old & val);
            }
            return;
        }
        AtomicBits.super.andNot(set);
    }

    @Override
    public void xor(Bits set) {
        if (set == this) {
            clear();
            return;
        }
        if (set instanceof AtomicBitsImpl) {
            AtomicBitsImpl other = (AtomicBitsImpl) set;
            int max = Math.min(other.arr.length(), arr.length());
            for (int i = 0; i < max; i++) {
                long val = other.arr.get(i);
                arr.updateAndGet(i, old -> old ^ val);
            }
            return;
        }
        AtomicBits.super.andNot(set);
    }

    @Override
    public void flip(int bitIndex) {
        if (bitIndex >= 0 && bitIndex < capacity) {
            int lix = indexOfLong(bitIndex);
            long mask = maskFor(bitIndex);
            arr.getAndUpdate(lix, old -> {
                if ((old & mask) != 0) {
                    return old & ~mask;
                } else {
                    return old | mask;
                }
            });
        }
    }

    @Override
    public void clear(int fromIndex, int toIndex) {
        change(fromIndex, toIndex, false);
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        change(fromIndex, toIndex, true);
    }

    /**
     * Set a value and clear a value, atomically if possible, and if not, using
     * the supplied preference to sequence the set and clear operations (used by
     * IntMatrixMap to make operations effectively atomic).
     *
     * @param toSet An index to set
     * @param toClear An index to clear
     * @param pref If the operation <i>cannot</i> be performed atomically, use
     * this preference to choreograph the order of operations
     * @return True if the data was altered by this operation
     */
    public boolean setAndClear(int toSet, int toClear, SetPreference pref) {
        if (toSet == toClear) {
            return false;
        }
        int setId = indexOfLong(toSet);
        int clearId = indexOfLong(toClear);
        if (setId == clearId) { // atomic! whee!
            long setMask = maskFor(toSet);
            long clearMask = maskFor(toClear);
            long val = arr.getAndUpdate(setId, old -> {
                return (old | setMask) & ~clearMask;
            });
            return (val & setMask) == 0
                    || (val & clearMask) != 0;
        } else {
            return pref.choreograph(toClear, this::clearing, toSet, this::setting);
        }
    }

    /**
     * Clear a range of bits and set one within it atomically such that a caller
     * cannot ever see the range within which the bit to set lies as being empty
     * unless it was empty prior to this call.
     *
     * @param clearStart The starting bit to clear
     * @param clearEnd The stop bit to clear
     * @param set The bit to set
     * @throws IllegalArgumentException if the value to set is not within the
     * passed range.
     */
    public void clearRangeAndSet(int clearStart, int clearEnd, int set) {
        clearEnd = Math.min(clearEnd, capacity);
        int startIx = indexOfLong(clearStart);
        int stopIx = Math.min(indexOfLong(clearEnd), this.arr.length() - 1);
        int setIx = indexOfLong(set);
        if (setIx < startIx || setIx > stopIx) {
            throw new IllegalArgumentException("To set "
                    + set + " is not in " + clearStart + ":" + clearEnd);
        }
        for (int lix = startIx; lix <= stopIx; lix++) {
            long mask = FULL_MASKS[FULL_MASKS.length - 1];
            if (lix == startIx) {
                long maskMask = 0;
                for (int i = 0; i < clearStart % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= ~maskMask;
            }
            if (lix == stopIx) {
                int ix = (capacity % BITS_PER_ENTRY) - 1;
                if (ix < 0) {
                    ix = FULL_MASKS.length - 1;
                }
                mask &= FULL_MASKS[ix];
                long maskMask = 0;
                for (int i = 0; i < clearEnd % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= maskMask;
            }
            int index = lix;
            long maskFinal;
            maskFinal = mask;
            arr.getAndUpdate(lix, old -> {
                if (index == setIx) {
                    return (old & ~maskFinal) | maskFor(set);
                } else {
                    return old & ~maskFinal;
                }
            });
        }
    }

    public void clearRangeAndSet(int clearStart, int clearEnd, boolean backwards, int set) {
        if (!backwards) {
            clearRangeAndSet(clearStart, clearEnd, set);
            return;
        }
        clearEnd = Math.min(clearEnd, capacity);
        int startIx = indexOfLong(clearStart);
        int stopIx = Math.min(indexOfLong(clearEnd), this.arr.length() - 1);
        int setIx = indexOfLong(set);
        if (setIx < startIx || setIx > stopIx) {
            throw new IllegalArgumentException("To set "
                    + set + " is not in " + clearStart + ":" + clearEnd);
        }
        for (int lix = stopIx; lix >= startIx; lix--) {
            long mask = FULL_MASKS[FULL_MASKS.length - 1];
            if (lix == startIx) {
                long maskMask = 0;
                for (int i = 0; i < clearStart % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= ~maskMask;
            }
            if (lix == stopIx) {
                int ix = (capacity % BITS_PER_ENTRY) - 1;
                if (ix < 0) {
                    ix = FULL_MASKS.length - 1;
                }
                mask &= FULL_MASKS[ix];
                long maskMask = 0;
                for (int i = 0; i < clearEnd % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= maskMask;
            }
            int index = lix;
            long maskFinal;
            maskFinal = mask;
            arr.getAndUpdate(lix, old -> {
                if (index == setIx) {
                    return (old & ~maskFinal) | maskFor(set);
                } else {
                    return old & ~maskFinal;
                }
            });
        }
    }

    private void change(int fromIndex, int toIndex, boolean set) {
        toIndex = Math.min(toIndex, capacity);
        int startIx = indexOfLong(fromIndex);
        int stopIx = indexOfLong(toIndex);
        for (int lix = startIx; lix <= stopIx; lix++) {
            long mask = FULL_MASKS[FULL_MASKS.length - 1];
            if (lix == startIx) {
                long maskMask = 0;
                for (int i = 0; i < fromIndex % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= ~maskMask;
            }
            if (lix == stopIx) {
                int ix = (capacity % BITS_PER_ENTRY) - 1;
                if (ix < 0) {
                    ix = FULL_MASKS.length - 1;
                }
                mask &= FULL_MASKS[ix];
                long maskMask = 0;
                for (int i = 0; i < toIndex % BITS_PER_ENTRY; i++) {
                    maskMask |= 1L << i;
                }
                mask &= maskMask;
            }
            long maskFinal = mask;
            arr.getAndUpdate(lix, old -> {
                if (set) {
                    return old | maskFinal;
                } else {
                    return old & ~maskFinal;
                }
            });
        }
    }

    /**
     * Get the (unalterable) total size of this Bits.
     *
     * @return the maximum number of bits that can be present
     */
    @Override
    public int capacity() {
        return capacity;
    }

    int indexOfLong(int bit) {
        return bit / BITS_PER_ENTRY;
    }

    long maskFor(int bit) {
        return 1L << (bit % BITS_PER_ENTRY);
    }

    long inverseMaskFor(int bit) {
        return ~maskFor(bit);
    }

    /**
     * Atomically set a bit, returning true if the operation modified the state
     * of this object.
     *
     * @param bit A bit index
     * @return true if the bit's value was changed
     */
    @Override
    public boolean setting(int bit) {
        if (bit < 0 || bit >= capacity) {
            throw new IndexOutOfBoundsException("Outside 0-" + capacity + ": " + bit);
        }
        long mask = maskFor(bit);
        long val = arr.getAndUpdate(indexOfLong(bit), old -> {
            return old | mask;
        });
        return (val & mask) == 0;
    }

    /**
     * Atomically <i>clear</i> a bit, returning true if the operation modified
     * the state of this object.
     *
     * @param bit A bit index
     * @return true if the bit's value was changed
     */
    @Override
    public boolean clearing(int bit) {
        if (bit < 0 || bit >= capacity) {
            throw new IndexOutOfBoundsException("Outside 0-" + capacity + ": " + bit);
        }
        long mask = maskFor(bit);
        long val = arr.getAndUpdate(indexOfLong(bit), old -> {
            return old & ~mask;
        });
        return (val & mask) != 0;
    }

    @Override
    public void set(int bit) {
        setting(bit);
    }

    @Override
    public void set(int bitIndex, boolean value) {
        if (value) {
            setting(bitIndex);
        } else {
            clearing(bitIndex);
        }
    }

    @Override
    public int cardinality() {
        int result = 0;
        for (int i = 0; i < arr.length(); i++) {
            result += Long.bitCount(arr.get(i));
        }
        return result;
    }

    @Override
    public AtomicBitsImpl copy() {
        return mutableCopy();
    }

    public AtomicBitsImpl copy(int newBits) {
        AtomicLongArray nue = new AtomicLongArray(newBits / BITS_PER_ENTRY);
        for (int i = 0; i < Math.min(nue.length(), arr.length()); i++) {
            nue.set(i, arr.get(i));
        }
        return new AtomicBitsImpl(nue, newBits, true);
    }

    @Override
    public AtomicBitsImpl mutableCopy() {
        return new AtomicBitsImpl(arr, capacity, false);
    }

    @Override
    public boolean get(int bitIndex) {
        return (arr.get(indexOfLong(bitIndex)) & maskFor(bitIndex)) != 0;
    }

    private ToIntFunction<IntUnaryOperator> iterate(int from) {
        // A function that starts with the passed from-bit,
        // and if no unset bits are found in that cell (the IntUnaryOperator
        // returns < 0) then tries the 0th bit of the next long,
        // until a non-zero result or all available positions in the
        // array have been exhausted
        return op -> {
            int step = from;
            if (step > capacity) {
                return -1;
            }
            int result = -1;
            do {
                result = op.applyAsInt(step);
                if (result < 0) {
                    if (step == from) {
                        int lix = indexOfLong(from);
                        int base = lix * BITS_PER_ENTRY;
                        step = base + BITS_PER_ENTRY;
                    } else {
                        step += BITS_PER_ENTRY;
                    }
                } else if (result >= capacity) {
                    return -1;
                }
            } while (result < 0 && step < capacity);
            return result;
        };
    }

    @Override
    public int settingNextClearBit(int from) {
        if (from < 0) {
            from = 0;
        } else if (from >= capacity) {
            return -1;
        }
        return iterate(from).applyAsInt(this::_settingNextClearBit);
    }

    private int _settingNextClearBit(int from) {
        // Holder for our result
        Int targetBit = Int.of(-1);
        // from / 64
        int lix = indexOfLong(from);
        int base = lix * BITS_PER_ENTRY;
        long full;
        if ((lix + 1) * BITS_PER_ENTRY >= capacity) {
            int ix = (capacity % BITS_PER_ENTRY) - 1;
            if (ix < 0) {
                ix = FULL_MASKS.length - 1;
            }
            full = FULL_MASKS[ix];
        } else {
            full = FULL_MASKS[BITS_PER_ENTRY - 1];
        }
        int f = from;
        arr.updateAndGet(lix, old -> {
            // AtomicLong busywaits, so we can be called more than once -
            // reset any past result here
            targetBit.set(-1);
            if (old != full) {
                if (old == 0 && f % BITS_PER_ENTRY == 0) {
                    targetBit.set(f);
                    return 1;
                }
                long mask = maskFor(f);
                // We got an immediate hit
                if ((old & mask) == 0) {
                    targetBit.set(f);
                    long result = old | mask;
                    return result;
                } else {
                    long compl = ~old;
                    for (;;) {
                        // Find the lowest one bit (actually the lowest 0 position)
                        // position in `old`
                        long leastZeroBit = lowestOneBit(compl);
                        if ((leastZeroBit & full) == 0) {
                            // All bit positions >= from thru totalBits are already occupied,
                            // so we're done
                            break;
                        }
                        // Compute which bit it is
                        int zeroBitPosition = BITS_PER_ENTRY - (numberOfLeadingZeros(leastZeroBit) + 1);
                        // And a mask for it
                        long bitmask = 1L << zeroBitPosition;
                        // It may be a bit below the first requested bit, in which
                        // case we loop
                        if ((zeroBitPosition + base) >= f) {
                            targetBit.set(zeroBitPosition + base);
                            // Merge it into our value
                            return old | bitmask;
                        }
                        compl &= ~bitmask;
                    }
                }
            }
            return old;
        });
        int result = targetBit.getAsInt();
        // If we found a bit, return it
        if (result != -1) {
            return result;
        }
        // No bits found; we're done
        return -1;
    }

    @Override
    public int nextClearBit(int fromIndex) {
        if (fromIndex >= capacity) {
            return -1;
        }
        int index = fromIndex < 0 ? 0 : fromIndex;
        int max = arr.length();
        long mask;
        for (int longIx = indexOfLong(index); longIx < max; longIx++) {
            long val = arr.get(longIx);
            do {
                mask = inverseMaskFor(index);
                if ((val | mask) == mask) {
                    return index;
                }
                index++;
            } while (index % BITS_PER_ENTRY != 0);
        }
        return -1;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        if (fromIndex >= capacity) {
            return -1;
        }
        int index = fromIndex < 0 ? 0 : fromIndex;
        int max = arr.length();
        long mask;
        for (int longIx = indexOfLong(index); longIx < max; longIx++) {
            long val = arr.get(longIx);
            if (val == 0) {
                index += BITS_PER_ENTRY;
                continue;
            }
            do {
                mask = maskFor(index);
                if ((val & mask) != 0) {
                    return index;
                }
                index++;
            } while (index % BITS_PER_ENTRY != 0);
        }
        return -1;
    }

    @Override
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }
        int index = Math.min(fromIndex, capacity - 1);
        int lix;
        long mask;
        do {
            lix = indexOfLong(index);
            long val = arr.get(lix);
            mask = inverseMaskFor(index);
            if ((mask | val) == mask) {
                return index;
            }
            index--;
        } while (index >= 0);
        return -1;
    }

    @Override
    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        }
        int index = Math.min(fromIndex, capacity - 1);
        int lix;
        long mask;
        do {
            lix = indexOfLong(index);
            long val = arr.get(lix);
            mask = maskFor(index);
            if ((mask & val) != 0) {
                return index;
            }
            index--;
        } while (index >= 0);
        return -1;
    }

    public String toBitsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            sb.append(bstr(arr.get(i)));
        }
        return sb.toString();
    }

    @Override
    public boolean contentEquals(Bits other) {
        if (other == this) {
            return true;
        } else if (other == null) {
            return false;
        }
        if (other instanceof AtomicBitsImpl) {
            AtomicBitsImpl ato = (AtomicBitsImpl) other;
            return equal(ato.arr, arr);
        }
        return AtomicBits.super.contentEquals(other);
    }

    @Override
    public int hashCode() {
        return AtomicBits.super.bitsHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        }
        return contentEquals((Bits) o);
    }

    @Override
    public String toString() {
        return stringValue();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(VER);
        out.writeInt(capacity);
        for (int i = 0; i < arr.length(); i++) {
            out.writeLong(arr.get(i));
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int ver = in.readByte();
        if (ver != VER) {
            throw new IOException("Incorrect version " + ver + " expected " + VER);
        }
        int bits = in.readInt();
        if (bits <= 0) {
            throw new IOException("Total bits <= 0 is impossible: " + bits);
        }
        int longs = bits / BITS_PER_ENTRY;
        if (bits % BITS_PER_ENTRY != 0) {
            longs++;
        }
        AtomicLongArray arrayLoc = new AtomicLongArray(longs);
        for (int i = 0; i < arrayLoc.length(); i++) {
            arrayLoc.set(i, in.readLong());
        }
        try {
            Field f = AtomicBitsImpl.class.getDeclaredField("arr");
            f.setAccessible(true);
            f.set(this, arrayLoc);
            f = AtomicBitsImpl.class.getDeclaredField("capacity");
            f.setAccessible(true);
            f.set(this, bits);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException ex) {
            throw new IOException("Could not find or set fields", ex);
        }
    }
}
