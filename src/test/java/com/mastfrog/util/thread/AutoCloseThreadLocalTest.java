package com.mastfrog.util.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class AutoCloseThreadLocalTest {

    @Test
    public void testSet() throws Throwable {
        int count = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);
        AutoCloseThreadLocal<Integer> local = new AutoCloseThreadLocal<>();
        List<R> rs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            R r = new R(done, local, start);
            rs.add(r);
            Thread t = new Thread(r);
            t.setName("Test thread " + i);
            t.start();
        }
        Thread.sleep(200);
        start.countDown();
        done.await();
        for (R r : rs) {
            r.throwIfError();
        }
    }

    static class R implements Runnable {

        private final CountDownLatch latch;
        private final AutoCloseThreadLocal<Integer> local;
        private final CountDownLatch start;
        private Throwable thrown;

        public R(CountDownLatch latch, AutoCloseThreadLocal<Integer> local, CountDownLatch start) {
            this.latch = latch;
            this.local = local;
            this.start = start;
        }

        void throwIfError() throws Throwable {
            if (thrown != null) {
                throw thrown;
            }
        }

        @Override
        public void run() {
            try {
                start.await();
                assertNull(local.get());
                try (QuietAutoCloseable ac1 = local.set(1)) {
                    assertEquals(Integer.valueOf(1), local.get());
                    try (QuietAutoCloseable ac2 = local.set(2)) {
                        assertEquals(Integer.valueOf(2), local.get());
                        try (QuietAutoCloseable ac3 = local.set(3)) {
                            assertEquals(Integer.valueOf(3), local.get());
                            try (QuietAutoCloseable ac4 = local.set(4)) {
                                assertEquals(Integer.valueOf(4), local.get());
                                try (QuietAutoCloseable ac5 = local.set(5)) {
                                    assertEquals(Integer.valueOf(5), local.get());
                                }
                                assertEquals(Integer.valueOf(4), local.get());
                            }
                            assertEquals(Integer.valueOf(3), local.get());
                        }
                        assertEquals(Integer.valueOf(2), local.get());
                    }
                    assertEquals(Integer.valueOf(1), local.get());
                }
                assertNull(local.get());
            } catch (Throwable ex) {
                thrown = ex;
                Logger.getLogger(AutoCloseThreadLocalTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                latch.countDown();
            }
        }

    }

}
