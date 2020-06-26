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

/**
 *
 * @author Tim Boudreau
 */
final class SymbolEscaper implements Escaper {

    private final boolean delimit;

    SymbolEscaper() {
        this(false);
    }

    SymbolEscaper(boolean delimit) {
        this.delimit = delimit;
    }

    @Override
    public CharSequence escape(char c) {
        return escape(c, true, false);
    }

    private boolean isSymbol(char c, boolean first) {
        if (first) {
            return !Character.isJavaIdentifierStart(c);
        } else {
            return !Character.isJavaIdentifierPart(c);
        }
    }

    @Override
    public CharSequence escape(char c, int index, int of, char prev) {
        boolean currSymbol = isSymbol(c, false);
        if (index != 0) {
            boolean prevSymbol = isSymbol(prev, index == 1);
            if (prevSymbol != currSymbol || (prevSymbol && currSymbol)) {
                return escape(c, false, true);
            }
        } else if (!delimit && Character.isLetter(c)) {
            if (currSymbol) {
                return escape(c, true, false);
            }
            return Character.toString(Character.toUpperCase(c));
        }
        return escape(c, index == 0, false);
    }

    public CharSequence escape(char c, boolean first, boolean boundary) {
        if (first) {
            if (Character.isJavaIdentifierStart(c)) {
                if (boundary) {
                    if (delimit) {
                        return "_" + c;
                    } else {
                        if (c == '_') {
                            return "";
                        }
                        return Character.toString(Character.toUpperCase(c));
                    }
                }
                return null;
            } else if (!delimit && Character.isJavaIdentifierStart(c)) {
                return Character.toString(Character.toLowerCase(c));
            }
        } else if (Character.isJavaIdentifierPart(c)) {
            if (boundary) {
                if (delimit) {
                    return "_" + c;
                } else {
                    return Character.toString(Character.toUpperCase(c));
                }
            } else if (!delimit) {
                if (c == '_') {
                    return "";
                }
                return Character.toString(Character.toLowerCase(c));
            }
            return null;
        }
        String pfx = boundary ? delimit ? "_" : "" : "";
        switch (c) {
            case ' ':
                return pfx + "Space";
            case '\t':
                return pfx + "Tab";
            case '\n':
                return pfx + "Newline";
            case '\r':
                return pfx + "CarriageReturn";
            case '0':
                return pfx + "Zero";
            case '1':
                return pfx + "One";
            case '2':
                return pfx + "Two";
            case '3':
                return pfx + "Three";
            case '4':
                return pfx + "Four";
            case '5':
                return pfx + "Five";
            case '6':
                return pfx + "Six";
            case '7':
                return pfx + "Seven";
            case '8':
                return pfx + "Eight";
            case '9':
                return pfx + "Nine";
            case '.':
                return pfx + "Dot";
            case '>':
                return pfx + "GreaterThan";
            case '<':
                return pfx + "LessThan";
            case '%':
                return pfx + "Percent";
            case '|':
                return pfx + "Pipe";
            case '_':
                return pfx + "Underscore";
            case '$':
                return pfx + "Dollar";
            case '\\':
                // Unicode name would be "reverse solidus" which is, well,
                // not exactly intuitive
                return pfx + "Backslash";
            case '/':
                return pfx + "Slash";
            case '`':
                return pfx + "Backtick";
            case '^':
                return pfx + "Caren";
            case '@':
                return pfx + "At";
            case '!':
                return pfx + "Bang";
            case '#':
                return pfx + "Pound";
            case '[':
                return pfx + "LeftBracket";
            case ']':
                return pfx + "RightBracket";
            case '{':
                return pfx + "LeftBrace";
            case '}':
                return pfx + "RightBrace";
            default:
                return pfx + namify(Character.getName(c));
        }
    }

    private String namify(String name) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            switch (c) {
                case '_':
                case ' ':
                case '-':
                    capitalize = true;
                    if (!delimit) {
                        continue;
                    }
                    sb.append('_');
                    capitalize = true;
                    continue;
            }
            if (capitalize) {
                c = Character.toUpperCase(c);
            } else {
                c = Character.toLowerCase(c);
            }
            sb.append(c);
            capitalize = false;
        }
        return sb.toString();
    }
}
