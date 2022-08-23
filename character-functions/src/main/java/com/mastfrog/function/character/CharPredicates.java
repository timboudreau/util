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
package com.mastfrog.function.character;

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static java.lang.Character.isISOControl;
import static java.lang.Character.isIdentifierIgnorable;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isSpaceChar;
import static java.lang.Character.isTitleCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.isWhitespace;

/**
 * Enum of standard, useful character predicates, largely based on static
 * methods of <code>java.lang.Character</code>
 *
 * @see java.lang.Character
 *
 * @author Tim Boudreau
 */
public enum CharPredicates implements CharPredicate {

    /**
     * Wraps Character.isAlphabetic().
     */
    ALPHABETIC,
    /**
     * Wraps Character.isDigit().
     */
    DIGIT,
    /**
     * Returns whether a character is punctuation by process of elimination: If
     * it is
     * <ul>
     * <li>Not whitespace</li>
     * <li>Not an ISO control character</li>
     * <li>Not a digit</li>
     * <li>Not alphabetic</li>
     * <li>Not a letter</li>
     * </ul>
     * then it is considered one.
     */
    PUNCTUATION,
    /**
     * Wraps Character.isWhitespace().
     */
    WHITESPACE,
    /**
     * Wraps Character.isISOControl().
     */
    ISO_CONTROL_CHARS,
    /**
     * Wraps Character.isJavaIdentifierStart().
     */
    JAVA_IDENTIFIER_START,
    /**
     * Wraps Character.isJavaIdentifierPart().
     */
    JAVA_IDENTIFIER_PART,
    /**
     * Wraps Character.isIdentifierIgnorable().
     */
    JAVA_IDENTIFIER_IGNORABLE,
    /**
     * Wraps Character.isUpperCase().
     */
    UPPER_CASE,
    /**
     * Wraps Character.isLowerCase().
     */
    LOWER_CASE,
    /**
     * Wraps Character.isTitleCase().
     */
    TITLE_CASE,
    /**
     * Wraps Character.isLetter().
     */
    LETTER,
    /**
     * Wraps Character.isLetterOrDigit().
     */
    LETTER_OR_DIGIT,
    /**
     * Wraps Character.isSpaceChar().
     */
    SPACE_CHAR,
    /**
     * Determines if the tested character is not one which is illegal (or
     * extremely inadvisable) on Windows or Linux.
     */
    FILE_NAME_SAFE;

    @Override
    public boolean test(char c) {
        switch (this) {
            case ALPHABETIC:
                return isAlphabetic(c);
            case DIGIT:
                return isDigit(c);
            case PUNCTUATION:
                return !isWhitespace(c)
                        && !isISOControl(c)
                        && !isDigit(c)
                        && !isAlphabetic(c)
                        && !isLetter(c);
            case ISO_CONTROL_CHARS:
                return isISOControl(c);
            case JAVA_IDENTIFIER_PART:
                return isJavaIdentifierPart(c);
            case JAVA_IDENTIFIER_START:
                return isJavaIdentifierStart(c);
            case WHITESPACE:
                return isWhitespace(c);
            case UPPER_CASE:
                return isUpperCase(c);
            case LOWER_CASE:
                return isLowerCase(c);
            case TITLE_CASE:
                return isTitleCase(c);
            case LETTER:
                return isLetter(c);
            case LETTER_OR_DIGIT:
                return isLetterOrDigit(c);
            case JAVA_IDENTIFIER_IGNORABLE:
                return isIdentifierIgnorable(c);
            case SPACE_CHAR:
                return isSpaceChar(c);
            case FILE_NAME_SAFE:
                // While fewer characters are illegal in file names on
                // Linux, this includes Windows ones and generally
                // extremely inadvisable ones
                switch (c) {
                    case '/':
                    case ':':
                    case '\\':
                    case ';':
                    case '<':
                    case '>':
                    case '|':
                    case '?':
                    case '*':
                    case '"':
                    case 0:
                        return false;
                }
                if (c < 31) {
                    return false;
                }
                return true;
            default:
                throw new AssertionError(this);
        }
    }
}
