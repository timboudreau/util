package com.mastfrog.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class IntListTest {

    private List<Integer> real = new ArrayList<>();
    private IntListImpl test = new IntListImpl(5);

    @Test
    public void testBasic() {
        addOne(3);
        addOne(5);
        addOne(7);
        addOne(9);
        addMany(11, 13, 15, 17, 19);
        addMany2(21, 23, 25, 27);
        test(a -> {
            boolean result = a.contains(7);
            assertTrue(real.toString(), result);
            return result;
        }, b -> {
            boolean result = b.contains(7);
            assertTrue(test.toString(), result);
            return result;
        });
        remove(2);
        test(a -> {
            boolean result = a.contains(7);
            assertFalse(test.toString(), result);
            return result;
        }, b -> {
            boolean result = b.contains(7);
            assertFalse(test.toString(), result);
            return result;
        });
        insertAt(72, 2);
        insertAt(73, 2);
        clear();
        assertTrue(test.isEmpty());
        assertEquals(0, test.size());
    }

    private void remove(int at) {
        check((a, b) -> {
            a.remove(at);
            b.removeAt(at);
        });
    }

    private void insertAt(int value, int at) {
        check((a, b) -> {
            a.add(at, value);
            b.add(at, value);
        });
    }

    private void addMany(int... val) {
        check((a, b) -> {
            List<Integer> l = new ArrayList<>(val.length);
            for (int i = 0; i < val.length; i++) {
                l.add(val[i]);
            }
            a.addAll(l);
            b.addArray(val);
        });
    }

    private void addMany2(int... val) {
        check((a, b) -> {
            List<Integer> l = new ArrayList<>(val.length);
            for (int i = 0; i < val.length; i++) {
                l.add(val[i]);
            }
            a.addAll(l);
            b.addAll(val);
        });
    }

    private void addOne(int val) {
        check((a, b) -> {
            a.add(val);
            b.add(val);
        });
    }

    private void clear() {
        check((a, b) -> {
            a.clear();
            b.clear();
        });
    }
    
    void test(Predicate<List<Integer>> a, Predicate<IntListImpl> b) {
        test((a1, b1) -> {
            return a.test(a1) == b.test(b1);
        });
    }

    void test(BiPredicate<List<Integer>, IntListImpl> tester) {
        assertTrue(tester.test(real, test));
    }

    void check(BiConsumer<List<Integer>, IntListImpl> manipulation) {
        manipulation.accept(real, test);
        assertEquals(test.toString() + " expected " + real, real.size(), test.size());
        assertEquals(test.toString(), real, test);
        assertEquals(test.toString(), real.hashCode(), test.hashCode());
    }

}
