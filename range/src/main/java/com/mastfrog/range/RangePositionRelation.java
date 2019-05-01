package com.mastfrog.range;

/**
 * Relationship between a range with a start and an end, and a coordinate which
 * might be in or outside that range.
 *
 * @author Tim Boudreau
 */
public enum RangePositionRelation implements BoundaryRelation {

    /**
     * The position is less than the start of the range.
     */
    BEFORE, 
    /**
     * The position is within the range.
     */
    IN,
    /**
     * The position is after the end of the range.
     */
    AFTER;

    @Override
    public boolean isOverlap() {
        return this == IN;
    }

    /**
     * Get the relationship between a position and a range.
     *
     * @param start The start of the range
     * @param end The end of the range
     * @param test The value to test
     * @return A relation
     */
    public static RangePositionRelation get(int start, int end, int test) {
        if (test < start) {
            return BEFORE;
        } else if (test >= end) {
            return AFTER;
        } else {
            return IN;
        }
    }

    /**
     * Get the relationship between a position and a range.
     *
     * @param start The start of the range
     * @param end The end of the range
     * @param test The value to test
     * @return A relation
     */
    public static RangePositionRelation get(long start, long end, long test) {
        if (test < start) {
            return BEFORE;
        } else if (test >= end) {
            return AFTER;
        } else {
            return IN;
        }
    }

    /**
     * Get the relationship between a position and a range.
     *
     * @param start The start of the range
     * @param end The end of the range
     * @param test The value to test
     * @return A relation
     */
    public static RangePositionRelation get(short start, short end, short test) {
        if (test < start) {
            return BEFORE;
        } else if (test >= end) {
            return AFTER;
        } else {
            return IN;
        }
    }
}
