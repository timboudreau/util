package com.mastfrog.function.state;

/**
 *
 * @author Tim Boudreau
 */
final class BoolImpl implements Bool {

    private boolean value;

    BoolImpl(boolean initial) {
        value = initial;
    }

    BoolImpl() {
        this(false);
    }

    @Override
    public void accept(boolean val) {
        value = val;
    }

    @Override
    public boolean getAsBoolean() {
        return value;
    }

    @Override
    public int hashCode() {
        return value ? 1 : 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bool && ((Bool) o).getAsBoolean() == value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
