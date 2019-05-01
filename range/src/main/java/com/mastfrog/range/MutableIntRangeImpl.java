package com.mastfrog.range;

import static com.mastfrog.range.RangeHolder.checkStartAndSize;

/**
 *
 * @author Tim Boudreau
 */
class MutableIntRangeImpl implements MutableIntRange<MutableIntRangeImpl> {

    private int start;
    private int size;

    MutableIntRangeImpl(int start, int size) {
        checkStartAndSize(start, size);
        this.start = start;
        this.size = size;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return start() + ":" + end() + "(" + size() + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.start;
        hash = 37 * hash + this.size;
        return hash;
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
    public boolean setStartAndSize(int start, int size) {
        checkStartAndSize(start, size);
        boolean result = start != this.start || size != this.size;
        this.start = start;
        this.size = size;
        return result;
    }

    @Override
    public MutableIntRangeImpl newRange(int start, int size) {
        return new MutableIntRangeImpl(start, size);
    }

    @Override
    public MutableIntRangeImpl newRange(long start, long size) {
        assert start <= Integer.MAX_VALUE;
        assert size <= Integer.MAX_VALUE && size >= 0;
        return new MutableIntRangeImpl((int) start, (int) size);
    }
}
