package com.mastfrog.util.streams;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A file channel over a byte array.
 *
 * @author Tim Boudreau
 */
final class ByteArrayChannel implements GeneralByteChannel {

    private byte[] bytes;
    private int cursor;
    private boolean closed;

    ByteArrayChannel(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] toByteArray() {
        if (cursor == bytes.length) {
            return bytes;
        }
        return Arrays.copyOf(bytes, cursor);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int rem = dst.remaining();
        if (rem == 0) {
            return 0;
        }
        int max = Math.min(bytes.length - cursor, rem);
        if (max <= 0) {
            return 0;
        }
        dst.put(bytes, cursor, max);
        cursor += max;
        return max;
    }

    public int reset() {
        int oldCursor = cursor;
        closed = false;
        cursor = 0;
        return oldCursor;
    }

    @Override
    public boolean isOpen() {
        return !closed && cursor < bytes.length;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int amt = src.remaining();
        if (amt > 0) {
            if (bytes.length - cursor < amt) {
                bytes = Arrays.copyOf(bytes, cursor + amt);
            }
            src.get(bytes, cursor, amt);
            cursor += amt;
            return amt;
        }
        return 0;
    }

    @Override
    public long position() throws IOException {
        return cursor;
    }

    @Override
    public int cursorPosition() {
        return cursor;
    }

    @Override
    public ByteArrayChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IOException("Position < 0: " + newPosition);
        }
        if (newPosition > bytes.length) {
            throw new IOException("Position past end: " + newPosition
                    + " of " + bytes.length);
        }
        cursor = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        return bytes.length;
    }

    @Override
    public ByteArrayChannel truncate(long size) throws IOException {
        if (size > bytes.length) {
            return this;
        }
        bytes = Arrays.copyOf(bytes, (int) size);
        return this;
    }
}
