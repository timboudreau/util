/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class LevenshteinDistanceTest {

    @Test
    public void testSorting() {
        List<String> base = Arrays.asList("Apples", "Oranges", "Orangutangs",
                "Orchids", "Orbit", "Orthanc", "Zebras", "Zoranges", "Zoos",
                "Artichokes", "Arthroscopy", "Arrival", "Zyzyzyzy");

        List<String> copy = new ArrayList<>(base);

        LevenshteinDistance.sortByDistance("Ortha", true, copy);
        assertEquals("Orthanc", copy.get(0));

        LevenshteinDistance.sortByDistance("ortha", false, copy);
        assertEquals("Orthanc", copy.get(0));

        LevenshteinDistance.sortByDistance("O", false, copy);
        assertEquals("Zoos", copy.get(0));

        LevenshteinDistance.sortByDistance("Orangina", false, copy);
        assertEquals("Oranges", copy.get(0));
        
        LevenshteinDistance.sortByDistance("Artist", false, copy);
        assertEquals("Orbit", copy.get(0));
    }

    @Test
    public void testScoring() {
        float score1 = LevenshteinDistance.score("Ambition", "ambidextrous", false);
        float score2 = LevenshteinDistance.score("Ambition", "aesthete", false);
        assertTrue(score1 + " vs " + score2, score1 < score2);

        score1 = LevenshteinDistance.score("Bat", "Cat", false);
        score2 = LevenshteinDistance.score("Bat", "Rats", false);
        assertTrue(score1 + " vs " + score2, score1 < score2);

        score1 = LevenshteinDistance.score("Bat", "Battt", false);
        score2 = LevenshteinDistance.score("Bat", "Batt", false);
        assertTrue(score1 + " vs " + score2, score1 > score2);

        assertEquals(1F, LevenshteinDistance.inverseScore("Antidisestabilshmentarianism", "Antidisestabilshmentarianism", true), 0.0000000000000001F);
        assertEquals(0F, LevenshteinDistance.score("Antidisestabilshmentarianism", "Antidisestabilshmentarianism", true), 0.0000000000000001F);
        assertNotEquals(1F, LevenshteinDistance.inverseScore("Antidisestabilshmentarianism", "antidisestabilshmentarianism", true), 0.0000000000000001F);
        assertEquals(1F, LevenshteinDistance.inverseScore("Antidisestabilshmentarianism", "Antidisestabilshmentarianism", false), 0.0000000000000001F);
        assertEquals(1F, LevenshteinDistance.score("", "Antidisestabilshmentarianism", false), 0.0000000000000001F);
        assertEquals(0F, LevenshteinDistance.inverseScore("", "Antidisestabilshmentarianism", false), 0.0000000000000001F);
        assertEquals(0F, LevenshteinDistance.inverseScore("Antidisestabilshmentarianism", "", false), 0.0000000000000001F);
        assertEquals(0F, LevenshteinDistance.score("", "", true), 0.0000000000000001F);
        assertEquals(0F, LevenshteinDistance.score("", "", false), 0.0000000000000001F);
        assertEquals(1F, LevenshteinDistance.inverseScore("", "", true), 0.0000000000000001F);
        assertEquals(1F, LevenshteinDistance.inverseScore("", "", false), 0.0000000000000001F);

    }
}
