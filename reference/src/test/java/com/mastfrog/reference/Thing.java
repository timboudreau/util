/*
 * The MIT License
 *
 * Copyright 2022 Mastfrog Technologies.
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
package com.mastfrog.reference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Tim Boudreau
 */
final class Thing implements Comparable<Thing> {

    private final String name;
    private final int index;
    private final IntSupp supp;

    public Thing(String name, int index, IntSupp supp) {
        this.name = name;
        this.index = index;
        this.supp = supp;
    }

    static Set<String> strings(Collection<? extends Thing> c) {
        Set<String> result = new TreeSet<>();
        for (Thing t : c) {
            result.add(t.toString());
        }
        return result;
    }

    static List<Thing> things(String baseText, int count, int mod) {
        return things(baseText, count, mod, new IntSupp());
    }

    static List<Thing> things(String baseText, int count, int mod, IntSupp supp) {
        List<Thing> all = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String txt = baseText + "-" + i;
            int ix = i % mod;
            all.add(new Thing(txt, ix, supp));
        }
        return all;
    }

    boolean isTarget() {
        return supp.value() == index;
    }

    @Override
    public String toString() {
        return name + "(" + index + ")::" + isTarget();
    }

    @Override
    public int compareTo(Thing o) {
        int result = name.compareTo(o.name);
        if (result == 0) {
            result = Integer.compare(index, o.index);
        }
        return result;
    }

}
