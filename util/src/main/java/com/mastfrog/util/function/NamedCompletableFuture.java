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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A completable future with a name for logging purposes, which includes a
 * context * name and optional creation stack trace.
 *
 * @author Tim Boudreau
 */
public class NamedCompletableFuture<T> extends CompletableFuture<T> {

    private static final StackTraceElement[] EMPTY_STACK = new StackTraceElement[0];
    private final String name;
    private final Observer<? super T> obs;
    private final Throwable creation;
    private final AtomicReference<Throwable> whenCompleted;

    NamedCompletableFuture(String name, Observer<? super T> obs, boolean stack) {
        this.name = name;
        this.obs = obs;
        if (stack) {
            creation = new Exception();
            whenCompleted = new AtomicReference<>();
        } else {
            creation = null;
            whenCompleted = null;
        }
    }

    public static <T> NamedCompletableFuture<T> loggingFuture(String name, boolean log) {
        return log ? loggingFuture(name) : namedFuture(name);
    }

    public static <T> NamedCompletableFuture<T> loggingFuture(String name) {
        return NamedCompletableFuture.namedFuture(name, true, LOGGING_OBSERVER);
    }

    /**
     * Create a named completable future with the passed name.
     *
     * @param <T> The completion type
     * @param name A name
     * @return A future
     */
    public static <T> NamedCompletableFuture<T> namedFuture(String name) {
        return new NamedCompletableFuture<>(name, null, false);
    }

    /**
     * Create a named completable future with the passed name.
     *
     * @param <T> The completion type
     * @param name A name
     * @param obs An observer notified before super.complete() or
     * super.completeExceptionally() is called
     * @return
     */
    public static <T> NamedCompletableFuture<T> namedFuture(String name, Observer<? super T> obs) {
        return new NamedCompletableFuture<>(name, obs, false);
    }

    /**
     * Create a named completable future with the passed name, which, if the
     * track parameter is true, will also keep a stack trace for when it was
     * created, and append that as a suppressed exception on exceptional
     * completion
     *
     * @param <T> The completion type
     * @param name A name
     * @param track Whether or not to hold a creation throwable
     * @param obs An observer
     * @return The name
     */
    public static <T> NamedCompletableFuture<T> namedFuture(String name, boolean track, Observer<? super T> obs) {
        return new NamedCompletableFuture<>(name, obs, track);
    }

    public StackTraceElement[] creationStackTrace() {
        return creation == null ? EMPTY_STACK : creation.getStackTrace();
    }

    public StackTraceElement[] completionStackTrace() {
        if (this.whenCompleted != null && this.whenCompleted.get() != null) {
            return this.whenCompleted.get().getStackTrace();
        }
        return EMPTY_STACK;
    }

    public void checkNotDone() {
        if (isDone()) {
            throw new IllegalStateException("Future " + name
                    + " already complete");
        }
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        if (creation != null) {
            StackTraceElement[] ste = creation.getStackTrace();
            if (ste.length > 3) {
                return name + "{done=" + isDone() + ","
                        + ste[2]
                        + "," + ste[3]
                        + " " + super.toString() + "}";
            } else {
                return name + "{done=" + isDone() + "," + super.toString() + "}";
            }
        } else {
            return name + "{done=" + isDone() + "," + super.toString() + "}";
        }
    }

    @Override
    public boolean complete(T value) {
        try {
            if (obs != null) {
                obs.onBeforeComplete(name, value, creation, isDone());
            }
            if (whenCompleted != null && whenCompleted.get() == null) {
                whenCompleted.set(new RuntimeException("Complete " + name + " normally"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        try {
            if (ex != null) { // ex null is a bug, but don't hide its stack by
                // raising an NPE here
                if (creation != null) {
                    ex.addSuppressed(creation);
                }
                if (whenCompleted != null && whenCompleted.get() != null) {
                    ex.addSuppressed(whenCompleted.get());
                } else if (whenCompleted != null && whenCompleted.get() == null) {
                    RuntimeException rex = new RuntimeException("Complete " + name + " exceptionally");
                    whenCompleted.set(rex);
                    ex.addSuppressed(rex);
                }
            }
            if (obs != null) {
                obs.onBeforeComplete(name, null, creation, isDone());
            }
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }
        return super.completeExceptionally(ex);
    }

    /**
     * Called before complete and complete exceptionally to debug or log
     * activity.
     *
     * @param <T>
     */
    @FunctionalInterface
    public interface Observer<T> {

        /**
         * Called before super.complete() or super.completeExceptionally() in
         * NamedCompletableFuture.
         *
         * @param name
         * @param completeWith
         * @param orCompleteWith
         * @param alreadyDone
         */
        void onBeforeComplete(String name, T completeWith, Throwable orCompleteWith, boolean alreadyDone);
    }

    static final Observer<Object> LOGGING_OBSERVER = new LoggingObserver();

    static final class LoggingObserver implements Observer<Object> {

        @Override
        public void onBeforeComplete(String name, Object completeWith, Throwable orCompleteWith, boolean alreadyDone) {
            if (completeWith != null) {
                System.err.println("complete " + name + " with success already done? " + alreadyDone);
            }
            if (alreadyDone && orCompleteWith != null) {
                RuntimeException ex = new RuntimeException("Completing " + name + " twice!", orCompleteWith);
                ex.printStackTrace(System.err);
            } else if (alreadyDone) {
                RuntimeException ex = new RuntimeException("Completing " + name + " twice!");
                ex.printStackTrace(System.err);
            }
            if (completeWith == null && orCompleteWith != null) {
                System.err.println("complete exceptionally " + name + " with " + orCompleteWith);
                orCompleteWith.printStackTrace();
            }
        }
    }
}
