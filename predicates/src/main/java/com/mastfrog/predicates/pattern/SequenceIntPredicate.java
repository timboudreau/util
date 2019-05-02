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

import com.mastfrog.predicates.integer.IntPredicates;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Allows a series of predicates to be concatenated, such that you get a single
 * predicate which only returns true if preceding calls to test() have matched
 * earlier values. Makes it possible to construct a predicate that only matches
 * if called with, say, 2, 4, 6 in that order with no intervening numbers passed
 * to it.
 * <p>
 * So, for example, if you created a predicate:
 * <pre>
 * SequenceIntPredicate.matchingAnyOf(2).then(4).then(6);
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
public final class SequenceIntPredicate implements ResettableCopyableIntPredicate<SequenceIntPredicate> {

    private final SequenceIntPredicate parent;
    private final IntPredicate test;
    private boolean lastResult;

    SequenceIntPredicate(int val) {
        this(IntPredicates.matching(val));
    }

    SequenceIntPredicate(IntPredicate test) {
        this(null, test);
    }

    SequenceIntPredicate(SequenceIntPredicate parent, int val) {
        this(parent, IntPredicates.matching(val));
    }

    SequenceIntPredicate(SequenceIntPredicate parent, IntPredicate test) {
        this.parent = parent;
        this.test = test;
    }

    /**
     * Create a duplicate matchingAnyOf this SequenceIntPredicate and its
     * parents, which does not share matching state with them.
     *
     * @return A new SequenceIntPredicate
     */
    public SequenceIntPredicate copy() {
        IntPredicate newTest = test;
        if (newTest instanceof SequenceIntPredicate) {
            newTest = ((SequenceIntPredicate) newTest).copy();
        }
        SequenceIntPredicate nue = parent == null ? new SequenceIntPredicate(test)
                : new SequenceIntPredicate(parent.copy(), newTest);
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
    public static SequenceIntPredicate toMatchSequence(int... sequence) {
        if (sequence.length == 0) {
            throw new IllegalArgumentException("0-byte array");
        }
        SequenceIntPredicate result = null;
        for (int i = 0; i < sequence.length; i++) {
            if (result == null) {
                result = new SequenceIntPredicate(sequence[i]);
            } else {
                result = result.then(sequence[i]);
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
    public static SequenceIntPredicate matching(int val) {
        return new SequenceIntPredicate(val);
    }

    /**
     * Returns a starting predicate whose first element will match <i>any</i>
     * matching the passed values.
     *
     * @param val The first value
     * @param moreVals Additional values
     * @return A predicate
     */
    public static SequenceIntPredicate matchingAnyOf(int val, int... moreVals) {
        if (moreVals.length == 0) {
            return matching(val);
        }
        return new SequenceIntPredicate(IntPredicates.anyOf(val, moreVals));
    }

    /**
     * Wrap a plain predicate as a SequenceIntPredicate.
     *
     * @param pred The original predicate
     * @return A new sequential predicate
     */
    public static SequenceIntPredicate of(IntPredicate pred) {
        return new SequenceIntPredicate(pred);
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
    public SequenceIntPredicate then(IntPredicate nextTest) {
        return new SequenceIntPredicate(this, nextTest);
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
    public SequenceIntPredicate then(int val) {
        return new SequenceIntPredicate(this, IntPredicates.matching(val));
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
    public SequenceIntPredicate then(int val, int... moreVals) {
        return new SequenceIntPredicate(this, IntPredicates.anyOf(val, moreVals));
    }

    @Override
    public String toString() {
        if (parent == null) {
            return test.toString();
        }
        return "Seq{" + parent.toString() + " -> " + test.toString() + "}";
    }

    private boolean reallyTest(int value) {
        lastResult = test.test(value);
        return lastResult;
    }

    @Override
    public boolean test(int value) {
        if (parent == null) {
            boolean result = reallyTest(value);
            return result;
        }
        if (parent != null && parent.lastResult) {
            boolean res = reallyTest(value);
            clear();
            return res;
        }
        SequenceIntPredicate toTest = parent;
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
    @Override
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
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public ResettableCopyableIntPredicate<?> and(IntPredicate other) {
        return new LogicalPredicateImpl(copy(), other, true);
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @return A negated version matchingAnyOf this predicate
     */
    @Override
    public ResettableCopyableIntPredicate<?> negate() {
        return new NegatingImpl(this);
    }

    /**
     * Overridden to return a predicate with a reasonable implementation
     * matchingAnyOf toString().
     *
     * @param other Another predicate
     * @return a new predicate
     */
    @Override
    public ResettableCopyableIntPredicate<?> or(IntPredicate other) {
        return new LogicalPredicateImpl(copy(), other, false);
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
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.parent);
        hash = 79 * hash + Objects.hashCode(this.test);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SequenceIntPredicate other = (SequenceIntPredicate) obj;
        if (!Objects.equals(this.parent, other.parent)) {
            return false;
        }
        if (!Objects.equals(this.test, other.test)) {
            return false;
        }
        return true;
    }

    private static final class NegatingImpl implements ResettableCopyableIntPredicate<NegatingImpl> {

        private final ResettableCopyableIntPredicate<?> delegate;

        public NegatingImpl(ResettableCopyableIntPredicate<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public NegatingImpl copy() {
            return new NegatingImpl(delegate.copy());
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public boolean test(int value) {
            return !delegate.test(value);
        }

        @Override
        public ResettableCopyableIntPredicate<?> and(IntPredicate other) {
            return new LogicalPredicateImpl(copy(), other, true);
        }

        @Override
        public ResettableCopyableIntPredicate<?> negate() {
            return delegate.copy();
        }

        @Override
        public ResettableCopyableIntPredicate<?> or(IntPredicate other) {
            return new LogicalPredicateImpl(copy(), other, false);
        }

        public String toString() {
            return "not(" + delegate + ")";
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + Objects.hashCode(this.delegate);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NegatingImpl other = (NegatingImpl) obj;
            if (!Objects.equals(this.delegate, other.delegate)) {
                return false;
            }
            return true;
        }
    }

    private static final class LogicalPredicateImpl implements ResettableCopyableIntPredicate<LogicalPredicateImpl> {

        private final ResettableCopyableIntPredicate<?> delegate;
        private final IntPredicate other;
        private final boolean and;

        public LogicalPredicateImpl(ResettableCopyableIntPredicate copy, IntPredicate other, boolean and) {
            this.delegate = copy;
            this.other = other;
            this.and = and;
        }

        public ResettableCopyableIntPredicate<?> or(IntPredicate predicate) {
            return new LogicalPredicateImpl(this, predicate, false);
        }

        public ResettableCopyableIntPredicate<?> and(IntPredicate predicate) {
            return new LogicalPredicateImpl(this, predicate, true);
        }

        @Override
        public boolean test(int value) {
            if (and) {
                return delegate.test(value) && other.test(value);
            } else {
                return delegate.test(value) || other.test(value);
            }
        }

        @Override
        public String toString() {
            return "(" + delegate + (and ? " & " : " | ") + other + ")";
        }

        @Override
        public LogicalPredicateImpl copy() {
            ResettableCopyableIntPredicate delegateCopy = delegate.copy();
            IntPredicate otherCopy = other;
            if (otherCopy instanceof ResettableCopyableIntPredicate<?>) {
                otherCopy = ((ResettableCopyableIntPredicate<?>) otherCopy).copy();
            }
            return new LogicalPredicateImpl(delegateCopy, otherCopy, and);
        }

        @Override
        public void reset() {
            delegate.reset();
            if (other instanceof ResettableCopyableIntPredicate<?>) {
                ((ResettableCopyableIntPredicate<?>) other).reset();
            }
        }

        @Override
        public ResettableCopyableIntPredicate<?> negate() {
            return new NegatingImpl(copy());
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.delegate);
            hash = 37 * hash + Objects.hashCode(this.other);
            hash = 37 * hash + (this.and ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LogicalPredicateImpl other = (LogicalPredicateImpl) obj;
            if (this.and != other.and) {
                return false;
            }
            if (!Objects.equals(this.delegate, other.delegate)) {
                return false;
            }
            if (!Objects.equals(this.other, other.other)) {
                return false;
            }
            return true;
        }
    }

    public static <T> SequenceIntPredicateBuilder<T> builder(
            Function<SequenceIntPredicate, T> converter) {
        return new SequenceIntPredicateBuilder<>(converter);
    }

    public static final class SequenceIntPredicateBuilder<T> {

        private final Function<SequenceIntPredicate, T> converter;

        public SequenceIntPredicateBuilder(
                Function<SequenceIntPredicate, T> converter) {
            this.converter = converter;
        }

        public FinishableSequenceIntPredicateBuilder<T> startingWith(int val) {
            return new FinishableSequenceIntPredicateBuilder<>(converter,
                    SequenceIntPredicate.matching(val));
        }

        public static final class FinishableSequenceIntPredicateBuilder<T> {

            private final Function<SequenceIntPredicate, T> converter;
            private SequenceIntPredicate predicate;

            FinishableSequenceIntPredicateBuilder(
                    Function<SequenceIntPredicate, T> converter,
                    SequenceIntPredicate predicate) {
                this.converter = converter;
                this.predicate = predicate;
            }

            public FinishableSequenceIntPredicateBuilder<T> thenAnyOf(int val,
                    int... more) {
                predicate = predicate.then(val, more);
                return this;
            }

            public FinishableSequenceIntPredicateBuilder<T> then(int val) {
                predicate = predicate.then(val);
                return this;
            }

            public FinishableSequenceIntPredicateBuilder<T> then(
                    IntPredicate pred) {
                predicate = predicate.then(pred);
                return this;
            }

            public T build() {
                return converter.apply(predicate);
            }
        }
    }
}
