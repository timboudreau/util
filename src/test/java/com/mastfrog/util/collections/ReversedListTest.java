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
