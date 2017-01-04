/*
 * The MIT License
 *
 * Copyright 2010-2015 Tim Boudreau.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class ReversedListTest {

    @Test
    public void test() {
        assertTrue(true);
        List<String> l = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            l.add("" + c);
        }
        ReversedList<String> rl = new ReversedList<>(l);
        Iterator<String> i = rl.iterator();
        assertTrue(i.hasNext());
        String s = i.next();
        assertNotNull(s);
        assertEquals("Z", s);
        assertTrue(i.hasNext());
        s = i.next();
        assertNotNull("Y", s);

        int ix = 0;
        for (char c = 'Z'; c >= 'A'; c--) {
            String curr = "" + c;
            assertEquals(curr, rl.get(ix++));
        }

        i = rl.iterator();
        for (char c = 'Z'; c >= 'A'; c--) {
            String curr = "" + c;
            assertTrue(i.hasNext());
            assertEquals(curr, i.next());
        }
        
        String[] all = rl.toArray(new String[0]);
        ix = 0;
        for (char c = 'Z'; c >= 'A'; c--) {
            String curr = "" + c;
            assertEquals(curr, all[ix++]);
        }
        
        for (int j = 0; j < all.length; j++) {
            assertEquals(rl.get(j), all[j]);
        }

        assertSame(l, rl.delegate());
        List<String> ll = CollectionUtils.reversed(rl);
        assertSame(l, ll);

        List<String> nue = new ArrayList<>(rl);
        Collections.reverse(nue);
        assertEquals(l, nue);

    }
}
