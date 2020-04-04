/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
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

import com.mastfrog.util.collections.IntIntMap.IntIntMapConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntIntMapImplTest {

    @Test
    public void testSimple() {
        IntIntMapImpl m = new IntIntMapImpl(20);
        IntSetImpl expKeys = new IntSetImpl();
        IntSetImpl vals = new IntSetImpl();
        Map<Integer, Integer> mExp = new LinkedHashMap<>(100);
        List<Integer> expVals = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            m.put(i + 1, i * 10);
            expKeys.add(i + 1);
            mExp.put(i + 1, i * 10);
            vals.add(i * 10);
            expVals.add(i * 10);
        }
        assertEquals(100, m.size());
        for (int i = 0; i < m.size(); i++) {
            assertTrue(m.containsKey(i + 1));
            assertEquals(i * 10, m.getAsInt(i + 1));
        }

        assertEquals(expKeys, m.keySet());
        assertEquals(mExp, m);
        assertEquals(expVals, m.values());
        assertEquals(vals, m.values());
        
        Map<Integer, Integer> mFound = new LinkedHashMap<>();

        m.forEachPair((IntIntMapConsumer) (key, val) -> {
            mFound.put(key, val);
        });
        assertEquals(mExp, mFound);

        IntSet toRemove = new IntSetImpl();
        for (int i = 1; i <= 100; i++) {
            if (i % 10 == 0) {
                toRemove.add(i);
                mFound.remove(i);
            }
        }
        m.removeAll(toRemove);
        assertEquals(mFound, m);
    }
}
