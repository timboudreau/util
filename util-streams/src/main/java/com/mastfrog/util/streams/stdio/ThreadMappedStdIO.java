/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.util.streams.stdio;

import com.mastfrog.function.throwing.ThrowingRunnable;
import com.mastfrog.function.throwing.ThrowingSupplier;
import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.streams.Streams;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Allows System.out and System.err to be replaced just for a single thread,
 * while leaving them alone for the rest of the system.
 * <p>
 * Note, this is neessarily imperfect - if some other code replaces Syetem.out
 * with something else, we do not detect that situation (it would be impossible
 * to know what to do anyway - maybe the replacement wraps our output, maybe it
 * doesn't) - so this code assumes it is the only thing in the system fiddling
 * with the identity of the system stdout.
 * </p>
 * <p>
 * <code>System.out</code> and <code>System.err</code> will be restored fully
 * when the last concurrent thread exits a closure that replaced either, so this
 * class is zero-impact when not actively in use. Note: There are synchronized
 * blocks on initiating and exiting, so if many threads are contending to
 * replace stdio, liveness issues are possible.
 * </p>
 * <p>
 * Reentrancy is supported, so a thread may replace stdout and stderr, then do
 * so again, and state will be restored correctly as each caller exits its
 * closure.
 * <p>
 * Basically, the issue is that Antlr has calls to println baked into
 * it, parsers and lexers by default attach listeners that do so as well, and
 * catching every case where one might not have them removed is difficult. The
 * output is more usefully routed to the output window anyway, so getting it out
 * of the console output is handy.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class ThreadMappedStdIO {

    private final Map<Thread, IO> ioForThread = new ConcurrentHashMap<>();
    private final IO base = new IO();
    private IO original;
    private int entryCount;
    private final Str stdout = new Str(this::stdout);
    private final Str stderr = new Str(this::stderr);
    private static final ThreadMappedStdIO INSTANCE = new ThreadMappedStdIO();
    private static PrintStream nullPrintStream;

    private ThreadMappedStdIO() {

    }

    static boolean isActive() {
        synchronized (INSTANCE) {
            return INSTANCE.entryCount > 0;
        }
    }

    /**
     * If any alternate stdout or stderr is set for the current thread, bypass
     * that and use the original for one runnable; this is needed in cases where
     * you are patching or subclassing in a library that is sloppy with stdout,
     * but you have things you legitimately need to log, and want to be sure
     * that a console logger logs to the right place.
     *
     * @param r A runnable
     */
    public static void bypass(Runnable r) {
        INSTANCE._bypass(r);
    }

    private void _bypass(Runnable r) {
        IO io = ioForThread.get(Thread.currentThread());
        if (io == null) {
            r.run();
            return;
        }
        try {
            ioForThread.remove(Thread.currentThread());
            r.run();
        } finally {
            ioForThread.put(Thread.currentThread(), io);
        }
    }

    /**
     * Discard any attempts at writing to stdout or stderr during the closure of
     * the passed supplier, while not interfering with output from other
     * threads.
     *
     * @param <T> The return type
     * @param toRun A supplier
     * @return The value returned by the supplier
     */
    public static <T> T blackholeNonThrowing(Supplier<T> toRun) {
        try {
            return blackhole(toRun::get);
        } catch (Error | RuntimeException e) {
            throw (e);
        } catch (Exception ex) {
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Discard any attempts at writing to stdout or stderr during the closure of
     * the passed runnable, while not interfering with output from other
     * threads.
     *
     * @param toRun A runnable
     */
    public static void blackhole(Runnable toRun) {
        try {
            blackhole(() -> {
                toRun.run();
                return null;
            });
        } catch (Error | RuntimeException e) {
            throw (e);
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    /**
     * Discard any attempts at writing to stdout or stderr during the closure of
     * the passed runnable, while not interfering with output from other
     * threads.
     *
     * @param toRun A runnable
     */
    public static void blackhole(ThrowingRunnable toRun) throws Exception {
        blackhole(() -> {
            toRun.run();
            return null;
        });
    }

    /**
     * Discard any attempts at writing to stdout or stderr during the closure of
     * the passed supplier, while not interfering with output from other
     * threads.
     *
     * @param <T> The return type
     * @param toRun A supplier
     * @return The value returned by the supplier
     */
    public static <T> T blackhole(ThrowingSupplier<T> toRun) throws Exception {
        PrintStream noOp = nullPrintStream == null ? nullPrintStream = new PrintStream(Streams.nullOutputStream()) : nullPrintStream;
        return enter(noOp, toRun);
    }

    /**
     * Replace the standard output and error with a single stream, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingSupplier.
     *
     * @param <T> The return type
     * @param output The print stream to output to instead of the system one
     * @param toRun The code to run with that set as the stdout and stderr
     * @return The result of the supplier
     * @throws Exception If something goes wrong
     */
    public static <T> T enter(PrintStream output, ThrowingSupplier<T> toRun) throws Exception {
        return INSTANCE._enter(output, toRun);
    }

    private <T> T _enter(PrintStream output, ThrowingSupplier<T> toRun) throws Exception {
        return enter(output, output, toRun);
    }

    /**
     * Replace the standard output and error with a single stream, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingSupplier.
     *
     * @param <T> The return type
     * @param output The print stream to output to instead of the system one
     * @param toRun The code to run with that set as the stdout and stderr
     * @return The result of the supplier
     * @throws Exception If something goes wrong
     */
    public static <T> T enterNonThrowing(PrintStream output, Supplier<T> toRun) {
        return INSTANCE._enterNonThrowing(output, toRun);
    }

    private <T> T _enterNonThrowing(PrintStream output, Supplier<T> toRun) {
        try {
            return _enter(output, output, toRun::get);
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            // If we get here, someone is rethrowing an undeclared checked
            // exception (probably also using Exceptions.chuck()) - swallowing
            // it is definitiely the wrong answer, so kick the can down the road
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Replace the standard output and error with a single stream, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingSupplier.
     *
     * @param <T> The return type
     * @param stdout The print stream to output to instead of the system one
     * @param toRun The code to run with that set as the stdout and stderr
     * @return The result of the supplier
     * @throws Exception If something goes wrong
     */
    public static <T> T enterNonThrowing(PrintStream stdout, PrintStream stderr, Supplier<T> toRun) {
        return INSTANCE._enterNonThrowing(stdout, stderr, toRun);
    }

    private <T> T _enterNonThrowing(PrintStream stdout, PrintStream stderr, Supplier<T> toRun) {
        try {
            return _enter(stdout, stderr, toRun::get);
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            // If we get here, someone is rethrowing an undeclared checked
            // exception (probably also using Exceptions.chuck()) - swallowing
            // it is definitiely the wrong answer, so kick the can down the road
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Replace the standard output and error with a single stream, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingRunnable.
     *
     * @param output The print stream to output to instead of the system one
     * @param toRun The code to run with that set as the stdout and stderr
     * @throws Exception If something goes wrong
     */
    public static void enter(PrintStream output, ThrowingRunnable toRun) throws Exception {
        INSTANCE._enter(output, toRun);
    }

    private void _enter(PrintStream output, ThrowingRunnable toRun) throws Exception {
        enter(output, output, () -> {
            toRun.run();
            return null;
        });
    }

    /**
     * Replace the standard output and error with the passed streams, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingRunnable.
     *
     * @param output The print stream to output stdout to instead of the system
     * one
     * @param error The print stream to output stderr to instead of the system
     * one
     * @param toRun The code to run with that set as the stdout and stderr
     * @throws Exception If something goes wrong
     */
    public static void enter(PrintStream output, PrintStream error, ThrowingRunnable toRun) throws Exception {
        INSTANCE._enter(output, error, toRun);
    }

    private void _enter(PrintStream output, PrintStream error, ThrowingRunnable toRun) throws Exception {
        enter(output, error, () -> {
            toRun.run();
            return null;
        });
    }

    /**
     * Replace the standard output and error with the passed streams, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingRunnable.
     *
     * @param output The print stream to output stdout to instead of the system
     * one
     * @param error The print stream to output stderr to instead of the system
     * one
     * @param toRun The code to run with that set as the stdout and stderr
     * @throws Exception If something goes wrong
     */
    public static void enterNonThrowing(PrintStream output, PrintStream error, Runnable toRun) {
        INSTANCE._enterNonThrowing(output, error, toRun);
    }

    private void _enterNonThrowing(PrintStream output, PrintStream error, Runnable toRun) {
        try {
            _enter(output, error, () -> {
                toRun.run();
                return null;
            });
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            // If we get here, someone is rethrowing an undeclared checked
            // exception (probably also using Exceptions.chuck()) - swallowing
            // it is definitiely the wrong answer, so kick the can down the road
            Exceptions.chuck(ex);
        }
    }

    /**
     * Replace the standard output and error with the passed stream, for the
     * current thread, for the duration of the closure of the passed Runnable.
     *
     * @param output The print stream to output stdout to instead of the system
     * one
     * @param toRun The code to run with that set as the stdout and stderr
     * @throws Exception If something goes wrong
     */
    public static void enterNonThrowing(PrintStream output, Runnable toRun) {
        INSTANCE._enterNonThrowing(output, toRun);
    }

    private void _enterNonThrowing(PrintStream output, Runnable toRun) {
        try {
            enter(output, output, () -> {
                toRun.run();
                return null;
            });
        } catch (Error | RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            Exceptions.chuck(ex);
        }
    }

    /**
     * Replace the standard output and error with the passed streams, for the
     * current thread, for the duration of the closure of the passed
     * ThrowingSupplier.
     *
     * @param <T> The return type
     * @param output The print stream to output stdout to instead of the system
     * one
     * @param error The print stream to output stderr to instead of the system
     * one
     * @param toRun The code to run with that set as the stdout and stderr
     * @return The result of the supplier
     * @throws Exception If something goes wrong
     */
    public static <T> T enter(PrintStream output, PrintStream error, ThrowingSupplier<T> toRun) throws Exception {
        return INSTANCE._enter(output, error, toRun);
    }

    /**
     * The actual entry point.
     *
     * @param <T> The return type
     * @param output The output
     * @param error The error
     * @param toRun Something to run
     * @return The return value from the passed supplier
     * @throws Exception If something goes wrong
     */
    private <T> T _enter(PrintStream output, PrintStream error, ThrowingSupplier<T> toRun) throws Exception {
        // In the case of reentrancy, put things back the way they were
        IO toRestore = enter(output, error);
        try {
            return toRun.get();
        } finally {
            if (exit(toRestore)) {
                ioForThread.clear();
                original = null;
            }
        }
    }

    PrintStream stdout() {
        return io().stdout();
    }

    PrintStream stderr() {
        return io().stdout();
    }

    /**
     * Get the IO associated with the current thread, or the original on entry
     * if any, or if the stdout instance was somehow carried outside the closure
     * of the original run method, use whatever is now associated with that
     * thread, or the base instance from creation time, which will always be the
     * original stdout and stderr at that point in time.
     *
     * @return An IO
     */
    IO io() {
        IO result = ioForThread.get(Thread.currentThread());
        if (result == null) {
            result = original;
        }
        if (result == null) {
            result = base;
        }
        return result;
    }

    private synchronized IO enter(PrintStream output, PrintStream error) {
        // Reentrancy - there may be something to put back, so we'll return
        // that so it can be
        IO previous = ioForThread.get(Thread.currentThread());
        IO io = new IO(output, error);
        ioForThread.put(Thread.currentThread(), io);
        // The synchronized block and closure-based entry points ensure
        // entryCount will be consistent
        if (entryCount++ == 0) {
            init();
        }
        return previous;
    }

    /**
     * Restore the state for one thread, and restore stdio if we are the last
     * thread out.
     *
     * @param toRestore The io to put back
     * @return true if we deinitialized.
     */
    private synchronized boolean exit(IO toRestore) {
        if (toRestore != null) {
            ioForThread.put(Thread.currentThread(), toRestore);
        } else {
            ioForThread.remove(Thread.currentThread());
        }
        if (--entryCount == 0) {
            deinit();
            return true;
        }
        return false;
    }

    /**
     * Replace stdout / stderr.
     */
    private void init() {
        assert Thread.holdsLock(this);
        original = new IO();
        System.setOut(stdout);
        System.setErr(stderr);
    }

    /**
     * Restore stdout/stderr.
     */
    private void deinit() {
        assert Thread.holdsLock(this);
        if (original != null) {
            System.setOut(original.stdout);
            System.setErr(original.stderr);
            original = null;
        } else {
            System.setOut(base.stdout);
            System.setErr(base.stderr);
        }
    }

    private static final class IO {

        final PrintStream stdout;
        final PrintStream stderr;

        public IO(PrintStream stdout, PrintStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public IO() {
            this(System.out, System.err);
        }

        public PrintStream stdout() {
            return stdout;
        }

        public PrintStream stderr() {
            return stdout;
        }
    }

    static class Str extends PrintStream {

        private final Supplier<PrintStream> delegate;

        Str(Supplier<PrintStream> delegate) {
            super(Streams.nullOutputStream());
            this.delegate = delegate;
        }

        @Override
        public void flush() {
            delegate.get().flush();
        }

        @Override
        public void close() {
            delegate.get().close();
        }

        @Override
        public boolean checkError() {
            return delegate.get().checkError();
        }

        @Override
        public void write(int b) {
            delegate.get().write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            delegate.get().write(buf, off, len);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            delegate.get().write(buf);
        }

// JDK14        @Override
//        public void writeBytes(byte[] buf) {
//            delegate.get().writeBytes(buf);
//        }

        @Override
        public void print(boolean b) {
            delegate.get().print(b);
        }

        @Override
        public void print(char c) {
            delegate.get().print(c);
        }

        @Override
        public void print(int i) {
            delegate.get().print(i);
        }

        @Override
        public void print(long l) {
            delegate.get().print(l);
        }

        @Override
        public void print(float f) {
            delegate.get().print(f);
        }

        @Override
        public void print(double d) {
            delegate.get().print(d);
        }

        @Override
        public void print(char[] s) {
            delegate.get().print(s);
        }

        @Override
        public void print(String s) {
            delegate.get().print(s);
        }

        @Override
        public void print(Object obj) {
            delegate.get().print(obj);
        }

        @Override
        public void println() {
            delegate.get().println();
        }

        @Override
        public void println(boolean x) {
            delegate.get().println(x);
        }

        @Override
        public void println(char x) {
            delegate.get().println(x);
        }

        @Override
        public void println(int x) {
            delegate.get().println(x);
        }

        @Override
        public void println(long x) {
            delegate.get().println(x);
        }

        @Override
        public void println(float x) {
            delegate.get().println(x);
        }

        @Override
        public void println(double x) {
            delegate.get().println(x);
        }

        @Override
        public void println(char[] x) {
            delegate.get().println(x);
        }

        @Override
        public void println(String x) {
            delegate.get().println(x);
        }

        @Override
        public void println(Object x) {
            delegate.get().println(x);
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            return delegate.get().printf(format, args);
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            return delegate.get().printf(l, format, args);
        }

        @Override
        public PrintStream format(String format, Object... args) {
            return delegate.get().format(format, args);
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            return delegate.get().format(l, format, args);
        }

        @Override
        public PrintStream append(CharSequence csq) {
            return delegate.get().append(csq);
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            return delegate.get().append(csq, start, end);
        }

        @Override
        public PrintStream append(char c) {
            return delegate.get().append(c);
        }

    }
}
