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
import com.mastfrog.util.preconditions.Checks;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstraction for appendable persistent storage of ad-hoc objects / records,
 * which can be read back sequentially with asynchronous commit semantics for
 * determining whether a record which was read has been fully processed and can
 * be discarded before processing the next one. Basically, abstracts a directory
 * full of spool files and facilities to manage them and read and write them
 * sequentially as Java objects.
 *
 * @author Tim Boudreau
 */
public interface LogStructuredStorage<T> {

    /**
     * Create a reader over this storage which allows an item to be read,
     * processed and then the reader advanced to the next item. Maintains a
     * cursor in an adjacent file, which survives process restarts.
     *
     * @return A reader
     * @throws IOException If something goes wrong
     */
    LogStructuredReader<T> reader() throws IOException;

    /**
     * Create a appender which can append new records (using the encoding of the
     * Serde passed when this storage was created).
     *
     * @return An apppender
     */
    LogStructuredAppender<T> appender();

    /**
     * Determine if all available records have been read.
     *
     * @return True if all existing records have been read.
     * @throws IOException If something goes wrong (file deleted, etc.)
     */
    boolean isFullyRead() throws IOException;

    /**
     * Delete all associated persistent files associated with this storage. May
     * return false if a writer is open over this storage.
     *
     * @return true if something was deleted
     * @throws IOException If something goes wrong
     */
    boolean delete() throws IOException;

    /**
     * Create a LogStructuredStorage over a single file and associated cursor
     * file, which can be appended to and independently read.
     *
     * @param <T> The type to serialize and deserialize
     * @param file The file to read and write - need not exist, but its parent
     * folder must
     * @param converter Serializes and deserializes data
     * @return A storage
     */
    static <T> LogStructuredStorage<T> create(Path file, Serde<T> converter, FileChannelPool pool) {
        return new LogStructuredFileImpl<>(file, converter, pool);
    }

    /**
     * Create a LogStructuredStorage over a folder, which will read and write
     * files within that folder; when a log file exceeds 1Mb, it will be closed
     * and a new one created. Records are read out in the order they were
     * appended, and the use of multiple files is transparent to the client.
     *
     * @param <T> The type to serialize and deserialize
     * @param dir The folder to place record files in
     * @param converter Serializes and deserializes data
     * @return A storage
     */
    static <T> LogStructuredStorage<T> forDir(Path dir, Serde<T> converter, FileChannelPool pool) {
        return new LogStructuredFileDirImpl<>("evs-", dir, converter, 1024 * 1024, pool);
    }

    /**
     * Create a LogStructuredStorage over a folder, which will read and write
     * files within that folder; when a log file exceeds 1Mb, it will be closed
     * and a new one created. Records are read out in the order they were
     * appended, and the use of multiple files is transparent to the client.
     *
     * @param <T> The type to serialize and deserialize
     * @param prefix The file prefix to use for file names - must not be null or
     * empty
     * @param dir The folder to place record files in
     * @param converter Serializes and deserializes data
     * @param suffix The file suffix (extensions should begin with a .
     * character) for record files
     * @param max The threshold size for creating a new log file
     * @return A storage
     */
    static <T> LogStructuredStorage<T> forDir(String prefix, Path dir, Serde<T> converter, String suffix, long max, FileChannelPool pool) {
        Checks.nonNegative("max", max);
        Checks.notNull("prefix", prefix);
        Checks.nonZero("prefix.length", prefix.length());
        Checks.notNull("suffix", suffix);
        Checks.nonZero("suffix.length", suffix.length());
        return new LogStructuredFileDirImpl<>(prefix, dir, converter, suffix, max, pool);
    }
}
