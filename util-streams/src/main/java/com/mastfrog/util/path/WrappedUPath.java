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
package com.mastfrog.util.path;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

/**
 *
 * @author Tim Boudreau
 */
final class WrappedUPath implements UnixPath {

    private final Path delegate;
    static final WrappedUPath EMPTY_PATH = WrappedUPath.of(Paths.get(""));
    static final WrappedUPath EMPTY_PATH_ABS = WrappedUPath.of(Paths.get("/"));

    WrappedUPath(Path delegate) {
        this.delegate = delegate;
        assert !(delegate instanceof WrappedUPath) : "double-wrapped";
    }

    static WrappedUPath of(Path p) {
        if (p instanceof WrappedUPath) {
            return (WrappedUPath) p;
        } else if (p != null) {
//            return new WrappedUPath(p.normalize());
            return new WrappedUPath(p);
        } else {
            return null;
        }
    }

    @Override
    public UnixPath toRelativePath() {
        if (isAbsolute()) {
            return new WrappedUPath(Paths.get(File.separator)
                    .relativize(delegate));
        } else {
            return this;
        }
    }

    @Override
    public FileSystem getFileSystem() {
        return delegate.getFileSystem();
    }

    @Override
    public boolean isAbsolute() {
        return delegate.isAbsolute();
    }

    @Override
    public UnixPath getRoot() {
        return of(delegate.getRoot());
    }

    @Override
    public UnixPath getFileName() {
        return of(delegate.getFileName());
    }

    @Override
    public UnixPath getParent() {
        return of(delegate.getParent());
    }

    @Override
    public int getNameCount() {
        return delegate.getNameCount();
    }

    @Override
    public UnixPath getName(int index) {
        return of(delegate.getName(index));
    }

    @Override
    public WrappedUPath subpath(int beginIndex, int endIndex) {
        return of(delegate.subpath(beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(Path other) {
        return delegate.startsWith(other);
    }

    @Override
    public boolean startsWith(String other) {
        return delegate.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
        return delegate.endsWith(other);
    }

    @Override
    public boolean endsWith(String other) {
        return delegate.endsWith(other);
    }

    @Override
    public UnixPath normalize() {
        return of(delegate.normalize());
    }

    @Override
    public UnixPath resolve(Path other) {
        return of(delegate.resolve(other));
    }

    @Override
    public UnixPath resolve(String other) {
        return of(delegate.resolve(other));
    }

    @Override
    public UnixPath resolveSibling(Path other) {
        return of(delegate.resolveSibling(other));
    }

    @Override
    public UnixPath resolveSibling(String other) {
        return of(delegate.resolveSibling(other));
    }

    @Override
    public UnixPath relativize(Path other) {
        return of(delegate.relativize(other));
    }

    @Override
    public URI toUri() {
        return delegate.toUri();
    }

    @Override
    public UnixPath toAbsolutePath() {
        return of(delegate.toAbsolutePath());
    }

    @Override
    public UnixPath toRealPath(LinkOption... options) throws IOException {
        return of(delegate.toRealPath(options));
    }

    @Override
    public File toFile() {
        return delegate.toFile();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return delegate.register(watcher, events, modifiers);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return delegate.register(watcher, events);
    }

    @Override
    public Iterator<Path> iterator() {
        return new It(delegate.iterator());
    }

    @Override
    public int compareTo(Path other) {
        // UnixPath will throw an exception if compared with the wrong type
//        return delegate.compareTo(other instanceof WrappedUPath
//                ? ((WrappedUPath) other).delegate : other);
        return delegate.toString().compareTo(other.toString());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public Path toNativePath() {
        return delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof WrappedUPath) {
            WrappedUPath other = (WrappedUPath) o;
            return delegate.equals(other.delegate);
        } else if (o instanceof Path) {
            return delegate.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    static class It implements Iterator<Path> {

        private final Iterator<Path> orig;

        public It(Iterator<Path> orig) {
            this.orig = orig;
        }

        @Override
        public boolean hasNext() {
            return orig.hasNext();
        }

        @Override
        public Path next() {
            return of(orig.next());
        }
    }
}
