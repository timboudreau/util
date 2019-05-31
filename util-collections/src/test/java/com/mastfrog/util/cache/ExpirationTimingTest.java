/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
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
package com.mastfrog.util.cache;

import com.mastfrog.util.preconditions.Exceptions;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ExpirationTimingTest {

    private static final long MAX_AGE = 400L;
    private final long ADD = 100;
    private String[] names;

    @Test
    public void test() throws Throwable {
        ExpireIt ei = new ExpireIt();
        TimedCache<String, Thing, IOException> tc = TimedCache.<String, Thing, IOException>createThrowing(MAX_AGE,
                (String request) -> new Thing(request)).onExpire(ei);

        TimedCacheImpl<String, Thing, IOException> tci = (TimedCacheImpl<String, Thing, IOException>) tc;
        assertEquals(MAX_AGE, tci.timeToLive);

        Map<String, Integer> idForThing = new HashMap<>();
        long youngest = 0;
        for (String n : names) {
            Thing thing = tc.get(n);
            youngest = Math.max(youngest, thing.created);
            assertNotNull(thing);
            Integer val = idForThing.get(n);
            if (val == null) {
                idForThing.put(n, System.identityHashCode(thing));
            } else {
                assertEquals("Thing was replaced in cache", val.intValue(), System.identityHashCode(thing));
            }

            tci.cache.entrySet().forEach((Map.Entry<String, TimedCacheImpl<String, Thing, IOException>.CacheEntry> en) -> {
                TimedCacheImpl<String, Thing, IOException>.CacheEntry v = en.getValue();
                long touched = v.touched;
                assertNotEquals("Entry touch value is 0", 0L, touched);
                long delay = v.getDelay(TimeUnit.MILLISECONDS);
                assertTrue("Wrong delay <= 0 for " + en, delay > 0);
                long elapsed = System.currentTimeMillis() - touched;
                assertFalse(en.getKey() + ": Elapsed " + elapsed
                        + " delay " + delay
                        + " should be " + (System.currentTimeMillis() - touched), v.isExpired());
            });
            ei.checkThrown();
            assertFalse(thing.wasExpired());
        }
        for (long then = youngest; System.currentTimeMillis() - then < MAX_AGE + ADD;) {
            for (String n : names) {
                Thing t = tc.get(n);
                boolean exp = t.wasExpired();
                // this could race with a nice GC pause
                if (System.currentTimeMillis() - t.created < MAX_AGE + 20) {
                    assertFalse(exp);
                }
                assertNotNull(t);
                ei.checkThrown();
            }
            ei.checkThrown();
        }
    }

    @Before
    public void before() {
        names = new String[20];
        for (int i = 0; i < names.length; i++) {
            char c = (char) ('a' + i);
            names[i] = c + "-" + i;
        }
    }

    static final class Thing {

        private final long created = System.currentTimeMillis();
        private final String name;
        private volatile boolean expired;

        public Thing(String name) {
            this.name = name;
        }

        public long age() {
            return System.currentTimeMillis() - created;
        }

        @Override
        public String toString() {
            return name + ":" + age() + "ms age;" + (expired ? "expired" : "alive");
        }

        void expire() {
            if (expired) {
                throw new IllegalStateException("Expired twice");
            }
            expired = true;
        }

        boolean wasExpired() {
            return expired;
        }
    }

    static final class ExpireIt implements BiConsumer<String, Thing> {

        Throwable t;

        synchronized void checkThrown() {
            if (t != null) {
                t.initCause(new RuntimeException());
                Exceptions.chuck(t);
            }
        }

        @Override
        public void accept(String s, Thing u) {
            try {
                assertEquals(u.name, s);
                assertTrue("Expired too soon: " + u, u.age() > MAX_AGE);
            } catch (Throwable tt) {
                tt.printStackTrace();
                synchronized (this) {
                    if (t == null) {
                        t = tt;
                    } else {
                        t.addSuppressed(tt);
                    }
                }
            }
            u.expire();
        }
    }
}
