/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.converters;

import com.mastfrog.graph.ObjectPath;
import java.io.File;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ConvertersTest {

    @Test
    public void testSomeMethod() {
        Converters c = new Converters();
        c.register(Path.class, File.class, new PathToFileConverter());
        c.register(File.class, Path.class, new FileToPathConverter());
        c.register(String.class, Path.class, new StringToPathConverter());
        c.register(File.class, String.class, new FileToStringConverter());
        c.register(String.class, StringBuilder.class, new StringToStringBuilderConverter());
        c.register(String.class, byte[].class, new StringToByteArrayConverter());
        c.register(byte[].class, Thing.class, new ByteArrayToThingConverter());
        c.register(Thing.class, String.class, new ThingToStringConverter());
        c.register(Thing.class, byte[].class, new ThingToBytesConverter());

        Function<? super String, ? extends File> conv = c.converter(String.class, File.class);
        assertNotNull(conv);

        assertEquals(new File("foo/bar/baz"), conv.apply("foo/bar/baz"));

        Function<? super byte[], ? extends File> c2 = c.converter(byte[].class, File.class);
        assertNotNull(c2);

        assertEquals(new File("foo/bar/baz"), c2.apply("foo/bar/baz".getBytes(UTF_8)));

        c.register(byte[].class, File.class, new BytesToFileConverter());
        c.register(String.class, Thing.class, new StringToThingConverter());
//        c.register(byte[].class, String.class, new ByteArrayToStringConverter());

        c2 = c.converter(byte[].class, File.class);
        assertNotNull(c2);

        assertEquals(new File("foo/bar/baz"), c2.apply("foo/bar/baz".getBytes(UTF_8)));

        Function<? super Integer, ? extends File> c3 = c.converter(Integer.class, File.class);
        assertNull(c3);

        String s = c.convert(new File("x/y/z"), String.class);
        assertNotNull(s);
        assertEquals("x/y/z", s);

        s = c.convert(new Goober("a/b/c"), String.class);
        assertNotNull(s);
        assertEquals("a/b/c", s);

        Object o = c.convert(new Object(), String.class);
        assertNull(o);

        o = c.convert("Hello world", Object.class);
        assertNull(o);
    }

    static String path2string(ObjectPath<Class<?>> path) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c : path) {
            if (sb.length() > 0) {
                sb.append(" -> ");
            }
            String nm;
            if (c.isArray()) {
                nm = c.getComponentType().getSimpleName() + "[]";
            } else {
                nm = c.getSimpleName();
            }
            sb.append(nm);
        }
        return sb.toString();
    }

    static final class PathToFileConverter implements Function<Path, File> {

        @Override
        public File apply(Path t) {
            return t.toFile();
        }

        @Override
        public <V> Function<Path, V> andThen(Function<? super File, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static final class FileToPathConverter implements Function<File, Path> {

        @Override
        public Path apply(File t) {
            return t.toPath();
        }

        @Override
        public <V> Function<File, V> andThen(Function<? super Path, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static final class FileToStringConverter implements Function<File, String> {

        @Override
        public String apply(File t) {
            return t.toString();
        }

        @Override
        public <V> Function<File, V> andThen(Function<? super String, ? extends V> after) {
            return new CF<>(this, after);
        }

    }

    static final class StringToPathConverter implements Function<String, Path> {

        @Override
        public Path apply(String t) {
            return Paths.get(t);
        }

        @Override
        public <V> Function<String, V> andThen(Function<? super Path, ? extends V> after) {
            return new CF<>(this, after);
        }

    }

    static class StringToStringBuilderConverter implements Function<String, StringBuilder> {

        @Override
        public StringBuilder apply(String t) {
            return new StringBuilder(t);
        }

        @Override
        public <V> Function<String, V> andThen(Function<? super StringBuilder, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class StringToByteArrayConverter implements Function<String, byte[]> {

        @Override
        public byte[] apply(String t) {
            return t.getBytes(UTF_8);
        }

        @Override
        public <V> Function<String, V> andThen(Function<? super byte[], ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class ByteArrayToStringConverter implements Function<byte[], String> {

        @Override
        public String apply(byte[] t) {
            return new String(t, UTF_8);
        }

        @Override
        public <V> Function<byte[], V> andThen(Function<? super String, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class ByteArrayToThingConverter implements Function<byte[], Thing> {

        @Override
        public Thing apply(byte[] t) {
            return new Thing(t);
        }

        @Override
        public <V> Function<byte[], V> andThen(Function<? super Thing, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class ThingToStringConverter implements Function<Thing, String> {

        @Override
        public String apply(Thing t) {
            return t.toString();
        }

        @Override
        public <V> Function<Thing, V> andThen(Function<? super String, ? extends V> after) {
            return new CF<>(this, after);
        }

    }

    static class ThingToBytesConverter implements Function<Thing, byte[]> {

        @Override
        public byte[] apply(Thing t) {
            return t.bytes;
        }

        @Override
        public <V> Function<Thing, V> andThen(Function<? super byte[], ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class BytesToFileConverter implements Function<byte[], File> {

        @Override
        public File apply(byte[] t) {
            return new File(new String(t, UTF_8));
        }

        @Override
        public <V> Function<byte[], V> andThen(Function<? super File, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class StringToThingConverter implements Function<String, Thing> {

        @Override
        public Thing apply(String t) {
            return new Thing(t.getBytes(UTF_8));
        }

        @Override
        public <V> Function<String, V> andThen(Function<? super Thing, ? extends V> after) {
            return new CF<>(this, after);
        }
    }

    static class Thing {

        private final byte[] bytes;

        public Thing(byte[] bytes) {
            this.bytes = bytes;
        }

        public String toString() {
            return new String(bytes, UTF_8);
        }
    }

    static class Goober extends File {

        public Goober(String pathname) {
            super(pathname);
        }
    }

    private static final class CF<P, F, T> implements Function<P, T> {

        private final Function<? super P, ? extends F> pre;
        private final Function<? super F, ? extends T> post;

        public CF(Function<? super P, ? extends F> pre, Function<? super F, ? extends T> post) {
            this.pre = pre;
            this.post = post;
        }

        @Override
        public T apply(P t) {
            return post.apply(pre.apply(t));
        }

        public String toString() {
            return "(" + pre + "->" + post + ')';
        }
    }
}
