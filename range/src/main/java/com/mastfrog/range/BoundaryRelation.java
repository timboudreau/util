package com.mastfrog.range;

/**
 * Interface for relations between positions and/or ranges.
 *
 * @see PositionRelation
 * @see RangeRelation
 * @see RangePositionRelation
 * @author Tim Boudreau
 */
public interface BoundaryRelation {

    /**
     * If true, this relationship indicates an overlap - shared coordinates -
     * with whatever else was tested.
     *
     * @return True if the tested coordinate(s) overlap(s)
     */
    boolean isOverlap();

}
