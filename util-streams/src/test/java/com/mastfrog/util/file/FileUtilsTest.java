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
package com.mastfrog.util.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class FileUtilsTest {

    private static final Random rnd = new Random(132994);
    private static final String NON_ASCII = "ॠॻβॺझ δεζηгЮославиярубльшоколадčďéháčkem末ㄵㄾㄿㅄ";
    private static final char[] MOSTLY_NON_ASCII = ("Q" + NON_ASCII).toCharArray();
    static String nonAsciiMultiline = randomNonAscii(23) + "\n" + randomNonAscii(65)
            + "\n" + "\n  " + randomNonAscii(14) + "\n";

    static Path asciiFile;
    static Path utf16asciiFile;
    static final String TEST_CONTENT = "Hello"
            + "\nThis is a file"
            + "\n"
            + "\nWhich shall be tested and have"
            + "\n   multiple lines."
            + "\n"
            + "\nThat's just how it is.\n";
    private static Path utf8nonAsciiFile;
    private static Path utf16nonAsciiFile;

    @Test
    public void testDecodeSingleBuffer() throws IOException {
        CharBuffer buf;
        try (FileChannel channel = FileChannel.open(asciiFile, StandardOpenOption.READ)) {
            buf = CharBuffer.allocate((int) channel.size() + 20);
            ByteBuffer readBuffer = ByteBuffer.allocate((int) channel.size() + 20);
            FileUtils.decode(channel, readBuffer, buf, US_ASCII.newDecoder(), true);
        }
        assertEquals(TEST_CONTENT, buf.toString());
    }

    @Test(timeout = 3000)
    public void testDecodeWithMismatchedBufferSizes() throws IOException {
        List<CharBuffer> buffers = new ArrayList<>();
        ByteBuffer readBuffer = ByteBuffer.allocate(30);
        int length = 0;
        CharBuffer buf = CharBuffer.allocate(15);
        try (FileChannel channel = FileChannel.open(asciiFile, StandardOpenOption.READ)) {
//            while (channel.position() < channel.size()) {
            while (true) {
                int count = FileUtils.decode(channel, readBuffer, buf, US_ASCII.newDecoder(), true);
                boolean full = readBuffer.position() == readBuffer.limit();
                if (count <= 0) {
                    break;
                }
                if (buf.limit() > 0) {
                    CharBuffer b = CharBuffer.allocate(buf.limit());
                    b.put(buf);
                    b.flip();
                    buffers.add(b);
                    buf.clear();
                } else {
                    break;
                }
                if (full) {
                    readBuffer.rewind();
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (CharBuffer b : buffers) {
            sb.append(b.toString());
        }
        assertEquals(TEST_CONTENT, sb.toString());
    }

    @Test
    public void testDecodeMultiBuffer() throws IOException {
        List<CharBuffer> buffers = new ArrayList<>();
        ByteBuffer readBuffer = ByteBuffer.allocate(15);
        try (FileChannel channel = FileChannel.open(asciiFile, StandardOpenOption.READ)) {
            while (channel.position() < channel.size()) {
                CharBuffer buf = CharBuffer.allocate(10);
                int count = FileUtils.decode(channel, readBuffer, buf, US_ASCII.newDecoder(), true);
                if (count < 0) {
                    break;
                }
                if (buf.limit() > 0) {
                    buffers.add(buf);
                } else {
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (CharBuffer b : buffers) {
            sb.append(b.toString());
        }
        assertEquals(TEST_CONTENT, sb.toString());
    }

    @Test(timeout = 3000)
    public void testRead() throws IOException {
        CharSequence seq = FileUtils.readAscii(asciiFile);
        assertStringsEquals(TEST_CONTENT, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadAsciiUtf16() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf16asciiFile, 10, UTF_16, true);
        assertStringsEquals(TEST_CONTENT, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadMultiBuffer() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(asciiFile, 10, US_ASCII, true);
        assertStringsEquals(TEST_CONTENT, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadMultiBufferUtf16() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf16asciiFile, 10, UTF_16, true);
        assertStringsEquals(TEST_CONTENT, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadByteOverByte() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(asciiFile, 1, US_ASCII, true);
        assertStringsEquals(TEST_CONTENT, seq.toString());
    }

    @Test(timeout = 3000)
    public void testEmptyFileByteOverByte() throws IOException {
        Path nue = FileUtils.newTempFile("x");
        try {
            CharSequence seq = FileUtils.readCharSequence(nue, 1, US_ASCII, true);
            assertEquals(0, seq.length());
            assertStringsEquals("", seq.toString());
        } finally {
            Files.delete(nue);
        }
    }

    @Test(timeout = 3000)
    public void testEmptyFile() throws IOException {
        Path nue = FileUtils.newTempFile("y");
        try {
            CharSequence seq = FileUtils.readCharSequence(nue, 128, US_ASCII, true);
            assertEquals(0, seq.length());
            assertStringsEquals("", seq.toString());
        } finally {
            Files.delete(nue);
        }
    }

    @Test(timeout = 3000)
    public void testRangeOfFileAndBufferSizes() throws IOException {
        for (int fileSize = 1; fileSize < 51; fileSize++) {
            Path tf = FileUtils.newTempFile("testRangeOfFileAndBufferSizes-" + fileSize);
            try {
                String randomString = randomString(fileSize);
                assertEquals(fileSize, randomString.length());
                FileUtils.writeFile(tf, randomString, UTF_8, fileSize, false);
                assertEquals(randomString, Files.readAllLines(tf, UTF_8).iterator().next());
                for (int bufferSize = 1; bufferSize <= fileSize + 10; bufferSize++) {
                    CharSequence seq = FileUtils.readCharSequence(tf, false, bufferSize, UTF_8, true);
                    assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " non direct buffer", randomString, seq.toString());
                    seq = FileUtils.readCharSequence(tf, false, bufferSize, UTF_8, true);
                    assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " with direct buffer", randomString, seq.toString());
                    seq = FileUtils.readCharSequence(tf, false, bufferSize, UTF_8, false);
                }
            } finally {
                Files.delete(tf);
            }
        }
    }

    // multi-byte encoding tests
    @Test(timeout = 3000)
    public void testReadNonAsciiUtf8() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf8nonAsciiFile, 1024, UTF_8, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadMultiBufferNonAsciiUtf8() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf8nonAsciiFile, 10, UTF_8, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadNonAsciiUtf16() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf16nonAsciiFile, 1024, UTF_16, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadMultiBufferNonAsciiUtf16() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf16nonAsciiFile, 10, UTF_16, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadByteOverByteNonAsciiUtf8() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf8nonAsciiFile, 1, UTF_8, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testReadByteOverByteNonAsciiUtf16() throws IOException {
        CharSequence seq = FileUtils.readCharSequence(utf16nonAsciiFile, 1, UTF_16, true);
        assertStringsEquals(nonAsciiMultiline, seq.toString());
    }

    @Test(timeout = 3000)
    public void testPWrite() throws Exception {
//        String s = "ζㅄζ末";
//        String s = "ллгεшβॻㄿддβогоㄾаmвčгhлодďяагδεㄿсheиввॠákॻлkηиεáдčηк' ex";
        String s = "оॺॺδㄾॠяшовсиem čокॠуkQㄾсηㄿллkॠ末лоок末ㄿбиаmeбрॠझㄿоॠлη";
        Path a = FileUtils.newTempFile();
        Path b = FileUtils.newTempFile();
        Path c = FileUtils.newTempFile();
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        try {
            Files.write(a, s.getBytes(UTF_8), StandardOpenOption.WRITE);
            FileUtils.writeFile(b, s, UTF_8, 8, false);
            legacyWriteFile(c, s, UTF_8, false);
            assertEquals(Files.size(a), Files.size(b));
            assertEquals(Files.size(a), Files.size(c));
            assertEquals(s, legacyReadFile(a, UTF_8));
            assertEquals(s, legacyReadFile(b, UTF_8));
            assertEquals(s, legacyReadFile(c, UTF_8));
            assertEquals(s, FileUtils.readUTF8String(a));
            assertEquals(s, FileUtils.readUTF8String(b));
            assertEquals(s, FileUtils.readUTF8String(c));
        } finally {
            Files.delete(a);
            Files.delete(b);
            Files.delete(c);
        }
    }

    static String legacyReadFile(Path path, Charset cs) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, cs);
    }

    static void legacyWriteFile(Path path, String content, Charset as, boolean append) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path.toFile(), append))) {
            os.write(content.toString().getBytes(as));
        }
    }

    @Test
    public void testRangeOfFileAndBufferSizesNonAsciiUTF8() throws IOException {
        for (Charset cs : new Charset[]{UTF_8}) {
            for (int fileSize = 51; fileSize >= 1; fileSize--) {
                Path tf = FileUtils.newTempFile("testRangeOfFileAndBufferSizes-" + fileSize);
                try {
                    String randomString = randomNonAscii(fileSize);
                    assertEquals(fileSize, randomString.length());
                    FileUtils.writeFile(tf, randomString, cs, fileSize, false);
                    assertEquals("Should not have newlines", 1, Files.readAllLines(tf, cs).size());
                    assertEquals("Write failed for '" + randomString + "' with file size " + fileSize, randomString, legacyReadFile(tf, cs));
                    for (int bufferSize = fileSize + 9; bufferSize >= 1; bufferSize--) {
                        CharSequence seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, true);
                        assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " non direct buffer and " + cs.name(), randomString, seq.toString());
                        seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, true);
                        assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " with direct buffer and " + cs.name(), randomString, seq.toString());
                        seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, false);
                    }
                } finally {
                    Files.delete(tf);
                }
            }
        }
    }

    @Test
    public void testRangeOfFileAndBufferSizesNonAsciiUTF16() throws IOException {
        for (Charset cs : new Charset[]{UTF_16}) {
            for (int fileSize = 51; fileSize >= 1; fileSize--) {
                Path tf = FileUtils.newTempFile("testRangeOfFileAndBufferSizes-" + fileSize);
                try {
                    String randomString = randomNonAscii(fileSize);
                    assertEquals(fileSize, randomString.length());
                    FileUtils.writeFile(tf, randomString, cs, fileSize, false);
                    assertEquals(randomString, Files.readAllLines(tf, cs).iterator().next());
                    for (int bufferSize = fileSize + 9; bufferSize >= 1; bufferSize--) {
                        CharSequence seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, true);
                        assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " non direct buffer and " + cs.name(), randomString, seq.toString());
                        seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, true);
                        assertEquals("Mismatch when file size " + fileSize + " and buffer size " + bufferSize + " with direct buffer and " + cs.name(), randomString, seq.toString());
                        seq = FileUtils.readCharSequence(tf, false, bufferSize, cs, false);
                    }
                } finally {
                    Files.delete(tf);
                }
            }
        }
    }

    static final String randomNonAscii(int length) {
        char[] c = new char[length];
        for (int i = 0; i < c.length; i++) {
            c[i] = MOSTLY_NON_ASCII[rnd.nextInt(MOSTLY_NON_ASCII.length)];
        }
        return new String(c);
    }

    static final String randomString(int length) {
        char[] c = new char[length];
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) ('A' + rnd.nextInt(26));
        }
        return new String(c);
    }

    static void assertStringsEquals(String expect, String got) {
        assertEquals(escape(expect), escape(got));
    }

    static String escape(CharSequence seq) {
        int len = seq.length();
        StringBuilder sb = new StringBuilder(seq.length() + 20);
        for (int i = 0; i < len; i++) {
            char c = seq.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    @Test
    public void testSplit() throws IOException {
        Set<String> expect = setOf("/tmp/foo", "/usr/bin/bar", "/home/tim/work", "/dev/dogwhiskers");
        assertEquals(expect, FileUtils.splitUniqueNoEmpty(':', "/tmp/foo:/usr/bin/bar:/home/tim/work:/dev/dogwhiskers"));
        assertEquals(expect, FileUtils.splitUniqueNoEmpty(':', " /tmp/foo:/usr/bin/bar : /home/tim/work :/dev/dogwhiskers"));
        assertEquals(expect, FileUtils.splitUniqueNoEmpty(':', ":/tmp/foo:/usr/bin/bar:/home/tim/work:/dev/dogwhiskers"));
        assertEquals(expect, FileUtils.splitUniqueNoEmpty(':', ":/tmp/foo::/usr/bin/bar: : :/home/tim/work:/dev/dogwhiskers:::"));
        assertEquals(setOf("/foo/bar"), FileUtils.splitUniqueNoEmpty(':', "/foo/bar"));
        assertEquals(Collections.emptySet(), FileUtils.splitUniqueNoEmpty(':', ""));
        assertEquals(Collections.emptySet(), FileUtils.splitUniqueNoEmpty(':', null));
    }

    @Test
    public void findJavaCommand() throws IOException {
        Path dir = Paths.get(System.getProperty("java.home")).resolve("bin");
        Path expect = dir.resolve("java");
        assertTrue(Files.exists(expect));
        assertTrue(Files.isExecutable(expect));
        assertEquals(expect, FileUtils.findExecutable("java", true, true, dir.toString()));
        assertNotNull(FileUtils.findExecutable("java", true, true));
    }

    @Test
    public void testFind() throws IOException {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Set<Path> paths = FileUtils.find(javaHome, true, "properties");
        assertFalse(paths.isEmpty());
        assertEquals(legacyFindProperties(javaHome.toFile(), javaHome.toFile(), new HashSet<>(), true), paths);

        Set<Path> paths2 = FileUtils.find(javaHome, false, "properties");
        assertFalse(paths2.isEmpty());
        assertEquals(legacyFindProperties(javaHome.toFile(), javaHome.toFile(), new HashSet<>(), false), paths2);
    }

    static final Set<Path> legacyFindProperties(File root, File f, Set<Path> addTo, boolean relativize) {
        if (f.isDirectory()) {
            for (File f1 : f.listFiles()) {
                legacyFindProperties(root, f1, addTo, relativize);
            }
        } else {
            if (f.getName().endsWith(".properties")) {
                Path rel = relativize ? root.toPath().relativize(f.toPath()) : f.toPath();
                addTo.add(rel);
            }
        }
        return addTo;
    }

    @Test(timeout = 4000)
    public void testLinesAscii() {
        List<String> expected = Arrays.asList(TEST_CONTENT.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(asciiFile, US_ASCII).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf16Ascii() {
        List<String> expected = Arrays.asList(TEST_CONTENT.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf16asciiFile, UTF_16).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf16AsciiOddBufferSize() {
        // If we use a buffer that takes an odd number of bytes, some reads
        // will leave the read ByteBuffer with the first byte of the next
        // character at the tail - here we make sure that that byte is not
        // lost
        List<String> expected = Arrays.asList(TEST_CONTENT.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf16asciiFile, 13, UTF_16).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf8NonAscii() {
        List<String> expected = Arrays.asList(nonAsciiMultiline.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf8nonAsciiFile, UTF_8).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf8NonAsciiOddBufferSize() {
        List<String> expected = Arrays.asList(nonAsciiMultiline.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf8nonAsciiFile, 3, UTF_8).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf16NonAscii() {
        List<String> expected = Arrays.asList(nonAsciiMultiline.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf16nonAsciiFile, UTF_16).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testLinesUtf16NonAsciiOddBufferSize() {
        List<String> expected = Arrays.asList(nonAsciiMultiline.split("\n"));
        List<String> got = new ArrayList<>();
        Stream<String> stream = FileUtils.lines(utf16nonAsciiFile, 27, UTF_16).map(CharSequence::toString);
        stream.forEach(got::add);
        assertEquals(expected, got);
    }

    @Test(timeout = 4000)
    public void testPermissons() throws IOException {
        Path tmp = FileUtils.newTempFile("wookie", PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE);
        try {
            assertTrue(Files.isExecutable(tmp));
            assertFalse(Files.isReadable(tmp));
            assertTrue(Files.isWritable(tmp));
            assertTrue(tmp.getFileName().toString().startsWith("wookie"));
        } finally {
            Files.delete(tmp);
        }
    }

    @Test(timeout = 4000)
    public void testDeleteIfExists() throws IOException {
        Path tmp = FileUtils.newTempFile("wookie", PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE);
        assertTrue(FileUtils.deleteIfExists(tmp));
        assertFalse(FileUtils.deleteIfExists(tmp));
        assertFalse(FileUtils.deleteIfExists(null));
    }


    static Set<String> setOf(String... strings) {
        return new LinkedHashSet<>(Arrays.asList(strings));
    }

    @BeforeClass
    public static void setup() throws Exception {
        asciiFile = FileUtils.newTempFile(FileUtilsTest.class
                .getSimpleName() + "-");
        FileUtils.writeFile(asciiFile, TEST_CONTENT, US_ASCII, 128, true);
        utf16asciiFile = FileUtils.newTempFile(FileUtilsTest.class
                .getSimpleName() + "-");
        FileUtils.writeFile(utf16asciiFile, TEST_CONTENT, UTF_16, 128, true);
        List<String> check = Files.readAllLines(asciiFile, US_ASCII);
        assertEquals(Arrays.asList(TEST_CONTENT.split("\n")), check);
        check = Files.readAllLines(utf16asciiFile, UTF_16);
        assertEquals(Arrays.asList(TEST_CONTENT.split("\n")), check);

        utf8nonAsciiFile = FileUtils.newTempFile("utf8nonAscii");
        FileUtils.writeFile(utf8nonAsciiFile, nonAsciiMultiline, UTF_8, 128, true);
        check = Files.readAllLines(utf8nonAsciiFile, UTF_8);
        assertEquals(Arrays.asList(nonAsciiMultiline.split("\n")), check);

        utf16nonAsciiFile = FileUtils.newTempFile("utf16nonAscii");
        FileUtils.writeFile(utf16nonAsciiFile, nonAsciiMultiline, UTF_16, 128, true);
        check = Files.readAllLines(utf16nonAsciiFile, UTF_16);
        assertEquals(Arrays.asList(nonAsciiMultiline.split("\n")), check);
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (asciiFile != null && Files.exists(asciiFile)) {
            Files.delete(asciiFile);
        }
    }
}
