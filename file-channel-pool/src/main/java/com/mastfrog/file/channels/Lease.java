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
package com.mastfrog.file.channels;

import com.mastfrog.function.throwing.io.IOConsumer;
import com.mastfrog.function.throwing.io.IOFunction;
import com.mastfrog.function.throwing.io.IOSupplier;
import com.mastfrog.function.throwing.io.IOToIntFunction;
import com.mastfrog.function.throwing.io.IOToLongFunction;
import com.mastfrog.util.preconditions.Checks;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * A lease of a shared FileChannel with some read or write options, which
 * maintains position and state independent of the channel and resets the
 * channel to the appropriate state on use. Enables a file to be logically
 * opened for reading, writing or appending multiple times without the resource
 * consumption. Do not close the FileChannel instances obtained here - they are
 * managed by the FileChannelPool that created this lease.
 * <p>
 * Writes will acquire the NIO FileChannel's lock on the file, and if it is
 * already locked, will block until it is unlocked.
 * </p><p>
 * Do <b>not</b> allow FileChannel instances to escape the closure of the
 * callbacks passed to <code>use*()</code> methods, as the file channel is only
 * guaranteed to be in the expected state and not in use on another thread via
 * some other Lease instance during the period and on the thread that the
 * callback is executing within.
 * </p>
 *
 * @author Tim Boudreau
 */
public final class Lease {

    private final ChannelKey key;
    private final FileChannelPool pool;
    private long position = 0;

    Lease(ChannelKey key, FileChannelPool pool) {
        this.key = key;
        this.pool = pool;
    }

    /**
     * Create another lease with (potentially) different open options than this
     * one, in the same pool this one originated from.
     *
     * @param opts
     * @return
     * @throws IOException
     */
    public Lease forSameFile(StandardOpenOption... opts) throws IOException {
        return pool.lease(key.path, opts);
    }

    /**
     * Create another lease with (potentially) different open options or
     * attributes (for creation) than this one, in the same pool this one
     * originated from.
     *
     * @param opts
     * @param attrs
     * @return
     * @throws IOException
     */
    public Lease forSameFile(Set<StandardOpenOption> opts, FileAttribute... attrs) throws IOException {
        return pool.lease(key.path, opts, attrs);
    }

    /**
     * Determine if this lease offers read access.
     *
     * @return True if reading is possible
     */
    public boolean isRead() {
        return key.opts.contains(StandardOpenOption.READ);
    }

    /**
     * Determine if write access is possible.
     *
     * @return True if write access is possible
     */
    public boolean isWrite() {
        return key.isWrite();
    }

    /**
     * Get the file path for this lease.
     *
     * @return A path
     */
    public Path of() {
        return key.path;
    }

    /**
     * Get the pool that created this lease.
     *
     * @return The pool managing this lease
     */
    public FileChannelPool pool() {
        return pool;
    }

    /**
     * Delete the file associated with this lease. All open channels over this
     * file will be closed prior to deletion. <i>It is the caller's
     * responsibility to ensure nothing will try to use those channels
     * subsequently.</i>
     *
     * @throws IOException if deletion fails somehow
     */
    public void deleteFile() throws IOException {
        if (pool.deleteFile(key)) {
            synchronized (this) {
                position = 0;
            }
        }
    }

    @Override
    public String toString() {
        return key + "@" + position;
    }

    /**
     * Update the position of this lease; if it is possible that the underlying
     * file has been truncated, this ensures the position of this lease is no
     * greater than the end of the file (this is also done in calls to
     * <code>use*()</code>, so this method is only useful if you are using the
     * return value of position() somehow and need to ensure it is up-to-date).
     * This method need be used only if you expect the file could have been or
     * grown <i>externally</i> - i.e. not by another consumer of a lease in the
     * same pool.
     *
     * @return The position after calling this method
     * @throws IOException
     */
    public synchronized long syncPosition() throws IOException {
        FileChannel channel = pool.get(key);
        if (channel.isOpen()) {
            if (position > channel.size()) {
                position = channel.size();
            }
        } else if (Files.exists(key.path)) {
            long sz = pool.size(key);
            if (sz > position) {
                position = sz;
            }
        }
        return position;
    }

    /**
     * Get the position of the channel at the end of the last call to a
     * <code>use*()</code> method, or 0 if never used, or the position set in
     * the last call to <code>position(long)</code>. This method may be called
     * from any thread, and does not need to be called within the closure of a
     * callback passed to one of the <code>use*()</code> methods of this class.
     *
     * @return The position this lease will try to use on the next call to use
     */
    public synchronized long position() {
        return position;
    }

    /**
     * Set the position this lease will use on the next call to use(). If the
     * set position is past the end of the file, the position will be reset to
     * the end of the file on the next call to use().
     *
     * @param position A new position
     * @return this
     */
    public synchronized Lease position(long position) {
        Checks.nonNegative("position", position);
        this.position = position;
        return this;
    }

    /**
     * Get the size of the underlying file, using the channel for this lease if
     * one is open.
     *
     * @return The file size, if it exists
     * @throws IOException
     */
    public long size() throws IOException {
        return pool.size(key);
    }

    private FileLock acquireLock(FileChannel channel) throws IOException {
        // Acquires a lock on the underlying file, spinning until
        // it is available
        for (;;) {
            try {
                return channel.lock();
            } catch (OverlappingFileLockException ex) {
                try {
                    // ok
                    Thread.sleep(50);
                } catch (InterruptedException ex1) {
                    ex.addSuppressed(ex1);
                    throw ex;
                }
            }
        }
    }

    private void positionChannel(FileChannel ch) throws IOException {
        assert Thread.holdsLock(this);
        // A file that was previously truncated may end before
        // than the last position recorded by another lease.
        // It's this or track all the leases and update them, and
        // most will be defunct
        if (position > ch.size()) {
            position = ch.size();
        }
        ch.position(position);
    }

    /**
     * Use the file channel this lease wraps. The channel's position will be set
     * to the minimum of 1. Zero if this is the first use of this lease and it
     * is not in append mode, and no call to position(long) has been made 2. The
     * position on exit of the last call to use(), or on the last call to
     * position(long) if one has occurred subsequent to the last call to use(),
     * or 3. The end of the file
     *
     * @param c A callback to receive the callback
     * @throws IOException If something goes wrong
     */
    public synchronized void use(IOConsumer<FileChannel> c) throws IOException {
        pool.underLock(key, () -> {
            FileChannel channel = pool.get(key);
            long oldPosition = position;
            boolean thrown = false;
            FileLock lock = null;
            if (key.isWrite()) {
                lock = acquireLock(channel);
            }
            try {
                positionChannel(channel);
                try {
                    c.accept(channel);
                    pool.checkUnexpectedClose(key, this, channel, c);
                } catch (LeaseException de) {
                    de.setLease(this, oldPosition);
                    throw de;
                } catch (IOException ioe) {
                    thrown = true;
                    throw ioe;
                } finally {
                    if (thrown) {
                        if (channel.isOpen()) {
                            channel.position(oldPosition);
                        }
                    } else {
                        position = channel.position();
                    }
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

    /**
     * Use the file channel this lease wraps. The channel's position will be set
     * to the minimum of 1. Zero if this is the first use of this lease and it
     * is not in append mode, and no call to position(long) has been made 2. The
     * position on exit of the last call to use(), or on the last call to
     * position(long) if one has occurred subsequent to the last call to use(),
     * or 3. The end of the file
     *
     * @param c A callback to receive the callback
     * @return The output of the callback
     * @throws IOException If something goes wrong
     */
    public synchronized <T> T use(IOFunction<FileChannel, T> c) throws IOException {
        return pool.underLock(key, () -> {
            FileChannel channel = pool.get(key);
            FileLock lock = null;
            if (key.isWrite()) {
                lock = acquireLock(channel);
            }
            try {
                boolean thrown = false;
                long oldPosition = position;
                positionChannel(channel);
                try {
                    T result = c.apply(channel);
                    pool.checkUnexpectedClose(key, this, channel, c);
                    return result;
                } catch (LeaseException de) {
                    de.setLease(this, oldPosition);
                    throw de;
                } catch (IOException ioe) {
                    thrown = true;
                    throw ioe;
                } finally {
                    if (thrown) {
                        position = oldPosition;
                    } else {
                        if (channel.isOpen()) {
                            position = channel.position();
                        }
                    }
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

    /**
     * Makes a Lease usable by some stream writing methods in FileUtils.
     *
     * @param <T> The return type
     * @param c A function that takes the stream converted to a supplier
     * @throws IOException If something goes wrong
     */
    public void asSupplier(IOConsumer<IOSupplier<FileChannel>> c) throws IOException {
        use(channel -> {
            c.accept(() -> channel);
        });
    }

    /**
     * Makes a Lease usable by some stream writing methods in FileUtils.
     *
     * @param <T> The return type
     * @param c A function that takes the stream converted to a supplier
     * @return The result of applying the passed function
     * @throws IOException If something goes wrong
     */
    public <T> T asFunction(IOFunction<IOSupplier<FileChannel>, T> c) throws IOException {
        return use((IOFunction<FileChannel, T>) channel -> {
            return c.apply(() -> channel);
        });
    }

    /**
     * Use the file channel this lease wraps. The channel's position will be set
     * to the minimum of <ol><li>Zero if this is the first use of this lease and
     * it is not in append mode, and no call to position(long) has been
     * made</li><li>The position on exit of the last call to use(), or on the
     * last call to position(long) if one has occurred subsequent to the last
     * call to use(), or</li><li>The end of the file</li></ol>
     *
     * @param c A callback to receive the callback
     * @return The output of the callback
     * @throws IOException If something goes wrong
     */
    public synchronized long useAsLong(IOToLongFunction<FileChannel> c) throws IOException {
        return pool.underLock(key, () -> {
            FileChannel channel = pool.get(key);
            FileLock lock = null;
            if (key.isWrite()) {
                lock = acquireLock(channel);
            }
            try {
                boolean thrown = false;
                long oldPosition = position;
                positionChannel(channel);
                try {
                    long result = c.applyAsLong(channel);
                    pool.checkUnexpectedClose(key, this, channel, c);
                    return result;
                } catch (LeaseException de) {
                    de.setLease(this, oldPosition);
                    throw de;
                } catch (IOException ioe) {
                    thrown = true;
                    throw ioe;
                } finally {
                    if (thrown) {
                        position = oldPosition;
                    } else {
                        if (channel.isOpen()) {
                            position = channel.position();
                        }
                    }
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

    /**
     * Use the file channel this lease wraps. The channel's position will be set
     * to the minimum of <ol><li>Zero if this is the first use of this lease and
     * it is not in append mode, and no call to position(long) has been
     * made</li><li>The position on exit of the last call to use(), or on the
     * last call to position(long) if one has occurred subsequent to the last
     * call to use(), or</li><li>The end of the file</li></ol>
     *
     * @param c A callback to receive the callback
     * @return The output of the callback
     * @throws IOException If something goes wrong
     */
    public synchronized long useAsInt(IOToIntFunction<FileChannel> c) throws IOException {
        return pool.underLock(key, () -> {
            FileChannel channel = pool.get(key);
            FileLock lock = null;
            if (key.isWrite()) {
                lock = acquireLock(channel);
            }
            try {
                boolean thrown = false;
                long oldPosition = position;
                positionChannel(channel);
                try {
                    int result = c.applyAsInt(channel);
                    pool.checkUnexpectedClose(key, this, channel, c);
                    return result;
                } catch (LeaseException de) {
                    de.setLease(this, oldPosition);
                    throw de;
                } catch (IOException ioe) {
                    thrown = true;
                    throw ioe;
                } finally {
                    if (thrown) {
                        position = oldPosition;
                    } else {
                        if (channel.isOpen()) {
                            position = channel.position();
                        }
                    }
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        });
    }

}
