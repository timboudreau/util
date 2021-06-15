package com.mastfrog.util.streams;

import com.mastfrog.function.state.Obj;
import com.mastfrog.util.file.FileUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * An InputStream-like construct which does not acknowledge the end of files.
 */
public final class ContinuousStringStream implements CharSequenceSource {

    private final FileChannel fileChannel;
    private final ByteBuffer readBuffer;

    public ContinuousStringStream(FileChannel fileChannel, int readBufferSizeInBytes) {
        this.fileChannel = fileChannel;
        readBuffer = ByteBuffer.allocateDirect(readBufferSizeInBytes);
    }

    public boolean isOpen() {
        return fileChannel.isOpen();
    }

    int bufferSize() {
        return readBuffer.capacity();
    }

    /**
     * Get the postion the next read will come from
     *
     * @return The position in the file
     * @throws IOException
     */
    @Override
    public synchronized long position() throws IOException {
        return fileChannel.position();
    }

    /**
     * Change the position
     *
     * @param pos
     * @throws IOException
     */
    @Override
    public synchronized void position(long pos) throws IOException {
        fileChannel.position(pos);
    }

    @Override
    public synchronized void close() throws IOException {
        fileChannel.close();
    }

    public synchronized int available() throws IOException {
        return (int) (fileChannel.size() - fileChannel.position());
    }

    public synchronized long skip(long l) throws IOException {
        fileChannel.position(fileChannel.position() + l);
        return fileChannel.position();
    }

    boolean hasContent() throws IOException {
        return available() > 0 || readBuffer.position() > 0;
    }

    long size() throws IOException {
        return fileChannel.size();
    }

    /**
     * Decode whatever characters are available into the passed CharBuffer. Note
     * that for multi-byte encodings, CharsetDecoders are stateful, and a
     * previous call could result in being at a byte-position that's part-way
     * through reading a character. Always pass the same decoder unless a
     * decoding error has occurred.
     *
     * @param target The charbuffer to decode results into
     * @param charsetDecoder A decoder for the desired charset
     * @return The result of decoding
     * @throws IOException If something goes wrong
     */
    @Override
    public synchronized CoderResult decode(CharBuffer target, CharsetDecoder charsetDecoder) throws IOException {
        Obj<CoderResult> result = Obj.create();
        if (readBuffer.position() == readBuffer.capacity()) {
            readBuffer.clear();
        }
        int count = FileUtils.decode(fileChannel, readBuffer, target, charsetDecoder, result, true);
        if (count > 0) {
            target.flip();
        }
        return result.get();
    }
}
