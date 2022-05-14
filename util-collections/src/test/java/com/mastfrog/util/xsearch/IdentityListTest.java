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

package com.mastfrog.util.xsearch;

import com.mastfrog.util.collections.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdentityListTest {

    @Test
    @SuppressWarnings("IncompatibleEquals")
    public void testIdList() {
        AlwaysEqual one = new AlwaysEqual();
        AlwaysEqual two = new AlwaysEqual();
        ArrayList<AlwaysEqual> al = new ArrayList<>();
        List<AlwaysEqual> il = CollectionUtils.newIdentityList();
        assertTrue(il.add(one));
        assertTrue(il.add(two));
        assertTrue(al.add(one));
        assertTrue(al.add(two));

        assertTrue(al.remove(new AlwaysEqual()));
        assertEquals(1, al.size());
        assertEquals(2, il.size());

        assertFalse(il.remove(new AlwaysEqual()));
        assertEquals(2, il.size());
        assertTrue(il.remove(two));
        assertEquals(1, il.size());
        assertTrue(il.remove(one));
        assertTrue(il.isEmpty());
        assertFalse(il.remove(one));

        assertEquals(il, il);
        assertFalse(il.equals(al));
    }

    static class AlwaysEqual {

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 23;
        }
    }
}
