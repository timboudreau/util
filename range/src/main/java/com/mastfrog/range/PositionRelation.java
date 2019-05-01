package com.mastfrog.range;

/**
 * Relationship between two positions.
 *
 * @author Tim Boudreau
 */
public enum PositionRelation implements BoundaryRelation {
    /**
     * The tested position is less than the reference one.
     */
    LESS, 
    /**
     * The tested position is greater than the reference one.
     */
    GREATER,
    /**
     * The tested position is equal to the reference one.
     */
    EQUAL;

    /**
     * Get the relationship between two numbers.
     *
     * @param a The first number
     * @param b The second number
     * @return A relation
     */
    public static PositionRelation relation(int a, int b) {
        return a < b ? LESS : a == b ? EQUAL : GREATER;
    }

    /**
     * Get the relationship between two numbers.
     *
     * @param a The first number
     * @param b The second number
     * @return A relation
     */
    public static PositionRelation relation(long a, long b) {
        return a < b ? LESS : a == b ? EQUAL : GREATER;
    }

        /**
     * Get the relationship between two numbers.
     *
     * @param a The first number
     * @param b The second number
     * @return A relation
     */
    public static PositionRelation relation(short a, short b) {
        return a < b ? LESS : a == b ? EQUAL : GREATER;
    }

    @Override
    public boolean isOverlap() {
        return this == EQUAL;
    }

}
