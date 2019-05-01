package com.mastfrog.range;

import static com.mastfrog.range.RangeHolder.checkStartAndSize;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class DataFixedLongRange<T> implements DataLongRange<T, DataFixedLongRange<T>>, Supplier<T> {

    private final long start;
    private final long size;
    private final T object;

    DataFixedLongRange(long start, long size, T object) {
        checkStartAndSize(start, size);
        this.start = start;
        this.size = size;
        this.object = object;
    }

    @Override
    public T get() {
        return object;
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
        return "'" + get() + "' " + start() + ":" + end() + " (" + size() + ")";
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
        } else if (obj instanceof Range<?>) {
            if (((Range) obj).isEmpty() && isEmpty()) {
                return true;
            }
            if (obj instanceof IntRange<?>) {
                IntRange<?> oi = (IntRange<?>) obj;
                return oi.start() == start() && oi.size() == size();
            } else if (obj instanceof LongRange<?>) {
                LongRange<?> li = (LongRange<?>) obj;
                return li.start() == start() && li.size() == size();
            }
            Range<?> r = (Range<?>) obj;
            return matches(r);
        }
        return false;
    }

    @Override
    public DataFixedLongRange<T> newRange(int start, int size) {
        return new DataFixedLongRange<>(start, size, object);
    }

    @Override
    public DataFixedLongRange<T> newRange(long start, long size) {
        return new DataFixedLongRange<>((int) start, (int) size, object);
    }

    @Override
    public DataFixedLongRange<T> newRange(int start, int size, T newObject) {
        return new DataFixedLongRange<>(start, size, newObject);
    }

    @Override
    public DataFixedLongRange<T> newRange(long start, long size, T newObject) {
        assert start <= Integer.MAX_VALUE;
        assert size <= Integer.MAX_VALUE && size >= 0;
        return new DataFixedLongRange<>((int) start, (int) size, newObject);
    }
}
