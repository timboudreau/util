package com.mastfrog.range;

/**
 * Base interface for mutable ranges. Type specific subclasses methods should be
 * used where available.
 *
 * @author Tim Boudreau
 */
public interface MutableRange<OI extends MutableRange<OI>> extends Range<OI> {

    /**
     * Set the size and start.
     *
     * @param start
     * @param size
     * @throws IllegalArgumentException if the result will be a negative or
     * invalid size or negative start position
     * @return True if the size was altered
     */
    boolean setStartAndSizeValues(Number start, Number size);
}
