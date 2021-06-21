/*
 * The MIT License
 *
 * Copyright 2021 Tim Boudreau.
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
package com.mastfrog.util.strings;

import java.util.EnumMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class MultiLiteralPatternTest {

    private final MultiLiteralPattern<Stuff> pat
            = new MultiLiteralPattern<Stuff>(Stuff.stuff(), Stuff.class);

    @Test
    public void testFind() {
        testOne(Stuff.THING, "I think I could be a thing or two");
        testOne(Stuff.SOMETHING_ELSE, "I think I could be something else");
        testOne(Stuff.PEOPLE, "Are people dogs?");
        testOne(Stuff.DOGS, "Are dogs people?");

        testOne(Stuff.THING, "I think I could be a thing");
        testOne(Stuff.THING, "could be a thing that I lost");
        testOne(Stuff.THING, "could be a thing");

        testOne(null, "I could be something eligible");
        testOne(Stuff.SOMETHING_ELSE,
                "doggie could be something eligible for peopling but I could "
                + "be something else entirely");

        testOne(null, "Nothing matches here");

        testOne(Stuff.DOGS, "dogs eat cheese");
        testOne(null, "Dogs eat cheese");
    }

    private void testOne(Stuff type, String text) {
        Stuff result = pat.apply(text);
        if (type == null) {
            assertNull("Should not have matched: '" + text + "'", result);
        } else {
            assertEquals("Should have been " + type.name()
                    + " but got " + (result == null ? null : result.name())
                    + " for '" + text + "'", type, result);
        }
    }

    static enum Stuff {
        THING("could be a thing"),
        SOMETHING_ELSE("could be something else"),
        PEOPLE("people"),
        DOGS("dogs");

        private final String pattern;

        Stuff(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return pattern;
        }

        static EnumMap<Stuff, String> stuff() {
            EnumMap<Stuff, String> result = new EnumMap<>(Stuff.class);
            for (Stuff s : values()) {
                result.put(s, s.toString());
            }
            return result;
        }
    }
}
