/*
 * The MIT License
 *
 * Copyright 2018 tim.
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

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import static com.mastfrog.util.collections.SetOfTest.Foo.BAR;
import static com.mastfrog.util.collections.SetOfTest.Foo.BAZ;
import static com.mastfrog.util.collections.SetOfTest.Foo.SKIDDOO;
import static com.mastfrog.util.collections.SetOfTest.Foo.WOOGLE;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SetOfTest {

    @Test
    public void test() {
        Set<Foo> a = setOf(BAR, BAZ, SKIDDOO);
        assertTrue(a.getClass().getName(), a instanceof EnumSet<?>);

        Set<Foo> s = setOf(BAR, BAZ, SKIDDOO, WOOGLE);
        assertTrue(s.getClass().getName(), s instanceof EnumSet<?>);

        Set<String> ss = setOf("hello", "world");
        assertTrue(ss.getClass().getName(), ss instanceof ArraySet<?>);

        Set<String> ss2 = setOf("hello", "you", "world");
        assertTrue(ss2.getClass().getName(), ss2 instanceof ArrayBinarySet<?>);

        Set<Integer> ss3 = setOf(1, 2, 3, 4, 5, 6, 7, 8);
        assertTrue(ss3.getClass().getName(), ss3 instanceof ArrayBinarySet<?>);

        Set<Integer> ss4 = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
                23, 24, 25, 26, 27, 28, 29, 30, 31, 32);

        assertTrue(ss4.getClass().getName(), ss4 instanceof LinkedHashSet<?>);
    }

    enum Foo {
        BAR,
        BAZ,
        SKIDDOO,
        WOOGLE
    }
}
