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
package com.mastfrog.util.file;

import com.mastfrog.function.throwing.io.IOFunction;
import com.mastfrog.function.throwing.io.IOSupplier;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility methods for reading and writing files and directories. The read and
 * write methods take special care to be memory-efficient and avoid unnecessary
 * memory-copies.
 *
 * @author Tim Boudreau
 */
public final class FileUtils {

    public static final String SYSPROP_DEFAULT_BUFFER_SIZE = "FileUtils.defaultBufferSize";
    private static final int DEFAULT_BUFFER_SIZE;
    private static final String PREFIX = "java-";
    private static final AtomicInteger FILES_INDEX = new AtomicInteger(1);
    private static final Set<StandardOpenOption> OPEN_WRITE_CREATE = EnumSet.noneOf(StandardOpenOption.class);
    private static final Set<StandardOpenOption> OPEN_APPEND_CREATE = EnumSet.noneOf(StandardOpenOption.class);
    private static final FileVisitOption[] NO_FV_OPTIONS = new FileVisitOption[0];
    private static final FileVisitOption[] FOLLOW_LINKS = new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS};

    private static final String[] DEFAULT_SEARCH_PATH
            = {"/usr/bin", "/usr/local/bin", "/opt/local/bin", "/bin", "/sbin", "/usr/sbin", "/opt/bin"};

    static {
        OPEN_WRITE_CREATE.add(StandardOpenOption.CREATE);
        OPEN_WRITE_CREATE.add(StandardOpenOption.WRITE);
        OPEN_WRITE_CREATE.add(StandardOpenOption.TRUNCATE_EXISTING);
        OPEN_APPEND_CREATE.add(StandardOpenOption.CREATE);
        OPEN_APPEND_CREATE.add(StandardOpenOption.APPEND);

        String s = System.getProperty(SYSPROP_DEFAULT_BUFFER_SIZE);
        if (s != null) {
            DEFAULT_BUFFER_SIZE = Integer.parseInt(s);
            Checks.greaterThanZero(SYSPROP_DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
        } else {
            // 512 is usually at least an even fraction of page size, and is
            // usually the block size for SSD and NVMe disks, so this is
            // likely to be optimal on modern systems
            DEFAULT_BUFFER_SIZE = 512;
        }
    }

    /**
     * Create a new file with the given name and extension, appending "-1",
     * "-2", and so forth to the name portion until a name is generated which
     * does not already exist. Creates a 0-byte file.
     *
     * @param dir The folder
     * @param name The name
     * @param ext The extension
     * @return The created file
     * @throws IOException
     */
    public synchronized Path newFile(Path dir, String name, String ext) throws IOException {
        return Files.createFile(newPath(dir, name, ext));
    }

    /**
     * Create a path to a nonexistent file of the given name and extension,
     * appending "-1", "-2", and so forth to the name portion until a name is
     * generated which does not already exist.
     *
     * @param dir The folder
     * @param name The name
     * @param ext The extension
     * @return The created file
     * @throws IOException
     */
    public synchronized Path newPath(Path dir, String name, String ext) throws IOException {
        int ix = 0;
        Path p = dir.resolve(name + "." + ext);
        while (Files.exists(p)) {
            p = dir.resolve(name + "-" + ++ix + "." + ext);
        }
        Files.createFile(p);
        return p;
    }

    // These methods need to be synchronized so two threads cannot
    // race and create the same unused file name
    /**
     * Create a new temporary subfolder in the system temporary files folder.
     *
     * @return A new folder created on disk
     * @throws IOException
     */
    public synchronized static Path newTempDir() throws IOException {
        return newTempDir(PREFIX);
    }

    /**
     * Create a new temporary subfolder in the system temporary files folder.
     *
     * @param prefix String prefix to prepend to the file name for
     * identification
     * @return A new folder created on disk
     * @throws IOException If something goes wrong
     */
    public synchronized static Path newTempDir(String prefix) throws IOException {
        Path result = newTempPath(prefix);
        Files.createDirectories(result);
        return result;
    }

    /**
     * Create a new temporary file on disk in the system temporary files folder.
     * An empty file is created on disk.
     *
     * @return A new file
     * @throws IOException If something goes wrong
     */
    public synchronized static Path newTempFile() throws IOException {
        return newTempFile(PREFIX);
    }

    /**
     * Create a new temporary file in the system temporary files folder.
     *
     * @param prefix String prefix to prepend to the file name for
     * identification
     * @return A new folder created on disk
     * @throws IOException If something goes wrong
     */
    public synchronized static Path newTempFile(String prefix) throws IOException {
        Path path = newTempPath(prefix);
        Files.createFile(path);
        return path;
    }

    /**
     * Create a new file or directory path in the system temporary directory
     * <i>without creating anything on disk</i>.
     *
     * @param prefix The prefix to prepend to the file name
     * @return A file name
     */
    public synchronized static Path newTempPath(String prefix) {
        Checks.notNull("prefix", prefix);
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"));
        long now = System.currentTimeMillis();
        String base = prefix + Long.toString(now, 36)
                + "-" + FILES_INDEX.getAndIncrement();
        Path target = tmp.resolve(base);
        int ix = 1;
        while (Files.exists(target)) {
            target = tmp.resolve(base + "-" + ix++);
        }
        return target;
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param content The character sequence to write
     * @param as The character set to use
     * @param bufferSize The size of buffer to use, trading performance for
     * memory use as desired
     * @param append If true, append to the file, rather than truncating it if
     * it exists
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, CharSequence content, Charset as, int bufferSize, boolean append) throws IOException {
        writeFile(path, true, content, as, bufferSize, append);
    }

    /**
     * Write a character sequence to a file as UTF-8.
     *
     * @param path The file path
     * @param content The character sequence to write
     * @throws IOException If something goes wrong
     */
    public static void writeUtf8(Path path, CharSequence content) throws IOException {
        writeFile(path, content, UTF_8);
    }

    /**
     * Write a character sequence to a file as ASCII.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param as The character set to use
     * @throws IOException If something goes wrong
     */
    public static void writeAscii(Path path, CharSequence content) throws IOException {
        writeFile(path, content, US_ASCII);
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param as The character set to use
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, CharSequence content, Charset as) throws IOException {
        writeFile(path, true, content, as, DEFAULT_BUFFER_SIZE, false);
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param as The character set to use
     * @param append If true, append to the file, rather than truncating it if
     * it exists
     * @param attrs Any file attributes to apply if the file is being created
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, CharSequence content, Charset as, boolean append, FileAttribute... attrs) throws IOException {
        writeFile(path, true, content, as, DEFAULT_BUFFER_SIZE, append, attrs);
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param as The character set to use
     * @param bufferSize The size of buffer to use, trading performance for
     * memory use as desired
     * @param append If true, append to the file, rather than truncating it if
     * it exists
     * @param attrs Any file attributes to apply if the file is being created
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, CharSequence content, Charset as, int bufferSize, boolean append, FileAttribute... attrs) throws IOException {
        writeFile(path, true, content, as, bufferSize, append, attrs);
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param as The character set to use
     * @param bufferSize The size of buffer to use, trading performance for
     * memory use as desired
     * @param append If true, append to the file, rather than truncating it if
     * it exists
     * @param attrs Any file attributes to apply if the file is being created
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, boolean permissive, CharSequence content, Charset as, int bufferSize, boolean append, FileAttribute... attrs) throws IOException {
        writeFile(path, permissive, content, false, as, bufferSize, append, attrs);
    }

    /**
     * Write a character sequence to a file. This method provides tight control
     * over how much memory is used to perform the write, and trade-offs between
     * performance and memory pressure.
     *
     * @param path The file path
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param content The character sequence to write
     * @param directBuffers Use direct buffers - slower performance for small
     * files, but one less memory copy
     * @param as The character set to use
     * @param bufferSize The size of buffer to use, trading performance for
     * memory use as desired
     * @param append If true, append to the file, rather than truncating it if
     * it exists
     * @param attrs Any file attributes to apply if the file is being created
     * @throws IOException If something goes wrong
     */
    public static void writeFile(Path path, boolean permissive, CharSequence content, boolean directBuffers, Charset as, int bufferSize, boolean append, FileAttribute... attrs) throws IOException {
        Set<? extends OpenOption> options = append ? OPEN_APPEND_CREATE : OPEN_WRITE_CREATE;
        writeCharSequence(content, permissive, as, bufferSize, directBuffers, () -> {
            return FileChannel.open(path, options, attrs);
        }).close();
    }

    /**
     * Write a character sequence to a channel. Note: <i>this method does
     * <b>not</b> close the channel (unless an exception is thrown while
     * writing)</i> - the channel is returned and may be closed by the caller if
     * desired. This enables this method to be used with channel pools where the
     * channel should not be closed.
     *
     * @param content The character sequence to write
     * @param permissive If true, do not throw exceptions if characters are
     * unencodable
     * @param as The character set to use
     * @param bufferSize The size of buffer to use, trading performance for
     * memory use as desired
     * @param directBuffers Use direct buffers - slower performance for small
     * files, but one less memory copy
     * @param channelSupplier The supplier which will open a writable channel
     * @return The channel which was written to
     * @throws IOException If something goes wrong
     */
    public static WritableByteChannel writeCharSequence(CharSequence content,
            boolean permissive, Charset as, int bufferSize,
            boolean directBuffers, IOSupplier<? extends WritableByteChannel> channelSupplier) throws IOException {
        Checks.notNull("content", content);
        Checks.notNull("as", as);
        Checks.greaterThanZero("bufferSize", bufferSize);
        Checks.notNull("channelSupplier", channelSupplier);
        WritableByteChannel channel = channelSupplier.get();
        try {
            ByteBuffer buffer = directBuffers ? ByteBuffer.allocateDirect(Math.max(4, bufferSize)) : ByteBuffer.allocate(Math.max(4, bufferSize));
            CharsetEncoder enc = as.newEncoder();
            CharBuffer charBuffer = CharBuffer.wrap(content);
            for (;;) {
                buffer.clear();
                CoderResult res = enc.encode(charBuffer, buffer, false);
                if (!permissive && res.isError() || res.isMalformed()) {
                    res.throwException();
                }
                buffer.flip();
                int bytesWritten = channel.write(buffer);
                if (bytesWritten == 0) {
                    break;
                }
            }
            return channel;
        } catch (Exception ex) {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            return Exceptions.chuck(ex);
        }
    }

    /**
     * Read an ASCII character sequence from a file using default settings and
     * permissive handling of coding errors.
     *
     * @param path The file
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readAscii(Path path) throws IOException {
        return readCharSequence(path, US_ASCII);
    }

    /**
     * Read an ASCII character sequence from a file using default settings and
     * permissive handling of coding errors.
     *
     * @param path The file
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static String readAsciiString(Path path) throws IOException {
        return readString(path, US_ASCII);
    }

    /**
     * Read a UTF-8 character sequence from a file using default settings and
     * permissive handling of coding errors.
     *
     * @param path The file
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readUTF8(Path path) throws IOException {
        return readCharSequence(path, UTF_8);
    }

    /**
     * Read a UTF-8 character sequence from a file using default settings and
     * permissive handling of coding errors.
     *
     * @param path The file
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static String readUTF8String(Path path) throws IOException {
        return readString(path, UTF_8);
    }

    /**
     * Read a String from a file using default settings and permissive handling
     * of coding errors.
     *
     * @param path The file
     * @param as The character set
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static String readString(Path path, Charset as) throws IOException {
        return readString(path, DEFAULT_BUFFER_SIZE, as, true);
    }

    /**
     * Read a character sequence from a file using default settings and
     * permissive handling of coding errors; the returned character sequence
     * wraps the character buffers decoded to minimize copies.
     *
     * @param path The file
     * @param as The character set
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readCharSequence(Path path, Charset charset) throws IOException {
        return readCharSequence(path, DEFAULT_BUFFER_SIZE, charset, true);
    }

    /**
     * Read a String from a file using default settings and permissive handling
     * of coding errors.
     *
     * @param path The file
     * @param bufferSize The size of the buffer used to read, trading memory for
     * performance or performance for memory footprint
     * @param as The character set
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static String readString(Path path, int bufferSize, Charset charset) throws IOException {
        return readString(path, bufferSize, charset, true);
    }

    /**
     * Read a character sequence from a file using default settings and
     * permissive handling of coding errors; the returned character sequence
     * wraps the character buffers decoded to minimize copies.
     *
     * @param path The file
     * @param bufferSize The size of the buffer used to read, trading memory for
     * performance or performance for memory footprint
     * @param as The character set
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readCharSequence(Path path, int bufferSize, Charset charset) throws IOException {
        return readCharSequence(path, bufferSize, charset, true);
    }

    /**
     * Read a String from a file using default settings and permissive handling
     * of coding errors.
     *
     * @param path The file
     * @param bufferSize The size of the buffer used to read, trading memory for
     * performance or performance for memory footprint
     * @param as The character set
     * @param permissive If true, ignore undecodable characters
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static String readString(Path path, int bufferSize, Charset charset, boolean permissive) throws IOException {
        return readCharSequence(path, bufferSize, charset, permissive).toString();
    }

    /**
     * Read a character sequence from a file using default settings and
     * permissive handling of coding errors; the returned character sequence
     * wraps the character buffers decoded to minimize copies.
     *
     * @param path The file
     * @param bufferSize The size of the buffer used to read, trading memory for
     * performance or performance for memory footprint
     * @param as The character set
     * @param permissive If true, ignore undecodable characters
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readCharSequence(Path path, int bufferSize, Charset charset, boolean permissive) throws IOException {
        return readCharSequence(path, false, bufferSize, charset, permissive);
    }

    /**
     * Read a character sequence from a file using default settings and
     * permissive handling of coding errors; the returned character sequence
     * wraps the character buffers decoded to minimize copies.
     *
     * @param path The file
     * @param directBuffers If true, use NIO direct buffers for reads, trading
     * some performance for one less memory copy
     * @param bufferSize The size of the buffer used to read, trading memory for
     * performance or performance for memory footprint
     * @param as The character set
     * @param permissive If true, ignore undecodable characters
     * @return A character sequence
     * @throws IOException If something goes wrong
     */
    public static CharSequence readCharSequence(Path path, boolean directBuffers, int bufferSize, Charset charset, boolean permissive) throws IOException {
        Checks.notNull("path", path);
        Checks.greaterThanZero("bufferSize", bufferSize);
        Checks.notNull("charset", charset);
        // Less than 4 will cause malformed decode on some character sets, and is
        // silly anyway
        ReadableByteChannel[] holder = new ReadableByteChannel[1];
        CharSequence result = readCharSequence(directBuffers, bufferSize, charset, permissive, holder, () -> {
            return FileChannel.open(path, StandardOpenOption.READ);
        });
        if (holder[0] != null) {
            holder[0].close();
        }
        return result;
    }

    /**
     * Read a character sequence from a channel at its current position as
     * supplied, using the passed settings to tune performance versus memory
     * footprint and copying overhead; the returned character sequence wraps the
     * decoded character buffers to minimize copies.
     *
     * @param directBuffers If true, use direct buffers to eliminate one memory
     * copy
     * @param bufferSize The number of bytes to read at a time to manage memory
     * pressure
     * @param charset The character set
     * @param permissive If true, ignore encoding errors
     * @param channelHolder The channel provided by the supplier is set as the
     * zeroth element, to allow the caller to close the channel if it was
     * created by the supplier
     * @param ch A supplier of a channel; if the channel is already in
     * existence, it is read from the current position; the channel will not be
     * closed by this method unless an exception is thrown while reading
     * @return A character sequence which wraps the decoded character buffers
     * @throws IOException If something goes wrong
     */
    public static CharSequence readCharSequence(boolean directBuffers, int bufferSize, Charset charset, boolean permissive, ReadableByteChannel[] channelHolder, IOSupplier<ReadableByteChannel> ch) throws IOException {
        Checks.greaterThanZero("channelHolder.length", channelHolder.length);
        ReadableByteChannel channel = channelHolder[0] = ch.get();
        try {
            if (channel instanceof SeekableByteChannel && ((SeekableByteChannel) channel).size() == 0) {
                SeekableByteChannel sk = (SeekableByteChannel) channel;
                if (sk.position() == sk.size()) {
                    return "";
                }
            }
            bufferSize = Math.max(4, bufferSize);
            ByteBuffer readBuffer = directBuffers ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
            CharsetDecoder decoder = charset.newDecoder();
            List<CharBuffer> charBuffers = new ArrayList<>();
            bufferSize = Math.max((int) Math.ceil(1f / decoder.averageCharsPerByte()), bufferSize);
            int charBufferSize = Math.max(2, ((int) Math.ceil(decoder.averageCharsPerByte() * (float) bufferSize)));
            for (; /*channel.position() < channel.size() || */ readBuffer.limit() > 0;) {
                CharBuffer charBuffer = directBuffers
                        ? ByteBuffer.allocate(charBufferSize).asCharBuffer()
                        : CharBuffer.allocate(charBufferSize);

                int count = decode(channel, readBuffer, charBuffer, decoder, true);
                if (count < 0) {
                    break;
                }
                if (charBuffer.limit() > 0) {
                    charBuffers.add(charBuffer);
                }
                if (count == 0) {
                    break;
                }
            }
            return new CharBuffersCharSequence(charBuffers.toArray(new CharBuffer[charBuffers.size()]));
        } catch (Exception ioe) {
            channel.close();
            return Exceptions.chuck(ioe);
        }
    }

    static int decode(ReadableByteChannel fileChannel, ByteBuffer readBuffer, CharBuffer target, CharsetDecoder charsetDecoder, boolean permissive) throws IOException {
//        if (readBuffer.position() == 0 && fileChannel.position() == fileChannel.size()) {
//            return -1;
//        }
        int numBytesRead;
        int total = 0;
        CoderResult lastCoderResult = null;
        for (;;) {
            numBytesRead = fileChannel.read(readBuffer);
            if (readBuffer.position() > 0) {
                ByteBuffer rb = readBuffer;
                total += Math.max(0, numBytesRead);
                // got nothing?  We're done for now.
                if (numBytesRead < 0 && rb.position() == 0) {
                    return -1;
                }
                if (numBytesRead <= 0 /*&& fileChannel.position() == fileChannel.size() */ && rb.position() == rb.limit()) {
                    return -1;
                }
                rb.flip();
                // Decode the bytes into the character set
                lastCoderResult = charsetDecoder.decode(rb, target, true);
                if (rb.position() < rb.limit()) {
                    ByteBuffer tail = rb.slice();
                    rb.rewind();
                    rb.put(tail);
                } else {
                    rb.rewind();
                }
                if (!permissive) {
                    if (lastCoderResult.isError() || lastCoderResult.isUnmappable() || lastCoderResult.isMalformed()) {
                        lastCoderResult.throwException();
                    }
                }
            } else {
                break;
            }
            if (target.capacity() >= target.position()) {
                break;
            }
        }
        if (!permissive /*&& fileChannel.position() == fileChannel.size()*/) {
            if (lastCoderResult.isUnderflow()) {
                lastCoderResult.throwException();
            }
        }
        target.flip();
        return total;
    }

    /**
     * Delete a folder and its subtree; handles the condition that this method
     * may race and files may already have been deleted (for example, shutting
     * down a database process which deletes its pidfile) after the set of files
     * being iterated was computed, silently.
     *
     * @param dir A directory
     * @throws IOException If a file cannot be deleted, the passed file is not a
     * directory, or the process owner does not have the needed permissions
     */
    public static void deltree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }
        Set<Path> paths = new HashSet<>();
        for (;;) {
            try (Stream<Path> all = Files.walk(dir)) {
                all.forEach(paths::add);
                break;
            } catch (NoSuchFileException ex) {
                // ok, pid file deleted by postgres during
                // shutdown or similar racing with our file deletion
            }
        }
        List<Path> l = new ArrayList<>(paths);
        l.sort((pa, pb) -> {
            return -Integer.compare(pa.getNameCount(), pb.getNameCount());
        });
        for (Path p : l) {
            try {
                Files.delete(p);
            } catch (NoSuchFileException ex) {
                // do nothing - this can race, since a process may still be
                // shutting down and deleting things
            }
        }
    }

    /**
     * Tail a file, such as a log file. This method returns a function which
     * takes a Predicate&lt;CharSequence&gt;, which will be called whenever a
     * complete line of text is appended to the file (it will initially be
     * passed all existing lines of the file). The function returns a Runnable
     * which can be invoked to cancel tailing the file.
     * <p>
     * Internally, uses ContinuousLineStream and the JDK's directory watch
     * facility to unpause the thread that reads lines.
     * </p>
     *
     * @param file The fail to tail (must exist)
     * @param charset The character set for lines
     * @return A function which takes a listener predicate (which may return
     * false to abort watching) and returns a Runnable which can be invoked to
     * cancel listening
     * @throws IOException If something goes wrong
     */
    public static IOFunction<Predicate<CharSequence>, Runnable> tail(Path file, Charset charset) throws IOException {
        ExecutorService svc = Executors.newCachedThreadPool();
        IOFunction<Predicate<CharSequence>, Runnable> result = tail(file, charset, DEFAULT_BUFFER_SIZE, svc);
        return (pred -> {
            Runnable stopWatching = result.apply(pred);
            return () -> {
                try {
                    stopWatching.run();
                } finally {
                    svc.shutdown();
                }
            };
        });
    }

    /**
     * Tail a file, such as a log file. This method returns a function which
     * takes a Predicate&lt;CharSequence&gt;, which will be called whenever a
     * complete line of text is appended to the file (it will initially be
     * passed all existing lines of the file). The function returns a Runnable
     * which can be invoked to cancel tailing the file.
     * <p>
     * Internally, uses ContinuousLineStream and the JDK's directory watch
     * facility to unpause the thread that reads lines.
     * </p>
     *
     * @param file The fail to tail (must exist)
     * @param charset The character set for lines
     * @param bufferSize The size of the internal buffer used to read bytes from
     * disk
     * @param exe The thread pool the watch thread will run in
     * @return A function which takes a listener predicate (which may return
     * false to abort watching) and returns a Runnable which can be invoked to
     * cancel listening
     * @throws IOException If something goes wrong
     */
    public static IOFunction<Predicate<CharSequence>, Runnable> tail(Path file,
            Charset charset, int bufferSize, Executor exe) throws IOException {
        return new Tail(exe, file, bufferSize, charset);
    }

    /**
     * Find an executable file with the passed file name, search the system path
     * and the default search path
     * (<code>/usr/bin:/usr/local/bin:/opt/local/bin:/bin:/sbin:/usr/sbin:/opt/bin</code>).
     * Null is never returned - if nothing is found, returns a raw path name of
     * the command name by itself.
     *
     * @param command The name of the executable; if the name is a file path,
     * only the file name portion is used
     * @return A path, which may or may not exist.
     */
    public static Path findExecutable(String command) {
        return findExecutable(command, true, true);
    }

    /**
     * Find an executable file with the passed file name, search the system path
     * and the default search path
     * (<code>/usr/bin:/usr/local/bin:/opt/local/bin:/bin:/sbin:/usr/sbin:/opt/bin</code>).
     * Null is never returned - if nothing is found, returns a raw path name of
     * the command name by itself.
     *
     * @param command The name of the executable; if the name is a file path,
     * only the file name portion is used
     * @param alsoSearch A path or list of paths to search <i>first</i>,
     * preferring any executable of the right name found in these locations
     * @return A path, which may or may not exist.
     */
    public static Path findExecutable(String command, String... alsoSearch) {
        return findExecutable(command, true, true, alsoSearch);
    }

    /**
     * Find an executable file with the passed file name, search the system path
     * and the default search path
     * (<code>/usr/bin:/usr/local/bin:/opt/local/bin:/bin:/sbin:/usr/sbin:/opt/bin</code>).
     * Null is never returned - if nothing is found, returns a raw path name of
     * the command name by itself.
     *
     * @param command The name of the executable; if the name is a file path,
     * only the file name portion is used
     * @param useDefaultSearchPath if true, search the default search path in
     * addition to any <code>PATH</code> variable in the environment (if
     * <code>useSystemPath</code> is true)
     * (<code>/usr/bin:/usr/local/bin:/opt/local/bin:/bin:/sbin:/usr/sbin:/opt/bin</code>).
     * @param useSystemPath Search any system path provided by the
     * <code>PATH</code> environment variable
     * @param alsoSearch A path or list of paths to search <i>first</i>,
     * preferring any executable of the right name found in these locations
     * @return A path, which may or may not exist.
     */
    public static Path findExecutable(String command, boolean useDefaultSearchPath, boolean useSystemPath, String... alsoSearch) {
        command = Paths.get(command).getFileName().toString();
        Set<String> searched = new HashSet<>();
        for (String als : alsoSearch) {
            for (String path : splitUniqueNoEmpty(File.pathSeparatorChar, als)) {
                Path dir = Paths.get(path);
                Path file = dir.resolve(command);
                if (Files.exists(file) && Files.isExecutable(file)) {
                    return file;
                }
                searched.add(path);
            }
        }
        if (useSystemPath) {
            String systemPath = System.getenv("PATH");
            if (systemPath != null) {
                for (String path : splitUniqueNoEmpty(File.pathSeparatorChar, systemPath)) {
                    Path dir = Paths.get(path);
                    Path file = dir.resolve(command);
                    if (Files.exists(file) && Files.isExecutable(file)) {
                        return file;
                    }
                    searched.add(path);
                }
            }
        }
        if (useDefaultSearchPath) {
            for (String path : DEFAULT_SEARCH_PATH) {
                if (!searched.contains(path)) {
                    Path dir = Paths.get(path);
                    Path file = dir.resolve(command);
                    if (Files.exists(file) && Files.isExecutable(file)) {
                        return file;
                    }
                    searched.add(path);
                }
            }
        }
        return Paths.get(command);
    }

    /**
     * Duplicates Strings.splitUniqueNoEmpty to avoid a dependency. Splits a
     * string on a delimiter character, trimming resulting strings and returning
     * a set of unique non-empty strings.
     *
     * @param splitOn The character to split on
     * @param path A string
     * @return A set of unique, trimmed strings created from splitting the
     * passed string on the delimiter character
     */
    static Set<String> splitUniqueNoEmpty(char splitOn, String path) {
        if (path == null) {
            return Collections.emptySet();
        }
        Set<String> seqs = new LinkedHashSet<>();
        StringBuilder curr = new StringBuilder();
        int max = path.length();
        for (int i = 0; i < max; i++) {
            char c = path.charAt(i);
            if (c == splitOn || i == max - 1) {
                if (i == max - 1 && c != splitOn) {
                    curr.append(c);
                }
                String s = curr.toString().trim();
                if (s.length() > 0) {
                    seqs.add(s);
                }
                curr.setLength(0);
            } else {
                curr.append(c);
            }
        }
        return seqs;
    }

    /**
     * Get a predicate for filtering file streams which only lets files through.
     *
     * @return A predicate
     */
    public static Predicate<Path> filesOnly() {
        return pth -> !Files.isDirectory(pth);
    }

    /**
     * Get a predicate for filtering file streams which only lets folders
     * through.
     *
     * @return A predicate
     */
    public static Predicate<Path> foldersOnly() {
        return pth -> !Files.isDirectory(pth);
    }

    /**
     * Get a predicate for filtering file streams which only lets non-folders
     * with a give file extension delimited by <code>.</code> through.
     *
     * @return A predicate
     */
    public static Predicate<Path> byExtension(String ext) {
        Checks.notEmpty("ext", Checks.notNull("ext", ext));
        if (ext.charAt(0) != '.') {
            ext = '.' + ext;
        }
        final String extension = ext;
        return pth -> pth.toString().endsWith(extension);
    }

    /**
     * Find all files in the subtree under a folder which have the given file
     * extension.
     *
     * @param dir A folder
     * @param extension A file extension
     * @return A set of paths
     * @throws IOException If something goes wrong
     */
    public static Set<Path> find(Path dir, String extension) throws IOException {
        return find(dir, false, extension);
    }

    /**
     * Find all files in the subtree under a folder which have the given file
     * extension.
     *
     * @param dir A folder
     * @param relativize If true, returned paths will be relative to the passed
     * folder
     * @param extension A file extension
     * @return A set of paths
     * @throws IOException If something goes wrong
     */
    public static Set<Path> find(Path dir, boolean relativize, String extension) throws IOException {
        Set<Path> result = new LinkedHashSet<>(24);
        Predicate<Path> test = filesOnly().and(byExtension(extension));
        int count = search(relativize, dir, false, test, result::add);
        return count == 0 ? Collections.emptySet() : result;
    }

    /**
     * Search the subtree of a directory for paths.
     *
     * @param relativize If true, pass the portion of the path relative to the
     * folder, not the absolute path of each match.
     * @param dir The folder
     * @param followLinks If true, follow symlinks
     * @param predicate A predicate to determine which are paths are passed to
     * the consumer
     * @param consumer A consumer to receive matches
     * @return The number of matches which were passed to the consumer
     * @throws IOException If something goes wrong
     */
    public static int search(boolean relativize, Path dir, boolean followLinks, Predicate<? super Path> predicate, Consumer<? super Path> consumer) throws IOException {
        int[] count = new int[1];
        try (Stream<Path> all = Files.walk(dir, followLinks ? FOLLOW_LINKS : NO_FV_OPTIONS)) {
            all.filter(predicate).forEach(path -> {
                if (relativize) {
                    path = dir.relativize(path);
                }
                consumer.accept(path);
                count[0]++;
            });
        }
        return count[0];
    }

    private FileUtils() {
        throw new AssertionError();
    }
}
