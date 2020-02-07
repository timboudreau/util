package com.mastfrog.range;

/**
 * Relationship between two ranges which may not, may partially or may
 * completely overlap each other.
 *
 * @author Tim Boudreau
 */
public enum RangeRelation implements BoundaryRelation {
    /**
     * The range doing the testing does not overlap and occurs entirely before
     * the bounds of the second range.
     */
    BEFORE,
    /**
     * The range doing the testing contains the start point but not the end
     * point of the second range.
     */
    STRADDLES_START,
    /**
     * The range doing the testing entirely contains, but is not equal to, the
     * range being tested.
     */
    CONTAINS,
    /**
     * Two ranges have exactly the same start and size.
     */
    EQUAL,
    /**
     * The range doing the testing is entirely contained by, but is not equal
     * to, the range being tested.
     */
    CONTAINED,
    /**
     * The range doing the testing contains the end point but not the start
     * point of the second range.
     */
    STRADDLES_END,
    /**
     * The range doing the testing does not overlap and occurs entirely after
     * the bounds of the second range.
     */
    AFTER;

    @Override
    public boolean isOverlap() {
        return this != BEFORE && this != AFTER;
    }

    /**
     * Determine if this relationship is one of containment.
     *
     * @return true if this == CONTAINS or this == CONTAINED
     */
    public boolean isContainerContainedRelationship() {
        return this == CONTAINS || this == CONTAINED;
    }

    /**
     * Returns true if this relationship is one of partial overlap.
     *
     * @return true if the start or end point of a range is within this
     * one
     */
    public boolean isStraddle() {
        return this == STRADDLES_END || this == STRADDLES_START;
    }

    /**
     * Value used in comparing two ranges.
     *
     * @return The bias, -1, 0 or 1
     */
    public int bias() {
        switch (this) {
            case STRADDLES_START:
            case BEFORE:
            case CONTAINS:
                return -1;
            case STRADDLES_END:
            case AFTER:
            case CONTAINED:
                return 1;
            case EQUAL:
                return 0;
            default:
                throw new AssertionError(this);
        }
    }

    /**
     * Get the relationship between two ranges.
     *
     * @param aStart The first range's start
     * @param aEnd The first range's end
     * @param bStart The second ranges's start
     * @param bEnd The second range's end
     * @return The relationship
     */
    public static RangeRelation get(int aStart, int aEnd, int bStart, int bEnd) {
        if (aStart == bStart && aEnd == bEnd) {
            return EQUAL;
        } else if (aEnd <= bStart) {
            return BEFORE;
        } else if (aStart >= bEnd) {
            return AFTER;
        } else if (aStart <= bStart && aEnd >= bEnd) {
            return CONTAINS;
        } else if (bStart <= aStart && bEnd >= aEnd) {
            return CONTAINED;
        } else if (aStart < bStart && aEnd > bStart && aEnd < bEnd) {
            return STRADDLES_START;
        } else if (aStart > bStart && aStart < bEnd && aEnd > bEnd) {
            return STRADDLES_END;
        }
        throw new AssertionError(aStart + "," + aEnd + "," + bStart + "," + bEnd);
    }

    /**
     * Get the relationship between two ranges.
     *
     * @param aStart The first range's start
     * @param aEnd The first range's end
     * @param bStart The second ranges's start
     * @param bEnd The second range's end
     * @return The relationship
     */
    public static RangeRelation get(double aStart, double aEnd, double bStart, double bEnd) {
        if (aStart == bStart && aEnd == bEnd) {
            return EQUAL;
        } else if (aEnd <= bStart) {
            return BEFORE;
        } else if (aStart >= bEnd) {
            return AFTER;
        } else if (aStart <= bStart && aEnd >= bEnd) {
            return CONTAINS;
        } else if (bStart <= aStart && bEnd >= aEnd) {
            return CONTAINED;
        } else if (aStart < bStart && aEnd > bStart && aEnd < bEnd) {
            return STRADDLES_START;
        } else if (aStart > bStart && aStart < bEnd && aEnd > bEnd) {
            return STRADDLES_END;
        }
        throw new AssertionError(aStart + "," + aEnd + "," + bStart + "," + bEnd);
    }

    /**
     * Get the relationship between two ranges.
     *
     * @param aStart The first range's start
     * @param aEnd The first range's end
     * @param bStart The second ranges's start
     * @param bEnd The second range's end
     * @return The relationship
     */
    public static RangeRelation get(long aStart, long aEnd, long bStart, long bEnd) {
        if (aStart == bStart && aEnd == bEnd) {
            return EQUAL;
        } else if (aEnd <= bStart) {
            return BEFORE;
        } else if (aStart >= bEnd) {
            return AFTER;
        } else if (aStart <= bStart && aEnd >= bEnd) {
            return CONTAINS;
        } else if (bStart <= aStart && bEnd >= aEnd) {
            return CONTAINED;
        } else if (aStart < bStart && aEnd > bStart && aEnd < bEnd) {
            return STRADDLES_START;
        } else if (aStart > bStart && aStart < bEnd && aEnd > bEnd) {
            return STRADDLES_END;
        }
        throw new AssertionError(aStart + "," + aEnd + "," + bStart + "," + bEnd);
    }
}
