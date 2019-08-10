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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class SimpleWeakSetTest {

    private void forceGc(Reference<?> ref) throws InterruptedException {
        for (int i = 0; i < 35; i++) {
            if (ref.get() == null) {
                break;
            }
            System.gc();
            System.runFinalization();
            Thread.sleep(20);
        }
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        Thing thing1 = new Thing("foo");
        Thing thing2 = new Thing("bar");
        Thing thing3 = new Thing("baz");
        SimpleWeakSet<Thing> weak = new SimpleWeakSet<>(Arrays.asList(thing1, thing2, thing3));
        assertFalse(weak.isEmpty());
        assertTrue(weak.contains(thing1));
        assertTrue(weak.contains(thing2));
        assertTrue(weak.contains(thing3));
        assertEquals(3, weak.size());
        Reference<Thing> ref1 = new WeakReference<>(thing1);
        Reference<Thing> ref2 = new WeakReference<>(thing2);
        Reference<Thing> ref3 = new WeakReference<>(thing3);

        assertEquals(new HashSet<>(Arrays.asList(thing1, thing2, thing3)), weak);
        assertEquals(new HashSet<>(Arrays.asList(thing1, thing2, thing3)).hashCode(), weak.hashCode());

        thing1 = null;
        forceGc(ref1);
        assertTrue(weak.contains(thing2));
        assertTrue(weak.contains(thing3));
        assertNull(ref1.get());
        assertEquals(2, weak.size());

        thing2 = null;
        forceGc(ref2);
        assertTrue(weak.contains(thing3));
        assertNull(ref2.get());
        assertEquals(1, weak.size());

        thing3 = null;
        forceGc(ref3);
        assertNull(ref3.get());
        assertTrue(weak.isEmpty());

        thing1 = new Thing("mfoo");
        thing2 = new Thing("mbar");
        thing3 = new Thing("mbaz");
        weak.addAll(Arrays.asList(thing1, thing2, thing3));

        assertEquals(3, weak.size());

        assertTrue(weak.remove(thing2));
        assertFalse(weak.contains(thing2));
        assertEquals(2, weak.size());
    }

    static final class Thing {

        private final String val;

        public Thing(String val) {
            this.val = val;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + Objects.hashCode(this.val);
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
            final Thing other = (Thing) obj;
            return Objects.equals(this.val, other.val);
        }

        @Override
        public String toString() {
            return "Thing{" + "val=" + val + '}';
        }
    }
}
