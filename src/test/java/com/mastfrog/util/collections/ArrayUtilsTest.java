/*
 * The MIT License
 *
 * Copyright 2017 Tim Boudreau.
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
package com.mastfrog.util.collections;

import static com.mastfrog.util.collections.ArrayUtils.concatenate;
import static com.mastfrog.util.collections.ArrayUtils.concatenateAll;
import static com.mastfrog.util.collections.ArrayUtils.dedup;
import static com.mastfrog.util.collections.ArrayUtils.dedupByType;
import static com.mastfrog.util.collections.ArrayUtils.emptyForNull;
import static com.mastfrog.util.collections.ArrayUtils.flatten;
import java.util.Arrays;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ArrayUtilsTest {

    @Test
    public void testConcatAndMore() {
        Object[] a = new Object[]{1, "hello"};
        Object[] b = new Object[]{1, "goodbye"};

        Object[] c = concatenate(a, b);

        assertArrayEquals(new Object[]{1, "hello", 1, "goodbye"}, c);

        Object[] d = dedup(c);
        assertArrayEquals(new Object[]{1, "hello", "goodbye"}, d);

        Object[] e = dedupByType(c);
        assertArrayEquals(new Object[]{1, "goodbye"}, e);

        Object[] a1 = concatenate(a, new Object[]{new Object[]{"b", "c", new Object[]{"d", "e"}}, "f", new Object[]{"g", "h"}});

        Object[] b1 = flatten(a1);
        assertArrayEquals(new Object[]{1, "hello", "b", "c", "d", "e", "f", "g", "h"}, b1);

        int[] n1 = ArrayUtils.<Integer>toIntArray(Arrays.<Integer>asList(1, 2, 3, 4, 5, 6));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, n1);

        long[] n2 = ArrayUtils.<Long>toLongArray(Arrays.<Long>asList(1L, 2L, 3L, 4L, 5L, 6L));
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L, 5L, 6L}, n2);
    }

    @Test
    public void testMultiConcatenate() {
        Object[] a = new Object[]{"a", "b", "c"};
        Object[] b = new Object[]{"d", "e", "f"};
        Object[] c = new Object[]{"g", "h", "i"};
        Object[] d = new Object[]{"j", "k", "l"};

        Object[] cc = concatenateAll(a, b, c, null, d);

        assertArrayEquals(new Object[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"}, cc);
    }

    public void testEmptyForNull() {
        Object[] a = new Object[]{"a", "b", "c"};
        Object[] b = emptyForNull(a);
        assertSame(a, b);

        Object[] c = emptyForNull(null);
        assertArrayEquals(new Object[0], c);
    }

}
