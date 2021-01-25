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

import java.io.IOException;
import java.io.PrintStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TailTest {

    private Path tmp;

    @Test(timeout=5000)
    public void testSomeMethod() throws IOException, InterruptedException {
        ExecutorService exe = Executors.newSingleThreadExecutor();
        Tail tail = new Tail(exe, tmp, 256, UTF_8);
        C c = new C();
        Runnable canceller = tail.watch(c);
        try (PrintStream ps = new PrintStream(tmp.toFile(), "UTF-8")) {
            ps.println("Hello");
            Thread.sleep(10);
            ps.println("Goodbye.");
            Thread.sleep(10);
            ps.println("Whatevs.");
            Thread.sleep(10);
            ps.println("Woo hoo.");
            Thread.sleep(10);
            ps.println("Gwerp.");
        }
        Thread.sleep(2100);
        canceller.run();
        exe.shutdown();
        c.assertSeen("Hello", "Goodbye.", "Whatevs.", "Woo hoo.", "Gwerp.");
    }

    private static final class C implements Predicate<CharSequence> {

        private final List<String> all = Collections.synchronizedList(new LinkedList<>());

        public void assertSeen(String... lines) throws InterruptedException {
            for (int i = 0; i < 20; i++) {
                if (all.size() < lines.length) {
                    Thread.sleep(100);
                }
            }
            assertEquals(lines.length, all.size());
            int line = 0;
            for (Iterator<String> it = all.iterator(); it.hasNext();) {
                String s = it.next();
                it.remove();
                assertEquals(lines[line++], s);
            }
        }

        @Override
        public boolean test(CharSequence t) {
            all.add(t.toString());
            return true;
        }
    }

    @Before
    public void setup() throws IOException {
        tmp = FileUtils.newTempFile("tail", PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);
        assertTrue("Temp file " + tmp + " was not actually created", Files.exists(tmp));
    }

    @After
    public void teardown() throws Exception {
        Files.deleteIfExists(tmp);
    }

}
