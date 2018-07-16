/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.fileformat;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple way to write JSON from primitive values, arrays, maps and lists, for
 * use in annotation processors where depending on a library for doing this is
 * fraught. Date-like objects are serialized to ISO format GMT..
 * <p/>
 * Useful for libraries which only need to <i>write</i> JSON and cannot afford
 * a dependency on a large JSON library - such as in annotation processors.
 *
 * @author Tim Boudreau
 */
public final class SimpleJSON {

    static final DateTimeFormatter ISO_INSTANT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant()
            .toFormatter(Locale.US);

    private static final DecimalFormat FLOATING_POINT = new DecimalFormat("###############0.0#############");
    private static final ZoneId GMT = ZoneId.of("GMT");

    private SimpleJSON() {
    }

    public enum Style {
        MINIFIED,
        COMPACT,
        PRETTY;

        boolean isSpaces() {
            return this != MINIFIED;
        }

        boolean isIndent() {
            return this == PRETTY;
        }

        static final char[] EMPTY = new char[0];
        static final char[] SINGLE_SPACE = new char[]{' '};
        static final char[] COMMA_NO_SPACE = new char[]{','};
        static final char[] COMMA_SPACE = new char[]{',', ' '};

        char[] newLinePad(int depth) {
            switch (this) {
                case MINIFIED:
                    return EMPTY;
                case COMPACT:
                    return SINGLE_SPACE;
                case PRETTY:
                    char[] c = new char[(depth * 2) + 1];
                    Arrays.fill(c, ' ');
                    c[0] = '\n';
                    return c;
                default:
                    throw new AssertionError(this);
            }
        }

        char[] comma() {
            switch (this) {
                case MINIFIED:
                case COMPACT:
                    return COMMA_NO_SPACE;
                case PRETTY:
                    return COMMA_SPACE;
                default:
                    throw new AssertionError(this);
            }
        }

        private static final char[] COLON = new char[]{':'};
        private static final char[] COLON_SPACE = new char[]{':', ' '};
        private static final char[] COLON_SPACES = new char[]{' ', ':', ' '};

        char[] keyValueDelmiter() {
            switch (this) {
                case MINIFIED:
                    return COLON;
                case COMPACT:
                    return COLON_SPACE;
                case PRETTY:
                    return COLON_SPACES;
                default:
                    throw new AssertionError(this);
            }
        }
    }

    public static String stringify(Object o) {
        return stringify(o, Style.MINIFIED);
    }

    public static String stringify(Object o, Style style) {
        return write(o, new StringBuilder(), 0, style).toString();
    }

    private static StringBuilder write(Object o, StringBuilder sb, int depth, Style style) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte) {
            sb.append(Long.toString(((Number) o).longValue()));
        } else if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            sb.append(FLOATING_POINT.format(d));
        } else if (o instanceof Boolean) {
            sb.append(Boolean.toString(((Boolean) o)));
        } else if (o instanceof Character) {
//            String s = new String(new char[]{((Character) o).charValue()});
//            write(s, sb, depth, style);
            delimit('"', sb, () ->{
                sb.append(o);
            });
        } else if (o instanceof char[]) {
            write(new String((char[]) o), sb, depth, style);
        } else if (o instanceof byte[]) {
            String s = Base64.getEncoder().encodeToString((byte[]) o);
            write(s, sb, depth, style);
        } else if (o instanceof CharSequence) {
            delimit('"', sb, () -> {
                escape((CharSequence) o, sb);
            });
        } else if (o instanceof Date) {
            ZonedDateTime ldt = Instant.ofEpochMilli(((Date) o).getTime()).atZone(GMT);
            write(ldt, sb, depth, style);
        } else if (o instanceof OffsetDateTime) {
            delimit('"', sb, () -> {
                sb.append(((OffsetDateTime) o).format(ISO_INSTANT));
            });
        } else if (o instanceof ZonedDateTime) {
            delimit('"', sb, () -> {
                sb.append(((ZonedDateTime) o).format(ISO_INSTANT));
            });
        } else if (o instanceof Instant) {
            stringify(((Instant) o).atZone(GMT));
        } else if (o instanceof Duration) {
            delimit('"', sb, () -> {
                sb.append(o.toString());
            });
        } else if (o instanceof List<?>) {
            writeList((List<?>) o, sb, depth, style);
        } else if (o instanceof Collection<?>) {
            writeList((Collection<?>) o, sb, depth, style);
        } else if (o instanceof Map<?, ?>) {
            writeMap((Map<?, ?>) o, sb, depth, style);
        } else if (o instanceof Enum<?>) {
            write(((Enum<?>)o).name(), sb, depth, style);
        } else if (o.getClass().isArray()) {
            int max = Array.getLength(o);
            List<Object> l = new ArrayList<>(max);
            for (int i = 0; i < max; i++) {
                Object item = Array.get(o, i);
                l.add(item);
            }
            write(l, sb, depth, style);
        } else if (o instanceof Reference<?>) {
            write(((Reference<?>) o).get(), sb, depth, style);
        } else if (o instanceof Optional<?>) {
            Optional<?> opt = (Optional<?>) o;
            if (opt.isPresent()) {
                write(opt.get(), sb, depth, style);
            } else {
                write(null, sb, depth, style);
            }
        } else if (o instanceof AtomicReference<?>) {
            write(((AtomicReference<?>) o).get(), sb, depth, style);
        } else if (o instanceof AtomicInteger) {
            write(((AtomicInteger) o).get(), sb, depth, style);
        } else if (o instanceof AtomicLong) {
            write(((AtomicLong) o).get(), sb, depth, style);
        } else if (o instanceof Class<?>) {
            write(((Class<?>) o).getName(), sb, depth, style);
        } else if (o instanceof Package) {
            write(((Package) o).getName(), sb, depth, style);
        } else if (o instanceof Throwable) {
            Map<String,Object> t = new LinkedHashMap<>();
            t.put("type", o.getClass().getName());
            t.put("message", ((Throwable) o).getMessage());
            write(t, sb, depth, style);
        } else {
            write(mapify(o), sb, depth, style);
        }
        return sb;
    }

    private static Map<String, Object> mapify(Object o) {
        Class<?> type = o.getClass();
        Map<String, Object> result = new LinkedHashMap<>();
        List<Field> fields = new ArrayList<>(Arrays.asList(type.getFields()));
        Collections.sort(fields, (a, b) -> {
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (java.lang.reflect.Field field : fields) {
            int mods = field.getModifiers();
            if ((mods & java.lang.reflect.Modifier.STATIC) == 0 && (mods & java.lang.reflect.Modifier.PUBLIC) == 1) {
                try {
                    result.put(field.getName(), field.get(o));
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(SimpleJSON.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(SimpleJSON.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static void writeList(Iterable<?> list, StringBuilder sb, int depth, Style style) {
        char[] pad = style.newLinePad(depth);
        delimit('[', ']', sb, () -> {
            for (Iterator<?> it = list.iterator(); it.hasNext();) {
                Object o = it.next();
                sb.append(pad);
                write(o, sb, depth + 1, style);
                if (it.hasNext()) {
                    sb.append(style.comma());
                } else {
                    if (style == Style.PRETTY) {
                        sb.append(pad);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void writeMap(Map<?, ?> map, StringBuilder sb, int depth, Style style) {
        char[] pad = style.newLinePad(depth);
        delimit('{', '}', sb, () -> {
            Iterator<Entry<?, ?>> it = (Iterator<Entry<?, ?>>) map.entrySet().iterator();
            while (it.hasNext()) {
                Entry<?, ?> e = it.next();
                sb.append(pad);
                if (!(e.getKey() instanceof CharSequence)) {
                    delimit('"', sb, () -> {
                        write(e.getKey(), sb, depth + 1, style);
                    });
                } else {
                    write(e.getKey(), sb, depth + 1, style);
                }
                sb.append(style.keyValueDelmiter());
                write(e.getValue(), sb, depth + 1, style);
                if (it.hasNext()) {
                    sb.append(style.comma());
                }
            }
        });
    }

    private static void escape(CharSequence orig, StringBuilder into) {
        int max = orig.length();
        for (int i = 0; i < max; i++) {
            char c = orig.charAt(i);
            switch (c) {
                case '\b':
                    into.append('\\').append('b');
                    continue;
                case '\f':
                    into.append('\\').append('f');
                    continue;
                case '\n':
                    into.append('\\').append('n');
                    continue;
                case '\r':
                    into.append('\\').append('r');
                    continue;
                case '\t':
                    into.append('\\').append('t');
                    continue;
                case '"':
                    into.append('\\').append('"');
                    continue;
                case '\\':
                    into.append('\\').append('\\');
                    continue;
            }
            into.append(c);
        }
    }

    private static void delimit(char delimiter, StringBuilder sb, Runnable run) {
        sb.append(delimiter);
        run.run();
        sb.append(delimiter);
    }

    private static void delimit(char delimiterOpen, char delimiterClose, StringBuilder sb, Runnable run) {
        sb.append(delimiterOpen);
        run.run();
        sb.append(delimiterClose);
    }
}
