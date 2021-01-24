/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

/**
 * Just a holder for an integer, for use where a counter needs to be incremented
 * within a functional interface.
 *
 * @author Tim Boudreau
 */
class IntImpl implements Int {

    private int value;

    IntImpl() {

    }

    IntImpl(int initial) {
        this.value = initial;
    }

    @Override
    public int set(int val) {
        int old = value;
        value = val;
        return old;
    }

    @Override
    public int getAsInt() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IntImpl && ((IntImpl) o).value == value
                || o instanceof Integer && ((Integer) o).intValue() == value;
    }
}
