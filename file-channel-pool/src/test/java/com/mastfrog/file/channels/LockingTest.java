package com.mastfrog.file.channels;

import com.mastfrog.function.throwing.io.IOConsumer;
import com.mastfrog.util.file.FileUtils;
import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.thread.AtomicMaximum;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LockingTest {

    private Path file;
    private FileChannelPool pool;
    private static final String[] EXPECTED = {
        "This is some content which shall be given up for you.",
        "And this is some more.",
        "Hello world."
    };

    @Test
    public void testLocking() throws Exception {
        Lease lease1 = pool.lease(file, StandardOpenOption.READ);
        Lease lease2 = pool.lease(file, StandardOpenOption.READ);
        Lease lease3 = pool.lease(file, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        assertNotSame(lease1, lease2);
        CountDownLatch enterLatch = new CountDownLatch(3);
        CountDownLatch exitLatch = new CountDownLatch(3);
        CountDownLatch afterMove = new CountDownLatch(1);
        FileChannel[] channels = new FileChannel[3];
        Phaser phaser = new Phaser(1);
        AtomicMaximum max = new AtomicMaximum();
        IOConsumer<FileChannel> mover = ch -> {
            synchronized (channels) {
                channels[0] = FileChannelWrapper.unwrap(ch);
            }
            max.ioRun(() -> {
                assertEquals(0L, ch.position());
                ch.position(5);
                afterMove.countDown();
            });
        };

        IOConsumer<FileChannel> checker = ch -> {
            synchronized (channels) {
                channels[1] = FileChannelWrapper.unwrap(ch);
            }
            try {
                afterMove.await(4, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Exceptions.chuck(ex);
            }
            max.ioRun(() -> {
                assertEquals(0L, ch.position(), "Position is not as expected");
            });
        };

        IOConsumer<FileChannel> writer = ch -> {
            synchronized (channels) {
                channels[2] = FileChannelWrapper.unwrap(ch);
            }
            max.ioRun(() -> {
                assertEquals(ch.size(), ch.position());
                ch.write(ByteBuffer.wrap("Hello world.\n".getBytes(US_ASCII)));
            });
        };
        Th th1 = new Th(1, lease1, exitLatch, phaser, mover, enterLatch);
        Th th2 = new Th(2, lease2, exitLatch, phaser, checker, enterLatch);
        Th th3 = new Th(3, lease3, exitLatch, phaser, writer, enterLatch);
        th1.start();
        th2.start();
        th3.start();
        enterLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(350);
        phaser.arriveAndDeregister();
        exitLatch.await(20, TimeUnit.SECONDS);
        assertNotNull(channels[0], "th-1 did not run");
        assertNotNull(channels[1], "th-2 did not run");
        assertSame(channels[0], channels[1], "Did not get same channel");
        assertNotSame(channels[0], channels[2], "Write channel should not be same");
        th1.assertExited();
        th2.assertExited();
        th3.assertExited();
        th1.rethrow();
        th2.rethrow();
        th3.rethrow();
        List<String> lines = Files.readAllLines(file);
        assertEquals(Arrays.asList(EXPECTED), lines);
        // We cannot look for a hard value because this is up to the vagaries
        // of the os's thread scheduler, though the sleep above increases the
        // likelihood of concurrent access between the write and one of the
        // readers
        int concurrency = max.getMaximum();
        assertTrue(concurrency > 0 && concurrency < 3, "Minimum of one and "
                + "maximum of two threads should have been able to touch "
                + file + " at the same time, but concurrency was " + concurrency);
    }

    static class Th extends Thread {

        private final Lease lease;
        private final CountDownLatch exitLatch;
        private final IOConsumer<FileChannel> ch;
        private volatile Throwable thrown;
        private final Phaser phaser;
        private volatile boolean exited;
        private final CountDownLatch enterLatch;

        public Th(int num, Lease lease, CountDownLatch exitLatch, Phaser phaser, IOConsumer<FileChannel> ch, CountDownLatch enterLatch) {
            super("th-" + num);
            this.lease = lease;
            this.exitLatch = exitLatch;
            this.ch = ch;
            this.phaser = phaser;
            phaser.register();
            this.enterLatch = enterLatch;
        }

        void rethrow() {
            if (thrown != null) {
                Exceptions.chuck(thrown);
            }
        }

        void assertExited() {
            assertTrue(exited, getName());
        }

        @Override
        public void run() {
            try {
                enterLatch.countDown();
                phaser.arriveAndAwaitAdvance();
                lease.use(ch);
            } catch (Throwable ex) {
                thrown = ex;
            } finally {
                exited = true;
                exitLatch.countDown();
            }
        }
    }

    @BeforeEach
    public void setup() throws IOException {
        file = FileUtils.newTempFile(
                LockingTest.class.getName().replace('.', '-') + "-");
        FileUtils.writeAscii(file, "This is some content which shall be "
                + "given up for you.\nAnd this is some more.\n");
        pool = FileChannelPool.newPool(Duration.ofMinutes(1));
    }

    @AfterEach
    public void teardown() throws IOException {
        try {
            FileUtils.deleteIfExists(file);
        } finally {
            if (pool != null) {
                pool.close();
            }
        }
    }
}
