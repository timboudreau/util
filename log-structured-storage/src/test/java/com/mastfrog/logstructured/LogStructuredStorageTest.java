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
package com.mastfrog.logstructured;

import com.mastfrog.file.channels.FileChannelPool;
import com.mastfrog.file.channels.LeaseException;
import com.mastfrog.util.collections.ArrayUtils;
import com.mastfrog.util.file.FileUtils;
import com.mastfrog.util.preconditions.Exceptions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class LogStructuredStorageTest {

    private Path file;
    private FileChannelPool pool;
    private LogStructuredStorage<Thing> stor;
    private Path dir;
    private LogStructuredStorage<Thing> dirStor;

    @Test
    public void testDirStorage() throws InterruptedException, IOException {
        LogStructuredAppender<Thing> appender = dirStor.appender();
        Set<Thing> all = Collections.synchronizedSet(new HashSet<>());
        AtomicBoolean done = new AtomicBoolean();
        CountDownLatch exit = new CountDownLatch(3);
        R[] rs = new R[]{
            new R(1, appender, all, done, exit),
            new R(100000, appender, all, done, exit),
            new R(500000, appender, all, done, exit)
        };
        for (int i = 0; i < rs.length; i++) {
            Thread t = new Thread(rs[i]);
            t.setDaemon(true);
            t.setName("th-" + i);
            t.start();
        }
        Thread.sleep(750);
        done.set(true);
        exit.await(10, TimeUnit.SECONDS);
        long num;
        try (Stream<Path> s = Files.list(dir)) {
            num = s.count();
        }
        assertTrue(num > 0);
        for (R r : rs) {
            r.assertCleanExit();
        }
        LogStructuredReader<Thing> reader = dirStor.reader();
        Set<Thing> got = new HashSet<>();
        while (reader.hasUnread()) {
            UnadvancedRead<Thing> read = reader.read();
            got.add(read.get());
            read.advance();
        }
        assertEquals(all, got);
    }

    @Test
    public void testFileStorage() throws InterruptedException, IOException {
        LogStructuredAppender<Thing> appender = stor.appender();
        Set<Thing> all = Collections.synchronizedSet(new HashSet<>());
        AtomicBoolean done = new AtomicBoolean();
        CountDownLatch exit = new CountDownLatch(3);
        R[] rs = new R[]{
            new R(1, appender, all, done, exit),
            new R(100000, appender, all, done, exit),
            new R(500000, appender, all, done, exit)
        };
        for (int i = 0; i < rs.length; i++) {
            Thread t = new Thread(rs[i]);
            t.setDaemon(true);
            t.setName("th-" + i);
            t.start();
        }
        Thread.sleep(500);
        done.set(true);
        exit.await(10, TimeUnit.SECONDS);
        for (R r : rs) {
            r.assertCleanExit();
        }
        LogStructuredReader<Thing> reader = stor.reader();
        Set<Thing> got = new HashSet<>();
        while (reader.hasUnread()) {
            UnadvancedRead<Thing> read = reader.read();
            got.add(read.get());
            read.advance();
        }
        assertEquals(all, got);
    }

    class R implements Runnable {

        private final int base;
        private final LogStructuredAppender<Thing> appender;
        private final Set<Thing> things;
        private final AtomicBoolean done;
        private final CountDownLatch exit;
        private volatile boolean exited;
        private int counter;
        private volatile Throwable thrown;

        public R(int base, LogStructuredAppender<Thing> appender, Set<Thing> things, AtomicBoolean done, CountDownLatch exit) {
            this.base = base;
            this.appender = appender;
            this.things = things;
            this.done = done;
            this.exit = exit;
        }

        void assertCleanExit() {
            if (thrown != null) {
                Exceptions.chuck(thrown);
            }
            if (!exited) {
                throw new AssertionError("Exited is false");
            }
        }

        @Override
        public void run() {
            try {
                for (; !done.get();) {
                    Thing t = new Thing(base + counter++);
                    appender.append(t);
                    things.add(t);
                }
            } catch (Throwable ex) {
                thrown = ex;
            } finally {
                exit.countDown();
                exited = true;
            }
        }
    }

    @Test
    public void sanityCheck() throws IOException {
        ThingSerde things = new ThingSerde();
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            things.serialize(new Thing(1), ch);
            things.serialize(new Thing(13), ch);
            things.serialize(new Thing(29), ch);
        }
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            for (int i = 0; i < 3; i++) {
                Thing t = things.deserialize(file, ch);
                switch (i) {
                    case 0:
                        assertEquals(1, t.id());
                        break;
                    case 1:
                        assertEquals(13, t.id());
                        break;
                    case 2:
                        assertEquals(29, t.id());
                        break;
                }
            }
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        file = FileUtils.newTempFile(LogStructuredStorageTest.class.getName().replace('.', '-') + "-");
        pool = FileChannelPool.newPool(Duration.ofSeconds(10));
        stor = LogStructuredStorage.create(file, new ThingSerde(), pool);
        dir = FileUtils.newTempDir(LogStructuredStorageTest.class.getName().replace('.', '-') + "-");
        dirStor = LogStructuredStorage.forDir("thing", dir, new ThingSerde(), ".foo", 32768, pool);
    }

    @AfterEach
    public void teardown() throws IOException {
        pool.close();
        FileUtils.deleteIfExists(file);
        dirStor.delete();
        FileUtils.deltree(dir);
    }

    static final class ThingSerde implements Serde<Thing> {

        @Override
        public <C extends java.nio.channels.WritableByteChannel & java.nio.channels.SeekableByteChannel> void serialize(Thing obj, C channel) throws IOException {
            byte[] bytes = obj.toByteArray();
            ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
            len.putInt(bytes.length);
            len.flip();
            channel.write(len);
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            buf.flip();
            channel.write(buf);
        }

        @Override
        public <C extends java.nio.channels.ReadableByteChannel & java.nio.channels.SeekableByteChannel> Thing deserialize(Path in, C channel) throws LeaseException, IOException {
            ByteBuffer len = ByteBuffer.allocate(Integer.BYTES);
            channel.read(len);
            len.flip();
            int length = len.getInt();
            assert length > 0 : length + " too small";
            ByteBuffer body = ByteBuffer.allocate(length);
            int bytesRead = channel.read(body);
            assert length == bytesRead : "Expected " + length + " but read " + bytesRead;
            return new Thing(body.array());
        }
    }

    static final class Thing implements Comparable<Thing> {

        private final int id;

        public Thing(int id) {
            this.id = id;
        }

        public Thing(byte[] bytes) throws IOException {
            this(readId(bytes));
        }

        static int readId(byte[] b) throws IOException {
            if (b[0] != 1) {
                throw new IOException("No " + 1 + " at 0");
            }
            if (b[1] != 17) {
                throw new IOException("No " + 17 + " at 1");
            }
            if (b[2] != 23) {
                throw new IOException("No " + 23 + " at 2");
            }
            if (b[3] != 54) {
                throw new IOException("No " + 54 + " at " + 3);
            }
            ByteBuffer buf = ByteBuffer.wrap(b);
            buf.getInt(); // sig
            int result = buf.getInt();
            int remLength = (result % 20) * Integer.BYTES;
            if (remLength != b.length - (Integer.BYTES + 4)) {
                throw new IOException("Wrong remaining length: " + remLength + " but was " + (b.length - Integer.BYTES + 4));
            }
            for (int i = 0; i < remLength / Integer.BYTES; i++) {
                int val = buf.getInt();
                int exp = Integer.MAX_VALUE - (result + i);
                if (val != exp) {
                    throw new IOException("Expected " + exp + " for tail byte " + i + " but got " + val);
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return "Thing-" + id;
        }

        public int id() {
            return id;
        }

        public byte[] toByteArray() {
            byte[] result = new byte[]{1, 17, 23, 54};
            byte[] next = new byte[Integer.BYTES];
            ByteBuffer.wrap(next).putInt(id);
            int remLength = (id % 20) * Integer.BYTES;
            byte[] remainder = new byte[remLength];
            ByteBuffer buf = ByteBuffer.wrap(remainder);
            for (int i = 0; i < remLength / Integer.BYTES; i++) {
                buf.putInt(Integer.MAX_VALUE - (id + i));
            }
            return ArrayUtils.concatenate(result, next, remainder);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Thing other = (Thing) obj;
            if (this.id != other.id) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(Thing o) {
            return Integer.compare(id, o.id);
        }

    }
}
