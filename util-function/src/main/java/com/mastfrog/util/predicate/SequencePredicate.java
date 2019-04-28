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
package com.mastfrog.util.predicate;

import com.mastfrog.util.preconditions.Checks;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Allows a series of predicates to be concatenated, such that you get a single
 * predicate which only returns true if preceding calls to test() have matched
 * earlier values. Makes it possible to construct a predicate that only matches
 * if called with, say, 2, 4, 6 in that order with no intervening numbers passed
 * to it.
 * <p>
 * So, for example, if you created a predicate:
 * <pre>
 * SequencePredicate.matchingAnyOf(2).then(4).then(6);
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
public final class SequencePredicate<T> implements Predicate<T>,
        ResettableCopyablePredicate<T, SequencePredicate<T>> {

    private final SequencePredicate<T> parent;
    private final Predicate<T> test;
    private boolean lastResult;

    SequencePredicate(T val) {
        this(predicate(val));
    }

    SequencePredicate(Predicate<T> test) {
        this(null, test);
    }

    SequencePredicate(SequencePredicate<T> parent, T val) {
        this(parent, predicate(val));
    }

    SequencePredicate(SequencePredicate<T> parent, Predicate<T> test) {
        Checks.notNull("test", test);
        this.parent = parent;
        this.test = test;
    }

    /**
     * Create a duplicate matchingAnyOf this SequenceIntPredicate and its
     * parents, which does not share matching state with them.
     *
     * @return A new SequenceIntPredicate
     */
    public SequencePredicate<T> copy() {
        SequencePredicate<T> nue = parent == null ? new SequencePredicate<>(test)
                : new SequencePredicate<>(parent.copy(), test);
        return nue;
    }

    /**
     * Returns a predicate which will match the passed array if test() is called
     * sequentially with the same values as appear in the array in the same
     * order.
     *
     * @param sequence A sequence matching values to match
     * @return A combined SequenceBytePredicate which matches all matching the
     * passed values
     * @throws IllegalArgumentException if the array is 0-length
     */
    @SafeVarargs
    public static <T> SequencePredicate<T> toMatchSequence(T... sequence) {
        Checks.notNull("sequence", sequence);
        if (sequence.length == 0) {
            throw new IllegalArgumentException("0-byte array");
        }
        SequencePredicate<T> result = null;
        for (int i = 0; i < sequence.length; i++) {
            if (result == null) {
                result = new SequencePredicate<>(sequence[i]);
            } else {
                result = result.then(sequence[i]);
            }
        }
        return result;
    }

    /**
     * Returns a predicate which will match the passed array if test() is called
     * sequentially with the same values as appear in the array in the same
     * order.
     *
     * @param sequence A sequence matching values to match
     * @return A combined SequenceBytePredicate which matches all matching the
     * passed values
     * @throws IllegalArgumentException if the array is 0-length
     */
    public static <T> SequencePredicate<T> toMatchSequence(List<T> sequence) {
        if (Checks.notNull("sequence", sequence).isEmpty()) {
            throw new IllegalArgumentException("0-length collection");
        }
        SequencePredicate<T> result = null;
        for (int i = 0; i < sequence.size(); i++) {
            if (result == null) {
                result = new SequencePredicate<>(sequence.get(i));
            } else {
                result = result.then(sequence.get(i));
            }
        }
        return result;
    }

    /**
     * Start a new predicate with one which matches the passed value (call
     * then() to match additional subsequent values).
     *
     * @param val The initial value to match
     * @return a predicate
     */
    public static <T> SequencePredicate<T> matching(T val) {
        Checks.notNull("val", val);
        return new SequencePredicate<>(val);
    }

    /**
     * Returns a starting predicate whose first element will match <i>any</i>
     * matching the passed values.
     *
     * @param val The first value
     * @param moreVals Additional values
     * @return A predicate
     */
    @SafeVarargs
    public static <T> SequencePredicate<T> matchingAnyOf(T val, T... moreVals) {
        return new SequencePredicate<>(predicate(Checks.notNull("val", val),
                Checks.notNull("moreVals", moreVals)));
    }

    /**
     * Wrap a plain predicate as a SequenceIntPredicate.
     *
     * @param pred The original predicate
     * @return A new sequential predicate
     */
    public static <T> SequencePredicate<T> of(Predicate<T> pred) {
        Checks.notNull("pred", pred);
        return new SequencePredicate<>(pred);
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
    public SequencePredicate<T> then(Predicate<T> nextTest) {
        return new SequencePredicate<>(this, Checks.notNull("nextTest", nextTest));
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
    public SequencePredicate<T> then(T val) {
        return new SequencePredicate<>(this, predicate(val));
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
    @SafeVarargs
    public final SequencePredicate<T> then(T val, T... moreVals) {
        return new SequencePredicate<>(this, predicate(val, moreVals));
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

    @Override
    public String toString() {
        if (parent == null) {
            return test.toString();
        }
        return "Seq{" + test.toString() + " <- " + parent.toString() + "}";
    }

    private boolean reallyTest(T value) {
        lastResult = test.test(value);
        return lastResult;
    }

    @Override
    public boolean test(T value) {
        if (parent == null) {
            boolean result = reallyTest(value);
            return result;
        }
        if (parent != null && parent.lastResult) {
            boolean res = reallyTest(value);
            clear();
            return res;
        }
        SequencePredicate<T> toTest = parent;
        while (toTest != null) {
            boolean testThisOne
                    = (toTest.parent != null && toTest.parent.lastResult)
                    || (toTest.parent == null);

            if (testThisOne) {
                boolean res = toTest.reallyTest(value);
                if (!res) {
                    // We have a repetition of a partial value
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
    public SequencePredicate<T> reset() {
        clear();
        return this;
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

    // Sigh - so we get meaninfgfully loggable objects
    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public ResettableCopyablePredicate<T, ?> and(Predicate<? super T> other) {
        Checks.notNull("other", other);
        return new LogicalSequencePredicate<>(copy(), other, true);
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @return A negated version matchingAnyOf this predicate
     */
    @Override
    public ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> negate() {
        return new NegatedSequencePredicate<>(copy());
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> or(Predicate<? super T> other) {
        Checks.notNull("other", other);
        return new LogicalSequencePredicate<>(copy(), other, false);
    }

    @SafeVarargs
    static <T> Predicate<T> predicate(T first, T... types) {
        Checks.notNull("first", first);
        Set<T> all = new HashSet<>(Arrays.asList(types));
        all.add(first);
        assert types.getClass().getComponentType().isInstance(first);
        return new Predicate<T>() {
            @Override
            public boolean test(T val) {
                return all.contains(val);
            }

            public String toString() {
                if (all.size() == 1) {
                    return "match(" + all.iterator().next() + ")";
                } else {
                    return "matchAny(" + all + ")";
                }
            }
        };
    }

    private static final class NegatedSequencePredicate<T> implements ResettableCopyablePredicate<T, NegatedSequencePredicate<T>> {

        private final ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> owner;

        public NegatedSequencePredicate(ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> owner) {
            this.owner = owner;
        }

        @Override
        public boolean test(T t) {
            return !owner.test(t);
        }

        @Override
        public NegatedSequencePredicate<T> reset() {
            owner.reset();
            return this;
        }

        @Override
        public NegatedSequencePredicate<T> copy() {
            return new NegatedSequencePredicate<>(owner.copy());
        }

        public String toString() {
            return "not(" + owner + ")";
        }

        @Override
        public ResettableCopyablePredicate<T, ?> and(Predicate<? super T> other) {
            return new LogicalSequencePredicate<>(copy(), other, true);
        }

        @Override
        public ResettableCopyablePredicate<T, ?> or(Predicate<? super T> other) {
            return new LogicalSequencePredicate<>(copy(), other, false);
        }

        @Override
        public ResettableCopyablePredicate<T, ?> negate() {
            return owner.copy();
        }
    }

    private static class LogicalSequencePredicate<T> implements ResettableCopyablePredicate<T, LogicalSequencePredicate<T>> {

        private final Predicate<? super T> other;
        private final ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> owner;
        private final boolean and;

        public LogicalSequencePredicate(ResettableCopyablePredicate<T, ? extends ResettableCopyablePredicate<T, ?>> owner, Predicate<? super T> other, boolean and) {
            this.owner = owner;
            this.other = other;
            this.and = and;
        }

        @Override
        public ResettableCopyablePredicate<T, ?> and(Predicate<? super T> other) {
            return new LogicalSequencePredicate<>(copy(), other, true);
        }

        @Override
        public ResettableCopyablePredicate<T, ?> or(Predicate<? super T> other) {
            return new LogicalSequencePredicate<>(copy(), other, false);
        }

        @Override
        public ResettableCopyablePredicate<T, ?> negate() {
            return new NegatedSequencePredicate<>(this);
        }

        @Override
        public boolean test(T value) {
            if (and) {
                return owner.test(value) && other.test(value);
            } else {
                return owner.test(value) || other.test(value);
            }
        }

        @Override
        public String toString() {
            return "(" + owner + (and ? " & " : " | ") + other + ")";
        }

        @Override
        public LogicalSequencePredicate<T> reset() {
            if (other instanceof Resettable<?>) {
                ((Resettable<?>) other).reset();
            }
            owner.reset();
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public LogicalSequencePredicate<T> copy() {
            Predicate<? super T> o = other;
            if (other instanceof Copyable<?>) {
                o = (Predicate<? super T>) ((Copyable<?>) o).copy();
            }
            return new LogicalSequencePredicate<>(owner, o, and);
        }
    }
}
