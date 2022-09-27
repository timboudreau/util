/* 
 * The MIT License
 *
 * Copyright 2020 Tim Boudreau.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class PhantomReferencePoolTest {

    @Test
    public void testSomeMethod() throws InterruptedException {
        Thing t = new Thing();
        StringBuilder sb = getOne(t);
        String last = "Thing " + t;
        sb.append(last);
        t = null;
        t = new Thing();
        generatePressure(true);
        StringBuilder sb2 = getOne(t);
        assertEquals(last, sb2.toString());
    }

    @Test
    public void testWithBackgroundThread() throws InterruptedException {
        pool.start();
        Thing t = new Thing();
        StringBuilder sb = getOne(t);
        String last = "Thing " + t;
        sb.append(last);
        t = null;
        t = new Thing();
        generatePressure(false);
        StringBuilder sb2 = getOne(t);
        assertEquals(last, sb2.toString());
    }

    @Test
    public void testMultipleWithBackgroundThread() throws InterruptedException {
        pool.start();
        Set<Integer> idHcs = new TreeSet<>();
        List<Thing> things = new ArrayList<>();
        Set<String> strings = new HashSet<>();
        for (int i = 0; i < pool.maximumSize() + 10; i++) {
            Thing t = new Thing();
            things.add(t);
            StringBuilder sb = pool.takeFromPool(t);
            assertTrue(sb.length() == 0, "Got used instance '" + sb + "'");
            String s = "Thing-" + i;
            sb.append(s);
            strings.add(s);
            idHcs.add(System.identityHashCode(sb));
        }
        things.clear();
        generatePressure(false);
        generatePressure(false);

        Map<Thing, StringBuilder> sbFor = new LinkedHashMap<>();
        for (int i = 0; i < pool.maximumSize() + 20; i++) {
            Thing t = new Thing();
            things.add(t);
            StringBuilder sb = pool.takeFromPool(t);
            if (sb.length() > 0) {
                assertFalse(sbFor.values().contains(sb), "Got '" + sb + "' twice");
            } else {
                sb.append("Thing-" + t);
            }
            sbFor.put(t, sb);
        }

        Thing nue = new Thing();
        StringBuilder nxt = pool.takeFromPool(nue);
        assertTrue(nxt.length() == 0, "Should be no items available for reuse");
        nxt.append("next");
        Thing nue2 = new Thing();
        StringBuilder nxt2 = pool.takeFromPool(nue2);
        assertTrue(nxt2.length() == 0, "Should be no items available for reuse");
        nxt2.append("nextNext");

        assertTrue(pool.isReclamationRunning());
        things.clear();
        sbFor.clear();
        generatePressure(false);
        generatePressure(false);
        pool.reclaimAvailable();
        assertEquals(2, pool.outstanding());

        Thing more = new Thing();
        StringBuilder moreSb = pool.takeFromPool(more);
        assertFalse(moreSb.length() == 0);

        for (int i = 0; i < pool.maximumSize() + 10; i++) {
            Thing t = new Thing();
            things.add(t);
            StringBuilder sb = pool.takeFromPool(t);
            assertNotSame(nxt, sb);
            assertNotSame(nxt2, sb);
            assertNotSame(moreSb, sb);
        }
    }

    private void generatePressure(boolean poll) throws InterruptedException {
        generateGcPressure(poll);
        if (!poll) {
            Thread.sleep(50);
        }
        generateGcPressure(poll);
        if (!poll) {
            Thread.sleep(50);
            generateGcPressure(poll);
            Thread.sleep(50);
        }
    }

    private void generateGcPressure(boolean poll) {
        byte[] bts;
        List<byte[]> s = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            bts = new byte[1024 * 1024];
            Arrays.fill(bts, (byte) i);
            bts[bts.length - 1] = (byte) -1;
            bts[0] = 23;
            s.add(bts);
            if (poll) {
                StringBuilder sb = pool.reclaimationPoll();
                if (sb != null) {
                    break;
                }
            }
        }
        s.clear();
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
        }
    }

    private StringBuilder getOne(Thing thing) {
        return pool.takeFromPool(thing);
    }

    private PhantomReferencePool<StringBuilder> pool;

    @BeforeEach
    public void setup() {
        pool = new PhantomReferencePool<>("stuff", 20, StringBuilder::new);
    }

    @AfterEach
    public void teardown() {
        pool.stop();
    }

    private static final class Thing {

        private static int ids = 0;
        private final int id = ids++;

        public String toString() {
            return Integer.toString(id);
        }
    }
}
