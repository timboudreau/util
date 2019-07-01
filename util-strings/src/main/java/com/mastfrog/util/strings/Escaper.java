/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Used by various conversion methods on Strings methods.  Takes a character
 * and returns either an escaped string representation of it, or null to
 * indicate the character should be used as is.  Typically used to generate
 * valid lines of code in a language, but could also be used to, say, convert
 * to hexadecimal or anything else.
 *
 * @author Tim Boudreau
 */
public interface Escaper {

    CharSequence escape(char c);

    /**
     * For use when logging a badly encoded string.  Converts
     * unencodable characters to hex and ISO control characters
     * to hex or their standard escaped Java string representation
     * if there is one (e.g. 0x05 -> "&lt;0x05&gt;" but \n -> "\n").
     *
     * @param cs The character set.
     * @return A string representation that does not include raw
     * unencodable or control characters.
     */
    static Escaper escapeUnencodableAndControlCharacters(Charset cs) {
        CharsetEncoder enc = cs.newEncoder();
        return c -> {
            switch (c) {
                case '\t':
                    return "\\t";
                case '\r':
                    return "\\r";
                case '\n':
                    return "\\n";
            }
            if (!enc.canEncode(c) || Character.isISOControl(c)) {
                return "<0x" + Strings.toHex(c) + ">";
            }
            return null;
        };
    }

    /**
     * Returns an escaper which does not escape the specified
     * character, but otherwise behaves the same as its parent.
     *
     * @param c A character
     * @return a new escaper
     */
    default Escaper ignoring(char c) {
        return c1 -> {
            if (c1 == c) {
                return null;
            }
            return this.escape(c);
        };
    }

    /**
     * Combine this escaper with another, such that the passed
     * escaper is used only on characters this escaper did not
     * escape.
     *
     * @param other Another escaper
     * @return A new escaper
     */
    default Escaper and(Escaper other) {
        return c -> {
            CharSequence result = escape(c);
            return result == null ? other.escape(c) : result;
        };
    }

    /**
     * Returns a new escaper which will also escape the passed
     * character by prefixing it with \ in output.
     *
     * @param c A character
     * @return A new escaper
     */
    default Escaper escaping(char c) {
        return and((char c1) -> {
            if (c1 == c) {
                return "\\" + c;
            }
            return null;
        });
    }

    /**
     * Adds the behavior of escaping " characters.
     *
     * @return A new escaper
     */
    default Escaper escapeDoubleQuotes() {
        return and((char c) -> {
            if (c == '"') {
                return "\\\"";
            }
            return null;
        });
    }

    /**
     * Adds the behavior of escaping ' characters.
     *
     * @return A new escaper
     */
    default Escaper escapeSingleQuotes() {
        return and((char c) -> {
            if (c == '"') {
                return "\\\"";
            }
            return null;
        });
    }

    /**
     * Escapes the standard characters which must be escaped for generating
     * valid lines of code in Java or Javascript - \n, \r, \t, \b, \f and \.
     * Does <i>not</i> escape quote characters (this may differ based on the target
     * language) - call escapeSingleQuotes() or escapeDoubleQuotes() to create a
     * wrapper around this escaper which does that.
     */
    public static Escaper CONTROL_CHARACTERS = c -> {
        switch (c) {
            case '\n':
                return ("\\n");
            case '\r':
                return ("\\r");
            case '\t':
                return ("\\t");
            case '\b':
                return ("\\b");
            case '\f':
                return ("\\f");
            case '\\':
                return ("\\\\");
            default:
                return null;
        }
    };

}
