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
package com.mastfrog.util.streams;

import com.mastfrog.function.throwing.io.IOConsumer;
import com.mastfrog.util.file.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ContinuousLineStreamTest {

    Path file;

    @Test(timeout = 12000)
    public void testSimple() throws Exception {
        StreamHarness.forNewFile(file, sh -> {
            sh.check();
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    String s = "hello-" + i + "-" + j;
                    sh.println(s);
                }
                sh.check();
            }
        }, "Hello world", "This is the second line");
    }

    @Test(timeout = 12000)
    public void testBlankLines() throws Exception {
        StreamHarness.forNewFile(file, sh -> {
            sh.println("Hey");
            sh.println("");
            sh.println("You");
            sh.check();
            sh.println("We");
            sh.println("");
            sh.println("");
            sh.println("They");
            sh.check();
        }, "Hello world\n", "This is the second line", "\n", "I follow a blank line");
    }

    @Test(timeout = 12000)
    public void testBlankLinesPreprint() throws Exception {
        for (int h = 7; h < 38; h++) {
            try {
                StreamHarness.forNewFile(h, file, sh -> {
                    sh.check();
                    for (int i = 0; i < 5; i++) {
                        for (int j = 0; j < 10; j++) {
                            String s = "hello-" + i + "-" + j;
                            sh.println(s);
                        }
                        sh.check();
                    }
                    sh.println("Hey");
                    sh.println("");
                    sh.println("You");
                    sh.check();
                    sh.println("We");
                    sh.println("");
                    sh.println("");
                    sh.println("They");
                    sh.check();
                }, "Hello world\n", "This is the second line", "I follow a blank line");
            } finally {
                Files.delete(file);
                file = FileUtils.newTempFile();
            }
        }
    }

    @Test(timeout = 12000)
    public void testComplex() throws Exception {
        for (int i = 31; i < 128; i++) {
            StreamHarness.copyOf(31, downloadOrFind(), file, StreamHarness::check);
        }
    }

    private static final String DL_URL = "https://mirror1.malwaredomains.com/files/justdomains";
//    private static final String DL_URL = "https://s3.amazonaws.com/lists.disconnect.me/simple_tracking.txt";
    private static final String DL_FILE = "testdl.txt";

    static Path downloadOrFind() throws IOException {
//        Path p = Paths.get("/home/tim/work/personal/notrack/downloaded/disconnect-me-tracking.list");
//        if (Files.exists(p)) {
//            return p;
//        }
        Path file = new File(".").getAbsoluteFile().toPath();
        while (!Files.exists(file.resolve("pom.xml"))) {
            file = file.getParent();
            if (file == null) {
                fail("Could not find project dir from " + file);
            }
        }
        Path dlfile = file.resolve(DL_FILE).toFile().getAbsoluteFile().toPath();
        if (!Files.exists(dlfile)) {
            URL url = new URL(DL_URL);
            try (InputStream in = url.openStream()) {
                try (OutputStream out = Files.newOutputStream(dlfile,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    Streams.copy(in, out);
                }
            }
        }
        return dlfile;
    }

    @Before
    public void setup() throws IOException {
        file = FileUtils.newTempFile();
    }

    @After
    public void teardown() throws IOException {
        if (file != null && Files.exists(file)) {
            Files.delete(file);
        }
    }

    static final class StreamHarness {

        private final Path file;
        private final boolean preread;
        private final String[] preprint;
        private int bufferSize = 512;
        ContinuousLineStream lines;
        PrintStream print;
        List<String> printed;

        StreamHarness(Path file, boolean preread, String... preprint) throws IOException {
            this.file = file;
            this.preread = preread;
            this.preprint = preprint;
            if (preread) {
                printed = new ArrayList<>();
                Files.readAllLines(file).forEach(l -> {
                    printed.add(l);
                });
            } else if (preprint != null && preprint.length > 0) {
                try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    try (PrintStream ps = new PrintStream(out)) {
                        for (int i = 0; i < preprint.length; i++) {
                            for (String sub : preprint[i].split("\n")) {
                                ps.println(sub);
                            }
                        }
                    }
                }
            }
        }

        public StreamHarness bufferSize(int sz) {
            assertNull("Already started", print);
            assertNull("Already started", lines);
            assertTrue(sz > 0);
            bufferSize = sz;
            return this;
        }

        public static void forNewFile(Path file, IOConsumer<StreamHarness> bi, String... preprint) throws IOException {
            new StreamHarness(file, false).run(bi);
        }

        public static void forNewFile(int bufferSize, Path file, IOConsumer<StreamHarness> bi, String... preprint) throws IOException {
            new StreamHarness(file, false).bufferSize(bufferSize).run(bi);
        }

        public static void copyOf(Path orig, Path file, IOConsumer<StreamHarness> bi) throws IOException {
            Files.copy(orig, file, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            new StreamHarness(file, true).run(bi);
        }

        public static void copyOf(int bufSize, Path orig, Path file, IOConsumer<StreamHarness> bi) throws IOException {
            Files.copy(orig, file, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            new StreamHarness(file, true).bufferSize(bufSize).run(bi);
        }

        public static void forExistingFile(Path file, IOConsumer<StreamHarness> bi) throws IOException {
            new StreamHarness(file, true).run(bi);
        }

        void println(String str) {
            assertNotNull(printed);
            assertNotNull(print);
            printed.addAll(Arrays.asList(str.split("\n")));
            print.println(str);
            print.flush();
        }

        void check() throws IOException {
            List<String> found = new ArrayList<>();
            while (lines.hasMoreLines()) {
                String nxt = lines.next().toString();
                found.add(nxt);
            }
            try {
                assertSetsEqual(printed, found);
                assertEquals(printed, found);
            } finally {
                printed.clear();
            }
        }

        void assertSetsEqual(Collection<String> expected, Collection<String> got) {
            if (!Objects.equals(expected, got)) {
                Set<String> absent = new HashSet<>(expected);
                Set<String> surplus = new HashSet<>(got);
                absent.removeAll(got);
                surplus.removeAll(expected);
                if (!absent.isEmpty() && !surplus.isEmpty()) {
                    fail("Buffer size " + bufferSize + ": Missing and unexpected items.  Absent: " + escaped(absent) + "; Unexpected: " + escaped(surplus));
                } else if (!absent.isEmpty()) {
                    fail("Buffer size " + bufferSize + ": Missing items: " + escaped(absent));
                } else if (!surplus.isEmpty()) {
                    fail("Buffer size " + bufferSize + ": Unexpected items: " + escaped(surplus) + " in " + escaped(got, true));
                }
            }
        }

        static <S extends CharSequence> String escaped(Collection<S> c) {
            return escaped(c, false);
        }

        static <S extends CharSequence> String escaped(Collection<S> c, boolean printIndex) {
            StringBuilder sb = new StringBuilder();
            int ix = 0;
            for (Iterator<S> it = c.iterator(); it.hasNext();) {
                if (printIndex) {
                    sb.append(ix++).append('.');
                }
                sb.append('\'');
                CharSequence s = it.next();
                int max = s.length();
                for (int i = 0; i < max; i++) {
                    char ch = s.charAt(i);
                    switch (ch) {
                        case '\n':
                            sb.append("\\n");
                            break;
                        case '\t':
                            sb.append("\\t");
                            break;
                        case 0:
                            sb.append("\\0");
                            break;
                        default:
                            sb.append(ch);
                    }
                }
                if (it.hasNext()) {
                    sb.append("', ");
                } else {
                    sb.append('\'');
                }
            }
            return sb.toString();
        }

        void run(IOConsumer<StreamHarness> tri) throws IOException {
            if (preread) {
                try (ContinuousLineStream lines = ContinuousLineStream.of(file, bufferSize)) {
                    this.lines = lines;
                    tri.accept(this);
                } finally {
                    this.lines = null;
                }
            } else {
                printed = new ArrayList<>(Arrays.asList(preprint));
                try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    try (PrintStream print = new PrintStream(out)) {
                        this.print = print;
                        try (ContinuousLineStream lines = ContinuousLineStream.of(file, bufferSize)) {
                            this.lines = lines;
                            tri.accept(this);
                        }
                    }
                } finally {
                    printed = null;
                    this.print = null;
                    this.lines = null;
                }
            }
        }
    }

    static String escape(CharSequence seq) {
        return StreamHarness.escaped(Arrays.asList(seq));
    }
}
