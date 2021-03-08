/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
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
package com.mastfrog.bits;

import static com.mastfrog.bits.AtomicBitsImpl.BITS_PER_ENTRY;
import static com.mastfrog.util.preconditions.Checks.greaterThanZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.IntPredicate;

/**
 * Extension to MutableBits which is thread-safe without synchronization
 * and exposes methods for atomic operations.
 *
 * @author Tim Boudreau
 */
public interface AtomicBits extends MutableBits {

    /**
     * Get the (unalterable) total size of this Bits.
     *
     * @return the maximum number of bits that can be present
     */
    int capacity();

    /**
     * Atomically <i>clear</i> a bit, returning true if the operation modified
     * the state of this object.
     *
     * @param bit A bit index
     * @return true if the bit's value was changed
     */
    boolean clearing(int bit);

    /**
     * Atomically set a bit, returning true if the operation modified the state
     * of this object.
     *
     * @param bit A bit index
     * @return true if the bit's value was changed
     */
    boolean setting(int bit);

    /**
     * Atomically sets the next clear bit subsequent to the passed one,
     * returning that bit, or -1 if there are no such unset bits remaining; this
     * can be very useful for coordinating processing of a list of items across
     * multiple threads in a non-blocking, low-overhead manner.
     *
     * @param from The bfirst bit that could potentially be set if unset
     * @return
     */
    int settingNextClearBit(int from);

    @Override
    AtomicBits mutableCopy();

    @Override
    AtomicBits copy();

    @Override
    AtomicBits filter(IntPredicate pred) ;

    /**
     * Create a new AtomicBits with the passed (fixed, immutable) capacity.
     *
     * @param capacity The capacity
     * @return An AtomicBits
     */
    public static AtomicBits create(int capacity) {
        return new AtomicBitsImpl(greaterThanZero("capacity", capacity));
    }

    /**
     * Create an AtomicBits from an array of longs.
     *
     * @param totalBits The total capacity of the created AtomicBits, regardless
     * of the number of bits the passed array of longs can hold - if smaller,
     * some values will be partially or completely ignored, and if larger, the
     * resulting set will be padded with unset bits to the capacity
     * @param arr An array of longs
     * @return An AtomicBits
     */
    public static AtomicBits fromLongArray(int totalBits, long... arr) {
        int longs = greaterThanZero("totalBits", totalBits) / BITS_PER_ENTRY;
        if (totalBits % BITS_PER_ENTRY != 0) {
            longs++;
        }
        AtomicLongArray ala = new AtomicLongArray(Math.max(notNull("arr", arr).length, longs));
        for (int i = 0; i < arr.length; i++) {
            ala.set(i, arr[i]);
        }
        return new AtomicBitsImpl(ala, totalBits, true);
    }

    /**
     * Create an AtomicBits from a BitSet.
     *
     * @param bits
     * @return
     */
    public static AtomicBits fromBitSet(BitSet bits) {
        return fromBitSet(bits, bits.size());
    }

    public static AtomicBits fromBitSet(BitSet bits, int capacity) {
        AtomicBitsImpl newBits = new AtomicBitsImpl(capacity);
        Bits.fromBitSet(bits).forEachSetBitAscending(bit -> {
            if (bit >= capacity) {
                return false;
            }
            newBits.set(bit);
            return true;
        });
        return newBits;
    }

    public static AtomicBits of(Bits bits) {
        AtomicBitsImpl newBits = new AtomicBitsImpl(bits.length());
        bits.forEachSetBitAscending(bit -> {
            newBits.set(bit);
            return true;
        });
        return newBits;
    }
}
