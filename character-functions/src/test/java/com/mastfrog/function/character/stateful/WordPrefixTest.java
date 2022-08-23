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
package com.mastfrog.function.character.stateful;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class WordPrefixTest {

    WordPrefix pfx = WordPrefix.of("this", "that", "something",
            "some other thing");

    @Test
    public void testWordPrefix() {
        assertMatch("this");
        assertMatch("that");
        assertMatch("something");
        assertMatch("some other thing");
        assertNonMatch("abcdefg");
        assertNonMatch(" ");
        assertMatch("this thing");
        assertMatch("that thing");
        assertMatch("something else");
        assertNonMatch("some bad thing");
        assertMatch("some other thing but I don't know what");
    }

    private void assertMatch(String what) {
        pfx.reset();
        for (int i = 0; i < what.length(); i++) {
            char c = what.charAt(i);
            assertTrue(pfx.test(c), "Failed on char " + i + " (" + c
                    + ") in '" + what + "'");
        }
        assertTrue(pfx.toStringPredicate().test(what), "String predicate fails");
    }

    private void assertNonMatch(String what) {
        pfx.reset();
        boolean result = true;
        for (int i = 0; i < what.length(); i++) {
            char c = what.charAt(i);
            result = pfx.test(c);
        }
        assertFalse(result);
        assertFalse(pfx.toStringPredicate().test(what), "String predicate should not have matched " + what + ": " + pfx);
    }

}
