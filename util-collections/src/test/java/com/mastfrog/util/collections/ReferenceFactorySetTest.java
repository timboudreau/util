/*
 * Copyright 2016-2019 Tim Boudreau, Frédéric Yvon Vinet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mastfrog.util.collections;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ReferenceFactorySetTest {

    @Test
    @SuppressWarnings({"UnusedAssignment", "CallToThreadYield"})
    public void testSet() throws IllegalArgumentException, InterruptedException {
        ReferenceFactorySet<Thing> set = new ReferenceFactorySet<>(Ref::new);
        IntSet ihcs = IntSet.arrayBased(10);
        List<Thing> things = Thing.list(ihcs, "Hello", "world", "this", "is", "fun", "stuff");
        set.addAll(things);

        assertEquals(new HashSet<>(things), set);
        assertTrue(set.containsAll(things));
        assertFalse(set.contains(new Thing("wookie")));
        assertFalse(set.contains(null));

        int origSize = things.size();
        assertEquals(origSize, set.size());

        List<Thing> sub = things.subList(0, 3);
        IntSet subIhc = ihcs(sub);
        assertTrue("Did not remove " + sub + " from " + set, set.removeAll(sub));

        assertEquals("Items not removed " + set, things.size() - sub.size(), set.size());
        for (Thing t : things) {
            if (sub.contains(t)) {
                assertFalse("Still present: " + t + " in " + set, set.contains(t));
            } else {
                assertTrue(set.contains(t));
            }
        }
        assertNotEquals(set, things);
        set.addAll(sub);
        assertEquals(set, new HashSet<>(things));
        things.removeAll(sub);
        things = new ArrayList<>(things);
        sub = null;
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
            Thread.yield();
        }
        IntSet collected = RQ.collect(origSize, 10000);
        assertEquals(subIhc, collected);
        assertEquals(things.size(), set.size());
    }

    static IntSet ihcs(Collection<?> items) {
        IntSet is = IntSet.arrayBased(items.size());
        for (Object o : items) {
            is.add(System.identityHashCode(o));
        }
        return is;
    }

    private static class Thing {

        private final String name;

        public Thing(String name) {
            this.name = name;
        }

        static List<Thing> list(IntSet ihcs, String... names) {
            List<Thing> result = new ArrayList<>();
            for (String s : names) {
                Thing t = new Thing(s);
                ihcs.add(System.identityHashCode(t));
                result.add(t);
            }
            return result;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.name);
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
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }
    }

    static class Ref extends WeakReference<Thing> {

        private final int hc;

        public Ref(Thing strong) {
            super(strong, RQ);
            hc = System.identityHashCode(strong);
        }

        @Override
        public String toString() {
            return "Ref(" + get() + ")";
        }
    }

    static RQ RQ = new RQ();

    static class RQ extends ReferenceQueue<Thing> {

        @Override
        public Reference<? extends Thing> remove(long timeout)
                throws IllegalArgumentException, InterruptedException {
            return super.remove(timeout);
        }

        IntSet collect(int anticipated, long timeout)
                throws IllegalArgumentException, InterruptedException {
            long end = System.currentTimeMillis() + timeout;
            long slice = timeout / anticipated;
            IntSet set = IntSet.arrayBased(anticipated);
            for (int i = 0; i < anticipated; i++) {
                Reference<? extends Thing> ref = remove(slice);
                if (ref instanceof Ref) {
                    set.add(((((Ref) ref).hc)));
                }
                if (System.currentTimeMillis() >= end) {
                    break;
                }
            }
            return set;
        }
    }
}
