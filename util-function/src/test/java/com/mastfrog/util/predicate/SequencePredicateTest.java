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

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class SequencePredicateTest {

    @Test
    public void testLogical() {
        ResettableCopyablePredicate<String, ? extends ResettableCopyablePredicate<String, ?>> pred
                = SequencePredicate
                        .matchingAnyOf("foo", "bar")
                        .then("baz")
                        .then("quux")
                        .or(SequencePredicate.predicate("hey", "you"));

        testOne("quux", pred, "hi", "bye", "bar", "baz", "quux", "twaddle");
        testOne("quux", pred, "hi", "bye", "foo", "baz", "quux", "twaddle");
        testOne("quux", pred, "hi", "bye", "foo", "foo", "bar", "bar", "baz", "quux", "twaddle");
        testOne("--", pred, "a", "b", "c", "d");
        testOne("hey", pred, "a", "b", "c", "hey");
        testOne("you", pred, "a", "b", "c", "you");
    }

    @Test
    public void testSequences() {
        SequencePredicate<Integer> pred = SequencePredicate.matching(2).then(4).then(6);
        testOne(6, pred, 2, 4, 6, 8);
        testOne(6, pred, 0, 1, 2, 4, 6, 8);
        testOne(-1, pred, 0, 1, 2, 3, 4, 6, 8);
        testOne(6, pred, 2, 4, 6, 6, 6, 6);
        testOne(6, pred, 0, 5, 2, 4, 3, 2, 4, 6, 8, 2);
        testOne(6, pred, 4, 2, 4, 2, 2, 4, 2, 4, 2, 2, 4, 4, 2, 4, 6, 2, 4, 10);
        testOne(-1, pred, 0, 1, 2, 4, 2, 4, 2, 4, 4, 2);
        testOne(-1, pred, 6, 2, 4);

        pred.reset();
        assertFalse(pred.isPartiallyMatched());
        pred.test(2);
        assertTrue(pred.isPartiallyMatched());
        pred.test(3);
        assertFalse(pred.isPartiallyMatched());
        pred.test(2);
        pred.test(4);
        assertTrue(pred.isPartiallyMatched());
        assertTrue(pred.test(6));
        assertFalse(pred.isPartiallyMatched());

        pred = pred.then(1).then(1).then(1);
        assertFalse(pred.test(1));
        pred.test(2);
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test(4));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test(6));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test(1));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test(1));
        assertTrue(pred.isPartiallyMatched());
        assertTrue(pred.test(1));
        assertFalse(pred.isPartiallyMatched());
    }

    @SafeVarargs
    private final <T> void testOne(T shouldPassAfterFirst, ResettableCopyablePredicate<T, ?> pred, T... vals) {
        assert vals.getClass().getComponentType().isInstance(shouldPassAfterFirst);
        pred.reset();
        boolean matched = false;
        List<T> seen = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            T v = vals[i];
            seen.add(v);
            boolean result = pred.test(v);
            if (v.equals(shouldPassAfterFirst) && !matched) {
                matched = true;
                assertTrue("Should have passed after " + seen + ": " + pred, result);
            } else {
                if (matched && result) {
                    fail("Should not have matched duplicate at " + seen + ": " + pred);
                }
                assertFalse("Should not have passed after " + seen + ": " + pred, result);
            }
        }
    }

}
