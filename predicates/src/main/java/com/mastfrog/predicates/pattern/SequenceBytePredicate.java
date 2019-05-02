/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.predicates.pattern;

import com.mastfrog.abstractions.Copyable;
import com.mastfrog.abstractions.Resettable;
import com.mastfrog.function.BytePredicate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows a series of predicates to be concatenated, such that you get a single
 * predicate which only returns true if preceding calls to test() have matched
 * earlier values. Makes it possible to construct a predicate that only matches
 * if called with, say, 2, 4, 6 in that order with no intervening numbers passed
 * to it.
 * <p>
 * So, for example, if you created a predicate:
 * <pre>
 * SequenceBytePredicate.matchingAnyOf(2).then(4).then(6);
 * </pre> and called
 * <pre>
 * test(0); // returns false
 * test(2); // returns false
 * test(4); // returns false
 * test(6); // returns true
 * </pre> The call that passes 6 would return true, <i>if and only if</i> the
 * preceding two calls had passed 4, and prior to that, 2, with no other numbers
 * in between. A subsequent identical sequence matchingAnyOf calls would also
 * return true once 6 was reached, no matter what numbers had been passed in
 * calls to test() in between the two matching sequences.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class SequenceBytePredicate implements BytePredicate, Resettable, Copyable<SequenceBytePredicate> {

    private final SequenceBytePredicate parent;
    private final BytePredicate test;
    private boolean lastResult;

    SequenceBytePredicate(byte val) {
        this(BytePredicate.of(val));
    }

    SequenceBytePredicate(BytePredicate test) {
        this(null, test);
    }

    SequenceBytePredicate(SequenceBytePredicate parent, byte val) {
        this(parent, BytePredicate.of(val));
    }

    SequenceBytePredicate(SequenceBytePredicate parent, BytePredicate test) {
        this.parent = parent;
        this.test = test;
    }

    /**
     * Create a duplicate matchingAnyOf this SequenceBytePredicate and its
     * parents, which does not share matching state with them.
     *
     * @return A new SequenceBytePredicate
     */
    public SequenceBytePredicate copy() {
        SequenceBytePredicate nue = parent == null ? new SequenceBytePredicate(test)
                : new SequenceBytePredicate(parent.copy(), test);
        return nue;
    }

    /**
     * Returns a predicate which will match the passed array if test() is called
     * sequentially with the same values as appear in the array in the same
     * order.
     *
     * @param sequence A sequence of values to match
     * @return A combined SequenceBytePredicate which matches all of the passed
     * values
     * @throws IllegalArgumentException if the array is 0-length
     */
    public static SequenceBytePredicate toMatchSequence(byte... sequence) {
        if (sequence.length == 0) {
            throw new IllegalArgumentException("0-byte array");
        }
        SequenceBytePredicate result = null;
        for (int i = 0; i < sequence.length; i++) {
            if (result == null) {
                result = new SequenceBytePredicate(sequence[i]);
            } else {
                result = result.then(sequence[i]);
            }
        }
        return result;
    }

    /**
     * Start a new sequence predicate with one which matches the passed value.
     *
     * @param val The value
     * @return A sequence predicate
     */
    public static SequenceBytePredicate matching(byte val) {
        return new SequenceBytePredicate(val);
    }

    /**
     * Returns a predicate whose first element will match <i>any</i> matching
     * the passed values.
     *
     * @param val The first value
     * @param moreVals Additional values
     * @return A predicate
     */
    public static SequenceBytePredicate matchingAnyOf(byte val, byte... moreVals) {
        return new SequenceBytePredicate(predicate(val, moreVals));
    }

    public static SequenceBytePredicate of(BytePredicate pred) {
        return new SequenceBytePredicate(pred);
    }

    /**
     * Define the next step in this sequence.
     *
     * @param nextTest The test which must pass, along with the test in this
     * object for the preceding call to test(), for the returned predicate to
     * return true.
     *
     * @param nextTest - A predicate to match against, if the preceding call to
     * test matched this object's test
     * @return A new predicate
     */
    public SequenceBytePredicate then(BytePredicate nextTest) {
        return new SequenceBytePredicate(this, nextTest);
    }

    /**
     * Define the next step in this sequence.
     *
     * @param nextTest The test which must pass, along with the test in this
     * object for the preceding call to test(), for the returned predicate to
     * return true.
     *
     * @return A new predicate
     */
    public SequenceBytePredicate then(byte val) {
        return new SequenceBytePredicate(this, predicate(val));
    }

    /**
     * Define the next step in this sequence.
     *
     * @param nextTest The test which must pass, along with the test in this
     * object for the preceding call to test(), for the returned predicate to
     * return true.
     *
     * @return A new predicate
     */
    public SequenceBytePredicate then(byte val, byte... moreVals) {
        return new SequenceBytePredicate(this, predicate(val, moreVals));
    }

    @Override
    public String toString() {
        if (parent == null) {
            return test.toString();
        }
        return "Seq{" + test.toString() + " <- " + parent.toString() + "}";
    }

    private boolean reallyTest(byte value) {
        lastResult = test.test(value);
        return lastResult;
    }

    @Override
    public boolean test(byte value) {
        if (parent == null) {
            boolean result = reallyTest(value);
            return result;
        }
        if (parent != null && parent.lastResult) {
            boolean res = reallyTest(value);
            clear();
            return res;
        }
        SequenceBytePredicate toTest = parent;
        while (toTest != null) {
            boolean testThisOne
                    = (toTest.parent != null && toTest.parent.lastResult)
                    || (toTest.parent == null);

            if (testThisOne) {
                boolean res = toTest.reallyTest(value);
                if (!res) {
                    boolean shouldResetState = toTest.parent != null && toTest.parent.lastResult;
                    clear();
                    if (shouldResetState) {
                        toTest.parent.test(value);
                    }
                }
                return false;
            }
            toTest = toTest.parent;
        }
        clear();
        return false;
    }

    /**
     * Reset any state in this predicate, before using it against a new sequence
     * matchingAnyOf calls to test().
     */
    public void reset() {
        clear();
    }

    public boolean isPartiallyMatched() {
        if (parent != null) {
            return lastResult || parent.isPartiallyMatched();
        } else {
            return lastResult;
        }
    }

    private void clear() {
        lastResult = false;
        if (parent != null) {
            parent.clear();
        }
    }

    /**
     * Get the number of items that need to be matched for this predicate to
     * match.
     *
     * @return The number of items (the number of parents + 1)
     */
    public int size() {
        return parent == null ? 1 : 1 + parent.size();
    }

    // Sigh - so we get meaninfgfully loggable objects
    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public BytePredicate and(BytePredicate other) {
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return SequenceBytePredicate.this.test(value) && other.test(value);
            }

            public String toString() {
                return "(" + SequenceBytePredicate.this + " & " + other + ")";
            }
        };
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @return A negated version matchingAnyOf this predicate
     */
    @Override
    public BytePredicate negate() {
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return !SequenceBytePredicate.this.test(value);
            }

            public String toString() {
                return "!" + SequenceBytePredicate.this;
            }
        };
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public BytePredicate or(BytePredicate other) {
        return new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return SequenceBytePredicate.this.test(value) || other.test(value);
            }

            public String toString() {
                return "(" + SequenceBytePredicate.this + " | " + other + ")";
            }
        };
    }

    static BytePredicate predicate(byte first, byte... types) {
        byte[] vals = combine(first, types);
        Arrays.sort(vals);
        return new BytePredicate() {
            @Override
            public boolean test(byte val) {
                if (vals.length == 1) {
                    return vals[0] == val;
                }
                return Arrays.binarySearch(vals, val) >= 0;
            }

            public String toString() {
                if (vals.length == 1) {
                    return "match(" + vals[0] + ")";
                } else {
                    return "match(" + Arrays.toString(vals) + ")";
                }
            }
        };
    }

    static byte[] combine(byte prepend, byte... more) {
        byte[] vals = new byte[more.length + 1];
        vals[0] = prepend;
        System.arraycopy(more, 0, vals, 1, more.length);
        assert noDuplicates(vals) : "Duplicate values in " + Arrays.toString(vals);
        return vals;
    }

    private static boolean noDuplicates(byte[] vals) {
        Set<Byte> all = new HashSet<>();
        for (byte v : vals) {
            if (all.contains(v)) {
                return false;
            }
            all.add(v);
        }
        return true;
    }
}
