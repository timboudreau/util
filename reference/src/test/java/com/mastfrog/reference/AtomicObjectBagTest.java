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

import static com.mastfrog.reference.Thing.things;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicObjectBagTest {

    @Test
    public void testBagOfFour() {
        ObjectBag<Thing> bag = new ObjectBag<>(4);
        doTestBag(bag);
    }

    @Test
    public void testBagOfOne() {
        ObjectBag<Thing> bag = new ObjectBag<>(1);
        doTestBag(bag);
    }

    @Test
    public void testBagOfThirtyTwo() {
        ObjectBag<Thing> bag = new ObjectBag<>(1);
        doTestBag(bag);
    }

    public void doTestBag(ObjectBag<Thing> bag) {
        IntSupp vals = new IntSupp();

        List<Thing> things = things("aa", 32, 4, vals);
        assertTrue(bag.isEmpty(), bag::toString);
        assertEquals(0, bag.size(), bag::toString);
        distribute(things, bag);

        assertFalse(bag.isEmpty(), bag::toString);
        assertEquals(things.size(), bag.size(), bag::toString);

        for (Thing t : things) {
            assertTrue(bag.contains(t));
        }
        assertEquals(new TreeSet<>(things), bag.snapshot());

        Set<Thing> rem = removing(bag);
        assertTrue(rem.isEmpty());

        vals.set(3);
        rem = removing(bag);

        assertFalse(rem.isEmpty());

        for (Thing tt : rem) {
            assertFalse(bag.contains(tt), bag::toString);
            assertFalse(bag.snapshot().contains(tt), bag::toString);
        }

        assertEquals(things.size() - rem.size(), bag.size());

        Set<Thing> remaining = bag.snapshot();
        Set<Thing> expectedRemaining = new HashSet<>(things);
        expectedRemaining.removeAll(rem);

        assertEquals(expectedRemaining, remaining, bag::toString);

        assertEquals(bag.drain(), expectedRemaining);

        assertTrue(bag.isEmpty());
    }

    private static Set<Thing> removing(ObjectBag<Thing> bag) {
        Set<Thing> removed = new TreeSet<>();
        bag.removing(item -> {
            if (item.isTarget()) {
                removed.add(item);
                return true;
            }
            return false;
        });
        return removed;
    }

    private static <T> void distribute(Collection<? extends T> t, ObjectBag<T> bag) {
        List<T> objs = new ArrayList<>(t);
        Collections.shuffle(objs, new Random(1921083013));
        for (T obj : objs) {
            distribute(obj, bag);
        }
    }

    private static <T> void distribute(T t, ObjectBag<T> bag) {
        int ix = bag.add(t);
    }

}
