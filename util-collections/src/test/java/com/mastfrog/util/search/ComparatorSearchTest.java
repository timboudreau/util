/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.util.search;

import static com.mastfrog.util.search.BinarySearch.stringSearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ComparatorSearchTest {

    public static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static List<String> toCharStrings(IntPredicate skip) {
        return toCharStrings(ALPHA, skip);
    }

    static List<String> toCharStrings(String alpha, IntPredicate skip) {
        List<String> result = new ArrayList<>(alpha.length());
        for (int i = 0; i < alpha.length(); i++) {
            if (!skip.test(alpha.charAt(i))) {
                result.add("" + alpha.charAt(i));
            }
        }
        return result;
    }

    @Test
    public void testBiases() {
        List<String> strings = toCharStrings(val -> false);
        int ix = stringSearch(false, "P", 0, strings.size(), strings::get, Bias.NONE);
        assertEquals("P", strings.get(ix));
        testExactMatches(strings);
        testRemovals(strings);
    }

    @Test
    public void testPathologicalEmpty() {
        List<String> strings = Collections.emptyList();
        assertEquals(-1, stringSearch(false, "P", 0, strings.size(), strings::get, Bias.NEAREST));
        assertEquals(-1, stringSearch(false, "P", 0, strings.size(), strings::get, Bias.NONE));
        assertEquals(-1, stringSearch(false, "P", 0, strings.size(), strings::get, Bias.FORWARD));
        assertEquals(-1, stringSearch(false, "P", 0, strings.size(), strings::get, Bias.BACKWARD));
    }

    @Test
    public void testPathologicalOne() {
        List<String> strings = Arrays.asList("thing");
        assertEquals(-1, stringSearch(false, "stuff", 0, 1, strings::get, Bias.NONE));
        assertEquals(-1, stringSearch(false, "things", 0, 1, strings::get, Bias.NONE));
        assertEquals(0, stringSearch(false, "thing", 0, 1, strings::get, Bias.NONE));
        assertEquals(0, stringSearch(false, "thing", 0, 1, strings::get, Bias.FORWARD));
        assertEquals(0, stringSearch(false, "thing", 0, 1, strings::get, Bias.BACKWARD));
        assertEquals(0, stringSearch(false, "thing", 0, 1, strings::get, Bias.NEAREST));

        assertEquals(-1, stringSearch(false, "stuff", 0, 1, strings::get, Bias.BACKWARD));
        assertEquals(0, stringSearch(false, "stuff", 0, 1, strings::get, Bias.FORWARD));
        assertEquals(0, stringSearch(false, "stuff", 0, 1, strings::get, Bias.NEAREST));
        assertEquals(0, stringSearch(false, "zubitron", 0, 1, strings::get, Bias.BACKWARD));
    }

    @Test
    public void testPathologicalTwo() {
        List<String> strings = Arrays.asList("lemur", "walrus");

        assertEquals(0, stringSearch(true, "LEMUR", 0, strings.size(), strings::get, Bias.NONE));
        assertEquals(1, stringSearch(true, "zebra", 0, strings.size(), strings::get, Bias.BACKWARD));

        assertEquals(-1, stringSearch(true, "apterix", 0, strings.size(), strings::get, Bias.NONE));

        assertEquals(0, stringSearch(true, "apterix", 0, strings.size(), strings::get, Bias.NEAREST));
        assertEquals(0, stringSearch(true, "apterix", 0, strings.size(), strings::get, Bias.FORWARD));
        assertEquals(-1, stringSearch(true, "apterix", 0, strings.size(), strings::get, Bias.BACKWARD));

        assertEquals(0, stringSearch(true, "nanite", 0, strings.size(), strings::get, Bias.BACKWARD));
        assertEquals(1, stringSearch(true, "nanite", 0, strings.size(), strings::get, Bias.FORWARD));
    }

    @Test
    public void testUnsortedIsDetected() {
        List<String> strings = Arrays.asList("zildjian", "jackdaws", "love", "my", "big", "sphinx", "of", "quartz", "archeopterix", "sniffles");
        assertThrows(IllegalArgumentException.class, () -> {
            for (String s : strings) {
                int result = stringSearch(true, s, 0, strings.size(), strings::get, Bias.FORWARD);
            }
        });
    }

    @Test
    public void testDuplicateTolerance() {
        List<String> strings = toCharStrings((x) -> false);
        for (int i = 0; i < strings.size(); i++) {
            for (int j = 1; j < strings.size() - 1; j++) {
                List<String> copy = new ArrayList<>(strings);
                String toCopy = copy.get(i);
                for (int k = 0; k < j; k++) {
                    copy.add(i, toCopy);
                }
                for (Bias b : Bias.values()) {
                    testExactMatches(copy, b, true);
                }
            }
        }
    }

    private void testExactMatches(List<String> strings) {
        for (Bias bias : Bias.values()) {
            testExactMatches(strings, bias, false);
        }
    }

    private void testExactMatches(List<String> strings, Bias bias, boolean duplicates) {
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            int target = stringSearch(false, s, 0, strings.size(), strings::get, bias);
            assertTrue("Not found '" + s + " expecting index " + i + " for bias " + bias, target >= 0);
            assertTrue("Result out of range: " + target + " searching for " + s + " in " + strings.size() + " items",
                    target < strings.size());
            if (!duplicates) {
                assertEquals(i, target);
            }
            assertEquals(s, strings.get(i));
        }
    }

    private void testRemovals(List<String> strings) {
        for (int i = 3; i < strings.size() - 3; i++) {
            List<String> copy = new ArrayList<>(strings);
            String next = copy.get(i + 2);
            String prev = copy.get(i - 2);

            String after = copy.remove(i + 1);
            String on = copy.remove(i);
            String before = copy.remove(i - 1);

            int noneSearchBefore = stringSearch(false, before, 0, copy.size(), copy::get, Bias.NONE);
            assertTrue("Bias.NONE should get -1 searching for " + before + " but got " + i + " (" + copy.get(i) + ") in " + copy, noneSearchBefore < 0);
            int noneSearchOn = stringSearch(false, on, 0, copy.size(), copy::get, Bias.NONE);
            assertTrue("Bias.NONE should get -1 searching for " + on + " but got " + i + " (" + copy.get(i) + ") in " + copy, noneSearchOn < 0);
            int noneSearchAfter = stringSearch(false, after, 0, copy.size(), copy::get, Bias.NONE);
            assertTrue("Bias.NONE should get -1 searching for " + after + " but got " + i + " (" + copy.get(i) + ") in " + copy, noneSearchAfter < 0);

            int val = stringSearch(false, before, 0, copy.size(), copy::get, Bias.NEAREST);

            String nexp = Math.abs(before.compareTo(prev)) < Math.abs(before.compareTo(next))
                    ? prev : next;
            assertEquals("Bias nearest should get " + prev + " for " + before + " but got " + val + " - " + copy.get(val)
                    + " with removed " + before + " / " + on + " / " + after + " in " + copy,
                    nexp, copy.get(val));

            String wexp = Math.abs(after.compareTo(prev)) < Math.abs(after.compareTo(next))
                    ? prev : next;
            val = stringSearch(false, after, 0, copy.size(), copy::get, Bias.NEAREST);
            assertEquals("Bias nearest should get " + next + " for " + before + " but got " + val + " - " + copy.get(val)
                    + " with removed " + before + " / " + on + " / " + after + " in " + copy,
                    wexp, copy.get(val));
            // for On, in this test it's equidistant

            val = stringSearch(false, before, 0, copy.size(), copy::get, Bias.BACKWARD);
            assertEquals(prev, copy.get(val));
            val = stringSearch(false, on, 0, copy.size(), copy::get, Bias.BACKWARD);
            assertEquals(prev, copy.get(val));
            val = stringSearch(false, after, 0, copy.size(), copy::get, Bias.BACKWARD);
            assertEquals(prev, copy.get(val));

            val = stringSearch(false, before, 0, copy.size(), copy::get, Bias.FORWARD);
            assertEquals(next, copy.get(val));

            val = stringSearch(false, on, 0, copy.size(), copy::get, Bias.FORWARD);
            assertEquals(next, copy.get(val));
            val = stringSearch(false, after, 0, copy.size(), copy::get, Bias.FORWARD);
            assertEquals(next, copy.get(val));
        }
    }

}
