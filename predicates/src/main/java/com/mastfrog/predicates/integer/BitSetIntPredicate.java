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
package com.mastfrog.predicates.integer;

import java.util.BitSet;
import java.util.function.IntPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class BitSetIntPredicate implements EnhIntPredicate {

    private final BitSet set;

    public BitSetIntPredicate(BitSet set, boolean copy) {
        this.set = copy ? (BitSet) set.clone() : set;
    }

    public BitSetIntPredicate(int[] items) {
        this(fromArray(items), false);
    }

    public BitSetIntPredicate(int first, int... more) {
        this(fromArray(first, more), false);
    }

    static BitSet fromArray(int[] items) {
        BitSet set = new BitSet(items.length);
        for (int i = 0; i < items.length; i++) {
            set.set(items[i]);
        }
        return set;
    }

    static BitSet fromArray(int first, int... more) {
        BitSet set = new BitSet(more.length + 1);
        set.set(first);
        for (int i = 0; i < more.length; i++) {
            set.set(more[i]);
        }
        return set;
    }

    @Override
    public boolean test(int value) {
        return value < 0 ? false : set.get(value);
    }

    @Override
    public EnhIntPredicate negate() {
        return new Negated(this);
    }

    boolean bitsEqual(int[] arr) {
        if (arr.length != size()) {
            return false;
        }
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < 0) {
                return false;
            }
            if (!set.get(arr[i])) {
                return false;
            }
        }
        return true;
    }

    int size() {
        return set.cardinality();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof ArrayPredicate || obj instanceof ArrayPredicateWithNames) {
            ArrayPredicate other = (ArrayPredicate) obj;
            if (other.negated) {
                return false;
            }
            return bitsEqual(other.vals);
        } else if (obj instanceof BitSetIntPredicate) {
            BitSetIntPredicate pred = (BitSetIntPredicate) obj;
            return set.equals(pred.set);
        } else if (obj instanceof SinglePredicate && size() == 1) {
            SinglePredicate sp = (SinglePredicate) obj;
            return set.get(sp.val);
        }
        return false;
    }

    private int preHash() {
        int hash = 7;

        int arraysHashCodeEquiv = 1;
        for (int element = set.nextSetBit(0); element >= 0; element = set.nextSetBit(element + 1)) {
            arraysHashCodeEquiv = 31 * arraysHashCodeEquiv + element;
        }

        hash = 59 * hash + arraysHashCodeEquiv;
        return hash;
    }

    @Override
    public int hashCode() {
        return preHash() * 59;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(set.cardinality() * 4).append('[');
        for (int bit = set.nextSetBit(0); bit >= 0; bit = set.nextSetBit(bit + 1)) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append(bit);
        }
        return sb.toString();
    }

    static final class Negated implements EnhIntPredicate {

        final BitSetIntPredicate orig;

        Negated(BitSetIntPredicate orig) {
            this.orig = orig;
        }

        boolean origEquals(IntPredicate other) {
            return orig.equals(other);
        }

        @Override
        public boolean test(int value) {
            return !orig.test(value);
        }

        @Override
        public String toString() {
            return "!" + orig.toString();
        }

        @Override
        public EnhIntPredicate negate() {
            return orig;
        }

        public int hashCode() {
            int hash = orig.preHash();
            hash = 59 * hash + 1;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (obj instanceof Negated) {
                Negated neg = (Negated) obj;
                return neg.orig.equals(orig);
            } else if (obj instanceof ArrayPredicate) {
                ArrayPredicate arr = (ArrayPredicate) obj;
                if (arr.negated) {
                    return orig.bitsEqual(arr.vals);
                }
            } else if (obj instanceof SinglePredicate) {
                SinglePredicate sp = (SinglePredicate) obj;
                if (sp.negated) {
                    return orig.test(sp.val);
                }
            }
            return false;
        }
    }
}
