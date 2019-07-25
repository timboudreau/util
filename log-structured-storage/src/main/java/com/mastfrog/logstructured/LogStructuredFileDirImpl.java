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
package com.mastfrog.logstructured;

import com.mastfrog.file.channels.FileChannelPool;
import com.mastfrog.function.throwing.io.IOFunction;
import com.mastfrog.util.preconditions.Exceptions;
import com.mastfrog.util.strings.Strings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implementation of LogStructuredStorage over a folder of log structured files,
 * where a new log file is started when a threshold length is reached.
 *
 * @author Tim Boudreau
 */
final class LogStructuredFileDirImpl<T> implements LogStructuredStorage<T> {

    private final Pattern DIGITS = Pattern.compile("^\\d+$");
    static final String DEFAULT_SUFFIX = ".oplog";
    private final String filePrefix;
    private final Path dir;
    private final Serde<T> serde;
    private final String suffix;
    private final long maxFileSize;
    private final FileChannelPool pool;

    LogStructuredFileDirImpl(String filePrefix, Path dir, Serde<T> serde, long maxFileSize, FileChannelPool pool) {
        this(filePrefix, dir, serde, DEFAULT_SUFFIX, maxFileSize, pool);
    }

    LogStructuredFileDirImpl(String filePrefix, Path dir, Serde<T> serde, String suffix, long maxFileSize, FileChannelPool pool) {
        this.filePrefix = filePrefix;
        this.dir = dir;
        this.serde = serde;
        this.suffix = suffix;
        this.maxFileSize = maxFileSize;
        this.pool = pool;
    }

    boolean isLogFile(Path pth) {
        String nm = pth.getFileName().toString();
        boolean result = nm.length() > (filePrefix.length() + suffix.length())
                && nm.startsWith(filePrefix) && nm.endsWith(suffix);
        return result && DIGITS.matcher(middle(pth)).matches();
    }

    private String middle(Path pth) {
        String name = pth.getFileName().toString();
        return name.substring(filePrefix.length(), name.length() - (suffix.length()));
    }

    long numericPortion(Path path) {
        return Long.parseLong(middle(path));
    }

    private int compare(Path a, Path b) {
        long an = numericPortion(a);
        long bn = numericPortion(b);
        return Long.compare(an, bn);
    }

    private boolean isUnread(Path path) throws IOException {
        return !new LogStructuredFileImpl<>(path, serde, pool).isFullyRead();
    }

    private <F> F listDir(IOFunction<Stream<Path>, F> c) throws IOException {
        try (Stream<Path> str = Files.list(dir)) {
            return c.apply(str);
        }
    }

    private List<Path> allNotFullyReadFilesNewerThan(long timestamp) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        return listDir(str -> {
            List<Path> l = new ArrayList<>();
            str.filter(this::isLogFile)
                    .filter(path -> {
                        long val = numericPortion(path);
                        return val > timestamp;
                    })
                    .filter(pth -> {
                        try {
                            return isUnread(pth);
                        } catch (IOException ex) {
                            return Exceptions.chuck(ex);
                        }
                    })
                    .forEach(l::add);
            Collections.sort(l, this::compare);
            return l;
        });
    }

    List<Path> allFiles() throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        return listDir(str -> {
            List<Path> paths = new ArrayList<>();
            str.filter(this::isLogFile).forEach(paths::add);
            paths.sort(this::compare);
            return paths;
        });
    }

    private List<Path> allNotFullyReadFiles() throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        return listDir(str -> {
            List<Path> paths = new ArrayList<>();
            str.filter(this::isLogFile)
                    .filter(path -> {
                        try {
                            return isUnread(path);
                        } catch (IOException ex) {
                            return Exceptions.chuck(ex);
                        }
                    })
                    .forEach(paths::add);
            paths.sort(this::compare);
            return paths;
        });
    }

    @Override
    public boolean isFullyRead() throws IOException {
        return allNotFullyReadFiles().isEmpty();
    }

    @Override
    public LogStructuredReader<T> reader() {
        return new LSR();
    }

    @Override
    public LogStructuredAppender<T> appender() {
        return new LSW();
    }

    @Override
    public boolean delete() throws IOException {
        boolean result = false;
        for (Path p : allFiles()) {
            result |= new LogStructuredFileImpl<>(p, serde, pool).delete();
        }
        return result;
    }

    final class LSW implements LogStructuredAppender<T> {

        LogStructuredAppender<T> delegate;
        private Runnable onWrite = null;

        @Override
        public String toString() {
            return "LSW(" + delegate + " for " + dir + ")";
        }

        private synchronized LogStructuredAppender<T> delegate() throws IOException {
            if (delegate == null) {
                String now = Strings.zeroPrefix(System.currentTimeMillis(), 19); // digits required for Long.MAX_VALUE
                String name = filePrefix + now + suffix;
                Path path = dir.resolve(name);
                if (!Files.exists(dir)) {
                    FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE));
                    Files.createDirectories(dir, attr);
                }
                delegate = new LogStructuredAppenderImpl<>(path, serde, pool);
                if (onWrite != null) {
                    delegate.onWrite(onWrite);
                }
            }
            return delegate;
        }

        private synchronized void onAfterWrite() throws IOException {
            if (delegate != null && delegate.size() >= maxFileSize) {
                delegate.close();
                delegate = null;
            }
        }

        @Override
        public synchronized T append(T env) throws IOException {
            T result = delegate().append(env);
            onAfterWrite();
            return result;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public synchronized long size() throws IOException {
            return delegate == null ? 0 : delegate.size();
        }

        @Override
        public synchronized void close() throws IOException {
            if (delegate != null) {
                delegate.close();
                delegate = null;
            }
        }

        @SuppressWarnings(value = "ThrowFromFinallyBlock")
        @Override
        public void onWrite(Runnable onWrite) {
            Runnable old = this.onWrite;
            if (old != null) {
                this.onWrite = () -> {
                    Throwable t = null;
                    try {
                        old.run();
                    } catch (RuntimeException | Error e) {
                        t = e;
                    } finally {
                        try {
                            onWrite.run();
                        } catch (RuntimeException | Error e1) {
                            if (t == null) {
                                t = e1;
                            } else {
                                t.addSuppressed(e1);
                            }
                        } finally {
                            if (t != null) {
                                if (t instanceof RuntimeException) {
                                    throw (RuntimeException) t;
                                } else {
                                    throw (Error) t;
                                }
                            }
                        }
                    }
                };
            } else {
                this.onWrite = onWrite;
            }
            synchronized (this) {
                if (delegate != null) {
                    delegate.onWrite(this.onWrite);
                }
            }
        }

    }

    final class LSR implements LogStructuredReader<T> {

        LogStructuredReader<T> delegate;
        private volatile boolean anyRead;

        @Override
        public String toString() {
            return "LSR(" + delegate + " for " + dir + ")";
        }

        private synchronized LogStructuredReader<T> delegate() throws IOException {
            if (delegate != null && !(delegate instanceof EmptyReader<?>)) {
                if (!delegate.hasUnread()) {
                    delegate.close();
                    delegate = null;
                }
            }
            if (delegate == null) {
                List<Path> all = allNotFullyReadFiles();
                if (all.isEmpty()) {
                    return new EmptyReader<>();
                }
                Path p = all.get(0);
                delegate = new LogStructuredFileImpl<>(p, serde, pool).reader();
            }
            return delegate;
        }

        @Override
        public UnadvancedRead<T> read() throws IOException {
            anyRead = true;
            return delegate().read();
        }

        @Override
        public T readAndAdvance() throws IOException {
            anyRead = true;
            return delegate().readAndAdvance();
        }

        @Override
        public boolean readAndAdvance(Consumer<T> consumer) throws IOException {
            anyRead = true;
            return delegate().readAndAdvance(consumer);
        }

        @Override
        public synchronized void close() throws IOException {
            if (delegate != null) {
                if (anyRead) {
                    delegate.deleteIfAllReadAndAdvanced();
//                    delegate.discardCommitted();
                }
                delegate.close();
                delegate = null;
            }
        }

        @Override
        public void deleteIfAllReadAndAdvanced() throws IOException {
            for (Path p : allFiles()) {
                LogStructuredFileImpl<T> f = new LogStructuredFileImpl<>(p, serde, pool);
                if (f.isFullyRead()) {
                    f.delete();
                }
            }
        }

        @Override
        public synchronized void discardCommitted() throws IOException {
            if (delegate != null) {
                delegate.discardCommitted();
            }
        }

        @Override
        public boolean hasUnread() throws IOException {
            return delegate().hasUnread();
        }

        @Override
        public void rewind() throws IOException {
            delegate().rewind();
        }
    }

    static final class EmptyReader<T> implements LogStructuredReader<T> {

        @Override
        public void close() throws IOException {
            // do nothing
        }

        @Override
        public void deleteIfAllReadAndAdvanced() throws IOException {
            // do nothing
        }

        @Override
        public void discardCommitted() throws IOException {
            // do nothing
        }

        @Override
        public boolean hasUnread() throws IOException {
            return false;
        }

        @Override
        public UnadvancedRead<T> read() throws IOException {
            return null;
        }

        @Override
        public T readAndAdvance() throws IOException {
            return null;
        }

        @Override
        public boolean readAndAdvance(Consumer<T> consumer) throws IOException {
            return false;
        }

        @Override
        public void rewind() throws IOException {
            // do nothing
        }
    }
}
