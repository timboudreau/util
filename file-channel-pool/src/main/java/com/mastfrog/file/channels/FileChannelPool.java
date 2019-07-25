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
package com.mastfrog.file.channels;

import com.mastfrog.function.throwing.io.IOIntSupplier;
import com.mastfrog.function.throwing.io.IOLongSupplier;
import com.mastfrog.function.throwing.io.IORunnable;
import com.mastfrog.function.throwing.io.IOSupplier;
import com.mastfrog.util.cache.TimedCache;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.preconditions.Checks;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Avoids the dreaded "too many open files" exception by pooling file channels
 * in a cache which will close unused ones after a timeout, and providing
 * exclusive access to those channels. Obtain a lease for a channel with
 * particular read/write/attributes characteristics, use its
 * <code>useAsLong()</code> methods to read or write, but do not let the channel
 * passed to your callback leak beyond the scope of that calback. Each lease
 * maintains the position and resets it on the channel prior to invoking the
 * callback, simulating multiple open file channels without the OS-level
 * overhead (at the price of disallowing concurrent access).
 *
 * @author Tim Boudreau
 */
public final class FileChannelPool implements AutoCloseable {

    private static final FileAttribute[] NO_ATTRS = new FileAttribute[0];
    private static final Map<String, Integer> DEBUG_OPEN_COUNT;
    private static final boolean DEBUG;
    private static FileChannelPool DEFAULT_POOL;
    private final TimedCache<ChannelKey, FileChannel, IOException> channels;
    private final Map<Path, Set<ChannelKey>> keysForPath
            = CollectionUtils.concurrentSupplierMap(HashSet::new);

    static {
        DEBUG = Boolean.getBoolean("mastfrog.file.channel.debug");
        if (DEBUG) {
            DEBUG_OPEN_COUNT = CollectionUtils.concurrentSupplierMap(() -> Integer.valueOf(0));
        } else {
            DEBUG_OPEN_COUNT = null;
        }
    }

    private static FileChannel open(Path path, Set<? extends OpenOption> opts, FileAttribute... attrs) throws IOException {
        log(path, opts);
        return FileChannel.open(path, opts, attrs);
    }

    /**
     * Create a new FileChannelPool.
     *
     * @param channelCloseTimeout The timeout after which, if the associated
     * channel has not been used, the channel should be closed and removed from
     * the pool.
     *
     * @return A pool
     */
    public static FileChannelPool newPool(Duration channelCloseTimeout) {
        return newPool(channelCloseTimeout.toMillis());
    }

    /**
     * Create a new FileChannelPool.
     *
     * @param channelCloseTimeout The timeout after which, if the associated
     * channel has not been used, the channel should be closed and removed from
     * the pool.
     *
     * @return A pool
     */
    public static FileChannelPool newPool(long channelCloseTimeout) {
        Checks.greaterThanZero("channelCloseTimeout", channelCloseTimeout);
        return new FileChannelPool(channelCloseTimeout);
    }

    private static void log(Path path, Set<? extends OpenOption> opts) {
        if (!DEBUG) {
            return;
        }
        boolean read = opts.contains(StandardOpenOption.READ);
        boolean write = opts.contains(StandardOpenOption.WRITE) || opts.contains(StandardOpenOption.APPEND);
        String type = read && write ? "read/write" : read ? "read" : write ? "write" : "?";
        String s = type + ":" + path.toString();
        int val = DEBUG_OPEN_COUNT.get(s);
        DEBUG_OPEN_COUNT.put(s, val + 1);
        if (val > 3) {
            new Exception("Opened " + path.getFileName() + " " + val + " times for " + type).printStackTrace();
        }
        System.out.println("OPEN " + path.getFileName() + " " + type);
    }

    /**
     * Get the default global FileChannelPool with a one-minute timeout on
     * unused. Note that the default pool may not be closed and will throw an
     * IOException if that is attempted.
     *
     * @return The global FileChannelPool.
     */
    public static final synchronized FileChannelPool defaultPool() {
        if (DEFAULT_POOL == null) {
            DEFAULT_POOL = new FileChannelPool(60000);
        }
        return DEFAULT_POOL;
    }

    private FileChannelPool(long leaseTimeout) {
        channels = TimedCache.<ChannelKey, FileChannel, IOException>createThrowing(
                leaseTimeout, this::newChannelFor).onExpire(this::expire);
    }

    private void expire(ChannelKey key, FileChannel channel) {
        if (channel.isOpen()) {
            try {
                if (DEBUG) {
                    System.out.println("Close channel for " + key);
                }
                channel.close();
            } catch (IOException ioe) {
                Logger.getLogger(FileChannelPool.class.getName()).log(Level.SEVERE, null, ioe);
            }
        }
    }

    long size(ChannelKey key) throws IOException {
        return size(key.path);
    }

    private long size(Path path) throws IOException {
        for (ChannelKey ck : keysForPath.get(path)) {
            Optional<FileChannel> oc = channels.cachedValue(ck);
            if (oc.isPresent()) {
                FileChannel ch = oc.get();
                if (ch.isOpen()) {
                    try {
                        return ch.size();
                    } catch (IOException ioe) {
                        // could be closed between our open check and the call to size()
                        Logger.getLogger(FileChannelPool.class.getName()).log(Level.INFO, null, ioe);
                    }
                }
            }
        }
        return Files.size(path);
    }

    private FileChannel newChannelFor(ChannelKey key) throws IOException {
        if (key.randomAccess) {
            String args = toRandomAccessFileArgs(key.opts);
            RandomAccessFile file = new RandomAccessFile(key.path.toFile(), args);
            FileChannel result = file.getChannel();
            if (key.opts.contains(StandardOpenOption.APPEND)) {
                result.position(file.length());
            }
            return result;
        }
        return open(key.path, key.opts, key.attrs);
    }

    private String toRandomAccessFileArgs(Set<StandardOpenOption> opts) {
        StringBuilder sb = new StringBuilder(3);
        if (opts.contains(StandardOpenOption.READ)) {
            sb.append("r");
        }
        if (opts.contains(StandardOpenOption.WRITE)) {
            sb.append("w");
        }
        if (opts.contains(StandardOpenOption.SYNC)) {
            sb.append("s");
        }
        if (opts.contains(StandardOpenOption.DSYNC)) {
            sb.append("d");
        }
        return sb.toString();
    }

    synchronized FileChannel get(ChannelKey key) throws IOException {
        FileChannel ch = channels.get(key);
        if (!ch.isOpen()) {
            channels.remove(key);
            ch = channels.get(key);
        }
        return new FileChannelWrapper(ch);
    }

    @SuppressWarnings("ManualArrayToCollectionCopy") // no need to create and throw away an intermediate List
    private static EnumSet<StandardOpenOption> toSet(StandardOpenOption... opts) {
        EnumSet<StandardOpenOption> set = EnumSet.noneOf(StandardOpenOption.class);
        for (StandardOpenOption s : opts) {
            set.add(s);
        }
        return set;
    }

    private Lease randomAccessLease(Path path, StandardOpenOption... opts) throws IOException {
        return randomAccessLease(path, toSet(opts), NO_ATTRS);
    }

    private Lease randomAccessLease(Path path, Set<StandardOpenOption> openOptions, FileAttribute... attrs) throws IOException {
        ChannelKey ck = new ChannelKey(path, openOptions, true, attrs);
        keysForPath.get(path).add(ck);
        Lease lease = new Lease(ck, this);
        if (ck.isAppend() && Files.exists(path)) {
            lease.position(Files.size(path));
        }
        return lease;
    }

    /**
     * Obtain a lease for a given file with the given set of options. Multiple
     * leases may be obtained over the same file channel with different options.
     * Where the options are mutually compatible, they may share an underlying
     * <code>FileChannel</code>, but the position of the channel will be set to
     * that of the lease before it is provided to a callback using the lease.
     * <p>
     * The lease's <code>use*()</code> methods take a callback which is passed a
     * <code>FileChannel</code> (which may be shared with other leases but will
     * not be used by them concurrently).
     * </p>
     *
     * @param path The file patrh
     * @param openOptions The open options
     * @return A lease
     * @throws IOException If the file cannot be opened with the requested
     * options
     * @throws IllegalArgumentException if mutually incompatible options are
     * requested (such as TRUNCATE_EXISTING + APPEND).
     */
    public Lease lease(Path path, StandardOpenOption... openOptions) throws IOException {
        Set<StandardOpenOption> optionSet = toSet(openOptions);
        if (optionSet.contains(StandardOpenOption.TRUNCATE_EXISTING) && optionSet.contains(StandardOpenOption.APPEND)) {
            throw new IllegalArgumentException("Contradictory open options TRUNCATE_EXISTING and APPEND cannot be combined");
        }
        // NIO does NOT allow combining read access and append, while RandomAccessFile
        // does
        if (optionSet.contains(StandardOpenOption.READ) && optionSet.contains(StandardOpenOption.APPEND)) {
            return randomAccessLease(path, openOptions);
        }
        return lease(path, toSet(openOptions), NO_ATTRS);
    }

    /**
     * Obtain a lease for a given file with the given set of options. Multiple
     * leases may be obtained over the same file channel with different options.
     * Where the options are mutually compatible, they may share an underlying
     * <code>FileChannel</code>, but the position of the channel will be set to
     * that of the lease before it is provided to a callback using the lease.
     * <p>
     * The lease's <code>use*()</code> methods take a callback which is passed a
     * <code>FileChannel</code> (which may be shared with other leases but will
     * not be used by them concurrently).
     * </p>
     *
     * @param path The file patrh
     * @param openOptions The open options
     * @param attrs File attributes (only relevant if the file is being created)
     * @return A lease
     * @throws IOException If the file cannot be opened with the requested
     * options
     * @throws IllegalArgumentException if mutually incompatible options are
     * requested (such as TRUNCATE_EXISTING + APPEND).
     */
    public Lease lease(Path path, Set<StandardOpenOption> openOptions, FileAttribute... attrs) throws IOException {
        ChannelKey ck = new ChannelKey(path, openOptions, false, attrs);
        keysForPath.get(path).add(ck);
        Lease lease = new Lease(ck, this);
        if (ck.isAppend() && Files.exists(path)) {
            lease.position(Files.size(path));
        }
        return lease;
    }

    /**
     * Close this file channel pool, closing all channels owned by it. Close
     * operations will attempt to block until any callback actively using the
     * channel has exited, by first obtaining the channel's lock.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (this == DEFAULT_POOL) {
            throw new IOException("Cannot close the global pool");
        }
        channels.close();
    }

    void checkUnexpectedClose(ChannelKey key, Lease aThis, FileChannel channel, Object consumer) {
        if (!channel.isOpen()) {
            String msg = "FileChannel for " + key + " belonging to " + this
                    + " unexpectedly closed by "
                    + consumer + " - channels belonging to a FileChannelPool are"
                    + " managed by it, and should not be directly closed by"
                    + " code that uses the pool.";
            new IOException(msg).printStackTrace();
        }
    }

    synchronized boolean deleteFile(ChannelKey key) throws IOException {
        boolean result = deleteFile(key.path);
        if (result) {
            readLocks.remove(key);
        }
        return result;
    }

    /**
     * Delete a file, ensuring any open channels are closed and no longer
     * referenced.
     *
     * @param path The file path
     * @return True if the file exists and was deleted
     * @throws IOException If deletion fails for some reason
     */
    public synchronized boolean deleteFile(Path path) throws IOException {
        if (Files.exists(path)) {
            Set<ChannelKey> keys = keysForPath.get(path);
            try {
                for (ChannelKey k : keys) {
                    Optional<FileChannel> optCh = channels.getOptional(k);
                    if (optCh.isPresent()) {
                        FileChannel ch = optCh.get();
                        if (ch.isOpen()) {
                            ch.close();
                        }
                    }
                }
                for (ChannelKey k : keys) {
                    channels.remove(k);
                    readLocks.remove(k);
                }
            } finally {
                Files.delete(path);
                writeLocks.remove(path);
            }
            return true;
        }
        return false;
    }

    private final Map<ChannelKey, ReentrantLock> readLocks
            = Collections.synchronizedMap(CollectionUtils.supplierMap(ReentrantLock::new));
    private final Map<Path, ReentrantLock> writeLocks
            = Collections.synchronizedMap(CollectionUtils.supplierMap(ReentrantLock::new));

    private ReentrantLock lockFor(ChannelKey key) {
        // The purpose of the lock is to ensure two threads are not
        // repositioning the same *channel* at the same time, so it
        // is not necessary to block writes for reads.
        if (key.isAppend() || key.isWrite()) {
            return writeLocks.get(key.path);
        } else {
            return readLocks.get(key);
        }
    }

    void underLock(ChannelKey key, IORunnable r) throws IOException {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            r.run();
        } finally {
            lock.unlock();
        }
    }

    <R> R underLock(ChannelKey key, IOSupplier<R> r) throws IOException {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            return r.get();
        } finally {
            lock.unlock();
        }
    }

    long underLock(ChannelKey key, IOLongSupplier r) throws IOException {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            return r.getAsLong();
        } finally {
            lock.unlock();
        }
    }

    int underLock(ChannelKey key, IOIntSupplier r) throws IOException {
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
            return r.getAsInt();
        } finally {
            lock.unlock();
        }
    }
}
