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

import static com.mastfrog.util.preconditions.Checks.notNull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Platform-independent implementation of java.nio.file.Path allowing for
 * consistency across Unix and Windows; useful for doing path transforms and as
 * keys where it is imperative that the path separator be '/'
 * <i>no matter what</i>. Has some caveats - particularly cannot be mixed with
 * system path instances in a collection which will be sorted, because UnixPath
 * assumes all paths are UnixPaths; may fail or do the wrong thing if passed
 * directly to java.nio.Files. Used in particular as path keys in the JFS
 * filesystem for NetBeans antlr support.
 * <p>
 * The behavior of instances retrieved from the factory methods should be
 * identical with sun.nio.UnixPath, which is not public in the JDK.
 * </p>
 * Note the iterator will contain instances of UnixPath, though the API does not
 * allow them to be typed as such.
 *
 * @author Tim Boudreau
 */
public interface UnixPath extends Path {

    static UnixPath empty() {
        return UnixPaths.empty();
    }

    static UnixPath emptyAbsolute() {
        return UnixPaths.emptyAbsolute();
    }

    static UnixPath get(String name) {
        return UnixPaths.get(notNull("name", name));
    }

    static UnixPath get(String name, String... moreNames) {
        return UnixPaths.get(notNull("name", name), moreNames);
    }

    static UnixPath get(Path path) {
        return path instanceof UnixPath ? (UnixPath) path : UnixPaths.get(notNull("path", path));
    }

    static UnixPath get(Path first, Path... morePaths) {
        return UnixPaths.get(notNull("first", first), morePaths);
    }

    UnixPath toRelativePath();

    @Override
    UnixPath toAbsolutePath();

    @Override
    UnixPath relativize(Path other);

    @Override
    UnixPath resolveSibling(String other);

    @Override
    UnixPath resolveSibling(Path other);

    @Override
    UnixPath resolve(String other);

    @Override
    UnixPath resolve(Path other);

    @Override
    UnixPath normalize();

    @Override
    UnixPath subpath(int beginIndex, int endIndex);

    @Override
    UnixPath getName(int index);

    @Override
    UnixPath getParent();

    @Override
    UnixPath getRoot();

    @Override
    UnixPath getFileName();

    /**
     * Convert this path to the native equivalent retrieved from Paths.get() (if
     * this involves Windows drive letters, all bets are off).
     *
     * @return A native path suitable for use with Files
     */
    default Path toNativePath() {
        int nc = getNameCount();
        StringBuilder sb = new StringBuilder(nc * 32);
        if (isAbsolute()) {
            sb.append(File.separatorChar);
        }
        for (int i = 0; i < nc; i++) {
            Path nm = getName(i);
            if (i != nc - 1) {
                sb.append(File.separatorChar);
            }
        }
        return Paths.get(sb.toString());
    }

    /**
     * Determine if this path contains no "." or ".." elements.
     *
     * @return True if the path is normalized
     */
    default boolean isNormalized() {
        for (Path p : this) {
            if (".".equals(p.toString()) || "..".equals(p.toString())) {
                return false;
            }
        }
        return true;
    }

    default int visitNames(Consumer<String> consumer) {
        int result = 0;
        for (Path p : this) {
            consumer.accept(p.toString());
            result++;
        }
        return result;
    }

    default String extension() {
        int ct = getNameCount();
        if (ct == 0) {
            return "";
        }
        String nm = getName(ct - 1).toString();
        if (".".equals(nm) || "..".equals(nm)) {
            UnixPath up = normalize();
            ct = up.getNameCount();
            if (ct == 0) {
                return "";
            }
            nm = up.getName(ct - 1).toString();
        }
        int ix = nm.lastIndexOf('.');
        if (ix < 1 || ix == nm.length() - 1) {
            return "";
        }
        return nm.substring(ix + 1);
    }

    default String rawName() {
        int ct = getNameCount();
        if (ct == 0) {
            return "";
        }
        String nm = getName(ct - 1).toString();
        if (".".equals(nm) || "..".equals(nm)) {
            UnixPath up = normalize();
            ct = up.getNameCount();
            if (ct == 0) {
                return "";
            }
            nm = up.getName(ct - 1).toString();
        }
        int ix = nm.lastIndexOf('.');
        if (ix < 1 || ix == nm.length() - 1) {
            return nm;
        }
        return nm.substring(0, ix);
    }

    default boolean isExtension(String ext) {
        if (ext == null || ext.length() == 0) {
            return false;
        }
        if (ext.charAt(0) == '.') {
            ext = ext.substring(1);
        }
        return ext.equals(extension());
    }

    default String toString(char separator) {
        StringBuilder sb = new StringBuilder();
        visitNames(name -> {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(name);
        });
        return sb.toString();
    }

    default boolean isEmpty() {
        int nc = getNameCount();
        switch (nc) {
            case 0:
                return true;
            case 1:
                return rawName().isEmpty();
            default:
                return false;
        }
    }
}
