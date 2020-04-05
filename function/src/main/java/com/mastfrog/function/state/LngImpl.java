package com.mastfrog.function.state;

/**
 * Just a holder for a long, for use where a counter needs to be incremented
 * within a functional interface.
 *
 * @author Tim Boudreau
 */
class LngImpl implements Lng {

    private long value;

    LngImpl() {

    }

    LngImpl(long initial) {
        this.value = initial;
    }

    @Override
    public long set(long val) {
        long old = value;
        value = val;
        return old;
    }

    @Override
    public long getAsLong() {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >> 32));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LngImpl && ((LngImpl) o).value == value
                || o instanceof Long && ((Long) o).longValue() == value;
    }
}
