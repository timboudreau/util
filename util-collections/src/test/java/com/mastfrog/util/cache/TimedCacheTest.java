package com.mastfrog.util.cache;

import com.mastfrog.util.cache.TimedCacheImpl.BidiCacheImpl;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TimedCacheTest {

    @Test
    public void testCache() throws Throwable {
        Set<Integer> expiredInts = Collections.synchronizedSet(new HashSet<>());
        BiConsumer<Integer, String> onExpire = (i, s) -> {
            expiredInts.add(i);
        };
        EXPIRER.blockExpire();
        BidiCacheImpl<Integer, String, RuntimeException> cache
                = (BidiCacheImpl<Integer, String, RuntimeException>) TimedCache.<Integer, String>create(60,
                        i -> "i" + Integer.toString(i))
                        .toBidiCache(
                                s -> Integer.parseInt(s.substring(1))).onExpire(onExpire);
        assertEquals("i23", cache.get(23));
        assertTrue(cache.containsValue("i23"));
        assertTrue(cache.containsKey(23));
        EXPIRER.assertLastOffered("23:i23");
        Thread.sleep(120);
        EXPIRER.assertLastExpired("23:i23");
        assertFalse(cache.containsValue("i23"));
        assertFalse(cache.containsKey(23));
        EXPIRER.blockExpire();
        assertEquals(Integer.valueOf(24), cache.getKey("i24"));
        assertEquals("i24", cache.get(24));
        assertTrue(cache.containsValue("i24"));
        assertTrue(cache.containsKey(24));
        EXPIRER.assertLastOffered("24:i24");
        Thread.sleep(120);
        EXPIRER.assertLastExpired("24:i24");
        assertTrue(expiredInts.contains(23));
        assertTrue(expiredInts.contains(24));
    }

    @BeforeClass
    public static void setup() {
        TimedCacheImpl.expirerFactory = () -> {
            return EXPIRER;
        };
    }

    private static TestExpirer EXPIRER = new TestExpirer();

    static class TestExpirer extends Expirer {

        volatile CountDownLatch latch;
        final AtomicReference<Expirable> lastOffered = new AtomicReference<>();
        final AtomicReference<Expirable> lastExpired = new AtomicReference<>();

        TestExpirer() {
            super(Thread.NORM_PRIORITY + 2);
            assertNull(EXPIRER);
            EXPIRER = this;
        }

        void blockExpire() {
            latch = new CountDownLatch(1);
        }

        void assertLastExpired(String txt) throws InterruptedException {
            synchronized (this) {
                wait(100);
            }
            Expirable last = lastExpired.getAndSet(null);
            assertNotNull(last);
            assertEquals(txt, last.toString());
        }

        void assertLastOffered(String txt) throws InterruptedException {
            synchronized (this) {
                wait(100);
            }
            Expirable last = lastOffered.getAndSet(null);
            assertNotNull(last);
            assertEquals(txt, last.toString());
            if (latch != null) {
                latch.countDown();
                latch = null;
            }
        }

        @Override
        void offer(Expirable expirable) {
            lastOffered.set(expirable);
            super.offer(expirable);
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        void expireOne(Expirable toExpire) {
            lastExpired.set(toExpire);
            CountDownLatch l = latch;
            if (l != null) {
                try {
                    l.await(5, SECONDS);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            super.expireOne(toExpire);
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
