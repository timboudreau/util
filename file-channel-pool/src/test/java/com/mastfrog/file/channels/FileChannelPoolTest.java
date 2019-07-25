/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to useAsLong, copy, modify, merge, publish, distribute, sublicense, and/or sell
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
class FileChannelPoolTest {

    private Path path;
    private FileChannelPool pool;
    private String[] datas;
    private int targetLength;
    private static final int TRUNCATE_POS = 4;
    private static final int TRUNCATE_POS_2 = 2;

    @Test
    void testSomeMethod() throws Throwable {
        assertNotNull(datas);
        assertTrue(datas.length > 0);
        Lease writeLease = pool.lease(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        long lastPos = -1;
        long[] truncateAt = new long[]{-1L, -1L};
        for (int i = 0; i < datas.length; i++) {
            long[] postWriteChannelPosition = new long[1];
            String dta = datas[i];
            assertNotNull(dta, "Test is broken at " + i);
            long preWriteLeasePosition = writeLease.position();
            assertNotEquals(lastPos, preWriteLeasePosition);
            if (i == TRUNCATE_POS) {
                truncateAt[0] = preWriteLeasePosition;
            } else if (i == TRUNCATE_POS_2) {
                truncateAt[1] = preWriteLeasePosition;
            }
            writeLease.use(channel -> {
                long origChannelPosition = channel.position();
                byte[] b = dta.getBytes(US_ASCII);
                ByteBuffer buf = ByteBuffer.allocate(b.length);
                buf.put(b);
                buf.flip();
                channel.write(buf);
                long channelPos = channel.position();
                assertNotEquals(origChannelPosition, channelPos);
                assertEquals(origChannelPosition, preWriteLeasePosition);
                postWriteChannelPosition[0] = channelPos;
            });
            long postWriteLeasePosition = writeLease.position();
            assertNotSame(preWriteLeasePosition, postWriteChannelPosition[0]);
            assertEquals(postWriteChannelPosition[0], postWriteLeasePosition);
            lastPos = preWriteLeasePosition;
        }
        assertNotEquals(-1L, truncateAt[0]);
        assertNotEquals(-1L, truncateAt[1]);
        assertTrue(truncateAt[1] < truncateAt[0], Arrays.toString(truncateAt));

        Lease readLease = pool.lease(path, StandardOpenOption.READ);
        for (int i = 0; i < datas.length; i++) {
            String dta = datas[i];
            assertNotNull(dta, "Test is broken at " + i);
            long preReadLeasePosition = readLease.position();
            long[] postWriteChannelPosition = new long[1];
            String got = readLease.use((FileChannel ch) -> {
                long pos = ch.position();
                assertEquals(preReadLeasePosition, pos, "Channel position is not lease position");
                ByteBuffer b = ByteBuffer.allocate(targetLength);
                ch.read(b);
                b.flip();
                byte[] bytes = new byte[b.limit()];
                b.get(bytes);
                postWriteChannelPosition[0] = ch.position();
                return new String(bytes, US_ASCII);
            });
            long postReadLeasePosition = readLease.position();
            assertNotEquals(preReadLeasePosition, postReadLeasePosition);
            assertEquals(preReadLeasePosition + targetLength, postReadLeasePosition);
            assertEquals(postWriteChannelPosition[0], postReadLeasePosition);
            assertEquals(dta, got);
        }

        writeLease.use(ch -> {
            System.out.println("truncate to " + truncateAt[0]);
            ch.truncate(truncateAt[0]);
        });

        readLease.use(ch -> {
            assertEquals(truncateAt[0], ch.position());
            assertEquals(ch.size(), ch.position());
        });

        long oldEnd = readLease.position();
        writeLease.use(ch -> {
            System.out.println("now truncate to " + truncateAt[1]);
            ch.truncate(truncateAt[1]);
        });
        long newEnd = readLease.syncPosition();
        assertNotEquals(oldEnd, newEnd);
        assertEquals(Files.size(path), newEnd);

        readLease.position(0);

        for (int i = 0; i < TRUNCATE_POS_2; i++) {
            String dta = datas[i];
            assertNotNull(dta, "Test is broken at " + i);
            long preReadLeasePosition = readLease.position();
            long[] postWriteChannelPosition = new long[1];
            String got = readLease.use((FileChannel ch) -> {
                long pos = ch.position();
                assertEquals(preReadLeasePosition, pos, "Channel position is not lease position");
                ByteBuffer b = ByteBuffer.allocate(targetLength);
                ch.read(b);
                b.flip();
                byte[] bytes = new byte[b.limit()];
                b.get(bytes);
                postWriteChannelPosition[0] = ch.position();
                return new String(bytes, US_ASCII);
            });
            long postReadLeasePosition = readLease.position();
            assertNotEquals(preReadLeasePosition, postReadLeasePosition);
            assertEquals(preReadLeasePosition + targetLength, postReadLeasePosition);
            assertEquals(postWriteChannelPosition[0], postReadLeasePosition);
            assertEquals(dta, got);
        }
    }

    private static final String base = "Hello-there-";

    @BeforeEach
    void before() {
        datas = new String[16];
        targetLength = (base + "0").getBytes(US_ASCII).length;
        for (int i = 0; i < datas.length; i++) {
            String s = base + Integer.toHexString(i);
            datas[i] = s;
            byte[] b = s.getBytes(US_ASCII);
            assertEquals(targetLength, b.length, s);
        }
        pool = FileChannelPool.newPool(5000);
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        path = tmp.resolve(getClass().getSimpleName() + "-"
                + Long.toString(System.currentTimeMillis(), 36)
                + ".cursor");
    }

    @AfterEach
    void after() throws IOException {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } finally {
            pool.close();
        }
    }
}
