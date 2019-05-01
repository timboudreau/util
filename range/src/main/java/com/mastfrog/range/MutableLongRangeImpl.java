package com.mastfrog.range;

import static com.mastfrog.range.RangeHolder.checkStartAndSize;

/**
 *
 * @author Tim Boudreau
 */
class MutableLongRangeImpl implements MutableLongRange<MutableLongRangeImpl> {

    private long start;
    private long size;

    MutableLongRangeImpl(long start, long size) {
        checkStartAndSize(start, size);
        this.start = start;
        this.size = size;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public String toString() {
        return start() + ":" + end() + "(" + size() + ")";
    }

    @Override
    public int hashCode() {
        long hash = 7;
        hash = 37 * hash + this.start;
        hash = 37 * hash + this.size;
        return (int) (hash ^ (hash << 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof Range) {
            if (((Range) obj).isEmpty() && isEmpty()) {
                return true;
            }
            if (obj instanceof IntRange) {
                IntRange oi = (IntRange) obj;
                return oi.start() == start() && oi.size() == size();
            } else if (obj instanceof LongRange) {
                LongRange li = (LongRange) obj;
                return li.start() == start() && li.size() == size();
            }
            Range r = (Range) obj;
            return matches(r);
        }
        return false;
    }

    @Override
    public boolean setStartAndSize(long start, long size) {
        checkStartAndSize(start, size);
        boolean result = start != this.start || size != this.size;
        this.start = start;
        this.size = size;
        return result;
    }

    @Override
    public MutableLongRangeImpl newRange(int start, int size) {
        return new MutableLongRangeImpl(start, size);
    }

    @Override
    public MutableLongRangeImpl newRange(long start, long size) {
        return new MutableLongRangeImpl((int) start, (int) size);
    }
}
