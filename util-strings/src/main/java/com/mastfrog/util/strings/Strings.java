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
package com.mastfrog.util.strings;

import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * String utilities - in particular, contains a number of utility methods for
 * performing String operations on CharSequence instances in-place, which are
 * useful for libraries such as Netty which implement 8-bit CharSequences, where
 * otherwise we would need to copy the bytes into a String to perform
 * operations.
 */
public final class Strings {

    public static String reverse(String s) {
        int max = s.length() - 1;
        char[] c = new char[max + 1];
        for (int i = 0; i <= max; i++) {
            c[i] = s.charAt(max - i);
        }
        return new String(c);
    }

    /**
     * Determine if a string has no contents other than whitespace more cheaply
     * than <code>String.trim().isEmpty()</code> - superseded by JDK 14's
     * <code>String.isBlank()</code> but useful when that cannot be depended on.
     *
     * @param s A string
     * @return true if only whitespace is encountered, the length is zero or the
     * string is null
     */
    public static boolean isBlank(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        int len = s.length();
        if (Character.isWhitespace(s.charAt(len - 1))) {
            return false;
        }
        for (int i = 0; i < len - 1; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine if a string has no contents other than whitespace more cheaply
     * than <code>String.trim().isEmpty()</code> - superseded by JDK 14's
     * <code>String.isBlank()</code> but useful when that cannot be depended on.
     *
     * @param seq A string
     * @return true if only whitespace is encountered, the length is zero or the
     * string is null
     */
    public static boolean isBlank(CharSequence seq) {
        if (seq == null || seq.length() == 0) {
            return true;
        }
        int len = seq.length();
        if (Character.isWhitespace(seq.charAt(len - 1))) {
            return false;
        }
        for (int i = 0; i < len - 1; i++) {
            if (!Character.isWhitespace(seq.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove leading and trailing quote characters from a string if both are
     * present.
     *
     * @param text The text
     * @param quote The quote character
     * @return A string with quotes stripped if present, the original if not
     */
    public static String dequote(String text, char quote) {
        return dequote(text, quote, quote);
    }

    /**
     * Remove leading and trailing quote characters from a string if both are
     * present.
     *
     * @param text The text
     * @param opening The opening quote character
     * @param closing The closing quote character
     * @return A string with quotes stripped if present, the original if not
     */
    public static String dequote(String text, char opening, char closing) {
        if (text.length() > 1) {
            if (text.charAt(0) == opening && text.charAt(1) == closing) {
                text = text.substring(1, text.length() - 1);
            }
        }
        return text;
    }

    /**
     * If the passed string is bracketed by " characters, remove them.
     *
     * @param text The text
     * @return The text with leading and trailing quotes stripped if both were
     * present
     */
    public static String dequote(String text) {
        return dequote(text, '"');
    }

    /**
     * If the passed string is bracketed by ' characters, remove them.
     *
     * @param text The text
     * @return The text with leading and trailing quotes stripped if both were
     * present
     */
    public static String deSingleQuote(String text) {
        return dequote(text, '\'');
    }

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
     * Trim a list of strings, removing any strings which are empty after
     * trimming, and returning a new list with the same traversal order as the
     * original. The resulting list is mutable.
     *
     * @param strings A list of strings
     * @return A new list of those strings, trimmed and empty strings pruned
     */
    public static List<String> trim(List<String> strings) {
        if (notNull("strings", strings).isEmpty()) {
            return strings;
        }
        List<String> result;
        if (strings instanceof LinkedList<?>) {
            result = new LinkedList<>();
        } else {
            result = new ArrayList<>();
        }
        for (String s : strings) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }

    /**
     * Trim a set of strings, removing any strings which are empty after
     * trimming, and returning a new set. If the original set was a
     * <code>SortedSet</code>, the result will be too. Otherwise the resulting
     * set will retain the traversal order of the input set. The resulting set
     * is mutable.
     *
     * @param strings A set of strings
     * @return A new set of those strings, trimmed and empty strings pruned
     */
    public static Set<String> trim(Set<String> strings) {
        if (notNull("strings", strings).isEmpty()) {
            return strings;
        }
        Set<String> result;
        if (strings instanceof SortedSet<?>) {
            result = new TreeSet<>();
        } else {
            result = new LinkedHashSet<>();
        }
        for (String s : strings) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Take an array of strings, and return an array of trimmed strings,
     * removing any empty strings.
     *
     * @param in An array of strings
     * @return A new array of trimmed strings;
     */
    public static String[] trim(String[] in) {
        if (notNull("in", in).length == 0) {
            return in;
        }
        String[] result = new String[in.length];
        System.arraycopy(in, 0, result, 0, result.length);
//        String[] result = ArrayUtils.copyOf(in);
        int last = 0;
        for (int i = 0; i < result.length; i++) {
            String trimmed = result[i].trim();
            if (!trimmed.isEmpty()) {
                result[last++] = trimmed;
            }
        }
        if (last == 0) {
            return new String[0];
        }
        if (last != in.length) {
            result = Arrays.copyOf(result, last);
        }
        return result;
    }

    /**
     * Get the sha1 hash of a string in UTF-8 encoding.
     *
     * @param s The string
     * @return The sha-1 hash
     */
    public static String sha1(String s) {
        MessageDigest digest = createDigest("SHA-1");
        byte[] result = digest.digest(s.getBytes(Charset.forName("UTF-8")));
        return toBase64(result);
    }

    private static MessageDigest createDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalArgumentException("No such algorithm: " + algorithm, ex);
        }
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
        if (notNull("seqs", seqs).length == 0) {
            return seqs;
        }
        CharSequence[] result = new CharSequence[seqs.length];
        System.arraycopy(seqs, 0, result, 0, seqs.length);
        int last = 0;
        for (int i = 0; i < result.length; i++) {
            CharSequence trimmed = trim(result[i]);
            if (trimmed.length() != 0) {
                result[last++] = trimmed;
            }
        }
        if (last == 0) {
            return new CharSequence[0];
        }
        if (last != seqs.length) {
            result = Arrays.copyOf(result, last);
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
     * Convenience method for joining strings delimited by ", ".
     *
     * @param parts Objects to invoke toString() on and concatenate together
     * @return A string
     */
    public static String commas(Iterable<?> parts) {
        return join(", ", parts);
    }

    /**
     * Convenience method for joining strings delimited by ", ".
     *
     * @param a The first object
     * @param b More objects
     * @return A string
     */
    public static String commas(Object a, Object... more) {
        if (a instanceof Iterable<?> && more.length == 0) {
            return join(", ", (Iterable<?>) a);
        }
        List<Object> all = new ArrayList<>(more.length + 1);
        return commas(all);
    }

    public static <T> void concatenate(String delimiter, Iterable<T> iter, StringBuilder into, Function<? super T, String> toString) {
        for (Iterator<T> it = iter.iterator(); it.hasNext();) {
            into.append(toString.apply(it.next()));
            if (it.hasNext()) {
                into.append(delimiter);
            }
        }
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

    public static <T> String join(char delim, Iterable<T> parts, Function<T, String> stringConvert) {
        StringBuilder sb = new StringBuilder(256);
        for (Iterator<T> it = parts.iterator(); it.hasNext();) {
            String sv = stringConvert.apply(it.next());
            if (sv != null && !sv.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(delim);
                }
                sb.append(sv);
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

    public static CharSequence[] split(char delim, CharSequence seq) {
        List<CharSequence> l = splitToList(delim, seq);
        return l.toArray(new CharSequence[l.size()]);
    }

    public static CharSequence[] splitOnce(char c, CharSequence cs) {
        int max = cs.length();
        int splitAt = -1;
        for (int i = 0; i < cs.length(); i++) {
            if (c == cs.charAt(i)) {
                splitAt = i;
                break;
            }
        }
        if (splitAt == -1) {
            return new CharSequence[]{cs};
        }
        CharSequence left = cs.subSequence(0, splitAt);
        if (splitAt < max - 1) {
            CharSequence right = cs.subSequence(splitAt + 1, max);
            return new CharSequence[]{left, right};
        }
        return new CharSequence[]{left};
    }

    public static String[] splitOnce(char c, String cs) {
        int max = cs.length();
        int splitAt = -1;
        for (int i = 0; i < cs.length(); i++) {
            if (c == cs.charAt(i)) {
                splitAt = i;
                break;
            }
        }
        if (splitAt == -1) {
            return new String[]{cs};
        }
        String left = cs.subSequence(0, splitAt).toString();
        if (splitAt < max - 1) {
            String right = cs.subSequence(splitAt + 1, max).toString();
            return new String[]{left, right};
        }
        return new String[]{left};
    }

    public static final CharSequence stripDoubleQuotes(CharSequence cs) {
        if (cs.length() >= 2) {
            if (cs.charAt(0) == '"') {
                cs = cs.subSequence(1, cs.length());
            }
            if (cs.charAt(cs.length() - 1) == '"') {
                cs = cs.subSequence(0, cs.length() - 1);
            }
        }
        return cs;
    }

    /**
     * URL encodes using UTF-8 and silently rethrowing the irritating
     * UnsupportedEncodingException that otherwise would need to be caught if
     * any JVM actually didn't support UTF-8.
     *
     * @param str The string to encode
     * @return An encoded string
     */
    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(notNull("str", str), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return Exceptions.chuck(ex);
        }
    }

    /**
     * URL decodes using UTF-8 and silently rethrowing the irritating
     * UnsupportedEncodingException that otherwise would need to be caught if
     * any JVM actually didn't support UTF-8.
     *
     * @param str The string to decode
     * @return An decoded string
     */
    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(notNull("str", str), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Split a string using a single-character delimiter; faster than
     * String.split() with regex.
     *
     * @param delim The delimiter character
     * @param seq The string
     * @return An array of strings
     */
    public static String[] split(char delim, String seq) {
        List<String> result = new ArrayList<>(20);
        int max = seq.length();
        int start = 0;
        for (int i = 0; i < max; i++) {
            char c = seq.charAt(i);
            boolean last = i == max - 1;
            if (c == delim || last) {
                result.add(seq.substring(start, last ? c == delim ? i : i + 1 : i));
                start = i + 1;
            }
        }
        return result.toArray(new String[result.size()]);
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

    /**
     * Split a string on a delimiter such as a comma, trimming the results and
     * eliminating empty and duplicate strings. So for
     * <code>foo,foo,,bar, foo,   bar</code> you get back a set of
     * <code>foo,bar</code>.
     *
     * @param delim The delimiter
     * @param seq The string to split
     * @return A set of strings
     */
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
        if (max == 0) {
            return;
        }
        for (int i = 0; i < max; i++) {
            char c = seq.charAt(i);
            if (delim == c || i == max - 1) {
                if (lastStart != i) {
                    int offset = i == max - 1 ? i + 1 : i;
                    if (i == max - 1 && delim == c) {
                        offset--;
                    }
                    CharSequence sub = seq.subSequence(lastStart, offset);
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

    /**
     * Determine if a character sequence starts with another sequence without
     * having to convert it to a string.
     *
     * @param target The target character sequence
     * @param start The proposed starting character sequence
     * @return True if the second argument is exactly the same characters as
     * that many initial characters of the first argument
     */
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

    /**
     * Determine if a character sequence starts with another sequence without
     * having to convert it to a string, and ignoring case.
     *
     * @param target The target character sequence
     * @param start The proposed starting character sequence
     * @return True if the second argument is exactly the same characters as
     * that many initial characters of the first argument, ignoring case
     */
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

    /**
     * Determine if two character sequences are the same, ignoring case.
     *
     * @param a The first sequence
     * @param b The second sequence
     * @return Whether or not they are equal, ignoring case
     */
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

    /**
     * Determine if one character sequence contains another, optionally ignoring
     * case, without converting to String.
     *
     * @param container The sequence to look in
     * @param contained The sequence to look for
     * @param ignoreCase If true, do case-insensitive comparison
     * @return True if the first argument contains the second, optionally
     * ignoring case
     */
    public static boolean charSequenceContains(CharSequence container, CharSequence contained, boolean ignoreCase) {
        notNull("container", container);
        notNull("contained", contained);
        if (!ignoreCase && container instanceof String && contained instanceof String) {
            return ((String) container).contains(contained);
        }
        int containerLength = container.length();
        int containedLength = contained.length();
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

    /**
     * Parse an integer from a character sequence without converting to String.
     *
     * @param seq The character sequence
     * @return An integer
     * @throws NumberFormatException for all the usual reasons
     */
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

    /**
     * Parse an long from a character sequence without converting to String.
     *
     * @param seq The character sequence
     * @return An integer
     * @throws NumberFormatException for all the usual reasons
     */
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

    /**
     * Find the first index of a character in a character sequence, without
     * converting to String.
     *
     * @param c A character
     * @param seq The sequence
     * @return The first index or -1
     */
    public static int indexOf(char c, CharSequence seq) {
        int max = seq.length();
        for (int i = 0; i < max; i++) {
            if (c == seq.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last index of a character in a character sequence, without
     * converting to String.
     *
     * @param c A character
     * @param seq The sequence
     * @return The first index or -1
     */
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
     * Replace all occurrances of a pattern in a string without the regular
     * expression handling done by String.replaceAll(). Does a single pass over
     * the characters, so the replacement text may contain the pattern.
     *
     * @param pattern The literal pattern
     * @param replacement The replacement
     * @param in The input string
     * @return A new string
     */
    public static String literalReplaceAll(String pattern, String replacement, String in) {
        return literalReplaceAll(pattern, replacement, in, false).toString();
    }

    /**
     * Replace all occurrances of a pattern in a string without the regular
     * expression handling done by String.replaceAll(). Does a single pass over
     * the characters, so the replacement text may contain the pattern.
     *
     * @param pattern The literal pattern
     * @param replacement The replacement
     * @param in The input string
     * @return A new string
     */
    public static String literalReplaceAllIgnoreCase(String pattern, String replacement, String in) {
        return literalReplaceAll(pattern, replacement, in, true).toString();
    }

    /**
     * Replace all occurrances of a pattern in a character sequence without the
     * regular expression handling done by String.replaceAll(). Does a single
     * pass over the characters, so the replacement text may contain the
     * pattern.
     *
     * @param pattern The literal pattern
     * @param replacement The replacement
     * @param in The input character sequence
     * @return A new character sequence
     */
    public static CharSequence literalReplaceAll(CharSequence pattern, CharSequence replacement, CharSequence in, boolean ignoreCase) {
        if (in.length() < pattern.length()) {
            return in;
        }
        if (pattern.length() == 0) {
            throw new IllegalArgumentException("Pattern is the empty string");
        }
        if (pattern.equals(replacement)) {
            throw new IllegalArgumentException("Replacing pattern with itself: " + pattern);
        }
        int max = in.length();
        StringBuilder result = new StringBuilder(in.length() + replacement.length());
        int patternEnd = pattern.length() - 1;
        int testPos = pattern.length() - 1;
        int lastMatch = -1;
        for (int i = max - 1; i >= 0; i--) {
            char realChar = in.charAt(i);
            char testChar = pattern.charAt(testPos);
            if (ignoreCase) {
                realChar = Character.toLowerCase(realChar);
                testChar = Character.toLowerCase(testChar);
            }
            if (realChar == testChar) {
                testPos--;
                if (lastMatch == -1) {
                    lastMatch = i;
                }
                if (testPos < 0) {
                    result.insert(0, replacement);
                    testPos = patternEnd;
                    lastMatch = -1;
                }
            } else {
                if (lastMatch != -1) {
                    CharSequence missed = in.subSequence(i, lastMatch + 1);
                    result.insert(0, missed);
                    lastMatch = -1;
                } else {
                    result.insert(0, realChar);
                }
                testPos = patternEnd;
            }
        }
        return result;
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
            int max = Array.getLength(o);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < max; i++) {
                Object item = Array.get(o, i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(Objects.toString(item));
            }
            return sb.toString();
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
        if (in instanceof String) {
            return ((String) in).indexOf(lookFor) != -1;
        }
        int max = in.length();
        for (int i = 0; i < max; i++) {
            if (in.charAt(i) == lookFor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert a camel-case sequence to hyphenated, e.g. thisThingIsWeird -&gt;
     * this-thing-is-weird.
     *
     * @param s A camel case sequence
     * @return A hyphenated sequence
     */
    public static String camelCaseToDashes(CharSequence s) {
        return camelCaseToDelimited(s, '-');
    }

    /**
     * Convert a camel-case sequence to hyphenated or otherwise delimited, e.g.
     * thisThingIsWeird -&gt; this_thing_is_weird.
     *
     * @param s A camel case sequence
     * @param delimiter the character to insert between the cirst capital letter
     * of one or more and the preceding character
     * @return A hyphenated sequence
     */
    public static String camelCaseToDelimited(CharSequence s, char delimiter) {
        StringBuilder sb = new StringBuilder();
        int max = s.length();
        for (int i = 0; i < max; i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append(delimiter);
                }
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * Convert hyphenated words to camel case, e.g. this-thing-is-weird -&gt;
     * thisThingIsWeird.
     *
     * @param s A hyphenated sequence
     * @return A camel case string
     */
    public static String dashesToCamelCase(CharSequence s) {
        return delimitedToCamelCase(s, '-');
    }

    /**
     * Convert delimited words to camel case, e.g. this_thing_is_weird -&gt;
     * thisThingIsWeird.
     *
     * @param s A hyphenated sequence
     * @return A camel case string
     */
    public static String delimitedToCamelCase(CharSequence s, char delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean upcase = true;
        int max = s.length();
        for (int i = 0; i < max; i++) {
            char c = s.charAt(i);
            if (c == delimiter) {
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

    /**
     * Get a SHA-1 hash of a string encoded as Base64.
     *
     * @param s The string
     * @return The base 64 SHA-1 hash of the string
     */
    public static String hash(String s) {
        try {
            return hash(s, "SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Get a URL-safe SHA-1 hash of a string encoded as Base64.
     *
     * @param s The string
     * @return The url-safe base 64 SHA-1 hash of the string
     */
    public static String urlHash(String s) {
        try {
            return urlHash(s, "SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            return Exceptions.chuck(ex);
        }
    }

    public static String hash(String s, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return Base64.getEncoder().encodeToString(digest.digest(s.getBytes(Charset.forName("UTF-8"))));
    }

    public static String urlHash(String s, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return Base64.getUrlEncoder().encodeToString(digest.digest(s.getBytes(Charset.forName("UTF-8"))));
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
        shuffle(rnd, c);
        return new String(Arrays.copyOf(c, targetLength));
    }

    /**
     * Fisher-Yates shuffle.
     *
     * @param rnd A random
     * @param array An array
     */
    static void shuffle(Random rnd, char[] array) {
        for (int i = 0; i < array.length - 2; i++) {
            int r = rnd.nextInt(array.length);
            if (i != r) {
                char hold = array[i];
                array[i] = array[r];
                array[r] = hold;
            }
        }
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

    public static StringBuilder appendPaddedHex(long val, StringBuilder sb) {
        String sval = Long.toHexString(val);
        for (int i = 0; i < 16 - sval.length(); i++) {
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

    public static String toPaddedHex(byte[] bytes, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            appendPaddedHex(b, sb);
            if (i != bytes.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String toPaddedHex(short[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (short b : bytes) {
            appendPaddedHex(b, sb);
        }
        return sb.toString();
    }

    public static String toPaddedHex(short[] bytes, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            short b = bytes[i];
            appendPaddedHex(b, sb);
            if (i != bytes.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String toHex(char c) {
        String result = Integer.toString(c, 16);
        if (result.length() == 1) {
            result = "0" + result;
        }
        return result;
    }

    public static String toPaddedHex(int[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int b : bytes) {
            appendPaddedHex(b, sb);
        }
        return sb.toString();
    }

    public static String toPaddedHex(int[] bytes, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            appendPaddedHex(b, sb);
            if (i != bytes.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String toPaddedHex(long[] longs, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longs.length; i++) {
            long b = longs[i];
            appendPaddedHex(b, sb);
            if (i != longs.length) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
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

    /**
     * String equality test which is slower than String.equals(), but is
     * constant-time, so useful in cryptography to avoid a
     * <a href="http://codahale.com/a-lesson-in-timing-attacks/">timing
     * attack</a>.
     */
    public static boolean timingSafeEquals(String first, String second) {
        if (first == null) {
            return second == null;
        } else if (second == null) {
            return false;
        } else if (second.length() <= 0) {
            return first.length() <= 0;
        }
        char[] firstChars = first.toCharArray();
        char[] secondChars = second.toCharArray();
        char result = (char) ((firstChars.length == secondChars.length) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < firstChars.length; ++i) {
            result |= firstChars[i] ^ secondChars[j];
            j = (j + 1) % secondChars.length;
        }
        return result == 0;
    }

    /**
     * String equality test which is slower than String.equals(), but is
     * constant-time, so useful in cryptography to avoid a
     * <a href="http://codahale.com/a-lesson-in-timing-attacks/">timing
     * attack</a>.
     *
     * @param first The first character sequence
     * @param second The second character sequence
     * @return Whether or not the two sequences are equal
     */
    public static boolean timingSafeEquals(CharSequence first, CharSequence second) {
        if (first == null) {
            return second == null;
        } else if (second == null) {
            return false;
        } else if (second.length() <= 0) {
            return first.length() <= 0;
        }
        int firstLength = first.length();
        int secondLength = second.length();
        char result = (char) ((firstLength == secondLength) ? 0 : 1);
        int j = 0;
        for (int i = 0; i < firstLength; ++i) {
            result |= first.charAt(i) ^ second.charAt(j);
            j = (j + 1) % secondLength;
        }
        return result == 0;
    }

    public static CharSequence replaceAll(final char c, String replacement, CharSequence in) {
        if (indexOf(c, in) < 0) {
            return in;
        }
        StringBuilder sb = new StringBuilder(in.length() + 4);
        int max = in.length();
        for (int i = 0; i < max; i++) {
            char cc = in.charAt(i);
            if (cc == c) {
                sb.append(replacement);
            } else {
                sb.append(cc);
            }
        }
        return sb;
    }

    public static CharSequence quickJson(Object... args) {
        if ((args.length % 2) != 0) {
            throw new IllegalArgumentException("Odd number of arguments: " + join(',', args));
        }
        AppendableCharSequence sb = new AppendableCharSequence('{');
        for (int i = 0; i < args.length; i += 2) {
            CharSequence key = jsonArgument(args[i]);
            CharSequence val = jsonArgument(args[i + 1]);
            sb.append(key).append(':').append(val);
            if (i != args.length - 2) {
                sb.append(',');
            }
        }
        return sb.append('}');
    }

    private static CharSequence jsonArgument(Object o) {
        if (o instanceof Collection<?> || (o != null && o.getClass().isArray())) {
            if (o.getClass().isArray()) {
                int max = Array.getLength(o);
                List<Object> l = new ArrayList<>();
                for (int i = 0; i < max; i++) {
                    l.add(Array.get(o, i));
                }
                o = l;
            }
            Collection<?> c = (Collection<?>) o;
            AppendableCharSequence sq = new AppendableCharSequence(c.size() + 4);
            sq.append(singleChar('['));
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                Object o1 = it.next();
                sq.append(jsonArgument(o1));
                if (it.hasNext()) {
                    sq.append(singleChar(','));
                }
            }
            sq.append(singleChar(']'));
            return sq;
        }
        if (o != null
                && !(o instanceof Number)
                && !(o instanceof CharSequence)
                && !(o instanceof Boolean)
                && !(o instanceof Date)
                && !(o instanceof ZonedDateTime)
                && !(o instanceof OffsetDateTime)
                && !(o instanceof LocalDateTime)
                && !(o instanceof Duration)
                && !(o instanceof Instant)
                && !(o instanceof Enum<?>)) {
            throw new IllegalArgumentException("quickJson does not support " + o.getClass().getName());
        }
        return escapeJson(o);
    }

    private static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            .toFormatter(Locale.US);

    private static final ZoneId GMT = ZoneId.of("GMT");

    private static CharSequence escapeJson(Object o) {
        if (o instanceof Date) {
            Date d = (Date) o;
            ZonedDateTime zdt = ZonedDateTime.ofInstant(d.toInstant(), GMT);
            return quote(zdt.format(ISO_INSTANT));
        } else if (o instanceof ZonedDateTime) {
            ZonedDateTime zdt = (ZonedDateTime) o;
            return quote(zdt.format(ISO_INSTANT));
        } else if (o instanceof OffsetDateTime) {
            OffsetDateTime odt = (OffsetDateTime) o;
            return quote(odt.format(ISO_INSTANT));
        } else if (o instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) o;
            return quote(ldt.format(ISO_INSTANT));
        } else if (o instanceof Instant) {
            Instant ins = (Instant) o;
            return quote(ZonedDateTime.ofInstant(ins, GMT).format(ISO_INSTANT));
        } else if (o instanceof Duration) {
            Duration dur = (Duration) o;
            return quote(dur.toString());
        } else if (o instanceof Enum<?>) {
            return quote(((Enum<?>) o).name());
        } else if (o instanceof Number || o instanceof Boolean) {
            return o.toString();
        } else if (o == null) {
            return "null";
        }

        String stringRep = o.toString();
        CharSequence result = replaceAll('"', "\\\"", stringRep);
        result = replaceAll('\n', "\\n", result);
        result = replaceAll('\t', "\\t", result);
        return '"' + result.toString() + '"';
    }

    private static String quote(String s) {
        return '"' + s + '"';
    }

    public static AppendingCharSequence newAppendingCharSequence() {
        return new AppendableCharSequence(5);
    }

    public static AppendingCharSequence newAppendingCharSequence(int components) {
        return new AppendableCharSequence(components);
    }

    public static AppendingCharSequence newAppendingCharSequence(CharSequence seqs) {
        return new AppendableCharSequence(seqs);
    }

    /**
     * Returns a predicate which will extremely efficiently answer the question
     * of whether a passed character sequence starts with any of the passed
     * ones.
     *
     * @param sequences An array of character sequences none of which may be a
     * prefix to another and none of which may be empty
     * @return A predicate
     */
    public static Predicate<CharSequence> matchPrefixes(CharSequence... sequences) {
        return MatchWords.matchPrefixes(sequences);
    }

    /**
     * Returns a predicate which will extremely efficiently answer the question
     * of whether a passed character sequence matches any of the passed ones.
     *
     * @param sequences An array of character sequences, none of which may be
     * empty
     * @return A predicate
     */
    public static Predicate<CharSequence> matchWords(CharSequence... sequences) {
        return MatchWords.matchWords(sequences);
    }

    /**
     * Returns a function which will extremely efficiently answer the question
     * of whether a passed character sequence starts with any of the passed
     * ones, returning the match or null.
     *
     * @param sequences An array of character sequences none of which may be a
     * prefix to another and none of which may be empty
     * @return A predicate
     */
    public static Function<CharSequence, CharSequence> findPrefixes(CharSequence... prefixen) {
        return MatchWords.findPrefixes(prefixen);
    }

    /**
     * Perform escaping adequate to convert a string into a JSON or Java source
     * string.
     *
     * @param seq A character sequence
     * @return An escaped string
     */
    public static String escapeControlCharactersAndQuotes(CharSequence seq) {
        int len = seq.length();
        StringBuilder sb = new StringBuilder(seq.length() + 1);
        escapeControlCharactersAndQuotes(seq, len, sb);
        return sb.toString();
    }

    /**
     * Perform escaping adequate to convert a string into a JSON or Java source
     * string, appending the result to the passed StringBuidler.
     *
     * @param seq A character sequence
     * @param into A string builder
     * @return An escaped string
     */
    public static void escapeControlCharactersAndQuotes(CharSequence seq, StringBuilder into) {
        escapeControlCharactersAndQuotes(seq, seq.length(), into);
    }

    public static String singleQuote(CharSequence what) {
        return quote('\'', what);
    }

    public static String quote(CharSequence what) {
        return quote('"', what);
    }

    public static String quotedLines(Iterable<? extends CharSequence> items) {
        return quote("\n", items);
    }

    public static String quotedCommaDelimitedLines(Iterable<? extends CharSequence> items) {
        return quote(",\n", items);
    }

    public static String quote(String delimiter, Iterable<? extends CharSequence> items) {
        return quote('"', delimiter, items);
    }

    public static String singleQuote(String delimiter, Iterable<? extends CharSequence> items) {
        return quote('\'', delimiter, items);
    }

    public static String quote(char quoteChar, String delimiter, Iterable<? extends CharSequence> items) {
        StringBuilder sb = new StringBuilder();
        boolean hasDelimiter = delimiter != null && !delimiter.isEmpty();
        for (Iterator<? extends CharSequence> it = notNull("items", items).iterator(); it.hasNext();) {
            CharSequence seq = it.next();
            appendQuoted(quoteChar, seq, Escaper.CONTROL_CHARACTERS.escaping(quoteChar), sb);
            if (hasDelimiter && it.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String quote(char quoteChar, CharSequence what) {
        StringBuilder sb = new StringBuilder(what.length() + 12);
        appendQuoted(quoteChar, what, Escaper.CONTROL_CHARACTERS.escaping(quoteChar), sb);
        return sb.toString();
    }

    private static StringBuilder appendQuoted(char quoteChar, CharSequence what, Escaper escaper, StringBuilder into) {
        into.append(quoteChar);
        escape(what, what.length(), escaper, into);
        into.append(quoteChar);
        return into;
    }

    public static String escape(CharSequence seq, Escaper escaper) {
        StringBuilder sb = new StringBuilder();
        escape(seq, seq.length(), escaper, sb);
        return sb.toString();
    }

    public static void escape(CharSequence seq, int len, Escaper escaper, StringBuilder into) {
        for (int i = 0; i < len; i++) {
            char c = seq.charAt(i);
            CharSequence escaped = escaper.escape(c);
            if (escaped != null) {
                into.append(escaped);
            } else {
                into.append(c);
            }
        }
    }

    private static void escapeControlCharactersAndQuotes(CharSequence seq, int len, StringBuilder sb) {
        escape(seq, len, Escaper.CONTROL_CHARACTERS.escapeDoubleQuotes(), sb);
    }

    /**
     * Returns a string representation of an integer, prefixed with zeros to fit
     * the desired length. If the result will not fit in the prescribed number
     * of characters, a longer string is returned. If the number is negative,
     * and the requested length is greater than the number of characters
     * requested, the leading zero is replaced with a '-' so the desired length
     * is still returned. So,
     * <ul>
     * <li><code>zeroPrefix(124, 4) = "0124"</code></li>
     * <li><code>zeroPrefix(-124, 4) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 3) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 5) = "-0124"</code></li>
     * </ul>
     *
     * @param value A number
     * @param length A number of characters for the result, which will be padded
     * with leading zeros as needed
     * @return A string representation of the number which will parse to the
     * original number and may have leading zeros.
     */
    public static String zeroPrefix(int value, int length) {
        return new String(zeroPrefixChars(value, length));
    }

    /**
     * Returns a string representation of an integer, prefixed with zeros to fit
     * the desired length. If the result will not fit in the prescribed number
     * of characters, a longer string is returned. If the number is negative,
     * and the requested length is greater than the number of characters
     * requested, the leading zero is replaced with a '-' so the desired length
     * is still returned. So,
     * <ul>
     * <li><code>zeroPrefix(124, 4) = "0124"</code></li>
     * <li><code>zeroPrefix(-124, 4) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 3) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 5) = "-0124"</code></li>
     * </ul>
     *
     * @param value A number
     * @param length A number of characters for the result, which will be padded
     * with leading zeros as needed
     * @return A string representation of the number which will parse to the
     * original number and may have leading zeros.
     */
    public static String zeroPrefix(long value, int length) {
        return new String(zeroPrefixChars(value, length));
    }

    static char[] writeInto(int value, char[] target) {
        if (value == Integer.MIN_VALUE) {
            return writeInto((long) value, target);
        }
        if (value == 0) {
            if (target.length > 0) {
                Arrays.fill(target, '0');
                return target;
            } else {
                return new char[]{'0'};
            }
        }
        int cursor = target.length - 1;
        boolean negative = value < 0;
        int curr = value;
        int pos = 0;
        for (; curr != 0; pos++) {
            if (cursor < 0) {
                char[] nue = new char[target.length + 1];
                System.arraycopy(target, 0, nue, 1, target.length);
                target = nue;
                cursor++;
            }
            target[cursor--] = (char) ('0' + Math.abs(curr % 10));
            curr /= 10;
            if (curr == 0) {
                break;
            }
        }
        if (pos < target.length) {
            Arrays.fill(target, 0, target.length - (pos + 1), '0');
        }
        if (negative) {
            if (pos < target.length) {
                if (target[0] != '0') {
                    char[] nue = new char[target.length + 1];
                    System.arraycopy(target, 0, nue, 1, target.length);
                    target = nue;
                }
                target[0] = '-';
            } else {
                char[] nue = new char[target.length + 1];
                System.arraycopy(target, 0, nue, 1, target.length);
                target = nue;
                target[0] = '-';
            }
        }
        return target;
    }

    static char[] writeInto(long value, char[] target) {
        if (value == 0) {
            if (target.length > 0) {
                Arrays.fill(target, '0');
                return target;
            } else {
                return new char[]{'0'};
            }
        }
        int cursor = target.length - 1;
        boolean negative = value < 0;
        long curr = value;
        int pos = 0;
        for (; curr != 0; pos++) {
            if (cursor < 0) {
                char[] nue = new char[target.length + 1];
                System.arraycopy(target, 0, nue, 1, target.length);
                target = nue;
                cursor++;
            }
            char nextChar = (char) ('0' + Math.abs(curr % 10));
            assert nextChar >= '0' && nextChar <= '9' : "@ " + pos + " '"
                    + nextChar + "' " + cursor + " '" + new String(target)
                    + "' " + value;
            target[cursor--] = nextChar;
            curr /= 10L;
            if (curr == 0) {
                break;
            }
        }
        if (pos < target.length) {
            Arrays.fill(target, 0, target.length - (pos + 1), '0');
        }
        if (negative) {
            if (pos < target.length) {
                if (target[0] != '0') {
                    char[] nue = new char[target.length + 1];
                    System.arraycopy(target, 0, nue, 1, target.length);
                    target = nue;
                }
                target[0] = '-';
            } else {
                char[] nue = new char[target.length + 1];
                System.arraycopy(target, 0, nue, 1, target.length);
                target = nue;
                target[0] = '-';
            }
        }
        return target;
    }

    /**
     * Returns a string representation of an integer, prefixed with zeros to fit
     * the desired length. If the result will not fit in the prescribed number
     * of characters, a longer string is returned. If the number is negative,
     * and the requested length is greater than the number of characters
     * requested, the leading zero is replaced with a '-' so the desired length
     * is still returned. So,
     * <ul>
     * <li><code>zeroPrefix(124, 5) = "00124"</code></li>
     * <li><code>zeroPrefix(124, 4) = "0124"</code></li>
     * <li><code>zeroPrefix(-124, 4) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 3) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 5) = "-0124"</code></li>
     * </ul>
     *
     * @param value A number
     * @param length A number of characters for the result, which will be padded
     * with leading zeros as needed
     * @return A string representation of the number which will parse to the
     * original number and may have leading zeros as a character array
     */
    public static char[] zeroPrefixChars(int value, int length) {
        Checks.nonNegative("length", length);
        if (value >= 0 && value < 10 && length > 0) {
            char[] result = new char[length];
            Arrays.fill(result, 0, length - 1, '0');
            result[length - 1] = (char) ('0' + value);
            return result;
        }
        return writeInto(value, new char[length]);
    }

    /**
     * Returns a string representation of an integer, prefixed with zeros to fit
     * the desired length. If the result will not fit in the prescribed number
     * of characters, a longer string is returned. If the number is negative,
     * and the requested length is greater than the number of characters
     * requested, the leading zero is replaced with a '-' so the desired length
     * is still returned. So,
     * <ul>
     * <li><code>zeroPrefix(124, 4) = "0124"</code></li>
     * <li><code>zeroPrefix(-124, 4) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 3) = "-124"</code></li>
     * <li><code>zeroPrefix(-124, 5) = "-0124"</code></li>
     * </ul>
     *
     * @param value A number
     * @param length A number of characters for the result, which will be padded
     * with leading zeros as needed
     * @return A string representation of the number which will parse to the
     * original number and may have leading zeros as a character array
     */
    public static char[] zeroPrefixChars(long value, int length) {
        Checks.nonNegative("length", length);
        if (value >= 0 && value < 10 && length > 0) {
            char[] result = new char[length];
            Arrays.fill(result, 0, length - 1, '0');
            result[length - 1] = (char) ('0' + value);
            return result;
        }
        return writeInto(value, new char[length]);
    }

    /**
     * If a string, trimmed, starts and ends with the same character and that
     * character is ' or ", returns the subsequence between the quote
     * characters.
     *
     * @param s A string
     * @return The string or the unquoted substring
     */
    public static String unquote(String s) {
        s = s.trim();
        if (s.length() > 1) {
            char start = s.charAt(0);
            if (start == '"' || start == '\'') {
                char end = s.charAt(s.length() - 1);
                if (end == start) {
                    return s.substring(1, s.length() - 1);
                }
            }
        }
        return s;
    }

    /**
     * If a character sequence starts and ends with the same character and that
     * character is ' or ", returns the subsequence between the quote
     * characters.
     *
     * @param s A string
     * @return The string or the unquoted substring
     */
    public static CharSequence unquoteCharSequence(CharSequence s) {
        int len = s.length();
        if (len > 1) {
            char start = s.charAt(0);
            if (start == '"' || start == '\'') {
                char end = s.charAt(len - 1);
                if (end == start) {
                    return s.subSequence(1, s.length() - 1);
                }
            }
        }
        return s;

    }

    /**
     * For template logging where you don't want to call toString() on some
     * object unless it actually is getting logged, simply wraps the object you
     * want to log in another which delegates its toString() method.
     *
     * @param stringify An object to stringify or null
     * @return An object whose toString() method returns the same value
     */
    public static Object lazy(Object stringify) {
        return new LazyToString(stringify);
    }

    public static Object wrappedSupplier(Supplier<String> s) {
        return new LazySupplierToString(s);
    }

    /**
     * Capitalize a string.
     *
     * @param orig The original
     * @return The original if already capitalized, or a capitalized copy.
     */
    public static String capitalize(CharSequence orig) {
        if (orig.length() == 0 || Character.isUpperCase(orig.charAt(0))) {
            return orig instanceof String ? (String) orig : orig.toString();
        }
        StringBuilder sb = new StringBuilder(orig.length());
        for (int i = 0; i < orig.length(); i++) {
            switch (i) {
                case 0:
                    sb.append(Character.toUpperCase(orig.charAt(i)));
                    break;
                default:
                    sb.append(orig.charAt(i));
                    break;
            }
        }
        return sb.toString();
    }

    static final class LazyToString {

        private final Object stringify;

        public LazyToString(Object stringify) {
            this.stringify = stringify;
        }

        public String toString() {
            return Objects.toString(stringify);
        }
    }

    static final class LazySupplierToString {

        private final Supplier<String> stringify;

        public LazySupplierToString(Supplier<String> stringify) {
            this.stringify = stringify;
        }

        public String toString() {
            String result = stringify.get();
            return result == null ? "null" : result;

        }
    }

}
