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

import com.mastfrog.util.streams.HashingInputStream;
import com.mastfrog.util.streams.HashingOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * String utilities
 */
public final class Strings {

    public static <T extends CharSequence> CharSequence trim(CharSequence seq) {
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
        PrintWriter p = new PrintWriter(w);
        throwable.printStackTrace(p);
        p.close();
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

    public static CharSequence[] trim(CharSequence[] seqs) {
        CharSequence[] result = new CharSequence[seqs.length];
        for (int i = 0; i < seqs.length; i++) {
            result[i] = trim(seqs[i]);
        }
        return result;
    }

    public static int compareCharSequences(CharSequence a, CharSequence b, boolean ignoreCase) {
        if (a == b) {
            return 0;
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

    public static Comparator<CharSequence> charSequenceComparator(boolean caseInsensitive) {
        return new CharSequenceComparator(caseInsensitive);
    }

    public static Comparator<CharSequence> charSequenceComparator() {
        return charSequenceComparator(false);
    }

    private static final class CharSequenceComparator implements Comparator<CharSequence> {

        private final boolean caseInsensitive;

        public CharSequenceComparator(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            return compareCharSequences(o1, o2, caseInsensitive);
        }
    }

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

    public static String join(char delim, String... parts) {
        return join(delim, Arrays.asList(parts));
    }

    public static String join(char delim, CharSequence... parts) {
        return join(delim, Arrays.asList(parts));
    }

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
            return ((String) container).contains((String) contained);
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
            char d = ignoreCase ? Character.toLowerCase(contained.charAt(i)) : contained.charAt(offset);
            if (c != d) {
                offset = 0;
            } else {
                offset++;
            }
            if (offset == containedLength) {
                return true;
            }
        }
        return false;
    }

}
