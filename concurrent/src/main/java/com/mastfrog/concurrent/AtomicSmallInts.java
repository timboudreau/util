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
package com.mastfrog.concurrent;

import com.mastfrog.function.IntQuadConsumer;
import com.mastfrog.function.IntQuadFunction;
import com.mastfrog.function.state.Lng;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Packs four 16-bit unsigned values into a single atomically updatable long.
 *
 * @author Tim Boudreau
 */
public class AtomicSmallInts implements Serializable {

    private static final AtomicLongFieldUpdater<AtomicSmallInts> UPD
            = AtomicLongFieldUpdater.newUpdater(AtomicSmallInts.class, "value");
    private volatile long value;

    public AtomicSmallInts() {

    }

    public AtomicSmallInts(int a, int b, int c, int d) {
        this.value = pack(a, b, c, d);
    }

    public int[] toArray() {
        int[] result = new int[4];
        get((a, b, c, d) -> {
            result[0] = a;
            result[1] = b;
            result[2] = c;
            result[3] = d;
        });
        return result;
    }

    /**
     * Get the current values.
     *
     * @param quad A consumer
     */
    public AtomicSmallInts get(IntQuadConsumer quad) {
        long val = UPD.get(this);
        quad.accept(unpackA(val), unpackB(val), unpackC(val), unpackD(val));
        return this;
    }

    /**
     * Call the passed function with the four internal values, returning the
     * result of calling that function.
     *
     * @param <T> The return type
     * @param func A function
     * @return The result of calling the function
     */
    public <T> T get(IntQuadFunction<T> func) {
        long val = longValue();
        return func.apply(unpackA(val), unpackB(val), unpackC(val), unpackD(val));
    }

    /**
     * Get the internal state as a long.
     *
     * @return A long
     */
    public long longValue() {
        return UPD.get(this);
    }

    /**
     * Set all of the values.
     *
     * @param a the first value
     * @param b The second value
     * @param c The third value
     * @param d the fourth value
     */
    public AtomicSmallInts set(int a, int b, int c, int d) {
        UPD.set(this, pack(a, b, c, d));;
        return this;
    }

    /**
     * Get the first value.
     *
     * @return a
     */
    public int a() {
        return unpackA(longValue());
    }

    /**
     * Get the second value.
     *
     * @return b
     */
    public int b() {
        return unpackB(longValue());
    }

    /**
     * Get the third value.
     *
     * @return c
     */
    public int c() {
        return unpackC(longValue());
    }

    /**
     * Get the fourth value.
     *
     * @return d
     */
    public int d() {
        return unpackD(longValue());
    }

    /**
     * Set the first value.
     *
     * @param a a
     * @return this
     */
    public AtomicSmallInts setA(int a) {
        update((oa, b, c, d, con) -> {
            con.accept(a, b, c, d);
        });
        return this;
    }

    /**
     * Set the second value.
     *
     * @param b b
     * @return this
     */
    public AtomicSmallInts setB(int b) {
        update((a, ob, c, d, con) -> {
            con.accept(a, b, c, d);
        });
        return this;
    }

    /**
     * Set the third value.
     *
     * @param c c
     * @return this
     */
    public AtomicSmallInts setC(int c) {
        update((a, b, oc, d, con) -> {
            con.accept(a, b, c, d);
        });
        return this;
    }

    /**
     * Set the fourth value.
     *
     * @param d d
     * @return this
     */
    public AtomicSmallInts setD(int d) {
        update((a, b, c, od, con) -> {
            con.accept(a, b, c, d);
        });
        return this;
    }

    /**
     * Update all of the values atomically.
     *
     * @param upd
     * @return this
     */
    public AtomicSmallInts update(ASIUpdater upd) {
        UPD.getAndUpdate(this, old -> {
            Lng lng = Lng.of(old);
            upd.update(unpackA(old), unpackB(old), unpackC(old),
                    unpackD(old), (a, b, c, d) -> {
                lng.set(pack(a, b, c, d));
            });
            return lng.getAsLong();
        });
        return this;
    }

    public interface ASIUpdater {

        void update(int a, int b, int c, int d, IntQuadConsumer newValue);
    }

    public int[] get() {
        int[] result = new int[4];
        get((a, b, c, d) -> {
            result[0] = a;
            result[1] = b;
            result[2] = c;
            result[3] = d;
        });
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        get((a, b, c, d) -> {
            sb.append(a).append(", ").append(b).append(", ")
                    .append(c).append(", ").append(d);
        });
        return sb.append(")").toString();
    }

    static int rangeCheck(int val) {
        if (val < 0 || val > 65535) {
            throw new IllegalArgumentException("Value out of range 0-65535: " + val);
        }
        return val;
    }

    static long pack(int a, int b, int c, int d) {
        return (checkRange(a) << 48)
                | (long) ((checkRange(b) & 0xFFFF_FFFF_FFFFL) << 32)
                | (long) ((checkRange(c) & 0xFFFF_FFFF_FFFFL) << 16)
                | (long) (checkRange(d) & 0xFFFF_FFFF_FFFFL);
    }

    static int unpackA(long value) {
        return (int) ((value >>> 48) & 0xFFFFL);
    }

    static int unpackB(long value) {
        return (int) ((value >>> 32) & 0xFFFFL);
    }

    static int unpackC(long value) {
        return (int) ((value >>> 16) & 0xFFFFL);
    }

    static int unpackD(long value) {
        return (int) (value & 0xFFFFL);
    }

    static long checkRange(int val) {
        if (val < 0 || val > 65535) {
            throw new IllegalArgumentException("Out of range 0-65535");
        }
        return val;
    }
}
