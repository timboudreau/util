/*
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util;

import com.mastfrog.util.collections.ArrayUtils;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.streams.HashingInputStream;
import com.mastfrog.util.streams.HashingOutputStream;
import com.mastfrog.util.strings.AppendableCharSequence;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

/**
 * String utilities - in particular, contains a number of utility methods for
 * performing String operations on CharSequence instances in-place, which are
 * useful for libraries such as Netty which implement 8-bit CharSequences, where
 * otherwise we would need to copy the bytes into a String to perform
 * operations.
 */
public final class Strings {

    /**
     * Trim a CharSequence returning a susbsequence.
     *
     * @param seq The string
     * @return
     */
    public static CharSequence trim(CharSequence seq) {
        if (seq instanceof String) {
            return ((String) seq).trim();
        }
        int len = seq.length();
        if (len == 0) {
            return seq;
        }
        if (seq instanceof String) {
            return ((String) seq).trim();
        }
        int start = 0;
        int end = len;
        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(seq.charAt(i))) {
                start++;
            } else {
                break;
            }
        }
        if (start == len - 1) {
            return "";
        }
        for (int i = len - 1; i >= 0; i--) {
            if (Character.isWhitespace(seq.charAt(i))) {
                end--;
            } else {
                break;
            }
        }
        if (end == 0) {
            return "";
        }
        if (end == len && start == 0) {
            return seq;
        }
        return seq.subSequence(start, end);
    }

    /**
     * Get the sha1 hash of a string in UTF-8 encoding.
     *
     * @param s The string
     * @return The sha-1 hash
     */
    public static String sha1(String s) {
        MessageDigest digest = HashingInputStream.createDigest("SHA-1");
        byte[] result = digest.digest(s.getBytes(Charset.forName("UTF-8")));
        return HashingOutputStream.hashString(result);
    }

    /**
     * Convenience function for formatting an array of elements separated by a
     * comma.
     *
     * @param <T> type
     * @param collection collection
     * @return resulting string
     */
    public static <T> String toString(T[] collection) {
        return toString(Arrays.asList(collection));
    }

    /**
     * Split a comma-delimited list into an array of trimmed strings
     *
     * @param string The input string
     * @return An array of resulting strings
     */
    public static String[] split(String string) {
        String[] result = string.split(",");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }

    /**
     * Convenience function for formatting a collection (Iterable) of elements
     * separated by a comma.
     *
     * @param <T> type
     * @param collection collection
     * @return resulting string
     */
    public static <T> String toString(Iterable<T> collection) {
        return toString(collection.iterator());
    }

    public static String toString(final Iterator<?> iter) {
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Converts a Throwable to a string.
     *
     * @param throwable The throwable
     * @return The string
     */
    public static String toString(final Throwable throwable) {
        StringWriter w = new StringWriter();
        try (PrintWriter p = new PrintWriter(w)) {
            throwable.printStackTrace(p);
        }
        return w.toString();
    }

    /**
     * Private constructor prevents construction.
     */
    private Strings() {
    }

    /**
     * Join / delimited paths, ensuring no doubled slashes
     *
     * @param parts An array of strings
     * @return A string. If a leading slash is desired, the first element must
     * have one
     * @deprecated Use joinPath
     */
    public static String join(String... parts) {
        return joinPath(parts);
    }

    /**
     * Join / delimited paths, ensuring no doubled slashes
     *
     * @param parts An array of strings
     * @return A string. If a leading slash is desired, the first element must
     * have one
     */
    public static String joinPath(String... parts) {
        StringBuilder sb = new StringBuilder();
        if (parts.length > 0) {
            sb.append(parts[0]);
        }
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || (part.length() == 1 && part.charAt(0) == '/')) {
                continue;
            }
            boolean gotTrailingSlash = sb.length() == 0 ? false : sb.charAt(sb.length() - 1) == '/';
            boolean gotLeadingSlash = part.charAt(0) == '/';
            if (gotTrailingSlash != !gotLeadingSlash) {
                sb.append(part);
            } else {
                if (!gotTrailingSlash) {
                    sb.append('/');
                } else {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Computes a hash code for a CharSequence. The result will be the same as
     * that of java.lang.String.
     *
     * @param seq A char sequence passed CharSequence
     * @return A hash code
     * @since 1.7.0
     */
    public static int charSequenceHashCode(CharSequence seq) {
        return charSequenceHashCode(seq, false);
    }

    /**
     * Computes a hash code for a CharSequence. If ignoreCase is false, the
     * result will be the same as that of java.lang.String.
     *
     * @param seq A char sequence
     * @param ignoreCase If true, generate a hash code for the lower-case
     * version of the passed CharSequence
     * @return A hash code
     * @since 1.7.0
     */
    public static int charSequenceHashCode(CharSequence seq, boolean ignoreCase) {
        Checks.notNull("seq", seq);
        // Same computation as java.lang.String for case sensitive
        int length = seq.length();
        if (length == 0) {
            return 0;
        }
        int result = 0;
        for (int i = 0; i < length; i++) {
            if (ignoreCase) {
                result = 31 * result + Character.toLowerCase(seq.charAt(i));
            } else {
                result = 31 * result + seq.charAt(i);
            }
        }
        return result;
    }

    /**
     * Compare the contents of two CharSequences which may be of different types
     * for exact character-level equality.
     *
     * @param a One character sequence
     * @param b Another character sequence
     * @return true if they match
     * @since 1.7.0
     */
    @SuppressWarnings("null")
    public static boolean charSequencesEqual(CharSequence a, CharSequence b) {
        return charSequencesEqual(a, b, false);
    }

    /**
     * Compare the contents of two CharSequences which may be of different types
     * for equality.
     *
     * @param a One character sequence
     * @param b Another character sequence
     * @param ignoreCase If true, do a case-insensitive comparison
     * @return true if they match
     * @since 1.7.0
     */
    @SuppressWarnings("null")
    public static boolean charSequencesEqual(CharSequence a, CharSequence b, boolean ignoreCase) {
        Checks.notNull("a", a);
        Checks.notNull("b", b);
        if ((a == null) != (b == null)) {
            return false;
        } else if (a == b) {
            return true;
        }
        if (a instanceof String && b instanceof String) {
            return ignoreCase ? ((String) a).equalsIgnoreCase((String) b) : a.equals(b);
        }
        @SuppressWarnings("null")
        int length = a.length();
        if (length != b.length()) {
            return false;
        }
        if (ignoreCase && a.getClass() == b.getClass()) {
            return a.equals(b);
        }
        if (!ignoreCase && a instanceof String) {
            return ((String) a).contentEquals(b);
        } else if (!ignoreCase && b instanceof String) {
            return ((String) b).contentEquals(a);
        } else {
            for (int i = 0; i < length; i++) {
                char ca = ignoreCase ? Character.toLowerCase(a.charAt(i)) : a.charAt(i);
                char cb = ignoreCase ? Character.toLowerCase(b.charAt(i)) : b.charAt(i);
                if (cb != ca) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Trim an array of CharSequences at once.
     *
     * @param seqs The strings
     * @return a new array of CharSequences
     */
    public static CharSequence[] trim(CharSequence[] seqs) {
        CharSequence[] result = new CharSequence[seqs.length];
        for (int i = 0; i < seqs.length; i++) {
            result[i] = trim(seqs[i]);
        }
        return result;
    }

    /**
     * Compare two CharSequences, optionally ignoring case.
     *
     * @param a The first
     * @param b The second
     * @param ignoreCase If true, do case-insensitive comparison
     * @return the difference
     */
    public static int compareCharSequences(CharSequence a, CharSequence b, boolean ignoreCase) {
        if (a == b) {
            return 0;
        }
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        int aLength = a.length();
        int bLength = b.length();
        if (aLength == 0 && bLength == 0) {
            return 0;
        }
        int max = Math.min(aLength, bLength);
        for (int i = 0; i < max; i++) {
            char ac = ignoreCase ? Character.toLowerCase(a.charAt(i)) : a.charAt(i);
            char bc = ignoreCase ? Character.toLowerCase(b.charAt(i)) : b.charAt(i);
            if (ac > bc) {
                return 1;
            } else if (ac < bc) {
                return -1;
            }
        }
        if (aLength == bLength) {
            return 0;
        } else if (aLength > bLength) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Get a comparator that calls compareCharSequences().
     *
     * @param caseInsensitive If true, do case-insensitive comparison.
     * @return A comparator
     */
    public static Comparator<CharSequence> charSequenceComparator(boolean caseInsensitive) {
        return new CharSequenceComparator(caseInsensitive);
    }

    /**
     * Get a comparator that calls compareCharSequences().
     *
     * @return A comparator
     */
    public static Comparator<CharSequence> charSequenceComparator() {
        return charSequenceComparator(false);
    }

    private static final class CharSequenceComparator implements Comparator<CharSequence> {

        private final boolean caseInsensitive;

        CharSequenceComparator(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            return compareCharSequences(o1, o2, caseInsensitive);
        }
    }

    /**
     * Returns an empty char sequence.
     *
     * @return An empty char sequence.
     */
    public static CharSequence emptyCharSequence() {
        return EMPTY;
    }

    private static final EmptyCharSequence EMPTY = new EmptyCharSequence();

    private static final class EmptyCharSequence implements CharSequence {

        @Override
        public int length() {
            return 0;
        }

        @Override
        public char charAt(int index) {
            throw new StringIndexOutOfBoundsException(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start == 0 && end == 0) {
                return this;
            }
            throw new StringIndexOutOfBoundsException("Empty but requested subsequence from "
                    + start + " to " + end);
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CharSequence && ((CharSequence) o).length() == 0;
        }
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(char delim, String... parts) {
        return join(delim, Arrays.asList(parts));
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static CharSequence join(char delim, CharSequence... parts) {
        AppendableCharSequence seq = new AppendableCharSequence();
        for (int i = 0; i < parts.length; i++) {
            seq.append(parts[i]);
            if (i != parts.length - 1) {
                seq.append(delim);
            }
        }
        return seq;
    }

    public static CharSequence join(char delim, Object... parts) {
        if (parts.length == 1 && parts[0] instanceof Iterable<?>) {
            return join(delim, (Iterable<?>) parts[0]);
        }
        AppendableCharSequence seq = new AppendableCharSequence();
        for (int i = 0; i < parts.length; i++) {
            String ts = parts[i] == null ? "null" : parts[i].toString();
            seq.append(ts);
            if (i != parts.length - 1) {
                seq.append(delim);
            }
        }
        return seq;
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(char delim, Iterable<?> parts) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> iter = parts.iterator(); iter.hasNext();) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(String delim, Iterable<?> parts) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<?> iter = parts.iterator(); iter.hasNext();) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    private static final SingleCharSequence CR = new SingleCharSequence('\n');
    private static final SingleCharSequence COMMA = new SingleCharSequence(',');
    private static final SingleCharSequence DOT = new SingleCharSequence('.');
    private static final SingleCharSequence COLON = new SingleCharSequence(':');
    private static final SingleCharSequence OPEN_CURLY = new SingleCharSequence('{');
    private static final SingleCharSequence CLOSE_CURLY = new SingleCharSequence('}');
    private static final SingleCharSequence OPEN_PAREN = new SingleCharSequence('(');
    private static final SingleCharSequence CLOSE_PAREN = new SingleCharSequence(')');
    private static final SingleCharSequence OPEN_SQUARE = new SingleCharSequence('[');
    private static final SingleCharSequence CLOSE_SQUARE = new SingleCharSequence(']');

    public static CharSequence singleChar(char c) {
        switch (c) {
            case '\n':
                return CR;
            case ',':
                return COMMA;
            case '.':
                return DOT;
            case ':':
                return COLON;
            case '{':
                return OPEN_CURLY;
            case '}':
                return CLOSE_CURLY;
            case '(':
                return OPEN_PAREN;
            case ')':
                return CLOSE_PAREN;
            case '[':
                return OPEN_SQUARE;
            case ']':
                return CLOSE_SQUARE;
            default:
                return new SingleCharSequence(c);
        }
    }

//    public static List<CharSequence> splitToList(char delim, CharSequence seq) {
//        Checks.notNull("seq", seq);
//        List<CharSequence> seqs = new ArrayList<>(5);
//        split(delim, seq, (val) -> {
//            seqs.add(val);
//            return true;
//        });
//        return seqs;
//    }
    public static CharSequence[] split(char delim, CharSequence seq) {
        List<CharSequence> l = splitToList(delim, seq);
        return l.toArray(new CharSequence[l.size()]);
    }

    public static List<CharSequence> splitToList(char delimiter, CharSequence seq) {
        List<CharSequence> result = new ArrayList<>(5);
        int max = seq.length();
        int start = 0;
        for (int i = 0; i < max; i++) {
            char c = seq.charAt(i);
            boolean last = i == max - 1;
            if (c == delimiter || last) {
                result.add(seq.subSequence(start, last ? c == delimiter ? i : i + 1 : i));
                start = i + 1;
            }
        }
        return result;
    }

    public static Set<CharSequence> splitUniqueNoEmpty(char delim, CharSequence seq) {
        Set<CharSequence> result = new LinkedHashSet<>();
        split(delim, seq, (s) -> {
            s = trim(s);
            if (s.length() > 0) {
                result.add(s);
            }
            return true;
        });
        return result;
    }

    public static void split(char delim, CharSequence seq, Function<CharSequence, Boolean> proc) {
        Checks.notNull("seq", seq);
        Checks.notNull("proc", proc);
        int lastStart = 0;
        int max = seq.length();
        for (int i = 0; i < max; i++) {
            char c = seq.charAt(i);
//            System.out.println("At " + i + " " + c + " look for " + delim);
            if (delim == c || i == max - 1) {
                if (lastStart != i) {
                    CharSequence sub = seq.subSequence(lastStart, i == max - 1 ? i + 1 : i);
                    if (!proc.apply(sub)) {
                        return;
                    }
                } else {
                    if (!proc.apply("")) {
                        return;
                    }
                }
                lastStart = i + 1;
            }
        }
    }

    public static boolean startsWith(CharSequence target, CharSequence start) {
        int targetLength = target.length();
        int startLength = start.length();
        if (startLength > targetLength) {
            return false;
        }
        for (int i = 0; i < startLength; i++) {
            if (start.charAt(i) != target.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean startsWithIgnoreCase(CharSequence target, CharSequence start) {
        int targetLength = target.length();
        int startLength = start.length();
        if (startLength > targetLength) {
            return false;
        }
        for (int i = 0; i < startLength; i++) {
            if (start.charAt(i) != target.charAt(i) && Character.toLowerCase(start.charAt(i)) != target.charAt(i) && Character.toUpperCase(start.charAt(i)) != target.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean contentEqualsIgnoreCase(CharSequence a, CharSequence b) {
        int len = a.length();
        if (b.length() != len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i) && Character.toLowerCase(a.charAt(i)) != b.charAt(i) && Character.toUpperCase(a.charAt(i)) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean charSequenceContains(CharSequence container, CharSequence contained, boolean ignoreCase) {
        if (!ignoreCase && container instanceof String && contained instanceof String) {
            return ((String) container).contains(contained);
        }
        int containedLength = contained.length();
        int containerLength = container.length();
        if (containedLength > containerLength) {
            return false;
        }
        int offset = 0;
        int max = container.length(); //(containerLength - containedLength) + 1;
        for (int i = 0; i < max; i++) {
            char c = ignoreCase ? Character.toLowerCase(container.charAt(i)) : container.charAt(i);
            char d = ignoreCase ? Character.toLowerCase(contained.charAt(offset)) : contained.charAt(offset);
            if (c != d) {
                offset = 0;
            } else {
                offset++;
            }
            if (offset >= containedLength) {
                return true;
            }
        }
        return false;
    }

    public static int parseInt(CharSequence seq) {
        int result = 0;
        int max = seq.length() - 1;
        int position = 1;
        boolean negative = false;
        for (int i = max; i >= 0; i--) {
            char c = seq.charAt(i);
            switch (c) {
                case '-':
                    if (i == 0) {
                        negative = true;
                        continue;
                    }
                    throw new NumberFormatException("- encountered not at start of '" + seq + "'");
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    int prev = result;
                    result += position * (c - '0');
                    if (prev > result) {
                        throw new NumberFormatException("Number too large for integer: '" + seq + "' - "
                                + " " + prev + " + " + (position * (c - '0')) + " = " + result);
                    }
                    position *= 10;
                    continue;
                default:
                    throw new NumberFormatException("Illegal character '" + c + "' in number '" + seq + "'");
            }
        }
        return negative ? -result : result;
    }

    public static long parseLong(CharSequence seq) {
        long result = 0;
        int max = seq.length() - 1;
        long position = 1;
        boolean negative = false;
        for (int i = max; i >= 0; i--) {
            char c = seq.charAt(i);
            switch (c) {
                case '-':
                    if (i == 0) {
                        negative = true;
                        continue;
                    }
                    throw new NumberFormatException("- encountered not at start of '" + seq + "'");
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    long prev = result;
                    result += position * (c - '0');
                    if (prev > result) {
                        throw new NumberFormatException("Number too large for long: '" + seq + "' - "
                                + " " + prev + " + " + (position * (c - '0')) + " = " + result);
                    }
                    position *= 10;
                    continue;
                default:
                    throw new NumberFormatException("Illegal character '" + c + "' in number '" + seq + "'");
            }
        }
        return negative ? -result : result;
    }

    public static int indexOf(char c, CharSequence seq) {
        int max = seq.length();
        for (int i = 0; i < max; i++) {
            if (c == seq.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    public static int lastIndexOf(char c, CharSequence seq) {
        int max = seq.length() - 1;
        for (int i = max; i >= 0; i--) {
            if (c == seq.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Interleave the characters of two strings.
     *
     * @param a The first string
     * @param b The second string
     * @return A string combining both
     */
    public static String interleave(CharSequence a, CharSequence b) {
        StringBuilder sb = new StringBuilder();
        int maxA = a.length();
        int maxB = b.length();
        int max = Math.max(maxA, maxB);
        for (int i = 0; i < max; i++) {
            if (i < maxA) {
                sb.append(a.charAt(i));
            }
            if (i < maxB) {
                sb.append(b.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Replace all occurrances of a pattern in a string without regular
     * expression parsing.
     *
     * @param pattern The pattern
     * @param replacement The replacement
     * @param in The input string
     * @return A new string
     */
    public static String literalReplaceAll(String pattern, String replacement, String in) {
        // XXX this could be made considerably more efficient by iterating
        // the char array
        if (in.length() < pattern.length()) {
            return in;
        }
        if (pattern.equals(replacement)) {
            throw new IllegalArgumentException("Replacing pattern with itself: " + pattern);
        }
        if (replacement.contains(pattern)) {
            throw new IllegalArgumentException("Pattern contains its replacement - would "
                    + "loop forever.  Pattern: '" + pattern + "' Replacement: '" + replacement + "'");
        }
        boolean replaced;
        do {
            int ix = in.indexOf(pattern);
            if (ix >= 0) {
                replaced = true;
                if (ix == 0) {
                    in = replacement + in.substring(ix + pattern.length());
                } else {
                    String begin = in.substring(0, ix);
                    String end = in.substring(ix + pattern.length(), in.length());
                    in = begin + replacement + end;
                }
            } else {
                replaced = false;
            }
        } while (replaced);
        return in;
    }

    public static String toString(Object o) {
        if (o == null) {
            return "null";
        } else if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Iterable<?>) {
            return join(',', (Iterable<?>) o);
        } else if (o.getClass().isArray()) {
            return join(',', CollectionUtils.toList(o));
        }
        return o.toString();
    }

    /**
     * A faster String.contains() for single characters, case insensitive.
     *
     * @param lookFor The character to look for
     * @param in Look for in this string
     * @return True if the character is present
     */
    public static boolean containsCaseInsensitive(char lookFor, CharSequence in) {
        char lookFor1 = Character.toLowerCase(lookFor);
        char lookFor2 = Character.toLowerCase(lookFor);
        int max = in.length();
        for (int i = 0; i < max; i++) {
            if (in.charAt(i) == lookFor1 || in.charAt(i) == lookFor2) {
                return true;
            }
        }
        return false;
    }

    /**
     * A faster String.contains() for single characters, case insensitive.
     *
     * @param lookFor The character to look for
     * @param in Look for in this string
     * @return True if the character is present
     */
    public static boolean contains(char lookFor, CharSequence in) {
        int max = in.length();
        for (int i = 0; i < max; i++) {
            if (in.charAt(i) == lookFor) {
                return true;
            }
        }
        return false;
    }

    public static String camelCaseToDashes(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        int max = s.length();
        for (int i = 0; i < max; i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static String dashesToCamelCase(CharSequence s) {
        StringBuilder sb = new StringBuilder();
        boolean upcase = true;
        int max = s.length();
        for (int i = 0; i < max; i++) {
            char c = s.charAt(i);
            if (c == '-') {
                upcase = true;
                continue;
            }
            if (upcase) {
                c = Character.toUpperCase(c);
                upcase = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String hash(String s) {
        try {
            return hash(s, "SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            return Exceptions.chuck(ex);
        }
    }

    public static String hash(String s, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return Base64.getEncoder().encodeToString(digest.digest(s.getBytes(Charset.forName("UTF-8"))));
    }

    public static List<String> commaDelimitedToList(String commas, int lengthLimit) {
        if (commas == null || commas.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> tgs = new LinkedHashSet<>();
        for (String t : commas.split(",")) {
            t = t.trim();
            if (t.length() > lengthLimit) {
                throw new IllegalArgumentException("Length limit is " + lengthLimit);
            }
            if (!t.isEmpty()) {
                tgs.add(t);
            }
        }
        List<String> result = new ArrayList<>(tgs);
        Collections.sort(result);
        return result;
    }

    /**
     * Shuffle the characters in a string, and then extract some number of
     * characters from it as a new string to return.
     *
     * @param rnd A random
     * @param s The string
     * @param targetLength The desired length of the resulting string, limited
     * by the length of the input string
     * @return A new string containing a random choice of characters from the
     * original.
     */
    public static String shuffleAndExtract(Random rnd, String s, int targetLength) {
        targetLength = Math.min(targetLength, s.length());
        char[] c = s.toCharArray();
        ArrayUtils.shuffle(rnd, c);
        return new String(ArrayUtils.extract(c, 0, targetLength));
    }

    public static StringBuilder appendPaddedHex(byte val, StringBuilder sb) {
        String sval = Integer.toHexString(val & 0xFF);
        if (sval.length() == 1) {
            sb.append('0');
        }
        return sb.append(sval);
    }

    public static StringBuilder appendPaddedHex(short val, StringBuilder sb) {
        String sval = Integer.toHexString(val & 0xFFFF);
        for (int i = 0; i < 4 - sval.length(); i++) {
            sb.append('0');
        }
        return sb.append(sval);
    }

    public static StringBuilder appendPaddedHex(int val, StringBuilder sb) {
        String sval = Integer.toHexString(val & 0xFFFFFFFF);
        for (int i = 0; i < 8 - sval.length(); i++) {
            sb.append('0');
        }
        return sb.append(sval);
    }

    public static String toPaddedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            appendPaddedHex(b, sb);
        }
        return sb.toString();
    }

    public static String toNonPaddedBase36(byte[] bytes) {
        if (bytes.length % 8 != 0) {
            throw new IllegalArgumentException("Byte count must be divisible by 8");
        }
        LongBuffer buf = ByteBuffer.wrap(bytes).asLongBuffer();
        StringBuilder sb = new StringBuilder();
        while (buf.remaining() > 0) {
            sb.append(Long.toString(buf.get(), 36));
        }
        return sb.toString();
    }

    public static String toDelimitedPaddedBase36(byte[] bytes) {
        if (bytes.length % 8 != 0) {
            throw new IllegalArgumentException("Byte count must be divisible by 8");
        }
        LongBuffer buf = ByteBuffer.wrap(bytes).asLongBuffer();
        StringBuilder sb = new StringBuilder();
        while (buf.remaining() > 0) {
            long val = buf.get();
            if (val >= 0) {
                if (sb.length() > 0) {
                    sb.append('~');
                }
                sb.append(Long.toString(val, 36));
            } else {
                sb.append(Long.toString(val, 36));
            }
        }
        return sb.toString();
    }
}
