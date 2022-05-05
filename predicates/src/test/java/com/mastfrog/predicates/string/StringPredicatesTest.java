/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.predicates.string;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class StringPredicatesTest {

    @Test
    public void testContains() {
        EnhStringPredicate cont = StringPredicates.contains("blah");
        assertTrue(cont.test("Blee blah bloo"));
        assertFalse(cont.test("Weebles wobble"));
        assertFalse(cont.negate().test("Blee blah bloo"));
        assertTrue(cont.negate().test("Weebles wobble"));
        System.out.println(cont);

        EnhStringPredicate comb = cont.and(StringPredicates.endsWith("bloo"));
        assertNotSame(cont, comb);
        assertTrue(comb.test("Blee blah bloo"));
        assertFalse(comb.test("Weebles wobble"));
        assertFalse(comb.negate().test("Blee blah bloo"));
        assertTrue(comb.negate().test("Weebles wobble"));

        EnhStringPredicate stwee = comb.and(StringPredicates.startsWith("Blee"));
        assertNotSame(stwee, comb);
        assertTrue(stwee.test("Blee blah bloo"));
        assertFalse(stwee.test("Weebles wobble"));
        assertFalse(stwee.negate().test("Blee blah bloo"));
        assertTrue(stwee.negate().test("Weebles wobble"));

        EnhStringPredicate glurg = stwee.andNot(StringPredicates.contains("glurg"));
        assertNotSame(glurg, comb);
        assertTrue(glurg.test("Blee blah bloo"));
        assertFalse(glurg.test("Weebles wobble"));
        assertFalse(glurg.negate().test("Blee blah bloo"));
        assertTrue(glurg.negate().test("Weebles wobble"));

        assertEquals("and(contains(blah),ends_with(bloo),starts_with(Blee),not(contains(glurg)))", glurg.toString());
    }
}
