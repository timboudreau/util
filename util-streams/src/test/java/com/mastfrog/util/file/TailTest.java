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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
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

    @Test(timeout = 40000)
    public void testSomeMethod() throws IOException, InterruptedException {
        ScheduledExecutorService exe = Executors.newScheduledThreadPool(1);
        WatchManager watchManager = new WatchManager(exe, 20, 20, 100);
        try {
            Tail tail = new Tail(watchManager, tmp, 256, UTF_8);
            C c = new C();
            Runnable canceller = tail.watch(c);
            try (PrintStream ps = new PrintStream(tmp.toFile(), "UTF-8")) {
                ps.println("Hello");
                ps.flush();
                Thread.sleep(30);
                ps.println("Goodbye.");
                ps.flush();
                Thread.sleep(30);
                ps.println("Whatevs.");
                ps.flush();
                Thread.sleep(30);
                ps.println("Woo hoo.");
                ps.flush();
                Thread.sleep(30);
                ps.println("Gwerp.");
                ps.flush();
            }
            Thread.sleep(12100);
            canceller.run();
            exe.shutdown();
            c.assertSeen("Hello", "Goodbye.", "Whatevs.", "Woo hoo.", "Gwerp.");
        } finally {
            watchManager.shutdown();
            exe.shutdownNow();
        }
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
        Path dir = Paths.get("/tmp");
        Path file = dir.resolve("tailtest-" + System.currentTimeMillis() + "_"
                + ThreadLocalRandom.current().nextInt());
        tmp = Files.createFile(file);
//        tmp = FileUtils.newTempFile("tail", PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE);
        assertTrue("Temp file " + tmp + " was not actually created", Files.exists(tmp));
    }

    @After
    public void teardown() throws Exception {
        Files.deleteIfExists(tmp);
    }
}
