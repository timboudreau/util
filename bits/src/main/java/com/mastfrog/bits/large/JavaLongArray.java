package com.mastfrog.bits.large;

import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
class JavaLongArray implements LongArray {

    private long[] value;

    JavaLongArray(long[] value) {
        this.value = value;
    }

    JavaLongArray(long size) {
        this((int) size);
    }

    JavaLongArray(int size) {
        value = new long[(int) size];
    }

    JavaLongArray() {
        this(0);
    }

    @Override
    public Object clone() {
        return new JavaLongArray(Arrays.copyOf(value, value.length));
    }

    @Override
    public long size() {
        return value.length;
    }

    @Override
    public long get(long index) {
        return value[(int) index];
    }

    @Override
    public void set(long index, long value) {
        this.value[(int) index] = value;
    }

    @Override
    public void addAll(long[] longs) {
        long start = size();
        resize(size() + longs.length);
        System.arraycopy(longs, 0, value, (int) start, longs.length);
    }

    @Override
    public boolean isZeroInitialized() {
        return true;
    }

    @Override
    public long maxSize() {
        return Integer.MAX_VALUE;
    }

    @Override
    public LongArray copy() {
        return new JavaLongArray(Arrays.copyOf(value, value.length));
    }

    @Override
    public void fill(long start, long length, long value) {
        if (length == 0) {
            return;
        } else if (length == 1) {
            this.value[(int) start] = value;
            return;
        }
        Arrays.fill(this.value, (int) start, (int) (start + length), value);
    }

    @Override
    public void copy(long dest, LongArray from, long start, long length, boolean grow) {
        if (from instanceof JavaLongArray) {
            long[] ov = ((JavaLongArray) from).value;
            System.arraycopy(ov, (int) start, value, (int) dest, (int) length);
            return;
        }
        LongArray.super.copy(dest, from, start, length, grow);
    }

    @Override
    public long[] toLongArray() {
        return value;
    }

    @Override
    public long[] toLongArray(long len) {
        if (len == value.length) {
            return value;
        }
        return Arrays.copyOf(value, (int) len);
    }

    @Override
    public void resize(long size) {
        if (size != value.length) {
            value = Arrays.copyOf(value, (int) size);
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(value);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof JavaLongArray) {
            return Arrays.equals(((JavaLongArray) o).value, value);
        } else if (o instanceof LongArray) {
            LongArray la = (LongArray) o;
            if (la.size() == size()) {
                for (int i = 0; i < size(); i++) {
                    if (la.get(i) != get(i)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
