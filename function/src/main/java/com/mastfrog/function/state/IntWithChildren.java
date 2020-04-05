package com.mastfrog.function.state;

/**
 * An Int which, when incremented, also increments any children it has created,
 * to allow for offset nested counters that are updated in lock-step.
 *
 * @author Tim Boudreau
 */
public interface IntWithChildren extends Int {

    /**
     * Create a child instance which is incremented and decremented when this
     * one is, but can be set or reset independently.
     *
     * @return
     */
    public IntWithChildren child();
}
