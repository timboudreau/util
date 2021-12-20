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

import static com.mastfrog.bits.Bits.Characteristics.LARGE;
import static com.mastfrog.bits.Bits.Characteristics.LONG_VALUED;
import static com.mastfrog.bits.Bits.Characteristics.NEGATIVE_VALUES_ALLOWED;
import static com.mastfrog.bits.RLEBits.UpdateStatus.*;
import com.mastfrog.function.state.Int;
import com.mastfrog.function.state.Lng;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;

/**
 *
 * @author Tim Boudreau
 */
final class RLEBits implements MutableBits {

    private final SortedSet<Run> runs;
    private static final Set<Characteristics> CHARACTERISTICS
            = Collections.unmodifiableSet(EnumSet.of(LARGE, LONG_VALUED, NEGATIVE_VALUES_ALLOWED));

    RLEBits() {
        this.runs = new TreeSet<>();
    }

    private RLEBits(SortedSet<Run> runs) {
        this.runs = runs;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Run r : runs) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(r);
        }
        return sb.toString();
    }

    @Override
    public int cardinality() {
        int result = 0;
        for (Run r : runs) {
            result += r.size();
        }
        return result;
    }

    @Override
    public RLEBits copy() {
        return mutableCopy();
    }

    long last() {
        if (runs.isEmpty()) {
            return -1;
        }
        return runs.last().end - 1;
    }

    long first() {
        if (runs.isEmpty()) {
            return -1;
        }
        return runs.first().start;
    }

    @Override
    public RLEBits mutableCopy() {
        TreeSet<Run> newRuns = new TreeSet<>();
        for (Run r : runs) {
            newRuns.add(r.copy());
        }
        return new RLEBits(newRuns);
    }

    @Override
    public boolean get(int bitIndex) {
        Run run = runFor(bitIndex, true);
        if (run != null && run.contains(bitIndex)) {
            return true;
        }
        return false;
    }

    @Override
    public BitSet toBitSet() {
        if (isEmpty()) {
            return new BitSet(0);
        }
        BitSet result = new BitSet();
        for (Run r : runs) {
            long st = r.start;
            long en = r.end;
            if (en < 0) {
                continue;
            } else if (r.end > 0) {
                st = 0;
            }
            if (st > Integer.MAX_VALUE) {
                break;
            }
            if (en > Integer.MAX_VALUE) {
                en = Integer.MAX_VALUE;
            }
            result.set((int) st, (int) en);
        }
        return result;
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, long upTo, LongConsumer consumer) {
        long first = first();
        long count = 0;
        if (first > from) {
            for (long l = 0; l < first; l++) {
                if (l >= upTo) {
                    return count;
                }
                count++;
                consumer.accept(l);
            }
        }
        if (runs.size() == 1) {
            long last = last();
            if (upTo > last) {
                for (long l = last + 1; l < upTo; l++) {
                    count++;
                    consumer.accept(l);
                }
            }
            return count;
        }
        Run prev = null;
        for (Run r : runs) {
            if (prev != null) {
                if (prev.abuts(r)) {
                    continue;
                }
                for (long l = prev.end; l < r.start; l++) {
                    if (l >= upTo) {
                        return count;
                    }
                    consumer.accept(l);
                }
            }
        }
        if (prev != null && upTo >= prev.end) {
            for (long l = prev.end; l < upTo; l++) {
                count++;
                consumer.accept(l);
            }
        }
        return count;
    }

    @Override
    public long forEachUnsetLongBitAscending(long from, long upTo, LongPredicate consumer) {
        long first = first();
        long count = 0;
        if (first > from) {
            for (long l = 0; l < first; l++) {
                if (l >= upTo) {
                    return count;
                }
                count++;
                if (!consumer.test(l)) {
                    return count;
                }
            }
        }
        if (runs.size() == 1) {
            long last = last();
            if (upTo > last) {
                for (long l = last + 1; l < upTo; l++) {
                    count++;
                    if (!consumer.test(l)) {
                        return count;
                    }
                }
            }
            return count;
        }
        Run prev = null;
        for (Run r : runs) {
            if (prev != null) {
                if (prev.abuts(r)) {
                    continue;
                }
                for (long l = prev.end; l < r.start; l++) {
                    if (l >= upTo) {
                        return count;
                    }
                    if (!consumer.test(l)) {
                        return count;
                    }
                }
            }
        }
        if (prev != null && upTo >= prev.end) {
            for (long l = prev.end; l < upTo; l++) {
                count++;
                if (!consumer.test(l)) {
                    return count;
                }
            }
        }
        return count;
    }

    @Override
    public void forEachLongSetBitDescending(LongConsumer consumer) {
        for (Run run : new RI(null)) {
            for (long l = run.end - 1; l >= run.start; l--) {
                consumer.accept(l);
            }
        }
    }

    @Override
    public long forEachLongSetBitDescending(LongPredicate consumer) {
        long result = 0;
        for (Run run : new RI(null)) {
            for (long l = run.end - 1; l >= run.start; l--) {
                result++;
                if (!consumer.test(l)) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public long forEachLongSetBitAscending(LongConsumer consumer) {
        long result = 0;
        for (Run run : runs) {
            for (long l = run.start; l < run.end; l++) {
                result++;
                consumer.accept(l);
            }
        }
        return result;
    }

    @Override
    public long forEachLongSetBitAscending(LongPredicate consumer) {
        long result = 0;
        for (Run run : runs) {
            for (long l = run.start; l < run.end; l++) {
                result++;
                if (!consumer.test(l)) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public int forEachSetBitAscending(IntConsumer consumer) {
        int result = 0;
        for (Run r : runs) {
            long st = r.start;
            if (st > Integer.MAX_VALUE) {
                break;
            }
            long en = r.end;
            if (en < Integer.MIN_VALUE) {
                continue;
            } else if (st < Integer.MIN_VALUE) {
                st = Integer.MIN_VALUE;
            }
            if (en > ((long) Integer.MAX_VALUE) + 1L) {
                en = ((long) Integer.MAX_VALUE) + 1L;
            }
            for (long l = st; l < en; l++) {
                result++;
                consumer.accept((int) l);
            }
        }
        return result;

    }

    @Override
    public int forEachSetBitAscending(IntPredicate consumer) {
        int result = 0;
        for (Run r : runs) {
            long st = r.start;
            if (st > Integer.MAX_VALUE) {
                break;
            }
            long en = r.end;
            if (en < Integer.MIN_VALUE) {
                continue;
            } else if (st < Integer.MIN_VALUE) {
                st = Integer.MIN_VALUE;
            }
            if (en > ((long) Integer.MAX_VALUE) + 1L) {
                en = ((long) Integer.MAX_VALUE) + 1L;
            }
            for (long l = st; l < en; l++) {
                result++;
                if (!consumer.test((int) l)) {
                    break;
                }
            }
        }
        return result;
    }

    public int forEachSetBitAscending(int start, IntConsumer consumer) {
        return forEachSetBitAscending(start, val -> {
            consumer.accept(val);
            return true;
        });
    }

    public int forEachSetBitAscending(int start, IntPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        SortedSet<Run> tail = runs.tailSet(new Run(start - 1));
        Int ct = Int.create();
        for (Run r : tail) {
            boolean keepGoing = r.eachBit(start, item -> {
                if (item > Integer.MAX_VALUE) {
                    return false;
                }
                ct.increment();
                return consumer.test((int) item);
            });
            if (!keepGoing) {
                break;
            }
        }
        return ct.getAsInt();

    }

    @Override
    public long forEachLongSetBitAscending(long start, LongConsumer consumer) {
        return forEachLongSetBitAscending(start, val -> {
            consumer.accept(val);
            return true;
        });
    }

    public long forEachLongSetBitAscending(long start, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        SortedSet<Run> tail = runs.tailSet(new Run(start - 1));
        Lng ct = Lng.create();
        for (Run r : tail) {
            boolean keepGoing = r.eachBit(start, item -> {
                ct.increment();
                return consumer.test(item);
            });
            if (!keepGoing) {
                break;
            }
        }
        return ct.getAsLong();
    }

    public long forEachLongSetBitAscending(long start, long end, LongConsumer consumer) {
        return forEachLongSetBitAscending(start, end, val -> {
            consumer.accept(val);
            return true;
        });
    }

    public long forEachLongSetBitAscending(long start, long end, LongPredicate consumer) {
        if (isEmpty()) {
            return 0;
        }
        SortedSet<Run> tail = runs.tailSet(new Run(start - 1));
        Lng ct = Lng.create();
        for (Run r : tail) {
            if (r.start >= end) {
                break;
            }
            boolean keepGoing = r.eachBit(start, item -> {
                ct.increment();
                return consumer.test(item);
            });
            if (!keepGoing) {
                break;
            }
        }
        return ct.getAsLong();
    }


    /*
    @Override
    public void or(Bits set) {
        if (set instanceof RLEBits) {
            TreeSet<Run> nue = new TreeSet<>(runs);
            nue.addAll(((RLEBits) set).runs);
            Run prev = null;
            for (Iterator<Run> it = nue.iterator(); it.hasNext();) {
                Run run = it.next();
                if (prev == null) {
                    nue.add(run);
                    continue;
                }
                if (prev.contains(run.start) || prev.canContain(run.start)) {
                    it.remove();
                    prev.end = run.end;
                } else if (run.contains(prev.end) || prev.abuts(run)) {
                    it.remove();
                    prev.end = run.end;
                } else if (prev.contains(run.end) && prev.contains(run.start)) {
                    it.remove();
                }
            }
            runs.clear();
            runs.addAll(nue);
            return;
        }
        MutableBits.super.or(set);
    }

    @Override
    public MutableBits orWith(Bits set) {
        if (set instanceof RLEBits) {
            TreeSet<Run> nue = new TreeSet<>(runs);
            nue.addAll(((RLEBits) set).runs);
            Run prev = null;
            for (Iterator<Run> it = nue.iterator(); it.hasNext();) {
                Run run = it.next();
                if (prev == null) {
                    nue.add(run);
                    continue;
                }
                if (prev.contains(run.start) || prev.canContain(run.start)) {
                    it.remove();
                    prev.end = run.end;
                } else if (run.contains(prev.end) || prev.abuts(run)) {
                    it.remove();
                    prev.end = run.end;
                } else if (prev.contains(run.end) && prev.contains(run.start)) {
                    it.remove();
                }
            }
            runs.clear();
            runs.addAll(nue);
            return new RLEBits(nue);
        }
        return MutableBits.super.orWith(set);
    }
     */
    private RI reverseIterator(Run from) {
        return from == null ? new RI(runs) : new RI(runs.headSet(from));
    }

    static class RI implements Iterator<Run>, Iterable<Run> {

        private SortedSet<Run> runs;

        public RI(SortedSet<Run> runs) {
            this.runs = runs;
        }

        @Override
        public boolean hasNext() {
            return runs != null && !runs.isEmpty();
        }

        @Override
        public Run next() {
            if (runs.isEmpty()) {
                throw new NoSuchElementException();
            }
            Run r = runs.last();
            runs = runs.headSet(r);
            return r;
        }

        @Override
        public Iterator<Run> iterator() {
            return this;
        }
    }

    @Override
    public void set(int bitIndex, boolean value) {
        update(bitIndex, value);
    }

    @Override
    public void set(long bitIndex, boolean value) {
        update(bitIndex, value);
    }

    @Override
    public void set(int bitIndex) {
        set((long) bitIndex);
    }

    @Override
    public void set(long bitIndex) {
        update(bitIndex, true);
    }

    @Override
    public void clear(long bitIndex) {
        update(bitIndex, false);
    }

    @Override
    public void clear(int bitIndex) {
        clear((long) bitIndex);
    }

    public void clear(long fromIndex, long toIndex) {
        if (isEmpty()) {
            return;
        }
        if (toIndex < fromIndex) {
            long hold = toIndex;
            toIndex = fromIndex;
            fromIndex = toIndex;
        }
        Run head = null;
        Run tail = null;
        for (Iterator<Run> it = runs.iterator(); it.hasNext();) {
            Run r = it.next();
            if (r.contains(fromIndex)) {
                head = r;
            } else if (r.contains(toIndex)) {
                tail = r;
                break;
            } else if (r.isWithin(fromIndex, toIndex)) {
                it.remove();
            }
        }
        if (head != null) {
            head.end = fromIndex;
        }
        if (tail != null) {
            tail.start = toIndex + 1;
            if (tail.start == tail.end) {
                runs.remove(tail);
            }
        }
    }

    @Override
    public void set(long fromIndex, long toIndex) {
        if (toIndex < fromIndex) {
            long hold = toIndex;
            toIndex = fromIndex;
            fromIndex = hold;
        }
        if (fromIndex == toIndex) {
            set(fromIndex);
        } else if (isEmpty()) {
            runs.add(new Run(fromIndex, toIndex));
            return;
        }
        if (toIndex < first()) {
            runs.add(new Run(fromIndex, toIndex));
            return;
        } else if (fromIndex > last()) {
            runs.add(new Run(fromIndex, toIndex));
            return;
        }
        Run prev = null;
        List<Run> spanned = null;
        for (Run r : runs) {
            if (r.contains(fromIndex) && r.contains(toIndex) || toIndex == r.end) {
                return;
            }
            if (prev == null && toIndex < r.start) {
                if (toIndex == r.start) {
                    r.start = fromIndex;
                    return;
                } else {
//                    System.out.println("r1");
                    runs.add(new Run(fromIndex, toIndex));
                    return;
                }
            } else if (prev != null) {
                if (prev.end <= fromIndex && r.start > toIndex) {
//                    System.out.println("r2");
                    if (prev.end == fromIndex) {
                        prev.end = toIndex;
                        return;
                    }
                    runs.add(new Run(fromIndex, toIndex));
                    return;
                } else if (prev.end == fromIndex && r.start == toIndex) {
                    runs.remove(r);
                    prev.end = r.end;
                    return;
                } else if (prev.end == fromIndex && r.start > toIndex) {
                    prev.end = toIndex;
                    return;
                } else if (prev.end < fromIndex && r.start == toIndex) {
                    r.start = fromIndex;
                    return;
                } else if (prev.contains(fromIndex) && r.contains(toIndex)) {
                    runs.remove(r);
                    prev.end = r.end;
                    return;
                } else if (prev.contains(fromIndex) && toIndex < r.start) {
                    prev.end = toIndex;
                    return;
                } else if (!prev.contains(fromIndex) && r.contains(toIndex)) {
                    r.start = fromIndex;
                    return;
                } else if (spanned != null && r.isWithin(fromIndex, toIndex)) {
//                    System.out.println("add sp 1 " + r + " is within " + fromIndex + ":" + toIndex);
                    spanned.add(r);
                }
            } else if (spanned != null) {
                if (r.contains(toIndex) || r.canContain(toIndex)) {
//                    System.out.println("add sp 2 " + r);
                    spanned.add(r);
                    break;
                } else if (r.isWithin(fromIndex, toIndex)) {
//                    System.out.println("add sp 3 " + r);
                    spanned.add(r);
                }
            }
            if (spanned == null && r.isWithin(fromIndex, toIndex)) {
                spanned = new LinkedList<>();
//                System.out.println("add sp 4 " + r);
                spanned.add(r);
            }
            prev = r;
        }
        if (spanned != null) {
//            System.out.println("REMOVING SPANNED " + spanned);
            Run nue = new Run(fromIndex, toIndex);
            runs.removeAll(spanned);
            runs.add(nue);
            return;
        }
    }

    public void set(int fromIndex, int toIndex) {
        this.set((long) fromIndex, (long) toIndex);
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        set((long) fromIndex, (long) toIndex, value);
    }

    @Override
    public void set(long fromIndex, long toIndex, boolean value) {
        if (value) {
            set(fromIndex, toIndex);
        } else {
            clear(fromIndex, toIndex);
        }
    }

    @Override
    public int min() {
        return Integer.MIN_VALUE;
    }

    @Override
    public int max() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long minLong() {
        return Long.MIN_VALUE;
    }

    @Override
    public long maxLong() {
        return Long.MAX_VALUE;
    }

    @Override
    public MutableBits newBits(long size) {
        return new RLEBits();
    }

    @Override
    public boolean isNativelyLongIndexed() {
        return true;
    }

    @Override
    public boolean get(long bitIndex) {
        Run run = runFor(bitIndex, true);
        if (run != null && run.contains(bitIndex)) {
            return true;
        }
        return false;
    }

    @Override
    public Bits get(long fromIndex, long toIndex) {
        RLEBits nue = new RLEBits();
        for (Run run : runs) {
            if (run.end <= fromIndex) {
                continue;
            }
            if (run.isWithin(fromIndex, toIndex)) {
                nue.runs.add(run.copy());
            } else if (run.contains(fromIndex)) {
                if (run.contains(toIndex)) {
                    nue.runs.add(new Run(fromIndex, toIndex));
                    break;
                } else {
                    nue.runs.add(new Run(fromIndex, run.end));
                }
            } else if (run.contains(toIndex)) {
                nue.runs.add(new Run(run.start, toIndex));
                break;
            }
        }
        return nue;
    }

    @Override
    public boolean isEmpty() {
        return runs.isEmpty();
    }

    @Override
    public int nextClearBit(int fromIndex) {
        long val = nextClearBitLong(fromIndex);
        if (val > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) val;
    }

    @Override
    public int nextSetBit(int fromIndex) {
        long val = nextSetBitLong(fromIndex);
        if (val > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) val;
    }

    @Override
    public int previousSetBit(int fromIndex) {
        long val = previousSetBitLong(fromIndex);
        if (val < Integer.MIN_VALUE) {
            return -1;
        }
        return (int) val;
    }

    @Override
    public int previousClearBit(int fromIndex) {
        long result = previousClearBitLong((long) fromIndex);
        if (result < Integer.MIN_VALUE) {
            return -1;
        }
        return (int) result;
    }

    @Override
    public long previousSetBitLong(long fromIndex) {
        SortedSet<Run> tail = runs.tailSet(new Run(fromIndex));
        if (tail != null && !tail.isEmpty()) {
            Run r = tail.first();
            if (r.contains(fromIndex)) {
                return fromIndex;
            }
        }
        SortedSet<Run> hs = runs.headSet(new Run(fromIndex));
        if (hs != null && !hs.isEmpty()) {
            Run head = hs.last();
            if (head.contains(fromIndex)) {
                return fromIndex;
            }
            return head.end - 1;
        }
        return -1;
    }

    @Override
    public long previousClearBitLong(long fromIndex) {
        if (isEmpty()) {
            return fromIndex;
        }
        Run r = runFor(fromIndex, false);
        if (r != null) {
            if (!r.contains(fromIndex)) {
                return fromIndex;
            } else {
                for (Run prev : reverseIterator(r)) {
                    if (!prev.abuts(r)) {
                        return r.start - 1L;
                    }
                    r = prev;
                }
            }
        }
        return -1;
    }

    @Override
    public long nextSetBitLong(long fromIndex) {
        if (isEmpty()) {
            return -1;
        }
        long first = first();
        if (fromIndex < first) {
            return first;
        }
        Run r = runFor(fromIndex, true);
        if (r != null && r.contains(fromIndex)) {
            return fromIndex;
        }
        if (r != null) {
            if (r.start >= fromIndex) {
                return r.start;
            }
            SortedSet<Run> tail = runs.tailSet(r);
            Iterator<Run> it = tail.iterator();
            Run prev = r;
            while (it.hasNext()) {
                Run next = it.next();
                if (next == r) {
                    continue;
                }
                if (next.start > fromIndex) {
                    return next.start;
                }
                if (next.contains(fromIndex)) {
                    return fromIndex;
                } else {
                    return next.start;
                }
            }
        } else if (r != null && r.start > fromIndex) {
            return r.start;
        }
        return -1;
    }

    @Override
    public long nextClearBitLong(long fromIndex) {
        Run r = runFor(fromIndex, true);
        if (r == null) {
            return fromIndex;
        }
        if (r.contains(fromIndex)) {
            SortedSet<Run> tail = runs.tailSet(r);
            Iterator<Run> it = tail.iterator();
            Run prev = r;
            while (it.hasNext()) {
                Run next = it.next();
                if (next == r) {
                    continue;
                }
                if (!prev.abuts(r)) {
                    return (int) prev.end;
                }
                if (!it.hasNext()) {
                    return (int) next.end;
                }
                prev = next;
            }
            return -1;
        } else {
            return fromIndex;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof Bits)) {
            return false;
        } else if (o instanceof RLEBits) {
            RLEBits other = (RLEBits) o;
            return other.runs.equals(runs);
        } else {
            Bits b = (Bits) o;
            return contentEquals(b);
        }
    }

    @Override
    public int forEachSetBitDescending(IntConsumer consumer) {
        Int ct = Int.create();
        forEachLongSetBitDescending(bit -> {
            if (bit >= Integer.MIN_VALUE && bit <= Integer.MAX_VALUE) {
                ct.increment();
                consumer.accept((int) bit);
            }
        });
        return ct.getAsInt();
    }

    @Override
    public int forEachSetBitDescending(IntPredicate consumer) {
        Int ct = Int.create();
        forEachLongSetBitDescending(bit -> {
            boolean offEnd = bit > Integer.MAX_VALUE;
            if (bit >= Integer.MIN_VALUE && !offEnd) {
                ct.increment();
                return consumer.test((int) bit);
            }
            return !offEnd;
        });
        return ct.getAsInt();
    }

    @Override
    public MutableBits get(int fromIndex, int toIndex) {
        return (MutableBits) get((long) fromIndex, (long) toIndex);
    }

    @Override
    public void clear() {
        runs.clear();
    }

    @Override
    public double sum(double[] values, int ifNot) {
        double sum = 0.0;
        for (Run r : runs) {
            int min = (int) Math.max(0L, r.start);
            if (min >= values.length) {
                return sum;
            }
            int max = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, Math.min(values.length, r.end)));
            for (int i = min; i < max; i++) {
                if (i != ifNot) {
                    sum += values[i];
                }
            }
        }
        return sum;
    }

    @Override
    public long cardinalityLong() {
        long result = 0;
        for (Run r : runs) {
            result += r.size();
        }
        return result;
    }

    @Override
    public LongSupplier asLongSupplier() {
        if (isEmpty()) {
            return () -> -1L;
        }
        return new LongSupplier() {
            Iterator<Run> iter = runs.iterator();
            LongSupplier supp;

            {
                if (iter.hasNext()) {
                    supp = iter.next().supp();
                } else {
                    supp = null;
                }
            }

            @Override
            public long getAsLong() {
                if (supp == null) {
                    return -1;
                }
                long val = supp.getAsLong();
                if (val == -1) {
                    if (iter.hasNext()) {
                        supp = iter.next().supp();
                        return supp.getAsLong();
                    } else {
                        supp = null;
                    }
                }
                return val;
            }
        };
    }

    @Override
    public IntSupplier asIntSupplier() {
        if (isEmpty()) {
            return () -> -1;
        }
        SortedSet<Run> hs = runs.tailSet(new Run(Integer.MIN_VALUE));
        return new IntSupplier() {
            Iterator<Run> iter = hs.iterator();
            LongSupplier curr = iter.hasNext() ? iter.next().supp() : null;

            @Override
            public int getAsInt() {
                if (curr == null) {
                    return -1;
                }
                long val = curr.getAsLong();
                if (val == -1L) {
                    if (iter.hasNext()) {
                        curr = iter.next().supp();
                        val = curr.getAsLong();
                    } else {
                        curr = null;
                    }
                }
                if (val > Integer.MAX_VALUE) {
                    curr = null;
                    val = -1;
                }
                return (int) val;
            }
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return CHARACTERISTICS;
    }

    Run runFor(long index, boolean backward) {
//        Run prev = null;
//        SortedSet<Run> set = runs.tailSet(new Run(index - 1));
//        for (Run r : set) {
//            if (r.contains(index) || r.canContain(index)) {
//                return r;
//            } else if (r.isAfter(index) && prev != null && !prev.isAfter(index)) {
//                return backward ? prev : r;
//            }
//            prev = r;
//        }
//        return null;

//        Run template = new Run(index);
//        SortedSet<Run> set = runs.headSet(template);
//        if (set != null && !set.isEmpty()) {
//            Run run = set.first();
//            if (run.contains(index) || !backward) {
//                return run;
//            }
//        }
//        set = runs.tailSet(template);
//        if (set != null) {
//
//        }
        Run prev = null;
        for (Run r : runs) {
            if (r.contains(index) || r.canContain(index)) {
                return r;
            } else if (r.isAfter(index) && prev != null && !prev.isAfter(index)) {
                return backward ? prev : r;
            }
            prev = r;
        }
        return null;
    }

    void checkInvariants() {
        Run prev = null;
        for (Run r : runs) {
            if (r.start == r.end) {
                throw new IllegalStateException("Have an empty range " + r.start + ":" + r.end);
            }
            if (r.start < r.end) {
                throw new IllegalStateException("Have a negative range");
            }
            if (prev != null) {
                if (prev.end > r.start) {
                    throw new IllegalStateException("Have overlapping ranges: " + prev + " to " + r);
                } else if (r.start < prev.end) {
                    throw new IllegalStateException("Having overlapping ranges: " + prev + " to " + r);
                }
            }
            prev = r;
        }
    }

    void update(long index, boolean isAdd) {
        if (isEmpty()) {
            if (isAdd) {
                runs.add(new Run(index));
            } else {
                return;
            }
        }
        Run prev = null;
        for (Run r : runs) {
            if (r.start > index + 1) {
//                System.out.println("out of range for " + (isAdd ? " add " : " remove ") + index + " at " + r);
                break;
            }
            if (r.canContain(index)) {
                if (isAdd) {
//                    System.out.println( r + " can contain and is add of " + index);
                    if (r.contains(index)) {
//                        System.out.println("  already present in " + r);
                        return;
                    } else {
//                        System.out.println("  add " + index + " to " + r + " prev is " + prev);
                        UpdateStatus res = r.put(index);
//                        System.out.println("    R is now " + r + " " + res);
                        if (prev != null && prev.abuts(r)) {
//                            System.out.println("   coalesce 1 " + prev + " and " + r);
                            prev.coalesce(r);
                            runs.remove(r);
                        } else if (prev == null) {
                            SortedSet<Run> tail = runs.tailSet(r);
                            Iterator<Run> iter = tail.iterator();
                            while (iter.hasNext()) {
                                Run next = iter.next();
                                if (next == r) {
                                    continue;
                                }
                                if (r.abuts(next)) {
                                    r.coalesce(next);
                                    runs.remove(next);
                                    break;
                                }
                            }
//                            Iterator<Run> piter = reverseIterator(r);
//                            if (piter.hasNext()) {
//                                prev = piter.next();
//                                if (prev.abuts(r)) {
//                                    prev.coalesce(r);
//                                    runs.remove(r);
//                                }
//                            }
                        }
                        return;
                    }
                } else if (r.contains(index)) {
//                    System.out.println("Have remove of " + index);
                    RemoveStatus stat = r.splitOrRemove(index, (a, b) -> {
//                        System.out.println("Split: " + " remove " + r + " add " + a + " and " + b);
                        runs.remove(r);
                        if (!a.isEmpty()) {
                            runs.add(a);
                        }
                        if (!b.isEmpty()) {
                            runs.add(b);
                        }
                    });
                    if (r.isEmpty()) {
                        runs.remove(r);
                    }
//                    System.out.println("  split or rem " + index + "gets " + stat);
                    return;
                }
            } else if (r.contains(index) && !isAdd) {
//                System.out.println("No go for " + index + ": " + r);
//                System.out.println("2 Have remove of " + index);
                RemoveStatus stat = r.splitOrRemove(index, (a, b) -> {
//                    System.out.println("Split: " + " remove " + r + " add " + a + " and " + b);
                    runs.remove(r);
                    if (!a.isEmpty()) {
                        runs.add(a);
                    }
                    if (!b.isEmpty()) {
                        runs.add(b);
                    }
                });
                if (r.isEmpty()) {
                    runs.remove(r);
                }
//                System.out.println("  split or rem " + index + "gets " + stat + " r was " + r);
                return;
            }
            prev = r;
        }
        if (isAdd) {
            Run run = new Run(index);
//            System.out.println("Add a fresh run for " + index + ": " + run);
            runs.add(run);
        }
    }

    static final class Run implements Iterable<Long>, Comparable<Run> {

        long start;
        long end;

        public Run(long start) {
            this.start = start;
            this.end = start + 1;
        }

        public Run(long start, long end) {
            this.start = start;
            this.end = end;
        }

        boolean eachBit(long first, long last, LongPredicate c) {
            if (first >= end || last < start) {
                return true;
            }
            for (long l = Math.max(start, first); l < Math.min(end, last); l++) {
                if (!c.test(l)) {
                    return false;
                }
            }
            return true;
        }

        boolean eachBit(long first, LongPredicate c) {
            if (first >= end) {
                return true;
            }
            for (long l = Math.max(start, first); l < end; l++) {
                if (!c.test(l)) {
                    return false;
                }
            }
            return true;
        }

        void eachBit(long first, LongConsumer c) {
            for (long l = Math.max(start, first); l < end; l++) {
                c.accept(l);
            }
        }

        LongSupplier supp() {
            return new LongSupplier() {
                long pos = start;

                @Override
                public long getAsLong() {
                    if (pos >= end) {
                        return -1;
                    }
                    return pos++;
                }
            };
        }

        boolean isWithin(long start, long end) {
            return this.start >= start && this.end <= end;
        }

        boolean is(long start, long end) {
            return start == this.start && end == this.end;
        }

        boolean isAfter(long val) {
            return start > val;
        }

        Run copy() {
            return new Run(start, end);
        }

        boolean abuts(Run other) {
            return start == other.end || other.start == end;
        }

        boolean canContain(long val) {
            return val == start - 1 || val == end;
        }

        boolean isEmpty() {
            return start >= end;
        }

        public RemoveStatus splitOrRemove(long removing, BiConsumer<Run, Run> c) {
            if (removing == end - 1) {
                end--;
                return isEmpty() ? RemoveStatus.CLEARED : RemoveStatus.UPDATED;
            } else if (removing == start) {
                start++;
                return isEmpty() ? RemoveStatus.CLEARED : RemoveStatus.UPDATED;
            } else if (contains(removing)) {
                Run ra = new Run(start, removing);
                Run rb = new Run(removing + 1, end);
                c.accept(ra, rb);
                return RemoveStatus.SPLIT;
            }
            return RemoveStatus.OUT_OF_RANGE;
        }

        public String toString() {
            if (end - start == 1) {
                return Long.toString(start);
            }
            return start + "-" + (end - 1);
        }

        public void coalesce(Run other) {
            start = Math.min(start, other.start);
            end = Math.max(end, other.end);
        }

        public boolean contains(long bit) {
            return bit >= start && bit < end;
        }

        public boolean contains(int bit) {
            return bit >= start && bit < end;
        }

        public void forEach(LongConsumer consumer) {
            for (long i = start; i < end; i++) {
                consumer.accept(i);
            }
        }

        public void forEachInt(IntConsumer consumer) {
            if (end > Integer.MAX_VALUE) {
                throw new IllegalStateException("Range outside that of int: "
                        + start + ":" + end);
            }
            for (int i = (int) start; i < end; i++) {
                consumer.accept(i);
            }
        }

        public long size() {
            return end - start;
        }

        public PrimitiveIterator.OfLong iterator() {
            return new PrimitiveIterator.OfLong() {
                long cursor = start;

                @Override
                public long nextLong() {
                    if (cursor >= end) {
                        throw new NoSuchElementException();
                    }
                    return cursor++;
                }

                @Override
                public boolean hasNext() {
                    return cursor < end;
                }
            };
        }

        public UpdateStatus put(long bit) {
            if (bit >= start && bit < end) {
                return UNCHANGED;
            }
            if (bit == end) {
                end++;
                return UPDATED;
            } else if (bit == start - 1) {
                start--;
                return UPDATED;
            }
            return OUT_OF_RANGE;
        }

        @Override
        public int compareTo(Run o) {
            int result = Long.compare(end, o.end);
            if (result == 0) {
                result = Long.compare(start, o.start);
            }
            return result;
        }

        @Override
        public int hashCode() {
            long res = start + (end * 1_000_000_000L);
            return (int) ((res << 32) ^ res);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || o.getClass() != Run.class) {
                return false;
            }
            Run r = (Run) o;
            return r.start == start && r.end == end;
        }
    }

    enum UpdateStatus {
        UPDATED,
        UNCHANGED,
        OUT_OF_RANGE;

        RemoveStatus toRemoveStatus() {
            switch (this) {
                case UPDATED:
                    return RemoveStatus.UPDATED;
                case OUT_OF_RANGE:
                    return RemoveStatus.OUT_OF_RANGE;
                case UNCHANGED:
                    return RemoveStatus.UNCHANGED;
                default:
                    throw new AssertionError(this);
            }
        }
    }

    enum RemoveStatus {
        SPLIT,
        CLEARED,
        UPDATED,
        UNCHANGED,
        OUT_OF_RANGE
    }
}
