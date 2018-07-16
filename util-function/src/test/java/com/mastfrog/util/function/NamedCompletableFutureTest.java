/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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
package com.mastfrog.util.function;

import com.mastfrog.util.preconditions.Exceptions;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class NamedCompletableFutureTest {

    @Test
    public void testObserverIsCalled() {
        Obs obs = new Obs();
        NamedCompletableFuture<String> one = NamedCompletableFuture.namedFuture("one", true, obs);
        assertTrue(one.toString().contains("one"));
        assertNotEquals(0, one.creationStackTrace().length);
        assertEquals(0, one.completionStackTrace().length);
        obs.assertNotCompleted();
        one.complete("hello");
        obs.assertCompletedWith("hello")
                .assertCompleted("one")
                .assertCompleted()
                .assertNotCompletedTwice()
                .assertNotCompletedExceptionally();

        one = NamedCompletableFuture.namedFuture("two", true, obs.reset());
        one.completeExceptionally(new Exception("hey"));
        obs.assertCompleted("two").assertCompletedExceptionally()
                .assertNotCompletedTwice()
                .assertCompleted();
        assertNotEquals(0, one.completionStackTrace().length);

        one = NamedCompletableFuture.namedFuture("three", true, obs.reset());
        one.complete("a");
        one.complete("b");
        obs.assertCompletedTwice()
                .assertCompleted()
                .assertCompleted("three")
                .assertCompletedWith("a")
                .assertCompletedWith("b");
    }

    @Test
    public void testLoggingWorks() throws Throwable {
        String out = wrapSystemOut(() -> {
            NamedCompletableFuture<String> one = NamedCompletableFuture.loggingFuture("someFuture", true);
            one.checkNotDone();
            one.complete("foo");
            one.complete("bar");

            one = NamedCompletableFuture.loggingFuture("b", true);
            one.completeExceptionally(new RuntimeException("hey"));
        });
        assertTrue(out.contains("someFuture"));
        assertTrue(out.contains("Completing someFuture twice!"));
    }

    String wrapSystemOut(ThrowingRunnable run) throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.err;
        PrintStream newOut = new PrintStream(out);
        System.setErr(newOut);
        boolean failed = false;
        try {
            run.run();
        } catch (Throwable t) {
            failed = true;
            Exceptions.chuck(t);
        } finally {
            System.setErr(oldOut);
            if (failed) {
                String s = new String(out.toByteArray(), US_ASCII);
                oldOut.append(s);
            }
        }
        return new String(out.toByteArray(), US_ASCII);
    }

    static final class Obs implements NamedCompletableFuture.Observer<Object> {

        private volatile boolean completedWhenDone;
        private final Set<String> completedNames = new HashSet<>();
        private volatile boolean completed;
        private volatile boolean completedExceptionally;
        private final Set<Object> completedWith = new HashSet<>();

        Obs reset() {
            completedWhenDone = false;
            completedNames.clear();
            completed = false;
            completedExceptionally = false;
            completedWith.clear();
            return this;
        }

        Obs assertNotCompleted() {
            boolean result = completed;
            completed = false;
            assertFalse("Already completed", result);
            return this;
        }

        Obs assertCompleted() {
            boolean result = completed;
            completed = false;
            assertTrue("Never completed", result);
            return this;
        }

        Obs assertNotCompletedTwice() {
            boolean result = completedWhenDone;
            completedWhenDone = false;
            assertFalse("Was completed twice", result);
            return this;
        }

        Obs assertCompletedTwice() {
            boolean result = completedWhenDone;
            completedWhenDone = false;
            assertTrue("Was not completed twice", result);
            return this;
        }

        Obs assertCompleted(String name) {
            String names = completedNames.toString();
            boolean result = completedNames.remove(name);
            assertTrue(name + " not found in " + names, result);
            return this;
        }

        Obs assertNotCompleted(String name) {
            boolean result = completedNames.remove(name);
            assertFalse(name, result);
            return this;
        }

        Obs assertCompletedWith(Object o) {
            boolean result = completedWith.remove(o);
            assertTrue("Not completed with " + o, result);
            return this;
        }

        Obs assertCompletedExceptionally() {
            boolean result = completedExceptionally;
            completedExceptionally = false;
            assertTrue("Was not completed exceptionally", result);
            return this;
        }

        Obs assertNotCompletedExceptionally() {
            boolean result = completedExceptionally;
            completedExceptionally = false;
            assertFalse("Was completed exceptionally ", result);
            return this;
        }

        @Override
        public void onBeforeComplete(String name, Object completeWith, Throwable orCompleteWith, boolean alreadyDone) {
            completedWhenDone |= alreadyDone;
            completed = true;
            completedNames.add(name);
            completedExceptionally |= completeWith == null && orCompleteWith != null;
            if (completeWith != null) {
                completedWith.add(completeWith);
            }
        }
    }
}
