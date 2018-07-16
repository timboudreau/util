/*
 * The MIT License
 *
 * Copyright 2018 Tim Boudreau.
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

import java.io.File;
import java.io.IOException;

/**
 * Unique identifiers.
 *
 * @author Tim Boudreau
 * @deprecated All methods delegate to com.mastfrog.util.strings.UniqueIDs
 */
@Deprecated
public class UniqueIDs {

    private final com.mastfrog.util.strings.UniqueIDs delegate;

    private UniqueIDs(com.mastfrog.util.strings.UniqueIDs delegate) {
        this.delegate = delegate;
    }

    public UniqueIDs(File f) throws IOException {
        this(new com.mastfrog.util.strings.UniqueIDs(f));
    }

    public static com.mastfrog.util.strings.UniqueIDs createNoFile() throws IOException {
        return com.mastfrog.util.strings.UniqueIDs.createNoFile();
    }

    public static com.mastfrog.util.strings.UniqueIDs noFile() {
        return com.mastfrog.util.strings.UniqueIDs.noFile();
    }

    public String uniqueToVmString() {
        return delegate.uniqueToVmString();
    }

    public String newId() {
        return delegate.newId();
    }

    public String toString() {
        return delegate.toString();
    }

    public String newRandomString() {
        return delegate.newRandomString();
    }

    public String newRandomString(int count) {
        return delegate.newRandomString(count);
    }

    public boolean recognizes(String id) {
        return delegate.recognizes(id);
    }

    public boolean knows(String id) {
        return delegate.knows(id);
    }
}
