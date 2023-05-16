/*
 * The MIT License
 *
 * Copyright 2023 Mastfrog Technologies.
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
package com.mastfrog.bits.large;

import java.util.ServiceLoader;
import java.util.function.LongFunction;

/**
 *
 * @author Tim Boudreau
 */
public abstract class OffHeapLongArrayFactory implements LongFunction<LongArray> {

    private static int pageSize = -1;

    public static LongArray get(long value) {
        for (OffHeapLongArrayFactory f : ServiceLoader.load(OffHeapLongArrayFactory.class)) {
            return f.apply(value);
        }
        throw new IllegalStateException("No instance of OffHeapLongFactory installed in "
                + "META-INF/services - perhaps you need a dependency on bits-unsafe?");
    }

    /**
     * Allow the implementation to hint at the system page size (eg.with
     * sun.misc.unsafe.Unsafe.pageSize()).
     *
     * @return A value &gt; zero if the value can be obtained
     */
    protected int systemPageSize() {
        return 0;
    }

    public static int pageSize(int ifUnavailable) {
        int sz = pageSize();
        return sz <= 0 ? ifUnavailable : sz;
    }

    private static int pageSize() {

        if (pageSize == -1) {
            for (OffHeapLongArrayFactory f : ServiceLoader.load(OffHeapLongArrayFactory.class)) {
                int result = f.systemPageSize();
                if (result > 0) {
                    return pageSize = result;
                }
            }
            pageSize = 0;
        }
        return pageSize;
    }

}
