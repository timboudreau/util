package com.mastfrog.range;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A linked list structure that can coalesce with limited lookback.
 *
 * @author Tim Boudreau
 */
class RangeHolder<R extends Range<R>> {

    private R range;
    private RangeHolder<R> parent;

    RangeHolder(R range, RangeHolder<R> parent) {
        this.range = range;
        this.parent = parent;
    }

    RangeHolder(R range) {
        this.range = range;
        this.parent = null;
    }

    @Override
    public String toString() {
        if (parent != null) {
            return parent.toString() + ", " + range;
        }
        return range.toString();
    }

    int size() {
        return size(1);
    }

    private int size(int size) {
        return parent == null ? size : parent.size(size + 1);
    }

    RangeHolder<R> insertBefore(R range) {
        RangeHolder<R> nue = new RangeHolder<>(range, parent);
        this.parent = nue;
        return this;
    }

    RangeHolder<R> insertAfter(R range) {
        return new RangeHolder<>(range, this);
    }

    RangeHolder<R> replace(List<R> ranges) {
        if (ranges.isEmpty()) {
            return parent;
        }
        if (ranges.size() == 1) {
            range = ranges.get(0);
            return this;
        }
        this.range = ranges.get(0);
        RangeHolder<R> result = this;
        for (int i = 1; i < ranges.size(); i++) {
            result = result.insertAfter(ranges.get(i));
        }
        return result;
    }

    boolean overlaps(R range) {
        boolean result = (range != this.range && this.range.overlaps(range))
                || (parent != null && parent.overlaps(range));
//        System.out.println(this + " overlaps " + range + "? " + result);
        return result;
    }

    RangeHolder<R> coalesce(R range, Coalescer<R> c) {
        if (range == this.range) {
            return this;
        }
        RangeRelation rel = this.range.relationTo(range);
        if (rel == RangeRelation.BEFORE) {
            RangeHolder<R> result = insertAfter(range);
//            System.out.println("insertAfter " + range + " before " + this.range + " to get " + result);
            return result;
        }

        if (parent != null && parent.overlaps(range)) {
            RangeHolder<R> newEnd = parent.coalesce(range, c);
            parent = newEnd;
//            System.out.println("revise to " + this);
            RangeHolder<R> result = this;
            if (this.range.overlaps(newEnd.range)) {
//                System.out.println("Do repl " + newEnd.range + " and " + this.range);
                List<R> replacements = this.range.coalesce(newEnd.range, c);
                parent.range = replacements.get(0);
                return replace(replacements.subList(1, replacements.size()));
            }
            return result;
        } else if (overlaps(range)) {
            List<R> replacements = this.range.coalesce(range, c);
//            System.out.println(this.range + " and " + range + "  coalesce to "
//                    + replacements + " types " + this.range.getClass().getSimpleName()
//                    + " and " + range.getClass().getSimpleName());

            RangeHolder<R> result = replace(replacements);
//            System.out.println("I am now " + result);
            return result;
        } else {
//            System.out.println("fallthrough " + range);
            return this;
        }
    }

    List<R> toList() {
        LinkedList<R> result = new LinkedList<>();
        populate(result);
        Collections.sort(result);
        return result;
    }

    private void populate(List<R> l) {
        l.add(0, range);
        if (parent != null) {
            parent.populate(l);
        }
    }

    static void checkStartAndSize(int start, int size) {
        if (start < 0) {
            throw new IllegalArgumentException("Negative start " + start + " with size " + size);
        }
        if (size < 0) {
            throw new IllegalArgumentException("Negative size " + size + " with start " + start);
        }
        if ((long) start + (long) size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Start " + start + " + "
                    + size + " > Integer.MAX_VALUE");
        }
    }

    static void checkStartAndSize(long start, long size) {
        if (start < 0) {
            throw new IllegalArgumentException("Negative start " + start + " with size " + size);
        }
        if (size < 0) {
            throw new IllegalArgumentException("Negative size " + size + " with start " + start);
        }
        if (Long.MAX_VALUE - start < size) {
            throw new IllegalArgumentException("Start " + start + " + "
                    + size + " > Long.MAX_VALUE");
        }
    }
}
