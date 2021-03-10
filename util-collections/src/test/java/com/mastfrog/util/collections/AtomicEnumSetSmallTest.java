/*
 * The MIT License
 *
 * Copyright 2021 Mastfrog Technologies.
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

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim Boudreau
 */
public class AtomicEnumSetSmallTest {

    @Test
    public void testSomeMethod() {
        AtomicEnumSetSmall<X> a = new AtomicEnumSetSmall<>(X.class);
        assertTrue(a.isEmpty());
        assertEquals(0, a.size());

        boolean added = a.add(X.TWO);
        assertTrue("two not added", added);
        assertEquals("size should be one after add", 1, a.size());
        assertFalse("should not be ampty after adding one", a.isEmpty());
        assertFalse("should not contain the unadded item", a.contains(X.ONE));
        assertTrue("should contain added item", a.contains(X.TWO));
        assertFalse("should not contain another unadded item", a.contains(X.THREE));
        added = a.add(X.FOUR);
        assertFalse("Should not be empty after adding two items", a.isEmpty());
        assertEquals(2, a.size());
        assertTrue("four not added", added);
        assertTrue("should contain the second added item", a.contains(X.FOUR));
        assertFalse("should not contain the unadded item", a.contains(X.ONE));
        assertTrue("should contain added item", a.contains(X.TWO));
        assertFalse("should not contain another unadded item", a.contains(X.THREE));
        assertFalse("should not contain another unadded item", a.contains(X.ZERO));
        int old = a.get();
        assertNotEquals(0, old);
        boolean removed = a.remove(X.ONE);
        int nue = a.get();
        assertNotEquals(0, old);
        assertEquals(old, nue);
        assertFalse(removed);

        assertEquals(a.complement().toString(), cString(a.complement()));
        assertEquals(a.complement().toString(), itString(a.complement()));

        removed = a.remove(X.TWO);
        assertTrue(removed);
        assertEquals(1, a.size());

        assertFalse(a.complement().containsAll(a));
        assertEquals(a.complement().toString(), cString(a.complement()));
        assertEquals(a.complement().toString(), itString(a.complement()));

        assertTrue(a.remove(X.FOUR));
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());
        assertEquals("Complement of the empty set should be all constants, but have wrong size " + a.complement()
                + " should have " + EnumSet.allOf(X.class), X.values().length, a.complement().size());

        assertEquals(a.complement().toString(), cString(a.complement()));
        assertEquals(a.complement().toString(), itString(a.complement()));
    }

    @Test
    public void testVs() {

        AtomicEnumSetSmall<V> vsEmpty = new AtomicEnumSetSmall<V>(V.class);
        AtomicEnumSetSmall<V> vs = vsEmpty.complement();

        int ct = 1;
        for (V v : V.values()) {
            if (!vs.contains(v)) {
                fail("Missing from full one: " + v);
            }
            if (vsEmpty.contains(v)) {
                fail("Present in empty one: " + v);
            }
            boolean added = vsEmpty.add(v);
            assertTrue("added should be true", added);
            boolean removed = vs.remove(v);
            assertTrue("removed should be true", removed);

            boolean added2 = vsEmpty.add(v);
            assertFalse("second add should notbe true", added2);
            boolean removed2 = vs.remove(v);
            assertFalse("second remove should be false", removed2);

            assertEquals("Size incorrect after addition " + vsEmpty, ct, vsEmpty.size());
            assertEquals("Size incorrect after removals", V.values().length - ct, vs.size());
            ct++;
        }

        vsEmpty.retainAll(EnumSet.of(V.V_00, V.V_05, V.V_10, V.V_15, V.V_20, V.V_25, V.V_30));

        assertEquals("Differing iteration to construct string", vsEmpty.toString(), itString(vsEmpty));
        assertEquals("Differing iteration to construct string", vsEmpty.toString(), cString(vsEmpty));

        int oldSize = vsEmpty.size();
        AtomicEnumSetSmall<V> addIt = new AtomicEnumSetSmall<>(V.V_03, V.V_31, V.V_30);
        vsEmpty.addAll(addIt);
        assertEquals(vsEmpty.size(), oldSize + 2);

        assertEquals("Differing iteration to construct string", vsEmpty.toString(), itString(vsEmpty));
        assertEquals("Differing iteration to construct string", vsEmpty.toString(), cString(vsEmpty));

        assertEquals("Differing iteration to construct string", vsEmpty.complement().toString(), itString(vsEmpty.complement()));
        assertEquals("Differing iteration to construct string", vsEmpty.complement().toString(), cString(vsEmpty.complement()));

        AtomicEnumSetSmall<V> none = new AtomicEnumSetSmall<>(V.class);
        assertTrue(none.isEmpty());
        assertEquals(0, none.size());
        boolean[] called = new boolean[1];
        none.forEach(x -> called[0] = true);
        assertFalse(called[0]);
        assertFalse(none.iterator().hasNext());

        try {
            none.iterator().next();
            fail("NSEE should be thrown");
        } catch (NoSuchElementException ex) {

        }

        assertEquals(none.toString(), itString(none));
        assertEquals(none.toString(), cString(none));

        EnumSet en = EnumSet.of(V.V_00, V.V_01, V.V_02, V.V_03, V.V_04, V.V_05, V.V_06);

        AtomicEnumSetSmall<V> comp = vsEmpty.complement();

        comp.retainAll(new HashSet<>(Arrays.asList(V.V_00, V.V_01, V.V_02, V.V_03, V.V_04, V.V_05, V.V_06)));

        for (V vv : comp) {
            assertTrue("Element " + vv + " retained which was not passed to retainAll()", en.contains(vv));
        }

        AtomicEnumSetSmall<V> copy = comp.copy();
        AtomicEnumSetSmall<V> copy2 = comp.copy();
        comp.removeAll(new AtomicEnumSetSmall<>(V.V_02, V.V_03, V.V_03, V.V_04));
        copy.removeAll(EnumSet.of(V.V_02, V.V_03, V.V_03, V.V_04));
        copy2.removeAll(Arrays.asList(V.V_02, V.V_03, V.V_03, V.V_04));

        assertEquals(setOf(V.V_01, V.V_06), comp);
        assertEquals(setOf(V.V_01, V.V_06), copy);
        assertEquals(setOf(V.V_01, V.V_06), copy2);
        assertEquals(copy, comp);
        assertEquals(copy2, comp);
    }

    @Test
    public void testPermutations() {
        Permutations<N> perms = new Permutations<>(N.class);
        Consumer<Set<N>> retainer = set -> {
            set.retainAll(odds());
        };
        testManipulation("retainAll", retainer, perms);
        Consumer<Set<N>> remover = set -> {
            set.removeAll(odds());
        };
        testManipulation("removeAll ", remover, perms);
        Consumer<Set<N>> adder = set -> {
            set.addAll(odds());
        };
        testManipulation("removeAll ", adder, perms);
        for (int i = 0; i < N.values().length; i++) {
            int ii = i;
            Consumer<Set<N>> singleRem = set -> {
                for (int j = 0; j < ii; j++) {
                    boolean was = set.contains(N.values()[j]);
                    boolean removed = set.remove(N.values()[j]);
                    assertFalse(set.contains(N.values()[j]));
                    assertEquals("Was present but not removed", removed, was);
                }
            };
            testManipulation("removeSingle  " + i + " - " + 32, singleRem, perms);

            Consumer<Set<N>> singleAdd = set -> {
                for (int j = 0; j < ii; j++) {
                    boolean was = set.contains(N.values()[j]);
                    boolean added = set.add(N.values()[j]);
                    assertTrue(set.contains(N.values()[j]));
                    assertNotEquals(was, added);
                }
            };
            testManipulation("addSingle " + i + " - " + 32, singleAdd, perms);
        }

        for (int i = 3; i < 13; i++) {
            int ii = i;
            Consumer<Set<N>> grouper = set -> {
                groupsOf(ii, set1 -> {
                    Consumer<Set<N>> adder2 = st -> {
                        set.addAll(set);
                    };
                    testManipulation("Subman ", adder2, perms);
                });
            };
            testManipulation("By groups of " + i + " addAll", grouper, perms);
            Consumer<Set<N>> grouper2 = set -> {
                groupsOf(ii, set1 -> {
                    Consumer<Set<N>> adder2 = st -> {
                        set.removeAll(set);
                    };
                    testManipulation("Subman ", adder2, perms);

                });
            };
            testManipulation("By groups of " + i + "removeAll", grouper, perms);
            Consumer<Set<N>> grouper3 = set -> {
                groupsOf(ii, set1 -> {
                    Consumer<Set<N>> adder2 = st -> {
                        set.retainAll(set);
                    };
                    testManipulation("Subman ", adder2, perms);

                });
            };
            testManipulation("By groups of " + i + "removeAll", grouper, perms);
        }

        Random rnd = new Random(139813013);

        testManipulation("ContainsAll", set -> {
            Set<N> ss = EnumSet.noneOf(N.class);
            HashSet<N> hash = new HashSet<>();
            AtomicEnumSetSmall<N> ato = new AtomicEnumSetSmall<>(N.class);
            List<N> l = new ArrayList<>(set);
            Collections.shuffle(l, rnd);
            N[] ns = N.values();
            for (int i = 0; i < l.size(); i++) {
                hash.clear();
                ato.clear();
                assertTrue(ato.isEmpty());
                assertEquals(0, ato.size());
                ss.clear();
                for (int j = i; j < l.size(); j++) {
                    N n = l.get(i);
                    boolean add1 = hash.add(n);
                    boolean add2 = ato.add(n);
                    boolean add3 = ss.add(n);

                    assertEquals("Adding " + n + " to " + hash + " and " + ato
                            + " return different results - hash " + add1 + " ato " + add2, add1, add2);
                    assertEquals("Adding " + n + " to " + ato + " and " + ss
                            + " return different results - hash " + add1 + " ss " + add3, add1, add3);

                    assertEquals(hash, ato);
                    assertEquals(ss, ato);
                    assertTrue("Set " + set.getClass().getSimpleName() + " claims not to contain all of AtomicEnumSetSmall " + ato + " - " + hash + " and " + set, set.containsAll(ato));
                    assertTrue("Set " + set.getClass().getSimpleName() + " claims not to contain all of AtomicEnumSetSmall " + ss + " - " + set + " and " + set, set.containsAll(ss));
                    assertTrue("Set " + set.getClass().getSimpleName() + " claims not to contain all of HashSet " + hash + " - " + hash + " and " + set, set.containsAll(hash));

                    assertTrue(set.containsAll(ss));

                }
            }
        }, perms);
    }

    private static Set<N> odds() {
        return byIndex(i -> i % 2 == 0);
    }

    private static void groupsOf(int by, Consumer<Set<N>> c) {
        int max = N.values().length;
        int groups = max / by;
        int sz = max / groups;
        for (int i = 0; i < groups; i++) {
            int start = i * sz;
            Set<N> sa = new HashSet<>();
            Set<N> s1 = EnumSet.noneOf(N.class);
            Set<N> s2 = new AtomicEnumSetSmall<>(N.class);
            for (int j = start; j < Math.min(max, start + sz); j++) {
                N n = N.values()[j];
                sa.add(n);
                s1.add(n);
                s2.add(n);
            }
        }
    }

    private static Set<N> byIndex(IntPredicate p) {
        AtomicEnumSetSmall<N> result = new AtomicEnumSetSmall<>(N.class);
        for (int i = 0; i < N.values().length; i++) {
            if (p.test(i)) {
                result.add(N.values()[i]);
            }
        }
        return result;
    }

    static class Permutations<E extends Enum<E>> implements Consumer<Tri<String, Set<E>, Set<E>>> {

        final Class<E> type;
        final Set<E> base;

        public Permutations(Class<E> type) {
            this.type = type;
            base = new HashSet<>(Arrays.asList(type.getEnumConstants()));
        }

        private final Random rnd = new Random(23013);

        int removeOne() {
            int r = rnd.nextInt(base.size());
            base.remove(new ArrayList<>(base).get(r));
            return base.size();
        }

        @Override
        public void accept(Tri<String, Set<E>, Set<E>> t) {
            for (;;) {
                EnumSet<E> ens = EnumSet.noneOf(type);
                ens.addAll(base);
                AtomicEnumSetSmall<E> avv = new AtomicEnumSetSmall<>(type);
                avv.addAll(base);
                t.accept("", ens, avv);
                if (removeOne() <= 2) {
                    break;
                }
            }
            base.addAll(Arrays.asList(type.getEnumConstants()));
        }
    }

    private <T extends Enum<T>> void testManipulation(String what, Consumer<Set<T>> tester, Consumer<Tri<String, Set<T>, Set<T>>> b) {
        b.accept((taskName, left, right) -> {
            tester.accept(left);
            tester.accept(right);

            assertEquals(taskName + " sets are not equal: " + left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName(), left, right);
            assertEquals(taskName + " toString not equal: " + left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName(), left.toString(), right.toString());
            assertEquals(taskName + " hashCode not equal: " + left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName(), left.hashCode(), right.hashCode());
            Set<T> iteratedA = new TreeSet<>();
            iteratedA.addAll(left);
            Set<T> iteratedB = new TreeSet<>();
            iteratedB.addAll(right);
            assertEquals(taskName + " copy using iterator not equal", iteratedA, iteratedB);
            iteratedA.clear();
            iteratedB.clear();
            left.forEach(iteratedA::add);
            right.forEach(iteratedB::add);
            assertEquals(taskName + " copy using forEach not equal", iteratedA, iteratedB);

            assertEquals(left.size(), right.size());
            assertNotSame(left, right);
            assertFalse(left.getClass() == right.getClass());

            assertEquals(left.isEmpty(), right.isEmpty());

            left.clear();
            right.clear();
            assertTrue(left.isEmpty());
            assertTrue(right.isEmpty());
        });
    }

    static <T extends Enum<T>> String itString(AtomicEnumSetSmall<T> coll) {
        StringBuilder sb = new StringBuilder("[");
        for (T obj : coll) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(obj);
        }
        return sb.append(']').toString();
    }

    static <T extends Enum<T>> String cString(AtomicEnumSetSmall<T> coll) {
        StringBuilder sb = new StringBuilder("[");
        coll.forEach(obj -> {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(obj);
        });
        return sb.append(']').toString();
    }

    interface Tri<A, B, C> {

        void accept(A a, B b, C c);
    }

    public enum V {
        V_00(1),
        V_01(2),
        V_02(4),
        V_03(8),
        V_04(16),
        V_05(32),
        V_06(64),
        V_07(128),
        V_08(256),
        V_09(512),
        V_10(1024),
        V_11(2048),
        V_12(4096),
        V_13(8192),
        V_14(16384),
        V_15(32768),
        V_16(65536),
        V_17(131072),
        V_18(262144),
        V_19(524288),
        V_20(1048576),
        V_21(2097152),
        V_22(4194304),
        V_23(8388608),
        V_24(16777216),
        V_25(33554432),
        V_26(67108864),
        V_27(134217728),
        V_28(268435456),
        V_29(536870912),
        V_30(1073741824),
        V_31(-2147483648);
        private final int mask;

        V(int mask) {
            this.mask = mask;
        }

        public String toString() {
//            return name() + "(" + Integer.toBinaryString(mask) + ")";
            return Integer.toString(ordinal());
        }
    }

    public enum X {
        ZERO,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGH,
        NINE,
        TEN,
        ELEVEN,
        TWELVE,
        THIRTEEN,
        FOURTEEN
    }

    public enum N {
        // Need something smaller than all 32 bits to ensure we
        // never overflow and try to use bits that are > element count
        N_00(1),
        N_01(2),
        N_02(4),
        N_03(8),
        N_04(16),
        N_05(32),
        N_06(64),
        N_07(128),
        N_08(256),
        N_09(512),
        N_10(1024),
        N_11(2048),
        N_12(4096),
        N_13(8192),
        N_14(16384),
        N_15(32768),
        N_16(65536),
        N_17(131072),
        N_18(262144),
        N_19(524288),
        N_20(1048576),
        N_21(2097152),
        N_22(4194304),
        N_23(8388608),
        N_24(16777216),
        N_25(33554432),
        N_26(67108864),
        N_27(134217728),
        N_28(268435456);
        private final int mask;

        N(int mask) {
            this.mask = mask;
        }

        public String toString() {
//            return name() + "(" + Integer.toBinaryString(mask) + ")";
            return Integer.toString(ordinal());
        }
    }

}
