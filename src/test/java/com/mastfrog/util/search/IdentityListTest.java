/*
 *               BSD LICENSE NOTICE
 * Copyright (c) 2010-2012, Tim Boudreau
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.util.search;

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
