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

    public static List<CharSequence> split(char delim, CharSequence seq) {
        Checks.notNull("seq", seq);
        List<CharSequence> seqs = new ArrayList<>(5);
        split(delim, seq, (val) -> {
            seqs.add(val);
            return true;
        });
        return seqs;
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

}
