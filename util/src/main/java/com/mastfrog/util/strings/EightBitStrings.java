/*
 * The MIT License
 *
 * Copyright 2016 Tim Boudreau.
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

import com.mastfrog.util.Strings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * byte[] backed string to cut memory consumption from Strings in half for cases
 * where ASCII strings are guaranteed. Create an instance of EightBitStrings,
 * which holds an intern pool, and then use it to create individual string
 * instances.
 *
 * @author Tim Boudreau
 */
public final class EightBitStrings implements Serializable {

    private final InternTable INTERN_TABLE = new InternTable();
    public final CharSequence DOT = create(".");
    public final CharSequence QUOTE = create("\"");
    public final CharSequence SPACE = create(" ");
    public final CharSequence QUOTE_SPACE = create("\" ");
    public final CharSequence CLOSE_OPEN_QUOTE = create("\" \"");

    private final boolean disabled;
    private final boolean aggressive;
    private final boolean ascii;

    public EightBitStrings(boolean disabled) {
        this(disabled, false, true);
    }

    public EightBitStrings(boolean disabled, boolean aggressive, boolean ascii) {
        this.disabled = disabled;
        this.aggressive = aggressive;
        this.ascii = ascii;
    }

    public Charset charset() {
        return ascii ? US_ASCII : UTF_8;
    }

    public void clear() {
        INTERN_TABLE.dispose();
    }

    public ComparableCharSequence create(CharSequence string) {
        if (disabled) {
            return string instanceof ComparableCharSequence ? (ComparableCharSequence) string
                    : new StringWrapper(string.toString());
        }
        if (aggressive) {
            return concat(string);
        }
        return INTERN_TABLE.intern(string);
    }

    public ComparableCharSequence concat(CharSequence... seqs) {
        if (seqs.length == 1 && seqs[0] instanceof ComparableCharSequence) {
            return (ComparableCharSequence) seqs[0];
        }
        if (disabled) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence c : seqs) {
                sb.append(c);
            }
            return new StringWrapper(sb.toString());
        } else if (aggressive) {
            List<CharSequence> nue = new ArrayList<>(seqs.length + (seqs.length / 2));
            for (CharSequence seq : seqs) {
                if (seq == ComparableCharSequence.EMPTY) {
                    continue;
                }
                int ln = seq.length();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ln; i++) {
                    char c = seq.charAt(i);
                    if (Character.isLetter(c) || Character.isDigit(c)) {
                        sb.append(c);
                    } else {
                        nue.add(sb.toString());
                        sb.setLength(0);
                        nue.add(new String(new char[]{c}));
                    }
                }
                if (sb.length() > 0) {
                    nue.add(sb.toString());
                }
            }
            if (nue.size() != seqs.length) {
                seqs = nue.toArray(new CharSequence[nue.size()]);
            }
        }
        return new Concatenation(INTERN_TABLE, seqs);
    }

    public ComparableCharSequence concatQuoted(Collection<Object> seqs) {
        if (disabled) {
            StringBuilder sb = new StringBuilder("\"");
            boolean first = true;
            for (Iterator<Object> it = seqs.iterator(); it.hasNext();) {
                Object c = it.next();
                if (!first) {
                    sb.append(SPACE);
                }
                if (c instanceof CharSequence) {
                    sb.append(QUOTE);
                }
                sb.append(c);
                if (c instanceof CharSequence) {
                    sb.append(QUOTE);
                }
                first = false;
            }
            return new StringWrapper(sb.toString());
        } else {
            List<CharSequence> quoted = new ArrayList<>((seqs.size() * 3) + 1);
            for (Iterator<Object> it = seqs.iterator(); it.hasNext();) {
                Object c = it.next();
                if (c instanceof CharSequence) {
                    quoted.add(QUOTE);
                    quoted.add((CharSequence) c);
                    if (it.hasNext()) {
                        quoted.add(QUOTE_SPACE);
                    } else {
                        quoted.add(QUOTE);
                    }
                } else {
                    quoted.add(create(c.toString()));
                    quoted.add(SPACE);
                }
            }
            Concatenation result = new Concatenation(INTERN_TABLE,
                    quoted.toArray(new CharSequence[quoted.size()]));
            if (result.entries.length == 1) {
                return result.entries[0];
            }
            return result;
        }
    }

    private byte[] toBytes(CharSequence seq) {
        if (seq instanceof Entry) {
            return ((Entry) seq).bytes;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(seq.toString().getBytes(charset()));
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static CharSequence toCharSequence(Charset charset, byte[] bytes) {
        try {
            return charset.newDecoder().decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    int internTableSize() {
        return INTERN_TABLE.last + 1;
    }

    List<CharSequence> dumpInternTable() {
        return INTERN_TABLE.dumpInternTable();
    }

    class InternTable implements Serializable {

        private static final int SIZE_INCREMENT = 150;

        private int last = -1;
        private Entry[] entries = new Entry[SIZE_INCREMENT];

        void dispose() {
            entries = new Entry[SIZE_INCREMENT];
            last = -1;
        }

        boolean owns(CharSequence seq) {
            if (seq instanceof Entry) {
                return ((Entry) seq).belongsTo(this);
            } else if (seq instanceof Concatenation) {
                return ((Concatenation) seq).belongsTo(this);
            } else {
                return false;
            }
        }

        Entry[] intern(CharSequence... seq) {
            Entry[] result = new Entry[seq.length];
            for (int i = 0; i < seq.length; i++) {
                result[i] = intern(seq[i], false);
            }
            if (seq.length > 0) {
                resort(seq[0]);
            }
            return result;
        }

        Entry intern(CharSequence seq) {
            return intern(seq, true);
        }

        Entry intern(CharSequence seq, boolean sort) {
            if (seq instanceof Entry && ((Entry) seq).belongsTo(this)) {
                return (Entry) seq;
            }
            // We are using an array and binary search to conserve memory
            // here.  This is slower than a HashMap (we sort on insert so
            // we can binary search later), but involves far fewer allocations
            Entry entry = new Entry(toBytes(seq), (short) seq.length(), ascii);
            synchronized (this) {
                int offset = last == -1 ? -1 : Arrays.binarySearch(entries, 0, last + 1, entry);
                if (offset > 0) {
                    return entries[offset];
                }
                if (last == entries.length - 1) {
                    Entry[] nue = new Entry[entries.length + SIZE_INCREMENT];
                    System.arraycopy(entries, 0, nue, 0, entries.length);
                    entries = nue;
                }
                entries[++last] = entry;
                resort(seq);
            }
            return entry;
        }

        private void resort(CharSequence seq) {
            try {
                Arrays.sort(entries, 0, last + 1);
            } catch (IllegalArgumentException e) {
                throw new AssertionError("Broken sorting '" + seq
                        + "' into array for item " + last
                        + ". Full table: " + dumpInternTable(), e);
            }

        }

        List<CharSequence> dumpInternTable() {
            return Arrays.asList(entries);
        }
    }

    private static final class Entry implements ComparableCharSequence, Serializable {

        private final byte[] bytes;
        private final short length;
        int hash = 0;
        private final boolean ascii;

        public Entry(byte[] bytes, short length, boolean ascii) {
            if (length < 0) {
                throw new Error("String too large");
            }
            this.bytes = bytes;
            this.length = length;
            this.ascii = ascii;
        }

        public Charset charset() {
            return ascii ? US_ASCII : UTF_8;
        }

        @Override
        public int hashCode() {
            if (hash != 0) {
                return hash;
            }
            int h = 0;
            if (h == 0 && bytes.length > 0) {
                if (ascii) {
                    int max = bytes.length;
                    for (int i = 0; i < max; i++) {
                        h = 31 * h + ((char) bytes[i]);
                    }
                } else {
                    CharSequence val = toChars();
                    int max = val.length();
                    for (int i = 0; i < max; i++) {
                        h = 31 * h + val.charAt(i);
                    }
                }
            }
            return hash = h;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (o instanceof Entry) {
                Entry other = (Entry) o;
                if (other.bytes.length < bytes.length) {
                    return false;
                }
                // XXX if two strings with different unicode encodings,
                // such as numeric encoding of ascii chars in one,
                // will give the wrong answer
                return Arrays.equals(bytes, other.bytes);
            } else if (o instanceof CharSequence) {
                return charSequencesEqual(this, (CharSequence) o);
            } else {
                return false;
            }
        }

        public int compareChars(Entry o) {
            int max = Math.min(bytes.length, o.bytes.length);
            for (int i = 0; i < max; i++) {
                if (bytes[i] > o.bytes[i]) {
                    return 1;
                } else if (bytes[i] < o.bytes[i]) {
                    return -1;
                }
            }
            return 0;
        }

        public int compare(Entry o) {
            if (o == this) {
                return 0;
            }
            if (!ascii) {
                return Strings.compareCharSequences(this, o, false);
            }
            int result = compareChars(o);
            if (result != 0) {
                return result;
            }
            if (bytes.length == o.bytes.length) {
                return 0;
            } else if (bytes.length > o.bytes.length) {
                return 1;
            } else {
                return -1;
            }
        }

        @Override
        public String toString() {
            return new String(bytes, charset());
        }

        @Override
        public int length() {
            return length;
        }

        CharSequence toChars() {
            return toCharSequence(charset(), bytes);
        }

        @Override
        public char charAt(int index) {
            if (ascii) {
                return (char) bytes[index];
            }
            return toCharSequence(charset(), bytes).charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start == end) {
                return Strings.emptyCharSequence();
            }
            if (start == 0 && end == length) {
                return this;
            }
            if (ascii) {
                byte[] b = new byte[end - start];
                System.arraycopy(bytes, start, b, 0, b.length);
                return new Entry(b, (short) b.length, ascii);
            }
            return toCharSequence(charset(), bytes).subSequence(start, end);
        }

        @Override
        public int compareTo(CharSequence o) {
            if (o instanceof Entry) {
                return compare((Entry) o);
            }
            return compareCharSequences(this, o);
        }

        private boolean belongsTo(InternTable aThis) {
            for (Entry e : aThis.entries) {
                if (e == this) {
                    return true;
                }
            }
            return false;
        }
    }

    static int compareCharSequences(CharSequence a, CharSequence b) {
        if (a == b) {
            return 0;
        }
        int aLength = a.length();
        int bLength = b.length();
        int max = Math.min(aLength, bLength);
        if (a instanceof Entry && b instanceof Entry) {
            Entry ae = (Entry) a;
            Entry be = (Entry) b;
            return ae.compare(be);
        } else {
            for (int i = 0; i < max; i++) {
                if (a.charAt(i) > b.charAt(i)) {
                    return 1;
                } else if (a.charAt(i) < b.charAt(i)) {
                    return -1;
                }
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

    static boolean debug;

    static class Concatenation implements ComparableCharSequence, Comparable<CharSequence>, Serializable {

        private final Entry[] entries;
        private final InternTable table;

        Concatenation(InternTable table, CharSequence... entries) {
            this.table = table;
            List<Entry> l = new ArrayList<>(entries.length);
            for (CharSequence cs : entries) {
                if (cs instanceof Concatenation) {
                    Concatenation c1 = (Concatenation) cs;
                    if (c1.belongsTo(table)) {
                        l.addAll(Arrays.asList(c1.entries));
                    } else {
                        for (Entry e : c1.entries) {
                            l.add(table.intern(e));
                        }
                    }
                } else {
                    l.add(table.intern(cs));
                }
            }
            this.entries = l.toArray(new Entry[l.size()]);
        }

        @Override
        public int length() {
            int result = 0;
            for (int i = 0; i < entries.length; i++) {
                result += entries[i].length;
            }
            return result;
        }

        @Override
        public char charAt(int index) {
            if (entries.length == 0) {
                throw new IndexOutOfBoundsException("0 length but asked for " + index);
            }
            for (int i = 0; i < entries.length; i++) {
                Entry e = entries[i];
                if (index >= e.length) {
                    index -= e.length;
                } else {
                    return e.charAt(index);
                }
            }
            throw new IndexOutOfBoundsException(index + " of "
                    + length() + " in " + this + " with entries "
                    + Arrays.asList(entries));
        }

//        @Override
//        public CharSequence subSequence(int start, int end) {
//            return table.intern(toString().subSequence(start, end));
//        }
        @Override
        public CharSequence subSequence(int start, int end) {
            if (start > end) {
                throw new IllegalArgumentException("Start greater than end " + start + " > " + end);
            }
            if (start == end) {
                return Strings.emptyCharSequence();
            }
            if (start == 0 && end == length()) {
                return this;
            }
            int aggregateLength = 0;
            int max = entries.length;
//        AppendableCharSequence result = new AppendableCharSequence();
            List<CharSequence> result = new ArrayList<>(entries.length);
            for (int i = 0; i < max; i++) {
                if (aggregateLength >= end + 1) {
                    break;
                }
                CharSequence curr = entries[i];
                int len = curr.length();
                int first = Math.max(0, start - aggregateLength);
                int last = Math.min(len, Math.min((end + 1) - start, first + (end - aggregateLength)));

                if (end >= aggregateLength + len) {
                    last = len;
                } else if (end <= aggregateLength + len) {
                    last = end - aggregateLength;
                }
                if (last > first) {
                    CharSequence sub = curr.subSequence(first, last);
                    result.add(sub);
                }
                aggregateLength += len;
            }
            return new Concatenation(table, result.toArray(new CharSequence[result.size()]));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Entry e : entries) {
                sb.append(e);
            }
            if (debug) {
                sb.append(" - ").append(Arrays.asList(entries));
            }
            return sb.toString();
        }

        private int hash = 0;

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0 && length() > 0) {
                for (Entry e : entries) {
                    CharSequence chars = e.toChars();
                    int max = chars.length();
                    for (int i = 0; i < max; i++) {
                        h = 31 * h + chars.charAt(i);
                    }
                }
                hash = h;
            }
            return h;
        }

        public boolean equals(Object o) {
            if (true) {
                if (o instanceof CharSequence) {
                    return charSequencesEqual(this, (CharSequence) o);
                }
            }
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof Concatenation) {
                Concatenation c = (Concatenation) o;
                if (c.entries.length != entries.length) {
                    return charSequencesEqual(this, c);
                }
                for (int i = 0; i < entries.length; i++) {
                    if (entries[i].length() != entries[i].length) {
                        return charSequencesEqual(this, c);
                    }
                    if (!entries[i].equals(c.entries[i])) {
                        return false;
                    }
                }
                return true;
            } else if (o instanceof CharSequence) {
                return charSequencesEqual(this, (CharSequence) o);
            } else {
                return false;
            }
        }

        @Override
        public int compareTo(CharSequence o) {
            if (o instanceof Concatenation) {
                Concatenation other = (Concatenation) o;
                int ec = Math.min(entries.length, other.entries.length);
                if (ec > 0) {
                    int res = entries[0].compareChars(other.entries[0]);
                    if (res != 0) {
                        return res;
                    }
                }
            }
            return compareCharSequences(this, o);
        }

        private boolean belongsTo(InternTable aThis) {
            return table == aThis;
        }
    }

    private static boolean charSequencesEqual(CharSequence a, CharSequence b) {
        if (a instanceof Entry && b instanceof Entry) {
            return Arrays.equals(((Entry) a).bytes, ((Entry) b).bytes);
        }
        if (a instanceof Concatenation && b instanceof Concatenation) {
            Concatenation ca = (Concatenation) a;
            Concatenation cb = (Concatenation) b;
            if (ca.entries.length > 0 && cb.entries.length > 0) {
                if (ca.entries[0].compareChars(cb.entries[0]) != 0) {
                    return false;
                }
            }
        }
        int maxA = a.length();
        int maxB = b.length();
        if (maxA != maxB) {
            return false;
        }
        for (int i = 0; i < maxA; i++) {
            char aa = a.charAt(i);
            char bb = b.charAt(i);
            if (aa != bb) {
                return false;
            }
        }
        return true;
    }

    static class StringWrapper implements ComparableCharSequence, Serializable {

        private final String s;

        public StringWrapper(String s) {
            this.s = s.intern();
        }

        @Override
        public int length() {
            return s.length();
        }

        public boolean isEmpty() {
            return s.isEmpty();
        }

        @Override
        public char charAt(int index) {
            return s.charAt(index);
        }

        public int codePointAt(int index) {
            return s.codePointAt(index);
        }

        public int codePointBefore(int index) {
            return s.codePointBefore(index);
        }

        public int codePointCount(int beginIndex, int endIndex) {
            return s.codePointCount(beginIndex, endIndex);
        }

        public int offsetByCodePoints(int index, int codePointOffset) {
            return s.offsetByCodePoints(index, codePointOffset);
        }

        public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
            s.getChars(srcBegin, srcEnd, dst, dstBegin);
        }

        @SuppressWarnings("deprecation")
        public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
            s.getBytes(srcBegin, srcEnd, dst, dstBegin);
        }

        public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
            return s.getBytes(charsetName);
        }

        public byte[] getBytes(Charset charset) {
            return s.getBytes(charset);
        }

        public byte[] getBytes() {
            return s.getBytes();
        }

        public boolean equals(Object anObject) {
            if (anObject == this) {
                return true;
            }
            if (anObject == null) {
                return false;
            }
            if (!(anObject instanceof CharSequence)) {
                return false;
            }
            if (anObject instanceof String) {
                return s.equals(anObject);
            }
            return s.contentEquals((CharSequence) anObject);
        }

        public boolean contentEquals(StringBuffer sb) {
            return s.contentEquals(sb);
        }

        public boolean contentEquals(CharSequence cs) {
            return s.contentEquals(cs);
        }

        public boolean equalsIgnoreCase(String anotherString) {
            return s.equalsIgnoreCase(anotherString);
        }

        public int compareTo(CharSequence anotherString) {
            return compareCharSequences(this, anotherString);
        }

        public int compareToIgnoreCase(String str) {
            return s.compareToIgnoreCase(str);
        }

        public boolean regionMatches(int toffset, String other, int ooffset, int len) {
            return s.regionMatches(toffset, other, ooffset, len);
        }

        public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
            return s.regionMatches(ignoreCase, toffset, other, ooffset, len);
        }

        public boolean startsWith(String prefix, int toffset) {
            return s.startsWith(prefix, toffset);
        }

        public boolean startsWith(String prefix) {
            return s.startsWith(prefix);
        }

        public boolean endsWith(String suffix) {
            return s.endsWith(suffix);
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        public int indexOf(int ch) {
            return s.indexOf(ch);
        }

        public int indexOf(int ch, int fromIndex) {
            return s.indexOf(ch, fromIndex);
        }

        public int lastIndexOf(int ch) {
            return s.lastIndexOf(ch);
        }

        public int lastIndexOf(int ch, int fromIndex) {
            return s.lastIndexOf(ch, fromIndex);
        }

        public int indexOf(String str) {
            return s.indexOf(str);
        }

        public int indexOf(String str, int fromIndex) {
            return s.indexOf(str, fromIndex);
        }

        public int lastIndexOf(String str) {
            return s.lastIndexOf(str);
        }

        public int lastIndexOf(String str, int fromIndex) {
            return s.lastIndexOf(str, fromIndex);
        }

        public String substring(int beginIndex) {
            return s.substring(beginIndex);
        }

        public String substring(int beginIndex, int endIndex) {
            return s.substring(beginIndex, endIndex);
        }

        public CharSequence subSequence(int beginIndex, int endIndex) {
            return s.subSequence(beginIndex, endIndex);
        }

        public String concat(String str) {
            return s.concat(str);
        }

        public String replace(char oldChar, char newChar) {
            return s.replace(oldChar, newChar);
        }

        public boolean matches(String regex) {
            return s.matches(regex);
        }

        public boolean contains(CharSequence s) {
            return this.s.contains(s);
        }

        public String replaceFirst(String regex, String replacement) {
            return s.replaceFirst(regex, replacement);
        }

        public String replaceAll(String regex, String replacement) {
            return s.replaceAll(regex, replacement);
        }

        public String replace(CharSequence target, CharSequence replacement) {
            return s.replace(target, replacement);
        }

        public String[] split(String regex, int limit) {
            return s.split(regex, limit);
        }

        public String[] split(String regex) {
            return s.split(regex);
        }

        public static String join(CharSequence delimiter, CharSequence... elements) {
            return String.join(delimiter, elements);
        }

        public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
            return String.join(delimiter, elements);
        }

        public String toLowerCase(Locale locale) {
            return s.toLowerCase(locale);
        }

        public String toLowerCase() {
            return s.toLowerCase();
        }

        public String toUpperCase(Locale locale) {
            return s.toUpperCase(locale);
        }

        public String toUpperCase() {
            return s.toUpperCase();
        }

        public String trim() {
            return s.trim();
        }

        public String toString() {
            return s.toString();
        }

        public char[] toCharArray() {
            return s.toCharArray();
        }

        public static String format(String format, Object... args) {
            return String.format(format, args);
        }

        public static String format(Locale l, String format, Object... args) {
            return String.format(l, format, args);
        }

        public static String valueOf(Object obj) {
            return String.valueOf(obj);
        }

        public static String valueOf(char[] data) {
            return String.valueOf(data);
        }

        public static String valueOf(char[] data, int offset, int count) {
            return String.valueOf(data, offset, count);
        }

        public static String copyValueOf(char[] data, int offset, int count) {
            return String.copyValueOf(data, offset, count);
        }

        public static String copyValueOf(char[] data) {
            return String.copyValueOf(data);
        }

        public static String valueOf(boolean b) {
            return String.valueOf(b);
        }

        public static String valueOf(char c) {
            return String.valueOf(c);
        }

        public static String valueOf(int i) {
            return String.valueOf(i);
        }

        public static String valueOf(long l) {
            return String.valueOf(l);
        }

        public static String valueOf(float f) {
            return String.valueOf(f);
        }

        public static String valueOf(double d) {
            return String.valueOf(d);
        }

        public String intern() {
            return s.intern();
        }
    }
}
