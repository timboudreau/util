/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
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
package com.mastfrog.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utilities methods for working with input and output streams.
 */
public final class Streams {

    /**
     * Writes the input stream to the output stream. Input is done without a
     * Reader object, meaning that the input is copied in its raw form.
     *
     * @param in The input stream
     * @param out The output stream
     * @param bufferSize The number of bytes to fetch in a single loop
     * @return Number of bytes copied from one stream to the other
     * @throws IOException
     */
    public static int copy(final InputStream in, final OutputStream out, int bufferSize)
            throws IOException {
        Checks.nonZero("bufferSize", bufferSize);
        Checks.nonNegative("bufferSize", bufferSize);
        final byte[] buffer = new byte[bufferSize];
        int bytesCopied = 0;
        for (;;) {
            int byteCount = in.read(buffer, 0, buffer.length);
            if (byteCount <= 0) {
                break;
            } else {
                out.write(buffer, 0, byteCount);
                bytesCopied += byteCount;
            }
        }
        return bytesCopied;
    }

    /**
     * Writes the input stream to the output stream. Input is done without a
     * Reader object, meaning that the input is copied in its raw form.
     *
     * @param in The input stream
     * @param out The output stream
     * @return Number of bytes copied from one stream to the other
     * @throws IOException
     */
    public static int copy(final InputStream in, final OutputStream out)
            throws IOException {
        final byte[] buffer = new byte[4096];
        int bytesCopied = 0;
        for (;;) {
            int byteCount = in.read(buffer, 0, buffer.length);
            if (byteCount <= 0) {
                break;
            } else {
                out.write(buffer, 0, byteCount);
                bytesCopied += byteCount;
            }
        }
        return bytesCopied;
    }

    /**
     * Writes the input stream to <i>two</i> output streams.
     *
     * @param in The input stream
     * @param out The output stream
     * @param otherOut Another output stream
     * @return the number of bytes copied
     * @throws IOException if something goes wrong
     */
    public static int copy(final InputStream in, final OutputStream out, final OutputStream otherOut)
            throws IOException {
        final byte[] buffer = new byte[4096];
        int bytesCopied = 0;
        while (true) {
            int byteCount = in.read(buffer, 0, buffer.length);
            if (byteCount <= 0) {
                break;
            }
            out.write(buffer, 0, byteCount);
            otherOut.write(buffer, 0, byteCount);
            bytesCopied += byteCount;
        }
        return bytesCopied;
    }

    public static String readUTF8String(InputStream in) throws IOException {
        return readString(in, "UTF-8");
    }

    /**
     * Reads a stream as a string.
     *
     * @param in The input stream
     * @return The string
     * @throws IOException
     */
    public static String readString(final InputStream in) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(in));
        try {
            return readString(r);
        } finally {
            r.close();
        }
    }

    /**
     * Read a string with a fixed buffer size
     * @param in An input stream
     * @param bufferSize A buffer size, non-negative;  if zero, no buffering
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, int bufferSize) throws IOException {
        Checks.nonNegative("bufferSize", bufferSize);
        Reader r = bufferSize <= 0 ? new InputStreamReader(in) : new BufferedReader(new InputStreamReader(in), bufferSize);
        try {
            return readString(r);
        } finally {
            r.close();
        }
    }

    /**
     * Read a string with a specified charset and a fixed buffer size
     * @param in An input stream
     * @param charset A character set
     * @param bufferSize A buffer size, non-negative;  if zero, no buffering
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, String charset, int bufferSize) throws IOException {
        Checks.nonNegative("bufferSize", bufferSize);
        Reader r = bufferSize == 0 ? new InputStreamReader(in) : new BufferedReader(new InputStreamReader(in), bufferSize);
        try {
            return readString(r);
        } finally {
            r.close();
        }
    }

    /**
     * Read a string with a specified charset
     * @param in An input stream
     * @param charset A character set
     * @return A string
     * @throws IOException if something goes wrong
     */
    public static String readString(final InputStream in, String charset) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(in, Charset.forName(charset)));
        try {
            return readString(r);
        } finally {    
            r.close();
        }
    }

    /**
     * Reads SQL from an input stream. Returns statements separated by
     * semi-colons, skipping comments. The input stream will be closed by this
     * invocation.
     *
     * @param is input stream
     * @return SQL array of sql strings, possibly empty, never null
     */
    public static String[] readSql(InputStream is) {
        Checks.notNull("input stream is", is);
        StringBuilder bldr = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#") && !line.startsWith("--")) {
                        bldr.append(line);
                    }
                }
            } finally {
                is.close();
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<String> l = new ArrayList<>();
        StringTokenizer t = new StringTokenizer(bldr.toString(), ";");
        while (t.hasMoreTokens()) {
            String cmd = t.nextToken();
            l.add(cmd);
        }
        return l.toArray(new String[l.size()]);
    }

    /**
     * Reads a string using a character encoding.
     *
     * @param in The input
     * @param encoding The character encoding of the input data
     * @return The string
     * @throws IOException
     */
    public static String readString(final InputStream in,
            final CharSequence encoding) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(in, encoding.toString()));
        try {
            return readString(r);
        } finally {
            r.close();
        }
    }

    /**
     * Reads all input from a reader into a string.
     *
     * @param in The input
     * @return The string
     * @throws IOException
     */
    public static String readString(final Reader in) throws IOException {
        final StringBuffer buffer = new StringBuffer(2048);
        int value;

        while ((value = in.read()) != -1) {
            buffer.append((char) value);
        }

        return buffer.toString();
    }
    
    static class NamedInputStream extends FilterInputStream {
        private final String name;
        public NamedInputStream(Object src, InputStream in) {
            super(in);
            name = src + "";
        }
        
        @Override
        public String toString() {
            return name + " - " + super.in;
        }
    }

    /**
     * Locate an input stream for the provided location, possibly multiple if it
     * is a resource from the classpath that can be found at multiple locations.
     *
     * @param location either a URL or a path that can be found on the class
     * path or as a file
     * @return input streams or null if not found
     */
    public static InputStream[] locate(String location) {
        Checks.notNull("location", location);
        List<InputStream> inputStreams = new ArrayList<InputStream>();
        try {
            if (location.contains("://") || location.startsWith("file:/")) {
                URL url = new URL(location);
                NamedInputStream i = new NamedInputStream(url, url.openStream());
                inputStreams.add(i);
            } else {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = Streams.class.getClassLoader();
                }
                Enumeration<URL> i = loader.getResources(location);
                if (!i.hasMoreElements()) {
                    i = loader.getResources(location);
                }
                if (i.hasMoreElements()) {
                    while (i.hasMoreElements()) {
                        URL url = i.nextElement();
                        inputStreams.add(new NamedInputStream(url, url.openStream()));
                    }
                } else {
                    // If the InputStream doesn't work in the classpath,
                    // give it a go as an absolute path to a file.
                    try {
                        inputStreams.add(new NamedInputStream(location, new FileInputStream(location)));
                    } catch (IOException ex) {
                        // eat up exception, as the check on is after
                        // this takes care of that
                    }
                }
            }
            return inputStreams.isEmpty() ? null : inputStreams.toArray(new InputStream[inputStreams.size()]);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void writeString(String s, File to) throws IOException {
        try (FileOutputStream out = new FileOutputStream(to)) {
            out.write(s.getBytes(Charset.forName("UTF-8")));
        }
    }
    
    /**
     * Get a ByteBuffer as an InputStream. The passed buffer will be wrapped as
     * a read-only buffer. The position, mark and limit of the passed buffer
     * will remain unmodified, and the returned InputStream will read the byte
     * buffer from position 0, not its current position.
     *
     * @param buf A ByteBuffer
     * @return An InputStream for reading the byte buffer
     */
    public static InputStream asInputStream(ByteBuffer buf) {
        ByteBuffer use = buf.asReadOnlyBuffer();
        use.rewind();
        return new ByteBufferInputStream(use);
    }

    public static InputStream asInputStream(ReadableByteChannel channel) {
        if (channel instanceof InputStreamByteChannel) {
            return ((InputStreamByteChannel) channel).in;
        }
        return new ByteChannelInputStream(channel);
    }

    public static ReadableByteChannel asByteChannel(InputStream in) {
        if (in instanceof ByteChannelInputStream) {
            return ((ByteChannelInputStream) in).channel;
        }
        return new InputStreamByteChannel(in);
    }

    public static ByteBuffer asByteBuffer(InputStream in) throws IOException {
        if (in instanceof ByteBufferInputStream) {
            ByteBuffer res = ((ByteBufferInputStream) in).buf.asReadOnlyBuffer();
            res.rewind();
            return res;
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(2048);
        int pos = 0;
        int amt;
        byte[] b = new byte[2048];
        while ((amt = in.read(b)) > 0) {
            if (pos + amt > buf.capacity()) {
                ByteBuffer nue = ByteBuffer.allocateDirect(buf.capacity() + 2048);
                buf.rewind();
                nue.put(buf);
                buf = nue;
            }
            pos += amt;
            buf.put(b);
        }
        buf.rewind();
        buf.limit(pos);
        return buf;
    }

    public static OutputStream nullOutputStream() {
        return new NullOutputStream();
    }

    private static final class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            //do nothing
        }

        @Override
        public void close() throws IOException {
            //do nothing
        }

        @Override
        public void flush() throws IOException {
            //do nothing
        }

        @Override
        public void write(byte[] b) throws IOException {
            //do nothing
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            //do nothing
        }
    }

    public static InputStream asInputStream(Iterable<ByteBuffer> buffers) {
        return new ByteBuffersInputStream(buffers.iterator());
    }

    private static final class ByteBuffersInputStream extends InputStream {

        private final Iterator<ByteBuffer> iter;
        private ByteBuffer curr;

        ByteBuffersInputStream(Iterator<ByteBuffer> iter) {
            this.iter = iter;
            curr = iter.hasNext() ? iter.next() : ByteBuffer.allocate(0);
        }

        private ByteBuffer buf() {
            while (curr != null && curr.remaining() == 0) {
                if (curr.remaining() == 0) {
                    curr = iter.hasNext() ? iter.next() : null;
                }
            }
            return curr;
        }

        @Override
        public int read() throws IOException {
            ByteBuffer buf = buf();
            if (buf == null || buf.remaining() == 0) {
                return -1;
            }
            return buf.get();
        }

        @Override
        public int read(byte[] b) throws IOException {
            int position = 0;
            for (ByteBuffer buf = buf(); buf != null && buf.remaining() > 0; buf = buf()) {
                int oldPosition = buf.position();
                buf.get(b, position, b.length - position);
                position += buf.position() - oldPosition;
            }
            return position == 0 ? -1 : position;
        }
    }
    
    public static InputStream forByteBuffers(ByteBuffer... iter) {
        return new ByteBufferCollectionInputStream(iter);
    }

    public static InputStream forByteBuffers(Iterable<ByteBuffer> iter) {
        return new ByteBufferCollectionInputStream(iter);
    }

    private static final class ByteBufferInputStream extends InputStream {

        private final ByteBuffer buf;

        ByteBufferInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public int read() throws IOException {
            return buf.remaining() == 0 ? -1 : buf.get();
        }

        @Override
        public int read(byte[] b) throws IOException {
            int rem = buf.remaining();
            int result = Math.min(rem, b.length);
            buf.get(b, 0, result);
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (off + len > b.length) {
                throw new IOException("offset + length=" + (off + len) + " but array length is " + b.length);
            }
            int rem = buf.remaining();
            if (rem == 0) {
                return -1;
            }
            int result = Math.min(len, rem);
            buf.get(b, off, result);
            return result;
        }
    }

    private static final class InputStreamByteChannel implements ReadableByteChannel {

        private final InputStream in;
        private boolean open = true;

        InputStreamByteChannel(InputStream in) {
            this.in = in;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            byte[] b = new byte[2048];
            int result = in.read(b);
            if (result > 0) {
                dst.put(b);
            }
            return result;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
            in.close();
        }
    }

    private static final class ByteChannelInputStream extends InputStream {

        private final ReadableByteChannel channel;

        ByteChannelInputStream(ReadableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public int read() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(1);
            channel.read(buf);
            buf.rewind();
            return buf.get();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ByteBuffer buf = ByteBuffer.wrap(b, off, len);
            return channel.read(buf);
        }

        @Override
        public int available() throws IOException {
            if (channel instanceof FileChannel) {
                FileChannel fc = (FileChannel) channel;
                return (int) Math.max(0L, fc.size() - fc.position());
            }
            return super.available();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                channel.close();
            }
        }
    }

    public static void copyFile(File orig, File nue, boolean create) throws IOException {
        if (orig == nue) {
            throw new IOException("Copying file to itself");
        }
        if (orig.isDirectory()) {
            throw new IllegalArgumentException(orig.getAbsolutePath() + " is a directory");
        }

        boolean newExists = nue.exists();
        if (newExists && !create) {
            throw new IOException(nue + " does not exist");
        }
        if (!newExists) {
            if (!nue.createNewFile()) {
                throw new IOException("Could not create file " + nue);
            }
        } else if (nue.isDirectory()) {
            throw new IllegalArgumentException("Copying file to a directory");
        }
        FileInputStream in = new FileInputStream(orig);
        try {
            FileChannel inChannel = in.getChannel();
            try {
                FileOutputStream out = new FileOutputStream(nue);
                try {
                    FileChannel outChannel = out.getChannel();
                    try {
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                    } finally {
                        outChannel.close();
                    }
                } finally {
                    out.close();
                }
            } finally {
                inChannel.close();
            }

        } finally {
            in.close();
        }
    }

    public static InputStream streamForURL(URL url) {
        return new LazyURLStream(url);
    }

    private static final class LazyURLStream extends InputStream {

        private final java.net.URL url;
        private InputStream stream;

        LazyURLStream(java.net.URL url) {
            this.url = url;
        }

        private synchronized InputStream stream() throws IOException {
            if (stream == null) {
                stream = url.openStream();
            }
            return stream;
        }

        @Override
        public synchronized int read() throws IOException {
            return stream().read();
        }

        @Override
        public synchronized int available() throws IOException {
            return stream().available();
        }

        @Override
        public synchronized void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
            super.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                stream().mark(readlimit);
            } catch (IOException ex) {
                Logger.getLogger(LazyURLStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public synchronized boolean markSupported() {
            try {
                return stream().markSupported();
            } catch (IOException ex) {
                Logger.getLogger(LazyURLStream.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        @Override
        public synchronized int read(byte[] b) throws IOException {
            return stream().read(b);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            return stream().read(b, off, len);
        }

        @Override
        public synchronized void reset() throws IOException {
            if (stream != null) {
                stream.reset();
            }
        }

        @Override
        public synchronized long skip(long n) throws IOException {
            return stream().skip(n);
        }
    }

    public static PrintStream teeSystemOut(PrintStream other) {
        return tee(System.out, other);
    }

    public static PrintStream tee(PrintStream... streams) {
        return new MergePrintStream(streams);
    }

    private static final class MergePrintStream extends PrintStream {

        private final PrintStream[] p;

        MergePrintStream(PrintStream... p) {
            super(Streams.nullOutputStream());
            this.p = p;
        }

        @Override
        public PrintStream append(CharSequence csq) {
            for (PrintStream pp : p) {
                pp.append(csq);
            }
            return this;
        }

        @Override
        public PrintStream append(CharSequence csq, int start, int end) {
            for (PrintStream pp : p) {
                pp.append(csq, start, end);
            }
            return this;
        }

        @Override
        public PrintStream append(char c) {
            for (PrintStream pp : p) {
                pp.append(c);
            }
            return this;
        }

        @Override
        public boolean checkError() {
            boolean result = false;
            for (PrintStream pp : p) {
                result |= pp.checkError();
            }
            return result;

        }

        @Override
        protected void clearError() {
            super.clearError();
        }

        @Override
        public void close() {
            for (PrintStream pp : p) {
                pp.close();
            }
        }

        @Override
        public void flush() {
            for (PrintStream pp : p) {
                pp.flush();
            }
        }

        @Override
        public PrintStream format(String format, Object... args) {
            for (PrintStream pp : p) {
                pp.format(format, args);
            }
            return this;
        }

        @Override
        public PrintStream format(Locale l, String format, Object... args) {
            for (PrintStream pp : p) {
                pp.format(l, format, args);
            }
            return this;
        }

        @Override
        public void print(boolean b) {
            for (PrintStream pp : p) {
                pp.print(b);
            }
        }

        @Override
        public void print(char c) {
            for (PrintStream pp : p) {
                pp.print(c);
            }
        }

        @Override
        public void print(int i) {
            for (PrintStream pp : p) {
                pp.print(i);
            }
        }

        @Override
        public void print(long l) {
            for (PrintStream pp : p) {
                pp.print(l);
            }
        }

        @Override
        public void print(float f) {
            for (PrintStream pp : p) {
                pp.print(f);
            }
        }

        @Override
        public void print(double d) {
            for (PrintStream pp : p) {
                pp.print(d);
            }
        }

        @Override
        public void print(char[] s) {
            for (PrintStream pp : p) {
                pp.print(s);
            }
        }

        @Override
        public void print(String s) {
            for (PrintStream pp : p) {
                pp.print(s);
            }
        }

        @Override
        public void print(Object obj) {
            for (PrintStream pp : p) {
                pp.print(obj);
            }
        }

        @Override
        public PrintStream printf(String format, Object... args) {
            for (PrintStream pp : p) {
                pp.printf(format, args);
            }
            return this;
        }

        @Override
        public PrintStream printf(Locale l, String format, Object... args) {
            for (PrintStream pp : p) {
                pp.printf(l, format, args);
            }
            return this;
        }

        @Override
        public void println() {
            for (PrintStream pp : p) {
                pp.println();
            }
        }

        @Override
        public void println(boolean x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(char x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(int x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(long x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(float x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(double x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(char[] x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(String x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        public void println(Object x) {
            for (PrintStream pp : p) {
                pp.println(x);
            }
        }

        @Override
        protected void setError() {
            try {
                Method m = PrintStream.class.getDeclaredMethod("setError");
                m.setAccessible(true);
                for (PrintStream pp : p) {
                    try {
                        m.invoke(pp);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void write(int b) {
            for (PrintStream pp : p) {
                pp.write(b);
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            for (PrintStream pp : p) {
                pp.write(buf, off, len);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            for (PrintStream pp : p) {
                pp.write(b);
            }
        }
    }

    /**
     * Private to prevent instantiation.
     */
    private Streams() {
    }

    private static String stripTrailingSeparator(String path) {
        if (path.charAt(path.length() - 1) == File.separatorChar) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Create a relative symbolic link between two files
     *
     * @param original The original
     * @param target The target, which should not yet exist
     * @throws IOException if something goes wrong
     */
    public static void link(File original, File target) throws IOException {
        //XXX JDK 7 NIO Path would be nice
        String rp = getRelativePath(original.getAbsolutePath(), target.getAbsolutePath());
        String[] cmd = new String[]{"ln", "-s", rp, target.getName()};
        ProcessBuilder b = new ProcessBuilder();
        b.directory(target.getParentFile());
        b.command(cmd);
        Process p = b.start();
        try {
            p.waitFor();
            p.destroy();
        } catch (InterruptedException ex) {
            Logger.getLogger(Streams.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static String getRelativePath(String targetPath, String basePath) throws IOException {
        String pathSeparator = File.separator;
        // Normalize the paths
        String tp = stripTrailingSeparator(targetPath);
        String bp = stripTrailingSeparator(basePath);

        String[] base = bp.split(Pattern.quote(pathSeparator));
        String[] target = tp.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuilder common = new StringBuilder();

        int distanceToCommonParent = 0;
        while (distanceToCommonParent < target.length && distanceToCommonParent < base.length
                && target[distanceToCommonParent].equals(base[distanceToCommonParent])) {
            common.append(target[distanceToCommonParent]).append(pathSeparator);
            distanceToCommonParent++;
        }

        if (distanceToCommonParent == 0) {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativized.
            throw new IOException("No common parent: '" + tp + "' and '" + bp
                    + "'");
        }
        boolean baseIsFile = true;
        File baseResource = new File(bp);
        if (baseResource.exists()) {
            baseIsFile = baseResource.isFile();
        } else if (basePath.endsWith(pathSeparator)) {
            baseIsFile = false;
        }
        StringBuilder relative = new StringBuilder();
        if (base.length != distanceToCommonParent) {
            int numDirsUp = baseIsFile
                    ? base.length - distanceToCommonParent - 1
                    : base.length - distanceToCommonParent;

            for (int i = 0; i < numDirsUp; i++) {
                relative.append("..").append(pathSeparator);
            }
        }
        relative.append(tp.substring(common.length()));
        return relative.toString();
    }
}
