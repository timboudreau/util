/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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
package com.mastfrog.bits.large;

import com.mastfrog.function.state.Int;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author Tim Boudreau
 */
public class FileChannelLongArrayTest {

    private static Path DIR;

    @Test
    public void testSomeMethod() throws IOException {
        long[] lngs = new long[2048];
        Random rnd = new Random(682);
        for (int i = 0; i < lngs.length; i++) {
            lngs[i] = rnd.nextLong();
        }
        try (FileChannelLongArray fcl = new FileChannelLongArray(FileChannel.open(DIR.resolve("testSimple"),
                READ, WRITE, TRUNCATE_EXISTING, CREATE))) {

            fcl.addAll(lngs);

            assertEquals(lngs.length, fcl.size());

            assertArrayEquals(lngs, fcl.toLongArray());

            Int ct = Int.create();
            long[] res = new long[lngs.length];
            fcl.forEach(ln -> {
                res[ct.increment()] = ln;
            });
            assertArrayEquals(lngs, res);

            for (int i = 0; i < lngs.length; i++) {
                assertEquals(lngs[i], fcl.get(i));
            }
        }

        Path p2 = DIR.resolve("testSimple2");
        try (FileChannel ch = FileChannel.open(p2, READ, WRITE, TRUNCATE_EXISTING, CREATE)) {
            ByteBuffer buf = ByteBuffer.allocate(lngs.length * Long.BYTES);
            buf.asLongBuffer().put(lngs);
            buf.limit(buf.capacity());
            ch.write(buf, 0);
        }

        try (FileChannelLongArray fcl = new FileChannelLongArray(FileChannel.open(p2, READ))) {
            assertArrayEquals(lngs, fcl.toLongArray());
        }
    }

    @BeforeAll
    public static void temp() throws IOException {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        String proposal = "FileChannelLongArrayTest";
        int ix = 1;
        while (Files.exists(tmp.resolve(proposal))) {
            proposal = "FileChannelLongArrayTest-" + ix++;
        }
        DIR = tmp.resolve(proposal);
        Files.createDirectories(DIR);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        if (DIR != null) {
            try (Stream<Path> str = Files.list(DIR)) {
                str.forEach(pth -> {
                    try {
                        Files.delete(pth);
                    } catch (IOException ex) {
                        Logger.getLogger(FileChannelLongArrayTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
            Files.deleteIfExists(DIR);
        }
    }
}
