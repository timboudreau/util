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
package com.mastfrog.file.channels;

import com.mastfrog.util.file.FileUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class RandomAccessChannelTest {

    private Path tmp;
    private FileChannelPool pool;

    @Test
    public void testReadWriteIsCapableOfRandomAccess() throws IOException {
        Lease lease = pool.lease(tmp, StandardOpenOption.READ, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        lease.use(ch -> {
            assertNotEquals(0L, ch.position());
            assertNotEquals(0L, ch.size());
            ch.write(ByteBuffer.wrap("And this is some more\n".getBytes(US_ASCII)));
            ch.position(0);
            byte[] bytes = new byte[(int) ch.size()];
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            ch.read(buf);
            List<String> s = Arrays.asList(new String(bytes, US_ASCII).split("\n"));
            System.out.println("S: " + s);
            assertEquals(2, s.size());
            assertEquals("This is the initial content", s.get(0));
            assertEquals("And this is some more", s.get(1));
        });
    }

    @Test
    public void testReadWriteWithAppend() throws IOException {
        Lease lease = pool.lease(tmp, StandardOpenOption.READ, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        lease.use(ch -> {
            ch.write(ByteBuffer.wrap("And this is some more\n".getBytes(US_ASCII)));
        });
        List<String> l = Files.readAllLines(tmp, US_ASCII);
        assertEquals("This is the initial content", l.get(0));
        assertEquals(2, l.size());
        assertEquals("And this is some more", l.get(1));
        lease.use(ch -> {
            ch.position(0);
            assertEquals(0, ch.position());
            ch.write(ByteBuffer.wrap("Now we have new text\n".getBytes(US_ASCII)));
            ch.truncate(ch.position());
        });
        l = Files.readAllLines(tmp, US_ASCII);
        assertEquals(1, l.size());
        assertEquals("Now we have new text", l.get(0));
    }

    @BeforeEach
    void setup() throws IOException {
        tmp = FileUtils.newTempFile();
        FileUtils.writeAscii(tmp, "This is the initial content\n");
        pool = FileChannelPool.newPool(30000);
    }

    @AfterEach
    void teardown() throws Exception {
        FileUtils.deleteIfExists(tmp);
    }
}
