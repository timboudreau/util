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
import com.mastfrog.util.preconditions.InvalidArgumentException;
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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final BitSet PUNC = new BitSet(128);
    private static final CharSequence ELLIPSIS = new SingleCharSequence('\u2026');
    private static final int MAX_ELLIPSIS_SKEW = 12;
    private static boolean puncInitialized;

    static {
        for (int i = 0; i < 128; i++) {
            if (isPunctuation((char) i)) {
                PUNC.set(i);
            }
        }
        puncInitialized = true;
    }

    /**
     * Determine if an entier character sequence is punctuation by process of
     * elimination - not a letter, digit, whitespace, iso control, or unicode
     * "letter-like symbol".
     *
     * @param c A character
     * @return true if the character fits no other category.
     * @since 2.6.13
     */
    public static boolean isPunctuation(CharSequence name) {
        int len = name.length();
        if (len == 0) {
            return false;
        }
        if (len == 1) {
            return isPunctuation(name.charAt(0));
        }
        return is(name, Strings::isPunctuation);
    }

    /**
     * Determine if a character is punctuation by process of elimination - not a
     * letter, digit, whitespace, iso control, or unicode "letter-like symbol".
     *
     * @param c A character
     * @return true if the character fits no other category.
     * @since 2.6.13
     */
    public static boolean isPunctuation(char c) {
        if (puncInitialized && c < 128) {
            return PUNC.get((int) c);
        }
        return !(Character.isLetter(c) || Character.isDigit(c)
                || Character.isWhitespace(c) || Character.isISOControl(c)
                || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.LETTERLIKE_SYMBOLS);
    }

    /**
     * Reverse a string.
     *
     * @param s A string
     * @return The string wtth its characters reversed
     */
    public static String reverse(String s) {
        int max = s.length() - 1;
        if (max < 0) {
            return s;
        }
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
     * @since 2.6.11
     */
    public static boolean isBlank(String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        int len = s.length();
        if (len == 1) {
            return Character.isWhitespace(s.charAt(0));
        }
        if (!Character.isWhitespace(s.charAt(len - 1))) {
            return false;
        }
        return is(s, Character::isWhitespace);
    }

    /**
     * Determine if a string has no contents other than whitespace more cheaply
     * than <code>String.trim().isEmpty()</code> - superseded by JDK 14's
     * <code>String.isBlank()</code> but useful when that cannot be depended on.
     *
     * @param seq A string
     * @return true if only whitespace is encountered, the length is zero or the
     * string is null
     * @since 2.6.11
     */
    public static boolean isBlank(CharSequence seq) {
        if (seq == null || seq.length() == 0) {
            return true;
        }
        return is(seq, Character::isWhitespace);
    }

    /**
     * Remove leading and trailing quote characters from a string if both are
     * present.
     *
     * @param text The text
     * @param quote The quote character
     * @return A string with quotes stripped if present, the original if not
     * @since 2.6.10
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
     * @since 2.6.10
     */
    public static String dequote(String text, char opening, char closing) {
        if (text.length() > 1) {
            if (text.charAt(0) == opening && text.charAt(text.length() - 1) == closing) {
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
     * @since 2.6.10
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
     * @since 2.6.10
     */
    public static String deSingleQuote(String text) {
        return dequote(text, '\'');
    }
    
    /**
     * Turn a string into an optional which is present only if the passed
     * text is not null or empty.
     * 
     * @param text Some text or null
     * @return An optional
     * @since 2.8.3
     */
    public static Optional<String> ifNonBlank(String text) {
        if (text == null || text.isEmpty() || isBlank(text)) {
            return Optional.empty();
        }
        return Optional.of(text);
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
        try ( PrintWriter p = new PrintWriter(w)) {
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
    @Deprecated
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

    public static int countOccurrences(char c, CharSequence in) {
        int result = 0;
        int max = in.length();
        for (int i = 0; i < max; i++) {
            if (c == in.charAt(i)) {
                result++;
            }
        }
        return result;
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
        if (!ignoreCase && a.getClass() == b.getClass()) {
            return a.equals(b);
        }
        if (!ignoreCase && a instanceof String) {
            return ((String) a).contentEquals(b);
        } else if (!ignoreCase && b instanceof String) {
            return ((String) b).contentEquals(a);
        } else {
            if (ignoreCase) {
                return contentEqualsIgnoreCase(a, b);
            }
            return biIterate(a, (index, ch, l, remaining) -> {
                boolean match = ch == b.charAt(index);
                return !match ? BiIterateResult.NO : BiIterateResult.MAYBE;
            }).isOk();
        }
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
            int result = Character.compare(ac, bc);
            if (result != 0) {
                return result;
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
        return caseInsensitive ? CharSequenceComparator.INSENSITIVE : CharSequenceComparator.SENSITIVE;
    }

    /**
     * Get a comparator that calls compareCharSequences().
     *
     * @return A comparator
     */
    public static Comparator<CharSequence> charSequenceComparator() {
        return CharSequenceComparator.INSENSITIVE;
    }

    private static final class CharSequenceComparator implements Comparator<CharSequence> {

        private static final Comparator<CharSequence> INSENSITIVE = new CharSequenceComparator(true);
        private static final Comparator<CharSequence> SENSITIVE = new CharSequenceComparator(false);

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

    /**
     * Delimit and concatenate objects using a custom stringifier, into an
     * existing StringBuilder.
     *
     * @param <T> The type
     * @param delimiter The delimiter
     * @param iter The collection of objects
     * @param into The destination buffer
     * @param toString The stringifying method
     */
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

    /**
     * Concatenate objects to a delimited string , using a custom stringifier
     * function.
     *
     * @param <T> The object type
     * @param delim The delimiter
     * @param parts The collection of objects
     * @param stringConvert The converter function
     * @return A string
     */
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
    private static final SingleCharSequence SLASH = new SingleCharSequence('/');

    /**
     * Create a lightwieight single-character character sequence with minimal
     * footprint.
     *
     * @param c A character
     * @return A character sequence
     */
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
            case '/':
                return SLASH;
            case '\u2026':
                return ELLIPSIS;
            default:
                return new SingleCharSequence(c);
        }
    }

    /**
     * Split a character sequence on a character
     *
     * @param delim A character
     * @param seq A character sequence to split
     * @return An array of character sequences
     */
    public static CharSequence[] split(char delim, CharSequence seq) {
        if (seq.length() == 0) {
            return new CharSequence[0];
        }
        List<CharSequence> l = splitToList(delim, seq);
        return l.toArray(new CharSequence[l.size()]);
    }

    /**
     * Split a character sequence on the first occurrence of the passed
     * character, if any.
     *
     * @param c The character
     * @param cs The sequence
     * @return An array of strings - may be 1 length if not present
     */
    public static CharSequence[] splitOnce(char c, CharSequence cs) {
        int max = cs.length();
        if (max == 0) {
            return new CharSequence[0];
        }
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

    /**
     * Split a string on the first occurrence of the passed character, if any.
     *
     * @param c The character
     * @param cs The sequence
     * @return An array of strings - may be 1 length if not present
     */
    public static String[] splitOnce(char c, String cs) {
        int max = cs.length();
        if (max == 0) {
            return new String[0];
        }
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

    /**
     * Remove leading and trailing double quotes if present.
     *
     * @param cs A character sequence
     * @return A character sequence
     */
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

    /**
     * Split a character sequence on occurrences of a character, passing them to
     * the passed predicate and aborting splitting if it returns false - for
     * splitting strings when some initial number of matches is sufficient.
     *
     * @param delim A delimiter character
     * @param seq a character sequence
     * @param proc A predicate which receives split elements until it returns
     * false
     */
    public static void split(char delim, CharSequence seq, Predicate<CharSequence> proc) {
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
                    if (!proc.test(sub)) {
                        return;
                    }
                } else {
                    if (!proc.test("")) {
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
        if (target.charAt(startLength - 1) != start.charAt(startLength - 1)) {
            return false;
        }
        for (int i = 0; i < startLength - 1; i++) {
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
        if (Character.toLowerCase(target.charAt(startLength - 1)) != Character.toLowerCase(start.charAt(startLength - 1))) {
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
        return biIterate(a, (index, ch, l, remaining) -> {
            boolean match = Character.toLowerCase(ch) == Character.toLowerCase(b.charAt(index));
            return !match ? BiIterateResult.NO : BiIterateResult.MAYBE;
        }).isOk();
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
     * Convert a hex character 0-9a-fA-F to the corresponding int 0-16.
     *
     * @param c A char
     * @return An int
     * @throws NumberFormatException if an invalid character is passed
     */
    public static int charToNybbl(char c) {
        int val;
        switch (c) {
            case '0':
                val = 0;
                break;
            case '1':
                val = 1;
                break;
            case '2':
                val = 2;
                break;
            case '3':
                val = 3;
                break;
            case '4':
                val = 4;
                break;
            case '5':
                val = 5;
                break;
            case '6':
                val = 6;
                break;
            case '7':
                val = 7;
                break;
            case '8':
                val = 8;
                break;
            case '9':
                val = 9;
                break;
            case 'A':
            case 'a':
                val = 10;
                break;
            case 'B':
            case 'b':
                val = 11;
                break;
            case 'C':
            case 'c':
                val = 12;
                break;
            case 'D':
            case 'd':
                val = 13;
                break;
            case 'E':
            case 'e':
                val = 14;
                break;
            case 'F':
            case 'f':
                val = 15;
                break;
            default:
                throw new NumberFormatException("Invalid character '" + c
                        + "' is not hex");
        }
        return val;
    }

    public static byte parseHexByte(CharSequence seq) {
        if (Strings.startsWith(seq, "0x")) {
            seq = seq.subSequence(2, seq.length());
        }
        switch (seq.length()) {
            case 0:
                return 0;
            case 1:
                return (byte) charToNybbl(seq.charAt(0));
            case 2:
                return (byte) ((charToNybbl(seq.charAt(0)) << 4)
                        | (charToNybbl(seq.charAt(1))));
            default:
                throw new NumberFormatException("Invalid length for single byte "
                        + "of hex: " + seq.length() + " in '" + seq + "'");
        }
    }

    /**
     * Parses hexedecimal efficiently, tolerating a leading "0x", and does not
     * fail on negative numbers as Integer.parseInt() does; and does not require
     * an instance of String, only CharSequence.
     *
     * @param seq A character sequence
     * @throws NumberFormatException if not a valid number
     * @return An int
     */
    public static short parseHexShort(CharSequence seq) {
        short result = 0;
        int last = seq.length() - 1;
        if (last > 3) {
            throw new NumberFormatException("Too many characters (> 8) for an "
                    + "int in '" + seq + "'");
        }
        if (Strings.startsWith(seq, "0x")) {
            seq = seq.subSequence(2, seq.length());
        }
        for (int i = last, j = 0; i >= 0; i--, j += 4) {
            int val = charToNybbl(seq.charAt(i));
            val <<= j;
            result |= val;
        }
        return result;
    }

    /**
     * Parses hexedecimal efficiently, tolerating a leading "0x", and does not
     * fail on negative numbers as Integer.parseInt() does; and does not require
     * an instance of String, only CharSequence.
     *
     * @param seq A character sequence
     * @throws NumberFormatException if not a valid number
     * @return An int
     */
    public static int parseHexInt(CharSequence seq) {
        int result = 0;
        int last = seq.length() - 1;
        if (last > 7) {
            throw new NumberFormatException("Too many characters (> 8) for an "
                    + "int in '" + seq + "'");
        }
        if (Strings.startsWith(seq, "0x")) {
            seq = seq.subSequence(2, seq.length());
        }
        for (int i = last, j = 0; i >= 0; i--, j += 4) {
            int val = charToNybbl(seq.charAt(i));
            val <<= j;
            result |= val;
        }
        return result;
    }

    /**
     * Parses hexedecimal efficiently, tolerating a leading "0x", and does not
     * fail on negative numbers as Long.parseLong() does; and does not require
     * an instance of String, only CharSequence.
     *
     * @param seq A character sequence
     * @throws NumberFormatException if not a valid number
     * @return A long
     */
    public static long parseHexLong(CharSequence seq) {
        long result = 0;
        int last = seq.length() - 1;
        if (last > 15) {
            throw new NumberFormatException("Too many characters (> 16) for a "
                    + "long in '" + seq + "'");
        }
        if (Strings.startsWith(seq, "0x")) {
            seq = seq.subSequence(2, seq.length());
        }
        for (int i = last, j = 0; i >= 0; i--, j += 4) {
            long val = charToNybbl(seq.charAt(i));
            val <<= j;
            result |= val;
        }
        return result;
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
                sb.append(toString(item));
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
        char lookFor2 = Character.toUpperCase(lookFor);
        return biIterate(in, (index, ch, len, remaining) -> {
            if (ch == lookFor1 || ch == lookFor2) {
                return BiIterateResult.YES;
            }
            return BiIterateResult.MAYBE;
        }).isSuccess();
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
        return biIterate(in, (index, ch, len, rem) -> {
            return ch == lookFor ? BiIterateResult.YES : BiIterateResult.MAYBE;
        }).isSuccess();
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
        return appendPaddedHex(val, false, sb);
    }

    public static StringBuilder appendPaddedHex(byte val, boolean upperCase, StringBuilder sb) {
        int right = val & 0x0F;
        int left = (val & 0xF0) >> 4;
        return sb.append(nybblChar(left, upperCase))
                .append(nybblChar(right, upperCase));
    }

    public static StringBuilder appendPaddedHex(short val, StringBuilder sb) {
        return appendPaddedHex(val, false, sb);
    }

    public static StringBuilder appendPaddedHex(short val, boolean upperCase, StringBuilder sb) {
        for (int shift = 12; shift >= 0; shift -= 4) {
            int curr = (int) ((long) NYBBL_MASK & (val >> shift));
            sb.append(nybblChar(curr, upperCase));
        }
        return sb;
    }

    private static final int NYBBL_MASK = 0xF;

    public static StringBuilder appendPaddedHex(int val, StringBuilder sb) {
        return appendPaddedHex(val, false, sb);
    }

    public static char nybblChar(int curr, boolean upperCase) {
        switch (curr) {
            case 0:
                return '0';
            case 1:
                return '1';
            case 2:
                return '2';
            case 3:
                return '3';
            case 4:
                return '4';
            case 5:
                return '5';
            case 6:
                return '6';
            case 7:
                return '7';
            case 8:
                return '8';
            case 9:
                return '9';
            case 10:
                return upperCase ? 'A' : 'a';
            case 11:
                return upperCase ? 'B' : 'b';
            case 12:
                return upperCase ? 'C' : 'c';
            case 13:
                return upperCase ? 'D' : 'd';
            case 14:
                return upperCase ? 'E' : 'e';
            case 15:
                return upperCase ? 'F' : 'f';
            default:
                throw new AssertionError(curr);
        }
    }

    public static StringBuilder appendPaddedHex(int val, boolean upperCase, StringBuilder sb) {
        for (int shift = 28; shift >= 0; shift -= 4) {
            int curr = NYBBL_MASK & (val >> shift);
            sb.append(nybblChar(curr, upperCase));
        }
        return sb;
    }

    public static StringBuilder appendPaddedHex(long val, StringBuilder sb) {
        return appendPaddedHex(val, false, sb);
    }

    public static StringBuilder appendPaddedHex(long val, boolean upperCase, StringBuilder sb) {
        for (int shift = 60; shift >= 0; shift -= 4) {
            int curr = (int) ((long) NYBBL_MASK & (val >> shift));
            sb.append(nybblChar(curr, upperCase));
        }
        return sb;
    }

    public static String toPaddedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            appendPaddedHex(b, sb);
        }
        return sb.toString();
    }

    public static String toPaddedHex(byte[] bytes, String delimiter) {
        StringBuilder sb = new StringBuilder((bytes.length * 2) + (delimiter.length() * (bytes.length - 1)));
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
            throw new IllegalArgumentException("Byte count must be divisible "
                    + "by 8, but is " + bytes.length);
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
            throw new IllegalArgumentException("Byte count must be divisible "
                    + "by 8, but is " + bytes.length);
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
        StringBuilder sb = new StringBuilder(seq.length() + 5);
        escape(seq, seq.length(), escaper, sb);
        return sb.toString();
    }

    public static void escape(CharSequence seq, int len, Escaper escaper, StringBuilder into) {
        char prev = 0;
        for (int i = 0; i < len; i++) {
            char c = seq.charAt(i);
            CharSequence escaped = escaper.escape(c, i, len, prev);
            if (escaped != null) {
                into.append(escaped);
            } else {
                into.append(c);
            }
            prev = c;
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
     * Elide a string to 40 characters, breaking on whitespace if possible,
     * leaving the head and the tail and inserting &#2026; as an elipsis.
     *
     * @param orig The original character sequence
     * @return an elided sequence if the length is greater than 40 characters
     */
    public static CharSequence elide(CharSequence orig) {
        return elide(orig, 40);
    }

    /**
     * Elide a string to the passed number of characters plus the length of an
     * one-character elipsis, breaking on whitespace if possible, leaving the
     * head and the tail and inserting &#2026; as an elipsis.
     *
     * @param orig The original character sequence
     * @param thresholdLength The threshold length
     * @return an elided sequence if the length is greater than the passed
     * length characters
     */
    public static CharSequence elide(CharSequence orig, int thresholdLength) {
        return elide(orig, Checks.greaterThanOne("maxLength", thresholdLength), ELLIPSIS);
    }

    /**
     * Elide a string to the passed number of characters plus the length of an
     * one-character elipsis, breaking on whitespace if possible, leaving the
     * head and the tail and inserting &#2026; as an elipsis.
     *
     * @param orig The original character sequence
     * @param targetLength The target length
     * @param ellipsis The character sequence to insert as ellipsis
     * @return an elided sequence if the length is greater than 40 characters
     */
    public static CharSequence elide(CharSequence orig, int targetLength, CharSequence ellipsis) {
        int len = orig.length();
        if (len <= targetLength) {
            return orig;
        }
        targetLength -= ellipsis.length();
        targetLength = Math.max(targetLength, 3);
        if (len <= 2 + ellipsis.length() + 1 || orig.length() <= targetLength) {
            return orig;
        }
        if (targetLength % 2 != 0) {
            targetLength--;
        }
        if (targetLength <= 3) {
            return new AppendableCharSequence(singleChar(orig.charAt(0)),
                    ellipsis, singleChar(orig.charAt(orig.length() - 1)));
        }
        int halfLength = targetLength / 2;
//        int textMidpoint = len / 2;
        int leftEnd = halfLength;
        int rightStart = len - halfLength;

        // Fudge a little to find a whitespace character to split on
        int leftScanStop = Math.max(leftEnd - (leftEnd / 3), leftEnd - MAX_ELLIPSIS_SKEW);
        int rightScanStop = Math.min(rightStart + ((len - rightStart) / 3), rightStart + MAX_ELLIPSIS_SKEW);
        if (!Character.isWhitespace(orig.charAt(leftEnd))) {
            for (int i = leftEnd - 1; i > leftScanStop; i--) {
                if (Character.isWhitespace(orig.charAt(i))) {
                    rightStart -= (leftEnd - (i));
                    leftEnd = i;
                    break;
                }
            }
        } else {
            while (leftEnd > leftScanStop && Character.isWhitespace(orig.charAt(leftEnd + 1))) {
                leftEnd--;
            }
        }
        if (!Character.isWhitespace(orig.charAt(rightStart))) {
            for (int i = rightStart + 1; i < rightScanStop; i++) {
                if (Character.isWhitespace(orig.charAt(i))) {
                    rightStart = i + 1;
                    break;
                }
            }
        } else {
            while (rightStart < orig.length() - 2 && Character.isWhitespace(orig.charAt(rightStart))) {
                rightStart++;
            }
        }
        CharSequence left = orig.subSequence(0, leftEnd);
        CharSequence right = orig.subSequence(rightStart, len);
        return new AppendableCharSequence(left, ellipsis, right);
    }

    /**
     * Truncate a character sequence, appending an ellipsis (\u2026); the length
     * may be adjusted to not place the ellipsis immediately following a
     * whitespace character.
     *
     * @param orig The original character sequence
     * @param maxLength The maximum length of the result (less the length of the
     * ellipsis text if present)
     * @return A truncated version of the original string which may append the
     * ellipsis
     * @throws InvalidArgumentException if maxLength is less than or equal to 1
     */
    public static CharSequence truncate(CharSequence orig, int maxLength) {
        return truncate(orig, maxLength, ELLIPSIS);
    }

    /**
     * Truncate a character sequence, optionally appending an ellipsis; the
     * length may be adjusted to not place the ellipsis immediately following a
     * whitespace character.
     *
     * @param orig The original character sequence
     * @param maxLength The maximum length of the result (less the length of the
     * ellipsis text if present)
     * @param ellipsis The text to append to the truncated result
     * @return A truncated version of the original string which may append the
     * ellipsis
     * @throws InvalidArgumentException if maxLength is less than or equal to 1
     */
    public static CharSequence truncate(CharSequence orig, int maxLength, CharSequence ellipsis) {
        int len = orig.length();
        if (len <= Checks.greaterThanZero("maxLength", maxLength)) {
            return orig;
        }
        int end = Math.min(maxLength, len);
        int maxFudge = Math.max(2, Math.max(end - (maxLength / 3), end - MAX_ELLIPSIS_SKEW));
        while (end > maxFudge && Character.isWhitespace(orig.charAt(end - 1))) {
            end--;
        }
        if (end >= len - 1 || !Character.isWhitespace(orig.charAt(end + 1))) {
            for (int i = Math.min(len - 1, end); i >= maxFudge; i--) {
                if (Character.isWhitespace(orig.charAt(i))) {
                    end = i;
                    break;
                }
            }
        }
        if (ellipsis == null || ellipsis.length() == 0) {
            return orig.subSequence(0, end);
        }
        return new AppendableCharSequence(orig.subSequence(0, end), ellipsis);
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

    /**
     * Wrap a supplier of a string in an object whose toString() method invokes
     * the supplier - useful for some forms of logging where toString() is
     * expensive.
     *
     * @param s A supplier of a string value
     * @return An object
     */
    public static Object wrappedSupplier(Supplier<String> s) {
        return new LazySupplierToString(s);
    }

    /**
     * Create a CharSequence which delegates to a string supplier for its
     * contents, for cases where a CharSequence is required but it is desirable
     * to defer computation of the contents until they are required.
     *
     * @param s A supplier of a string
     * @return A character sequence
     * @since 2.6.13
     */
    public static CharSequence lazyCharSequence(Supplier<String> s) {
        return new LazyCharSequence(s, true);
    }

    /**
     * Create a CharSequence which delegates to a string supplier for its
     * contents, for cases where a CharSequence is required but it is desirable
     * to defer computation of the contents until they are required.
     *
     * @param s A supplier of a string
     * @param cache If the value, once computed, will not change, pass true for
     * this so it is not recomputed on every character request
     * @return A character sequence
     * @since 2.6.13
     */
    public static CharSequence lazyCharSequence(Supplier<String> s, boolean cache) {
        return new LazyCharSequence(s, cache);
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
        char[] chars = toCharArray(orig);
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    private static final char[] EMPTY_CHARS = new char[0];

    /**
     * Convert a CharSequence to an array (uses String.toCharArray() if the
     * argument is a String).
     *
     * @param seq A character sequence
     * @return A character array
     */
    public static char[] toCharArray(CharSequence seq) {
        int len = seq.length();
        switch (len) {
            case 0:
                return EMPTY_CHARS;
            case 1:
                return new char[]{seq.charAt(0)};
            case 2:
                return new char[]{seq.charAt(0), seq.charAt(1)};
            case 3:
                return new char[]{seq.charAt(0), seq.charAt(1), seq.charAt(2)};
            case 4:
                return new char[]{seq.charAt(0), seq.charAt(1), seq.charAt(2), seq.charAt(3)};
            case 5:
                return new char[]{seq.charAt(0), seq.charAt(1), seq.charAt(2), seq.charAt(3), seq.charAt(4)};
            default:
                char[] result = new char[len];
                for (int i = 0; i < len; i++) {
                    result[i] = seq.charAt(i);
                }
                return result;
        }
    }

    /**
     * Slightly faster than Character.isDigit() for ascii.
     *
     * @param ch A character
     * @return True if it is a digit
     */
    public static boolean isDigit(char ch) {
        // uses slightly faster test for ascii digits, and falls
        // back to Character.isDigit
        switch (ch) {
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
                return true;
            default:
                // Weed out the rest of ascii
                if (ch < 48) {
                    return false;
                } else if (ch > 57 && ch < 128) {
                    return false;
                }
        }
        return Character.isDigit(ch);
    }

    // Slightly more efficient to cache these
    private static final CharPred IS_DIGIT = Strings::isDigit;
    private static final CharPred IS_LETTERS = Character::isLetter;

    /**
     * Determine if a string is entirely composed of digits efficiently.
     *
     * @param seq A sequence
     * @return True if it is all digits
     */
    public static boolean isDigits(CharSequence seq) {
        return is(seq, IS_DIGIT);
    }

    /**
     * Determine if a string is entirely composed of digits efficiently.
     *
     * @param seq A sequence
     * @return True if it is all digits
     */
    public static boolean isLetters(CharSequence seq) {
        return is(seq, IS_LETTERS);
    }

    /**
     * Determine very efficiently if a string matches the regex pattern
     * \d+\.\d+.
     *
     * @param seq A character sequence
     * @return true if the pattern is matched
     */
    public static boolean isPositiveDecimal(CharSequence seq) {
        PositiveDecimalCheck pdc = new PositiveDecimalCheck();
        return is(seq, pdc) && pdc.dotCount == 1
                // do not match, e.g. '100.'
                && seq.charAt(seq.length() - 1) != '.';

    }

    static class PositiveDecimalCheck implements CharPred {

        private int dotCount;

        @Override
        public boolean test(char ch) {
            switch (ch) {
                case '.':
                    dotCount++;
                    return true;
                default:
                    return isDigit(ch);
            }
        }

    }

    /**
     * Test a string, testing both ends of the string alternately to produce
     * faster negative results and use fewer iterations.
     *
     * @param in A character sequence
     * @param pred A predicate
     * @return true if all of the characters match
     */
    public static boolean is(CharSequence in, CharPred pred) {
        int max = in.length();
        switch (max) {
            case 0:
                return false;
            case 1:
                return pred.test(in.charAt(0));
            case 2:
                return pred.test(in.charAt(0))
                        && pred.test(in.charAt(1));
            case 3:
                return pred.test(in.charAt(1))
                        && pred.test(in.charAt(0))
                        && pred.test(in.charAt(2));
            case 4:
                return pred.test(in.charAt(1))
                        && pred.test(in.charAt(3))
                        && pred.test(in.charAt(0))
                        && pred.test(in.charAt(2));
            case 5:
                return pred.test(in.charAt(0))
                        && pred.test(in.charAt(4))
                        && pred.test(in.charAt(3))
                        && pred.test(in.charAt(1))
                        && pred.test(in.charAt(2));
            default:
                boolean odd = (max % 2) != 0;
                int mid = max / 2;
                if (odd) {
                    if (pred.test(in.charAt(mid))) {
                        for (int i = 0; i < mid; i++) {
                            if (!pred.test(in.charAt(i))) {
                                return false;
                            } else if (!pred.test(in.charAt(max - (i + 1)))) {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    for (int i = 0; i < mid; i++) {
                        if (!pred.test(in.charAt(i))) {
                            return false;
                        } else if (!pred.test(in.charAt(max - (i + 1)))) {
                            return false;
                        }
                    }
                }
                return true;

        }
    }

    /**
     * Interface which is passed characters and positions while testing the
     * characters in a a string.
     *
     * @see Strings.biIterate(CharSequence, BiIterationReceiver)
     * @see BiIterateResult
     */
    @FunctionalInterface
    public interface BiIterationReceiver {

        BiIterateResult onChar(int index, char c, int of, int remaining);
    }

    /**
     * Iterate the characters in a char sequence in minimal steps, alternating
     * between head and tail positions (if the sequence has an odd number of
     * characters, tests the innermost character first). This is useful for
     * efficientlly testing strings for matches where the common case is a
     * non-match, by providing fewer steps to disconfirming a match when
     * iterating the string sequentially might test many characters before
     * determining that no match is present.
     *
     * @param in The string to examine
     * @param func A test function, which returns YES, NO or MAYBE (continue
     * iterating).
     * @return A result
     */
    public static BiIterateResult biIterate(CharSequence in, BiIterationReceiver func) {
        int max = notNull("in", in).length();
        switch (max) {
            case 0:
                return BiIterateResult.MAYBE;
            case 1:
                return func.onChar(0, in.charAt(0), 1, 0);
            case 2:
                return func.onChar(0, in.charAt(0), 2, 1).or(func.onChar(1, in.charAt(1), 2, 0));
            case 3:
                return func.onChar(1, in.charAt(1), 3, 2)
                        .or(func.onChar(0, in.charAt(0), 3, 1)
                                .or(func.onChar(2, in.charAt(2), 3, 0)));
            default:
                boolean odd = (max % 2) != 0;
                int mid = max / 2;
                int remaining = max;
                if (odd) {
                    BiIterateResult res;
                    if ((res = func.onChar(mid, in.charAt(mid), max, --remaining)).isOk()) {
                        for (int i = 0; i < mid; i++) {
                            res = func.onChar(i, in.charAt(i), max, --remaining);
                            switch (res) {
                                case MAYBE:
                                    break;
                                default:
                                    return res;
                            }
                            int op = max - (i + 1);
                            res = func.onChar(op, in.charAt(op), max, --remaining);
                            switch (res) {
                                case MAYBE:
                                    break;
                                default:
                                    return res;
                            }
                        }
                    } else {
                        return res;
                    }
                } else {
                    for (int i = 0; i < mid; i++) {
                        BiIterateResult res = func.onChar(i, in.charAt(i), max, --remaining);
                        switch (res) {
                            case MAYBE:
                                break;
                            default:
                                return res;
                        }
                        int op = max - (i + 1);
                        res = func.onChar(op, in.charAt(op), max, --remaining);
                        switch (res) {
                            case MAYBE:
                                break;
                            default:
                                return res;
                        }
                    }
                }
                return BiIterateResult.MAYBE;

        }
    }

    public static enum BiIterateResult {
        YES,
        MAYBE,
        NO;

        public BiIterateResult or(BiIterateResult res) {
            if (res == this) {
                return this;
            } else if (res == NO || this == NO) {
                return NO;
            } else {
                return MAYBE;
            }
        }

        public boolean isOk() {
            return this == YES || this == MAYBE;
        }

        public boolean isSuccess() {
            return this == YES;
        }

        public boolean isFinished() {
            return this == YES || this == null;
        }
    }

    /**
     * Create a pattern matcher which uses <code>toString()</code> on the passed
     * enum type for substrings to look for in the input to the returned
     * function, and returns the enum constant for the first one that matches.
     *
     * @param <T> The type
     * @param enumType An enum whose toString() return values are patterns to
     * look for
     * @return A function
     */
    public static <T extends Enum<T>> Function<CharSequence, T> literalMatcher(Class<T> enumType) {
        return MultiLiteralPattern.forEnums(enumType);
    }

    /**
     * Create a pattern matcher which efficiently matches the first occuring
     * value from the passed map, returning the associated key. The returned
     * function is safe to reuse or use across multiple threads.
     *
     * @param <T>
     * @param m
     * @return
     */
    public static <T extends Enum<T>> Function<CharSequence, T> literalMatcher(Map<T, CharSequence> m) {
        return MultiLiteralPattern.forEnums(m);
    }

    /**
     * Create a pattern matcher which efficiently matches the first occuring
     * value from the passed map, returning the index of that value in the
     * passed array. The returned function is safe to reuse or use across
     * multiple threads.
     *
     * @param patterns An array of patterns
     * @return
     */
    public static Function<CharSequence, Integer> literalMatcher(CharSequence... patterns) {
        return MultiLiteralPattern.forStrings(patterns);
    }

    /**
     * Find the first (by position, then map-iteration-order) exact match in the
     * passed map's values, in the input string, returning the key. If you need
     * to match the same patterns repeatedly, it is more efficient to call
     * literalPatternMatcher() and reuse the result.
     *
     * @param <T> A map where the values are literal strings to match
     * @param map A map whose values are literal strings to look for in the
     * input text
     * @param input The input text
     * @return An enum constant or null
     */
    public static <T extends Enum<T>> T findMatch(Map<T, CharSequence> map, CharSequence input) {
        return literalMatcher(map).apply(input);
    }

    /**
     * Find the first (by position in input string, then iteration order of the
     * passed patterns) pattern that is contained in the passed input string,
     * returning its index.
     *
     * @param input An input string
     * @param literalPatterns A list of literal strings the input may contain
     * @return The index of the matched literal or null
     */
    public static Integer findMatch(CharSequence input, CharSequence... literalPatterns) {
        return literalMatcher(literalPatterns).apply(input);
    }

    /**
     * Find the first (by position, then enum-constant-iteration-order) exact
     * textual match of the string value of an enum constant on the passed type.
     *
     * @param <T> The type
     * @param type An enum type whose toString() method returns a string literal
     * to look for
     * @param input An input string
     * @return An enum constant or null
     */
    public static <T extends Enum<T>> T findMatch(Class<T> type, CharSequence input) {
        return literalMatcher(type).apply(input);
    }

    /**
     * Simple variable substitution - given a target string, find occurrences of
     * <code>$PREFIX some text $SUFFIX</code> and replace them with whatever is
     * returned for the intervening contents from the passed function, if
     * anything.
     *
     * @param target The string to modify
     * @param prefix The prefix, non empty
     * @param suffix The suffix, non-empty
     * @param replacements A function to supply replacements
     * @return A modified string, or the original
     */
    public static String variableSubstitution(String target, String prefix, String suffix,
            Function<String, Optional<CharSequence>> replacements) {
        if (target.contains(prefix)) {
            StringBuilder result = null;
            int ix = target.indexOf("${");
            int lastEnd = 0;
            while (ix >= 0 && ix < target.length() - prefix.length()) {
                int endIx = target.indexOf(suffix, ix + prefix.length());
                if (endIx < ix) {
                    break;
                }
                String toResolve = target.substring(ix + prefix.length(), endIx);
                Optional<CharSequence> maybeResolved = replacements.apply(toResolve);
                if (maybeResolved.isPresent()) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(target.substring(lastEnd, ix));
                    result.append(maybeResolved.get());
                }
                lastEnd = endIx + 1;
                ix = target.indexOf(prefix, endIx);
            }
            if (result != null) {
                if (lastEnd < target.length() - 1) {
                    result.append(target.substring(lastEnd));
                }
                return result.toString();
            }
        }
        return target;

    }

    /**
     * A simple character predicate for use with Strings.is() for fast testing
     * of string contents.
     */
    public interface CharPred {

        /**
         * Test the character.
         *
         * @param ch The character
         * @return true if the test passes
         */
        boolean test(char ch);

        default CharPred or(CharPred other) {
            return ch -> test(ch) || other.test(ch);
        }

        default CharPred and(CharPred other) {
            return ch -> test(ch) && other.test(ch);
        }
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
