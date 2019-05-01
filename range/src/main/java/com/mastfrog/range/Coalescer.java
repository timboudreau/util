package com.mastfrog.range;

/**
 * Coalesces two overlapping ranges. The use case is a common situation best
 * described by how it occurs in syntax highlighting. Say you have a bunch of
 * things that don't know about each other, and each one can highlight a range
 * of text as being some color, font style, underline, or whatever. So you wind
 * up with a bunch of ranges each of which may overlap others. To actually paint
 * the text, you need to coalesce that down to a series of non-overlapping
 * ranges which combine the attributes of the ranges you were passed - and
 * ideally you need to do this in an efficient, performance-sensitive way. So,
 * if we have a range A from 100 to 120 with font style BOLD, and you have a
 * range B from 110 to 130 with font color BLUE, what you need is a range
 * 100:110-BOLD, 110-120:BLUE+BOLD and 120:130:BLUE.
 *
 * @author Tim Boudreau
 */
public interface Coalescer<R extends Range<? extends R>> {

    /**
     * Create a range with a new start and size which combines attributes of
     * others.
     *
     * @param a One range
     * @param b Another range
     * @param start The start location
     * @param size The size
     * @return A combination range of the specified start and size, which
     * somehow merges whatever data is associated with a and b
     */
    R combine(R a, R b, int start, int size);

    /**
     * Implementation for long-indexed. If you will really deal in ranges
     * containing offsets past <code>Integer.MAX_VALUE</code> you must override
     * this. The default implementation simply casts to int and will result in
     * errors in that case.
     *
     * @param a One range
     * @param b Another range
     * @param start A start point
     * @param size An end point
     * @return A combination range of the specified start and size, which
     * somehow merges whatever data is associated with a and b
     */
    default R combine(R a, R b, long start, long size) {
        return combine(a, b, (int) start, (int) size);
    }

    /**
     * Create a new range with a different size for the passed original range.
     * By default, delegates to <code>orig.newRange(start, size)</code>.
     *
     * @param orig The original range
     * @param start The start point
     * @param size The size
     * @return A range like the original, but with the specified start and size
     * @throws IllegalArgumentException if the bounds are invalid (negative
     * start or size, or passing the maximum representable value)
     */
    default R resized(R orig, int start, int size) {
        return orig.newRange(start, size);
    }
}
