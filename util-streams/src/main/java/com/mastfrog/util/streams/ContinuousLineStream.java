package com.mastfrog.util.streams;

import static com.mastfrog.util.preconditions.Checks.greaterThanOne;
import static com.mastfrog.util.preconditions.Checks.nonNegative;
import static com.mastfrog.util.preconditions.Checks.nonZero;
import static com.mastfrog.util.preconditions.Checks.notNull;
import static com.mastfrog.util.preconditions.Checks.readable;
import com.mastfrog.util.preconditions.Exceptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads a file line-by-line, but unlike a BufferedReader, does not give up when
 * the end of the file is reached. It is also conservative about the amount of
 * memory it consumes and avoids excessive memory-copies.
 * <p>
 * Implements Iterable&lt;CharSequence&gt; for convenience (these methods can
 * throw RuntimeExceptions wrapping any encountered IOException).
 * </p><p>
 * This class is not thread-safe.
 * </p>
 */
public final class ContinuousLineStream implements AutoCloseable, Iterator<CharSequence> {

    private final ContinuousStringStream stringStream;
    private final CharsetDecoder charsetDecoder;
    private final LinkedList<CharSequence> queuedLines = new LinkedList<>();
    private String cachedPartialNextLine = null;
    private final int byteCount;
    private final int maxLinesToBuffer;

    public ContinuousLineStream(ContinuousStringStream stringStream, CharsetDecoder charsetDecoder, int charsPerBuffer) {
        this(stringStream, charsetDecoder, charsPerBuffer, Integer.MAX_VALUE);
    }

    public ContinuousLineStream(ContinuousStringStream stringStream, CharsetDecoder charsetDecoder, int charsPerBuffer, int maxLinesToBuffer) {
        this.maxLinesToBuffer = greaterThanOne("maxLinesToBuffer", maxLinesToBuffer);
        this.stringStream = notNull("stringStream", stringStream);
        this.charsetDecoder = notNull("charsetDecoder", charsetDecoder);
        float bytesPerChar = Math.max(1F, 1F / charsetDecoder.averageCharsPerByte());
        this.byteCount = Math.max(512, (int) (charsPerBuffer * bytesPerChar));
    }

    /**
     * Create a UTF-8 line stream with a buffer size of 8192 bytes.
     *
     * @param file The file
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(Path file) {
        return of(file.toFile());
    }

    /**
     * Create a UTF-8 line stream with the specified <i>byte</i> buffer size.
     *
     * @param file The file
     * @param bufferSize the size of the read buffer which will be used to
     * collect lines - for better performance, the size should be > the average
     * line length in bytes for the character set
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(Path file, int bufferSize) {
        return of(file.toFile(), bufferSize);
    }

    /**
     * Create a line stream for the specified encoding with the specified
     * <i>byte</i> buffer size.
     *
     * @param file The file
     * @param bufferSize the size of the read buffer which will be used to
     * collect lines - for better performance, the size should be > the average
     * line length in bytes for the character set
     * @param charset The character set the file's data uses
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(Path file, int bufferSize, Charset charset) {
        return of(file.toFile(), bufferSize, charset);
    }

    /**
     * Create a UTF-8 line stream with a buffer size of 8192 bytes.
     *
     * @param file The file
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(File file) {
        return of(file, 8_192);
    }

    /**
     * Create a UTF-8 line stream with the specified <i>byte</i> buffer size.
     *
     * @param file The file
     * @param bufferSize the size of the read buffer which will be used to
     * collect lines - for better performance, the size should be > the average
     * line length in bytes for the character set
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(File file, int bufferSize) {
        return of(file, bufferSize, UTF_8);
    }

    /**
     * Create a line stream for the specified encoding with the specified
     * <i>byte</i> buffer size.
     *
     * @param file The file
     * @param bufferSize the size of the read buffer which will be used to
     * collect lines - for better performance, the size should be > the average
     * line length in bytes for the character set
     * @param charset The character set the file's data uses
     * @return
     * @throws IllegalArgumentException if the file is not readable
     */
    public static ContinuousLineStream of(File file, int bufferSize, Charset charset) {
        try {
            nonZero("bufferSize", nonNegative("bufferSize", bufferSize));
            notNull("charset", charset);
            readable("file", file);
            CharsetDecoder dec = charset.newDecoder();
            int charBufferSize = Math.max(2, ((int) Math.ceil(dec.averageCharsPerByte() * (float) bufferSize)));
            ContinuousStringStream stream = new ContinuousStringStream(new FileInputStream(file).getChannel(), bufferSize);
            CharsetDecoder decoder = charset.newDecoder();
            return new ContinuousLineStream(stream, decoder, charBufferSize);
        } catch (FileNotFoundException ex) {
            // readableAndNonZeroLength will throw an IllegalArgumentException before any
            // FileNotFoundException could be thrown
            return Exceptions.chuck(ex);
        }
    }

    public boolean isOpen() {
        return stringStream.isOpen();
    }

    private void check() throws IOException {
        if (queuedLines.isEmpty()) {
            findNextLines();
        }
    }

    /**
     * Determine if there are currently any more lines available.
     *
     * @return True if there are more lines
     * @throws IOException If something goes wrong
     */
    public synchronized boolean hasMoreLines() throws IOException {
        check();
        boolean result = !queuedLines.isEmpty();
        return result;
    }

    /**
     * Get the next line, if any.
     *
     * @return The next line, or null if there are none
     * @throws IOException If something goes wrong
     */
    public synchronized CharSequence nextLine() throws IOException {
        check();
        return queuedLines.isEmpty() ? null : queuedLines.pop();
    }

    /**
     * Get the current position in the file
     *
     * @return The file position
     * @throws IOException If the channel is closed or something else is wrong
     */
    public long position() throws IOException {
        return stringStream.position();
    }

    /**
     * Change the position for reading in the file, discarding any
     * enqueued partial or complete lines.
     *
     * @param pos The new position
     * @throws IOException If the position is invalid or something else is wrong
     */
    public synchronized void position(long pos) throws IOException {
        stringStream.position(pos);
        // Discard any partially read line
        cachedPartialNextLine = null;
        // Discard any lines in the pipeline to be read next
        queuedLines.clear();
    }

    /**
     * In the case that the file did not end with a newline, get the remaining
     * data from it
     *
     * @return The remaining data
     */
    public synchronized String remainingData() {
        return cachedPartialNextLine;
    }

    public long available() throws IOException {
        return stringStream.available();
    }

    private CharBuffer lastCharBuffer;

    private CharBuffer readCharacters() throws IOException {
        // Note that we MUST NOT reuse this buffer repeatedly, since we are
        // saving subsequences of it as queued lines, and rewinding and reusing
        // this buffer will cause those subsequences to have their data
        // replaced by whatever was loaded next.  So we use it until it's
        // full, and then discard it - though we could implement some sort
        // of pooling strategy if we were that paranoid about zero allocations
        if (lastCharBuffer != null && lastCharBuffer.position() < lastCharBuffer.limit()) {
            return lastCharBuffer;
        }
        CharBuffer characterData = lastCharBuffer = ByteBuffer.allocateDirect(byteCount).asCharBuffer();
        stringStream.decode(characterData, charsetDecoder);
        return characterData;
    }

    private static final boolean isJdk6 = "1.6".equals(System.getProperty("java.specification.version"));

    private static CharSequence subsequence(CharSequence string, int start, int end) {
        if (isJdk6) {
            // CharSequence.subsequence() throws UOE for CharBuffer in 1.6;
            // we can be used as a library from 1.6 and this is
            // the only JDK7ism;  this works, but is less efficient
            return string.toString().subSequence(start, end);
        } else {
            return string.subSequence(start, end);
        }
    }

    /**
     * Here we: - If we have queued lines or there is nothing to read, do
     * nothing - Loop until we've found some lines or we're out of data, and in
     * the loop - Create a buffer to read into - Create a CharBuffer to read
     * into - Decode data using the passed decoder - Track the most recently
     * seen line start - Loop over the characters we've decoded looking for \n -
     * If a new line, add the preceding one to the queue, - Do some special
     * handling for consecutive newlines (check this with \r\n terminators), and
     * add a blank cached line in that case - Otherwise we've reached the end of
     * a line that was partially cached on a previous call, so queue that up for
     * a read - If not a new line, create a CharBuffer subsequence (thus not
     * actually copying the bytes again) and queue it up to be returned from a
     * read - Record the current position as the new line start - Check if we've
     * almost exhausted the queued bytes, and if so, cache any partially read
     * characters to be prepended to the first line read by the next call
     *
     * @throws IOException
     */
    long sizeAtLastFindNext = -1;

    private synchronized void findNextLines() throws IOException {
        if (!stringStream.hasContent() || !queuedLines.isEmpty()) {
            return;
        }
        while (queuedLines.isEmpty() && stringStream.hasContent()) {
            CharBuffer characterData = readCharacters();
            int lineStart = characterData.position();
            int limit = characterData.limit();
            char prevChar = 0;
            int charIndex = 0;
            while (characterData.remaining() > 0) {
                char c = characterData.get();
                // XXX - double newlines cause a line starting with \n to
                // be emitted
                if (c == '\n') {
                    if (charIndex == lineStart) { // 0-length line
                        if (cachedPartialNextLine != null) {
                            queuedLines.add(cachedPartialNextLine);
                            cachedPartialNextLine = null;
                        } else {
                            queuedLines.add("");
                            lineStart = charIndex + 1;
                        }
                    } else {
                        int end = Math.max(0, prevChar == '\r' ? charIndex - 1 : charIndex);
                        int oldPos = characterData.position();
                        characterData.position(lineStart);
                        CharSequence currentLine = subsequence(characterData, 0, end - lineStart);
                        characterData.position(oldPos);
                        if (cachedPartialNextLine != null) {
                            queuedLines.add(cachedPartialNextLine + currentLine);
                            cachedPartialNextLine = null;
                        } else {
                            if (currentLine.length() > 0 && currentLine.charAt(0) == '\n') {
                                currentLine = currentLine.subSequence(1, currentLine.length());
                            }
                            queuedLines.add(currentLine);
                        }
                        lineStart = charIndex + 1;
                    }
                } else if (charIndex == limit - 1) {
                    int oldPos = characterData.position();
                    characterData.position(lineStart);
                    CharSequence partialLine = subsequence(characterData, 0, (charIndex + 1) - lineStart);
                    if (partialLine.length() > 0 && partialLine.charAt(0) == '\n') {
                        partialLine = partialLine.subSequence(1, partialLine.length());
                    }
                    characterData.position(oldPos);
                    if (cachedPartialNextLine == null) {
                        // Use a string - we don't want to hold the whole buffer
                        // which may contain earlier lines
                        cachedPartialNextLine = partialLine.toString();
                    } else {
                        // Concatenate if we never found a newline while looping
                        cachedPartialNextLine += partialLine.toString();
                    }
                }
                if (queuedLines.size() >= maxLinesToBuffer) {
                    break;
                }
                prevChar = c;
                charIndex++;
            }
        }
    }

    /**
     * Close this stream, returning any already read lines and
     * any partial line for which a newline had not yet been
     * encountered; if at the end of the file, this will be the
     * tail.
     *
     * @return A list of any remaining strings
     */
    public List<CharSequence> closeReturningTail() throws IOException {
        try {
            List<CharSequence> result = new ArrayList<>(queuedLines);
            queuedLines.clear();
            if (cachedPartialNextLine != null && cachedPartialNextLine.length() > 0) {
                result.add(cachedPartialNextLine);
                cachedPartialNextLine = null;
            }
            return result;
        } finally {
            close();
        }
    }

    public boolean isAtEndOfFile() throws IOException {
        return !isOpen() || this.stringStream.available() == 0;
    }

    /**
     * Close the underlying stream. Note there may still be partial lines or an
     * unfinished line held when this instance is closed.
     *
     * @throws IOException If something goes wrong
     */
    @Override
    public void close() throws IOException {
        stringStream.close();
    }

    @Override
    public boolean hasNext() {
        try {
            return hasMoreLines();
        } catch (IOException ex) {
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Get the next char sequence, returning none if at the end of the file or
     * nothing further is available.
     *
     * @return A char sequence or null
     */
    @Override
    public CharSequence next() {
        try {
            return nextLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Throws a UOE.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return this as an iterable.
     *
     * @return An iterable of char sequences
     */
    public Iterable<CharSequence> toIterable() {
        return () -> this;
    }
}
