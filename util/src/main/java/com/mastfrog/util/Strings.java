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

import com.mastfrog.util.strings.AppendingCharSequence;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Iterator;
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
 * @deprecated All methods delegate to com.mastfrog.util.strings.Strings
 */
@Deprecated
public final class Strings {

    public static String reverse(String s) {
        return com.mastfrog.util.strings.Strings.reverse(s);
    }

    /**
     * Trim a CharSequence returning a susbsequence.
     *
     * @param seq The string
     * @return
     */
    public static CharSequence trim(CharSequence seq) {
        return com.mastfrog.util.strings.Strings.trim(seq);
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
        return com.mastfrog.util.strings.Strings.trim(strings);
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
        return com.mastfrog.util.strings.Strings.trim(strings);
    }

    /**
     * Take an array of strings, and return an array of trimmed strings,
     * removing any empty strings.
     *
     * @param in An array of strings
     * @return A new array of trimmed strings;
     */
    public static String[] trim(String[] in) {
        return com.mastfrog.util.strings.Strings.trim(in);
    }

    /**
     * Get the sha1 hash of a string in UTF-8 encoding.
     *
     * @param s The string
     * @return The sha-1 hash
     */
    public static String sha1(String s) {
        return com.mastfrog.util.strings.Strings.sha1(s);
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
        return com.mastfrog.util.strings.Strings.toString(collection);
    }

    /**
     * Split a comma-delimited list into an array of trimmed strings
     *
     * @param string The input string
     * @return An array of resulting strings
     */
    public static String[] split(String string) {
        return com.mastfrog.util.strings.Strings.split(string);
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
        return com.mastfrog.util.strings.Strings.toString(collection);
    }

    public static String toString(final Iterator<?> iter) {
        return com.mastfrog.util.strings.Strings.toString(iter);
    }

    /**
     * Converts a Throwable to a string.
     *
     * @param throwable The throwable
     * @return The string
     */
    public static String toString(final Throwable throwable) {
        return com.mastfrog.util.strings.Strings.toString(throwable);
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
        return com.mastfrog.util.strings.Strings.join(parts);
    }

    /**
     * Join / delimited paths, ensuring no doubled slashes
     *
     * @param parts An array of strings
     * @return A string. If a leading slash is desired, the first element must
     * have one
     */
    public static String joinPath(String... parts) {
        return com.mastfrog.util.strings.Strings.joinPath(parts);
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
        return com.mastfrog.util.strings.Strings.charSequenceHashCode(seq);
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
        return com.mastfrog.util.strings.Strings.charSequenceHashCode(seq, ignoreCase);
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
        return com.mastfrog.util.strings.Strings.charSequencesEqual(a, b);
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
        return com.mastfrog.util.strings.Strings.charSequencesEqual(a, b, ignoreCase);
    }

    /**
     * Trim an array of CharSequences at once.
     *
     * @param seqs The strings
     * @return a new array of CharSequences
     */
    public static CharSequence[] trim(CharSequence[] seqs) {
        return com.mastfrog.util.strings.Strings.trim(seqs);
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
        return com.mastfrog.util.strings.Strings.compareCharSequences(a, b, ignoreCase);
    }

    /**
     * Get a comparator that calls compareCharSequences().
     *
     * @param caseInsensitive If true, do case-insensitive comparison.
     * @return A comparator
     */
    public static Comparator<CharSequence> charSequenceComparator(boolean caseInsensitive) {
        return com.mastfrog.util.strings.Strings.charSequenceComparator(caseInsensitive);
    }

    /**
     * Get a comparator that calls compareCharSequences().
     *
     * @return A comparator
     */
    public static Comparator<CharSequence> charSequenceComparator() {
        return com.mastfrog.util.strings.Strings.charSequenceComparator();
    }

    /**
     * Returns an empty char sequence.
     *
     * @return An empty char sequence.
     */
    public static CharSequence emptyCharSequence() {
        return com.mastfrog.util.strings.Strings.emptyCharSequence();
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(char delim, String... parts) {
        return com.mastfrog.util.strings.Strings.join(delim, parts);
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static CharSequence join(char delim, CharSequence... parts) {
        return com.mastfrog.util.strings.Strings.join(delim, parts);
    }

    public static CharSequence join(char delim, Object... parts) {
        return com.mastfrog.util.strings.Strings.join(delim, parts);
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(char delim, Iterable<?> parts) {
        return com.mastfrog.util.strings.Strings.join(delim, parts);
    }

    /**
     * Join strings using the passed delimiter.
     *
     * @param delim A delimiter
     * @param parts The parts
     * @return A string that joins the strings using the delimiter
     */
    public static String join(String delim, Iterable<?> parts) {
        return com.mastfrog.util.strings.Strings.join(delim, parts);
    }

    public static <T> String join(char delim, Iterable<T> parts, Function<T, String> stringConvert) {
        return com.mastfrog.util.strings.Strings.join(delim, parts, stringConvert);
    }

    public static CharSequence singleChar(char c) {
        return com.mastfrog.util.strings.Strings.singleChar(c);
    }

    public static CharSequence[] split(char delim, CharSequence seq) {
        return com.mastfrog.util.strings.Strings.split(delim, seq);
    }

    public static CharSequence[] splitOnce(char c, CharSequence cs) {
        return com.mastfrog.util.strings.Strings.splitOnce(c, cs);
    }

    public static String[] splitOnce(char c, String cs) {
        return com.mastfrog.util.strings.Strings.splitOnce(c, cs);
    }

    public static final CharSequence stripDoubleQuotes(CharSequence cs) {
        return com.mastfrog.util.strings.Strings.stripDoubleQuotes(cs);
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
        return com.mastfrog.util.strings.Strings.urlEncode(str);
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
        return com.mastfrog.util.strings.Strings.urlDecode(str);
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
        return com.mastfrog.util.strings.Strings.split(delim, seq);
    }

    public static List<CharSequence> splitToList(char delimiter, CharSequence seq) {
        return com.mastfrog.util.strings.Strings.splitToList(delimiter, seq);
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
        return com.mastfrog.util.strings.Strings.splitUniqueNoEmpty(delim, seq);
    }

    public static void split(char delim, CharSequence seq, Function<CharSequence, Boolean> proc) {
        com.mastfrog.util.strings.Strings.split(delim, seq, proc);
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
        return com.mastfrog.util.strings.Strings.startsWith(target, start);
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
        return com.mastfrog.util.strings.Strings.startsWithIgnoreCase(target, start);
    }

    /**
     * Determine if two character sequences are the same, ignoring case.
     *
     * @param a The first sequence
     * @param b The second sequence
     * @return Whether or not they are equal, ignoring case
     */
    public static boolean contentEqualsIgnoreCase(CharSequence a, CharSequence b) {
        return com.mastfrog.util.strings.Strings.contentEqualsIgnoreCase(a, b);
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
        return com.mastfrog.util.strings.Strings.charSequenceContains(container, contained, ignoreCase);
    }

    /**
     * Parse an integer from a character sequence without converting to String.
     *
     * @param seq The character sequence
     * @return An integer
     * @throws NumberFormatException for all the usual reasons
     */
    public static int parseInt(CharSequence seq) {
        return com.mastfrog.util.strings.Strings.parseInt(seq);
    }

    /**
     * Parse an long from a character sequence without converting to String.
     *
     * @param seq The character sequence
     * @return An integer
     * @throws NumberFormatException for all the usual reasons
     */
    public static long parseLong(CharSequence seq) {
        return com.mastfrog.util.strings.Strings.parseLong(seq);
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
        return com.mastfrog.util.strings.Strings.indexOf(c, seq);
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
        return com.mastfrog.util.strings.Strings.lastIndexOf(c, seq);
    }

    /**
     * Interleave the characters of two strings.
     *
     * @param a The first string
     * @param b The second string
     * @return A string combining both
     */
    public static String interleave(CharSequence a, CharSequence b) {
        return com.mastfrog.util.strings.Strings.interleave(a, b);
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
        return com.mastfrog.util.strings.Strings.literalReplaceAll(pattern, replacement, in);
    }

    public static String toString(Object o) {
        return com.mastfrog.util.strings.Strings.toString(o);
    }

    /**
     * A faster String.contains() for single characters, case insensitive.
     *
     * @param lookFor The character to look for
     * @param in Look for in this string
     * @return True if the character is present
     */
    public static boolean containsCaseInsensitive(char lookFor, CharSequence in) {
        return com.mastfrog.util.strings.Strings.containsCaseInsensitive(lookFor, in);
    }

    /**
     * A faster String.contains() for single characters, case insensitive.
     *
     * @param lookFor The character to look for
     * @param in Look for in this string
     * @return True if the character is present
     */
    public static boolean contains(char lookFor, CharSequence in) {
        return com.mastfrog.util.strings.Strings.contains(lookFor, in);
    }

    /**
     * Convert a camel-case sequence to hyphenated, e.g. thisThingIsWeird -&gt;
     * this-thing-is-weird.
     *
     * @param s A camel case sequence
     * @return A hyphenated sequence
     */
    public static String camelCaseToDashes(CharSequence s) {
        return com.mastfrog.util.strings.Strings.camelCaseToDashes(s);
    }

    /**
     * Convert hyphenated words to camel case, e.g. this-thing-is-weird -&gt;
     * thisThingIsWeird.
     *
     * @param s A hyphenated sequence
     * @return A camel case string
     */
    public static String dashesToCamelCase(CharSequence s) {
        return com.mastfrog.util.strings.Strings.dashesToCamelCase(s);
    }

    /**
     * Get a SHA-1 hash of a string encoded as Base64.
     *
     * @param s The string
     * @return The base 64 SHA-1 hash of the string
     */
    public static String hash(String s) {
        return com.mastfrog.util.strings.Strings.hash(s);
    }

    /**
     * Get a URL-safe SHA-1 hash of a string encoded as Base64.
     *
     * @param s The string
     * @return The url-safe base 64 SHA-1 hash of the string
     */
    public static String urlHash(String s) {
        return com.mastfrog.util.strings.Strings.urlHash(s);
    }

    public static String hash(String s, String algorithm) throws NoSuchAlgorithmException {
        return com.mastfrog.util.strings.Strings.hash(s, algorithm);
    }

    public static String urlHash(String s, String algorithm) throws NoSuchAlgorithmException {
        return com.mastfrog.util.strings.Strings.urlHash(s, algorithm);
    }

    public static List<String> commaDelimitedToList(String commas, int lengthLimit) {
        return com.mastfrog.util.strings.Strings.commaDelimitedToList(commas, lengthLimit);
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
        return com.mastfrog.util.strings.Strings.shuffleAndExtract(rnd, s, targetLength);
    }

    public static StringBuilder appendPaddedHex(byte val, StringBuilder sb) {
        return com.mastfrog.util.strings.Strings.appendPaddedHex(val, sb);
    }

    public static StringBuilder appendPaddedHex(short val, StringBuilder sb) {
        return com.mastfrog.util.strings.Strings.appendPaddedHex(val, sb);
    }

    public static StringBuilder appendPaddedHex(int val, StringBuilder sb) {
        return com.mastfrog.util.strings.Strings.appendPaddedHex(val, sb);
    }

    public static String toPaddedHex(byte[] bytes) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes);
    }

    public static String toPaddedHex(byte[] bytes, String delimiter) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes, delimiter);
    }

    public static String toPaddedHex(short[] bytes) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes);
    }

    public static String toPaddedHex(short[] bytes, String delimiter) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes, delimiter);
    }

    public static String toPaddedHex(int[] bytes) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes);
    }

    public static String toPaddedHex(int[] bytes, String delimiter) {
        return com.mastfrog.util.strings.Strings.toPaddedHex(bytes, delimiter);
    }

    public static String toBase64(byte[] bytes) {
        return com.mastfrog.util.strings.Strings.toBase64(bytes);
    }

    public static String toNonPaddedBase36(byte[] bytes) {
        return com.mastfrog.util.strings.Strings.toNonPaddedBase36(bytes);
    }

    public static String toDelimitedPaddedBase36(byte[] bytes) {
        return com.mastfrog.util.strings.Strings.toDelimitedPaddedBase36(bytes);
    }

    /**
     * String equality test which is slower than String.equals(), but is
     * constant-time, so useful in cryptography to avoid a
     * <a href="http://codahale.com/a-lesson-in-timing-attacks/">timing
     * attack</a>.
     */
    public static boolean timingSafeEquals(String first, String second) {
        return com.mastfrog.util.strings.Strings.timingSafeEquals(first, second);
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
        return com.mastfrog.util.strings.Strings.timingSafeEquals(first, second);
    }

    public static CharSequence replaceAll(final char c, String replacement, CharSequence in) {
        return com.mastfrog.util.strings.Strings.replaceAll(c, replacement, in);
    }

    public static CharSequence quickJson(Object... args) {
        return com.mastfrog.util.strings.Strings.quickJson(args);
    }

    public static AppendingCharSequence newAppendingCharSequence() {
        return com.mastfrog.util.strings.Strings.newAppendingCharSequence();
    }

    public static AppendingCharSequence newAppendingCharSequence(int components) {
        return com.mastfrog.util.strings.Strings.newAppendingCharSequence(components);
    }

    public static AppendingCharSequence newAppendingCharSequence(CharSequence seqs) {
        return com.mastfrog.util.strings.Strings.newAppendingCharSequence(seqs);
    }
}
