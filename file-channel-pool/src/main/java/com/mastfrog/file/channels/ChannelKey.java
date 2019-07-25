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
package com.mastfrog.file.channels;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;

/**
 * Key which incoporates a file path and options in order to cache
 * file channels.
 *
 * @author Tim Boudreau
 */
final class ChannelKey {

    final Path path;
    final Set<StandardOpenOption> opts;
    final boolean randomAccess;
    final FileAttribute[] attrs;

    ChannelKey(Path path, Set<StandardOpenOption> opts, boolean randomAccess, FileAttribute... attrs) {
        this.path = path;
        this.opts = opts;
        this.randomAccess = randomAccess;
        this.attrs = attrs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(path.getFileName().toString()).append(':');
        opts.forEach((o) -> {
            sb.append(o.name()).append(';');
        });
        if (randomAccess) {
            sb.append("-RA");
        }
        return sb.toString();
    }

    public boolean isAppend() {
        return opts.contains(StandardOpenOption.APPEND);
    }

    public boolean isWrite() {
        return opts.contains(StandardOpenOption.WRITE) || opts.contains(StandardOpenOption.APPEND);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.path);
        hash = 53 * hash + Objects.hashCode(this.opts);
        hash = 53 * hash + (this.randomAccess ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChannelKey other = (ChannelKey) obj;
        if (this.randomAccess != other.randomAccess) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return Objects.equals(this.opts, other.opts);
    }
}
