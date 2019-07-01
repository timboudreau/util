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
package com.mastfrog.util.file;

import com.mastfrog.function.throwing.io.IOFunction;
import com.mastfrog.util.streams.ContinuousLineStream;
import java.io.IOException;
import java.nio.charset.Charset;
import static java.nio.charset.CoderResult.OVERFLOW;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
final class Tail implements IOFunction<Predicate<CharSequence>, Runnable> {

    private final Executor exec;
    private final Path path;
    private final int bufferSize;
    private final Charset charset;

    Tail(Executor exec, Path path, int bufferSize, Charset charset) throws IOException {
        this.exec = exec;
        this.path = path;
        this.bufferSize = bufferSize;
        this.charset = charset;
    }

    /**
     * Watch a file, being passed a line of text whenever one is written, on a
     * background thread.
     *
     * @param lineConsumer A consumer which will receive lines
     * @return
     * @throws IOException
     */
    public Runnable watch(Predicate<CharSequence> lineConsumer) throws IOException {
        WatchService watch = path.getFileSystem().newWatchService();
        WatchKey key = path.getParent().register(watch, ENTRY_MODIFY);
        ContinuousLineStream stream = ContinuousLineStream.of(path.toFile(), bufferSize, charset);
        Canceller cancel = new Canceller(key);
        exec.execute(() -> {
            try {
                watch(cancel, watch, key, stream, lineConsumer);
            } catch (InterruptedException ex) {
                // do nothing
            } catch (IOException ex) {
                Logger.getLogger(Tail.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return cancel;
    }

    private void watch(Canceller canceller, WatchService watch, WatchKey key, ContinuousLineStream stream, Predicate<CharSequence> lineConsumer) throws InterruptedException, IOException {
        canceller.setThread();
        try {
            int tightLoopCount = 0;
            long lastLoop = System.currentTimeMillis();
            for (long polls = 0;; polls++) {
                if (canceller.isCancelled()) {
                    return;
                }
                while (stream.hasMoreLines()) {
                    if (!lineConsumer.test(stream.nextLine())) {
                        return;
                    }
                    if (canceller.isCancelled()) {
                        return;
                    }
                }
                if (canceller.isCancelled()) {
                    return;
                }

                // Handle the case that we are starving the
                // system of threads because we are being handed the
                // same key over and over - this happens and is OS and
                // environment dependent
                long now = System.currentTimeMillis();
                long elapsed = now - lastLoop;
                if (elapsed <= 1) {
                    tightLoopCount++;
                    if (tightLoopCount > 15) {
                        tightLoopCount = 0;
                        Thread.sleep(10);
                    }
                } else {
                    tightLoopCount = 0;
                }
                lastLoop = now;

                WatchKey k = watch.take();
                // If this is not called, resetting the watch does
                // nothing
                k.pollEvents();
                // This is unreliable, at least in tests:
                boolean foundOurFile = false;
                for (WatchEvent<?> e : k.pollEvents()) {
                    if (e.kind() == ENTRY_MODIFY) {
                        Path p = (Path) e.context();
                        if (path.equals(p) || path.getFileName().equals(p)) {
                            foundOurFile = true;
                            break;
                        }
                    } else if (e.kind() == OVERFLOW) {
                        foundOurFile = true; // maybe, maybe not.
                        break;
                    }
                }
                if (!foundOurFile) {
                    k.reset();
                    continue;
                }

                // If the operating system's watch facility is badly behaved,
                // we can tie up a ton of CPU time looping at high speed here
                // (for example, causing tests to lock up the desktop during
                // development).  Thread.yield() is not guaranteed to do anything
                // but currently does help the test's timeout thread have a
                // chance to detect a runaway test.
                Thread.yield();
//                System.out.println("poll " + polls);
                if (canceller.isCancelled()) {
                    return;
                }
                while (stream.hasMoreLines()) {
                    if (canceller.isCancelled()) {
                        return;
                    }
                    if (!lineConsumer.test(stream.nextLine())) {
                        return;
                    }
                }
                boolean valid = k.reset();
                if (!valid) {
                    break;
                }
            }
        } finally {
            canceller.setCancelled();
            try {
                key.cancel();
            } finally {
                try {
                    watch.close();
                } finally {
                    stream.close();
                }
            }
        }
    }

    @Override
    public Runnable apply(Predicate<CharSequence> a) throws IOException {
        return watch(a);
    }

    private static class Canceller implements Runnable {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private Thread thread;

        private final WatchKey key;

        Canceller(WatchKey key) {
            this.key = key;
        }

        void setCancelled() {
            cancelled.set(true);
            key.cancel();
        }

        boolean isCancelled() {
            return cancelled.get() || !key.isValid();
        }

        void cancel() {
            Thread t;
            synchronized (this) {
                t = thread;
            }
            if (cancelled.compareAndSet(false, true)) {
                if (t != null) {
                    t.interrupt();
                }
                key.cancel();
            }
        }

        synchronized void setThread() {
            if (cancelled.get()) {
                Thread.currentThread().interrupt();
                return;
            }
            thread = Thread.currentThread();
        }

        @Override
        public void run() {
            cancel();
        }
    }
}
