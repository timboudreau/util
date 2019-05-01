package com.mastfrog.graph;

import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
public final class BitSetUtils {

    private BitSetUtils() {
        throw new AssertionError();
    }

    public static BitSet copyOf(BitSet set) {
        BitSet nue = new BitSet(set.size());
        nue.or(set);
        return nue;
    }

    public static void forEach(BitSet set, IntConsumer cons) {
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            cons.accept(bit);
        }
    }

    public static BitSet invert(BitSet set) {
        int last = set.previousSetBit(Integer.MAX_VALUE);
        long[] arr = set.toLongArray();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ~arr[i];
        }
        BitSet result = BitSet.valueOf(arr);
        // This may give us some trailing set bits beyond the last used
        // bit in the original - clear them to avoid confusion
        int lastInvertedBit = result.previousSetBit(Integer.MAX_VALUE);
        if (last + 1 <= lastInvertedBit) {
            result.clear(last + 1, lastInvertedBit + 1);
        }
        return result;
    }

    public static BitSet invert(BitSet set, int lastBit) {
//        BitSet result = (BitSet) set.clone();
//        result.flip(0, lastBit + 1);
//        return result;
        int last = set.previousSetBit(Integer.MAX_VALUE);
        long[] arr = set.toLongArray();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ~arr[i];
        }
        BitSet result = BitSet.valueOf(arr);
        // This may give us some trailing set bits beyond the last used
        // bit in the original - clear them to avoid confusion
        if (last + 1 <= lastBit) {
            result.clear(last+1, lastBit+1);
        }
        return result;
    }

}
