package com.mastfrog.bits;

/**
 * Used by some graph algorithms for summing across graphs.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface DoubleLongFunction {

    double apply(long value);
}
