/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class ImmutableSetTest {

    @Test
    public void testIdentityVersusEquality() {
        HC hca, hcb, hcc;
        HC[] normal = new HC[]{
            hca = new HC("a", 1),
            hcb = new HC("b", 2),
            hcc = new HC("c", 3)
        };
        Set<HC> hcs = ImmutableSet.of(false, normal);
        assertTrue(hcs instanceof ImmutableSet);
        assertTrue(hcs.contains(hca));
        assertTrue(hcs.contains(hcb));
        assertTrue(hcs.contains(hcc));

        Set<HC> hcsIdentity = ImmutableSet.of(true, normal);
        assertTrue(hcsIdentity.contains(hca));
        assertTrue(hcsIdentity.contains(hcb));
        assertTrue(hcsIdentity.contains(hcc));

        assertFalse(hcsIdentity.contains(hca.copy()));
        assertFalse(hcsIdentity.contains(hcb.copy()));
        assertFalse(hcsIdentity.contains(hcc.copy()));

        assertEquals(hcs, hcsIdentity);
        assertTrue(hcs.containsAll(hcsIdentity));
        assertTrue(hcsIdentity.containsAll(hcs));

        assertTrue(hcs.containsAll(ImmutableSet.of(true, hca, hcb, hcc)));
        assertTrue(hcsIdentity.containsAll(ImmutableSet.of(true, hca, hcb, hcc)));
        assertFalse(hcsIdentity.containsAll(ImmutableSet.of(true, hca, hcb.copy(), hcc)));

        assertEquals(CollectionUtils.setOf(hca, hcb, hcc), hcs);
        assertEquals(CollectionUtils.setOf(hca, hcb, hcc), hcsIdentity);
    }

    @Test
    public void testNullsTolerated() {
        Set<String> a = testOne(set -> {
            set.add("a");
            set.add("b");
            set.add(null);
            set.add("c");
            set.add("d");
        });
        assertTrue(a.contains(null));
    }

    @Test
    public void testNullIsSingleton() {
        Set<Object> nulls = ImmutableSet.of(false, null, null, null);
        assertNotNull(nulls);
        assertEquals(1, nulls.size());
        assertEquals(0, nulls.hashCode());
        assertTrue("Set should report true for contains(null)", nulls.contains(null));
    }

    @Test
    public void testCornerCases() {
        HC hca, hcb, hcc;
        HC[] normal = new HC[]{
            hca = new HC("a", 1),
            hcb = new HC("b", 2),
            hcc = new HC("c", 3)
        };
        Set<HC> hcs = ImmutableSet.of(false, normal);

        HC weird = new HC("a", 4);
        HC weirder = new HC("a", 2);

        assertFalse(hcs.contains(weird));
        assertFalse(hcs.contains(weirder));

        ImmutableSet.of(false, hca, hcb, hcc, weird);

        try {
            ImmutableSet.of(false, hca, hcb, hcc, weirder);
            fail("Should not have allowed two non-equal objects with the same hash code through");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        HC weirdest = new HC("q", 0);
        Set<HC> withNull = ImmutableSet.of(false, hca, hcb, hcc, null);
        assertTrue(withNull.contains(null));
        assertFalse(withNull.contains(weirdest));

        Set<HC> nullOrZero = ImmutableSet.of(false, hca, null, hcb, hcc, weirdest);
        assertTrue(nullOrZero.contains(weirdest));
        assertFalse(nullOrZero.contains(null));
        try {
            nullOrZero = ImmutableSet.of(false, hca, weirdest, hcb, hcc, null);
            fail("When object with 0 hash code is encountered before the null, the "
                    + "situation should be detected and rejected");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testImmutableSet() {
        Set<String> notPresent = set("i", "j", "k", "l", "m", "n", "o",
                "p", "q", "r", "s", "t", "u", "v", "fish", "gumbo", "wookies",
                "ankle bearding", "tube drivers", "poodlefish", "wunk",
                "zoobitahoogita wukle wumpers fnood - don't read the fnords");
        ImmutableSet<String> s = testOne(set -> {
            set.addAll(Arrays.asList("a", "b", "c", "d", "d", "e", "f", "g", "work"));
        });
        Set<String> andMore = new HashSet<>(s);
        andMore.add("h");
        assertFalse(s.containsAll(andMore));
        for (String st : notPresent) {
            assertFalse(st + " reported present", s.contains(st));
        }

        ImmutableSet<String> s2 = testOne(set -> {
            set.addAll(Arrays.asList("a"));
        });

        ImmutableSet<String> s3 = testOne(set -> {

        });
        for (String st : notPresent) {
            assertFalse(st + " reported present", s2.contains(st));
        }
        for (String st : notPresent) {
            assertFalse(st + " reported present", s3.contains(st));
        }

        ImmutableSet<String> s4 = testOne(set -> {
            char[] cc = new char[2];
            for (char c = 'A'; c <= 'Z'; c++) {
                for (char c1 = '0'; c1 <= '9'; c1++) {
                    cc[0] = c;
                    cc[1] = c1;
                    set.add(new String(cc));
                }
            }
        });
        for (String st : notPresent) {
            assertFalse(st + " reported present", s4.contains(st));
            char[] cc = new char[3];
            for (char c = 'A'; c <= 'Z'; c++) {
                for (char c1 = '0'; c1 <= '9'; c1++) {
                    cc[0] = c;
                    cc[1] = c1;
                    cc[2] = c;
                    assertFalse(s4.contains(new String(cc)));
                }
            }
        }
    }

    @SuppressWarnings("element-type-mismatch")
    private <T> ImmutableSet<T> testOne(Consumer<Set<T>> c) {
        HashSet<T> set = new HashSet<>();
        c.accept(set);
        ImmutableSet<T> imm = new ImmutableSet<>(set, false);
        for (T t : set) {
            assertTrue("Contains reports false for " + t + " in " + imm, imm.contains(t));
        }
        assertSetsEqual(set, imm);
        List<T> l = new ArrayList<>(set);
        l.addAll(set);
        l.addAll(set);
        ImmutableSet<T> imm2 = new ImmutableSet<>(l, false);
        assertSetsEqual(set, imm2);
        assertFalse(imm.contains("z"));
        for (int i = 1; i < set.size() - 1; i++) {
            Set<T> partial = new HashSet<>();
            int ix = 0;
            for (T t : set) {
                if (ix++ > i) {
                    partial.add(t);
                    assertTrue(imm.containsAll(partial));
                }
            }
        }
        return imm;
    }

    private <T> void assertSetsEqual(Collection<? extends T> expect, Collection<? extends T> got) {
        for (T obj : expect) {
            assertTrue("Missing " + obj, got.contains(obj));
        }
        if (expect.size() != got.size()) {
            fail("Sizes differ");
        }

        Set<T> str = new HashSet<>();
        got.stream().forEach(str::add);
        assertEquals("Stream results differ", expect, str);

        Set<T> fe = new HashSet<>();
        got.forEach(fe::add);
        assertEquals(expect, fe);

        Spliterator<? extends T> spl = got.spliterator();
        Set<T> splGot = new HashSet<>();
        if (expect.size() > 2) {
            Spliterator<? extends T> splitB = spl.trySplit();
            assertNotNull(splitB);
            Spliterator<? extends T> splitC = splitB.trySplit();
            spl.forEachRemaining(splGot::add);
            splitB.forEachRemaining(splGot::add);
            if (splitC != null) {
                splitC.forEachRemaining(splGot::add);
            }
        } else {
            spl.forEachRemaining(splGot::add);
        }
        assertEquals("Spliterators did not iterate all objects", expect, splGot);
        assertEquals("Wrong hash code", expect.hashCode(), got.hashCode());
    }

    @SafeVarargs
    @SuppressWarnings("FinalPrivateMethod")
    private final <T> Set<T> set(T... objs) {
        HashSet<T> result = new HashSet<>();
        for (T o : objs) {
            result.add(o);
        }
        return result;
    }

    static final class HC {

        private final String name;
        private final int hashCode;

        public HC(String name, int hashCode) {
            this.name = name;
            this.hashCode = hashCode;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || !(o instanceof HC)) {
                return false;
            } else {
                HC other = (HC) o;
                return other.name.equals(name);
            }
        }

        HC copy() {
            return new HC(name, hashCode);
        }

        public String toString() {
            return name + ":" + hashCode;
        }
    }
}
