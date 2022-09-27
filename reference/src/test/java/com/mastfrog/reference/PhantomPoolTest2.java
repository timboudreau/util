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

import static com.mastfrog.reference.CleanupQueueTest.forceGc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PhantomPoolTest2 {

    @Test
    public void testIt() throws InterruptedException {
        System.out.println("PRP ");
        PhantomReferencePool<TextHolder> pool = new PhantomReferencePool<>("th", 30, TextHolder::new);
        List<HolderOfTextHolder> holderHolders = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            HolderOfTextHolder h = new HolderOfTextHolder(pool);
            holderHolders.add(h);
        }

        Set<String> strings = strings(holderHolders);
        holderHolders.clear();

        forceGc();

        System.out.println("New iter: ");
        for (int i = 0; i < 20; i++) {
            HolderOfTextHolder h = new HolderOfTextHolder(pool);
            holderHolders.add(h);
        }

        Set<String> newStrings = strings(holderHolders);
        assertEquals(strings, newStrings);
    }

    private Set<String> strings(Collection<? extends Object> c) {
        Set<String> result = new TreeSet<>();
        for (Object o : c) {
            result.add(o.toString());
        }
        return result;
    }

    static int ct;

    static class TextHolder {

        private final int ix = ct++;
        private String text = "empty-" + ix;

        TextHolder() {

        }

        TextHolder(String txt) {
            this.text = txt;
        }

        int ix() {
            return ix;
        }

        public String get() {
            return text;
        }

        public void set(String txt) {
            this.text = txt;
        }

        public String toString() {
            return text;
        }
    }

    static class HolderOfTextHolder {

        private final TextHolder holder;

        HolderOfTextHolder(PhantomReferencePool<TextHolder> pool) {
            this.holder = pool.takeFromPool(this);
        }

        public String toString() {
            return holder.toString();
        }
    }

}
