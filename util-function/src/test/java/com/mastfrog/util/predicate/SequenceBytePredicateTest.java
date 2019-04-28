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
package com.mastfrog.util.predicate;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

public class SequenceBytePredicateTest {

    @Test
    public void testSequences() {
        SequenceBytePredicate pred = SequenceBytePredicate.matching((byte) 2).then((byte) 4).then((byte) 6);
        testOne((byte) 6, pred, (byte) 2, (byte) 4, (byte) 6, (byte) 8);
        testOne((byte) 6, pred, (byte) 2, (byte) 2, (byte) 2, (byte) 4, (byte) 6, (byte) 8);
        testOne((byte) 6, pred, (byte) 0, (byte) 1, (byte) 2, (byte) 4, (byte) 6, (byte) 8);
        testOne(-1, pred, (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 6, (byte) 8);
        testOne((byte) 6, pred, (byte) 2, (byte) 4, (byte) 6, (byte) 6, (byte) 6, (byte) 6);
        testOne((byte) 6, pred, (byte) 0, (byte) 5, (byte) 2, (byte) 4, (byte) 3, (byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 2);
        testOne((byte) 6, pred, (byte) 4, (byte) 2, (byte) 4, (byte) 2, (byte) 2, (byte) 4, (byte) 2, (byte) 4, (byte) 2, (byte) 2, (byte) 4, (byte) 4, (byte) 2, (byte) 4, (byte) 6, (byte) 2, (byte) 4, (byte) 10);
        testOne(-1, pred, (byte) 0, (byte) 1, (byte) 2, (byte) 4, (byte) 2, (byte) 4, (byte) 2, (byte) 4, (byte) 4, (byte) 2);
        testOne(-1, pred, (byte) 6, (byte) 2, (byte) 4);

        pred.reset();
        assertFalse(pred.isPartiallyMatched());
        pred.test((byte) 2);
        assertTrue(pred.isPartiallyMatched());
        pred.test((byte) 3);
        assertFalse(pred.isPartiallyMatched());
        pred.test((byte) 2);
        pred.test((byte) 4);
        assertTrue(pred.isPartiallyMatched());
        assertTrue(pred.test((byte) 6));
        assertFalse(pred.isPartiallyMatched());

        pred = pred.then((byte) 1).then((byte) 1).then((byte) 1);
        assertFalse(pred.test((byte) 1));
        pred.test((byte) 2);
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test((byte) 4));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test((byte) 6));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test((byte) 1));
        assertTrue(pred.isPartiallyMatched());
        assertFalse(pred.test((byte) 1));
        assertTrue(pred.isPartiallyMatched());
        assertTrue(pred.test((byte) 1));
        assertFalse(pred.isPartiallyMatched());
    }

    private void testOne(int shouldPassAfterFirst, SequenceBytePredicate pred, byte... vals) {
        pred = pred.copy();
        pred.reset();
        boolean matched = false;
        List<Byte> seen = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            byte v = vals[i];
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
