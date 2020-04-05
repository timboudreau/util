package com.mastfrog.function.state;

/**
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface FloatBinaryOperator {

    float applyAsFloat(float left, float right);
}
