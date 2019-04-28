package com.mastfrog.util.tree;

/**
 * Function which takes an int and returns a double.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
interface DoubleIntFunction {

    double apply(int i);
}
