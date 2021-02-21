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
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.nio.file.WatchEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
final class Tail implements IOFunction<Predicate<CharSequence>, Runnable> {

    private final WatchManager mgr;
    private final Path path;
    private final int bufferSize;
    private final Charset charset;

    Tail(Path path, int bufferSize, Charset charset) throws IOException {
        this(WatchManager.sharedInstance(), path, bufferSize, charset);
    }

    Tail(WatchManager mgr, Path path, int bufferSize, Charset charset) {
        this.path = path;
        this.bufferSize = bufferSize;
        this.charset = charset;
        this.mgr = mgr;
    }

    /**
     * Watch a file, being passed a line of text whenever one is written, on a
     * background thread.
     *
     * @param lineConsumer A consumer which will receive lines
     * @return
     * @throws IOException
     */
    Runnable watch(Predicate<CharSequence> lineConsumer) throws IOException {
        ContinuousLineStream stream = ContinuousLineStream.of(path.toFile(), bufferSize, charset);
        C c = new C(path, stream, lineConsumer, mgr);
        mgr.watch(path, c, ENTRY_MODIFY, ENTRY_DELETE);
        return c;
    }

    @Override
    public Runnable apply(Predicate<CharSequence> in) throws IOException {
        return watch(in);
    }

    static class C implements BiConsumer<Path, WatchEvent.Kind<?>>, Runnable {

        private final Path path;
        private final ContinuousLineStream stream;
        private final Predicate<CharSequence> lineConsumer;
        private final WatchManager mgr;
        private final AtomicBoolean cancelled = new AtomicBoolean();

        public C(Path path, ContinuousLineStream stream, Predicate<CharSequence> lineConsumer, WatchManager mgr) {
            this.path = path;
            this.stream = stream;
            this.lineConsumer = lineConsumer;
            this.mgr = mgr;
        }

        @Override
        public void accept(Path t, WatchEvent.Kind<?> u) {
            if (!t.equals(path)) {
                new Exception(t + "").printStackTrace();
                return;
            }
            if (u == StandardWatchEventKinds.ENTRY_DELETE) {
                run();
                return;
            }
            try {
                while (stream.hasMoreLines() && !cancelled.get()) {
                    CharSequence line = stream.nextLine();
                    if (!lineConsumer.test(line)) {
                        run();
                        return;
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Tail.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            cancelled.set(true);
            mgr.unwatch(path, this);
        }
    }
}
