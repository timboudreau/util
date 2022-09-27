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

import static com.mastfrog.reference.Thing.strings;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class CleanupQueueTest {

    @Test
    public void testSomeMethod() throws Exception {
        Set<String> cleared = ConcurrentHashMap.newKeySet();
        List<Thing> things = Thing.things("cq", 32, 4);
        List<ThingRef> refs = refs(things, cleared::add);
        Set<String> expectedCleared = strings(things);

        Set<String> removes = removeEvery(things, 3);

        assertFalse(removes.isEmpty());

        assertNotEquals(removes.size(), expectedCleared.size(), "Everything was removed");

        forceGc();
        assertFalse(cleared.isEmpty());

        assertEquals(removes, cleared);

        things.clear();

        forceGc();
        assertEquals(expectedCleared, cleared);
    }

    Set<String> removeEvery(List<Thing> things, int count) {
        Set<String> result = new TreeSet<>();
        int cursor = 0;
        for (Iterator<Thing> it = things.iterator(); it.hasNext();) {
            Thing t = it.next();
            if ((cursor++ % count) == 0) {
                it.remove();
                result.add(t.toString());
            }
        }
        return result;
    }

    @SuppressWarnings({"CallToThreadYield", "deprecation"})
    static void forceGc() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            System.gc();
            Thread.yield();
            Thread.sleep(1);
        }
    }

    List<ThingRef> refs(List<Thing> things, Consumer<String> od) {
        List<ThingRef> refs = new ArrayList<>();
        for (Thing t : things) {
            refs.add(new ThingRef(t, od));
        }
        return refs;
    }

    static final class ThingRef extends WeakReference<Thing> implements Runnable {

        private final String str;
        private final Consumer<String> onDispose;

        ThingRef(Thing thing, Consumer<String> onDispose) {
            super(thing, CleanupQueue.queue());
            str = thing.toString();
            this.onDispose = onDispose;
        }

        @Override
        public void run() {
            onDispose.accept(str);
        }

    }

}
