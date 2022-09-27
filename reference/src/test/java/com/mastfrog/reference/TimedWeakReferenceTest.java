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
import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Tim Boudreau
 */
public class TimedWeakReferenceTest {

    private static final long DELAY = 150;

    @Test
    public void testSomeMethod() throws InterruptedException {
        long when = currentTimeMillis();
        List<Thing> things = Thing.things("trw", 32, 4);
        List<TimedWeakReference<Thing>> trws = trws(things);

        for (TimedWeakReference<Thing> trw : trws) {
            Thing tt = trw.getIf(x -> false);
            assertNull(tt);
            assertNotNull(trw.get());
            boolean str = trw.isStrong();
            boolean exp = trw.isExpired();
            boolean expectStrong = currentTimeMillis() - when < DELAY;
            if (expectStrong) {
                assertTrue(trw.isStrong());
                assertFalse(trw.isExpired());
            }
        }
        sleep(max(10L, (when + DELAY + DELAY / 2) - currentTimeMillis()));
        for (TimedWeakReference<Thing> trw : trws) {
            assertTrue(trw.isExpired(), "Not expired: " + trw);
            trw.getIf(x -> {
                assertNotNull(x);
                return false;
            });
            assertTrue(trw.isExpired());
            assertNotNull(trw.rawGet(), "Referent is still referenced - TimedWeakReference should return it");
        }

        long newExp = 0;
        for (TimedWeakReference<Thing> trw : trws) {
            Thing t = trw.get();
            newExp = currentTimeMillis() + DELAY;
            assertFalse(trw.isExpired(), "Calling get() did not clear status on " + trw);
        }

        things.clear();
        for (TimedWeakReference<Thing> trw : trws) {
            boolean wasNull = (trw.rawGet() == null);
            boolean expectPresent = currentTimeMillis() < newExp;
            if (expectPresent) {
                assertFalse(trw.isExpired());
            }
        }

        long rem = newExp - currentTimeMillis();
        sleep(max(10L, rem + (rem / 2)));
        forceGc();

        for (TimedWeakReference<Thing> trw : trws) {
            assertFalse(trw.isStrong(), () -> "Should not still be strongly referenced: " + trw);
            assertTrue(trw.isExpired());
            assertNull(trw.rawGet());
            assertNull(trw.get());
            assertNull(trw.getIf(x -> true));
        }
    }

    static List<TimedWeakReference<Thing>> trws(List<Thing> things) {
        List<TimedWeakReference<Thing>> result = new ArrayList<>();
        for (Thing t : things) {
            result.add(TimedWeakReference.create(t, 140));
        }
        return result;
    }

}
