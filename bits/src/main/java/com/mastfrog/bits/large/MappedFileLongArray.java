package com.mastfrog.bits.large;

import static com.mastfrog.bits.large.UnsafeUtils.UNSAFE;
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

    MappedFileLongArray() {
        this(newTempFile(), 0L);
    }

    MappedFileLongArray(Path file) {
        this(file, 0L);
    }

    MappedFileLongArray(Path file, long size) {
        this.file = file;
        if (Files.exists(file)) {
            init();
        } else if (size > 0) {
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 2);
            buf.putInt(1);
            buf.putInt(1);
            buf.putLong(size);
            buf.flip();
            workingLength = size;
            wrap(() -> {
                channel().write(buf);
                channel().position(workingLength - 1);
                channel.write(ByteBuffer.allocate(1));
                return null;
            });
        } else if (size < 0) {
            throw new IllegalArgumentException("Negative size " + size);
        }
    }

    MappedFileLongArray(long size) {
        this(newTempFile(), size);
    }

    public Path file() {
        return file;
    }

    private void init() {
        wrap(() -> {
            FileChannel channel = channel();
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
            channel.position(0);
            return null;
        });
    }

    private static Path newTempFile() {
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        int ix = 0;
        String now = Long.toString(System.currentTimeMillis(), 36);
        Path result = tmp.resolve(now + ".longs");
        while (Files.exists(result)) {
            result = tmp.resolve(now + "-" + ix++ + ".longs");
        }
        return result;
    }

    private MappedByteBuffer buffer;
    private FileChannel channel;

    private FileChannel channel() {
        if (channel == null) {
            try {
                boolean existed = Files.exists(file);
                channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
                if (!existed) {
                    ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
                    buf.putInt(1);
                    buf.putInt(1);
                    buf.putLong(0);
                    buf.flip();
                    channel.write(buf, 0);
                }
            } catch (IOException ex) {
                Logger.getLogger(MappedFileLongArray.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return channel;
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
