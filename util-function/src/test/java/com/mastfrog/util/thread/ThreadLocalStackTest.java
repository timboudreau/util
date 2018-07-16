/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.thread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ThreadLocalStackTest {

    static ThreadLocalStack<String> stack = new ThreadLocalStack<>();

    private static final int MAX_DEPTH = 2000;

    private static final int THREADS = 8;

    static char base = 'a';

    static class Tester implements Runnable {

        char myBase = base++;
        int ix;
        Throwable thrown;
        private final CountDownLatch awaitStart;
        private final CountDownLatch notifyDone;

        Tester(CountDownLatch awaitStart, CountDownLatch notifyDone) {
            this.awaitStart = awaitStart;
            this.notifyDone = notifyDone;

        }

        void rethrow() throws Throwable {
            if (thrown != null) {
                throw thrown;
            }
        }

        String next() {
            return myBase + "" + ++ix;
        }

        String prev() {
            return myBase + "" + --ix;
        }

        @Override
        public void run() {
            try {
                awaitStart.await(2, TimeUnit.SECONDS);
                recurse(null, 1);
            } catch (Throwable thrown) {
                this.thrown = thrown;
            } finally {
                notifyDone.countDown();
            }
        }

        void recurse(String curr, int ix) throws Throwable {
            if (ix > 0) {
                assertEquals(curr, stack.peek());
            }
            String next = myBase + "" + (ix + 1);
            if (ix < MAX_DEPTH) {
                try (QuietAutoCloseable ac = stack.push(next)) {
                    recurse(next, ix + 1);
                    Thread.yield();
                }
            }
        }
    }

    @Test
    public void testSomeMethod() throws Throwable {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREADS);
        ExecutorService threadPool = Executors.newFixedThreadPool(THREADS);
        Tester[] testers = new Tester[THREADS];
        for (int i = 0; i < testers.length; i++) {
            testers[i] = new Tester(startLatch, endLatch);
            threadPool.submit(testers[i]);
        }
        Thread.sleep(1);
        startLatch.countDown();
        endLatch.await(20, TimeUnit.SECONDS);
        for (Tester t : testers) {
            t.rethrow();
        }
    }

}
