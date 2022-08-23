/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.character.predicates;

import com.mastfrog.function.character.CharPredicates;
import com.mastfrog.function.character.CharFilter;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class CharFilterBuilderTest {

    @Test
    public void testJavaIdentifiers() {
        CharFilter filter = CharFilter.builder().testInitialCharacterWith().include(CharPredicates.JAVA_IDENTIFIER_START)
                .build().testSubsequentCharacterWith().include(CharPredicates.JAVA_IDENTIFIER_PART).build();

        assertNotNull(filter);
        assertTrue(filter.toString().endsWith("(JAVA_IDENTIFIER_START / JAVA_IDENTIFIER_PART)"));
        assertTrue(filter.test("java"));
        assertFalse(filter.test("hi there"));
        assertTrue(filter.test("i3"));
        assertFalse(filter.test("3i"));
    }

    @Test
    public void testComplex() {
        CharFilter filter = CharFilter.builder().testInitialCharacterWith().include('H').include('Q').build()
                .testSubsequentCharacterWith().include(CharPredicates.DIGIT).include(CharPredicates.WHITESPACE)
                .include('-', '_')
                .build();

        assertNotNull(filter);
        assertTrue(filter.toString().endsWith("(anyOf(HQ) / DIGIT | WHITESPACE | anyOf(-_)))"));
        assertTrue(filter.test("Q375"));
        assertTrue(filter.test("H73729"));
        assertTrue(filter.test("H737 29"));
        assertTrue(filter.test("H-737 29_"));
        assertTrue(filter.test("H-737 29_"));
        assertTrue(filter.test("H "));
        assertFalse(filter.test("Z73729"));
        assertFalse(filter.test("Z73729 87"));
        assertFalse(filter.test("Z73729o87"));
        assertFalse(filter.test("Hello"));
        assertFalse(filter.test("Quirk"));
    }

    @Test
    public void testGroups() {
        CharFilter filter = CharFilter.builder().testInitialCharacterWith().and(grp -> {
            grp.include('1', '2').include(CharPredicates.ALPHABETIC.negate());

        }).build().testSubsequentCharacterWith()
                .or(grp -> {
                    grp.include('a', 'b', 'c').include('x', 'y', 'z');
                })
                .build();

        assertNotNull(filter);
        assertTrue(filter.toString().endsWith("((anyOf(12) | not(ALPHABETIC)) / anyOf(abcxyz))"));
        assertTrue(filter.test("1ax"));
        assertFalse(filter.test("12ax"));
        assertTrue(filter.test(",ax"));
    }

    @Test
    public void testEmpty() {
        CharFilter filter = CharFilter.builder().testInitialCharacterWith().build().testSubsequentCharacterWith().build();
        assertNotNull(filter);
        assertTrue(filter.toString().endsWith("(<match-everything> / <match-everything>)"));
        assertTrue(filter.test("anything"));
        assertTrue(filter.test("6will"));
        assertTrue(filter.test(" be"));
        assertTrue(filter.test("acceptable"));
        assertFalse(filter.test("", false));
        assertTrue(filter.test("", true));
    }

    @Test
    public void testFilterBuilderWithConsumer() {
        CharFilter filter = CharFilter.builder().testInitialCharacterWith(b -> {
            b.include('X').and().include(CharPredicates.UPPER_CASE).build();
        }).testSubsequentCharacterWith(b -> {
            b.include(CharPredicates.PUNCTUATION.or(CharPredicates.WHITESPACE))
                    .exclude(CharPredicates.FILE_NAME_SAFE.negate());
        });

        assertNotNull(filter);
        assertTrue(filter.toString().endsWith("((anyOf(X) & UPPER_CASE) / ((PUNCTUATION | WHITESPACE) & FILE_NAME_SAFE))"));

        assertTrue(filter.test("X,"));
        assertTrue(filter.test("X,-"));
        assertTrue(filter.test("X, -."));
        assertFalse(filter.test("X, -.y"));
        assertFalse(filter.test("X, -./"));
        assertFalse(filter.test("X, -.*/"));
        assertFalse(filter.test("/, -."));
    }
}
