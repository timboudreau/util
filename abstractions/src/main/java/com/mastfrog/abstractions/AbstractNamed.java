package com.mastfrog.abstractions;


/**
 * Base class for things that are implement Named and want to use that
 * for their toString() implementation.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractNamed implements Named {

    protected AbstractNamed() {
        
    }

    @Override
    public final String toString() {
        return name();
    }
}
