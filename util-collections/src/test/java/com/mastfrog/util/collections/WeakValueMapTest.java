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

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Tim Boudreau
 */
public class WeakValueMapTest {

    @Test
    public void testWeakValues() {
        List<Doodad> all = Doodad.list("one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine");
        Map<String, Doodad> weak = new WeakValueMap<>(16);
        Doodad.map(all, weak);
        assertEquals(Doodad.map(all), weak);
        for (int i = all.size() - 1; i >= 0; i--) {
            if (i % 2 == 0) {
                Doodad d = all.remove(i);
            }
        }
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
            Thread.yield();
        }
        assertEquals(setOf("six", "four", "two", "eight"), weak.keySet());
        assertEquals(Doodad.map(Doodad.list("two", "four", "six", "eight")), weak);
    }

    @Test
    public void testGeneralSetFunctionality() {
if (true) return; //FIXME -IOOBE
        Random r = new Random(52);
        List<String> names = new ArrayList<>();
        for (char a = 'a'; a <= 'z'; a++) {
            for (char b = 'a'; b <= 'z'; b++) {
                String s = new String(new char[]{a, b});
                names.add(s);
            }
        }
        String[] all = names.toArray(new String[0]);
        Map<String, Doodad> strong = new HashMap<>();
        Map<String, Doodad> weak = new WeakValueMap<>(100);
        List<Doodad> allDoodads = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            for (int k = 0; k < 10; k++) {
                for (int j = 0; j < 10; j++) {
                    int start = r.nextInt(names.size() - 1);
                    int len = r.nextInt(names.size() - start);
                    List<String> toAdd = names.subList(start, start + len);
                    allDoodads.addAll(Doodad.putTwoMaps(strong, weak, toAdd));
                    assertEquals(weak, strong);
                    assertEquals(strong.hashCode(), weak.hashCode());
                    assertEquals(strong.keySet(), weak.keySet());
                    assertEquals(strong.size(), weak.size());
                }
                for (int j = 0; j < 20; j++) {
                    List<String> l = new ArrayList<>(strong.keySet());
                    if (l.isEmpty()) {
                        break;
                    }
                    String toRemove = l.get(r.nextInt(l.size()));
                    assertNotNull(weak.remove(toRemove));
                    assertNotNull(strong.remove(toRemove));
                    assertFalse(strong.containsKey(toRemove));
                    assertFalse("Still present: " + toRemove, weak.containsKey(toRemove));
                    assertEquals(strong, weak);
                    assertEquals(strong.hashCode(), weak.hashCode());
                    assertEquals(strong.keySet(), weak.keySet());
                    assertEquals(strong.size(), weak.size());
                }
                for (int j = 0; j < 10; j++) {
                    int start = r.nextInt(names.size());
                    int end = Math.min(names.size() - 1, start + r.nextInt(10));
                    List<String> toAdd = names.subList(start, start + end);
                    Map<String, Doodad> nue = new HashMap<>();
                    allDoodads.addAll(Doodad.put(nue, toAdd));
                    strong.putAll(nue);
                    weak.putAll(nue);
                    assertEquals(strong, weak);
                    assertEquals(strong.hashCode(), weak.hashCode());
                    assertEquals(strong.keySet(), weak.keySet());
                    assertEquals(strong.size(), weak.size());
                }
            }
        }
        while (!strong.isEmpty()) {
            List<String> l = new ArrayList<>(strong.keySet());
            int ix;
            if (l.size() == 1) {
                ix = 0;
            } else {
                ix = r.nextInt(l.size());
            }
            strong.remove(l.get(ix));
            weak.remove(l.get(ix));
            assertEquals(strong.isEmpty(), weak.isEmpty());
            assertEquals(strong, weak);
            assertEquals(strong.hashCode(), weak.hashCode());
            assertEquals(strong.keySet(), weak.keySet());
            assertEquals(strong.size(), weak.size());
        }
        allDoodads.clear();

        Doodad.putTwoMaps(strong, weak, names);
        assertEquals(strong.size(), weak.size());
        strong.clear();
        weak.clear();
        assertTrue(strong.isEmpty());
        assertTrue(weak.isEmpty());

        Doodad.putTwoMaps(strong, weak, names);
        strong.clear();
        for (int i = 0; i < 50; i++) {
            System.gc();
            System.runFinalization();
            Thread.yield();
        }
        assertTrue(weak.isEmpty());
    }

    static class Doodad {

        private final String text;

        public Doodad(String text) {
            this.text = text;
        }

        static List<Doodad> putTwoMaps(Map<String, Doodad> m, Map<String, Doodad> m1, String... names) {
            List<Doodad> doodads = new ArrayList<>();
            for (String s : names) {
                Doodad doo = new Doodad(s);
                m.put(s, doo);
                m1.put(s, doo);
                doodads.add(doo);
            }
            return doodads;
        }

        static List<Doodad> put(Map<String, Doodad> m, String... names) {
            List<Doodad> doodads = new ArrayList<>();
            for (String s : names) {
                Doodad doo = new Doodad(s);
                m.put(s, doo);
                doodads.add(doo);
            }
            return doodads;
        }

        static List<Doodad> put(Map<String, Doodad> m, List<String> names) {
            List<Doodad> doodads = new ArrayList<>();
            for (String s : names) {
                Doodad doo = new Doodad(s);
                m.put(s, doo);
                doodads.add(doo);
            }
            return doodads;
        }

        static List<Doodad> putTwoMaps(Map<String, Doodad> m, Map<String, Doodad> m1, List<String> names) {
            List<Doodad> doodads = new ArrayList<>();
            for (String s : names) {
                Doodad doo = new Doodad(s);
                m.put(s, doo);
                m1.put(s, doo);
                doodads.add(doo);
            }
            return doodads;
        }

        static List<Doodad> list(String... args) {
            List<Doodad> result = new ArrayList<>();
            for (String a : args) {
                result.add(new Doodad(a));
            }
            return result;
        }

        static Map<String, Doodad> map(Collection<? extends Doodad> coll, Map<String, Doodad> into) {
            for (Doodad d : coll) {
                into.put(d.text, d);
            }
            return into;

        }

        static Map<String, Doodad> map(Collection<? extends Doodad> coll) {
            Map<String, Doodad> result = new LinkedHashMap<>();
            return map(coll, result);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.text);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Doodad other = (Doodad) obj;
            if (!Objects.equals(this.text, other.text)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
