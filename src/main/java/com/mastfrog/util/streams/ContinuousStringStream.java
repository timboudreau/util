package com.mastfrog.util.streams;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * An InputStream-like construct which does not acknowledge the end of files.
 */
public final class ContinuousStringStream implements AutoCloseable {

    private final FileChannel fileChannel;
    private final ByteBuffer readBuffer;

    public ContinuousStringStream(FileChannel fileChannel, int readBufferSizeInBytes) {
        this.fileChannel = fileChannel;
        readBuffer = ByteBuffer.allocateDirect(readBufferSizeInBytes);
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
    public synchronized long position() throws IOException {
        return fileChannel.position();
    }

    /**
     * Change the position
     *
     * @param pos
     * @throws IOException
     */
    public synchronized void position(long pos) throws IOException {
        fileChannel.position(pos);
    }

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
    public synchronized CoderResult decode(CharBuffer target, CharsetDecoder charsetDecoder) throws IOException {
        if (fileChannel.position() == fileChannel.size()) {
            return CoderResult.UNDERFLOW;
        }
        for (;;) {
            // Clear the mark and limit
            readBuffer.clear();
            // Slurp in whatever we can
            int numBytesRead = fileChannel.read(readBuffer);
            // get ready to copy data out
            readBuffer.flip();
            // got nothing?  We're done for now.
            if (numBytesRead <= 0) {
                return CoderResult.UNDERFLOW;
            }
            // Decode the bytes into the character set
            CoderResult characterDecodingResult = charsetDecoder.decode(readBuffer, target, true);
            // If we're done, get out
            long remainingLength = target.capacity() - target.position();
            // Loop until the buffer is full
            if (remainingLength <= 0) {
                return characterDecodingResult;
            }
        }
    }
}
