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

/**
 *
 * @author Tim Boudreau
 */
final class DoubleRangeImpl implements DoubleRange<DoubleRangeImpl> {

    private final double start;
    private final double end;

    public DoubleRangeImpl(double start, double end) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
    }

    @Override
    public double start() {
        return start;
    }

    @Override
    public double end() {
        return end;
    }

    @Override
    public Number startValue() {
        return start();
    }

    @Override
    public Number sizeValue() {
        return end();
    }

    @Override
    public DoubleRangeImpl newRange(int start, int size) {
        return new DoubleRangeImpl(start, start + size);
    }

    @Override
    public DoubleRangeImpl newRange(long start, long size) {
        return new DoubleRangeImpl(start, start + size);
    }

    @Override
    public DoubleRangeImpl newRange(double start, double size) {
        return new DoubleRangeImpl(start, start + size);
    }

    @Override
    public DoubleRangeImpl gap(Range<?> range) {

        return DoubleRange.super.gap(range); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long distance(Range<?> other) {
        if (other == this || isEmpty() || other.isEmpty()) {
            return -1;
        }
        if (other instanceof DoubleRange<?>) {
            DoubleRange<?> dr = (DoubleRange<?>) other;
            double myStart = start();
            double myEnd = end();
            double otherStart = dr.start();
            double otherEnd = dr.end();
            if (otherEnd == myStart) {
                return 0;
            } else if (otherEnd <= myStart) {
                return (long) (myStart - otherEnd);
            } else if (myEnd <= otherStart) {
                return (long) (otherStart - myEnd);
            } else {
                return -1;
            }
        } else if (other instanceof IntRange<?>) {
            IntRange<?> ir = (IntRange<?>) other;
            double myStart = start();
            double myEnd = end();
            int otherStart = ir.start();
            int otherEnd = ir.end();
            if (otherEnd == myStart) {
                return 0;
            } else if (otherEnd <= myStart) {
                return (long) (myStart - otherEnd);
            } else if (myEnd <= otherStart) {
                return (long) (otherStart - myEnd);
            } else {
                return -1;
            }
        } else if (other instanceof LongRange<?>) {
            LongRange<?> ir = (LongRange<?>) other;
            double myStart = start();
            double myEnd = end();
            long otherStart = ir.start();
            long otherEnd = ir.end();
            if (otherEnd == myStart) {
                return 0;
            } else if (otherEnd <= myStart) {
                return (long) (myStart - otherEnd);
            } else if (myEnd <= otherStart) {
                return (long) (otherStart - myEnd);
            } else {
                return -1;
            }
        }
        return DoubleRange.super.distance(other);
    }

    @Override
    public DoubleRangeImpl overlapWith(Range<?> other) {
        if (other == this) {
            return cast();
        }
        RangeRelation rel = relationTo(other);
        double oStart = other.startValue().doubleValue();
        double oEnd = oStart + other.sizeValue().doubleValue();
        double myStart = startValue().doubleValue();
        double myEnd = myStart + sizeValue().doubleValue();
        switch (rel) {
            case EQUAL:
            case CONTAINED:
                return cast();
            case CONTAINS:
                if (other.getClass() == getClass()) {
                    return (DoubleRangeImpl) other;
                }
                return newRange(oStart, oEnd - oStart);
            case STRADDLES_START:
                return newRange(oStart, myEnd - oStart);
            case STRADDLES_END:
                return newRange(myStart, oEnd - myStart);
            case AFTER:
            case BEFORE:
                return newRange(myStart, 0);
            default:
                throw new AssertionError(rel);
        }
    }
}
