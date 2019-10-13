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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Tim Boudreau
 */
final class UnixPaths {

    static boolean unixChecked;
    static boolean systemUnixPaths;

    static boolean isNativeUnixPaths() {
        if (true) {
            return false;
        }
        if (unixChecked) {
            return systemUnixPaths;
        }
        Path pth = Paths.get("com", "foo");
        String str = pth.toString();
        boolean result = "UnixPath".equals(pth.getClass().getSimpleName())
                && str.indexOf('/') > 0 && str.indexOf('\\') < 0;
        unixChecked = true;
        return systemUnixPaths = result;
    }

    static UnixPath empty() {
        if (isNativeUnixPaths()) {
            return WrappedUPath.EMPTY_PATH;
        } else {
            return ComponentUPath.EMPTY_PATH;
        }
    }

    static UnixPath emptyAbsolute() {
        if (isNativeUnixPaths()) {
            return WrappedUPath.EMPTY_PATH_ABS;
        } else {
            return ComponentUPath.EMPTY_PATH_ABS;
        }
    }

    static UnixPath get(String name) {
        if (isNativeUnixPaths()) {
            return WrappedUPath.of(Paths.get(name));
        } else {
            return ComponentUPath.of(name);
        }
    }

    static UnixPath get(String name, String... moreNames) {
        if (isNativeUnixPaths()) {
            return WrappedUPath.of(Paths.get(name, moreNames));
        } else {
            return ComponentUPath.ofUnixPaths(name, moreNames);
        }
    }

    static UnixPath get(Path name) {
        if (isNativeUnixPaths()) {
            return WrappedUPath.of(name);
        } else {
            return ComponentUPath.of(name);
        }
    }

    static UnixPath get(Path name, Path... moreNames) {
        if (isNativeUnixPaths()) {
            String[] s = new String[moreNames.length];
            for (int i = 0; i < moreNames.length; i++) {
                s[i] = moreNames[i].toString();
            }
            return WrappedUPath.of(Paths.get(name.toString(), s));
        } else {
            return ComponentUPath.of(name, moreNames);
        }
    }
}
