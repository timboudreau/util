package com.mastfrog.bits.large;

import static com.mastfrog.bits.large.UnsafeUtils.UNSAFE;
import com.mastfrog.util.preconditions.Exceptions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
final class MappedFileLongArray implements CloseableLongArray {

    private static final int HEADER_LENGTH = 16;
    private long workingLength;
    private final Path file;
    private MappedByteBuffer buffer;
    private ChannelSupplier channel;

    MappedFileLongArray() {
        this(newTempFile(), 0L, false);
    }

    MappedFileLongArray(Path file) {
        this(file, 0L, true);
    }

    MappedFileLongArray(Path file, long size, boolean expectedToExist) {
        this.file = file;
        if (size < 0) {
            throw new IllegalArgumentException("Size is < 0");
        }
        if (expectedToExist) {
            this.channel = new LoadingChannelSupplier(size);
        } else {
            this.channel = new ClearingChannelSupplier(size);
        }
    }

    MappedFileLongArray(long size) {
        this(newTempFile(), size, false);
    }

    static Path newTempFile() {
        try {
            Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
            String proposal = "mapped-longs";
            int ix = 0;
            while (Files.exists(tmp.resolve(proposal))) {
                ix++;
                proposal = "mapped-longs-" + ix;
            }
            return Files.createFile(tmp.resolve(proposal));
        } catch (IOException ex) {
            return Exceptions.chuck(ex);
        }
    }

    @Override
    public boolean isZeroInitialized() {
        // This could return true on OS's other than SmartOS / Solaris?
        return false;
    }

    public Path file() {
        return file;
    }

    private static abstract class ChannelSupplier implements Supplier<FileChannel> {

        abstract void close() throws IOException;
    }

    private class LoadingChannelSupplier extends ChannelSupplier {

        private FileChannel channel;
        private final long targetSize;

        LoadingChannelSupplier(long targetSize) {
            this.targetSize = targetSize;
        }

        @Override
        public synchronized FileChannel get() {
            if (channel != null) {
                return channel;
            }
            return channel = init();
        }

        private FileChannel init() {
            try {
                channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING);
                if (channel.size() >= HEADER_LENGTH) {
                    ByteBuffer ver = ByteBuffer.allocate(Integer.BYTES);
                    channel.position(0);
                    channel.read(ver);
                    ver.flip();
                    if (ver.getInt() != 1) {
                        throw new IOException("Unrecognized version " + ver);
                    }
                    ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
                    channel.position(sizeOffset());
                    channel.read(buf);
                    buf.flip();
                    workingLength = buf.getLong();
                    if (workingLength < 0) {
                        throw new IOException("File is corrupt - reports length " + workingLength);
                    }
                    channel.position(0);
                } else {
//                    if (targetSize > 0) {
                    ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
                    buf.putInt(1);
                    buf.putInt(1);
                    buf.putLong(targetSize);
                    buf.flip();
                    channel.write(buf, 0);
                    channel.position(0);
                    workingLength = targetSize;
//                    }
                }
                return channel;
            } catch (IOException ex) {
                Logger.getLogger(MappedFileLongArray.class.getName()).log(Level.SEVERE, null, ex);
                return Exceptions.chuck(ex);
            }
        }

        @Override
        synchronized void close() throws IOException {
            if (channel != null) {
                channel.close();
            }
        }
    }

    private class ClearingChannelSupplier extends ChannelSupplier {

        private FileChannel channel;
        private final long targetSize;

        ClearingChannelSupplier(long targetSize) {
            this.targetSize = targetSize;
        }

        @Override
        public synchronized FileChannel get() {
            if (channel != null) {
                return channel;
            }
            return channel = init();
        }

        private FileChannel init() {
            try {
                channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING);
                ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
                buf.putInt(1);
                buf.putInt(1);
                buf.putLong(targetSize);
                buf.flip();
                workingLength = targetSize;
                channel.write(buf, 0);
                return channel;
            } catch (IOException ex) {
                Logger.getLogger(MappedFileLongArray.class.getName()).log(Level.SEVERE, null, ex);
                return Exceptions.chuck(ex);
            }
        }

        @Override
        synchronized void close() throws IOException {
            if (channel != null) {
                channel.close();
            }
        }
    }

    private FileChannel channel() {
        return channel.get();
    }

    private long fileLength(long targetSize) throws IOException {
        long targetSizeBytes = HEADER_LENGTH + (Long.BYTES * targetSize);
        long pageSize = UNSAFE.pageSize();
        if (targetSizeBytes % pageSize != 0) {
            long count = targetSizeBytes / pageSize;
            targetSizeBytes = Math.max(1, count) * pageSize;
        }
        FileChannel channel = channel();
        long size = channel.size();
        if (size > targetSizeBytes) {
            channel.truncate(targetSizeBytes);
            buffer = null;
        } else if (size < targetSizeBytes) {
            channel.write(ByteBuffer.allocate(1), targetSizeBytes - 1);
            buffer = null;
        }
        return targetSizeBytes;
    }

    private static <T> T wrap(Callable<T> wrapped) {
        try {
            return wrapped.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final MappedByteBuffer mapping() {
        if (buffer == null) {
            buffer = wrap(() -> {
                long len = fileLength((int) workingLength);
                return channel().map(FileChannel.MapMode.READ_WRITE, 0, len);
            });
        }
        return buffer;
    }

    public Object clone() {
        return wrap(() -> {
            MappedFileLongArray nue = new MappedFileLongArray();
            if (workingLength > 0) {
                nue.channel().transferFrom(channel(), 0, channel().size());
                nue.workingLength = workingLength;
            }
            return nue;
        });
    }

    private int sizeOffset() {
        return 8;
    }

    private int indexToFilePosition(int index) {
        return HEADER_LENGTH + (Long.BYTES * index);
    }

    synchronized void setWorkingLength(long len) {
        mapping().putLong(sizeOffset(), len);
        workingLength = len;
    }

    @Override
    public long size() {
        return workingLength;
    }

    @Override
    public long get(long index) {
        if (index > workingLength) {
            throw new IllegalStateException("Index " + index + " > " + workingLength);
        }
        return mapping().getLong(indexToFilePosition((int) index));
    }

    @Override
    public void set(long index, long value) {
        if (index > workingLength) {
            wrap(() -> {
                fileLength(index);
                workingLength = Math.max(workingLength, index);
                return null;
            });
        }
        mapping().putLong(indexToFilePosition((int) index), value);
    }

    @Override
    public void resize(long size) {
        wrap(() -> {
            fileLength(size);
            return null;
        });
    }

    @Override
    public void close() {
        buffer = null;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                Logger.getLogger(MappedFileLongArray.class.getName())
                        .log(Level.SEVERE, null, ex);
            } finally {
                channel = null;
            }
        }
    }

    public void destroy() throws Exception {
        close();
        if (Files.exists(file)) {
            Files.delete(file);
        }
    }

    @Override
    public void fill(long start, long length, long value) {
        if (length == 0) {
            return;
        }
        if (length == 1) {
            set(start, value);
            return;
        }
        assert start >= 0;
        if (start + length > workingLength) {
            wrap(() -> {
                fileLength(start + length);
                return null;
            });
        }
        if (length == 1) {
            mapping().putLong((int) start, value);
        } else {
            int startIndex = indexToFilePosition((int) start);
            ByteBuffer buf = ByteBuffer.allocateDirect(Long.BYTES);
            buf.putLong(value);
            buf.flip();
            ByteBuffer[] buffers = new ByteBuffer[(int) length];
            Arrays.fill(buffers, buf);
            wrap(() -> {
                FileChannel channel = channel();
                channel.position(startIndex);
                channel.write(buffers);
                channel.position(0);
                return null;
            });
        }
    }
}
