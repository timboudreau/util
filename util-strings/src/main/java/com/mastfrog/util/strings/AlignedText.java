/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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

import com.mastfrog.util.strings.AlignedText.Endable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 *
 * @author Tim Boudreau
 */
public class AlignedText {

    private List<Line> lines = new ArrayList<>(20);

    public AlignedText() {

    }

    public AlignedText(String tabbed) {
        for (String line : tabbed.split("\n")) {
            Line l = new Line(this);
            for (String col : line.split("\t")) {
                l.append(col).end();
            }
            l.end();
        }
    }

    public static String formatTabbed(CharSequence seq) {
        return new AlignedText(seq.toString()).toString();
    }

    AlignedText add(Line line) {
        lines.add(line);
        return this;
    }

    private int columnWidth(int columnIndex) {
        int result = 0;
        for (Line line : lines) {
            result = Math.max(result, line.length(columnIndex));
        }
        return result;
    }

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private boolean isNumbers(int columnIndex) {
        boolean result = true;
        for (Line line : lines) {
            CharSequence sq = line.getColumn(columnIndex);
            String cc = sq.toString().trim();
            if (cc.isEmpty()) {
                continue;
            }
            if (!DIGITS.matcher(cc).matches()) {
                result = false;
                break;
            }
        }
        return result;
    }

    private String pad(int length) {
        char[] result = new char[length];
        Arrays.fill(result, ' ');
        return new String(result);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public String end() {
        return toString();
    }

    public AStringBuilder append(String s) {
        Line l = new Line(this);
        return l.append(s);
    }

    public AStringBuilder append(char s) {
        Line l = new Line(this);
        return l.append(s);
    }

    public static void main(String[] args) {
        String s = new AlignedText().append("GET/HEAD").end().append("/sp").end().append("Do something cool").end().append("23").end().end()
                .append("GET").end().append("/some/long/path/too").end().append("Hey now what is this doing?").end().append("52043").end().end().end();
        ;
        System.out.println(s);

        System.out.println("----------------------\n\n");

        String tabbed = "FOO\t/path/to/foo\t341\tI like this thing I think\t3920202\n"
                + "BAR/BAZ\t/shrimp\t52\tWell, that's short\t39\n";
        System.out.println(new AlignedText(tabbed));
    }

    public final class Line implements Endable<AlignedText> {

        private final List<AStringBuilder> columns = new ArrayList<>(10);
        private final AlignedText txt;
        private boolean added;

        Line(AlignedText txt) {
            this.txt = txt;
        }

        CharSequence getColumn(int i) {
            if (i >= columns.size()) {
                return "";
            }
            return columns.get(i);
        }

        public AStringBuilder append(String s) {
            AStringBuilder b = new AStringBuilder(this);
            b.append(s);
            return b;
        }

        public AStringBuilder append(char s) {
            AStringBuilder b = new AStringBuilder(this);
            b.append(s);
            return b;
        }

        Line add(AStringBuilder b) {
            columns.add(b);
            return this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                AStringBuilder c = columns.get(i);
                int width = columnWidth(i);
                if (isNumbers(i)) {
                    String pad = pad(width - c.length());
                    sb.append(pad).append(c).append("  ");
                } else {
                    String pad = pad((width - c.length()) + 2);
                    sb.append(c);
                    sb.append(pad);
                }
            }
            return sb.toString();
        }

        @Override
        public AlignedText end() {
            if (!added) {
                txt.add(this);
            }
            return txt;
        }

        int length(int column) {
            if (column >= columns.size()) {
                return 0;
            }
            return columns.get(column).length();
        }
    }

    public interface Endable<T> {

        public T end();
    }

    public static final class AStringBuilder implements Appendable, AutoCloseable, CharSequence, Endable<Line> {

        private final StringBuilder sb = new StringBuilder();
        private final Line line;
        private boolean added;

        AStringBuilder(Line line) {
            this.line = line;
        }

        public Line end() {
            if (!added) {
                added = true;
                line.add(this);
            }
            return line;
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public AStringBuilder append(Object obj) {
            sb.append(obj);
            return this;
        }

        public AStringBuilder append(String str) {
            sb.append(str);
            return this;
        }

        public AStringBuilder append(StringBuffer sb) {
            this.sb.append(sb);
            return this;
        }

        public AStringBuilder append(CharSequence s) {
            sb.append(s);
            return this;
        }

        public AStringBuilder append(CharSequence s, int start, int end) {
            sb.append(s, start, end);
            return this;
        }

        public AStringBuilder append(char[] str) {
            sb.append(str);
            return this;
        }

        public AStringBuilder append(char[] str, int offset, int len) {
            sb.append(str, offset, len);
            return this;
        }

        public AStringBuilder append(boolean b) {
            sb.append(b);
            return this;
        }

        public AStringBuilder append(char c) {
            sb.append(c);
            return this;
        }

        public AStringBuilder append(int i) {
            sb.append(i);
            return this;
        }

        public AStringBuilder append(long lng) {
            sb.append(lng);
            return this;
        }

        public AStringBuilder append(float f) {
            sb.append(f);
            return this;
        }

        public AStringBuilder append(double d) {
            sb.append(d);
            return this;
        }

        public AStringBuilder appendCodePoint(int codePoint) {
            sb.appendCodePoint(codePoint);
            return this;
        }

        public AStringBuilder delete(int start, int end) {
            sb.delete(start, end);
            return this;
        }

        public AStringBuilder deleteCharAt(int index) {
            sb.deleteCharAt(index);
            return this;
        }

        public AStringBuilder replace(int start, int end, String str) {
            sb.replace(start, end, str);
            return this;
        }

        public AStringBuilder insert(int index, char[] str, int offset, int len) {
            sb.insert(index, str, offset, len);
            return this;
        }

        public AStringBuilder insert(int offset, Object obj) {
            sb.insert(offset, obj);
            return this;
        }

        public AStringBuilder insert(int offset, String str) {
            sb.insert(offset, str);
            return this;
        }

        public AStringBuilder insert(int offset, char[] str) {
            sb.insert(offset, str);
            return this;
        }

        public AStringBuilder insert(int dstOffset, CharSequence s) {
            sb.insert(dstOffset, s);
            return this;
        }

        public AStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
            sb.insert(dstOffset, s, start, end);
            return this;
        }

        public AStringBuilder insert(int offset, boolean b) {
            sb.insert(offset, b);
            return this;
        }

        public AStringBuilder insert(int offset, char c) {
            sb.insert(offset, c);
            return this;
        }

        public AStringBuilder insert(int offset, int i) {
            sb.insert(offset, i);
            return this;
        }

        public AStringBuilder insert(int offset, long l) {
            sb.insert(offset, l);
            return this;
        }

        public AStringBuilder insert(int offset, float f) {
            sb.insert(offset, f);
            return this;
        }

        public AStringBuilder insert(int offset, double d) {
            sb.insert(offset, d);
            return this;
        }

        public int indexOf(String str) {
            return sb.indexOf(str);
        }

        public int indexOf(String str, int fromIndex) {
            return sb.indexOf(str, fromIndex);
        }

        public int lastIndexOf(String str) {
            return sb.lastIndexOf(str);
        }

        public int lastIndexOf(String str, int fromIndex) {
            return sb.lastIndexOf(str, fromIndex);
        }

        public AStringBuilder reverse() {
            sb.reverse();
            return this;
        }

        public String toString() {
            return sb.toString();
        }

        public int length() {
            return sb.length();
        }

        public int capacity() {
            return sb.capacity();
        }

        public void ensureCapacity(int minimumCapacity) {
            sb.ensureCapacity(minimumCapacity);
        }

        public void trimToSize() {
            sb.trimToSize();
        }

        public void setLength(int newLength) {
            sb.setLength(newLength);
        }

        public char charAt(int index) {
            return sb.charAt(index);
        }

        public int codePointAt(int index) {
            return sb.codePointAt(index);
        }

        public int codePointBefore(int index) {
            return sb.codePointBefore(index);
        }

        public int codePointCount(int beginIndex, int endIndex) {
            return sb.codePointCount(beginIndex, endIndex);
        }

        public int offsetByCodePoints(int index, int codePointOffset) {
            return sb.offsetByCodePoints(index, codePointOffset);
        }

        public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
            sb.getChars(srcBegin, srcEnd, dst, dstBegin);
        }

        public void setCharAt(int index, char ch) {
            sb.setCharAt(index, ch);
        }

        public String substring(int start) {
            return sb.substring(start);
        }

        public CharSequence subSequence(int start, int end) {
            return sb.subSequence(start, end);
        }

        public String substring(int start, int end) {
            return sb.substring(start, end);
        }

        public IntStream chars() {
            return sb.chars();
        }

        public IntStream codePoints() {
            return sb.codePoints();
        }

        public boolean equals(Object o) {
            if (o instanceof CharSequence) {
                return Strings.charSequencesEqual(((CharSequence) o), sb, false);
            }
            return false;
        }

        public int hashCode() {
            return Strings.charSequenceHashCode(sb);
        }
    }
}