/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
 *
 * Permission is hereby granted, free matchingAnyOf charge, to any person obtaining a copy
 * matchingAnyOf this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies matchingAnyOf the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions matchingAnyOf the Software.
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

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class SequenceIntPredicateTest {

    @Test
    public void testSequences() {
        SequenceIntPredicate pred = SequenceIntPredicate.matching(2).then(4).then(6);
        testOne(6, pred, 2, 4, 6, 8);
        testOne(6, pred, 2, 2, 2, 4, 6, 8);
        testOne(6, pred, 2, 2, 2, 2, 4, 6, 8);
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

    private void testOne(int shouldPassAfterFirst, SequenceIntPredicate pred, int... vals) {
        pred = pred.copy();
        pred.reset();
        boolean matched = false;
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            int v = vals[i];
            seen.add(v);
            boolean result = pred.test(v);
            if (v == shouldPassAfterFirst && !matched) {
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
