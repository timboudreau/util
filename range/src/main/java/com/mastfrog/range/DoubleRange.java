/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
interface DoubleRange<OI extends DoubleRange<OI>> extends Range<OI> {

    double start();

    double end();

    default double size() {
        return end() - start();
    }

    @Override
    public default Number sizeValue() {
        return size();
    }

    @Override
    default boolean contains(Range<?> range) {
        return relationTo(range) == RangeRelation.CONTAINS;
    }

    @Override
    default boolean isContainedBy(Range<?> range) {
        return Range.super.isContainedBy(range); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Determine if this range is empty.
     *
     * @return True if the size is zero
     */
    default boolean isEmpty() {
        return start() == end();
    }

    /**
     * Get the relation between this range and another, describing its relative
     * position and if and how it overlaps.
     *
     * @param other Another range
     * @return A relation
     */
    @Override
    default RangeRelation relationTo(Range<?> other) {
        double myStart = start();
        double otherStart = other instanceof DoubleRange<?> ? ((DoubleRange<?>) other).start()
                : other.startValue().doubleValue();
        double end = end();
        double otherEnd = other instanceof DoubleRange<?> ? ((DoubleRange<?>) other).end()
                : otherStart + other.sizeValue().doubleValue();
        return RangeRelation.get(myStart, end, otherStart, otherEnd);
    }

    default RangePositionRelation relationTo(int position) {
        return RangePositionRelation.get(start(), end(), position);
    }

    default boolean containsBoundary(Range<?> range) {
        if (range instanceof DoubleRange<?>) {
            DoubleRange<?> dr = (DoubleRange<?>) range;
            return contains(dr.start()) || contains(dr.end());
        }
        if (matches(range)) {
            return false;
        }
        return contains(range.startValue().doubleValue())
                || contains(range.startValue().doubleValue() + range.sizeValue().doubleValue());
    }

    OI newRange(double start, double size);

    default OI gap(Range<?> range) {
        if (range == this) {
            return newRange(start(), 0D);
        }
        RangeRelation rel = relationTo(range);
        switch (rel) {
            case EQUAL:
            case CONTAINS:
            case STRADDLES_END:
                return newRange(range.startValue().doubleValue(), 0);
            case CONTAINED:
            case STRADDLES_START:
                return newRange(startValue().doubleValue(), 0);
            case AFTER:
            case BEFORE:
                double myStart = start();
                double mySize = size();
                double myEnd = myStart + mySize;
                double otherStart = range.startValue().doubleValue();
                double otherSize = range.sizeValue().doubleValue();
                double otherEnd = otherStart + otherSize;
                switch (rel) {
                    case BEFORE:
                        return newRange(myEnd, otherStart - myEnd);
                    case AFTER:
                        return newRange(otherEnd, myStart - otherEnd);
                }
                break;
            default:
                throw new AssertionError(rel);
        }
        return null;
    }

    @Override
    public default Number startValue() {
        return start();
    }

    @Override
    public default boolean abuts(Range<?> r) {
        if (r instanceof DoubleRange<?>) {
            DoubleRange<?> dr = (DoubleRange<?>) r;
            return start() == dr.end() || end() == dr.start();
        }
        return Range.super.abuts(r);
    }

    default List<OI> nonOverlap(Range<?> other) {
        if (other == this) {
            return Collections.emptyList();
        }
        double[] startStops = new double[0];
        if (!matches(other)) {
            double myStart = start();
            double myEnd = end();
            double myStop = myEnd - 0.00000001;
            double otherStart = other.startValue().doubleValue();
            double otherEnd = otherStart + other.sizeValue().doubleValue();
            double otherStop = otherEnd - 1;
            if (contains(other)) {
                if (myStart == otherStart) {
                    startStops = new double[]{otherEnd, myStop};
                } else if (myStop == otherStop) {
                    startStops = new double[]{myStart, otherStart - 1};
                } else {
                    startStops = new double[]{myStart, otherStart - 1, otherEnd, myStop};
                }
            } else if (other.contains(this)) {
                if (myStart == otherStart) {
                    startStops = new double[]{myEnd, otherStop};
                } else if (myStop == otherStop) {
                    startStops = new double[]{otherStart, myStart - 1};
                } else {
                    startStops = new double[]{otherStart, myStart - 1, myEnd, otherStop};
                }
            } else if (containsBoundary(other)) {
                if (contains(otherStop)) {
                    if (otherStop == myStop) {
                        startStops = new double[]{otherStart, myStart - 1};
                    } else {
                        startStops = new double[]{otherStart, myStart - 1, otherStop + 1, myStop};
                    }
                } else if (contains(otherStart)) {
                    if (myStart == otherStart) {
                        startStops = new double[]{myStart, otherStart - 1};
                    } else {
                        startStops = new double[]{myStart, otherStart - 1, myEnd, otherStop};
                    }
                }
            }
        }
        List<OI> result = new ArrayList<>(startStops.length / 2);
        for (int i = 0; i < startStops.length; i += 2) {
            double start = startStops[i];
            double stop = startStops[i + 1];
            result.add(newRange(start, stop - start + 0.000000001));
        }
        return result;
    }

}
