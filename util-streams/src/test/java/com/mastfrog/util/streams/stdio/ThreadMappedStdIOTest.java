/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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
package com.mastfrog.util.streams.stdio;

import com.mastfrog.util.preconditions.Exceptions;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ThreadMappedStdIOTest {


    private static final String[] MSGS = new String[]{
        "snakes", "wookie", "froober", "fnord", "wuzzle"
    };
    private CountDownLatch latch;
    private CountDownLatch startLatch;
    private CountDownLatch exitLatch;
    private Phaser phaser;
    private static final String NON_MSG = "jumpnjive";
    private static final int MAX = 2000;
    private PSFactory[] factories = new PSFactory[MSGS.length];
    private Msgr[] msgrs = new Msgr[MSGS.length];
    private Runnable[] ths = new Th[MSGS.length];
    private Thread[] threads = new Thread[MSGS.length];
    private Thread normThread;
    private Msgr normMsgr;
    private Bl blackhole;
    private Thread blackholeThread;
    private Msgr blMsg;

    @Before
    public void setup() throws UnsupportedEncodingException {
        phaser = new Phaser(MSGS.length + 3);
        latch = new CountDownLatch(MSGS.length + 2);
        startLatch = new CountDownLatch(MSGS.length + 2);
        exitLatch = new CountDownLatch(1);
        int total = MSGS.length;
        factories = new PSFactory[total];
        for (int i = 0; i < total; i++) {
            factories[i] = new PSFactory();
            msgrs[i] = new Msgr(MSGS[i], MAX, latch, phaser, startLatch, exitLatch);
            ths[i] = new Th(msgrs[i], factories[i].print);
            threads[i] = new Thread(ths[i]);
            threads[i].setName(MSGS[i]);
            threads[i].setDaemon(true);
        }
        normMsgr = new Msgr("normal", 1000, latch, phaser, startLatch, exitLatch);
        normThread = new Thread(normMsgr);
        normThread.setName("norm");
        normThread.setDaemon(true);
        blMsg = new Msgr("snikt", 1000, latch, phaser, startLatch, exitLatch);
        blackhole = new Bl(blMsg);
        blackholeThread = new Thread(blackhole);
        blackholeThread.setName("blackhole");
        blackholeThread.setDaemon(true);
    }

    private void doAllTheThings() throws InterruptedException {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        System.out.println("ORIG OUT IS " + origOut + ": " + origOut.getClass().getName());
        int total = MSGS.length;
        for (int i = 0; i < total; i++) {
            threads[i].start();
        }
        normThread.start();
        blackholeThread.start();
        // Allow the threads to enter their loops
        phaser.arriveAndDeregister();
        System.out.println("begin");
        // Wait for them all really to start executing their loops
        startLatch.await(30, SECONDS);
        assertNotSame(origOut, System.out);
        assertNotSame(origErr, System.err);
        System.out.println("end");
        // Print some stuff ourselves that should not show up in any thread's output
        for (int i = 0; i < 30; i++) {
            System.out.println(NON_MSG + "-" + i);
            if (i == 10) {
                // exitLatch protects us against all threads having exited before
                // we get here
                assertTrue(ThreadMappedStdIO.isActive());
            }
            Thread.sleep(10);
        }
        // Wait for all the loops to get done
        latch.await(30, SECONDS);
        // NOW, let the runnables exit io mapping
        exitLatch.countDown();
        for (Thread t : threads) {
            t.join(10000);
        }
        assertSame(origOut, System.out);
        assertSame(origErr, System.err);
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        doAllTheThings();
        for (int i = 0; i < MSGS.length; i++) {
            PSFactory outputHolder = factories[i];
            String[] notPresent = remainderLess(MSGS[i]);
            outputHolder.assertNotInText("In text for thread of '" + MSGS[i] + "'", notPresent);
            outputHolder.assertInText("Missing output that should go here", MSGS[i]);
        }
        assertFalse(ThreadMappedStdIO.isActive());
    }

    private String[] remainderLess(String msg) {
        Set<String> all = new TreeSet<>(Arrays.asList(MSGS));
        all.add(NON_MSG);
        all.add("snikt");
        all.add("normal");
        all.remove(msg);
        return all.toArray(new String[0]);
    }

    private static class Msgr implements Runnable {

        private final String msg;
        private final int max;
        private final CountDownLatch latch;
        private final Phaser phaser;
        private final CountDownLatch startLatch;
        private final CountDownLatch exitLatch;

        public Msgr(String msg, int max, CountDownLatch latch, Phaser phaser, CountDownLatch startLatch, CountDownLatch exitLatch) {
            this.msg = msg;
            this.max = max;
            this.latch = latch;
            this.phaser = phaser;
            this.startLatch = startLatch;
            this.exitLatch = exitLatch;
        }

        @Override
        public void run() {
            try {
                phaser.arriveAndAwaitAdvance();
                startLatch.countDown();
                for (int i = 0; i < max; i++) {
                    if (i % 2 == 0) {
                        System.out.println(msg + "-" + i);
                    } else {
                        System.err.println(msg + "-" + i);
                    }
                    Thread.yield();
                    if (i == 50) {
                        ThreadMappedStdIO.bypass(() -> {
                            System.out.println("Cukaburra!");
                        });
                    }
                }
            } finally {
                latch.countDown();
                try {
                    exitLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private static class Th implements Runnable {

        private final Runnable toRun;
        private final PrintStream out;
        private final PrintStream err;

        public Th(Runnable toRun, PrintStream out) {
            this(toRun, out, out);
        }

        public Th(Runnable toRun, PrintStream out, PrintStream err) {
            this.toRun = toRun;
            this.out = out;
            this.err = err;
        }

        @Override
        public void run() {
            System.out.println("enter io " + Thread.currentThread().getName());
            try {
                ThreadMappedStdIO.enterNonThrowing(out, err, toRun);
            } finally {
                System.out.println("exit io " + Thread.currentThread().getName());
            }
        }
    }

    private static class Bl implements Runnable {

        private final Runnable run;

        public Bl(Runnable run) {
            this.run = run;
        }

        @Override
        public void run() {
            ThreadMappedStdIO.blackhole(run);
        }
    }

    private static class PSFactory {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
//        private final PrintStream print = new PrintStream(out, true, US_ASCII);
        private final PrintStream print;

        PSFactory() throws UnsupportedEncodingException {
            print = new PrintStream(out, true, "us-ascii");
        }

        public String[] lines() {
            String s = new String(out.toByteArray(), US_ASCII);
            return s.split("\n");
        }

        public String toString() {
            return new String(out.toByteArray(), US_ASCII);
        }

        void assertInText(String msg, String... what) {
            String txt = toString();
            for (String w : what) {
                assertTrue(msg + ": did not find presence of '" + w + "' in \n" + txt, txt.contains(w));
            }

        }

        void assertNotInText(String msg, String... what) {
            String txt = toString();
            for (String w : what) {
                assertFalse(msg + ": found presence of '" + w + "' in \n" + txt, txt.contains(w));
            }
        }
    }

}
