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
import com.mastfrog.util.collections.CollectionUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
class LogStructuredFileImpl<T> implements LogStructuredStorage<T> {

    private final Path path;
    private static final String CURSOR_EXT = ".cursor";
    private final Serde<T> serde;
    private static final Map<Path, Set<LogStructuredAppenderImpl<?>>> OPEN_WRITERS
            = Collections.synchronizedMap(CollectionUtils.supplierMap(CollectionUtils::weakSet));
    private final FileChannelPool pool;

    LogStructuredFileImpl(Path path, Serde<T> serde, FileChannelPool pool) {
        this.path = path;
        this.serde = serde;
        this.pool = pool;
    }

    Path cursorFile() {
        return path.getParent().resolve(Paths.get(path.getFileName().toString() + CURSOR_EXT));
    }

    @Override
    public boolean delete() throws IOException {
        boolean result = true;
        Set<LogStructuredAppenderImpl<?>> live = OPEN_WRITERS.get(path);
        for (LogStructuredAppenderImpl<?> l : live) {
            if (l.isOpen()) {
                result = false;
                break;
            }
        }
        if (result) {
            result &= pool.deleteFile(path);
            result |= pool.deleteFile(cursorFile());
        }
        return result;
    }

    @Override
    public boolean isFullyRead() throws IOException {
        if (!Files.exists(path)) {
            return true;
        }
        CursorFile c = new CursorFile(cursorFile(), pool);
        return c.read() >= Files.size(path);
    }

    @Override
    public LogStructuredAppenderImpl<T> appender() {
        return new LogStructuredAppenderImpl<>(path, serde, pool);
    }

    @Override
    public LogStructuredReaderImpl<T> reader() {
        return new LogStructuredReaderImpl<>(path, cursorFile(), serde, pool);
    }
}
