/*
 * The MIT License
 *
 * Copyright 2019 Tim Boudreau.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class MergeIterablesTest {

    private MergeIterables<String> mi = new MergeIterables<>();
    private List<String> list = new ArrayList<>();

    @Test
    public void testSimple() {
        add("A", "B", "C");
        add("D", "E", "F", "G");
        add("H", "I", "J");
        add("K", "L", "M", "N", "O");
    }

    private void add(String... strings) {
        test((mi, list) -> {
            List<String> toAdd = Arrays.asList(strings);
            list.addAll(toAdd);
            mi.add(toAdd);
        });
    }

    private void test(BiConsumer<MergeIterables<String>, List<String>> cons) {
        cons.accept(mi, list);
        List<String> got = new ArrayList<>();
        for (String s : mi) {
            got.add(s);
        }
        assertEquals(list, got);
    }

}
