/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to useAsLong, copy, modify, merge, publish, distribute, sublicense, and/or sell
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
import com.mastfrog.file.channels.Lease;
import com.mastfrog.util.preconditions.Checks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class LogStructuredAppenderImpl<T> implements LogStructuredAppender<T> {

    static final FileAttribute<Set<PosixFilePermission>> ATTRS = PosixFilePermissions.asFileAttribute(
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    private static final Set<StandardOpenOption> WRITE_OPTIONS
            = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

    private final Path path;
    private Lease appendLease;
    private Runnable onWrite;
    private final Serde<T> serde;
    private final FileChannelPool pool;

    LogStructuredAppenderImpl(Path path, Serde<T> serde, FileChannelPool pool) {
        this.path = path;
        this.serde = serde;
        this.pool = pool;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @Override
    public synchronized boolean isOpen() {
//        return channel != null && channel.isOpen();
        return appendLease != null && Files.exists(path);
    }

    @SuppressWarnings(value = "ThrowFromFinallyBlock")
    @Override
    public void onWrite(Runnable onWrite) {
        Runnable old = this.onWrite;
        if (old != null && !old.equals(onWrite)) {
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
    }

    private Lease channel() throws IOException {
        assert Thread.holdsLock(this);
        if (appendLease == null) {
            appendLease = pool.lease(path, WRITE_OPTIONS, ATTRS);

        }
        return appendLease;
    }

    @Override
    public synchronized long size() throws IOException {
        if (appendLease != null) {
            return appendLease.size();
        }
        return Files.exists(path) ? Files.size(path) : -1;
    }

    @Override
    public synchronized T append(T env) throws IOException {
        Checks.notNull("env", env);
        channel().use(writeChannel -> {
            serde.serialize(env, writeChannel);
            if (onWrite != null) {
                onWrite.run();
            }
        });
        return env;
    }

    @Override
    public synchronized void close() throws IOException {
        if (appendLease != null && Files.exists(path)) {
            appendLease.use(ch -> {
                ch.force(true);
            });
            appendLease = null;
        } else if (appendLease != null) {
            appendLease = null;
        }
    }
}
