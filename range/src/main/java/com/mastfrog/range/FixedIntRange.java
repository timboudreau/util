package com.mastfrog.range;

import static com.mastfrog.range.RangeHolder.checkStartAndSize;

/**
 *
 * @author Tim Boudreau
 */
final class FixedIntRange implements IntRange<FixedIntRange> {

    private final int start;
    private final int size;

    FixedIntRange(int start, int size) {
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
    public FixedIntRange newRange(int start, int size) {
        return new FixedIntRange(start, size);
    }

    @Override
    public FixedIntRange newRange(long start, long size) {
        return new FixedIntRange((int) start, (int) size);
    }

}
