/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.function.state;

/**
 *
 * @author Tim Boudreau
 */
final class DblImpl implements Dbl {

    private double value;

    DblImpl() {

    }

    DblImpl(double initial) {
        this.value = initial;
    }

    @Override
    public void accept(double value) {
        this.value = value;
    }

    @Override
    public double getAsDouble() {
        return this.value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Dbl) {
            double otherVal = ((Dbl) o).getAsDouble();
            long a = Double.doubleToLongBits(value + 0.0);
            long b = Double.doubleToLongBits(otherVal + 0.0);
            return a == b;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long val = Double.doubleToLongBits(value);
        return (int) (val ^ val >> 32);
    }
}
